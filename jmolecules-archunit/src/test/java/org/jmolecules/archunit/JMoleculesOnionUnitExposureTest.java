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
import org.jmolecules.archunit.onion.exposure.client.AppC;
import org.jmolecules.archunit.onion.exposure.client.InfraC;
import org.jmolecules.archunit.onion.exposure.infra.AppI;
import org.jmolecules.archunit.onion.exposure.open.AppO;
import org.jmolecules.archunit.onion.exposure.restricted.AppR;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

/**
 * Tests for the exposure of types declared on {@link org.jmolecules.architecture.onion.Onion} units.
 *
 * @author Oliver Drotbohm
 */
@AnalyzeClasses(packages = "org.jmolecules.archunit.onion.exposure")
class JMoleculesOnionUnitExposureTest {

	@ArchTest // GH-418
	void onlyAllowsReferencesToTypesExposedByUnit(JavaClasses types) {

		assertThat(JMoleculesArchitectureRules.ensureOnionSimple().evaluate(types).getFailureReport().getDetails())
				.satisfiesExactlyInAnyOrder( //
						violation(AppC.class, AppI.class).andThen(it -> assertThat(it)
								.contains("which only exposes infrastructure ring types (to infrastructure ring types).")), //
						violation(InfraC.class, AppI.class).andThen(it -> assertThat(it)
								.contains("which only exposes infrastructure ring types (to infrastructure ring types).")), //
						violation(AppR.class, AppO.class).andThen(it -> assertThat(it)
								.contains("Only infrastructure ring types may refer to other architectural units")));
	}

	@ArchTest // GH-418
	void declaredExposureAndReferrersOverrideGlobalSettings(JavaClasses types) {

		OnionVerificationOptions options = OnionVerificationOptions.defaults()
				.withExposure(Exposure.INFRASTRUCTURE)
				.withReferrers(Referrer.INFRASTRUCTURE);

		// InfraC.appO is allowed as the target unit exposes its application ring explicitly
		assertThat(JMoleculesArchitectureRules.ensureOnionSimple(options).evaluate(types).getFailureReport().getDetails())
				.satisfiesExactlyInAnyOrder( //
						violation(AppC.class, AppI.class).andThen(it -> assertThat(it)
								.contains("Only infrastructure ring types may refer to other architectural units")), //
						violation(InfraC.class, AppI.class), //
						violation(AppR.class, AppO.class));
	}
}
