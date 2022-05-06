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
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;

import org.jmolecules.ddd.types.Entity;

/**
 * @author Oliver Drotbohm
 */
class JMoleculesElementMatchers {

	public static ElementMatcher<TypeDescription> isEntity() {

		return it -> {

			boolean match = it.isAssignableTo(Entity.class)
					|| hasAnnotation(it, org.jmolecules.ddd.annotation.Entity.class);

			return match;
		};
	}

	public static ElementMatcher<? super Generic> isCollectionOfEntity() {

		return it -> {

			boolean match = it.asErasure().isAssignableTo(Collection.class)
					&& isEntity().matches(it.asGenericType().getTypeArguments().get(0).asErasure());

			return match;
		};
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
				.filter(doesNotResideInAnyPackageStartingWith("java", "kotlin")) //
				.anyMatch(it -> hasAnnotation(it, annotation));
	}

	/**
	 * Returns a {@link Predicate} that passes for all {@link TypeDescription}s not living in packages that start with the
	 * given prefixes.
	 *
	 * @param prefixes the prefixes to skip
	 * @return will never be {@literal null}.
	 */
	private static Predicate<TypeDescription> doesNotResideInAnyPackageStartingWith(String... prefixes) {

		return description -> {

			String packageName = description.getPackage().getName();

			return Arrays.stream(prefixes).noneMatch(packageName::startsWith);
		};
	}
}
