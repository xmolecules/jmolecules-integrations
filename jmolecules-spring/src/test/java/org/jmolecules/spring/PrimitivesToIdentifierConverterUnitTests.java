/*
 * Copyright 2021 the original author or authors.
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

import lombok.Value;

import java.util.UUID;

import org.jmolecules.ddd.types.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

class PrimitivesToIdentifierConverterUnitTests {

	static final TypeDescriptor UUID_DESCRIPTOR = TypeDescriptor.valueOf(UUID.class);
	static final TypeDescriptor STRING_DESCRIPTOR = TypeDescriptor.valueOf(String.class);
	static final TypeDescriptor IDENTIFIER_DESCRIPTOR = TypeDescriptor.valueOf(SampleIdentifier.class);
	static final TypeDescriptor LONG_DESCRIPTOR = TypeDescriptor.valueOf(Long.class);

	ConfigurableConversionService conversionService;
	PrimitivesToIdentifierConverter converter;

	@BeforeEach
	void setUp() {

		this.conversionService = new DefaultConversionService();
		this.converter = new PrimitivesToIdentifierConverter(() -> conversionService);
		this.conversionService.addConverter(converter);
	}

	@Test // #16
	void convertsFromPrimitiveSources() {

		UUID uuid = UUID.randomUUID();

		assertThat(conversionService.convert(uuid, SampleIdentifier.class).getId()).isEqualTo(uuid);
		assertThat(conversionService.convert(uuid.toString(), SampleIdentifier.class).getId()).isEqualTo(uuid);

		assertThatExceptionOfType(ConverterNotFoundException.class)
				.isThrownBy(() -> converter.convert(1L, LONG_DESCRIPTOR, IDENTIFIER_DESCRIPTOR));
	}

	@Test // #16
	void matchesPrimitives() {

		assertThat(converter.matches(UUID_DESCRIPTOR, IDENTIFIER_DESCRIPTOR)).isTrue();
		assertThat(converter.matches(STRING_DESCRIPTOR, IDENTIFIER_DESCRIPTOR)).isTrue();
		assertThat(converter.matches(TypeDescriptor.valueOf(Long.class), IDENTIFIER_DESCRIPTOR)).isFalse();
	}

	@Test // #16
	void handlesNullValues() {
		assertThat(converter.convert(null, UUID_DESCRIPTOR, IDENTIFIER_DESCRIPTOR)).isNull();
	}

	@Test // #46
	void handlesIdentifierWithoutFactoryMethod() {

		TypeDescriptor descriptor = TypeDescriptor.valueOf(IdentifierWithoutFactoryMethod.class);

		assertThat(converter.matches(STRING_DESCRIPTOR, descriptor)).isTrue();
		assertThat(converter.matches(UUID_DESCRIPTOR, descriptor)).isTrue();
		assertThat(converter.matches(TypeDescriptor.valueOf(Long.class), descriptor)).isFalse();

		UUID uuid = UUID.randomUUID();
		IdentifierWithoutFactoryMethod expected = new IdentifierWithoutFactoryMethod(uuid);

		assertThat(converter.convert(uuid, UUID_DESCRIPTOR, descriptor)).isEqualTo(expected);
		assertThat(converter.convert(uuid.toString(), STRING_DESCRIPTOR, descriptor)).isEqualTo(expected);
	}

	@Value
	static class IdentifierWithoutFactoryMethod implements Identifier {
		UUID id;
	}
}
