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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

/**
 * @author Oliver Drotbohm
 */
public class ProjectConfiguration {

	public static final String CONFIGURATION_FILE = "jmolecules.config";

	public static final String LOMBOK_ENABLED = "lombok.enabled";
	public static final String SPRING_DATA_ENABLED = "spring-data.enabled";
	public static final String SPRING_MODULITH_ENABLED = "spring-modulith.enabled";

	private final Properties properties;
	private final @Nullable Path config;

	public ProjectConfiguration(Function<String, Path> resolver) {

		this.properties = new Properties();
		this.config = resolver.apply(CONFIGURATION_FILE);

		if (Files.exists(config)) {
			try (var content = Files.newInputStream(config)) {
				this.properties.load(content);
			} catch (IOException o_O) {
				throw new UncheckedIOException(o_O);
			}
		}
	}

	ProjectConfiguration(Properties properties) {

		this.properties = properties;
		this.config = null;
	}

	public String getBasePackage() {
		return properties.getProperty("base-package");
	}

	public boolean isLombokEnabled() {
		return "true".equals(properties.getProperty(LOMBOK_ENABLED));
	}

	public boolean isSpringModulithEnabled() {
		return "true".equals(properties.getProperty(SPRING_MODULITH_ENABLED));
	}

	public boolean isSpringDataEnabled() {
		return "true".equals(properties.getProperty(SPRING_DATA_ENABLED));
	}

	public ProjectConfiguration enableLombok() {
		this.properties.put(LOMBOK_ENABLED, "true");
		return this;
	}

	public ProjectConfiguration enableSpringData() {
		this.properties.put(SPRING_DATA_ENABLED, "true");
		return this;
	}

	public ProjectConfiguration enableSpringModulith() {
		this.properties.put(SPRING_MODULITH_ENABLED, "true");
		return this;
	}

	public ProjectConfiguration withBasePackage(String basePackage) {
		this.properties.put("base-package", basePackage);
		return this;
	}

	public boolean supportsRecords() {
		return true;
	}

	public void write() throws Exception {

		if (config == null) {
			return;
		}

		try (var writer = Files.newBufferedWriter(config)) {
			this.properties.store(writer, " jMolecules configuration");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return properties.toString();
	}
}
