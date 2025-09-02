/*
 * Copyright 2023-2025 the original author or authors.
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

import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

/**
 * @author Oliver Drotbohm
 */
@JsonTest
@Disabled
class JsonTestAutoConfigurationTests {

	@Autowired ObjectMapper mapper;

	@SpringBootApplication
	static class Sample {}

	@Test // #161
	void registersJMoleculesModuleForAtJsonTest() throws Exception {

		assertThat(mapper.getRegisteredModules())
				.extracting(JacksonModule::getModuleName)
				.contains("jmolecules-module");
	}
}
