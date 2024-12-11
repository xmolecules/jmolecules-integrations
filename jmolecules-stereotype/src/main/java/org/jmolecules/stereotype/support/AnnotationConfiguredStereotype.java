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
package org.jmolecules.stereotype.support;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jmolecules.stereotype.Stereotype;

/**
 * A stereotype that will calculate defaults based on a {@link StringBasedStereotype} for the given type name, but also
 * consider customizations made in the {@link Stereotype} annotations on the target type.
 *
 * @author Oliver Drotbohm
 */
public class AnnotationConfiguredStereotype extends AbstractStereotype {

	private final org.jmolecules.stereotype.api.Stereotype defaults;
	private final Stereotype annotation;

	private AnnotationConfiguredStereotype(org.jmolecules.stereotype.api.Stereotype defaults,
			org.jmolecules.stereotype.Stereotype annotation) {

		if (defaults == null) {
			throw new IllegalArgumentException("Defaulting Stereotype must not be null!");
		}

		if (annotation == null) {
			throw new IllegalArgumentException("Stereotype annotation must not be null!");
		}

		this.defaults = defaults;
		this.annotation = annotation;
	}

	/**
	 * Creates a new {@link AnnotationConfiguredStereotype} for the given {@link Class}.
	 *
	 * @param stereotype must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public static org.jmolecules.stereotype.api.Stereotype of(Class<?> stereotype) {

		if (stereotype == null) {
			throw new IllegalArgumentException("Stereotype class must not be null!");
		}

		return of(stereotype.getName(), stereotype.getAnnotation(Stereotype.class));
	}

	public static <T> org.jmolecules.stereotype.api.Stereotype of(String fullyQualifiedName, Stereotype annotation) {

		var defaults = StringBasedStereotype.of(fullyQualifiedName);

		return CachingStereotype.of(new AnnotationConfiguredStereotype(defaults, annotation));
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.StereotypeMetadata#getIdentifier()
	 */
	@Override
	public String getIdentifier() {

		if (isNotEmpty(Stereotype::id)) {
			return annotation.id();
		}

		return defaults.getIdentifier();
	}

	/*
	 *
	 * (non-Javadoc)
	 * @see org.jmolecules.StereotypeMetadata#getDisplayName()
	 */
	@Override
	public String getDisplayName() {

		if (isNotEmpty(Stereotype::name)) {
			return annotation.name();
		}

		if (isNotEmpty(Stereotype::value)) {
			return annotation.value();
		}

		return defaults.getDisplayName();
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.StereotypeMetadata#getGroups()
	 */
	@Override
	public List<String> getGroups() {

		if (annotation.groups().length != 0) {
			return List.of(annotation.groups());
		}

		return defaults.getGroups();
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.core.Stereotype#getPriority()
	 */
	@Override
	public int getPriority() {

		if (matches(Stereotype::priority, it -> it != 0)) {
			return annotation.priority();
		}

		return defaults.getPriority();
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.api.Stereotype#isInherited()
	 */
	@Override
	public boolean isInherited() {
		return annotation.inherited();
	}

	private <T> boolean isNotEmpty(Function<Stereotype, String> extractor) {
		return !extractor.apply(annotation).trim().isEmpty();
	}

	private <T> boolean matches(Function<Stereotype, T> extractor, Predicate<T> predicate) {
		return predicate.test(extractor.apply(annotation));
	}
}
