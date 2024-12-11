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
package org.jmolecules.stereotype.tooling;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

public class Grouped<K, T> implements Iterable<Entry<K, Collection<T>>> {

	private final Map<K, Collection<T>> groups;

	public Grouped(Map<K, Collection<T>> groups) {
		this.groups = groups;
	}

	Grouped(Map<K, Collection<T>> groups, @Nullable Comparator<K> comparator) {

		this.groups = comparator == null ? groups : new TreeMap<>(comparator);

		if (comparator != null) {
			this.groups.putAll(groups);
		}
	}

	protected Set<K> getKeys() {
		return groups.keySet();
	}

	protected boolean hasOnlyOneEntry() {
		return groups.size() == 1;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Entry<K, Collection<T>>> iterator() {
		return groups.entrySet().iterator();
	}

	Grouped<K, T> filtered(Predicate<Collection<T>> predicate) {

		return new Grouped<>(groups.entrySet().stream()
				.filter(it -> predicate.test(it.getValue()))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (l, r) -> r, LinkedHashMap::new)), null);
	}

	NodeContext getContext(K key) {
		return NodeContext.of(key, groups.keySet());
	}

	<S> Stream<S> flatMapValues(Function<T, Collection<S>> extractor) {

		return groups.values().stream()
				.flatMap(Collection::stream)
				.flatMap(extractor.andThen(Collection::stream));
	}
}
