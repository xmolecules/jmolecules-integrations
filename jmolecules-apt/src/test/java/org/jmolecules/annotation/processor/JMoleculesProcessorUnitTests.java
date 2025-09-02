/*
 * Copyright 2024-2025 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import io.toolisticon.cute.Cute;
import io.toolisticon.cute.CuteApi.BlackBoxTestInterface;
import io.toolisticon.cute.CuteApi.BlackBoxTestSourceFilesAndProcessorInterface;
import io.toolisticon.cute.CuteApi.CompilerTestExpectAndThatInterface;
import io.toolisticon.cute.CuteApi.DoCustomAssertions;
import net.minidev.json.JSONArray;

import javax.tools.StandardLocation;

import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.JsonPath;

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

		String source = getSourceFile("MyAggregateRoot");

		assertFailed(source)
				.andThat().compilerMessage().ofKindError().atLine(8).atSource(source)
				.contains("Invalid aggregate root reference!")
				.executeTest();
	}

	@Test // GH-230
	void detectsInvalidAggregateRootReferenceInImplementingAggregateInNestedClass() {

		String source = getSourceFile("MyAggregateRootNested");

		assertFailed(source)
				.andThat().compilerMessage().ofKindError().atLine(10).atSource(source)
				.contains("Invalid aggregate root reference!")
				.executeTest();
	}

	@Test // GH-230
	void detectsInvalidAggregateRootReferenceInImplementingEntity() {

		String source = getSourceFile("MyEntity");

		assertFailed(source)
				.andThat().compilerMessage().ofKindError().atLine(10).atSource(source)
				.contains("Invalid aggregate root reference!")
				.executeTest();
	}

	@Test // GH-230
	void detectsInvalidAggregateRootReferenceInAnnotatedAggregate() {

		String source = getSourceFile("AnnotatedAggregateRoot");

		assertFailed(source)
				.andThat().compilerMessage().ofKindError().atLine(10).atSource(source)
				.contains("Invalid aggregate root reference!")
				.executeTest();
	}

	@Test // GH-230
	void detectsMissingIdentifierInAnnotatedAggregate() {

		String source = getSourceFile("AnnotatedAggregateRoot");

		assertFailed(source)
				.andThat().compilerMessage().ofKindError().atLine(9).atSource(source).contains("identity")
				.executeTest();
	}

	@Test // GH-230
	void passesAnnotatedAggregateRootWithFieldIdentity() {
		assertSucceded(getSourceFile("valid/WithMethodIdentity"));
	}

	@Test // GH-230
	void passesAnnotatedAggregateRootWithMethodIdentity() {
		assertSucceded(getSourceFile("valid/WithFieldIdentity"));
	}

	@Test // GH-230
	void rejectsReferencesToIdentifiablesFromValueObject() {

		String source = getSourceFile("MyValueObject");

		assertFailed(source)
				.andThat().compilerMessage().ofKindError().atLine(7).atSource(source).contains("identifiables")
				.andThat().compilerMessage().ofKindError().atLine(8).atSource(source).contains("identifiables")
				.andThat().compilerMessage().ofKindError().atLine(9).atSource(source).contains("identifiables")
				.executeTest();
	}

	@Test // GH-230
	void rejectsReferencesToIdentifiablesFromAnnotatedValueObject() {

		String source = getSourceFile("AnnotatedValueObject");

		assertFailed(source)
				.andThat().compilerMessage().ofKindError().atLine(8).atSource(source).contains("identifiables")
				.andThat().compilerMessage().ofKindError().atLine(9).atSource(source).contains("identifiables")
				.andThat().compilerMessage().ofKindError().atLine(10).atSource(source).contains("identifiables")
				.executeTest();
	}

	@Test // GH-230
	void rejectsReferencesToIdentifiablesFromRecordValueObject() {

		String source = getSourceFile("MyRecord");

		assertFailed(source)
				.andThat().compilerMessage().ofKindError().atLine(5).atColumn(26).atSource(source).contains("identifiables")
				.executeTest();
	}

	@Test // GH-230
	void passesValidValueObject() {
		assertSucceded(getSourceFile("valid/ValidValueObject"));
		assertSucceded(getSourceFile("valid/ValidAnnotatedValueObject"));
	}

	@Test // GH-230
	void passesValidIdentifier() {
		assertSucceded(getSourceFile("valid/ValidIdentifier"));
	}

	@Test
	void createsStereotypesMetadata() {

		assertSourceProcessed(getSourceFile("stereotype/AssignableStereotype"))
				.thenExpectThat().compilationSucceeds().andThat()
				.fileObject(StandardLocation.CLASS_OUTPUT, "", "META-INF/jmolecules-stereotypes.json")
				.exists()
				.executeTest()
				.executeCustomAssertions(outcome -> {

					var file = outcome.getFileManager()
							.getGeneratedResourceFile("/META-INF/jmolecules-stereotypes.json");

					var content = file.get().getContent();
					var context = JsonPath.parse(content);

					assertThat(context.read("$.stereotypes['example.stereotype.AssignableStereotype']", Object.class))
							.isNotNull()
							.satisfies(it -> {

								var nested = JsonPath.parse(it);

								assertThat(nested.read("$.targets", JSONArray.class))
										.containsExactly("example.stereotype.AssignableStereotype");
								assertThat(nested.read("$.groups", JSONArray.class)).containsExactly("example.stereotype");
								assertThat(nested.read("$.priority", Integer.class)).isEqualTo(0);
							});
				});
	}

	private static CompilerTestExpectAndThatInterface assertFailed(String source) {

		return assertSourceProcessed(source)
				.thenExpectThat().compilationFails();
	}

	private static DoCustomAssertions assertSucceded(String source) {

		return assertSourceProcessed(source)
				.thenExpectThat().compilationSucceeds()
				.executeTest();
	}

	private static BlackBoxTestInterface assertSourceProcessed(String source) {

		return Cute.blackBoxTest()
				.given()
				.processor(JMoleculesProcessor.class)
				.andSourceFiles(source)
				.whenCompiled();
	}

	private static String getSourceFile(String name) {
		return "/example/" + name + ".java";
	}
}
