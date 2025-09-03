/*
 * Copyright 2022-2025 the original author or authors.
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
package org.jmolecules.archunit;

import static org.assertj.core.api.Assertions.*;
import static org.jmolecules.archunit.JMoleculesArchitectureRules.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jmolecules.archunit.JMoleculesArchitectureRules.StereotypeLookup;
import org.jmolecules.archunit.JMoleculesArchitectureRules.VerificationDepth;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;

/**
 * Unit tests for {@link JMoleculesArchitectureRules#ensureHexagonal()} and
 * {@link JMoleculesArchitectureRules#ensureHexagonalStrict()}.
 *
 * @author Oliver Drotbohm
 */
class JMoleculesHexagonalRulesTest {

	@AnalyzeClasses(packages = "org.jmolecules.archunit.hexagonal.pkg")
	static class PackageBased {

		List<String> expected = Arrays.asList("adapter.SampleAdapter.application", //
				"adapter.SamplePrimaryAdapter.application", //
				"adapter.SamplePrimaryAdapter.secondaryAdapter", //
				"adapter.SamplePrimaryAdapter.secondaryAdapterPackage", //
				"adapter.SamplePrimaryAdapter.secondaryPort", //
				"adapter.SamplePrimaryAdapter.secondaryPortPackage", //
				"adapter.SampleSecondaryAdapter.application", //
				"adapter.SampleSecondaryAdapter.primaryAdapter", //
				"adapter.SampleSecondaryAdapter.primaryAdapterPackage", //
				"adapter.SampleSecondaryAdapter.primaryPort", //
				"adapter.SampleSecondaryAdapter.primaryPortPackage", //
				"adapter.primary.SamplePrimaryAdapterPackage.application", //
				"adapter.primary.SamplePrimaryAdapterPackage.secondaryAdapter", //
				"adapter.primary.SamplePrimaryAdapterPackage.secondaryAdapterPackage", //
				"adapter.primary.SamplePrimaryAdapterPackage.secondaryPort", //
				"adapter.primary.SamplePrimaryAdapterPackage.secondaryPortPackage", //
				"adapter.secondary.SampleSecondaryAdapterPackage.application", //
				"adapter.secondary.SampleSecondaryAdapterPackage.primaryAdapter", //
				"adapter.secondary.SampleSecondaryAdapterPackage.primaryAdapterPackage", //
				"adapter.secondary.SampleSecondaryAdapterPackage.primaryPort", //
				"adapter.secondary.SampleSecondaryAdapterPackage.primaryPortPackage", //
				"application.SampleApplication.adapter", //
				"application.SampleApplication.primaryAdapter", //
				"application.SampleApplication.primaryAdapterPackage", //
				"application.SampleApplication.secondaryAdapter", //
				"application.SampleApplication.secondaryAdapterPackage", //
				"port.SamplePort.adapter()", //
				"port.SamplePort.primaryAdapter()", //
				"port.SamplePort.primaryAdapterPackage()", //
				"port.SamplePort.secondaryAdapter()", //
				"port.SamplePort.secondaryAdapterPackage()", //
				"port.SamplePrimaryPort.adapter()", //
				"port.SamplePrimaryPort.primaryAdapter()", //
				"port.SamplePrimaryPort.primaryAdapterPackage()", //
				"port.SamplePrimaryPort.secondaryAdapter()", //
				"port.SamplePrimaryPort.secondaryAdapterPackage()", //
				"port.SampleSecondaryPort.application()", //
				"port.SampleSecondaryPort.adapter()", //
				"port.SampleSecondaryPort.primaryAdapter()", //
				"port.SampleSecondaryPort.primaryAdapterPackage()", //
				"port.SampleSecondaryPort.primaryPort()", //
				"port.SampleSecondaryPort.primaryPortPackage()", //
				"port.SampleSecondaryPort.secondaryAdapter()", //
				"port.SampleSecondaryPort.secondaryAdapterPackage()",
				"port.secondary.SampleSecondaryPortPackage.application()")
				.stream() //
				.map("org.jmolecules.archunit.hexagonal.pkg."::concat) //
				.collect(Collectors.toList());

		@ArchTest // GH-306
		void verifiesStrictHexagonalArchitecture(JavaClasses classes) {
			assertExpectedViolations(ensureHexagonal().evaluate(classes), expected);
		}

