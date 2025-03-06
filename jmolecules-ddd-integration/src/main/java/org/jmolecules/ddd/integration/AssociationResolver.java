/*
 * Copyright 2020-2025 the original author or authors.
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
package org.jmolecules.ddd.integration;

import java.util.Optional;

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Identifier;

/**
 * Resolves {@link Association}s into the actual aggregate instance.
 *
 * @author Oliver Drotbohm
 * @param <T> the actual {@link AggregateRoot}
 * @param <ID> the actual {@link Identifier}
 */
public interface AssociationResolver<T extends AggregateRoot<T, ID>, ID extends Identifier>
		extends AggregateLookup<T, ID> {

	/**
	 * Resolves the {@link AggregateRoot} pointed to by the given {@link Association}.
	 *
	 * @param association must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	default Optional<T> resolve(Association<T, ID> association) {

		if (association == null) {
			throw new IllegalArgumentException("Association must not be null!");
		}

		return findById(association.getId());
	}

	/**
	 * Resolves the {@link AggregateRoot} pointed to by the given {@link Association}.
	 *
	 * @param association must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @throws IllegalArgumentException in case the resolution fails.
	 */
	default T resolveRequired(Association<T, ID> association) {

		if (association == null) {
			throw new IllegalArgumentException("Association must not be null!");
		}

		return resolve(association).orElseThrow(
				() -> new IllegalArgumentException(String.format("Could not resolve association %s!", association)));
	}
}
