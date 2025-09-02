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

import java.util.Comparator;

import org.jmolecules.stereotype.api.Stereotype;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * A group of {@link Stereotype}s.
 *
 * @author Oliver Drotbohm
 */
public class StereotypeGroup implements Comparable<StereotypeGroup> {

	private final String identifier;
	private final String displayName;
	private final Type type;
	private final int priority;

	/**
	 * Creates a new {@link StereotypeGroup} with the given identifiers and display name.
	 *
	 * @param identifiers must not be {@literal null}.
	 * @param displayName must not be {@literal null}.
	 * @param type can be {@literal null}.
	 * @param priority can be {@literal null}.
	 */
	public StereotypeGroup(String identifier, String displayName, @Nullable Type type, @Nullable Integer priority) {

		Assert.notNull(identifier, "Identifier must not be null!");

		this.identifier = identifier;
		this.displayName = displayName;
		this.priority = priority == null ? Integer.MAX_VALUE : priority.intValue();
		this.type = type == null ? Type.DESIGN : type;
	}

	public String getIdentifier() {
		return identifier;
	}

	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Returns whether the group contains the given {@link Stereotype}.
	 *
	 * @param stereotype must not be {@literal null}.
	 */
	public boolean contains(Stereotype stereotype) {
		return stereotype.getGroups().stream().anyMatch(identifier::equals);
	}

	public boolean hasIdentifierOrParent(String identifier) {

		return this.identifier.equals(identifier)
				|| this.identifier.startsWith(identifier + ".");
	}

	public boolean hasType(Type type) {
		return this.type == type;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(StereotypeGroup that) {
		return this.displayName.compareTo(that.displayName);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return displayName + " (" + identifier + ")";
	}

	static Comparator<StereotypeGroup> prioritized() {

		return Comparator.comparing((StereotypeGroup it) -> it.type)
				.thenComparing(it -> it.priority)
				.thenComparing(StereotypeGroup::getDisplayName);
	}

	public enum Type {
		ARCHITECTURE, DESIGN, TECHNOLOGY;
	}
}
