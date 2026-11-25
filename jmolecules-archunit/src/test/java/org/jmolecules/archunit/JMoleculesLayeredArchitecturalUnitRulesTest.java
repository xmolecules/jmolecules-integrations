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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jmolecules.architecture.layered.Layered.Exposure;
import org.jmolecules.architecture.layered.Layered.Referrer;
import org.jmolecules.archunit.layeredunits.a.AppA;
import org.jmolecules.archunit.layeredunits.a.DomainA;
import org.jmolecules.archunit.layeredunits.a.InfraA;
import org.jmolecules.archunit.layeredunits.a.InterfaceA;
import org.jmolecules.archunit.layeredunits.b.AppB;
import org.jmolecules.archunit.layeredunits.b.InfraB;
import org.jmolecules.archunit.layeredunits.b.InterfaceB;
import org.jmolecules.archunit.layeredunits.none.PlainNone;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Unit tests for the verification of {@link org.jmolecules.architecture.layered.Layered} unit boundaries.
 *
 * @author Oliver Drotbohm
 */
@AnalyzeClasses(packages = "org.jmolecules.archunit.layeredunits")
@SuppressWarnings("removal") // also covers the deprecated entry points until they are removed
class JMoleculesLayeredArchitecturalUnitRulesTest {

	@ArchTest // GH-418
	void onlyAllowsApplicationAndDomainLayersToReferToEachOtherByDefault(JavaClasses types) {

		LayeredVerificationOptions defaults = LayeredVerificationOptions.defaults();

		assertThat(defaults.getExposure()).containsExactlyInAnyOrder(Exposure.APPLICATION.name(), Exposure.DOMAIN.name());
		assertThat(defaults.getReferrers()).containsExactlyInAnyOrder(Referrer.APPLICATION.name(),
				Referrer.DOMAIN.name());

		Stream.of(ensureLayering(defaults), ensureLayeringStrict(defaults)).forEach(rule -> {

			List<String> details = details(rule, types);

			assertThat(details).satisfiesExactlyInAnyOrder(defaultViolations());
			assertReported(details, "AppA.interfaceB", "which only exposes application layer types and domain layer types.");
			assertReported(details, "InfraA.appB",
					"Only application layer types (including types not assigned to a layer) and domain layer types may refer");
			assertNotReported(details, "AppA.appB");
			assertNotReported(details, "DomainA.domainB");
			assertNotReported(details, "PlainNone.appB");
		});
	}

	@ArchTest // GH-418
	void doesNotConsiderUnitsInDeprecatedEntryPoints(JavaClasses types) {

		// Globally, the interface layer must not be accessed by any other layer
		assertReported(details(ensureLayering(), types), "InfraA.interfaceB", "");
		assertReported(details(ensureLayeringStrict(), types), "InfraA.interfaceB", "");
	}

	@ArchTest // GH-418
	void grantsDirectApplicationAccessIndependentlyOfReferrers(JavaClasses types) {

		LayeredVerificationOptions options = LayeredVerificationOptions.defaults().withReferrers(Referrer.INTERFACE);

		List<String> details = details(ensureLayering(options), types);

		assertReported(details, "AppA.appB", "Only interface layer types may refer");
		assertNotReported(details, "InterfaceA.appB");

		details = details(ensureLayering(options.withDirectApplicationAccess()), types);

		assertNotReported(details, "AppA.appB");
		assertNotReported(details, "InterfaceA.appB");
		assertReported(details, "AppA.interfaceB",
				"Only interface layer types, as well as application layer types (to application layer types), may refer");
		assertReported(details, "DomainA.domainB", "may refer to other architectural units");
	}

	@ArchTest // GH-418
	void allowsAdditionalLayersToBeExposedAndToReferToOtherUnits(JavaClasses types) {

		LayeredVerificationOptions options = LayeredVerificationOptions.defaults()
				.withAdditionalExposure(Exposure.INFRASTRUCTURE)
				.withAdditionalReferrers(Referrer.INTERFACE, Referrer.INFRASTRUCTURE);

		List<String> details = details(ensureLayering(options), types);

		assertNotReported(details, "AppA.infraB");
		assertNotReported(details, "InterfaceA.appB");
		assertNotReported(details, "InfraA.appB");
		assertNotReported(details, "DomainA.domainB");
		assertReported(details, "InfraA.interfaceB",
				"which only exposes application layer types, domain layer types and infrastructure layer types");

		// All layers exposed and allowed to refer to other units
		details = details(ensureLayering(LayeredVerificationOptions.defaults()
				.withExposure(Exposure.values())
				.withReferrers(Referrer.values())), types);

		assertThat(details).allSatisfy(it -> assertThat(it).doesNotContain("architectural unit"));
		assertReported(details, "InfraA.appA", "");
	}

	@ArchTest // GH-418
	void onlyVerifiesReferencesBetweenUnitsIfRequested(JavaClasses types) {

		LayeredVerificationOptions options = LayeredVerificationOptions.defaults();

		List<String> crossUnit = details(ensureLayering(options), types).stream()
				.filter(it -> it.contains("architectural unit"))
				.collect(Collectors.toList());

		List<String> details = details(ensureLayeringUnitBoundaries(options), types);

		assertThat(details).isNotEmpty().containsExactlyInAnyOrderElementsOf(crossUnit);
		assertNotReported(details, "InfraA.appA");
	}

	@SuppressWarnings("unchecked")
	private static Consumer<String>[] defaultViolations() {

		return new Consumer[] { //
				violation(InterfaceA.class, AppB.class), //
				violation(AppA.class, InfraB.class), //
				violation(AppA.class, InterfaceB.class), //
				violation(InfraA.class, InterfaceB.class), //
				violation(InfraA.class, AppB.class), //
				violation(InfraA.class, AppA.class), //
				violation(PlainNone.class, InterfaceB.class) //
		};
	}

	private static List<String> details(ArchRule rule, JavaClasses types) {
		return rule.evaluate(types).getFailureReport().getDetails();
	}
}
