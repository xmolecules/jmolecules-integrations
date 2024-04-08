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
import io.toolisticon.cute.CuteApi.BlackBoxTestSourceFilesAndProcessorInterface;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JMoleculesProcessor}.
 *
 * @author Oliver Drotbohm
 * @author Tobias Stamann
 */
class JMoleculesProcessorUnitTests {

	BlackBoxTestSourceFilesAndProcessorInterface baseBlackBoxSetup = Cute.blackBoxTest()
			.given()
			.processor(JMoleculesProcessor.class);

	@Test // GH-230
	void detectsInvalidAggregateRootReferenceInImplementingAggregate() {

		baseBlackBoxSetup
				.andSourceFiles(getSourceFile("MyAggregateRoot"))
				.whenCompiled()
				.thenExpectThat().compilationFails()
				.andThat().compilerMessage().ofKindError().atLine(8)
				.contains("Invalid aggregate root reference!")
				.executeTest();
	}

	@Test // GH-230
	void detectsInvalidAggregateRootReferenceInImplementingAggregateInNestedClass() {

		baseBlackBoxSetup.andSourceFiles(getSourceFile("MyAggregateRootNested"))
				.whenCompiled()
				.thenExpectThat().compilationFails()
				.andThat().compilerMessage().ofKindError().atLine(10)
				.contains("Invalid aggregate root reference!")
				.executeTest();
	}

	@Test // GH-230
	void detectsInvalidAggregateRootReferenceInImplementingEntity() {

		baseBlackBoxSetup
				.andSourceFiles(getSourceFile("MyEntity"))
				.whenCompiled()
				.thenExpectThat().compilationFails()
				.andThat().compilerMessage().ofKindError()
				.atLine(8)
				.contains("Invalid aggregate root reference!")
				.executeTest();
	}

	@Test // GH-230
	void detectsInvalidAggregateRootReferenceInAnnotatedAggregate() {

		baseBlackBoxSetup
				.andSourceFiles(getSourceFile("AnnotatedAggregateRoot"))
				.whenCompiled()
				.thenExpectThat().compilationFails()
				.andThat().compilerMessage().ofKindError()
				.atLine(10)
				.contains("Invalid aggregate root reference!")
				.executeTest();

	}

	@Test // GH-230
	void detectsMissingIdentifierInAnnotatedAggregate() {

		baseBlackBoxSetup
				.andSourceFiles(getSourceFile("AnnotatedAggregateRoot"))
				.whenCompiled()
				.thenExpectThat().compilationFails()
				.andThat().compilerMessage().ofKindError()
				.atLine(9)
				.contains("identity")
				.executeTest();
	}

	@Test // GH-230
	void passesAnnotatedAggregateRootWithFieldIdentity() {

		baseBlackBoxSetup
				.andSourceFiles(getSourceFile("valid/WithMethodIdentity"))
				.whenCompiled()
				.thenExpectThat().compilationSucceeds()
				.executeTest();
	}

	@Test // GH-230
	void passesAnnotatedAggregateRootWithMethodIdentity() {

		baseBlackBoxSetup
				.andSourceFiles(getSourceFile("valid/WithFieldIdentity"))
				.whenCompiled()
				.thenExpectThat().compilationSucceeds()
				.executeTest();
	}

	private static String getSourceFile(String name) {
		return "/example/" + name + ".java";
	}
}
