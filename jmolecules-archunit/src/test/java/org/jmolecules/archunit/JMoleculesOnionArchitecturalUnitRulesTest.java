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
	void allowsApplicationRingAccessBetweenUnitsByDefault(JavaClasses types) {

		assertThat(JMoleculesArchitectureRules.ensureOnionSimple().evaluate(types).getFailureReport().getDetails())
				.satisfiesExactlyInAnyOrder( //
						violation(AppA.class, DomainB.class), //
						violation(InfraA.class, DomainB.class), //
						violation(InfraA.class, HelperB.class), //
						violation(PlainC.class, DomainB.class)) //
				.allSatisfy(it -> assertThat(it).contains("Onion B"));
	}

	@ArchTest // GH-418
	void onlyAllowsInfrastructureRingToReferToOtherUnitsInStrictMode(JavaClasses types) {

		OnionVerificationOptions options = OnionVerificationOptions.defaults()
				.withExposure(Exposure.INFRASTRUCTURE)
				.withReferrers(Referrer.INFRASTRUCTURE);

		ArchRule rule = JMoleculesArchitectureRules.ensureOnionSimple(options);

		assertThat(rule.evaluate(types).getFailureReport().getDetails())
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
	void onlyVerifiesTypesInScope(JavaClasses types) {

		// Unit c is out of scope, so its references are ignored
		OnionVerificationOptions options = OnionVerificationOptions.defaults()
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
}
