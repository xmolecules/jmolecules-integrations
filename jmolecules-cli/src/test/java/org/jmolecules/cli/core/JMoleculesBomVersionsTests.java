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

import static org.assertj.core.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Hits Maven Central; ensure the Domtrip-backed lookup actually retrieves a release version.
 *
 * @author Oliver Drotbohm
 */
class JMoleculesBomVersionsTests {

	@Test
	void fetchesLatestBomVersionFromMavenCentral() {

		var version = JMoleculesBomVersions.latestRelease();

		assertThat(version).as("jmolecules-bom <release> from Maven Central")
				.isNotBlank()
				.matches("\\d{4}\\.\\d+\\.\\d+");
	}

	@Test
	void readsBomVersionFromLocalRepository(@TempDir Path localRepo) throws Exception {

		var artifactDir = localRepo.resolve("org/jmolecules/jmolecules-bom");
		Files.createDirectories(artifactDir);
		Files.writeString(artifactDir.resolve("maven-metadata-central.xml"), """
				<?xml version="1.0" encoding="UTF-8"?>
				<metadata>
					<groupId>org.jmolecules</groupId>
					<artifactId>jmolecules-bom</artifactId>
					<versioning>
						<latest>2025.0.2</latest>
						<release>2025.0.2</release>
					</versioning>
				</metadata>
				""");

		assertThat(JMoleculesBomVersions.latestReleaseFromLocalRepository(localRepo)).isEqualTo("2025.0.2");
	}

	@Test
	void offlineLookupFailsWhenNoLocalMetadata(@TempDir Path localRepo) {

		assertThatThrownBy(() -> JMoleculesBomVersions.latestReleaseFromLocalRepository(localRepo))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("No local metadata for jmolecules-bom")
				.hasMessageContaining("--version");
	}
}
