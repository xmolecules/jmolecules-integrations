/*
 * Copyright 2024-2025 the original author or authors.
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
package org.jmolecules.stereotype.catalog.support;

import java.util.function.BiPredicate;

import org.jmolecules.stereotype.catalog.StereotypeDefinition;
import org.jmolecules.stereotype.catalog.StereotypeDefinition.Assignment.Type;

/**
 * A matcher for a combine type and annotation abstraction. The matcher is not intended to be implemented directly but
 * rather set up using the {@link StereotypeMatcherBuilder} (see {@link #isAnnotatedWith(BiPredicate)}) and type and
 * annotation abstraction based {@link BiPredicate}s.
 *
 * @author Oliver Drotbohm
 * @param <T> the type abstraction
 * @param <A> the annotation abstraction
 * @see #isAnnotatedWith(BiPredicate)
 * @see org.jmolecules.stereotype.reflection.ReflectionStereotypeFactory
 */
public interface StereotypeMatcher<T, A> {

	/**
	 * Returns a {@link StereotypeMatcherBuilder} using the given {@link BiPredicate} as annotation matcher.
	 *
	 * @param <T> the type abstraction
	 * @param <A> the annotation abstraction
	 * @param matcher
	 * @return
	 */
	public static <T, A> StereotypeMatcherBuilder<T, A> isAnnotatedWith(BiPredicate<A, String> matcher) {
		return new StereotypeMatcherBuilder<>(matcher);
	}

	/**
	 * @author Oliver Drotbohm
	 * @param <T> the type abstraction
	 * @param <A> the annotation abstraction
	 */
	class StereotypeMatcherBuilder<T, A> {

		private final BiPredicate<A, String> annotationMatcher;

		/**
		 * Creates a new {@link StereotypeMatcherBuilder}
		 *
		 * @param annotationMatcher must not be {@literal null}.
		 */
		private StereotypeMatcherBuilder(BiPredicate<A, String> annotationMatcher) {
			this.annotationMatcher = annotationMatcher;
		}

		/**
		 * Concludes the creation of a {@link StereotypeMatcher} with the
		 *
		 * @param matcher
		 * @return
		 */
		public StereotypeMatcher<T, A> orImplements(BiPredicate<T, String> matcher) {

			return new StereotypeMatcher<T, A>() {

				/*
				 * (non-Javadoc)
				 * @see org.jmolecules.stereotype.catalog.StereotypeMatcher#hasImplementingStereotype(java.lang.Object, org.jmolecules.stereotype.catalog.StereotypeDefinition)
				 */
				@Override
				public boolean hasImplementingStereotype(T type, StereotypeDefinition definition) {

					return definition.getAssignments().stream()
							.filter(it -> it.hasType(Type.IMPLEMENTS))
							.anyMatch(it -> matcher.test(type, it.getTarget()));
				}

				/*
				 * (non-Javadoc)
				 * @see org.jmolecules.stereotype.catalog.StereotypeMatcher#hasAnnotatedStereotype(java.lang.Object, org.jmolecules.stereotype.catalog.StereotypeDefinition)
				 */
				@Override
				public boolean hasAnnotatedStereotype(A annotatedElement, StereotypeDefinition definition) {

					return definition.getAssignments().stream()
							.filter(it -> it.hasType(Type.IS_ANNOTATED))
							.anyMatch(it -> annotationMatcher.test(annotatedElement, it.getTarget()));
				}
			};
		}
	}

	/**
	 * Returns whether the given type implements the stereotype described by the given {@link StereotypeDefinition}.
	 *
	 * @param type must not be {@literal null}.
	 * @param definition must not be {@literal null}.
	 */
	boolean hasImplementingStereotype(T type, StereotypeDefinition definition);

	/**
	 * Returns whether the given annotated element carries the given annotated {@link StereotypeDefinition}.
	 *
	 * @param annotatedElement must not be {@literal null}.
	 * @param definition must not be {@literal null}.
	 */
	boolean hasAnnotatedStereotype(A annotatedElement, StereotypeDefinition definition);
}
