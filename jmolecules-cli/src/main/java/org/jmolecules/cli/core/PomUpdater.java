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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.jmolecules.cli.core.BuildSystem.ScriptOperations;
import org.jmolecules.cli.core.MavenBuildSystem.MavenDependency;
import org.jmolecules.codegen.ProjectFiles;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.maven.Coordinates;
import eu.maveniverse.domtrip.maven.PomEditor;

/**
 * @author Oliver Drotbohm
 */
public class PomUpdater implements ScriptOperations {

	private final Path pomPath;

	public PomUpdater(ProjectFiles workspace) {
		this.pomPath = workspace.resolve("pom.xml");
	}

	/**
	 * Opens a {@link PomEditor} against the project's {@code pom.xml}, hands it to {@code mutation}, and writes the
	 * result back. Use for batching multiple edits in a single read/write cycle (e.g. from a command).
	 */
	public void withPom(Consumer<PomEditor> mutation) {

		try {

			var editor = new PomEditor(Document.of(pomPath));
			mutation.accept(editor);
			Files.writeString(pomPath, editor.toXml());

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void addDependency(String coordinates) {

		var dep = MavenDependency.of(coordinates);

		withPom(editor -> editor.dependencies().updateDependency(true,
				Coordinates.of(dep.groupId(), dep.artifactId(), dep.version())));
	}

	@Override
	public void addManagedDependency(String coordinates) {

		var dep = MavenDependency.of(coordinates);

		withPom(editor -> {

			editor.dependencies().updateManagedDependency(true,
					Coordinates.of(dep.groupId(), dep.artifactId(), dep.version(), null, "pom"));

			// PomEditor.updateManagedDependency does not write <scope>import</scope>; add it ourselves.
			editor.root()
					.path("dependencyManagement", "dependencies")
					.ifPresent(deps -> deps.childElements("dependency")
							.filter(it -> dep.groupId().equals(it.childText("groupId")))
							.filter(it -> dep.artifactId().equals(it.childText("artifactId")))
							.findFirst()
							.filter(it -> it.childElement("scope").isEmpty())
							.ifPresent(it -> editor.addElement(it, "scope", "import")));
		});
	}
}
