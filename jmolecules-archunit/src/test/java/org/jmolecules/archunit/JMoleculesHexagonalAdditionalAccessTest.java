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

import static org.jmolecules.archunit.JMoleculesArchitectureRules.*;
import static org.jmolecules.archunit.TestUtils.*;

import java.util.List;

import org.jmolecules.architecture.hexagonal.Hexagon;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

/**
 * Tests for the {@code additionallyExposes} and {@code additionallyRefersVia} attributes of {@link Hexagon}.
 *
 * @author Oliver Drotbohm
 */
@AnalyzeClasses(packages = "org.jmolecules.archunit.hexagonal.additional")
class JMoleculesHexagonalAdditionalAccessTest {

	@ArchTest // GH-418
	void addsDeclaredRolesToTheGloballyConfiguredOnes(JavaClasses types) {

		List<String> details = ensureHexagonal().evaluate(types).getFailureReport().getDetails();

		// Secondary ports additionally exposed by the provider, application code additionally allowed to refer
		assertNotReported(details, "ConsumerApp.providerRepo");
		assertNotReported(details, "ConsumerSecondary.providerRepo");

		// Globally configured roles remain in place
		assertNotReported(details, "ConsumerSecondary.providerAdapter");
		assertReported(details, "ConsumerApp.providerPort", "which only exposes primary adapters (to secondary adapters)");
		assertReported(details, "ConsumerApp.providerApp", "which only exposes primary adapters (to secondary adapters)");
		assertReported(details, "ConsumerSecondary.providerPort",
				"which only exposes primary adapters (to secondary adapters)");

		// Not declared on the unit
		assertReported(details, "RestrictedApp.providerRepo",
				"Only secondary adapters may refer to other architectural units");
	}

	@ArchTest // GH-418
	void addsDeclaredRolesToTheOnesDeclaredViaExposes(JavaClasses types) {

		List<String> details = ensureHexagonal().evaluate(types).getFailureReport().getDetails();

		// exposes replaces the global default, additionallyExposes adds to it
		assertNotReported(details, "ConsumerApp.replacedPort");
		assertNotReported(details, "ConsumerApp.replacedApp");
		assertReported(details, "ConsumerApp.replacedAdapter", "which only exposes application code and primary ports");
	}
}
