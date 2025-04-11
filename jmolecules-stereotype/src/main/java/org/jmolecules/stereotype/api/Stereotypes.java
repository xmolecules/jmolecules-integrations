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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

/**
 * A set of {@link Stereotype}s.
 *
 * @author Oliver Drotbohm
 */
public class Stereotypes implements Iterable<Stereotype> {

	public static final Stereotypes NONE = new Stereotypes(Collections.emptySet());

	private final SortedSet<Stereotype> stereotypes;

	/**
	 * Creates a new {@link Stereotypes} for the given {@link Stereotype}s.
	 *
	 * @param stereotypes must not be {@literal null}.
	 */
	public Stereotypes(Collection<Stereotype> stereotypes) {

		if (stereotypes == null) {
			throw new IllegalArgumentException("Stereotypes must not be null!");
		}

		this.stereotypes = new TreeSet<>(stereotypes);
	}

	/**
	 * Returns the primary {@link Stereotype}, i.e. the one with the highest priority.
	 *
	 * @return can be {@literal null}.
	 */
	public @Nullable Stereotype getPrimary() {
		return stereotypes.isEmpty() ? null : stereotypes.iterator().next();
	}

	/**
	 * Returns all {@link Stereotype}s as {@link Stream}.
	 *
	 * @return will never be {@literal null}.
	 */
	public Stream<Stereotype> stream() {
		return stereotypes.stream();
	}

	/**
	 * Creates a new {@link Stereotypes} instance combining the current one with the given ones.
	 *
	 * @param other must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public Stereotypes and(Stereotypes other) {

		if (other == null) {
			throw new IllegalArgumentException("Stereotypes must not be null!");
		}

		var result = new ArrayList<>(this.stereotypes);

		for (Stereotype candidate : other) {
			if (!result.contains(candidate)) {
				result.add(candidate);
			}
		}

		return new Stereotypes(result);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Stereotype> iterator() {
		return stereotypes.iterator();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return stereotypes.toString();
	}
}
