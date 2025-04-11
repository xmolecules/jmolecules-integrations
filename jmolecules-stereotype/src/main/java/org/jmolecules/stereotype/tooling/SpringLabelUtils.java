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
package org.jmolecules.stereotype.tooling;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * @author Oliver Drotbohm
 */
public class SpringLabelUtils {

	public static String requestMappings(Method method, Class<?> contextual) {

		var annotation = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);

		if (annotation == null) {
			return toString(method, contextual);
		}

		var parser = new PathPatternParser();
		var condition = new PathPatternsRequestCondition(parser, annotation.path());
		var typeAnnotation = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), RequestMapping.class);

		if (typeAnnotation != null) {
			condition = new PathPatternsRequestCondition(parser, typeAnnotation.path()).combine(condition);
		}

		return toString(annotation.method())
				.concat(" ")
				.concat(condition.getFirstPattern().getPatternString())
				.concat(" ")
				.concat(toString(method, contextual));
	}

	private static String toString(Method method, Class<?> typeContext) {

		var type = typeContext == null ? method.getDeclaringClass().getSimpleName() + "." : "";

		return type + method.getName().concat("(")
				.concat(toString(method.getParameterTypes(), Class::getSimpleName))
				.concat(")");
	}

	private static <T> String toString(T[] array) {
		return toString(array, Function.identity());
	}

	private static <T> String toString(T[] array, Function<T, ?> extractor) {

		return List.of(array).stream()
				.map(extractor)
				.map(Object::toString)
				.collect(Collectors.joining(", "));
	}
}
