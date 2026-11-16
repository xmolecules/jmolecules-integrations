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

import java.util.List;
import java.util.stream.Stream;

import org.jmolecules.architecture.hexagonal.Hexagon;
import org.jmolecules.architecture.onion.Onion;
import org.jmolecules.archunit.JMoleculesArchitectureRules.HexagonAnnotation;
import org.jmolecules.archunit.JMoleculesArchitectureRules.OnionAnnotation;
import org.junit.jupiter.api.Test;

/**
 * Unit tests to make sure the role keys the rules use to refer to the {@code exposes} and {@code refersVia} attribute
 * values of {@link Hexagon} and {@link Onion} match the names of the enum constants, which the rules cannot refer to
 * directly as they might not be present on the classpath.
 *
 * @author Oliver Drotbohm
 */
class UnitAnnotationRoleKeysUnitTests {

	@Test // GH-418
	void usesHexagonConstantNamesAsRoleKeys() {

		assertThat(HexagonAnnotation.NAME).isEqualTo(Hexagon.class.getName());

		assertThat(List.of(HexagonAnnotation.EXPOSES_PRIMARY_ADAPTERS, HexagonAnnotation.EXPOSES_PRIMARY_PORTS,
				HexagonAnnotation.EXPOSES_APPLICATION, HexagonAnnotation.EXPOSES_SECONDARY_PORTS))
				.containsExactlyInAnyOrderElementsOf(names(Hexagon.Exposure.values()));

		assertThat(List.of(HexagonAnnotation.REFERS_VIA_SECONDARY_ADAPTERS, HexagonAnnotation.REFERS_VIA_APPLICATION,
				HexagonAnnotation.REFERS_VIA_PRIMARY_ADAPTERS, HexagonAnnotation.REFERS_VIA_PRIMARY_PORTS,
				HexagonAnnotation.REFERS_VIA_SECONDARY_PORTS))
				.containsExactlyInAnyOrderElementsOf(names(Hexagon.Referrer.values()));
	}

	@Test // GH-418
	void usesOnionConstantNamesAsRoleKeys() {

		assertThat(OnionAnnotation.NAME).isEqualTo(Onion.class.getName());

		assertThat(List.of(OnionAnnotation.EXPOSES_INFRASTRUCTURE, OnionAnnotation.EXPOSES_APPLICATION))
				.containsExactlyInAnyOrderElementsOf(names(Onion.Exposure.values()));

		assertThat(List.of(OnionAnnotation.REFERS_VIA_INFRASTRUCTURE, OnionAnnotation.REFERS_VIA_APPLICATION))
				.containsExactlyInAnyOrderElementsOf(names(Onion.Referrer.values()));
	}

	private static List<String> names(Enum<?>... values) {
		return Stream.of(values).map(Enum::name).toList();
	}
}
