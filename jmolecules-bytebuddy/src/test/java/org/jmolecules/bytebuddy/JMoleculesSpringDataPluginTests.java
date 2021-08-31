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
package org.jmolecules.bytebuddy;

import static org.assertj.core.api.Assertions.*;

import lombok.Getter;
import lombok.Value;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.stream.Stream;

import org.jmolecules.ddd.annotation.Repository;
import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.RepositoryDefinition;

/**
 * Tests for {@link JMoleculesSpringPlugin}.
 *
 * @author Oliver Drotbohm
 */
class JMoleculesSpringDataPluginTests<T extends AggregateRoot<T, ID>, ID extends Identifier<T, ID>> {

	@TestFactory
	Stream<DynamicTest> processesSpringDataTypes() {
		return DynamicTestSource.of(ProcessedType.stream());
	}

	@Test
	void doesNotTranslateRepositoryMissingGenerics() {

		assertThat(RawJMoleculesRepository.class.isAssignableFrom(org.springframework.data.repository.Repository.class))
				.isFalse();
	}

	@RepositoryDefinition(domainClass = Object.class, idClass = Object.class)
	interface AnnotatedSpringDataRepository {}

	interface ImplementingSpringDataRepository extends CrudRepository<Object, Object> {}

	@SuppressWarnings("rawtypes")
	interface RawJMoleculesRepository extends org.jmolecules.ddd.types.Repository {};

	interface JMoleculesRepository extends org.jmolecules.ddd.types.Repository<SampleAggregate, SampleIdentifier> {};

	@Getter
	static class SampleAggregate implements AggregateRoot<SampleAggregate, SampleIdentifier> {
		SampleIdentifier id;
	}

	static class SampleIdentifier implements Identifier<SampleAggregate, SampleIdentifier> {}

	interface DynamicTestSource<T> {

		public static <T extends DynamicTestSource<T>> Stream<DynamicTest> of(Stream<T> supplier) {
			return DynamicTest.stream(supplier, Object::toString, it -> it.verify());
		}

		void verify();
	}

	@Value(staticConstructor = "$")
	static class ProcessedType implements DynamicTestSource<ProcessedType> {

		Class<?> type, target;

		public static Stream<ProcessedType> stream() {

			return Stream.of(
					$(AnnotatedSpringDataRepository.class, Repository.class), //
					$(ImplementingSpringDataRepository.class, Repository.class), //
					$(JMoleculesRepository.class, org.springframework.data.repository.Repository.class) //
			);
		}

		public void verify() {

			if (target.isAnnotation()) {

				assertThat(Arrays.stream(type.getAnnotations())
						.map(Annotation::annotationType)
						.anyMatch(target::equals))
								.isTrue();
			} else {

				assertThat(target).isAssignableFrom(type);
			}
		}

		@Override
		public String toString() {

			String message = target.isAnnotation() ? "is annotated with" : "implements";

			return String.format("%s %s %s", type.getSimpleName(), message, target);
		}
	}
}
