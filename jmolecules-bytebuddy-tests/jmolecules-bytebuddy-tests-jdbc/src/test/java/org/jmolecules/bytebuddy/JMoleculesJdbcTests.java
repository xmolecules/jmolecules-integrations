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
package org.jmolecules.bytebuddy;

import static org.assertj.core.api.Assertions.*;

import example.SampleEntity;
import example.SampleRecord;

import org.junit.jupiter.api.Test;

/**
 * @author Oliver Drotbohm
 */
class JMoleculesJdbcTests {

	@Test // GH-315
	void generatesEqualsAndHashCodeForEntities() throws Exception {

		// equals(…) and hashCode()
		assertThat(SampleEntity.class.getMethod("equals", Object.class).getDeclaringClass())
				.isEqualTo(SampleEntity.class);
		assertThat(SampleEntity.class.getMethod("hashCode").getDeclaringClass())
				.isEqualTo(SampleEntity.class);

		var first = new SampleEntity(1L);

		var second = new SampleEntity(1L);
		second.setSampleRecord(new SampleRecord("First", "Last"));

		assertThat(first).isEqualTo(second);
	}
}
