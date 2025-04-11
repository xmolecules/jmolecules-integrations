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
import java.util.stream.Collectors;

import org.jmolecules.stereotype.catalog.support.CatalogSource;
import org.jmolecules.stereotype.catalog.support.JsonPathStereotypeCatalog;
import org.jmolecules.stereotype.reflection.ReflectionStereotypeFactory;
import org.junit.jupiter.api.Test;

/**
 * @author Oliver Drotbohm
 */
public class ProjectTreeUnitTests {

	@Test
	void testName() {

		var source = CatalogSource.ofClassLoader(ProjectTreeUnitTests.class.getClassLoader());
		var catalog = new JsonPathStereotypeCatalog(source);
		var factory = new ReflectionStereotypeFactory(catalog);

		// System.out.println(catalog.asMap().toString());

		var writer = new StringBuilderLineWriter();

		AsciiArtNodeHandler<Package, Class<?>, Method> handler = new AsciiArtNodeHandler<>(writer, Package::getName,
				(s, g) -> s.getDisplayName() + " "
						+ g.stream().map(Object::toString).collect(Collectors.joining(", ", "(", ")")),
				Class::getName, Method::getName);

		var tree = new ProjectTree<>(factory, catalog)
				.withPackagesExtractor(this::getDirectlyNestedPackages)
				.withTypesExtractor(this::reflectionsClassExtractor)
				// .withGrouper("spring", "org.jmolecules.ddd")
				// .withGrouper("spring", "org.jmolecules.architecture")
				.withGrouper("org.jmolecules.architecture")
				.withGrouper("spring")
		// .withGrouper("org.jmolecules.ddd")
		;

		tree.processTree(MyController.class.getPackage(), handler);

		System.out.println(writer.toString());
	}

	private Collection<Class<?>> reflectionsClassExtractor(Package pkg) {

		try (var scanResult = new ClassGraph().acceptPackages(pkg.getName()).scan()) {
			return scanResult.getAllClasses().loadClasses();
		}
	}

	private Collection<Package> getDirectlyNestedPackages(Package pkg) {

		// return List.of(pkg);

		return reflectionsClassExtractor(pkg).stream()
				.map(Class::getPackage)
				.distinct()
				.toList();
	}
}
