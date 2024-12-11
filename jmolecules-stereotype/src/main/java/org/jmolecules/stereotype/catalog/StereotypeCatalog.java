/*
 * Copyright 2024-2025 the original author or authors.
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
package org.jmolecules.stereotype.catalog;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jmolecules.stereotype.api.Stereotype;

/**
 * A catalog of {@link StereotypeDefinition}s.
 *
 * @author Oliver Drotbohm
 */
public interface StereotypeCatalog extends Iterable<StereotypeDefinition> {

	/**
	 * Returns all {@link StereotypeGroup}s that contain the {@link Stereotype} with the given identifier.
	 *
	 * @param stereotypeIdentifier must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 */
	SortedSet<StereotypeGroup> getGroupsFor(String stereotypeIdentifier);

	/**
	 * Returns all {@link StereotypeGroup}s that contain the given {@link Stereotype}.
	 *
	 * @param stereotype must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	default SortedSet<StereotypeGroup> getGroupsFor(Stereotype stereotype) {
		return getGroupsFor(stereotype.getIdentifier());
	}

	/**
	 * Returns all {@link StereotypeGroup}s that contain the {@link Stereotype} referred to by the given
	 * {@link StereotypeDefinition}.
	 *
	 * @param definition must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	default SortedSet<StereotypeGroup> getGroupsFor(StereotypeDefinition definition) {
		return getGroupsFor(definition.getStereotype());
	}

	/**
	 * Returns all registered {@link StereotypeDefinition}s.
	 *
	 * @return will never be {@literal null}.
	 */
	SortedSet<? extends StereotypeDefinition> getDefinitions();

	/**
	 * Returns all {@link StereotypeGroup}s.
	 *
	 * @return will never be {@literal null}.
	 */
	SortedSet<StereotypeGroup> getGroups();

	/**
	 * Returns all {@link StereotypeDefinition}s for the given {@link StereotypeGroup}.
	 *
	 * @param group must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	SortedSet<StereotypeDefinition> getDefinitions(StereotypeGroup group);

	/**
	 * Returns all type-based {@link Stereotype}s selected by the given {@link StereotypeMatcher}.
	 *
	 * @param <T> the type abstraction
	 * @param <A> the annotation abstraction
	 * @param type must not be {@literal null}.
	 * @param filter must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	default <T, A> List<Stereotype> getTypeBasedStereotypes(T type, StereotypeMatcher<T, A> filter) {

		return getDefinitions().stream()
				.filter(it -> filter.hasImplementingStereotype(type, it))
				.map(StereotypeDefinition::getStereotype)
				.toList();
	}

	/**
	 * Returns all annotation based {@link Stereotype}s selected by the given {@link StereotypeMatcher}.
	 *
	 * @param <T> the type abstraction
	 * @param <A> the annotation abstraction
	 * @param type must not be {@literal null}.
	 * @param filter must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	default <T, A> List<Stereotype> getAnnotationBasedStereotypes(A type, StereotypeMatcher<T, A> filter) {

		return getDefinitions().stream()
				.filter(it -> filter.hasAnnotatedStereotype(type, it))
				.map(StereotypeDefinition::getStereotype)
				.toList();
	}

	/**
	 * Returns all {@link StereotypeDefinition}s keyed by their {@link StereotypeGroup}.
	 *
	 * @return will never be {@literal null}.
	 */
	default Map<StereotypeGroup, SortedSet<StereotypeDefinition>> asMap() {

		return getGroups().stream()
				.collect(Collectors.toMap(Function.identity(), this::getDefinitions));
	}
}
