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
package org.jmolecules.annotation.processor;

import io.toolisticon.cute.Cute;
import io.toolisticon.cute.CuteApi.CompilerMessageCheckComparisonType;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JMoleculesProcessor}.
 *
 * @author Oliver Drotbohm
 */
class JMoleculesProcessorUnitTests {

	@Test
	@Disabled
	void detectsInvalidAggregateRootReferenceInImplementingAggregate() {

		assertFailed("MyAggregateRoot")
				.atLine(8)
				.contains("Invalid aggregate root reference!")
				.executeTest();
	}

	@Test
	@Disabled
	void detectsInvalidAggregateRootReferenceInImplementingEntity() {

		assertFailed("MyEntity")
				.atLine(8)
				.contains("Invalid aggregate root reference!")
				.executeTest();
	}

	@Test
	void detectsInvalidAggregateRootReferenceInAnnotatedAggregate() {

		assertFailed("AnnotatedAggregateRoot")
				.atLine(10)
				.contains("Invalid aggregate root reference!")
				.executeTest();
	}

	@Test
	void detectsMissingIdentifierInAnnotatedAggregate() {

		assertFailed("AnnotatedAggregateRoot")
				.atLine(9)
				.contains("identity")
				.executeTest();
	}

	@Test
	void passesAnnotatedAggregateRootWithFieldIdentity() {
		assertSucceeded("WithFieldIdentity");
	}

	@Test
	void passesAnnotatedAggregateRootWithMethodIdentity() {
		assertSucceeded("WithFieldIdentity");
	}

	private static CompilerMessageCheckComparisonType assertFailed(String source) {

		String file = getSourceFile(source);

		return Cute.blackBoxTest()
				.given()
				.processor(JMoleculesProcessor.class)
				.andSourceFiles(file)
				.whenCompiled()
				.thenExpectThat()
				.compilationFails()
				.andThat()
				.compilerMessage()
				.ofKindError()
				.atSource(file);
	}

	private static void assertSucceeded(String source) {

		String file = getSourceFile(source);

		Cute.blackBoxTest()
				.given()
				.processor(JMoleculesProcessor.class)
				.andSourceFiles(file)
				.whenCompiled()
				.thenExpectThat()
				.compilationSucceeds()
				.executeTest();
	}

	private static String getSourceFile(String name) {
		return "/example/" + name + ".java";
	}
}
