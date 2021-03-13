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

import static net.bytebuddy.matcher.ElementMatchers.*;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jmolecules.ddd.types.Identifier;
import org.springframework.util.ClassUtils;

/**
 * Utility methods to be used from different {@link Plugin} implementations
 *
 * @author Oliver Drotbohm
 */
@Slf4j
class PluginUtils {

	/**
	 * Returns whether the given {@link TypeDescription} is annotated with the given annotation.
	 *
	 * @param type must not be {@literal null}.
	 * @param annotationType must not be {@literal null}.
	 * @return
	 */
	static boolean isAnnotatedWith(TypeDescription type, Class<?> annotationType) {

		return type.getDeclaredAnnotations() //
				.asTypeList() //
				.stream() //
				.anyMatch(it -> it.isAssignableTo(annotationType));
	}

	/**
	 * Returns an {@link AnnotationDescription} for an empty (i.e. no attributes defined) annotation of the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	static AnnotationDescription getAnnotation(Class<? extends Annotation> type) {
		return AnnotationDescription.Builder.ofType(type).build();
	}

	/**
	 * Applies the given map of source type or annotation to annotation onto the given {@link Builder}.
	 *
	 * @param prefix the prefix to use for log output.
	 * @param builder the current {@link Builder}.
	 * @param type the currently described type.
	 * @param mappings the annotation or type mappings.
	 * @return
	 */
	static Builder<?> mapAnnotationOrInterfaces(String prefix, Builder<?> builder, TypeDescription type,
			Map<Class<?>, Class<? extends Annotation>> mappings) {

		for (Entry<Class<?>, Class<? extends Annotation>> entry : mappings.entrySet()) {

			Class<?> source = entry.getKey();

			if (source.isAnnotation() ? isAnnotatedWith(type, source) : type.isAssignableTo(source)) {
				builder = addAnnotationIfMissing(prefix, entry.getValue(), builder, type);
			}
		}

		return builder;
	}

	static ElementMatcher<FieldDescription> defaultMapping(PluginLogger logger, Junction<FieldDescription> source,
			AnnotationDescription annotation) {

		return it -> {

			boolean matches = source.matches(it);

			if (matches) {

				String ownerName = abbreviate(it.getDeclaringType());

				logger.info("{} - Defaulting {}.{} to {} mapping.", ownerName, ownerName, it.getName(), abbreviate(annotation));
			}

			return matches;
		};
	}

	@SafeVarargs
	static Builder<?> annotateIdentifierWith(PluginLogger logger, Builder<?> builder, Class<? extends Annotation> type,
			Class<? extends Annotation>... filterAnnotations) {

		AnnotationDescription idAnnotation = getAnnotation(type);
		Junction<AnnotationSource> alreadyAnnotated = ElementMatchers.isAnnotatedWith(type);

		for (Class<? extends Annotation> filterAnnotation : filterAnnotations) {
			alreadyAnnotated = alreadyAnnotated.or(ElementMatchers.isAnnotatedWith(filterAnnotation));
		}

		return builder
				.field(PluginUtils.defaultMapping(logger,
						fieldType(isSubTypeOf(Identifier.class)).and(not(alreadyAnnotated)), idAnnotation))
				.annotateField(idAnnotation);
	}

	static String abbreviate(Class<?> type) {
		return abbreviate(type.getName());
	}

	static String abbreviate(TypeDefinition type) {
		return abbreviate(type.getTypeName());
	}

	static String abbreviate(AnnotationDescription annotation) {

		String annotationString = annotation.toString();
		int openParenthesisIndex = annotationString.indexOf("(");

		String annotationName = annotationString.substring(1, openParenthesisIndex);

		return "@"
				.concat(abbreviate(annotationName))
				.concat(annotationString.substring(openParenthesisIndex));
	}

	@SafeVarargs
	static Builder<?> addAnnotationIfMissing(String prefix, Class<? extends Annotation> annotation, Builder<?> builder,
			TypeDescription type, Class<? extends Annotation>... exclusions) {
		return addAnnotationIfMissing(prefix, __ -> annotation, builder, type, exclusions);
	}

	@SafeVarargs
	static Builder<?> addAnnotationIfMissing(String prefix,
			Function<TypeDescription, Class<? extends Annotation>> producer,
			Builder<?> builder,
			TypeDescription type, Class<? extends Annotation>... exclusions) {

		AnnotationList existing = type.getDeclaredAnnotations();
		Class<? extends Annotation> annotation = producer.apply(type);

		String typeName = PluginUtils.abbreviate(type);
		String annotationName = PluginUtils.abbreviate(annotation);

		boolean existingFound = Stream.of(exclusions).anyMatch(it -> {

			boolean found = existing.isAnnotationPresent(it);

			if (found) {
				log.info("jMolecules {} - {} - Not adding @{} because type is already annotated with @{}.",
						prefix, typeName, annotationName, PluginUtils.abbreviate(it));
			}

			return found;
		});

		if (existingFound) {
			return builder;
		}

		log.info("jMolecules {} - {} - Adding @{}.", prefix, typeName, annotationName);

		return builder.annotateType(getAnnotation(annotation));
	}

	private static String abbreviate(String fullyQualifiedTypeName) {

		String abbreviatedPackage = Arrays.stream(ClassUtils.getPackageName(fullyQualifiedTypeName).split("\\."))
				.map(it -> it.substring(0, 1))
				.collect(Collectors.joining("."));

		return abbreviatedPackage.concat(".").concat(ClassUtils.getShortName(fullyQualifiedTypeName));
	}

	private static Builder<?> addAnnotationIfMissing(String prefix, Class<? extends Annotation> annotation,
			Builder<?> builder,
			TypeDescription type) {

		if (isAnnotatedWith(type, annotation)) {
			return builder;
		}

		log.info("{} - {} - Adding @{}.", prefix, PluginUtils.abbreviate(type),
				PluginUtils.abbreviate(annotation));

		return builder.annotateType(getAnnotation(annotation));
	}

}
