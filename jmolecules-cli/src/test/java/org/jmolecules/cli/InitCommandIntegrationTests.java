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

import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;

/**
 * End-to-end smoke test for {@code jm init}. Stages a project workspace from {@code sample-pom.xml} and the Maven
 * Wrapper, then drives the command via {@link CliApplication#createCommandLine(String)}. Spawns Maven via the wrapper;
 * first-ever run may download Maven.
 *
 * @author Oliver Drotbohm
 */
class InitCommandIntegrationTests {

	@TempDir Path workspace;

	@BeforeEach
	void setUp() throws IOException {

		Files.copy(getClass().getResourceAsStream("/sample-pom.xml"), workspace.resolve("pom.xml"));
		Files.createDirectories(workspace.resolve("src/main/java"));

		var projectRoot = findProjectRoot();
		copyExecutable(projectRoot.resolve("mvnw"), workspace.resolve("mvnw"));
		Files.copy(projectRoot.resolve("mvnw.cmd"), workspace.resolve("mvnw.cmd"));

		var wrapperDir = workspace.resolve(".mvn/wrapper");
		Files.createDirectories(wrapperDir);
		Files.copy(projectRoot.resolve(".mvn/wrapper/maven-wrapper.properties"),
				wrapperDir.resolve("maven-wrapper.properties"));
	}

	@Test
	void runsInitAgainstTestPom() throws Exception {

		var commandLine = CliApplication.createCommandLine(workspace.toString());

		var exitCode = commandLine.execute("init", "--force", "--version", "2025.0.2");

		assertThat(exitCode).as("`jm init` exit code").isZero();
		assertThat(workspace.resolve("jmolecules.config"))
				.as("jmolecules.config was written to workspace").exists();

		var pom = parsePom();
		var bom = "/project/dependencyManagement/dependencies/dependency"
				+ "[groupId='org.jmolecules' and artifactId='jmolecules-bom']";
		assertThat(xpath(pom, bom + "/version/text()")).isEqualTo("${jmolecules.version}");
		assertThat(xpath(pom, bom + "/type/text()")).isEqualTo("pom");
		assertThat(xpath(pom, bom + "/scope/text()")).isEqualTo("import");
		assertThat(xpath(pom, "/project/properties/jmolecules.version/text()")).isEqualTo("2025.0.2");
	}

	@Test
	void abortsWhenDependencyResolutionFails() throws Exception {

		Files.writeString(workspace.resolve("pom.xml"), """
				<?xml version="1.0" encoding="UTF-8"?>
				<project xmlns="http://maven.apache.org/POM/4.0.0">
					<modelVersion>4.0.0</modelVersion>
					<groupId>org.example</groupId>
					<artifactId>broken</artifactId>
					<version>1.0.0-SNAPSHOT</version>
					<dependencies>
						<dependency>
							<groupId>does.not.exist</groupId>
							<artifactId>nonexistent</artifactId>
							<version>9999.0.0</version>
						</dependency>
					</dependencies>
				</project>
				""");

		var commandLine = CliApplication.createCommandLine(workspace.toString());

		var stderr = new ByteArrayOutputStream();
		commandLine.setErr(new PrintWriter(stderr, true, StandardCharsets.UTF_8));

		var exitCode = commandLine.execute("init", "--force", "--version", "2025.0.2");

		assertThat(exitCode).as("`jm init` exit code on Maven failure").isNotZero();
		assertThat(workspace.resolve("jmolecules.config"))
				.as("jmolecules.config must not be written when init aborts").doesNotExist();

		var stderrText = stderr.toString(StandardCharsets.UTF_8);
		assertThat(stderrText).as("user-facing message").contains("classpath could not be resolved properly");
		assertThat(stderrText).as("no stack trace dumped").doesNotContain("\tat ");

		var errorFile = workspace.resolve(".jm/error");
		assertThat(errorFile).as("Maven failure details persisted").exists();
		assertThat(Files.readString(errorFile))
				.contains("dependency:build-classpath")
				.contains("does.not.exist:nonexistent");
	}

