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
package org.jmolecules.bytebuddy;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;
import java.util.stream.Stream;

import org.jmolecules.bytebuddy.JMoleculesPlugin.JMoleculesConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Tests for {@link JMoleculesConfiguration}.
 *
 * @author Oliver Drotbohm
 */
class JMoleculesConfigurationTests {

	@Test // GH-323
	void detectsConfigurationInRootFolder() {

		File file = getFolder("config/direct");

		assertThat(new JMoleculesConfiguration(file).getProperty("in")).isEqualTo("direct");
	}

	@Test // GH-323
	void stopsTraversingAtNonBuildFolder() {

		File file = getFolder("config/none");

		assertThat(new JMoleculesConfiguration(file).getProperty("in")).isNull();
	}

	@Test // GH-323
	void detectsConfigInParentBuildFolder() {

		File file = getFolder("config/intermediate/nested");

		assertThat(new JMoleculesConfiguration(file).getProperty("intermediate")).isNull();
	}

	@Test // GH-373
	void disablesPersistenceCodeGenerationIfConfigured() {

		Properties properties = new Properties();
		properties.put("bytebuddy.persistence", "none");

		JMoleculesConfiguration configuration = new JMoleculesConfiguration(properties);

		Stream.of("jdbc", "jpa", "mongodb")
				.forEach(it -> {
					assertThat(configuration.supportsPersistence(it)).isFalse();
				});
	}

	private static File getFolder(String name) {

		try {
			return new ClassPathResource(name, JMoleculesConfigurationTests.class).getFile();
		} catch (IOException o_O) {
			throw new UncheckedIOException(o_O);
		}
	}
}
