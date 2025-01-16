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
import java.util.List;
import java.util.function.Function;

import org.jmolecules.cli.core.BuildSystem.ScriptOperations;
import org.jmolecules.cli.core.MavenBuildSystem.MavenDependency;
import org.jmolecules.cli.core.PomUpdater.Pom.ManagedArtifact;
import org.jmolecules.codegen.ProjectFiles;
import org.xmlbeam.annotation.XBRead;
import org.xmlbeam.annotation.XBValue;
import org.xmlbeam.annotation.XBWrite;
import org.xmlbeam.io.FileIO;

/**
 * @author Oliver Drotbohm
 */
public class PomUpdater implements ScriptOperations {

	private final ProjectFiles workspace;

	private final FileIO io;
	private final Pom pom;

	public PomUpdater(ProjectFiles workspace) {

		this.workspace = workspace;

		// try {
		this.io = null; // new XBProjector().io().file(workspace.resolve("pom.xml").toFile());
		this.pom = null; // io.read(Pom.class);
		// } catch (IOException e) {
		// throw new RuntimeException(e);
		// }
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.cli.build.BuildSystem.ScriptOperations#addDependency(java.lang.String)
	 */
	@Override
	public void addDependency(String coordinates) {

	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.cli.build.BuildSystem.ScriptOperations#addManagedDependency(java.lang.String)
	 */
	@Override
	public void addManagedDependency(String coordinates) {

		var dependency = MavenDependency.of(coordinates);

		pom.addDependencyManagement(new ManagedArtifact() {

			@Override
			public String getVersion() {
				return dependency.version();
			}

			@Override
			public String getGroupId() {
				return dependency.groupId();
			}

			@Override
			public String getArtifactId() {
				return dependency.artifactId();
			}
		});
	}

	void update(Function<Pom, Pom> operations) {

		try {
			io.write(operations.apply(pom));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	interface Pom {

		interface Artifact {

			@XBRead("child::groupId")
			String getGroupId();

			@XBRead("child::artifactId")
			String getArtifactId();

			@XBRead("child::version")
			String getVersion();
		}

		interface ManagedArtifact extends Artifact {

			@XBRead("child::scope")
			default String getScope() {
				return "import";
			}

			@XBRead("child::pom")
			default String getType() {
				return "pom";
			}
		}

		@XBWrite("/properties/{0}")
		void addProperty(String name, @XBValue String value);

		default void addDependencyManagement(ManagedArtifact artifact) {
			var existing = getDependencyManagement();
			existing.add(artifact);

			setDependencyManagement(existing);
		}

		@XBRead("/dependencyManagement/dependencies")
		List<ManagedArtifact> getDependencyManagement();

		@XBWrite("/dependencyManagement/dependencies")
		void setDependencyManagement(List<ManagedArtifact> artifacts);

		@XBRead("/project/dependencies/dependency")
		List<Artifact> getDependencies();
	}
}
