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
package org.jmolecules.stereotype.catalog.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jmolecules.stereotype.api.Stereotype;
import org.jmolecules.stereotype.support.StringBasedStereotype;

/**
 * Default {@link StereotypeDefinition}.
 *
 * @author Oliver Drotbohm
 */
public class DefaultStereotypeDefinition implements AugmentableStereotypeDefinition {

	private final Stereotype stereotype;
	private final Set<Assignment> assignments;

	/**
	 * Creates a new {@link DefaultStereotypeDefinition} for the given {@link Stereotype} and {@link Assignment}.
	 *
	 * @param stereotype must not be {@literal null}.
	 * @param assignments must not be {@literal null}.
	 */
	private DefaultStereotypeDefinition(Stereotype stereotype, Set<Assignment> assignments) {

		if (stereotype == null) {
			throw new IllegalArgumentException("Stereotype must not be null!");
		}

		if (assignments == null) {
			throw new IllegalArgumentException("Assignments must not be null!");
		}

		this.stereotype = stereotype;
		this.assignments = assignments;
	}

	/**
	 * Creates a new {@link AugmentableStereotypeDefinition} for the given {@link Stereotype} and {@link Assignment}s.
	 *
	 * @param stereotype must not be {@literal null}.
	 * @param assignments must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */

	public static AugmentableStereotypeDefinition of(Stereotype stereotype, Set<Assignment> assignments) {
		return new DefaultStereotypeDefinition(stereotype, assignments);
	}

	public static Builder forIdentifier(String identifier) {
		return new Builder(identifier);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.catalog.StereotypeDefinition#getStereotype()
	 */
	@Override
	public Stereotype getStereotype() {
		return stereotype;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.catalog.StereotypeDefinition#getAssignment()
	 */
	@Override
	public Set<Assignment> getAssignments() {
		return assignments;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.catalog.support.AugmentableStereotypeDefinition#add(java.util.Collection)
	 */
	@Override
	public AugmentableStereotypeDefinition add(Collection<Assignment> assignment) {

		this.assignments.addAll(assignment);
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		return stereotype.toString() + " ("
				+ assignments.stream().map(Assignment::toString).collect(Collectors.joining(" or "))
				+ ", priority " + stereotype.getPriority() + ")";
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (obj == this) {
			return true;
		}

		if (!(obj instanceof DefaultStereotypeDefinition that)) {
			return false;
		}

		return Objects.equals(this.stereotype, that.stereotype)
				&& Objects.equals(this.assignments, that.assignments);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(stereotype, assignments);
	}

	public static class Builder {

		private final Set<Assignment> assignments;
		private final List<String> group;
		private final String identifier;
		private String displayName;
		private Integer priority;

		private Builder(String identifier) {
			this.identifier = identifier;
			this.assignments = new HashSet<>();
			this.group = new ArrayList<>();
		}

		public Builder addAssignment(Assignment assignment) {
			this.assignments.add(assignment);
			return this;
		}

		public Builder addGroup(String group) {
			this.group.add(group);
			return this;
		}

		public Builder andPriority(int priority) {
			this.priority = priority;
			return this;
		}

		public Builder andDisplayName(String displayName) {
			this.displayName = displayName;
			return this;
		}

		public AugmentableStereotypeDefinition build() {

			var stereotype = StringBasedStereotype.of(this.identifier);

			if (!group.isEmpty()) {
				stereotype = group.stream()
						.reduce(stereotype, (it, group) -> it.addGroup(group), (l, r) -> r);
			}

			if (priority != null) {
				stereotype = stereotype.withPriority(priority);
			}

			if (displayName != null) {
				stereotype = stereotype.withDisplayName(displayName);
			}

			return DefaultStereotypeDefinition.of(stereotype, assignments);
		}
	}
}
