/*
 * Copyright 2021-2025 the original author or authors.
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

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.Plugin.WithPreprocessor;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jmolecules.bytebuddy.PluginLogger.Log;
import org.jmolecules.ddd.types.Association;

/**
 * Registers a dedicated subclass of {@code AssociationAttributeConverter} for each {@link Association} declared without
 * an explicit {@link AttributeConverter} registered via {@link Convert}.
 *
 * @author Oliver Drotbohm
 */
@NoArgsConstructor
@AllArgsConstructor
public class JMoleculesSpringJpaPlugin implements LoggingPlugin, WithPreprocessor {

	private Jpa jpa;

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.matcher.ElementMatcher#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(TypeDescription target) {

		return !PluginUtils.isCglibProxyType(target)
				&& (target.getDeclaredAnnotations().isAnnotationPresent(jpa.getAnnotation("Entity"))
						|| target.isAssignableTo(org.jmolecules.ddd.types.Entity.class));
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.bytebuddy.JMoleculesPluginSupport#onPreprocess(net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public void onPreprocess(TypeDescription typeDescription, ClassFileLocator classFileLocator) {

		ClassWorld world = ClassWorld.of(classFileLocator);
		this.jpa = Jpa.getJavaPersistence(world).get();
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.build.Plugin#apply(net.bytebuddy.dynamic.DynamicType.Builder, net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public Builder<?> apply(Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {

		Log log = PluginLogger.INSTANCE.getLog(typeDescription, "Spring JPA");

		return JMoleculesTypeBuilder.of(log, builder)
				.mapBuilder(this::addConvertAnnotationIfNeeded)
				.conclude();
	}

	private Builder<?> addConvertAnnotationIfNeeded(Builder<?> builder, Log logger) {

		List<InDefinedShape> associationFields = builder.toTypeDescription().getDeclaredFields().stream()
				.filter(isAssociationOrCollectionThereof())
				.collect(Collectors.toList());

		for (InDefinedShape field : associationFields) {

			if (field.getDeclaredAnnotations().isAnnotationPresent(jpa.getAnnotation("Convert"))) {

				logger.info("Found existing converter registration for field {}.", field.getName());

				continue;
			}

			builder = createConvertAnnotation(field, builder, logger);
		}

		return builder;
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {}

	private Builder<?> createConvertAnnotation(InDefinedShape field, Builder<?> builder, Log log) {

		boolean isCollection = field.getType().asErasure().isAssignableTo(Collection.class);
		Generic associationType = isCollection ? field.getType().getTypeArguments().get(0) : field.getType();

		TypeList.Generic generic = associationType.asGenericType().getTypeArguments();
		Generic aggregateType = generic.get(0);
		Generic idType = generic.get(1);
		Generic idPrimitiveType = getIdPrimitiveType(idType);

		if (idPrimitiveType == null) {

			log.info("{} - Unable to detect id primitive in {}.", field.getName(), PluginUtils.abbreviate(idType));

			return builder;
		}

		ForLoadedType loadedType = new TypeDescription.ForLoadedType(jpa.getAssociationAttributeConverterBaseType());
		Generic superType = TypeDescription.Generic.Builder
				.parameterizedType(loadedType, aggregateType, idType, idPrimitiveType).build();

		Builder<?> converterBuilder = new ByteBuddy(ClassFileVersion.JAVA_V8)
				.with(new ReferenceTypePackageNamingStrategy(aggregateType.asErasure(), "AssociationConverter"))
				.subclass(superType)
				.annotateType(PluginUtils.getAnnotation(jpa.getAnnotation("Converter")));

		Unloaded<?> converterType = PluginUtils.markGenerated(converterBuilder, log)
				.defineConstructor(Visibility.PACKAGE_PRIVATE)
				.intercept(MethodCall.invoke(getConverterConstructor()).onSuper().with(idType.asErasure()))
				.make();

		builder = builder.require(converterType);

		Junction<FieldDescription> fieldMatcher = ElementMatchers.is(field);
		String logMessage = String.format("%s - Adding @j.p.Convert(converter=%s)", field.getName(),
				PluginUtils.abbreviate(converterType.getTypeDescription()));

		List<AnnotationDescription> annotations = new ArrayList<>();

		if (isCollection) {

			Class<Annotation> elementCollectionAnnotation = jpa.getAnnotation("ElementCollection");
			logMessage += " and " + PluginUtils.abbreviate(elementCollectionAnnotation);

			annotations.add(PluginUtils.getAnnotation(elementCollectionAnnotation));
		}

		log.info(logMessage + ".", field.getName(), PluginUtils.abbreviate(converterType.getTypeDescription()));

		annotations.add(AnnotationDescription.Builder.ofType(jpa.getAnnotation("Convert"))
				.define("converter", converterType.getTypeDescription())
				.build());

		return builder.field(fieldMatcher).annotateField(annotations);
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

	private static Predicate<? super InDefinedShape> isAssociationOrCollectionThereof() {

		return it -> {

			Generic type = it.getType();
			TypeDescription rawType = type.asErasure();

			if (rawType.represents(Association.class)) {
				return true;
			}

			if (!rawType.isAssignableTo(Collection.class)) {
				return false;
			}

			TypeList.Generic arguments = type.getTypeArguments();

			return !arguments.isEmpty() && arguments.get(0).asErasure().represents(Association.class);
		};
	}
}
