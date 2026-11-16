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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jmolecules.architecture.hexagonal.Hexagon.Exposure;
import org.jmolecules.architecture.hexagonal.Hexagon.Referrer;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

/**
 * Tests for the individual {@link Referrer}s of the Hexagonal Architecture.
 *
 * @author Oliver Drotbohm
 */
@AnalyzeClasses(packages = "org.jmolecules.archunit.hexagonal.referrers")
class JMoleculesHexagonalReferrersTest {

	@ArchTest // GH-418
	void onlyAllowsConfiguredReferrersToReferToOtherUnits(JavaClasses types) {

		// The types of unit Origin referring to unit Referenced, by the referrer needed for them to be accepted
		Map<Referrer, String> origins = new EnumMap<>(Referrer.class);
		origins.put(Referrer.APPLICATION, "OriginApp");
		origins.put(Referrer.PRIMARY_ADAPTERS, "OriginPrimaryAdapter");
		origins.put(Referrer.SECONDARY_ADAPTERS, "OriginSecondaryAdapter");
		origins.put(Referrer.PRIMARY_PORTS, "OriginPrimaryPort");
		origins.put(Referrer.SECONDARY_PORTS, "OriginSecondaryPort");

		assertThat(origins.keySet()).containsExactlyInAnyOrder(Referrer.values());

		for (Referrer allowed : Referrer.values()) {

			HexagonalVerificationOptions options = HexagonalVerificationOptions.defaults()
					.withExposure(Exposure.APPLICATION)
					.withReferrers(allowed);

			List<String> details = ensureHexagonal(options).evaluate(types).getFailureReport().getDetails();

			origins.forEach((referrer, origin) -> {

				String reference = "origin." + origin + ".referenced";

				if (referrer == allowed) {
					assertThat(details).as(reference).noneSatisfy(it -> assertThat(it).contains(reference));
				} else {
					assertThat(details).as(reference).anySatisfy(
							it -> assertThat(it).contains(reference).contains("may refer to other architectural units"));
				}
			});
		}
	}
}
