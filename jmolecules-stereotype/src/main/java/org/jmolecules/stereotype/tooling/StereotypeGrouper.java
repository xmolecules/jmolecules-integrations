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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jmolecules.stereotype.api.Stereotype;
import org.jmolecules.stereotype.api.Stereotypes;
import org.jmolecules.stereotype.catalog.StereotypeDefinition;

/**
 * @author Oliver Drotbohm
 */
class StereotypeGrouper<T> {

	private Function<T, Stereotypes> stereotyper;
	private final List<StereotypeDefinition> definitions;
	private final boolean skipOthers;

	public StereotypeGrouper(Function<T, Stereotypes> stereotyper, List<StereotypeDefinition> definitions,
			boolean skipOthers) {
		this.stereotyper = stereotyper;
		this.definitions = definitions;
		this.skipOthers = skipOthers;
	}

	public StereotypeGrouped<T> foo(Collection<T> source) {

		var collect = source.stream()
				.flatMap(it -> {

					var stereotypes = stereotyper.apply(it);

					return stereotypes.stream()
							.filter(stereotype -> {

								return definitions.stream().anyMatch(definition -> definition.refersTo(stereotype));
							})
							.findFirst()
							.map(stereotype -> Map.entry(stereotype, it))
							.stream();

					// return stereotypes.stream().map(stereotype -> Map.entry(stereotype, it));

					// var stereotype = stereotypes.getPrimary();
					//
					// return stereotype == null ? Stream.empty() : Stream.of(Map.entry(stereotype, it));
				})
				.collect(Collectors.groupingBy(Entry::getKey, Collectors.mapping(Entry::getValue, Collectors.toList())));

		var stereotyped = collect.entrySet().stream()
				.filter(it -> definitions.stream().anyMatch(definition -> definition.refersTo(it.getKey())))
				.collect(
						Collectors.toMap(Entry::getKey, Entry::getValue, (l, r) -> r, TreeMap::new));

		if (!skipOthers) {
			var others = source.stream()
					.filter(it -> !stereotyped.entrySet().stream().map(Entry::getValue).anyMatch(foo -> foo.contains(it)))
					.toList();

			if (!others.isEmpty()) {
				stereotyped.put(OtherStereotype.INSTANCE, others);
			}
		}

		return new StereotypeGrouped(stereotyped);
	}

	static class OtherStereotype implements Stereotype {

		static final OtherStereotype INSTANCE = new OtherStereotype();

		@Override
		public String toDetailedString() {
			return "Other";
		}

		@Override
		public int getPriority() {
			return Integer.MAX_VALUE;
		}

		@Override
		public String getIdentifier() {
			return "org.jmolecules.misc.Other";
		}

		@Override
		public List<String> getGroups() {
			return Collections.emptyList();
		}

		@Override
		public String getDisplayName() {
			return "Other";
		}

		@Override
		public String toString() {
			return getDisplayName();
		}
	}
}
