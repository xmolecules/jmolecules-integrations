/*
 * Copyright 2022 the original author or authors.
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
package org.jmolecules.spring.hibernate;

import static org.assertj.core.api.Assertions.*;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RecordInstantiator}.
 *
 * @author Oliver Drotbohm
 */
class RecordInstantiatorUnitTests {

	@Test // #98
	void instantiatesRecord() {

		RecordInstantiator instantiator = new RecordInstantiator(Person.class);
		Supplier<Object[]> sources = () -> new Object[] { "Dave", "Matthews" };

		assertThat(instantiator.instantiate(sources, null))
				.isEqualTo(new Person("Matthews", "Dave"));
	}

	record Person(String lastname, String firstname) {}
}
