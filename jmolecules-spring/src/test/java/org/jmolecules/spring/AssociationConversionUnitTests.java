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

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.function.Supplier;

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Identifier;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.ReflectionUtils;

class AssociationConversionUnitTests<T extends AggregateRoot<T, ID>, ID extends Identifier<T, ID>> {

	Supplier<ConversionService> conversionService = () -> DefaultConversionService.getSharedInstance();
	AssociationToPrimitivesConverter<T, ID> writing = new AssociationToPrimitivesConverter<>(conversionService);
	PrimitivesToAssociationConverter<?, ?> reading = new PrimitivesToAssociationConverter<>(conversionService);

	UUID uuid = UUID.randomUUID();
	SampleAggregateIdentifier identifier = SampleAggregateIdentifier.of(uuid);
	SampleAssociation association = SampleAssociation.of(identifier);

	TypeDescriptor identifierDescriptor = TypeDescriptor.valueOf(SampleAggregateIdentifier.class);
	TypeDescriptor associationDescriptor = TypeDescriptor.valueOf(SampleAssociation.class);
	TypeDescriptor objectDescriptor = TypeDescriptor.valueOf(Object.class);
	TypeDescriptor uuidDescriptor = TypeDescriptor.valueOf(UUID.class);

	Field field = ReflectionUtils.findField(Sample.class, "association");
	TypeDescriptor genericAssociationDescription = TypeDescriptor.nested(field, 0);

	@Test // #20
	void writesDedicatedAssociationType() {
		assertThat(writing.convert(association, TypeDescriptor.forObject(association),
				TypeDescriptor.valueOf(Object.class))).isEqualTo(uuid);
	}

	@Test // #20
	void writesGenericAssociation() {

		Association<SampleAggregate, SampleAggregateIdentifier> simpleAssociation = () -> identifier;

		assertThat(writing.convert(simpleAssociation, associationDescriptor, objectDescriptor)).isEqualTo(uuid);
	}

	@Test // #20
	void convertsToIdentifierIfExplicitlyRequested() {
		assertThat(writing.convert(association, associationDescriptor, identifierDescriptor)).isEqualTo(identifier);
	}

	@Test // #20
	void convertsNullAssociationToNull() {
		assertThat(writing.convert(null, associationDescriptor, objectDescriptor)).isNull();
	}

	@Test // #20
	void rejectsInvalidAssociationsOnWrite() {

		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
			Association<SampleAggregate, SampleAggregateIdentifier> invalidAssociation = () -> null;
			writing.convert(invalidAssociation, associationDescriptor, objectDescriptor);
		});
	}

	@Test // #20
	void readsPrimitiveIntoGenericAssociation() {

		assertThat(reading.convert(uuid, uuidDescriptor, genericAssociationDescription))
				.isInstanceOfSatisfying(Association.class, it -> {
					assertThat(it.getId()).isEqualTo(identifier);
				});
	}

	@Test // #20
	void readsPrimitiveIntoCustomAssociation() {
		assertThat(reading.convert(uuid, uuidDescriptor, associationDescriptor)).isNotNull();
	}

	@Test // #20
	void readsIdentifierIntoGenericAssociation() {
		assertThat(reading.convert(identifier, identifierDescriptor, genericAssociationDescription))
				.isInstanceOfSatisfying(Association.class, it -> {
					assertThat(it.getId()).isEqualTo(identifier);
				});
	}

	@Test // #20
	void reasdIdentifierIntoCustomAssociation() {
		assertThat(reading.convert(identifier, identifierDescriptor, associationDescriptor)).isEqualTo(association);
	}

	@Test // #20
	void readsNullIntoNullAssociation() {
		assertThat(reading.convert(null, uuidDescriptor, associationDescriptor)).isNull();
	}

	@Value(staticConstructor = "of")
	static class SampleAggregateIdentifier implements Identifier<SampleAggregate, SampleAggregateIdentifier> {
		UUID uuid;
	}

	@Value(staticConstructor = "of")
	static class SampleAggregate implements AggregateRoot<SampleAggregate, SampleAggregateIdentifier> {
		SampleAggregateIdentifier id;
	}

	@Value(staticConstructor = "of")
	static class SampleAssociation implements Association<SampleAggregate, SampleAggregateIdentifier> {
		SampleAggregateIdentifier id;
	}

	static class Sample {
		Association<SampleAggregate, SampleAggregateIdentifier> association;
	}
}
