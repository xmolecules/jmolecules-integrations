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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jmolecules.architecture.onion.Onion.Exposure;
import org.jmolecules.architecture.onion.Onion.Referrer;
import org.jmolecules.archunit.onion.units.a.AppA;
import org.jmolecules.archunit.onion.units.a.InfraA;
import org.jmolecules.archunit.onion.units.b.AppB;
import org.jmolecules.archunit.onion.units.b.DomainB;
import org.jmolecules.archunit.onion.units.b.HelperB;
import org.jmolecules.archunit.onion.units.c.PlainC;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Unit tests for the verification of {@link org.jmolecules.architecture.onion.Onion} unit boundaries.
 *
 * @author Oliver Drotbohm
 */
@AnalyzeClasses(packages = "org.jmolecules.archunit.onion.units")
@SuppressWarnings("removal") // also covers the deprecated entry points until they are removed
class JMoleculesOnionArchitecturalUnitRulesTest {

	@ArchTest // GH-418
	void onlyAllowsInfrastructureRingToReferToOtherUnitsByDefault(JavaClasses types) {

		assertThat(JMoleculesArchitectureRules.ensureOnionSimple().evaluate(types).getFailureReport().getDetails())
				.satisfiesExactlyInAnyOrder( //
						violation(AppA.class, DomainB.class), //
						violation(AppA.class, AppB.class), //
						violation(AppA.class, PlainC.class), //
						violation(InfraA.class, AppB.class), //
						violation(InfraA.class, DomainB.class), //
						violation(InfraA.class, HelperB.class), //
						violation(PlainC.class, AppB.class), //
						violation(PlainC.class, DomainB.class));
	}

	@ArchTest // GH-418
	void allowsApplicationRingAccessBetweenUnitsIfConfigured(JavaClasses types) {

		OnionVerificationOptions options = OnionVerificationOptions.defaults()
				.withExposure(Exposure.values())
				.withReferrers(Referrer.values());

		assertThat(JMoleculesArchitectureRules.ensureOnionSimple(options).evaluate(types).getFailureReport().getDetails())
				.satisfiesExactlyInAnyOrder( //
						violation(AppA.class, DomainB.class), //
						violation(InfraA.class, DomainB.class), //
						violation(InfraA.class, HelperB.class), //
						violation(PlainC.class, DomainB.class)) //
				.allSatisfy(it -> assertThat(it).contains("Onion B"));
	}

	@ArchTest // GH-418
	void onlyVerifiesTypesInScope(JavaClasses types) {

		// Unit c is out of scope, so its references are ignored
		OnionVerificationOptions options = OnionVerificationOptions.defaults()
				.withExposure(Exposure.values())
				.withReferrers(Referrer.values())
				.withScope(JavaClass.Predicates.resideInAnyPackage("org.jmolecules.archunit.onion.units.a",
						"org.jmolecules.archunit.onion.units.b"));

		assertThat(JMoleculesArchitectureRules.ensureOnionSimple(options).evaluate(types).getFailureReport().getDetails())
				.satisfiesExactlyInAnyOrder( //
						violation(AppA.class, DomainB.class), //
						violation(InfraA.class, DomainB.class), //
						violation(InfraA.class, HelperB.class));
	}

	@ArchTest // GH-418
	void ignoresUnitsNotUsingTheVerifiedVariant(JavaClasses types) {

		// No type uses classical rings, so no references between units are restricted
		assertThat(JMoleculesArchitectureRules.ensureOnionClassical().evaluate(types).hasViolation()).isFalse();
	}
	@ArchTest // GH-418
	void exposureCustomizationsReportReferencesToTypesOfOtherRings(JavaClasses types) {

		// Fields referring to a type of unit B, by the exposure that is needed for them to be accepted
		Map<Exposure, List<String>> fields = new EnumMap<>(Exposure.class);
		fields.put(Exposure.APPLICATION, List.of("AppA.appB", "InfraA.appB", "PlainC.appB"));
		fields.put(Exposure.INFRASTRUCTURE, List.of("InfraA.infraB", "PlainC.infraB"));

		Map<Exposure, String> descriptions = new EnumMap<>(Exposure.class);
		descriptions.put(Exposure.APPLICATION, "which only exposes application ring types.");
		descriptions.put(Exposure.INFRASTRUCTURE,
				"which only exposes infrastructure ring types (to infrastructure ring types).");

		for (Exposure exposed : Exposure.values()) {

			OnionVerificationOptions options = OnionVerificationOptions.defaults()
					.withExposure(exposed)
					.withReferrers(Referrer.values());

			List<String> details = JMoleculesArchitectureRules.ensureOnionSimple(options).evaluate(types)
					.getFailureReport().getDetails();

			fields.forEach((role, names) -> names.forEach(name -> {

				if (role == exposed) {
					assertNotReported(details, name);
				} else {
					assertReported(details, name, descriptions.get(exposed));
				}
			}));
		}
	}

	@ArchTest // GH-418
	void referrerCustomizationsReportReferencesFromTypesOfOtherRings(JavaClasses types) {

		// Fields referring to a type of another unit, by the referrer that is needed for them to be accepted
		Map<Referrer, List<String>> fields = new EnumMap<>(Referrer.class);
		fields.put(Referrer.APPLICATION, List.of("AppA.appB", "AppA.plainC"));
		fields.put(Referrer.INFRASTRUCTURE, List.of("InfraA.infraB", "InfraA.appB", "InfraA.plainC"));

		Map<Referrer, String> descriptions = new EnumMap<>(Referrer.class);
		descriptions.put(Referrer.APPLICATION,
				"Only application ring types (including types not assigned to a ring) may refer to other architectural units");
		descriptions.put(Referrer.INFRASTRUCTURE, "Only infrastructure ring types may refer to other architectural units");

		for (Referrer allowed : Referrer.values()) {

			OnionVerificationOptions options = OnionVerificationOptions.defaults()
					.withExposure(Exposure.values())
					.withReferrers(allowed);

			List<String> details = JMoleculesArchitectureRules.ensureOnionSimple(options).evaluate(types)
					.getFailureReport().getDetails();

			fields.forEach((role, names) -> names.forEach(name -> {

				if (role == allowed) {
					assertNotReported(details, name);
				} else {
					assertReported(details, name, descriptions.get(allowed));
				}
			}));

			// Origins in units not using rings are not restricted in what they refer to
			assertNotReported(details, "PlainC.infraB");
			assertNotReported(details, "PlainC.appB");
		}
	}

	@ArchTest // GH-418
	void directApplicationAccessOnlyOpensApplicationRingToApplicationRing(JavaClasses types) {

		OnionVerificationOptions options = OnionVerificationOptions.defaults().withDirectApplicationAccess();

		// Exposure and referrers stay untouched
		assertThat(options.getExposure()).containsExactly(Exposure.INFRASTRUCTURE.name());
		assertThat(options.getReferrers()).containsExactly(Referrer.INFRASTRUCTURE.name());

		List<String> details = JMoleculesArchitectureRules.ensureOnionSimple(options).evaluate(types)
				.getFailureReport().getDetails();

		assertNotReported(details, "AppA.appB");
		assertNotReported(details, "InfraA.infraB");
		assertReported(details, "InfraA.appB", "which only exposes infrastructure ring types");

		// Explicitly exposed rings are available to all referrers
		details = JMoleculesArchitectureRules.ensureOnionSimple(options.withExposure(Exposure.values()))
				.evaluate(types).getFailureReport().getDetails();

		assertNotReported(details, "InfraA.appB");
	}
}
