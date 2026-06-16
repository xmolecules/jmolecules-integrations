/*
 * Copyright 2025 the original author or authors.
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
package org.jmolecules.jackson;

import static org.assertj.core.api.Assertions.*;

import lombok.Value;

import java.util.UUID;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;
import org.junit.jupiter.api.Test;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;

/**
 * @author Hasan Kara
 */
class SingleValueWrappingOpenApiModelConverterUnitTests {

	ModelConverters converters = new ModelConverters();

	SingleValueWrappingOpenApiModelConverterUnitTests() {
		converters.addConverter(new SingleValueWrappingOpenApiModelConverter());
	}

	@Test // GH-310
	void flattensIdentifierToInnerType() {

		Schema<?> schema = resolveSchema(SampleIdentifier.class);

		assertThat(schema.getType()).isEqualTo("string");
		assertThat(schema.getFormat()).isEqualTo("uuid");
		assertThat(schema.getProperties()).isNull();
	}

	@Test // GH-310
	void flattensAnnotatedValueObjectToInnerType() {

		Schema<?> schema = resolveSchema(SampleValueObject.class);

		assertThat(schema.getType()).isEqualTo("integer");
		assertThat(schema.getFormat()).isEqualTo("int64");
		assertThat(schema.getProperties()).isNull();
	}

	@Test // GH-310
	void flattensImplementingValueObjectToInnerType() {

		Schema<?> schema = resolveSchema(ImplementingValueObject.class);

		assertThat(schema.getType()).isEqualTo("integer");
		assertThat(schema.getFormat()).isEqualTo("int64");
		assertThat(schema.getProperties()).isNull();
	}

	@Test // GH-310
	void doesNotFlattenMultiPropertyValueObject() {

		Schema<?> schema = resolveSchema(MultiPropertyValueObject.class);

		assertThat(schema.getProperties()).containsKey("name");
		assertThat(schema.getProperties()).containsKey("number");
	}

	@Test // GH-310
	void flattensRecordIdentifierToInnerType() {

		Schema<?> schema = resolveSchema(RecordIdentifier.class);

		assertThat(schema.getType()).isEqualTo("string");
		assertThat(schema.getFormat()).isEqualTo("uuid");
		assertThat(schema.getProperties()).isNull();
	}

	@Test // GH-310
	void flattensRecordValueObjectToInnerType() {

		Schema<?> schema = resolveSchema(RecordValueObject.class);

		assertThat(schema.getType()).isEqualTo("integer");
		assertThat(schema.getFormat()).isEqualTo("int64");
		assertThat(schema.getProperties()).isNull();
	}

	@Test // GH-310
	void doesNotFlattenNonValueObjectType() {

		Schema<?> schema = resolveSchema(PlainPojo.class);

		assertThat(schema.getProperties()).containsKey("name");
	}

	private Schema<?> resolveSchema(Class<?> type) {
		return converters.resolveAsResolvedSchema(new AnnotatedType(type)).schema;
	}

	// -- Test types --

	@Value(staticConstructor = "of")
	static class SampleIdentifier implements Identifier {
		UUID id;
	}

	@ValueObject
	@Value(staticConstructor = "of")
	static class SampleValueObject {
		Long number;
	}

	@Value(staticConstructor = "of")
	static class ImplementingValueObject implements org.jmolecules.ddd.types.ValueObject {
		Long value;
	}

	@ValueObject
	@Value
	static class MultiPropertyValueObject {
		String name;
		Long number;
	}

	record RecordIdentifier(UUID id) implements Identifier {}

	@ValueObject
	record RecordValueObject(Long number) {}

	@Value
	static class PlainPojo {
		String name;
	}
}
