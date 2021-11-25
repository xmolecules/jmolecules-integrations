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
package org.jmolecules.jackson;

import static org.assertj.core.api.Assertions.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.UUID;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Identifier;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

class JMoleculesModuleUnitTests {

	ObjectMapper mapper = new ObjectMapper().registerModule(new JMoleculesModule());

	@Test // #19, #79, #78
	void serialize() throws Exception {

		SampleIdentifier identifier = SampleIdentifier.of(UUID.randomUUID());
		SampleIdentifierWithConstructor identifierWithConstructor = new SampleIdentifierWithConstructor(UUID.randomUUID());
		SampleValueObject valueObject = SampleValueObject.of(42L);
		ImplementingValueObject implementingValueObject = ImplementingValueObject.of(27L);

		Document source = new Document(identifier, identifierWithConstructor, valueObject, implementingValueObject,
				Association.forId(identifier));

		String result = mapper.writeValueAsString(source);

		DocumentContext document = JsonPath.parse(result);

		assertThat(document.read("$.identifier", String.class)).isEqualTo(identifier.getId().toString());
		assertThat(document.read("$.identifierWithConstructor", String.class))
				.isEqualTo(identifierWithConstructor.getId().toString());
		assertThat(document.read("$.valueObject", Long.class)).isEqualTo(42L);
		assertThat(document.read("$.implementingValueObject", Long.class)).isEqualTo(27L);
		assertThat(document.read("$.association", String.class)).isEqualTo(identifier.getId().toString());
	}

	@Test // #19, #79, #78
	void deserialize() throws Exception {

		String uuidSource = "fe6f3370-5551-4251-86d3-b4db049a7ddd";
		UUID uuid = UUID.fromString(uuidSource);

		Document document = mapper.readValue("{ \"identifier\" : \"" + uuidSource + "\","
				+ " \"identifierWithConstructor\" : \"" + uuidSource + "\","
				+ " \"valueObject\" : 42,"
				+ " \"implementingValueObject\" : 27,"
				+ " \"association\" : \"" + uuidSource + "\" }",
				Document.class);

		assertThat(document.identifier).isEqualTo(SampleIdentifier.of(uuid));
		assertThat(document.valueObject).isEqualTo(SampleValueObject.of(42L));
		assertThat(document.implementingValueObject).isEqualTo(ImplementingValueObject.of(27L));
		assertThat(document.association).isEqualTo(Association.forId(SampleIdentifier.of(uuid)));
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	static class Document {
		SampleIdentifier identifier;
		SampleIdentifierWithConstructor identifierWithConstructor;
		SampleValueObject valueObject;
		ImplementingValueObject implementingValueObject;
		Association<?, SampleIdentifier> association;
	}

	@Value(staticConstructor = "of")
	static class SampleIdentifier implements Identifier {
		UUID id;
	}

	@Value
	static class SampleIdentifierWithConstructor implements Identifier {
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
}
