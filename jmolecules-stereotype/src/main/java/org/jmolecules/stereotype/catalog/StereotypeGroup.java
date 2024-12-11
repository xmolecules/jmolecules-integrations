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

import java.net.URI;
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

	private static final Comparator<StereotypeGroup> PARENT_FIRST = (left, right) -> left.isChildOf(right) ? 1
			: right.isChildOf(left) ? -1 : 0;

	private final String identifier;
	private final String displayName;
	private final Type type;
	private final int priority;
	private final URI source;

	/**
	 * Creates a new {@link StereotypeGroup} with the given identifiers and display name.
	 *
	 * @param identifiers must not be {@literal null}.
	 * @param displayName must not be {@literal null}.
	 * @param type can be {@literal null}.
	 * @param priority can be {@literal null}.
	 */
	public StereotypeGroup(String identifier, String displayName, @Nullable Type type, @Nullable Integer priority,
			URI source) {

		Assert.notNull(identifier, "Identifier must not be null!");
		Assert.notNull(source, "Source must not be null!");

		this.identifier = identifier;
		this.displayName = displayName;
		this.priority = priority == null ? 0 : priority.intValue();
		this.type = type == null ? Type.TECHNOLOGY : type;
		this.source = source;
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
		return this.identifier.equals(identifier) || isChildOf(identifier);
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
		return displayName + " (" + identifier + ", " + type + ", " + priority + ", " + source + ")";
	}

	public boolean hasType(Type type) {
		return this.type == type;
	}

	int getPriority() {
		return priority;
	}

	private boolean isChildOf(String identifier) {
		return this.identifier.startsWith(identifier + ".");
	}

	private boolean isChildOf(StereotypeGroup group) {
		return isChildOf(group.identifier);
	}

	static Comparator<StereotypeGroup> prioritized() {

		return Comparator.comparing((StereotypeGroup it) -> it.type)
				.thenComparing(PARENT_FIRST)
				.thenComparing(it -> it.priority)
				.thenComparing(StereotypeGroup::getDisplayName);
	}

	public enum Type {
		ARCHITECTURE, DESIGN, TECHNOLOGY;
	}
}
