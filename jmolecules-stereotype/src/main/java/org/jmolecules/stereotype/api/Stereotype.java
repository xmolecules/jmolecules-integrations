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
package org.jmolecules.stereotype.api;

import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Represents a stereotype detected in a codebase.
 *
 * @author Oliver Drotbohm
 * @see org.jmolecules.Stereotype
 */
public interface Stereotype extends Comparable<Stereotype> {

	public static final int DEFAULT_PRIORITY = 0;

	/**
	 * The identifier of the stereotype. Defaults to the fully qualified name of the annotated type or concrete annotation
	 * respectively.
	 *
	 * @return
	 */
	String getIdentifier();

	/**
	 * A human-readable name of the stereotype. Defaults to the simple name of the annotated type or concrete annotation
	 * respectively.
	 *
	 * @return
	 */
	String getDisplayName();

	/**
	 * The name of the group an identifier belongs to.
	 *
	 * @return must not be {@literal null}.
	 */
	List<String> getGroups();

	/**
	 * A numeric value to apply an ordering to the multiple stereotypes a particular target can be assigned to.
	 *
	 * @return defaults to 0.
	 */
	int getPriority();

	boolean isInherited();

	/**
	 * Returns a detailed {@link String} representation in contrast to a rather brief {@link #toString()}.
	 *
	 * @return will never be {@literal null}.
	 */
	String toDetailedString();

	/**
	 * Returns whether the current {@link Stereotype} has the same identifier as the given one.
	 *
	 * @param other must not be {@literal null}.
	 */
	default boolean hasSameIdentifierAs(Stereotype other) {

		if (other == null) {
			throw new IllegalArgumentException("Stereotype must not be null!");
		}

		return getIdentifier().equals(other.getIdentifier());
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	default int compareTo(@Nullable Stereotype that) {

		return Comparator
				.comparingInt(Stereotype::getPriority)
				.thenComparing(Stereotype::getIdentifier)
				.compare(this, that);
	}

	default boolean belongsToGroup(String identifier) {
		return getGroups().contains(identifier);
	}
}
