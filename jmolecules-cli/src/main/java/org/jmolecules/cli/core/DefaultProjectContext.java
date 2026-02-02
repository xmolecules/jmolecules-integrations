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
package org.jmolecules.cli.core;

import java.util.Arrays;
import java.util.function.Supplier;

import org.jmolecules.codegen.Dependency.Scope;
import org.jmolecules.codegen.ProjectContext;
import org.jmolecules.codegen.ProjectFiles;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.SingletonSupplier;

/**
 * @author Oliver Drotbohm
 */
public class DefaultProjectContext implements ProjectContext {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultProjectContext.class.getName());

	private final ProjectFiles files;
	private final MetadataCache workspace;
	private final BuildSystem buildSystem;

	private final Supplier<@Nullable String> basePackage;

	public DefaultProjectContext(ProjectFiles files, MetadataCache workspace, BuildSystem buildSystem) throws Exception {

		this.files = files;
		this.workspace = workspace;
		this.buildSystem = buildSystem;
		this.basePackage = SingletonSupplier.of(this::detectBasePackage);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.codegen.ProjectContext#hasDependency(java.lang.String, org.jmolecules.codegen.Dependency.Scope[])
	 */
	@Override
	public boolean hasDependency(String artifact, Scope... scopes) {

		var parts = artifact.split(":");

		var groupId = parts.length > 1 ? parts[0] : null;
		var artifactId = parts.length > 1 ? parts[1] : parts[0];

		return buildSystem.getDependencies().stream()
				.filter(it -> groupId == null ? true : it.hasGroupId(groupId))
				.filter(it -> it.hasArtifactId(artifactId))
				.anyMatch(it -> scopes.length == 0 || Arrays.stream(scopes).anyMatch(it::hasScope));
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.codegen.ProjectContext#getApplicationBasePackage()
	 */
	@Override
	public String getApplicationBasePackage() {
		return basePackage.get();
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.codegen.ProjectContext#getPackageForModule(java.lang.String)
	 */
	@Override
	public String getPackageForModule(String name) {
		return new ModulithMetadata(workspace).getPackageFor(name);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.codegen.ProjectContext#getFiles()
	 */
	@Override
	public ProjectFiles getFiles() {
		return files;
	}

	@Nullable
	private String detectBasePackage() {

		var list = files.findSourceFileContaining("^@SpringBootApplication", true);

		if (list.isEmpty()) {

			LOG.warn("⚠️  No class annotated with @SpringBootApplication found!");
			return null;
		}

		if (list.size() > 1) {

			LOG.warn("⚠️  Unable to detect main class as multiple classes are annotated with @SpringBootApplication: {}",
					list);
			return null;
		}

		var mainApplicationClass = list.get(0);

		LOG.info("🍃 Found main application class {0}.", mainApplicationClass);

		return ClassUtils.getPackageName(mainApplicationClass);
	}
}
