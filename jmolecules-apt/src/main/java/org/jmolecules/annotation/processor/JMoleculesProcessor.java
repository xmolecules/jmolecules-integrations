/*
 * Copyright 2024 the original author or authors.
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
package org.jmolecules.annotation.processor;

import io.toolisticon.aptk.common.ToolingProvider;
import io.toolisticon.aptk.tools.TypeMirrorWrapper;
import io.toolisticon.aptk.tools.corematcher.AptkCoreMatchers;
import io.toolisticon.aptk.tools.wrapper.AnnotationMirrorWrapper;
import io.toolisticon.aptk.tools.wrapper.ElementWrapper;
import io.toolisticon.aptk.tools.wrapper.PackageElementWrapper;
import io.toolisticon.aptk.tools.wrapper.TypeElementWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.Completion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * An APT {@link Processor} implementation to verify DDD rules.
 *
 * @author Oliver Drotbohm
 */
public class JMoleculesProcessor implements Processor {

	private final List<Verification> verifications = new ArrayList<>();

	private final Set<String> packagesToCheck = new HashSet<>();

	/*
	 * (non-Javadoc)
	 * @see javax.annotation.processing.Processor#getSupportedAnnotationTypes()
	 */
	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Collections.singleton("*");
	}

	/*
	 * (non-Javadoc)
	 * @see javax.annotation.processing.Processor#getSupportedOptions()
	 */
	@Override
	public Set<String> getSupportedOptions() {
		return Collections.emptySet();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.annotation.processing.Processor#getSupportedSourceVersion()
	 */
	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latest();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.annotation.processing.Processor#getCompletions(javax.lang.model.element.Element, javax.lang.model.element.AnnotationMirror, javax.lang.model.element.ExecutableElement, java.lang.String)
	 */
	@Override
	public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation,
			ExecutableElement member, String userText) {
		return Collections.emptyList();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.annotation.processing.AbstractProcessor#init(javax.annotation.processing.ProcessingEnvironment)
	 */
	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {

		ToolingProvider.setTooling(processingEnv);

		if (JMoleculesDddVerification.isAvailable()) {
			this.verifications.add(new JMoleculesDddVerification());
		}
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

		if (!roundEnv.processingOver()) {

			// First collect all packages
			for (ElementWrapper<?> rootElement : roundEnv.getRootElements().stream().map(ElementWrapper::wrap)
					.collect(Collectors.toSet())) {
				PackageElementWrapper packageElementWrapper;
				if (rootElement.isTypeElement()) {
					packageElementWrapper = rootElement.getPackage();
				} else {
					// must be Package element - this is usually the case if a package-info.java file is present
					packageElementWrapper = PackageElementWrapper.toPackageElement(rootElement);
				}
				packagesToCheck.add(packageElementWrapper.getQualifiedName());
			}

		} else {

			// In processing over phase do the checks for all types in collected packages
			Set<TypeElementWrapper> typesToCheck = packagesToCheck.stream()
					.<TypeElement> flatMap(fqn -> PackageElementWrapper.wrap(
							ToolingProvider.getTooling().getElements().getPackageElement(fqn))
							.filterEnclosedElements().applyFilter(AptkCoreMatchers.IS_TYPE_ELEMENT)
							.getResult().stream())
					.map(TypeElementWrapper::wrap)
					.collect(Collectors.toSet());

			typesToCheck.stream().flatMap(it -> {

				return verifications.stream().map(verification -> verification.verify(it));

			}).reduce(true, (l, r) -> l && r);

		}

		return true;
	}

	private static TypeMirror getTypeMirror(String fqn) {
		return TypeMirrorWrapper.wrap(fqn).erasure().unwrap();
	}

	private static class JMoleculesDddVerification implements Verification {

		private static final String PACKAGE = "org.jmolecules.ddd";
		private static final String IDENTITY_TYPE_NAME = PACKAGE + ".annotation.Identity";

		private final TypeMirror AGGREGATE_TYPE = getTypeMirror(PACKAGE + ".types.AggregateRoot");
		private final TypeMirror AGGREGATE_ANNOTATION = getTypeMirror(PACKAGE + ".annotation.AggregateRoot");
		private final TypeMirror ENTITY_ANNOTATION = getTypeMirror("org.jmolecules.ddd.annotation.Entity");
		private final TypeMirror IDENTIFIABLE_TYPE = getTypeMirror(PACKAGE + ".types.Identifiable");

		public static boolean isAvailable() {
			return getTypeMirror(PACKAGE + ".types.AggregateRoot") != null;
		}

		public boolean verify(TypeElementWrapper element) {

			boolean result = true;

			if (isIdentifiable(element)) {
				result = result & verifyAggregate(element);
			}

			if (isAnnotatedIdentifiable(element)) {

				result = result & element.validate().asError()
						.withCustomMessage("Needs identity field!")
						.check(__ -> hasIdentityMethodOrField(element))
						.validateAndIssueMessages();
			}

			return result;
		}

		private boolean verifyAggregate(TypeElementWrapper element) {

			return element.getFields().stream().map(field -> {

				return field.validate()
						.asError()
						.withCustomMessage("Invalid aggregate root reference! Use identifier reference or Association instead!",
								field.getSimpleName())
						.check(it -> !isAggregate(field.asType()))
						.validateAndIssueMessages();

			}).reduce(true, (l, r) -> l && r);
		}

		private static boolean hasIdentityMethodOrField(TypeElementWrapper element) {

			List<? extends ElementWrapper<? extends Element>> fields = element.getFields();
			List<? extends ElementWrapper<? extends Element>> methods = element.getMethods();

			return Stream.concat(fields.stream(), methods.stream())
					.anyMatch(it -> hasMetaAnnotation(it, IDENTITY_TYPE_NAME));
		}

		private boolean isAggregate(TypeMirrorWrapper mirror) {

			return mirror.isAssignableTo(AGGREGATE_TYPE)
					|| hasMetaAnnotation(mirror, AGGREGATE_ANNOTATION);
		}

		private boolean isIdentifiable(TypeElementWrapper element) {

			return element.asType().isAssignableTo(IDENTIFIABLE_TYPE)
					|| isAnnotatedIdentifiable(element);
		}

		private boolean isAnnotatedIdentifiable(TypeElementWrapper element) {

			return hasMetaAnnotation(element, ENTITY_ANNOTATION)
					|| hasMetaAnnotation(element, AGGREGATE_ANNOTATION);
		}
	}

	private interface Verification {
		boolean verify(TypeElementWrapper element);
	}

	private static final boolean hasMetaAnnotation(TypeMirrorWrapper element, TypeMirror mirror) {

		return TypeElementWrapper.getByTypeMirror(element.unwrap())
				.map(it -> hasMetaAnnotation(it, mirror))
				.orElse(false);
	}

	private static final boolean hasMetaAnnotation(ElementWrapper<? extends Element> element, TypeMirror mirror) {
		return hasMetaAnnotation(element, TypeMirrorWrapper.getQualifiedName(mirror));
	}

	private static final boolean hasMetaAnnotation(ElementWrapper<? extends Element> element, String fqn) {

		if (element.hasAnnotation(fqn)) {
			return true;
		}

		return element.getAnnotationMirrors().stream()
				.filter(JMoleculesProcessor::shouldTraverse)
				.anyMatch(it -> hasMetaAnnotation(it.asTypeMirror().getTypeElement().get(), fqn));
	}

	private static boolean shouldTraverse(AnnotationMirrorWrapper annotation) {

		String name = annotation.asTypeMirror().getQualifiedName();

		return name != null && !(name.startsWith("java") || name.startsWith("jakarta"));
	}
}
