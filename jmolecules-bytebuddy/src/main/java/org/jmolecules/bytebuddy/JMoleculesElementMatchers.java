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

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.matcher.ElementMatcher;
import org.jmolecules.ddd.types.Entity;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * @author Oliver Drotbohm
 */
class JMoleculesElementMatchers {

	static final List<String> PACKAGE_PREFIX_TO_SKIP = Arrays.asList("java.", "javax.", "kotlin.");

	public static ElementMatcher<TypeDescription> isEntity() {
		return it -> it.isAssignableTo(Entity.class) || hasAnnotation(it, org.jmolecules.ddd.annotation.Entity.class);
	}

	public static ElementMatcher<? super Generic> isCollectionOfEntity() {

		return it -> it.asErasure().isAssignableTo(Collection.class) && isEntity().matches(
				it.asGenericType().getTypeArguments().get(0).asErasure());
	}

	static boolean hasAnnotation(TypeDescription type, Class<? extends Annotation> annotation) {

		Objects.requireNonNull(type, "Type must not be null!");

		AnnotationList found = type.getDeclaredAnnotations();

		if (found.isAnnotationPresent(annotation)) {
			return true;
		}

		if (found.isEmpty()) {
			return false;
		}

		return found.asTypeList() //
				.stream() //
				.filter(doesNotResideInAnyPackageStartingWith(PACKAGE_PREFIX_TO_SKIP)) //
				.anyMatch(it -> hasAnnotation(it, annotation));
	}

	/**
	 * Matcher checking, if the method is annotated.
	 *
	 * @param type   type to check for.
	 * @param source source code.
	 * @param target target representation.
	 * @return element matcher.
	 */
	static ElementMatcher<? super MethodDescription> hasAnnotatedMethod(TypeDescription type,
			Class<? extends Annotation> source, Class<? extends Annotation> target, PluginLogger.Log log) {

		return method -> {

			if (!method.getDeclaringType().equals(type)) {
				return false;
			}

			if (PACKAGE_PREFIX_TO_SKIP.stream().anyMatch(it -> method.getDeclaringType().getTypeName().startsWith(it))) {
				return false;
			}

			AnnotationList annotations = method.getDeclaredAnnotations();

			if (annotations.isAnnotationPresent(target)) {
				// log.info("Already annotated with @{}.", PluginUtils.abbreviate(target));
				return false;
			}

			if (!annotations.isAnnotationPresent(source)) {
				// log.info("Annotation {} not found.", PluginUtils.abbreviate(source));
				return false;
			}

			log.info("Adding @{}.", PluginUtils.abbreviate(target));

			return true;
		};
	}

	/**
	 * Returns a {@link Predicate} that passes for all {@link TypeDescription}s not living in packages that start with the
	 * given prefixes.
	 *
	 * @param prefixes the prefixes to skip
	 * @return will never be {@literal null}.
	 */
	private static Predicate<TypeDescription> doesNotResideInAnyPackageStartingWith(List<String> prefixes) {
		return description -> !residesInAnyPackageStartingWith(description, prefixes);
	}

	/**
	 * Check that passes if given type is living in one of packages that start with the given prefixes.
	 *
	 * @param target   type description
	 * @param prefixes the prefixes to skip
	 * @return true if the type is inside a package starting with one of specified prefix.
	 */
	static boolean residesInAnyPackageStartingWith(TypeDescription target, List<String> prefixes) {
		return target.getPackage() != null && prefixes.stream().anyMatch(
				it -> target.getPackage().getName().startsWith(it));
	}
}
