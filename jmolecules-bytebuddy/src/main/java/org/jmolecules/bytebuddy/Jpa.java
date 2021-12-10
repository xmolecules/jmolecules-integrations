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

import lombok.RequiredArgsConstructor;

import java.lang.annotation.Annotation;
import java.util.Optional;

import org.jmolecules.ddd.types.Association;
import org.springframework.core.Constants;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Helper to abstract which flavor of JPA we need to work with.
 *
 * @author Oliver Drotbohm
 */
@RequiredArgsConstructor
class Jpa {

	private final String basePackage;

	/**
	 * Returns the type to be used as base classed for {@code AttributeConverter} implementations for {@link Association}
	 * types.
	 *
	 * @return will never be {@literal null}.
	 */
	Class<?> getAssociationAttributeConverterBaseType() {

		return basePackage.startsWith("javax")
				? loadClass("org.jmolecules.spring.jpa.JpaAssociationAttributeConverter")
				: loadClass("org.jmolecules.spring.jpa.JakartaPersistenceAssociationAttributeConverter");
	}

	/**
	 * Returns the {@code CascadeType.ALL} enum value from the appropriate {@code CascadeType}.
	 *
	 * @return will never be {@literal null}.
	 */
	@SuppressWarnings("unchecked")
	<T> T getCascadeTypeAll() {

		Class<? extends Annotation> annotation = loadClass("CascadeType");
		Constants constants = new Constants(annotation);

		return (T) constants.asObject("ALL");
	}

	/**
	 * Returns the FetchType.EAGER value for the JPA flavor in use.
	 *
	 * @param <T>
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	<T> T getFetchTypeEager() {

		Class fetchTypeType = loadClass("FetchType");
		return (T) Enum.valueOf(fetchTypeType, "EAGER");
	}

	/**
	 * Returns the annotation with the given name. If unqualified, the package of the JPA flavor in use will be prepended.
	 *
	 * @param name must not be {@literal null} or empty.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	<T extends Annotation> Class<T> getAnnotation(String name) {
		return (Class<T>) loadClass(name);
	}

	@SuppressWarnings("unchecked")
	<T> Class<T> getType(String name) {
		return (Class<T>) loadClass(name);
	}

	/**
	 * Returns a {@link Jpa} instance configured to represent the JPA flavor in use with the project. Returns
	 * {@link Optional#empty()} if no JPA is on the classpath.
	 *
	 * @param world must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public static Optional<Jpa> getJavaPersistence(ClassWorld world) {

		Assert.notNull(world, "ClassWorld must not be null!");

		if (world.isAvailable("javax.persistence.Entity")) {
			return Optional.of(new Jpa("javax.persistence"));
		} else if (world.isAvailable("jakarta.persistence.Entity")) {
			return Optional.of(new Jpa("jakarta.persistence"));
		}

		return Optional.empty();
	}

	@SuppressWarnings("unchecked")
	private Class<? extends Annotation> loadClass(String name) {

		Assert.hasText(name, "Type must not be null or empty!");

		name = name.contains(".") ? name : basePackage.concat(".").concat(name);

		return (Class<? extends Annotation>) ClassUtils.resolveClassName(name, Jpa.class.getClassLoader());
	}
}
