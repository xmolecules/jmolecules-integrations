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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.With;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jmolecules.stereotype.api.StereotypeFactory;
import org.jmolecules.stereotype.catalog.StereotypeCatalog;
import org.jmolecules.stereotype.tooling.StructureProvider.GroupingStructureProvider;
import org.jmolecules.stereotype.tooling.StructureProvider.SimpleStructureProvider;

/**
 * An abstraction of a tree-like analysis starting from a representation of the application, extracted into a packages
 * abstraction, a types packages in turn, and a methods abstraction eventually. The concrete types for the abstractions
 * are defined by the {@link StereotypeFactory} provided and extraction logic configured via
 * {@link #withPackages(Function)}, {@link #withTypes(Function)} and {@link #withMethods(Function)}. The step into the
 * types level can be advanced using {@link #withGroupedTypes(Function)} that allows producing a grouped set of types
 * qualified by a custom grouping criteria.
 *
 * @author Oliver Drotbohm
 * @param A the application abstraction
 * @param P the package abstraction
 * @param T the type abstraction
 * @param M the method abstraction
 * @param C a custom grouping criterion
 */
public class ProjectTree<A, P, T, M, C> {

	private final StereotypeFactory<P, T, M> factory;
	private final StereotypeCatalog catalog;
	private final Extractor<A, P, T, M> extractor;
	private final TreeConfig config;
	private final NodeHandler<A, P, T, M, C> handler;
	private final List<StereotypeGrouper<T>> grouper;

	private StereotypeGrouper<M> methodGrouper;

	/**
	 * Creates a new {@link ProjectTree} for the given {@link StereotypeFactory}, {@link StereotypeCatalog},
	 * {@link NodeHandler}, {@link Extractor} and {@link TreeConfig}.
	 *
	 * @param factory must not be {@literal null}.
	 * @param catalog must not be {@literal null}.
	 * @param handler must not be {@literal null}.
	 * @param extractor must not be {@literal null}.
	 * @param config must not be {@literal null}.
	 */
	private ProjectTree(StereotypeFactory<P, T, M> factory, StereotypeCatalog catalog,
			NodeHandler<A, P, T, M, C> handler, TreeConfig config, Extractor<A, P, T, M> extractor) {

		this.factory = factory;
		this.catalog = catalog;
		this.extractor = extractor;
		this.grouper = new ArrayList<>();
		this.config = config;
		this.handler = handler;
	}

	/**
	 * Creates a new {@link ProjectTree} from the given {@link StereotypeFactory}, {@link StereotypeCatalog} and
	 * {@link NodeHandler}.
	 *
	 * @param factory must not be {@literal null}.
	 * @param catalog must not be {@literal null}.
	 * @param handler must not be {@literal null}.
	 */
	public ProjectTree(StereotypeFactory<P, T, M> factory, StereotypeCatalog catalog,
			NodeHandler<A, P, T, M, C> handler) {
		this(factory, catalog, handler, TreeConfig.defaults(), new Extractor<>());
	}

