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

import com.palantir.javapoet.JavaFile;

/**
 * @author Oliver Drotbohm
 */
public record SourceFile(JavaFile file, Type type) {

	public String toPath() {
		return file.toJavaFileObject().toUri().toString();
	}

	public String getFileName() {
		return toPath().substring(toPath().lastIndexOf("/") + 1);
	}

	public enum Type {
		SOURCE, TEST_SOURCE, RESOURCE, TEST_RESOURCE;
	}
}
