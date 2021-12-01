/*
 * Copyright 2021 the original author or authors.
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
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class JMoleculesSpringJpaPlugin extends JMoleculesPluginSupport {

	private static PluginLogger logger = new PluginLogger("Spring JPA");

	private Jpa jpa;

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.matcher.ElementMatcher#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(TypeDescription target) {
		return target.getDeclaredAnnotations().isAnnotationPresent(jpa.getAnnotation("Entity"))
				|| target.isAssignableTo(org.jmolecules.ddd.types.Entity.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.bytebuddy.JMoleculesPluginSupport#onPreprocess(net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public void onPreprocess(TypeDescription typeDescription, ClassFileLocator classFileLocator) {

		if (jpa != null) {
			return;
		}

		this.jpa = Jpa.getJavaPersistence(ClassWorld.of(classFileLocator)).get();
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.build.Plugin#apply(net.bytebuddy.dynamic.DynamicType.Builder, net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public Builder<?> apply(Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {

		return JMoleculesType.of(logger, builder)
				.map((it, log) -> {

					List<InDefinedShape> associationFields = builder.toTypeDescription().getDeclaredFields().stream()
							.filter(field -> field.getType().asErasure().represents(Association.class))
							.collect(Collectors.toList());

					for (InDefinedShape field : associationFields) {

						if (field.getDeclaredAnnotations().isAnnotationPresent(jpa.getAnnotation("Convert"))) {

							log.info("jMolecules Spring JPA - {}.{} - Found existing converter registration.",
									field.getDeclaringType().getSimpleName(), field.getName());

							continue;
						}

						it = createConvertAnnotation(field, it);
					}

					return it;

				}).conclude();
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

			log.info("jMolecules Spring JPA - {}.{} - Unable to detect id primitive in {}.",
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

		log.info("jMolecules Spring JPA - {}.{} - Adding @j.p.Convert(converter={}).",
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

		try {
			return jpa.getAssociationAttributeConverterBaseType().getDeclaredConstructor(Class.class);
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
