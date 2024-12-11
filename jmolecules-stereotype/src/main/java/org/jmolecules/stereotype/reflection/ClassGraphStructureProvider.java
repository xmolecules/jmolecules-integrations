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
package org.jmolecules.stereotype.reflection;

import io.github.classgraph.ClassGraph;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import org.jmolecules.stereotype.tooling.StructureProvider;

/**
 * @author Oliver Drotbohm
 */
@RequiredArgsConstructor
public class ClassGraphStructureProvider
		implements StructureProvider.SimpleStructureProvider<Package, Package, Class<?>, Method> {

	@Override
	public Collection<Package> extractPackages(Package pkg) {

		try (var scanResult = new ClassGraph()
				.enableAllInfo()
				.rejectClasspathElementsContainingResourcePath("target/test-classes")
				.enableRealtimeLogging()
				.acceptPackages(pkg.getName())
				.scan()) {

			return scanResult.getAllClasses().loadClasses().stream()
					.map(Class::getPackage)
					.distinct()
					.toList();
		}
	}

	@Override
	public Collection<Method> extractMethods(Class<?> type) {
		return List.of(type.getDeclaredMethods());
	}

	@Override
	public Collection<Class<?>> extractTypes(Package pkg) {

		try (var scanResult = new ClassGraph().acceptPackages(pkg.getName()).scan()) {

			return scanResult.getAllClasses().loadClasses().stream()
					.filter(it -> it.getPackage().equals(pkg))
					.toList();
		}
	}
}
