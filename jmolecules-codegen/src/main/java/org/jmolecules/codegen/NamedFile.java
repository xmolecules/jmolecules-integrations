/*
 * Copyright 2026 the original author or authors.
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

/**
 * Returns a named file.
 *
 * @author Oliver Drotbohm
 */
public interface NamedFile {

	/**
	 * Returns the file name.
	 *
	 * @return will never be {@literal null}.
	 */
	String getFileName();

	default String getIcon() {
		return getIcon(getFileName());
	}

	public static String getIcon(String name) {

		var extension = name.substring(name.lastIndexOf("."));

		return switch (extension) {
			case ".java" -> "☕️";
			default -> "📄";
		};
	}
}
