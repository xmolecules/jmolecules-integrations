/*
 * Copyright 2025-2025 the original author or authors.
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
package org.jmolecules.stereotype.catalog.support;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jmolecules.stereotype.api.Stereotype;
import org.jmolecules.stereotype.catalog.StereotypeCatalog;
import org.jmolecules.stereotype.catalog.StereotypeDefinition;
import org.jmolecules.stereotype.catalog.StereotypeDefinition.Assignment;
import org.jmolecules.stereotype.catalog.StereotypeDefinitionRegistry;
import org.jmolecules.stereotype.catalog.StereotypeGroup;

/**
 * Base class for {@link StereotypeCatalog} implementations.
 *
 * @author Oliver Drotbohm
 */
public abstract class AbstractStereotypeCatalog implements StereotypeCatalog, StereotypeDefinitionRegistry {

	private final SortedSet<AugmentableStereotypeDefinition> definitions;
	private final SortedSet<StereotypeGroup> groups;

	/**
	 * Creates a new {@link AbstractStereotypeCatalog} for the given {@link CatalogSource}.
	 *
	 * @param source must not be {@literal null}.
	 */
	public AbstractStereotypeCatalog() {

		this.definitions = new TreeSet<>();
		this.groups = new TreeSet<>();
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.catalog.StereotypeCatalog#getDefinitions()
	 */
	@Override
	public SortedSet<StereotypeDefinition> getDefinitions() {

		return definitions.stream()
				.collect(Collectors.toCollection(TreeSet::new));
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.catalog.StereotypeCatalog#getDefinitions(org.jmolecules.stereotype.catalog.StereotypeGroup)
	 */
	@Override
	public SortedSet<StereotypeDefinition> getDefinitions(StereotypeGroup group) {

		return definitions.stream()
				.filter(it -> group.contains(it.getStereotype()))
				.collect(Collectors.toCollection(TreeSet::new));
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.catalog.StereotypeCatalog#getGroups()
	 */
	@Override
	public SortedSet<StereotypeGroup> getGroups() {
		return Collections.unmodifiableSortedSet(groups);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.catalog.StereotypesCatalog#getGroupsFor(java.lang.String)
	 */
	@Override
	public SortedSet<StereotypeGroup> getGroupsFor(String stereoTypeIdentifier) {

		return definitions.stream()
				.filter(it -> it.getStereotype().getIdentifier().equals(stereoTypeIdentifier))
				.flatMap(this::findGroup)
				.collect(Collectors.toCollection(TreeSet::new));
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.catalog.StereotypesCatalog#getGroupsFor(org.jmolecules.stereotype.Stereotype)
	 */
	@Override
	public SortedSet<StereotypeGroup> getGroupsFor(Stereotype stereotype) {

		return getGroups().stream()
				.filter(it -> it.contains(stereotype))
				.collect(Collectors.toCollection(TreeSet::new));
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<StereotypeDefinition> iterator() {
		return getDefinitions().iterator();
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.catalog.StereotypeCatalog#getOrRegister(org.jmolecules.stereotype.api.Stereotype, java.util.function.Supplier)
	 */
	@Override
	public StereotypeDefinition getOrRegister(Stereotype stereotype, Supplier<Assignment> assignment) {

		return definitions.stream()
				.filter(it -> it.refersTo(stereotype))
				.findFirst()
				.map(it -> it.verify(stereotype))
				.orElseGet(() -> add(DefaultStereotypeDefinition.of(stereotype, Set.of(assignment.get()))));
	}

	protected final AugmentableStereotypeDefinition add(AugmentableStereotypeDefinition definition) {

		return definitions.stream()
				.filter(it -> it.refersToSameStereotypeAs(definition))
				.findFirst()
				.map(it -> it.add(definition.getAssignments()))
				.orElseGet(() -> {
					this.definitions.add(definition);
					return definition;
				});
	}

	protected final void add(StereotypeGroup group) {
		this.groups.add(group);
	}

	private Stream<StereotypeGroup> findGroup(StereotypeDefinition definition) {

		return groups.stream()
				.filter(it -> it.contains(definition.getStereotype()));
	}
}
