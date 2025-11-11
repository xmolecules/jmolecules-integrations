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

import example.SampleAggregate;
import example.SampleAnnotatedAggregate;
import example.SampleAnnotatedEntity;
import example.SampleApplication;
import example.SampleEntity;
import example.SampleValueObject;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.aot.hint.annotation.RegisterReflection;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ReflectiveScan;

class JMoleculesSpringAotPluginTests {

	@Test
	void annotatesSpringBootApplicationClassWithAtReflectiveScan() {

		assertThat(SampleApplication.class.getDeclaredAnnotations())
				.<Class<?>> extracting(Annotation::annotationType)
				.containsExactlyInAnyOrder(SpringBootApplication.class, ReflectiveScan.class);
	}

	@TestFactory
	Stream<DynamicTest> annotatesValueObjectWithReflective() {

		var types = Stream.of(SampleValueObject.class, SampleEntity.class, SampleAnnotatedEntity.class,
				SampleAggregate.class, SampleAnnotatedAggregate.class);

		return DynamicTest.stream(types, it -> it + " is annotated with @" + Reflective.class.getSimpleName(), it -> {

			assertThat(it.getDeclaredAnnotations())
					.<Class<?>> extracting(Annotation::annotationType)
					.contains(RegisterReflection.class);
		});
	}
}
