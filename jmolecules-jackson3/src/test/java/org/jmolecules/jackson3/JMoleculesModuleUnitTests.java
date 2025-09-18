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
package org.jmolecules.jackson3;

import static org.assertj.core.api.Assertions.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Identifier;
import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

class JMoleculesModuleUnitTests {

	ObjectMapper mapper = JsonMapper.builder()
			.addModule(new JMoleculesModule())
			.build();

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

	@Test // GH-191
	void doesNotUseSupertypeFactory() throws Exception {

		Wrapper wrapper = mapper.readValue("{ \"first\" : 42 }", Wrapper.class);

		assertThat(wrapper.first).isEqualTo(new First(42L));
	}

	@Test // GH-336
	void instantiatesKotlinDataClass() throws Exception {

		// ObjectMapper mapper = this.mapper.rebuild()
		// .addModule(new KotlinModule.Builder().build());

		UUID uuid = UUID.randomUUID();
		String source = String.format("{ \"id\" : \"%s\", \"anotherId\" : \"%s\" }", uuid, uuid);

		KotlinWrapper wrapper = mapper.readValue(source, KotlinWrapper.class);

		assertThat(wrapper.getId()).isNotNull();
	}

	@Test // GH-347
	void canBeFoundAndAddedDynamically() {

		ObjectMapper mapper = JsonMapper.builder()
				.findAndAddModules()
				.build();

		assertThat(mapper.getRegisteredModules())
				.extracting(JacksonModule::getModuleName)
				.contains("jmolecules-module");
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

	// GH-191

	interface WithFactoryMethod {

		static Object of(Long source) {
			return source.toString();
		}
	}

	@Value
	@ValueObject
	static class First implements WithFactoryMethod {
		Long number;
	}

	@Data
	static class Wrapper {
		First first;
	}
}
