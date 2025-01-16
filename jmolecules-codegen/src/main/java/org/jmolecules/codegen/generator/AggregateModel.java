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
package org.jmolecules.codegen.generator;

import org.jmolecules.codegen.SourceFile;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;

class AggregateModel {

	private final String name;
	private final String packageName;

	public AggregateModel(String name, String packageName) {
		this.name = name;
		this.packageName = packageName;
	}

	/**
	 * @return the packageName
	 */
	String getPackageName() {
		return packageName;
	}

	ClassName getAggregateName() {
		return ClassName.get(packageName, name);
	}

	ClassName getAggregateUnitTestName() {
		return ClassName.get(packageName, name + "UnitTest");
	}

	ClassName getIdentifierName() {
		return getAggregateName().nestedClass(name + "Identifier");
	}

	ClassName getRepositoryName() {
		return ClassName.get(packageName, name + "Repository");
	}

	ClassName getRepositoryIntegrationTestName() {
		return ClassName.get(packageName, name + "RepositoryIntegrationTests");
	}

	ClassName getCreationEventName() {
		return getAggregateName().nestedClass(name + "Created");
	}

	SourceFile toFile(TypeSpec spec, SourceFile.Type type) {
		return new SourceFile(JavaFile.builder(packageName, spec).indent("\t").build(), type);
	}
}
