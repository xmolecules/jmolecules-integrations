/*
 * Copyright 2024 the original author or authors.
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
package org.jmolecules.codegen;

import java.nio.file.Path;

import org.jmolecules.codegen.Dependency.Scope;
import org.jspecify.annotations.Nullable;

/**
 * Contextual information about the project. You might wanna consider extending {@link AbstractProjectContext} for a
 * default {@link ProjectConfiguration} instance creation based on the {@link #getProjectRoot()}.
 *
 * @author Oliver Drotbohm
 * @see AbstractProjectContext
 */
public interface ProjectContext {

	/**
	 * Returns whether the project depends on an artifact with the given GAV expression (fully-qualified
	 * {@code $groupId:$artifactId:$version} or simple {@code $artifactId}).
	 *
	 * @param expression must not be {@literal null} empty.
	 * @param scope the {@link org.jmolecules.codegen.Dependency.Scope} the dependency is expected to be in.
	 *          Or-concatenated if multiple values given. Disregard if none given.
	 */
	boolean hasDependency(String expression, Scope... scope);

	/**
	 * Returns the application's base package.
	 *
	 * @return must not be {@literal null}.
	 */
	String getApplicationBasePackage();

	/**
	 * Returns the package name the module with the given identifier.
	 *
	 * @param name will never be {@literal null} or empty.
	 * @return can be {@literal null}.
	 */
	@Nullable
	String getPackageForModule(String name);

	/**
	 * Returns the {@link Path} to the project root.
	 *
	 * @return must not be {@literal null}.
	 */
	default Path getProjectRoot() {
		return getFiles().getProjectRoot();
	}

	/**
	 * Returns the project configuration.
	 *
	 * @return must not be {@literal null}.
	 */
	default ProjectConfiguration getConfiguration() {
		return getFiles().getConfiguration();
	}

	/**
	 * Returns the {@link ProjectFiles}.
	 *
	 * @return must not be {@literal null}.
	 * @see ProjectFiles#ProjectFiles(String)
	 */
	ProjectFiles getFiles();
}
