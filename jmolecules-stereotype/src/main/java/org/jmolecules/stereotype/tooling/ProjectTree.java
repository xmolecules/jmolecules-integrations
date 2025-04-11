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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.jmolecules.stereotype.api.StereotypeFactory;
import org.jmolecules.stereotype.catalog.StereotypeCatalog;

/**
 * @author Oliver Drotbohm
 */
public class ProjectTree<P, T, M> {

	private final StereotypeFactory<P, T, M> factory;
	private final StereotypeCatalog catalog;
	private final TreeConfiguration configuration;

	private List<StereotypeGrouper<T>> grouper;
	private StereotypeGrouper<M> methodGrouper;

	ProjectTree(StereotypeFactory<P, T, M> factory, StereotypeCatalog catalog,
			TreeConfiguration configuration) {

		this.factory = factory;
		this.catalog = catalog;
		this.configuration = configuration;
		this.grouper = new ArrayList<>();

	}

	public ProjectTree(StereotypeFactory<P, T, M> factory, StereotypeCatalog catalog) {

		this.factory = factory;
		this.catalog = catalog;
		this.configuration = new TreeConfiguration();
		this.grouper = new ArrayList<>();
	}

	public ProjectTree<P, T, M> withPackagesExtractor(Function<P, Collection<P>> extractor) {
		return new ProjectTree<>(factory, catalog,
				new TreeConfiguration(extractor, configuration.groupingTypesExtractor, configuration.methodsExtractor));
	}

	public ProjectTree<P, T, M> withTypesExtractor(Function<P, Collection<T>> extractor) {

		var groupedExtractor = new GroupedExtractor<P, T>() {

			@Override
			public NestingLevel foo(P pkg, NestingLevel level, BiConsumer<Collection<T>, NestingLevel> downstream) {

				var types = extractor.apply(pkg);
				var next = level.increase();

				downstream.accept(types, next);

				return next;
			}
		};

		return new ProjectTree<>(factory, catalog,
				new TreeConfiguration(configuration.packagesExtractor, groupedExtractor, configuration.methodsExtractor));
	}

	public ProjectTree<P, T, M> withMethodExtractor(Function<T, Collection<M>> extractor) {
		return new ProjectTree<>(factory, catalog,
				new TreeConfiguration(configuration.packagesExtractor, configuration.groupingTypesExtractor, extractor));
	}

	public <X, G extends Grouped<X, T>> ProjectTree<P, T, M> withNodeGrouper(Function<P, G> extractor,
			GroupedHandler<X, T, G> handler) {

		var grouper = new GroupedExtractor<P, T>() {

			@Override
			public NestingLevel foo(P pkg, NestingLevel level, BiConsumer<Collection<T>, NestingLevel> downstream) {

				var grouped = extractor.apply(pkg);
				var next = level.increase();

				for (var group : grouped) {

					var nested = handler.handle(group.getKey(), grouped, next);

					downstream.accept(group.getValue(), nested);
				}

				return next;
			}
		};

		return new ProjectTree<>(factory, catalog,
				new TreeConfiguration(configuration.packagesExtractor, grouper, configuration.methodsExtractor));
	}

	public ProjectTree<P, T, M> withGrouper(String... stereotypeGroupIds) {

		var definitions = Arrays.stream(stereotypeGroupIds)
				.flatMap(it -> catalog.getGroups(it).stream())
				.flatMap(it -> catalog.getDefinitions(it).stream())
				.toList();

		this.grouper.add(new StereotypeGrouper<>(factory::fromType, definitions, false));
		this.methodGrouper = new StereotypeGrouper<>(factory::fromMethod, definitions, true);

		return this;
	}

	public <X> void processTree(P pkg, NodeHandler<P, T, M> handler) {

		var packages = configuration.packagesExtractor.apply(pkg);
		var level = new NestingLevel(0);

		for (var element : packages) {

			handler.handlePackage(element, level);

			configuration.groupingTypesExtractor.foo(element, level, (t, l) -> {
				render(handler, l, t, grouper);
			});
		}
	}

	private <X> void render(NodeHandler<P, T, M> handler, NestingLevel level,
			Collection<T> types, List<StereotypeGrouper<T>> remainingGroupers) {

		var grouper = remainingGroupers.get(0);
		var groups = grouper.foo(types);

		for (var entry : groups) {

			// Skip empty groups
			if (entry.getValue().isEmpty()) {
				continue;
			}

			var stereotype = entry.getKey();
			var stereotypeGroups = catalog.getGroupsFor(stereotype);

			var nextLevel = handler.handleStereotypeGroup(entry.getKey(), stereotypeGroups, groups, level);

			if (remainingGroupers.size() > 1) {
				render(handler, nextLevel, entry.getValue(), remainingGroupers.subList(1, remainingGroupers.size()));

			} else {

				for (var type : entry.getValue()) {

					var postTypeLevel = handler.handleType(type, nextLevel);
					var methods = configuration.methodsExtractor.apply(type);
					var groupedMethods = methodGrouper.foo(methods);

					for (var foo : groupedMethods) {

						var key = foo.getKey();

						var postMethodStereotypesLevel = handler.handleStereotypeGroup(key, catalog.getGroupsFor(key),
								groupedMethods, postTypeLevel);

						for (var method : foo.getValue()) {
							handler.handleMethod(method, groupedMethods, postMethodStereotypesLevel);
						}
					}
				}
			}

			if (!groups.isLastEntry(entry.getKey())) {
				handler.postStereotypeGroup(groups);
			}
		}
	}

	class TreeConfiguration {

		private final Function<P, Collection<P>> packagesExtractor;
		private final GroupedExtractor<P, T> groupingTypesExtractor;
		private final Function<T, Collection<M>> methodsExtractor;

		/**
		 *
		 */
		public TreeConfiguration() {
			this.packagesExtractor = __ -> Collections.emptySet();
			this.groupingTypesExtractor = (pkg, level, downstream) -> level;
			this.methodsExtractor = __ -> Collections.emptySet();
		}

		public TreeConfiguration(Function<P, Collection<P>> packagesExtractor,
				GroupedExtractor<P, T> groupingTypesExtractor,
				Function<T, Collection<M>> methodsExtractor) {

			this.packagesExtractor = packagesExtractor;
			this.groupingTypesExtractor = groupingTypesExtractor;
			this.methodsExtractor = methodsExtractor;
		}
	}

	interface GroupedExtractor<P, T> {

		NestingLevel foo(P pkg, NestingLevel level, BiConsumer<Collection<T>, NestingLevel> downstream);
	}
}
