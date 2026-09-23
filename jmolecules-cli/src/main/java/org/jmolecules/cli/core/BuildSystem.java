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
package org.jmolecules.cli.core;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jmolecules.codegen.Dependency;
import org.jmolecules.codegen.ProjectFiles;

/**
 * @author Oliver Drotbohm
 */
public interface BuildSystem {

	void initialize();

	/**
	 * Toggles offline mode for subsequent Maven invocations performed by this build system. When {@code true}, Maven
	 * is invoked with {@code -o} and will only resolve already-cached artifacts.
	 */
	void setOffline(boolean offline);

	void refresh();

	void compile();

	List<Dependency> getDependencies();

	URL[] getClasspath();

	String getRawClasspath();

	Path getClasses();

	Path getTestClasses();

	ScriptOperations getScriptOperations();

	/**
	 * Returns whether the effective view of the project's POM declares any managed dependency under the given group id
	 * &mdash; covering local declarations, parent inheritance, and BOM imports (which Maven flattens into their
	 * constituent managed entries during effective-pom resolution).
	 */
	boolean hasManagedDependencyIn(String groupId);

	public static BuildSystem of(ProjectFiles workspace, MetadataCache cache) {

		var path = workspace.resolve("pom.xml");

		return Files.exists(path) ? new MavenBuildSystem(workspace, cache) : null;
	}

	interface ScriptOperations {

		void addDependency(String coordinates);

		void addManagedDependency(String coordinates);

		/**
		 * Adds a property to {@code <project><properties>}. No-op if a property with that name is already present.
		 */
		void addProperty(String name, String value);

		/**
		 * Whether the local {@code pom.xml} directly declares a managed dependency for the given group and artifact id.
		 * Does not consider parent inheritance or BOM imports &mdash; a fast file-only check meant as a short-circuit
		 * before more expensive effective-pom lookups.
		 */
		boolean declaresManagedDependency(String groupId, String artifactId);
	}
}
