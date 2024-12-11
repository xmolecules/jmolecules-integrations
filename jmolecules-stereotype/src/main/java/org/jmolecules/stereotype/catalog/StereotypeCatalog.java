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

	StereotypeDefinition getDefinition(Stereotype stereotype);

	/**
	 * Returns all registered {@link StereotypeDefinition}s.
	 *
	 * @return will never be {@literal null}.
	 */
	SortedSet<? extends StereotypeDefinition> getDefinitions();

	/**
	 * Returns all {@link StereotypeDefinition}s for the given {@link StereotypeGroup}. TODO: Refactor to allow different
	 * lookups (direct, incl. nested)
	 *
	 * @param group must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	SortedSet<StereotypeDefinition> getDefinitions(StereotypeGroup group);

	/**
	 * Returns all {@link StereotypeGroup}s.
	 *
	 * @return will never be {@literal null}.
	 */
	StereotypeGroups getGroups();

	StereotypeGroups getGroups(String groupIdentifier);

	/**
	 * Returns all {@link StereotypeGroup}s that contain the {@link Stereotype} with the given identifier.
	 *
	 * @param stereotypeIdentifier must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 */
	StereotypeGroups getGroupsFor(String stereotypeIdentifier);

	/**
	 * Returns all {@link StereotypeGroup}s that contain the given {@link Stereotype}.
	 *
	 * @param stereotype must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	default StereotypeGroups getGroupsFor(Stereotype stereotype) {
		return getGroupsFor(stereotype.getIdentifier());
	}

	/**
	 * Returns all {@link StereotypeGroup}s that contain the {@link Stereotype} referred to by the given
	 * {@link StereotypeDefinition}.
	 *
	 * @param definition must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	default StereotypeGroups getGroupsFor(StereotypeDefinition definition) {
		return getGroupsFor(definition.getStereotype());
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
