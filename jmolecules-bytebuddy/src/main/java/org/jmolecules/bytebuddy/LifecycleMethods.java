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

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription.InDefinedShape;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.util.Assert;

/**
 * Abstraction over the lifecycle methods with the configured annotations.
 *
 * @author Oliver Drotbohm
 */
class LifecycleMethods {

	private final Builder<?> builder;
	private final Map<Class<? extends Annotation>, Optional<String>> methods;

	/**
	 * Creates a new {@link LifecycleMethods} for the given {@link Builder} and lifecycle method annotations.
	 *
	 * @param builder must not be {@literal null}.
	 * @param annotation must not be {@literal null}.
	 */
	@SafeVarargs
	LifecycleMethods(Builder<?> builder, Class<? extends Annotation>... annotation) {

		Assert.notNull(builder, "Builder must not be null!");
		Assert.notNull(annotation, "Lifecycle methods must not be null!");

		this.builder = builder;
		this.methods = detectCallbackMethods(builder.toTypeDescription(), annotation);
	}

	/**
	 * Applies the given {@link Advice} to already existing methods or the given {@link Implementation} for methods that
	 * will be newly registered.
	 *
	 * @param forExisting
	 * @param forNew
	 * @return will never be {@literal null}.
	 */
	public Builder<?> apply(Supplier<Advice> forExisting, Supplier<Implementation> forNew) {

		Builder<?> result = builder;
		Set<String> handledMethods = new HashSet<>();

		forExisting = PluginUtils.memoized(forExisting);
		forNew = PluginUtils.memoized(forNew);

		for (Entry<Class<? extends Annotation>, Optional<String>> entry : methods.entrySet()) {

			Class<? extends Annotation> annotationType = entry.getKey();
			String name = entry.getValue().orElse(null);

			if (name != null) {

				if (handledMethods.contains(name)) {
					continue;
				}

				result = result.visit(forExisting.get().on(ElementMatchers.hasMethodName(name)));
				handledMethods.add(name);

			} else {
				result = result
						.defineMethod("__jMolecules__" + annotationType.getSimpleName(), void.class, Visibility.PACKAGE_PRIVATE)
						.intercept(forNew.get())
						.annotateMethod(PluginUtils.getAnnotation(annotationType));
			}
		}

		return result;
	}

	/**
	 * Detects all methods annotated with the given annotation types. Assumes that only one method can be annotated with
	 * one particular annotation but a method might be annotated with multiple annotations.
	 *
	 * @param type the type to inspect for methods, must not be {@literal null}.
	 * @param annotations the annotations to detect.
	 * @return a {@link Map} of annotations to method names, if no method carrying the annotation is found, the value is
	 *         {@link Optional#empty}.
	 */
	@SafeVarargs
	private static Map<Class<? extends Annotation>, Optional<String>> detectCallbackMethods(TypeDescription type,
			Class<? extends Annotation>... annotations) {

		Map<Class<? extends Annotation>, Optional<String>> result = new HashMap<>();

		type.getDeclaredMethods().forEach(method -> {

			Arrays.stream(annotations).forEach(annotation -> {
				result.compute(annotation, (annotationType, methodName) -> methodName != null && methodName.isPresent()
						? methodName
						: getMethodNameIfAnnotated(method, annotationType));
			});
		});

		return result;

	}

	private static Optional<String> getMethodNameIfAnnotated(InDefinedShape method, Class<? extends Annotation> type) {

		return method.getDeclaredAnnotations().isAnnotationPresent(type)
				? Optional.of(method.getName())
				: Optional.empty();
	}
}
