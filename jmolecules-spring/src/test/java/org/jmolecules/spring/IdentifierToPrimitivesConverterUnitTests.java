/*
 * Copyright 2021-2024 the original author or authors.
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
package org.jmolecules.spring;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.jmolecules.ddd.types.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;

class IdentifierToPrimitivesConverterUnitTests {

	static final TypeDescriptor UUID_DESCRIPTOR = TypeDescriptor.valueOf(UUID.class);
	static final TypeDescriptor STRING_DESCRIPTOR = TypeDescriptor.valueOf(String.class);
	static final TypeDescriptor IDENTIFIER_DESCRIPTOR = TypeDescriptor.valueOf(SampleIdentifier.class);

	ConversionService conversionService;
	IdentifierToPrimitivesConverter converter;

	@BeforeEach
	void setUp() {

		DefaultConversionService conversionService = new DefaultConversionService();
		this.converter = new IdentifierToPrimitivesConverter(() -> conversionService);

		conversionService.addConverter(converter);
		this.conversionService = conversionService;
	}

	@Test // #16
	void convertsToPrimitiveSources() {

		UUID uuid = UUID.randomUUID();
		SampleIdentifier identifier = SampleIdentifier.of(uuid);

		assertThat(conversionService.convert(identifier, UUID.class)).isEqualTo(uuid);
		assertThat(conversionService.convert(identifier, String.class)).isEqualTo(uuid.toString());
	}

	@Test // #16
	void matchesPrimitives() {

		assertThat(converter.matches(IDENTIFIER_DESCRIPTOR, UUID_DESCRIPTOR)).isTrue();
		assertThat(converter.matches(IDENTIFIER_DESCRIPTOR, STRING_DESCRIPTOR)).isTrue();
		assertThat(converter.matches(IDENTIFIER_DESCRIPTOR, TypeDescriptor.valueOf(Long.class))).isFalse();
	}

	@Test // #16
	void handlesNullValues() {

		assertThat(converter.convert(null, IDENTIFIER_DESCRIPTOR, UUID_DESCRIPTOR)).isNull();
		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
			converter.convert(SampleIdentifier.of(null), IDENTIFIER_DESCRIPTOR, UUID_DESCRIPTOR);
		});
	}

	@Test // #71
	void detectsIdentifierFieldInSuperClass() {

		ConcreteIdentifier identifier = new ConcreteIdentifier();
		identifier.id = UUID.randomUUID();

		assertThat(converter.convert(identifier, TypeDescriptor.forObject(identifier), UUID_DESCRIPTOR))
				.isEqualTo(identifier.id);
	}

	@Test // #86
	void generallyMatchesIdentifierToString() {

		assertThat(converter.matches(TypeDescriptor.valueOf(Identifier.class), TypeDescriptor.valueOf(String.class)))
				.isTrue();
	}

	static abstract class IdentifierBase implements Identifier {
		UUID id;
	}

	static class ConcreteIdentifier extends IdentifierBase {

	}
}
