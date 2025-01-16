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
package org.jmolecules.codegen;

import java.util.Arrays;

/**
 * @author Oliver Drotbohm
 */
public interface Dependency {

	String groupId();

	String artifactId();

	String version();

	Scope scope();

	default boolean hasScope(Scope scope) {
		return scope.equals(scope());
	}

	public enum Scope {

		COMPILE, RUNTIME, TEST;

		public static Scope of(String source) {

			return Arrays.stream(values())
					.filter(it -> source.equalsIgnoreCase(it.name()))
					.findFirst()
					.orElse(null);
		}
	}

	default boolean hasGroupId(String groupId) {
		return groupId.equals(groupId());
	}

	/**
	 * @param artifactId
	 * @return
	 */
	default boolean hasArtifactId(String artifactId) {
		return artifactId.equals(artifactId());
	}
}
