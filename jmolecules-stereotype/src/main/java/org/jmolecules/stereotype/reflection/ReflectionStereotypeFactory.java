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
package org.jmolecules.stereotype.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jmolecules.stereotype.api.Stereotype;
import org.jmolecules.stereotype.api.StereotypeFactory;
import org.jmolecules.stereotype.api.Stereotypes;
import org.jmolecules.stereotype.catalog.StereotypeDefinition.Assignment;
import org.jmolecules.stereotype.catalog.StereotypeDefinition.Assignment.Type;
import org.jmolecules.stereotype.catalog.StereotypeDefinitionRegistry;
import org.jmolecules.stereotype.catalog.support.StereotypeDetector;
import org.jmolecules.stereotype.catalog.support.StereotypeDetector.AnalysisLevel;
import org.jmolecules.stereotype.catalog.support.StereotypeMatcher;
import org.jmolecules.stereotype.support.AnnotationConfiguredStereotype;

/**
 * A {@link StereotypeFactory} based on Java's reflection API.
 *
 * @author Oliver Drotbohm
 */
public class ReflectionStereotypeFactory implements StereotypeFactory<Package, Class<?>, Method> {

	private static final Class<org.jmolecules.stereotype.Stereotype> STEREOTYPE_ANNOTATION = org.jmolecules.stereotype.Stereotype.class;
	private static final StereotypeMatcher<Class<?>, AnnotatedElement> STEREOTYPE_MATCHER = StereotypeMatcher
			.<Class<?>, AnnotatedElement> isAnnotatedWith((element, fqn) -> isAnnotated(element, fqn))
			.orImplements((type, fqn) -> doesImplement(type, fqn));

	private final StereotypeDetector detector;
	private final StereotypeDefinitionRegistry registry;
	private boolean detectLocalStereotypes;

	/**
	 * Creates a new {@link ReflectionStereotypeFactory} for the given {@link StereotypeCatalog} and
	 * {@link StereotypeRegistry}.
	 *
	 * @param catalog must not be {@literal null}.
	 * @param registry must not be {@literal null}.
	 */
	private ReflectionStereotypeFactory(StereotypeDetector catalog, StereotypeDefinitionRegistry registry) {

		if (catalog == null) {
			throw new IllegalArgumentException("StereotypeCatalog must not be null!");
		}

		if (registry == null) {
			throw new IllegalArgumentException("StereotypeRegistry must not be null!");
		}

		this.detector = catalog;
		this.registry = registry;
		this.detectLocalStereotypes = false;
	}

	/**
	 * Creates a new {@link ReflectionStereotypeFactory} for the given {@link StereotypeDetector} that's also a
	 * {@link StereotypeRegistry}.
	 *
	 * @param registry must not be {@literal null}.
	 */
	public <T extends StereotypeDefinitionRegistry & StereotypeDetector> ReflectionStereotypeFactory(T registry) {
		this(registry, registry);
	}