	/**
	 * Registers a {@link org.jmolecules.stereotype.tooling.StructureProvider.SimpleStructureProvider} to extract
	 * packages, types and methods from an application abstraction.
	 *
	 * @param structure must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public ProjectTree<A, P, T, M, C> withStructureProvider(SimpleStructureProvider<A, P, T, M> structure) {

		if (structure == null) {
			throw new IllegalArgumentException("SimpleStructureProvider must not be null!");
		}

		return withPackages(structure::extractPackages)
				.withTypes(structure::extractTypes)
				.withMethods(structure::extractMethods);

	}

	/**
	 * Registers a {@link org.jmolecules.stereotype.tooling.StructureProvider.GroupingStructureProvider} to extract
	 * packages, types and methods from an application abstraction grouping types to an additional nesting layer.
	 *
	 * @param structure must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public ProjectTree<A, P, T, M, C> withStructureProvider(GroupingStructureProvider<A, P, T, M, C> structure) {

		if (structure == null) {
			throw new IllegalArgumentException("GroupingStructureProvider must not be null!");
		}

		return withPackages(structure::extractPackages)
				.withGroupedTypes(structure::groupTypes)
				.withMethods(structure::extractMethods);
	}

	/**
	 * Registers the given {@link TreeConfig} which will affect the tree traversal.
	 *
	 * @param config must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public ProjectTree<A, P, T, M, C> withConfig(TreeConfig config) {
		return new ProjectTree<>(factory, catalog, handler, config, extractor);
	}

	/**
	 * Registers the given stereotype group identifiers as grouping targets. Multiple invocations of the method will cause
	 * additional layers of grouping.
	 *
	 * @param stereotypeGroupIds must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public ProjectTree<A, P, T, M, C> withGrouper(String... stereotypeGroupIds) {
		return withGrouper(List.of(stereotypeGroupIds));
	}

	/**
	 * Registers the given stereotype group identifiers as grouping targets. Multiple invocations of the method will cause
	 * additional layers of grouping.
	 *
	 * @param stereotypeGroupIds must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public ProjectTree<A, P, T, M, C> withGrouper(Collection<String> stereotypeGroupIds) {

		var definitions = stereotypeGroupIds.stream()
				.flatMap(it -> catalog.getGroups(it).stream())
				.flatMap(it -> catalog.getDefinitions(it).stream())
				.toList();

		this.grouper.add(new StereotypeGrouper<>(factory::fromType, definitions, catalog));
		this.methodGrouper = new StereotypeGrouper<>(factory::fromMethod, definitions, catalog);

		return this;
	}

	/**
	 * Processes the given application abstraction, building up a tree and invoking the configured {@link NodeHandler} for
	 * each of the discovered elements.
	 *
	 * @param application must not be {@literal null}.
	 */
	public void process(A application) {

		if (application == null) {
			throw new IllegalArgumentException("Application abstraction must not be null!");
		}

		if (!config.skipApplicationNode) {
			handler.handleApplication(application);
		}

		var packages = extractor.packages.apply(application);
		var skipPackagesNode = config.skipSinglePackageNode && packages.size() == 1;

		for (var pkg : packages) {

			if (!skipPackagesNode) {
				handler.handlePackage(pkg, NodeContext.of(pkg, packages));
			}

			extractor.types.group(pkg, (t) -> {
				render(t, grouper);
			});

			if (!skipPackagesNode) {
				handler.postGroup();
			}
		}

		if (!config.skipApplicationNode) {
			handler.postGroup();
		}
	}

