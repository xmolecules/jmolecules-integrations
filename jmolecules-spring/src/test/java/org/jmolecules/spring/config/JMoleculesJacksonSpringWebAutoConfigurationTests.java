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
package org.jmolecules.spring.config;

import static org.assertj.core.api.Assertions.*;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.UUID;

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Identifier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

/**
 * @author Oliver Drotbohm
 */
class JMoleculesJacksonSpringWebAutoConfigurationTests {

	@Test // #16
	void registersAssociationDeSerializers() throws Exception {

		AssertableApplicationContext context = AssertableApplicationContext.get(() -> {

			AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
			ctx.setServletContext(new MockServletContext());
			ctx.register(App.class);
			ctx.refresh();

			return ctx;
		});

		ObjectMapper mapper = context.getBean(ObjectMapper.class);

		UUID uuid = UUID.randomUUID();
		AggregateIdentifier identifier = AggregateIdentifier.of(uuid);
		Aggregate aggregate = new Aggregate(identifier, Association.forId(identifier));

		String serialized = mapper.writeValueAsString(aggregate);
		DocumentContext document = JsonPath.parse(serialized);

		assertThat(document.read("$.association", String.class)).isEqualTo(uuid.toString());
		assertThat(mapper.readValue(serialized, Aggregate.class)).isEqualTo(aggregate);
	}

	@SpringBootApplication
	static class App {}

	@Value
	@NoArgsConstructor(force = true)
	@RequiredArgsConstructor
	static class Aggregate implements AggregateRoot<Aggregate, AggregateIdentifier> {
		AggregateIdentifier id;
		Association<Aggregate, AggregateIdentifier> association;
	}

	@Value(staticConstructor = "of")
	static class AggregateIdentifier implements Identifier {
		UUID id;
	}
}
