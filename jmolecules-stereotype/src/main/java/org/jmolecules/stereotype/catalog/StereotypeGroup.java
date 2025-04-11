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
import java.util.stream.Collectors;

import org.jmolecules.stereotype.api.Stereotype;

/**
 * A group of {@link Stereotype}s.
 *
 * @author Oliver Drotbohm
 */
public class StereotypeGroup implements Comparable<StereotypeGroup> {

	private final List<String> identifiers;
	private final String displayName;

	/**
	 * Creates a new {@link StereotypeGroup} with the given identifiers and display name.
	 *
	 * @param identifiers must not be {@literal null}.
	 * @param displayName must not be {@literal null}.
	 */
	public StereotypeGroup(List<String> identifiers, String displayName) {

		this.identifiers = identifiers;
		this.displayName = displayName;
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
		return stereotype.getGroups().stream().anyMatch(identifiers::contains);
	}

	public boolean hasIdentifierOrParent(String identifier) {

		return identifiers.stream()
				.anyMatch(it -> it.equals(identifier) || it.startsWith(identifier + "."));
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
		return displayName + " (" + identifiers.stream().collect(Collectors.joining(",")) + ")";
	}
}
