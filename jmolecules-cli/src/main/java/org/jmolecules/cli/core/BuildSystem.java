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

	void refresh();

	List<Dependency> getDependencies();

	URL[] getClasspath();

	String getRawClasspath();

	Path getClasses();

	Path getTestClasses();

	ScriptOperations getScriptOperations();

	public static BuildSystem of(ProjectFiles workspace, MetadataCache cache) {

		var path = workspace.resolve("pom.xml");

		return Files.exists(path) ? new MavenBuildSystem(workspace, cache) : null;
	}

	interface ScriptOperations {

		void addDependency(String coordinates);

		void addManagedDependency(String coordinates);
	}
}