	@Test
	void skipsBomAdditionWhenBomAlreadyDeclared() throws Exception {

		Files.writeString(workspace.resolve("pom.xml"), """
				<?xml version="1.0" encoding="UTF-8"?>
				<project xmlns="http://maven.apache.org/POM/4.0.0">
					<modelVersion>4.0.0</modelVersion>
					<groupId>org.example</groupId>
					<artifactId>has-bom</artifactId>
					<version>1.0.0-SNAPSHOT</version>
					<dependencyManagement>
						<dependencies>
							<dependency>
								<groupId>org.jmolecules</groupId>
								<artifactId>jmolecules-bom</artifactId>
								<version>2025.0.2</version>
								<type>pom</type>
								<scope>import</scope>
							</dependency>
						</dependencies>
					</dependencyManagement>
				</project>
				""");

		var commandLine = CliApplication.createCommandLine(workspace.toString());

		var exitCode = commandLine.execute("init", "--force", "--version", "2025.0.2");

		assertThat(exitCode).as("`jm init` exit code").isZero();

		var pom = parsePom();
		var bomCount = "count(/project/dependencyManagement/dependencies/dependency"
				+ "[groupId='org.jmolecules' and artifactId='jmolecules-bom'])";
		assertThat(xpath(pom, bomCount)).as("BOM declaration must remain a single entry").isEqualTo("1");
		assertThat(xpath(pom, "count(/project/properties/jmolecules.version)"))
				.as("jmolecules.version property must not be added when BOM addition is skipped")
				.isEqualTo("0");
		assertThat(workspace.resolve(".jm/effective-pom.xml"))
				.as("local fast-path must short-circuit the help:effective-pom invocation").doesNotExist();
	}

	@Test
	void skipsBomAdditionViaEffectivePomWhenIndividualJMoleculesManagedDepDeclared() throws Exception {

		Files.writeString(workspace.resolve("pom.xml"), """
				<?xml version="1.0" encoding="UTF-8"?>
				<project xmlns="http://maven.apache.org/POM/4.0.0">
					<modelVersion>4.0.0</modelVersion>
					<groupId>org.example</groupId>
					<artifactId>has-jmolecules-managed</artifactId>
					<version>1.0.0-SNAPSHOT</version>
					<dependencyManagement>
						<dependencies>
							<dependency>
								<groupId>org.jmolecules</groupId>
								<artifactId>jmolecules-ddd</artifactId>
								<version>2.0.1</version>
							</dependency>
						</dependencies>
					</dependencyManagement>
				</project>
				""");

		var commandLine = CliApplication.createCommandLine(workspace.toString());

		var exitCode = commandLine.execute("init", "--force", "--version", "2025.0.2");

		assertThat(exitCode).as("`jm init` exit code").isZero();

		var pom = parsePom();
		assertThat(xpath(pom, "count(/project/dependencyManagement/dependencies/dependency"
				+ "[artifactId='jmolecules-bom'])"))
						.as("BOM must not be added; jMolecules already managed individually").isEqualTo("0");
		assertThat(workspace.resolve(".jm/effective-pom.xml"))
				.as("slow path goes through help:effective-pom").exists();
	}

	private Document parsePom() throws Exception {

		var dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(false);
		return dbf.newDocumentBuilder().parse(workspace.resolve("pom.xml").toFile());
	}

	private static String xpath(Document doc, String expression) throws Exception {
		return XPathFactory.newInstance().newXPath().evaluate(expression, doc);
	}

	private static Path findProjectRoot() {

		var candidate = Paths.get("").toAbsolutePath();

		while (candidate != null && !Files.exists(candidate.resolve("mvnw"))) {
			candidate = candidate.getParent();
		}

		if (candidate == null) {
			throw new IllegalStateException("Could not locate project root with mvnw from " + Paths.get("").toAbsolutePath());
		}

		return candidate;
	}

	private static void copyExecutable(Path source, Path target) throws IOException {

		Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);

		try {
			Files.setPosixFilePermissions(target, PosixFilePermissions.fromString("rwxr-xr-x"));
		} catch (UnsupportedOperationException ignored) {
			// non-POSIX filesystem (Windows); COPY_ATTRIBUTES already did what it can.
		}
	}
}
