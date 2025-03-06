/*
 * Copyright 2021-2025 the original author or authors.
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
package org.jmolecules.spring.jpa;

import static org.assertj.core.api.Assertions.*;

import lombok.Value;

import java.util.UUID;

import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Identifier;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AssociationAttributeConverter}.
 *
 * @author Oliver Drotbohm
 */
class JpaAssociationAttributeConverterTests {

	JpaAssociationAttributeConverter<?, SampleIdentifier, UUID> converter = new JpaAssociationAttributeConverter<>(
			SampleIdentifier.class);

	UUID uuid = UUID.randomUUID();
	SampleIdentifier identifier = SampleIdentifier.of(uuid);

	@Test // #27
	void convertsAssociationIntoPrimtive() {
		assertThat(converter.convertToDatabaseColumn(Association.forId(identifier))).isEqualTo(uuid);
	}

	@Test // #27
	void convertsPrimtiveIntoAssociation() {
		assertThat(converter.convertToEntityAttribute(uuid)).isEqualTo(Association.forId(identifier));
	}

	@Value(staticConstructor = "of")
	static class SampleIdentifier implements Identifier {
		UUID id;
	}
}
