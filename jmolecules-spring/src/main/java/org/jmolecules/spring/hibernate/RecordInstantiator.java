/*
 * Copyright 2022 the original author or authors.
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
package org.jmolecules.spring.hibernate;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;
import org.jmolecules.ddd.types.Identifier;
import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

/**
 * Hibernate 6 specific {@link EmbeddableInstantiator} that inspects a {@link Record} for its {@link RecordComponent}s
 * to assemble instances of it in {@link #instantiate(Supplier, SessionFactoryImplementor)}. To use it directly, declare
 * a subclass of it invoking {@link RecordInstantiator}'s constructor with the record type at hand and use that declared
 * types via {@link org.hibernate.annotations.EmbeddableInstantiator} on the record type.
 *
 * @author Oliver Drotbohm
 */
public class RecordInstantiator implements EmbeddableInstantiator {

	private final Class<?> type;
	private final List<Integer> indexes;
	private final Constructor<?> constructor;

	/**
	 * Creates a new {@link AssociationAttributeConverter} for the given {@link Identifier} type.
	 *
	 * @param type must not be {@literal null}.
	 */
	public RecordInstantiator(Class<?> type) {

		Assert.notNull(type, "Record type must not be null!");
		Assert.isTrue(type.isRecord(), "Type must be a record!");

		RecordComponent[] components = type.getRecordComponents();
		Class<?>[] parameterTypes = Arrays.stream(components)
				.map(RecordComponent::getType)
				.toArray(Class<?>[]::new);

		this.type = type;
		this.constructor = detectRecordConstructor(type, parameterTypes);
		this.indexes = IntStream.range(0, components.length)
				.mapToObj(it -> Map.entry(components[it].getName(), it))
				.sorted(Comparator.comparing(Entry::getKey))
				.map(Entry::getValue)
				.collect(Collectors.toList());
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

		return BeanUtils.instantiateClass(constructor, indexes.stream().map(it -> sources[it]).toArray());
	}

	private static Constructor<?> detectRecordConstructor(Class<?> type, Class<?>... parameterTypes) {

		try {
			return type.getDeclaredConstructor(parameterTypes);
		} catch (NoSuchMethodException | SecurityException e) {

			String message = String.format("Could not find record constructor on %s!", type.getClass());
			throw new IllegalArgumentException(message, e);
		}
	}
}