	public ReflectionStereotypeFactory enableLocalStereotypeDetection() {
		this.detectLocalStereotypes = true;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.StereotypeFactory#fromMethod(java.lang.Object)
	 */
	@Override
	public Stereotypes fromMethod(Method method) {

		List<Stereotype> result = new ArrayList<>();

		for (Annotation annotation : method.getAnnotations()) {
			result.addAll(fromAnnotatedElement(annotation.annotationType(), AnalysisLevel.DIRECT));
		}

		return new Stereotypes(result);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.StereotypeFactory#fromType(java.lang.Object)
	 */
	@Override
	public Stereotypes fromType(Class<?> type) {

		return new Stereotypes(fromTypeInternal(type))
				.and(fromPackage(type.getPackage()));
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.StereotypeFactory#fromPackage(java.lang.Object)
	 */
	@Override
	public Stereotypes fromPackage(Package pkg) {
		return new Stereotypes(fromAnnotatedElement(pkg, AnalysisLevel.DIRECT));
	}

	private Collection<Stereotype> fromTypeInternal(Class<?> type) {
		return this.fromTypeInternal(type, AnalysisLevel.DIRECT);
	}

	private Collection<Stereotype> fromTypeInternal(Class<?> type, AnalysisLevel level) {

		var result = new TreeSet<Stereotype>();

		result.addAll(detector.getTypeBasedStereotypes(type, level, STEREOTYPE_MATCHER));

		if (type.isAnnotation()) {

			result.addAll(fromAnnotatedElement(type, level));

			return result;
		}

		var candidates = type.getInterfaces();

		if (detectLocalStereotypes) {
			for (Class<?> candidate : candidates) {
				if (candidate.getAnnotation(STEREOTYPE_ANNOTATION) != null) {
					result.add(registerStereotypeDefinition(candidate, Type.IS_ANNOTATED));
				}
			}
		}

		for (Class<?> candidate : candidates) {
			result.addAll(fromTypeInternal(candidate, AnalysisLevel.INHERITED));
		}

		var superType = type.getSuperclass();

		if (superType != null && !Object.class.equals(superType)) {
			result.addAll(fromTypeInternal(type.getSuperclass()));
		}

		if (!type.isAnnotation()) {
			result.addAll(fromAnnotatedElement(type, level));
		}

		return result;
	}

	private <T extends AnnotatedElement> Collection<Stereotype> fromAnnotatedElement(AnnotatedElement element,
			AnalysisLevel level) {

		if (element == null) {
			return Collections.emptyList();
		}

		var result = new ArrayList<Stereotype>();

		result.addAll(detector.getAnnotationBasedStereotypes(element, level, STEREOTYPE_MATCHER));

		if (!detectLocalStereotypes) {
			return result;
		}

		for (Annotation annotation : element.getAnnotations()) {

			for (Class<?> type : fromAnnotation(annotation)) {

				var stereotype = registerStereotypeDefinition(type, Type.IS_ANNOTATED);

				if (level.supports(stereotype)) {
					result.add(stereotype);
				}
			}
		}

		return result;
	}

	private Stereotype registerStereotypeDefinition(Class<?> type, Type assignmentType) {

		var stereotype = AnnotationConfiguredStereotype.of(type);

		return registry.getOrRegister(stereotype, Assignment.of(type.getName(), assignmentType), type)
				.getStereotype();
	}

	private static boolean isAnnotated(AnnotatedElement element, String fqn) {
		return isAnnotated(element, fqn, new HashSet<>());
	}

	/**
	 * Returns whether the given element is (meta-)annotated with the annotation of the given name. Keeps track of the
	 * annotation types already inspected to terminate on annotation graphs containing cycles (e.g. Kotlin's
	 * {@code @Target}, which is annotated with itself).
	 */
	private static boolean isAnnotated(AnnotatedElement element, String fqn, Set<Class<?>> visited) {

		if (element == null) {
			return false;
		}

		return Stream.of(element.getDeclaredAnnotations())
				.map(Annotation::annotationType)
				.filter(it -> !isIgnored(it))
				.filter(visited::add)
				.anyMatch(it -> it.getName().equals(fqn) || isAnnotated(it, fqn, visited));
	}

	private static boolean doesImplement(Class<?> type, String fqn) {

		if (type.getName().equals(fqn)) {
			return true;
		}

		var supertype = Stream.of(type.getSuperclass());
		var interfaces = Stream.of(type.getInterfaces());

		return Stream.concat(supertype, interfaces)
				.filter(it -> it != null)
				.filter(Predicate.not(Object.class::equals))
				.anyMatch(it -> doesImplement(it, fqn));
	}

	private static Collection<Class<?>> fromAnnotation(Annotation annotation) {

		var result = new ArrayList<Class<?>>();

		collectStereotypeAnnotations(annotation, result, new HashSet<>());

		return result;
	}

	/**
	 * Collects the annotation types that are meta-annotated with {@link org.jmolecules.stereotype.Stereotype} starting
	 * from the type of the given {@link Annotation}. Keeps track of the annotation types already inspected to terminate
	 * on annotation graphs containing cycles.
	 */
	private static void collectStereotypeAnnotations(Annotation annotation, Collection<Class<?>> result,
			Set<Class<?>> visited) {

		var annotationType = annotation.annotationType();

		if (isIgnored(annotationType) || !visited.add(annotationType)) {
			return;
		}

		var annotations = Stream.of(annotationType.getAnnotations())
				.filter(it -> !isIgnored(it.annotationType()))
				.toList();

		for (Annotation candidate : annotations) {
			if (org.jmolecules.stereotype.Stereotype.class.equals(candidate.annotationType())) {
				result.add(annotationType);
			}
		}

		for (Annotation candidate : annotations) {
			collectStereotypeAnnotations(candidate, result, visited);
		}
	}

	/**
	 * Returns whether the given annotation type is part of the JDK or the Kotlin standard library and thus cannot be a
	 * stereotype or meta-annotated with one.
	 */
	private static boolean isIgnored(Class<?> annotationType) {

		var name = annotationType.getName();

		return name.startsWith("java") || name.startsWith("kotlin");
	}
}
