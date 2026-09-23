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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;

/**
 * Discovers the latest released version of {@code org.jmolecules:jmolecules-bom} by reading its
 * {@code maven-metadata.xml} from Maven Central.
 *
 * @author Oliver Drotbohm
 */
public final class JMoleculesBomVersions {

	private static final URI METADATA = URI
			.create("https://repo1.maven.org/maven2/org/jmolecules/jmolecules-bom/maven-metadata.xml");

	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	/**
	 * Returns the value of {@code /metadata/versioning/release} for {@code org.jmolecules:jmolecules-bom}.
	 *
	 * @throws RuntimeException if the lookup fails (network error, non-200 response, malformed metadata).
	 */
	public static String latestRelease() {

		try {

			var client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
			var request = HttpRequest.newBuilder(METADATA).timeout(TIMEOUT).GET().build();
			var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

			if (response.statusCode() != 200) {
				throw new IllegalStateException("HTTP " + response.statusCode() + " for " + METADATA);
			}

			var editor = new Editor(Document.of(response.body()));

			return readRelease(editor, METADATA.toString());

		} catch (Exception e) {
			throw new RuntimeException(
					"Could not fetch latest jmolecules-bom version from Maven Central; pass --version <x.y.z> to override.",
					e);
		}
	}

	/**
	 * Reads {@code <release>} from the locally cached {@code maven-metadata-central.xml} for
	 * {@code org.jmolecules:jmolecules-bom}. Honors the {@code maven.repo.local} system property and falls back to
	 * {@code ~/.m2/repository}.
	 *
	 * @throws RuntimeException if no local metadata is present.
	 */
	public static String latestReleaseFromLocalRepository() {
		return latestReleaseFromLocalRepository(defaultLocalRepository());
	}

	static String latestReleaseFromLocalRepository(Path localRepository) {

		var artifactDir = localRepository.resolve("org/jmolecules/jmolecules-bom");
		var metadata = artifactDir.resolve("maven-metadata-central.xml");

		if (!Files.exists(metadata)) {
			metadata = artifactDir.resolve("maven-metadata.xml");
		}

		if (!Files.exists(metadata)) {
			throw new RuntimeException("No local metadata for jmolecules-bom found under " + artifactDir
					+ "; resolve the BOM at least once before running --offline, or pass --version <x.y.z>.");
		}

		try {

			var editor = new Editor(Document.of(metadata));

			return readRelease(editor, metadata.toString());

		} catch (Exception e) {
			throw new RuntimeException(
					"Could not read local jmolecules-bom metadata at " + metadata + "; pass --version <x.y.z> to override.",
					e);
		}
	}

	private static String readRelease(Editor editor, String source) {

		var release = editor.root()
				.path("versioning", "release")
				.map(Element::textContent)
				.orElse(null);

		if (release == null || release.isBlank()) {
			throw new IllegalStateException("No <release> in metadata at " + source);
		}

		return release.trim();
	}

	private static Path defaultLocalRepository() {

		var override = System.getProperty("maven.repo.local");

		return override != null
				? Paths.get(override)
				: Paths.get(System.getProperty("user.home"), ".m2", "repository");
	}

	private JMoleculesBomVersions() {}
}
