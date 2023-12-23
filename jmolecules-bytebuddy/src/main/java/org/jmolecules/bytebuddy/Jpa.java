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
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * Helper to abstract which flavor of JPA we need to work with.
 *
 * @author Oliver Drotbohm
 */
@Slf4j
@RequiredArgsConstructor
class Jpa {

	private static Optional<Jpa> INSTANCE;

	private final String basePackage;
	private final Provider provider;

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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	<T> T getCascadeTypeAll() {

		Class<Enum> annotation = loadClass("CascadeType");

		return (T) Enum.valueOf(annotation, "ALL");
	}

	/**
	 * Returns the FetchType.EAGER value for the JPA flavor in use.
	 *
	 * @param <T>
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	<T extends Enum<?>> T getFetchTypeEager() {

		Class fetchTypeType = loadClass("FetchType");
		return (T) Enum.valueOf(fetchTypeType, "EAGER");
	}

	/**
	 * Returns the FetchType.LAZY value for the JPA flavor in use.
	 *
	 * @param <T>
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	<T> T getFetchTypeLazy() {

		Class fetchTypeType = loadClass("FetchType");
		return (T) Enum.valueOf(fetchTypeType, "LAZY");
	}

	boolean isHibernate() {
		return Provider.HIBERNATE.equals(provider);
	}

	/**
	 * Returns the annotation with the given name. If unqualified, the package of the JPA flavor in use will be prepended.
	 *
	 * @param name must not be {@literal null} or empty.
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	<T extends Annotation> Class<T> getAnnotation(String name) {
		return (Class) loadClass(name);
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

		if (world == null) {
			throw new IllegalArgumentException("ClassWorld must not be null!");
		}

		if (INSTANCE == null) {
			INSTANCE = createJavaPersistence(world);
		}

		return INSTANCE;
	}

	private static Optional<Jpa> createJavaPersistence(ClassWorld world) {

		Provider provider = world.isAvailable("org.hibernate.Hibernate")
				? Provider.HIBERNATE
				: Provider.GENERIC;

		if (world.isAvailable("javax.persistence.Entity")) {

			log.info("jMolecules - Detected legacy JPA…");
			return Optional.of(new Jpa("javax.persistence", provider));

		} else if (world.isAvailable("jakarta.persistence.Entity")) {

			log.info("jMolecules - Detected Jakarta Persistence…");
			return Optional.of(new Jpa("jakarta.persistence", provider));
		}

		return Optional.empty();
	}

	@SuppressWarnings("unchecked")
	private <T> Class<T> loadClass(String name) {

		if (name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException("Type must not be null or empty!");
		}

		name = name.contains(".") ? name : basePackage.concat(".").concat(name);

		try {
			return (Class<T>) Class.forName(name, false, Jpa.class.getClassLoader());
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

	private enum Provider {
		HIBERNATE, GENERIC;
	}
}
