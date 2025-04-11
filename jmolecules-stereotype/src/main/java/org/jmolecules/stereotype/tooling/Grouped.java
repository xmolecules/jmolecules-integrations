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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Grouped<K, T> implements Iterable<Entry<K, Collection<T>>> {

	private final Map<K, Collection<T>> groups;

	/**
	 * @param groups
	 */
	public Grouped(Map<K, Collection<T>> groups) {
		this.groups = groups;
	}

	protected Set<K> getKeys() {
		return groups.keySet();
	}

	public boolean hasOnlyOneEntry() {
		return groups.size() == 1;
	}

	public boolean isLastEntry(K key) {

		var keys = groups.keySet();
		var iterator = keys.iterator();
		K result = null;

		while (iterator.hasNext()) {
			result = iterator.next();
		}

		return result == key;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Entry<K, Collection<T>>> iterator() {
		return groups.entrySet().iterator();
	}

	boolean isEmpty() {
		return groups.isEmpty();
	}
}
