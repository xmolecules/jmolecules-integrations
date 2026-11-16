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

package org.jmolecules.archunit;

import static org.assertj.core.api.Assertions.*;
import static org.jmolecules.archunit.JMoleculesArchitectureRules.*;
import static org.jmolecules.archunit.TestUtils.*;

import java.util.List;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

/**
 * Tests for {@link HexagonalVerificationOptions#withDirectApplicationAccess()} in combination with the exposure and
 * referrers declared on {@link org.jmolecules.architecture.hexagonal.Hexagon} units.
 *
 * @author Oliver Drotbohm
 */
@AnalyzeClasses(packages = "org.jmolecules.archunit.hexagonal.direct")
class JMoleculesHexagonalDirectApplicationAccessTest {

	@ArchTest // GH-418
	void grantsDirectApplicationAccessIndependentlyOfDeclaredExposureAndReferrers(JavaClasses types) {

		HexagonalVerificationOptions options = HexagonalVerificationOptions.defaults().withDirectApplicationAccess();

		List<String> details = ensureHexagonal(options).evaluate(types).getFailureReport().getDetails();

		// Provider only exposes primary adapters, consumer only allows secondary adapters to refer to other units
		assertNotReported(details, "ConsumerApp.providerApp");
		assertNotReported(details, "ConsumerApp.providerPort");
		assertNotReported(details, "PlainNone.providerApp");
		assertNotReported(details, "ConsumerApp.plain");

		// Everything else is still subject to exposure and referrers
		assertReported(details, "ConsumerApp.providerAdapter",
				"Only secondary adapters, as well as application code and ports (to application code and primary ports), may refer");
		assertReported(details, "ConsumerApp.providerSecondaryPort",
				"Only secondary adapters, as well as application code and ports (to application code and primary ports), may refer");
		assertNotReported(details, "ConsumerSecondaryAdapter.providerAdapter");
		assertReported(details, "ConsumerSecondaryAdapter.providerApp", "which only exposes primary adapters");
		assertReported(details, "ConsumerSecondaryAdapter.providerPort", "which only exposes primary adapters");
		assertReported(details, "PlainNone.providerSecondaryPort", "which only exposes primary adapters");
	}

	@ArchTest // GH-418
	void doesNotGrantDirectApplicationAccessByDefault(JavaClasses types) {

		List<String> details = ensureHexagonal().evaluate(types).getFailureReport().getDetails();

		assertReported(details, "ConsumerApp.providerApp",
				"Only secondary adapters may refer to other architectural units");
		assertReported(details, "ConsumerApp.providerPort",
				"Only secondary adapters may refer to other architectural units");
		assertReported(details, "PlainNone.providerApp", "which only exposes primary adapters");
		assertReported(details, "ConsumerApp.plain", "Only secondary adapters may refer to other architectural units");
	}

	@ArchTest // GH-418
	void allowsDisablingDirectApplicationAccess(JavaClasses types) {

		HexagonalVerificationOptions options = HexagonalVerificationOptions.defaults()
				.withDirectApplicationAccess()
				.withoutDirectApplicationAccess();

		assertThat(ensureHexagonal(options).evaluate(types).getFailureReport().getDetails())
				.containsExactlyInAnyOrderElementsOf(ensureHexagonal().evaluate(types).getFailureReport().getDetails());
	}

	@ArchTest // GH-418
	void grantsDirectApplicationAccessToPorts(JavaClasses types) {

		HexagonalVerificationOptions options = HexagonalVerificationOptions.defaults().withDirectApplicationAccess();

		List<String> details = ensureHexagonal(options).evaluate(types).getFailureReport().getDetails();

		assertThat(details)
				.noneSatisfy(it -> assertThat(it).containsPattern("Consumer(Primary|Secondary)Port\\.provider(App|Port)\\(\\)"))
				.anySatisfy(it -> assertThat(it).contains("ConsumerPrimaryPort.providerAdapter()").contains("may refer"))
				.anySatisfy(it -> assertThat(it).contains("ConsumerSecondaryPort.providerAdapter()").contains("may refer"));

		// Not granted without direct application access
		assertThat(ensureHexagonal().evaluate(types).getFailureReport().getDetails())
				.anySatisfy(it -> assertThat(it).contains("ConsumerPrimaryPort.providerApp()"))
				.anySatisfy(it -> assertThat(it).contains("ConsumerSecondaryPort.providerPort()"));
	}
}
