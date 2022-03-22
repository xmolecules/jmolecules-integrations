/*
 * Copyright 2021-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmolecules.bytebuddy;

import jakarta.persistence.AttributeConverter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy.SuffixingRandom;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription.InDefinedShape;
import net.bytebuddy.description.field.FieldDescription.InGenericShape;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeDescription.ForLoadedType;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.stream.Collectors;

import org.jmolecules.ddd.types.Association;

/**
 * Registers a dedicated subclass of {@code AssociationAttributeConverter} for each {@link Association} declared without
 * an explicit {@link AttributeConverter} registered via {@link Convert}.
 *
 * @author Oliver Drotbohm
 */
@Slf4j
@NoArgsConstructor
public class JMoleculesSpringJpaPlugin extends JMoleculesPluginSupport {

	private static PluginLogger logger = new PluginLogger("Spring JPA");

	private Jpa jpa;
	private Class<? extends Annotation> embeddableInstantiatorAnnotationType;

	public JMoleculesSpringJpaPlugin(Jpa jpa, ClassWorld world) {
		init(jpa, world);
	}

	private void init(Jpa jpa, ClassWorld world) {

		if (this.jpa != null) {
			return;
		}

		this.jpa = jpa;

		if (world.isAvailable("org.hibernate.annotations.EmbeddableInstantiator")) {
			this.embeddableInstantiatorAnnotationType = jpa.getType("org.hibernate.annotations.EmbeddableInstantiator");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.matcher.ElementMatcher#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(TypeDescription target) {

		return target.getDeclaredAnnotations().isAnnotationPresent(jpa.getAnnotation("Entity"))
				|| target.isAssignableTo(org.jmolecules.ddd.types.Entity.class)
				|| target.isRecord();
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.bytebuddy.JMoleculesPluginSupport#onPreprocess(net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public void onPreprocess(TypeDescription typeDescription, ClassFileLocator classFileLocator) {

		ClassWorld world = ClassWorld.of(classFileLocator);
		init(Jpa.getJavaPersistence(world).get(), world);
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.build.Plugin#apply(net.bytebuddy.dynamic.DynamicType.Builder, net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public Builder<?> apply(Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {

		return JMoleculesType.of(logger, builder)
				.map(this::addConvertAnnotationIfNeeded)
				.map(JMoleculesType::isRecord, it -> it.annotateTypeIfMissing(jpa.getAnnotation("Embeddable")))
				.map(this::applyRecordInstantiator)
				.conclude();
	}

	private Builder<?> applyRecordInstantiator(Builder<?> builder, PluginLogger logger) {

		TypeDescription description = builder.toTypeDescription();

		// No record or Hibernate 6 present
		if (!description.isRecord() || embeddableInstantiatorAnnotationType == null) {
			return builder;
		}

		// Already annotated
		if (description.getDeclaredAnnotations().isAnnotationPresent(embeddableInstantiatorAnnotationType)) {

			logger.info("{} - Found explicit @EmbeddableInstantiator.",
					PluginUtils.abbreviate(description));

			return builder;
		}

		logger.info("{} - Adding @EmbeddableInstantiator for record.", PluginUtils.abbreviate(description));

		// Parent type information
		Class<?> instantiatorBaseType = jpa.getType("org.jmolecules.spring.hibernate.RecordInstantiator");
		ForLoadedType supeType = new TypeDescription.ForLoadedType(instantiatorBaseType);
		Constructor<?> constructor = getConstructor(instantiatorBaseType, Class.class);

		// Dedicated instantiator class for this particular record type
		Unloaded<?> instantiatorType = new ByteBuddy(ClassFileVersion.JAVA_V8)
				.with(new ReferenceTypePackageNamingStrategy(description))
				.subclass(supeType)
				.defineConstructor(Visibility.PACKAGE_PRIVATE)
				.intercept(MethodCall.invoke(constructor).onSuper().with(description))
				.make();

		builder = builder.require(instantiatorType);

		// Add the annotation
		return builder.annotateType(AnnotationDescription.Builder.ofType(embeddableInstantiatorAnnotationType)
				.define("value", instantiatorType.getTypeDescription())
				.build());
	}

	private Builder<?> addConvertAnnotationIfNeeded(Builder<?> builder, PluginLogger logger) {

		List<InDefinedShape> associationFields = builder.toTypeDescription().getDeclaredFields().stream()
				.filter(field -> field.getType().asErasure().represents(Association.class))
				.collect(Collectors.toList());

		for (InDefinedShape field : associationFields) {

			if (field.getDeclaredAnnotations().isAnnotationPresent(jpa.getAnnotation("Convert"))) {

				log.info("{}.{} - Found existing converter registration.",
						field.getDeclaringType().getSimpleName(), field.getName());

				continue;
			}

			builder = createConvertAnnotation(field, builder);
		}

		return builder;
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {}

	private Builder<?> createConvertAnnotation(InDefinedShape field, Builder<?> builder) {

		TypeList.Generic generic = field.getType().asGenericType().getTypeArguments();
		Generic aggregateType = generic.get(0);
		Generic idType = generic.get(1);
		Generic idPrimitiveType = getIdPrimitiveType(idType);

		if (idPrimitiveType == null) {

			log.info("{}.{} - Unable to detect id primitive in {}.",
					PluginUtils.abbreviate(field.getDeclaringType()), field.getName(),
					PluginUtils.abbreviate(idType));

			return builder;
		}

		ForLoadedType loadedType = new TypeDescription.ForLoadedType(jpa.getAssociationAttributeConverterBaseType());
		Generic superType = TypeDescription.Generic.Builder
				.parameterizedType(loadedType, aggregateType, idType, idPrimitiveType).build();

		Unloaded<?> converterType = new ByteBuddy(ClassFileVersion.JAVA_V8)
				.with(new ReferenceTypePackageNamingStrategy(field.getDeclaringType()))
				.subclass(superType)
				.annotateType(PluginUtils.getAnnotation(jpa.getAnnotation("Converter")))
				.defineConstructor(Visibility.PACKAGE_PRIVATE)
				.intercept(MethodCall.invoke(getConverterConstructor()).onSuper().with(idType.asErasure()))
				.make();

		builder = builder.require(converterType);

		log.info("{}.{} - Adding @j.p.Convert(converter={}).",
				PluginUtils.abbreviate(field.getDeclaringType()), field.getName(),
				PluginUtils.abbreviate(converterType.getTypeDescription()));

		return builder.field(ElementMatchers.is(field))
				.annotateField(AnnotationDescription.Builder.ofType(jpa.getAnnotation("Convert"))
						.define("converter", converterType.getTypeDescription())
						.build());
	}

	private static TypeDescription.Generic getIdPrimitiveType(Generic idType) {

		List<InGenericShape> fields = idType.getDeclaredFields().stream()
				.filter(it -> !it.isStatic())
				.collect(Collectors.toList());

		return fields.isEmpty()
				? getIdPrimitiveType(idType.getSuperClass())
				: fields.get(0).getType();
	}

	private Constructor<?> getConverterConstructor() {
		return getConstructor(jpa.getAssociationAttributeConverterBaseType(), Class.class);
	}

	private static Constructor<?> getConstructor(Class<?> type, Class<?>... parameters) {

		try {
			return type.getDeclaredConstructor(parameters);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * A naming strategy that returns a name for a type to be created so that it will be generated into the package of a
	 * reference type.
	 *
	 * @author Oliver Drotbohm
	 */
	private static class ReferenceTypePackageNamingStrategy extends SuffixingRandom {

		ReferenceTypePackageNamingStrategy(TypeDescription contextualType) {

			super("jMolecules", new BaseNameResolver() {

				/*
				 * (non-Javadoc)
				 * @see net.bytebuddy.NamingStrategy.SuffixingRandom.BaseNameResolver#resolve(net.bytebuddy.description.type.TypeDescription)
				 */
				public String resolve(TypeDescription type) {

					return contextualType.getPackage().getName()
							.concat(".")
							.concat(type.getSimpleName());
				}
			});
		}
	}
}
