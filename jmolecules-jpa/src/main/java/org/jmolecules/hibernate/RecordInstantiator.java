/*
 * Copyright 2022-2025 the original author or authors.
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
package org.jmolecules.hibernate;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Version;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;

/**
 * Hibernate 6 specific {@link EmbeddableInstantiator} that inspects a {@link Record} for its {@link RecordComponent}s
 * to assemble instances of it in {@link #instantiate(Supplier, SessionFactoryImplementor)}. To use it directly, declare
 * a subclass of it invoking {@link RecordInstantiator}'s constructor with the record type at hand and use that declared
 * types via {@link org.hibernate.annotations.EmbeddableInstantiator} on the record type.
 *
 * @author Oliver Drotbohm
 */
public class RecordInstantiator implements EmbeddableInstantiator {

	// https://hibernate.atlassian.net/browse/HHH-16457
	static final boolean IS_AFFECTED_HIBERNATE_VERSION = isAffectedHibernateVersion();

	private final Class<?> type;
	private final List<Integer> indexes;
	private final Constructor<?> constructor;

	/**
	 * Creates a new {@link AssociationAttributeConverter} for the given {@link Identifier} type.
	 *
	 * @param type must not be {@literal null}.
	 */
	public RecordInstantiator(Class<?> type) {

		if (type == null) {
			throw new IllegalArgumentException("Record type must not be null!");
		}

		if (!type.isRecord()) {
			throw new IllegalArgumentException("Type must be a record!");
		}

		List<RecordComponent> components = Arrays.asList(type.getRecordComponents());
		Class<?>[] parameterTypes = components.stream()
				.map(RecordComponent::getType)
				.toArray(Class<?>[]::new);

		this.type = type;
		this.constructor = detectRecordConstructor(type, parameterTypes);

		this.indexes = calculateIndexes(components);
	}

	/*
	 * (non-Javadoc)
	 * @see org.hibernate.metamodel.spi.Instantiator#isInstance(java.lang.Object, org.hibernate.engine.spi.SessionFactoryImplementor)
	 */
	@Override
	public boolean isInstance(Object object, SessionFactoryImplementor sessionFactory) {
		return type.isInstance(object);
	}

	/*
	 * (non-Javadoc)
	 * @see org.hibernate.metamodel.spi.Instantiator#isSameClass(java.lang.Object, org.hibernate.engine.spi.SessionFactoryImplementor)
	 */
	@Override
	public boolean isSameClass(Object object, SessionFactoryImplementor sessionFactory) {
		return type.equals(object.getClass());
	}

	/*
	 * (non-Javadoc)
	 * @see org.hibernate.metamodel.spi.EmbeddableInstantiator#instantiate(org.hibernate.metamodel.spi.ValueAccess, org.hibernate.engine.spi.SessionFactoryImplementor)
	 */
	@Override
	public Object instantiate(ValueAccess access, SessionFactoryImplementor factory) {

		Object[] sources = access.getValues();

		// See https://hibernate.atlassian.net/browse/HHH-16457
		Object[] parameters = IS_AFFECTED_HIBERNATE_VERSION
				? sources
				: indexes.stream().map(it -> sources[it]).toArray();

		try {
			return constructor.newInstance(parameters);
		} catch (InstantiationException | InvocationTargetException | IllegalAccessException o_O) {
			throw new RuntimeException(o_O);
		}
	}

	private static Constructor<?> detectRecordConstructor(Class<?> type, Class<?>... parameterTypes) {

		try {
			return makeAccessible(type.getDeclaredConstructor(parameterTypes));
		} catch (NoSuchMethodException | SecurityException e) {

			String message = String.format("Could not find record constructor on %s!", type.getClass());
			throw new IllegalArgumentException(message, e);
		}
	}

	@SuppressWarnings("deprecation")
	private static Constructor<?> makeAccessible(Constructor<?> constructor) {

		if ((!Modifier.isPublic(constructor.getModifiers())
				|| !Modifier.isPublic(constructor.getDeclaringClass().getModifiers()))
				&& !constructor.isAccessible()) {
			constructor.setAccessible(true);
		}

		return constructor;
	}

	private static boolean isAffectedHibernateVersion() {

		String version = Version.getVersionString();
		String[] parts = version.split("\\.");

		if (!parts[0].equals("6")) {
			return false;
		}

		if (!parts[1].equals("2")) {
			return false;
		}

		return Integer.parseInt(parts[2]) < 2;
	}

	private static List<Integer> calculateIndexes(List<RecordComponent> components) {

		if (components.size() == 1) {
			return Collections.singletonList(0);
		}

		List<RecordComponent> sorted = components.stream()
				.sorted(Comparator.comparing(RecordComponent::getName))
				.collect(Collectors.toList());

		return components.stream()
				.map(sorted::indexOf)
				.collect(Collectors.toList());
	}
}
