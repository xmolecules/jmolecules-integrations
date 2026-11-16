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

import org.jmolecules.architecture.onion.Onion;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

/**
 * Tests for the {@code additionallyExposes} and {@code additionallyRefersVia} attributes of {@link Onion}.
 *
 * @author Oliver Drotbohm
 */
@AnalyzeClasses(packages = "org.jmolecules.archunit.onion.additional")
class JMoleculesOnionAdditionalAccessTest {

	@ArchTest // GH-418
	void addsDeclaredRolesToTheGloballyConfiguredOnes(JavaClasses types) {

		List<String> details = ensureOnionSimple().evaluate(types).getFailureReport().getDetails();

		// Application ring additionally exposed by the provider, and allowed to refer by the consumer
		assertNotReported(details, "AppConsumer.appP");

		// Globally configured roles remain in place
		assertReported(details, "AppConsumer.infraP", "which only exposes");
		assertReported(details, "AppRestricted.appP",
				"Only infrastructure ring types may refer to other architectural units");
	}
}
