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

import example.MyController;
import io.github.classgraph.ClassGraph;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import org.jmolecules.stereotype.catalog.support.CatalogSource;
import org.jmolecules.stereotype.catalog.support.JsonPathStereotypeCatalog;
import org.jmolecules.stereotype.reflection.ReflectionStereotypeFactory;
import org.jmolecules.stereotype.tooling.StructureProvider.SimpleStructureProvider;
import org.junit.jupiter.api.Test;

/**
 * @author Oliver Drotbohm
 */
class ProjectTreeUnitTests {

	@Test
	void testName() {

		var source = CatalogSource.ofClassLoader(ProjectTreeUnitTests.class.getClassLoader());
		var catalog = new JsonPathStereotypeCatalog(source);
		var factory = new ReflectionStereotypeFactory(catalog);

		var labels = SimpleLabelProvider.forPackage(Package::getName, Class::getName,
				(Method m, Class<?> __) -> m.getName(), Object::toString);

		var handler = new AsciiArtNodeHandler<>(labels);

		var tree = new ProjectTree<>(factory, catalog, handler)
				.withStructureProvider(new SimpleStructureProvider<Package, Package, Class<?>, Method>() {

					@Override
					public Collection<Package> extractPackages(Package pkg) {

						return List.of(pkg);

						// return extractTypes(pkg).stream()
						// .map(Class::getPackage)
						// .distinct()
						// .toList();
					}

					@Override
					public Collection<Method> extractMethods(Class<?> type) {
						return List.of(type.getDeclaredMethods());
					}

					@Override
					public Collection<Class<?>> extractTypes(Package pkg) {

						try (var scanResult = new ClassGraph().acceptPackages(pkg.getName()).scan()) {
							return scanResult.getAllClasses().loadClasses();
						}
					}
				})
				// .withGrouper("spring", "org.jmolecules.ddd")
				.withGrouper("spring", "org.jmolecules.architecture")
		// .withGrouper("org.jmolecules.architecture")
		// .withGrouper("spring")
		// .withGrouper("org.jmolecules.ddd")
		;

		tree.process(MyController.class.getPackage());

		System.out.println(handler.getWriter().toString());
	}
}
