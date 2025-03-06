/*
 * Copyright 2021-2025 the original author or authors.
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
package org.jmolecules.jpa;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.Advice.OnMethodExit;
import net.bytebuddy.asm.Advice.This;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class JMoleculesJpa {

	private static Map<Class<?>, Collection<Field>> fields = new ConcurrentHashMap<>();

	public static void verifyNullability(@net.bytebuddy.implementation.bind.annotation.This Object object) {

		if (object == null) {
			return;
		}

		log.debug("Verifying nullability of {}!", object.getClass().getSimpleName());

		fields.computeIfAbsent(object.getClass(), JMoleculesJpa::fieldsToNullCheckFor)
				.forEach(it -> {

					Object value;

					try {
						value = it.get(object);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}

					if (value == null) {
						throw new IllegalStateException(
								String.format("%s.%s must not be null!", it.getDeclaringClass().getSimpleName(), it.getName()));
					}
				});
	}

	@OnMethodExit
	public static void adviceVerifyNullability(@This Object object) {
		verifyNullability(object);
	}

	private static Collection<Field> fieldsToNullCheckFor(Class<?> type) {

		return Arrays.stream(type.getDeclaredFields())
				.filter(field -> !isGeneratedValue(field))
				.filter(field -> !isNullable(field))
				.peek(field -> field.setAccessible(true))
				.collect(Collectors.toSet());
	}

	private static boolean isGeneratedValue(Field field) {

		return Arrays.stream(field.getAnnotations())
				.map(Annotation::annotationType)
				.map(Class::getName)
				.filter(it -> it.endsWith("GeneratedValue"))
				.filter(it -> it.startsWith("javax.persistence") || it.startsWith("jakarta.persistence"))
				.findAny()
				.isPresent();
	}

	private static boolean hasAnnotation(Field field, String simpleName) {
		return Arrays.stream(field.getAnnotations())
				.anyMatch(it -> it.annotationType().getSimpleName().equals(simpleName));
	}

	private static boolean isNullable(Field field) {

		if (hasAnnotation(field, "Nullable")) {
			return true;
		}

		return !Arrays.stream(field.getDeclaringClass().getPackage().getAnnotations())
				.anyMatch(it -> it.annotationType().getSimpleName().equals("NonNullApi"));
	}
}