	/**
	 * Configures the {@link ProjectTree} on how to extract packages from the fundamental application abstraction.
	 *
	 * @param <A> the application abstraction
	 * @param packagesExtractor must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	private ProjectTree<A, P, T, M, C> withPackages(Function<A, Collection<P>> packagesExtractor) {
		return withExtractor(extractor.withPackages(packagesExtractor));
	}

	/**
	 * Configures the {@link ProjectTree} on how to extract types from packages.
	 *
	 * @param typesExtractor must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	private ProjectTree<A, P, T, M, C> withTypes(Function<P, Collection<T>> typesExtractor) {

		return withExtractor(extractor.withTypes((pkg, downstream) -> {

			var types = typesExtractor.apply(pkg);
			downstream.accept(types);
		}));
	}

	private <G extends Grouped<C, T>> ProjectTree<A, P, T, M, C> withGroupedTypes(Function<P, G> typesExtractor) {

		return withExtractor(extractor.withTypes((pkg, downstream) -> {

			var grouped = typesExtractor.apply(pkg)
					.filtered(Predicate.not(Collection::isEmpty));

			for (var group : grouped) {

				var criteria = group.getKey();
				var types = group.getValue();

				handler.handleCustom(criteria, grouped.getContext(criteria));

				downstream.accept(types);

				ProjectTree.this.handler.postGroup();
			}
		}));
	}

	/**
	 * Configures the {@link ProjectTree} on how to extract methods from types.
	 *
	 * @param methodsExtractor must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	private ProjectTree<A, P, T, M, C> withMethods(Function<T, Collection<M>> methodsExtractor) {
		return withExtractor(extractor.withMethods(methodsExtractor));
	}

	private ProjectTree<A, P, T, M, C> withExtractor(Extractor<A, P, T, M> extractor) {
		return new ProjectTree<>(factory, catalog, handler, config, extractor);
	}

	private <X> void render(Collection<T> types, List<StereotypeGrouper<T>> remainingGroupers) {

		if (remainingGroupers.isEmpty()) {
			renderTypes(new StereotypeGrouped<>(types), remainingGroupers);
			return;
		}

		var grouper = remainingGroupers.get(0);
		var typeGroups = grouper.group(types, !config.showNonStereotypedTypes);

		if (remainingGroupers.size() == 1 && config.elevateMethodLevelStereotypes) {

			var methods = typeGroups.flatMapValues(extractor.methods).toList();
			var groupedMethods = methodGrouper.group(methods, true);

			renderMethods(groupedMethods, null);
		}

		renderTypes(typeGroups, remainingGroupers);
	}

	private void renderTypes(StereotypeGrouped<T> groups, List<StereotypeGrouper<T>> remainingGroupers) {

		for (var group : groups) {

			var types = group.getValue();

			// Skip empty groups
			if (config.omitSingleGroupingNodes && types.isEmpty()) {
				continue;
			}

			var stereotype = group.getKey();

			if (!groups.hasOnlyOtherStereotype()) {
				handler.handleStereotype(stereotype, groups.getContext(stereotype));
			}

			if (remainingGroupers.size() > 1) {

				render(types, remainingGroupers.subList(1, remainingGroupers.size()));

			} else {

				var typesComparator = handler.getTypeComparator();
				types = typesComparator == null ? types : types.stream().sorted(typesComparator).toList();

				for (var type : types) {

					handler.handleType(type, NodeContext.of(type, types));

					if (!config.elevateMethodLevelStereotypes) {
						var groupedMethods = extractMethods(type);
						renderMethods(groupedMethods, type);
					}

					handler.postGroup();
				}
			}

			if (!groups.hasOnlyOtherStereotype()) {
				handler.postGroup();
			}
		}
	}

	void renderMethods(StereotypeGrouped<M> grouped, T typeContext) {

		for (var group : grouped) {

			var stereotype = group.getKey();

			if (!grouped.hasOnlyOtherStereotype()) {
				handler.handleStereotype(stereotype, () -> typeContext != null);
			}

			var methods = group.getValue();

			for (var method : methods) {
				handler.handleMethod(method, MethodNodeContext.of(method, methods, typeContext));
			}

			if (!grouped.hasOnlyOtherStereotype()) {
				handler.postGroup();
			}
		}
	}

	private StereotypeGrouped<M> extractMethods(T type) {

		var methods = extractor.methods.apply(type);

		return methodGrouper.group(methods, true);
	}

	@Builder
	public static class TreeConfig {

		private static final @Default TreeConfig DEFAULT = TreeConfig.defaults();

		final boolean omitSingleGroupingNodes;
		final boolean showNonStereotypedTypes;
		final boolean elevateMethodLevelStereotypes;
		final boolean skipSinglePackageNode;
		final boolean skipApplicationNode;

		private TreeConfig(boolean omitSingleGroupingNodes, boolean showNonStereotypedTypes,
				boolean elevateMethodLevelStereotypes, boolean skipSinglePackageNode, boolean skipApplicationNode) {

			this.omitSingleGroupingNodes = omitSingleGroupingNodes;
			this.showNonStereotypedTypes = showNonStereotypedTypes;
			this.elevateMethodLevelStereotypes = elevateMethodLevelStereotypes;
			this.skipSinglePackageNode = skipSinglePackageNode;
			this.skipApplicationNode = skipApplicationNode;
		}

		public static TreeConfig defaults() {
			return new TreeConfig(false, true, true, false, false);
		}
	}

	/**
	 * Combines all extracting functions to move between the different abstraction levels.
	 *
	 * @author Oliver Drotbohm
	 */
	@With
	@AllArgsConstructor
	static class Extractor<A, P, T, M> {

		private final Function<A, Collection<P>> packages;
		private final GroupedExtractor<P, T> types;
		private final Function<T, Collection<M>> methods;

		private Extractor() {

			this.packages = __ -> Collections.emptySet();
			this.types = (pkg, downstream) -> {};
			this.methods = __ -> Collections.emptySet();
		}

		/**
		 * Custom wither to allow flipping the application type to something different.
		 *
		 * @param <N>
		 * @param extractor
		 * @return
		 */
		public <N> Extractor<N, P, T, M> withPackages(Function<N, Collection<P>> extractor) {
			return new Extractor<>(extractor, types, methods);
		}
	}

	interface GroupedExtractor<P, T> {
		void group(P pkg, Consumer<Collection<T>> downstream);
	}
}
