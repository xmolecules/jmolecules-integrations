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
package org.jmolecules.jackson.config;

import static org.assertj.core.api.Assertions.*;

import org.jmolecules.jackson.SingleValueWrappingOpenApiModelConverter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.swagger.v3.core.converter.ModelConverter;

/**
 * @author Hasan Kara
 */
class JMoleculesOpenApiAutoConfigurationTests {

	@Test // GH-310
	void registersModelConverterWhenSwaggerOnClasspath() {

		AssertableApplicationContext context = AssertableApplicationContext
				.get(() -> new AnnotationConfigApplicationContext(App.class));

		assertThat(context).hasSingleBean(SingleValueWrappingOpenApiModelConverter.class);
	}

	@Test // GH-310
	void doesNotRegisterModelConverterWithoutSwagger() {

		AssertableApplicationContext context = AssertableApplicationContext
				.get(() -> {

					AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
					// Filter out Swagger Core ModelConverter
					ctx.setClassLoader(new FilteredClassLoader(ModelConverter.class));
					ctx.register(App.class);
					ctx.refresh();

					return ctx;
				});

		assertThat(context).doesNotHaveBean(SingleValueWrappingOpenApiModelConverter.class);
	}

	@SpringBootApplication
	static class App {}
}