		@ArchTest // GH-306
		void verifiesSemiStrictHexagonalArchitecture(JavaClasses classes) {

			List<String> semiStrict = expected.stream()
					.filter(it -> !it.endsWith("SecondaryAdapter.application"))
					.filter(it -> !it.endsWith("SecondaryAdapterPackage.application"))
					.filter(it -> !it.endsWith("SecondaryPort.application()"))
					.filter(it -> !it.endsWith("SecondaryPortPackage.application()"))
					.filter(it -> !it.endsWith("PrimaryPort.application()"))
					.collect(Collectors.toList());

			assertExpectedViolations(ensureHexagonal(VerificationDepth.SEMI_STRICT).evaluate(classes), semiStrict);
		}

		@ArchTest // GH-306
		void verifiesLenientHexagonalArchitecture(JavaClasses classes) {

			List<String> lenient = expected.stream()
					.filter(it -> !it.endsWith(".application") && !it.endsWith(".application()"))
					.filter(it -> !it.endsWith("secondaryPort") && !it.endsWith("secondaryPortPackage"))
					.collect(Collectors.toList());

			assertExpectedViolations(ensureHexagonal(VerificationDepth.LENIENT).evaluate(classes), lenient);
		}

		@ArchTest // GH-346
		void verifiesHexagonalArchitectureWithNestedPackageAssignment(JavaClasses classes) {

			ArchRule rule = ensureHexagonal(VerificationDepth.LENIENT,
					StereotypeLookup.defaultLookup().withParentPackageTraversal());

			List<String> lenient = Stream
					.concat(expected.stream(), Stream.of("application.nested.SampleInNestedPackage.primaryAdapter"))
					.filter(it -> !it.endsWith("application") && !it.endsWith("application()"))
					.filter(it -> !it.endsWith("secondaryPort") && !it.endsWith("secondaryPortPackage"))
					.collect(Collectors.toList());

			assertExpectedViolations(rule.evaluate(classes), lenient);
		}
	}

	@AnalyzeClasses(packages = "org.jmolecules.archunit.hexagonal.simple")
	static class TypeBased {

		List<String> expected = Arrays.asList( //
				"SampleAdapter.application", //
				"SampleApplication.adapter", //
				"SampleApplication.primaryAdapter", //
				"SampleApplication.secondaryAdapter", //
				"SamplePrimaryAdapter.application", //
				"SamplePrimaryAdapter.secondaryPort", //
				"SampleSecondaryAdapter.application", //
				"SampleSecondaryAdapter.primaryPort", //
				"SampleSecondaryPort.application") //
				.stream() //
				.map("Field <org.jmolecules.archunit.hexagonal.simple."::concat) //
				.collect(Collectors.toList());

		@ArchTest // GH-306
		void verifiesStrictHexagonalArchitecture(JavaClasses classes) {
			assertExpectedViolations(ensureHexagonal().evaluate(classes), expected);
		}

		@ArchTest // GH-306
		void verifiesSemiStrictHexagonalArchitecture(JavaClasses classes) {

			// Allowed in semi-strict mode
			List<String> semiStrict = expected.stream()
					.filter(it -> !it.endsWith("SecondaryAdapter.application"))
					.filter(it -> !it.endsWith("SecondaryPort.application"))
					.collect(Collectors.toList());

			assertExpectedViolations(ensureHexagonal(VerificationDepth.SEMI_STRICT).evaluate(classes), semiStrict);
		}

		@ArchTest // GH-306
		void verifiesLenientHexagonalArchitecture(JavaClasses classes) {

			// Allowed in lenient mode
			List<String> lenient = expected.stream()
					.filter(it -> !it.endsWith(".application"))
					.filter(it -> !it.endsWith("SamplePrimaryAdapter.secondaryPort"))
					.collect(Collectors.toList());

			assertExpectedViolations(ensureHexagonal(VerificationDepth.LENIENT).evaluate(classes), lenient);
		}
	}

	private static void assertExpectedViolations(EvaluationResult result, List<String> expected) {

		List<String> filtered = expected;

		List<String> messages = result.getFailureReport().getDetails();
		assertThat(messages.stream().distinct()).hasSameSizeAs(filtered);

		Stream<String> consumed = messages.stream().distinct()
				.filter(message -> !filtered.stream().anyMatch(message::contains));
		assertThat(consumed).isEmpty();
	}
}
