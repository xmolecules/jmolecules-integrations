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

import example.SampleAnnotatedValueObject;
import example.SamplePlainValueObject;
import example.SampleValueObject;

import org.junit.jupiter.api.Test;

/**
 * @author Oliver Drotbohm
 */
class JMoleculesValueObjectTests {

	@Test
	void generatesEqualsAndHashCodeForValueObjectInterface() throws Exception {

		assertThat(SamplePlainValueObject.class.getMethod("equals", Object.class).getDeclaringClass())
				.isEqualTo(SamplePlainValueObject.class);
		assertThat(SamplePlainValueObject.class.getMethod("hashCode").getDeclaringClass())
				.isEqualTo(SamplePlainValueObject.class);

		var first = new SamplePlainValueObject("John", "Doe");
		var second = new SamplePlainValueObject("John", "Doe");
		var third = new SamplePlainValueObject("Jane", "Doe");

		assertThat(first).isEqualTo(second);
		assertThat(first).isNotEqualTo(third);
		assertThat(first.hashCode()).isEqualTo(second.hashCode());
	}

	@Test
	void generatesEqualsAndHashCodeForValueObjectAnnotation() throws Exception {

		assertThat(SampleAnnotatedValueObject.class.getMethod("equals", Object.class).getDeclaringClass())
				.isEqualTo(SampleAnnotatedValueObject.class);
		assertThat(SampleAnnotatedValueObject.class.getMethod("hashCode").getDeclaringClass())
				.isEqualTo(SampleAnnotatedValueObject.class);

		var first = new SampleAnnotatedValueObject("foo");
		var second = new SampleAnnotatedValueObject("foo");
		var third = new SampleAnnotatedValueObject("bar");

		assertThat(first).isEqualTo(second);
		assertThat(first).isNotEqualTo(third);
		assertThat(first.hashCode()).isEqualTo(second.hashCode());
	}

	@Test
	void doesNotOverwriteExistingEqualsAndHashCode() throws Exception {

		// SampleValueObject uses Lombok @Value which already generates equals/hashCode
		assertThat(SampleValueObject.class.getMethod("equals", Object.class).getDeclaringClass())
				.isEqualTo(SampleValueObject.class);
		assertThat(SampleValueObject.class.getMethod("hashCode").getDeclaringClass())
				.isEqualTo(SampleValueObject.class);
	}
}
