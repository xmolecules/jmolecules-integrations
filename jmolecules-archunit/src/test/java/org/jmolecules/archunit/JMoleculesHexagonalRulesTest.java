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

import org.jmolecules.archunit.JMoleculesArchitectureRules.VerificationDepth;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.thirdparty.com.google.common.base.Predicate;

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
				"port.SamplePort.sampleAdapter()", //
				"port.SamplePort.samplePrimaryAdapter()", //
				"port.SamplePort.samplePrimaryAdapterPackage()", //
				"port.SamplePort.sampleSecondaryAdapter()", //
				"port.SamplePort.sampleSecondaryAdapterPackage()", //
				"port.SamplePrimaryPort.sampleAdapter()", //
				"port.SamplePrimaryPort.samplePrimaryAdapter()", //
				"port.SamplePrimaryPort.samplePrimaryAdapterPackage()", //
				"port.SamplePrimaryPort.sampleSecondaryAdapter()", //
				"port.SamplePrimaryPort.sampleSecondaryAdapterPackage()", //
				"port.SampleSecondaryPort.sampleAdapter()", //
				"port.SampleSecondaryPort.samplePrimaryAdapter()", //
				"port.SampleSecondaryPort.samplePrimaryAdapterPackage()", //
				"port.SampleSecondaryPort.samplePrimaryPort()", //
				"port.SampleSecondaryPort.samplePrimaryPortPackage()", //
				"port.SampleSecondaryPort.sampleSecondaryAdapter()", //
				"port.SampleSecondaryPort.sampleSecondaryAdapterPackage()")
				.stream() //
				.map("org.jmolecules.archunit.hexagonal.pkg."::concat) //
				.collect(Collectors.toList());

		@ArchTest // GH-306
		void verifiesStrictHexagonalArchitecture(JavaClasses classes) {
			assertExpectedViolations(ensureHexagonal().evaluate(classes), expected, it -> true);
		}

		@ArchTest // GH-306
		void verifiesSemiStrictHexagonalArchitecture(JavaClasses classes) {
			assertExpectedViolations(ensureHexagonal(VerificationDepth.SEMI_STRICT).evaluate(classes), expected,
					it -> !(it.endsWith("application") && it.contains("SecondaryAdapter")));
		}

		@ArchTest // GH-306
		void verifiesHexagonalArchitecture(JavaClasses classes) {
			assertExpectedViolations(ensureHexagonal(VerificationDepth.LENIENT).evaluate(classes), expected,
					it -> !it.endsWith("application"));
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
				"SampleSecondaryAdapter.primaryPort") //
				.stream() //
				.map("Field <org.jmolecules.archunit.hexagonal.simple."::concat) //
				.collect(Collectors.toList());

		@ArchTest // GH-306
		void verifiesStrictHexagonalArchitecture(JavaClasses classes) {
			assertExpectedViolations(ensureHexagonal().evaluate(classes), expected, it -> true);
		}

		@ArchTest // GH-306
		void verifiesSemiStrictHexagonalArchitecture(JavaClasses classes) {
			assertExpectedViolations(ensureHexagonal(VerificationDepth.SEMI_STRICT).evaluate(classes), expected,
					it -> !(it.endsWith("application") && it.contains("SecondaryAdapter")));
		}

		@ArchTest // GH-306
		void verifiesHexagonalArchitecture(JavaClasses classes) {
			assertExpectedViolations(ensureHexagonal(VerificationDepth.LENIENT).evaluate(classes), expected,
					it -> !it.endsWith("application"));
		}
	}

	private static void assertExpectedViolations(EvaluationResult result, List<String> expected,
			Predicate<String> filter) {

		List<String> filtered = expected.stream().filter(filter).collect(Collectors.toList());

		List<String> messages = result.getFailureReport().getDetails();
		assertThat(messages.stream().distinct()).hasSameSizeAs(filtered);

		Stream<String> consumed = messages.stream().distinct()
				.filter(message -> !filtered.stream().anyMatch(message::contains));
		assertThat(consumed).isEmpty();
	}
}
