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

import org.jmolecules.spring.SampleIdentifier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

class JMoleculesSpringWebAutoConfigurationTests {

	@Test // #16
	void registersConverters() throws Exception {

		AssertableApplicationContext context = AssertableApplicationContext.get(() -> {

			AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
			ctx.setServletContext(new MockServletContext());
			ctx.register(App.class);
			ctx.refresh();

			return ctx;
		});

		ConversionService service = context.getBean(ConversionService.class);

		assertThat(service.canConvert(String.class, SampleIdentifier.class)).isTrue();
		assertThat(service.canConvert(SampleIdentifier.class, String.class)).isTrue();
	}

	@SpringBootApplication
	static class App {}
}
