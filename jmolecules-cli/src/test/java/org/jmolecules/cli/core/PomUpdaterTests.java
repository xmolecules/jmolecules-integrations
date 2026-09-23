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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import org.jmolecules.codegen.ProjectFiles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;

import eu.maveniverse.domtrip.maven.Coordinates;

class PomUpdaterTests {

	@TempDir Path workspace;

	private PomUpdater updater;
	private Path pom;

	@BeforeEach
	void setUp() throws IOException {

		pom = workspace.resolve("pom.xml");
		Files.copy(getClass().getResourceAsStream("/sample-pom.xml"), pom);
		updater = new PomUpdater(new ProjectFiles(workspace.toString()));
	}

	@Test
	void addsDependency() throws Exception {

		updater.addDependency("org.example:lib:jar:1.0:compile");

		var doc = parse();
		var dep = "/project/dependencies/dependency";
		assertThat(xpath(doc, dep + "/groupId/text()")).isEqualTo("org.example");
		assertThat(xpath(doc, dep + "/artifactId/text()")).isEqualTo("lib");
		assertThat(xpath(doc, dep + "/version/text()")).isEqualTo("1.0");
	}

	@Test
	void addsManagedDependencyAsBomImport() throws Exception {

		updater.addManagedDependency("org.example:bom:pom:2.0:import");

		var doc = parse();
		var dep = "/project/dependencyManagement/dependencies/dependency";
		assertThat(xpath(doc, dep + "/groupId/text()")).isEqualTo("org.example");
		assertThat(xpath(doc, dep + "/artifactId/text()")).isEqualTo("bom");
		assertThat(xpath(doc, dep + "/version/text()")).isEqualTo("2.0");
		assertThat(xpath(doc, dep + "/scope/text()")).isEqualTo("import");
		assertThat(xpath(doc, dep + "/type/text()")).isEqualTo("pom");
	}

	@Test
	void preservesCommentsThroughEdits() throws Exception {

		updater.addDependency("org.example:lib:jar:1.0:compile");
		updater.addManagedDependency("org.example:bom:pom:2.0:import");

		assertThat(Files.readString(pom)).contains("<!-- preserve me -->");
	}

	@Test
	void addsPropertyToProperties() throws Exception {

		updater.addProperty("jmolecules.version", "1.10.0");

		var doc = parse();
		assertThat(xpath(doc, "/project/properties/jmolecules.version/text()")).isEqualTo("1.10.0");
	}

	@Test
	void addPropertyIsIdempotent() throws Exception {

		updater.addProperty("jmolecules.version", "1.10.0");
		updater.addProperty("jmolecules.version", "9.9.9"); // ignored, already present

		var doc = parse();
		assertThat(xpath(doc, "count(/project/properties/jmolecules.version)")).isEqualTo("1");
		assertThat(xpath(doc, "/project/properties/jmolecules.version/text()")).isEqualTo("1.10.0");
	}

	@Test
	void declaresManagedDependencyReportsLocalDeclaration() throws Exception {

		assertThat(updater.declaresManagedDependency("org.example", "bom")).isFalse();

		updater.addManagedDependency("org.example:bom:pom:2.0:import");

		assertThat(updater.declaresManagedDependency("org.example", "bom")).isTrue();
		assertThat(updater.declaresManagedDependency("org.example", "other")).isFalse();
		assertThat(updater.declaresManagedDependency("other.group", "bom")).isFalse();
	}

	@Test
	void batchesEditsViaWithPom() throws Exception {

		updater.withPom(editor -> {
			editor.dependencies().updateDependency(true, Coordinates.of("org.example", "a", "1"));
			editor.dependencies().updateDependency(true, Coordinates.of("org.example", "b", "2"));
		});

		var doc = parse();
		assertThat(xpath(doc, "count(/project/dependencies/dependency)")).isEqualTo("2");
	}

	private Document parse() throws Exception {

		var dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(false);
		return dbf.newDocumentBuilder().parse(pom.toFile());
	}

	private static String xpath(Document doc, String expression) throws Exception {
		return XPathFactory.newInstance().newXPath().evaluate(expression, doc);
	}
}
