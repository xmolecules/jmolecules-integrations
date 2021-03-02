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

import lombok.Value;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jmolecules.ddd.annotation.Factory;
import org.jmolecules.ddd.annotation.Repository;
import org.jmolecules.ddd.annotation.Service;
import org.jmolecules.event.annotation.DomainEventHandler;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.context.event.EventListener;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Tests for {@link JMoleculesSpringPlugin}.
 *
 * @author Oliver Drotbohm
 */
public class JMoleculesSpringPluginTests {

	@TestFactory
	Stream<DynamicTest> annotatesTypes() {
		return DynamicTestSource.of(AnnotatedTypes.stream());
	}

	@TestFactory
	Stream<DynamicTest> translatesEventHandlerMethods() throws Exception {
		return DynamicTestSource.of(AnnotatedMethods.stream());
	}

	@Service
	@Repository
	@Factory
	static class JMoleculesType {

		@DomainEventHandler
		void on(Object event) {}
	}

	@org.springframework.stereotype.Service
	@org.springframework.stereotype.Repository
	static class SpringType {

		@EventListener
		void on(Object event) {}
	}

	interface DynamicTestSource<T> {

		public static <T extends DynamicTestSource<T>> Stream<DynamicTest> of(Stream<T> supplier) {
			return DynamicTest.stream(supplier, Object::toString, it -> it.verify());
		}

		void verify();
	}

	@Value(staticConstructor = "$")
	static class AnnotatedTypes implements DynamicTestSource<AnnotatedTypes> {

		Class<?> type, annotation;

		public static Stream<AnnotatedTypes> stream() {

			return Stream.of(
					$(JMoleculesType.class, org.springframework.stereotype.Service.class), //
					$(JMoleculesType.class, org.springframework.stereotype.Repository.class), //
					$(JMoleculesType.class, org.springframework.stereotype.Component.class), //
					$(SpringType.class, Repository.class), //
					$(SpringType.class, Service.class) //
			);
		}

		public void verify() {

			assertThat(Arrays.stream(type.getAnnotations())
					.map(Annotation::annotationType)
					.anyMatch(annotation::equals))
							.isTrue();
		}

		@Override
		public String toString() {
			return String.format("%s is annotated with %s", type.getSimpleName(), annotation);
		}
	}

	@Value(staticConstructor = "$")
	static class AnnotatedMethods implements DynamicTestSource<AnnotatedMethods> {

		Class<? extends Annotation> type;
		Method method;

		static Stream<AnnotatedMethods> stream() {

			Supplier<Stream<Method>> source = () -> Stream //
					.of(JMoleculesType.class, SpringType.class)
					.map(type -> ReflectionUtils.findMethod(type, "on", Object.class));

			return Stream.of(EventListener.class, DomainEventHandler.class)
					.flatMap(it -> source.get().map(method -> AnnotatedMethods.$(it, method)));
		}

		public void verify() {
			assertThat(method.getDeclaredAnnotation(type)).isNotNull();
		}

		@Override
		public String toString() {
			return String.format("%s(…) is annotated with %s", ClassUtils.getQualifiedMethodName(method), type.getName());
		}
	}
}
