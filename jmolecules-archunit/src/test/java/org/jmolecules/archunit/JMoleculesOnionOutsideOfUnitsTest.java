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
import static org.jmolecules.archunit.TestUtils.*;

import java.util.List;

import org.jmolecules.architecture.onion.Onion.Exposure;
import org.jmolecules.architecture.onion.Onion.Referrer;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

/**
 * Unit tests for the verification of Onion Architecture references between types outside of any architectural unit and
 * the ones of units.
 *
 * @author Oliver Drotbohm
 */
@AnalyzeClasses(packages = { "org.jmolecules.archunit.onion.units", "org.jmolecules.archunit.onion.outside" })
class JMoleculesOnionOutsideOfUnitsTest {

	@ArchTest // GH-418
	void respectsExposureOfUnitsForTypesOutsideOfUnits(JavaClasses types) {

		// Only the infrastructure ring exposed by default
		List<String> details = JMoleculesArchitectureRules.ensureOnionSimple().evaluate(types).getFailureReport()
				.getDetails();

		assertNotReported(details, "InfraOut.infraB");
		assertReported(details, "InfraOut.appB",
				"which only exposes infrastructure ring types (to infrastructure ring types).");
		assertReported(details, "InfraOut.domainB", "DomainB belongs to architectural unit Onion B");
		assertReported(details, "InfraOut.helperB", "HelperB belongs to architectural unit Onion B");

		OnionVerificationOptions options = OnionVerificationOptions.defaults().withExposure(Exposure.APPLICATION);

		details = JMoleculesArchitectureRules.ensureOnionSimple(options).evaluate(types).getFailureReport().getDetails();

		assertReported(details, "InfraOut.infraB", "which only exposes application ring types.");
		assertNotReported(details, "InfraOut.appB");
	}

	@ArchTest // GH-418
	void doesNotRestrictReferrersOutsideOfUnits(JavaClasses types) {

		OnionVerificationOptions options = OnionVerificationOptions.defaults()
				.withExposure(Exposure.values())
				.withReferrers(Referrer.INFRASTRUCTURE);

		List<String> details = JMoleculesArchitectureRules.ensureOnionSimple(options).evaluate(types)
				.getFailureReport().getDetails();

		// Application ring types of a unit must not refer to other units, but the ones outside of any unit may
		assertReported(details, "AppA.appB", "Only infrastructure ring types may refer to other architectural units");
		assertNotReported(details, "AppOut.appB");
	}

	@ArchTest // GH-418
	void appliesLayerRulesToTypesOutsideOfUnits(JavaClasses types) {

		List<String> details = JMoleculesArchitectureRules.ensureOnionSimple().evaluate(types).getFailureReport()
				.getDetails();

		assertThat(details)
				.anySatisfy(it -> assertThat(it).contains("DomainOut.infraOut>").doesNotContain("architectural unit"));

		// References into units are only governed by their exposure, not by the ring rules
		assertNotReported(details, "AppOut.infraB");
	}
}
