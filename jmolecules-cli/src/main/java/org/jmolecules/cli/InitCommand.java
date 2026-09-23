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
package org.jmolecules.cli;

import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

import org.jmolecules.cli.core.BuildSystem;
import org.jmolecules.cli.core.JMoleculesBomVersions;
import org.jmolecules.cli.core.MetadataCache;
import org.jmolecules.cli.core.ModulithMetadata;
import org.jmolecules.codegen.Dependency.Scope;
import org.jmolecules.codegen.ProjectConfiguration;
import org.jmolecules.codegen.ProjectContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * @author Oliver Drotbohm
 */
@Command(name = "init")
@RequiredArgsConstructor
public class InitCommand implements Callable<Void> {

	private static final Logger LOG = LoggerFactory.getLogger(InitCommand.class.getName());

	private final BuildSystem buildSystem;
	private final ProjectContext context;
	private final MetadataCache cache;

	@Option(names = { "--force", "-f" }, defaultValue = "false") //
	boolean force;

	@Option(names = { "--offline", "-o" }, defaultValue = "false",
			description = "Run Maven in offline mode and resolve the BOM version from the local repository.") //
	boolean offline;

	@Option(names = { "--version" },
			description = "jMolecules BOM version. Defaults to the latest release from Maven Central (or the local repo when --offline).") //
	String jMoleculesVersion;

	/*
	 * (non-Javadoc)
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public Void call() throws Exception {

		buildSystem.initialize();
		buildSystem.setOffline(offline);
		buildSystem.getClasspath();

		ensureJMoleculesBom();

		var configuration = context.getConfiguration();

		if (context.hasDependency("lombok")) {
			configuration = configuration.enableLombok();
		}

		if (!StringUtils.hasText(configuration.getBasePackage()) || force) {

			if (!force) {
				LOG.info("🍃 No base package configured. Detecting…");
			}

			var basePackage = context.getApplicationBasePackage();

			if (basePackage != null) {

				LOG.info("🍃 Configuring base package to '{}'.", basePackage);
				configuration.withBasePackage(context.getApplicationBasePackage());

			} else {

				LOG.warn("⚠️  Couldn't detect base package! Please manually configure `base-package` in jmolecules.config!");
			}
		}

		if (context.hasDependency("spring-modulith-core", Scope.TEST, Scope.RUNTIME, Scope.COMPILE)) {

			configuration = configuration.enableSpringModulith();
			initSpringModulith();

		} else {
			cache.writeInternal("modules", "{}");
		}

		if (context.hasDependency("spring-data-commons", Scope.COMPILE)) {
			configuration = configuration.enableSpringData();
		}

		LOG.info("⚗️  jMolecules configuration written to {}.", ProjectConfiguration.CONFIGURATION_FILE);

		configuration.write();

		return null;
	}

	private void ensureJMoleculesBom() {

		var ops = buildSystem.getScriptOperations();

		if (ops.declaresManagedDependency("org.jmolecules", "jmolecules-bom")) {
			LOG.info("⚗️  jMolecules BOM already declared locally; skipping BOM addition.");
			return;
		}

		if (buildSystem.hasManagedDependencyIn("org.jmolecules")) {
			LOG.info("⚗️  jMolecules already declared as a managed dependency; skipping BOM addition.");
			return;
		}

		var version = jMoleculesVersion != null
				? jMoleculesVersion
				: offline ? JMoleculesBomVersions.latestReleaseFromLocalRepository()
						: JMoleculesBomVersions.latestRelease();

		LOG.info("📦 Adding jMolecules BOM ({}) to dependencyManagement.", version);

		ops.addProperty("jmolecules.version", version);
		ops.addManagedDependency("org.jmolecules:jmolecules-bom:pom:${jmolecules.version}:import");
	}

	void initSpringModulith() throws Exception {

		var classpath = buildSystem.getRawClasspath();

		ProcessBuilder builder = new ProcessBuilder("java", "-cp", "target/classes:" + classpath,
				"org.springframework.modulith.core.util.ApplicationModulesExporter",
				context.getConfiguration().getBasePackage(),
				cache.resolveInternal(ModulithMetadata.MODULES_CACHE).toString());

		LOG.info("📦 Calculating application module metadata.");

		var process = builder.start();

		process.waitFor();
		process.exitValue();
	}
}
