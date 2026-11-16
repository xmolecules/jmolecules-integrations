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

import org.jmolecules.architecture.hexagonal.Hexagon;
import org.jmolecules.architecture.onion.Onion;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the exposure and referrer settings of {@link HexagonalVerificationOptions} and
 * {@link OnionVerificationOptions}.
 *
 * @author Oliver Drotbohm
 */
class UnitAwareVerificationOptionsUnitTests {

	@Test // GH-418
	void addsHexagonalExposureAndReferrersToTheConfiguredOnes() {

		HexagonalVerificationOptions options = HexagonalVerificationOptions.defaults()
				.withDirectApplicationAccess()
				.withAdditionalExposure(Hexagon.Exposure.PRIMARY_PORTS)
				.withAdditionalReferrers(Hexagon.Referrer.PRIMARY_ADAPTERS);

		assertThat(options.getExposure()).containsExactlyInAnyOrder(Hexagon.Exposure.PRIMARY_ADAPTERS.name(),
				Hexagon.Exposure.PRIMARY_PORTS.name());
		assertThat(options.getReferrers()).containsExactlyInAnyOrder(Hexagon.Referrer.SECONDARY_ADAPTERS.name(),
				Hexagon.Referrer.PRIMARY_ADAPTERS.name());
		assertThat(options.isDirectApplicationAccess()).isTrue();

		// Adds to explicitly configured settings, which in turn replace previous additions
		assertThat(options.withExposure(Hexagon.Exposure.APPLICATION)
				.withAdditionalExposure(Hexagon.Exposure.PRIMARY_PORTS).getExposure())
						.containsExactlyInAnyOrder(Hexagon.Exposure.APPLICATION.name(), Hexagon.Exposure.PRIMARY_PORTS.name());
	}

	@Test // GH-418
	void addsOnionExposureAndReferrersToTheConfiguredOnes() {

		OnionVerificationOptions options = OnionVerificationOptions.defaults()
				.withAdditionalExposure(Onion.Exposure.APPLICATION)
				.withAdditionalReferrers(Onion.Referrer.APPLICATION);

		assertThat(options.getExposure()).containsExactlyInAnyOrder(Onion.Exposure.INFRASTRUCTURE.name(),
				Onion.Exposure.APPLICATION.name());
		assertThat(options.getReferrers()).containsExactlyInAnyOrder(Onion.Referrer.INFRASTRUCTURE.name(),
				Onion.Referrer.APPLICATION.name());
	}
}
