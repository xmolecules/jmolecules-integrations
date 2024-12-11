/*
 * Copyright 2025 the original author or authors.
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jmolecules.stereotype.catalog.StereotypeGroup.Type;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * @author Oliver Drotbohm
 */
public class StereotypeGroups implements Iterable<StereotypeGroup> {

	private final SortedSet<StereotypeGroup> groups;

	/**
	 * @param groups
	 */
	public StereotypeGroups(Set<StereotypeGroup> groups) {
		this.groups = new TreeSet<>(groups);
	}

	public StereotypeGroups and(StereotypeGroups groups) {

		var result = new HashSet<>(this.groups);
		result.addAll(groups.groups);

		return new StereotypeGroups(result);
	}

	public @Nullable StereotypeGroup getPrimary() {
		return streamPrioritized().findFirst().orElse(null);
	}

	public boolean isEmpty() {
		return groups.isEmpty();
	}

	public Stream<StereotypeGroup> stream() {
		return groups.stream();
	}

	public Stream<StereotypeGroup> streamPrioritized() {
		return groups.stream().sorted(StereotypeGroup.prioritized());
	}

	/**
	 * Returns a {@link Stream} of {@link StereotypeGroup} for the given {@link Type} ordered by their priority.
	 *
	 * @param type must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public Stream<StereotypeGroup> streamByType(Type type) {

		Assert.notNull(type, "Type must not be null!");

		return stream()
				.filter(it -> it.hasType(type))
				.sorted(StereotypeGroup.prioritized());
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<StereotypeGroup> iterator() {
		return groups.iterator();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return groups.toString();
	}

	public static Collector<StereotypeGroup, ?, StereotypeGroups> collector() {
		return Collectors.collectingAndThen(Collectors.toCollection(TreeSet::new), StereotypeGroups::new);
	}
}
