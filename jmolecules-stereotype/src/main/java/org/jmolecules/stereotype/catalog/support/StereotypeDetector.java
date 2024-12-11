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
package org.jmolecules.stereotype.catalog.support;

import java.util.List;

import org.jmolecules.stereotype.api.Stereotype;
import org.jmolecules.stereotype.catalog.StereotypeCatalog;
import org.jmolecules.stereotype.catalog.StereotypeDefinition;

/**
 * @author Oliver Drotbohm
 */
public interface StereotypeDetector extends StereotypeCatalog {

	/**
	 * Returns all type-based {@link Stereotype}s selected by the given {@link StereotypeMatcher}.
	 *
	 * @param <T> the type abstraction
	 * @param <A> the annotation abstraction
	 * @param type must not be {@literal null}.
	 * @param filter must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	default <T, A> List<Stereotype> getTypeBasedStereotypes(T type, AnalysisLevel level, StereotypeMatcher<T, A> filter) {

		return getDefinitions().stream()
				.filter(it -> level.supports(it.getStereotype()))
				.filter(it -> filter.hasImplementingStereotype(type, it))
				.map(StereotypeDefinition::getStereotype)
				.toList();
	}

	/**
	 * Returns all annotation based {@link Stereotype}s selected by the given {@link StereotypeMatcher}.
	 *
	 * @param <T> the type abstraction
	 * @param <A> the annotation abstraction
	 * @param type must not be {@literal null}.
	 * @param filter must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	default <T, A> List<Stereotype> getAnnotationBasedStereotypes(A type, AnalysisLevel level,
			StereotypeMatcher<T, A> filter) {

		return getDefinitions().stream()
				.filter(it -> level.supports(it.getStereotype()))
				.filter(it -> filter.hasAnnotatedStereotype(type, it))
				.map(StereotypeDefinition::getStereotype)
				.toList();
	}

	public enum AnalysisLevel {

		DIRECT {

			@Override
			public boolean supports(Stereotype stereotype) {
				return true;
			}
		},

		INHERITED {

			@Override
			public boolean supports(Stereotype stereotype) {
				return stereotype.isInherited();
			}
		};

		public abstract boolean supports(Stereotype stereotype);
	}
}
