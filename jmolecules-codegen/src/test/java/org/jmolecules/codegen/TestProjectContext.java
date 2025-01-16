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
package org.jmolecules.codegen;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.With;

import java.nio.file.Path;
import java.util.Properties;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import org.jmolecules.codegen.Dependency.Scope;

/**
 * @author Oliver Drotbohm
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TestProjectContext implements ProjectContext {

	private final Properties properties;
	private final String basePackage;
	private final ProjectFiles files;
	private final @With BiPredicate<String, Scope> dependencyCheck;

	public TestProjectContext() {

		this.properties = new Properties();
		this.basePackage = "example";
		this.files = new ProjectFiles("integration-test");
		this.dependencyCheck = (artifact, scope) -> false;
	}

	public TestProjectContext withProjectRoot(Path projectRoot) {
		return new TestProjectContext(properties, basePackage, new ProjectFiles(projectRoot), dependencyCheck);
	}

	public TestProjectContext addProperty(String name, Object value) {

		var newProperties = new Properties(properties);

		if (value == null) {
			newProperties.remove(name);
		} else {
			newProperties.put(name, value.toString());
		}

		return new TestProjectContext(newProperties, basePackage, files, dependencyCheck);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.codegen.ProjectContext#hasDependency(java.lang.String, org.jmolecules.codegen.Dependency.Scope[])
	 */
	@Override
	public boolean hasDependency(String artifact, Scope... scope) {
		return Stream.of(scope).anyMatch(it -> dependencyCheck.test(artifact, it));
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.codegen.ProjectContext#getApplicationBasePackage()
	 */
	@Override
	public String getApplicationBasePackage() {
		return "example";
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.codegen.ProjectContext#getPackageForModule(java.lang.String)
	 */
	@Override
	public String getPackageForModule(String name) {
		return getApplicationBasePackage() + "." + name;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.codegen.ProjectContext#getConfiguration()
	 */
	@Override
	public ProjectConfiguration getConfiguration() {
		return new ProjectConfiguration(properties);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.codegen.ProjectContext#getFiles()
	 */
	@Override
	public ProjectFiles getFiles() {
		return files;
	}
}
