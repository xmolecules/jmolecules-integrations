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
package org.jmolecules.annotation.processor;

import io.toolisticon.aptk.common.ToolingProvider;
import io.toolisticon.aptk.tools.TypeMirrorWrapper;
import io.toolisticon.aptk.tools.fluentfilter.FluentElementFilter;
import io.toolisticon.aptk.tools.wrapper.AnnotationMirrorWrapper;
import io.toolisticon.aptk.tools.wrapper.ElementWrapper;
import io.toolisticon.aptk.tools.wrapper.PackageElementWrapper;
import io.toolisticon.aptk.tools.wrapper.TypeElementWrapper;
import net.minidev.json.JSONObject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
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
import javax.tools.Diagnostic.Kind;
import javax.tools.StandardLocation;

import org.jmolecules.stereotype.api.Stereotype;
import org.jmolecules.stereotype.catalog.StereotypeDefinition;
import org.jmolecules.stereotype.catalog.StereotypeDefinition.Assignment;
import org.jmolecules.stereotype.catalog.StereotypeDefinition.Assignment.Type;
import org.jmolecules.stereotype.catalog.support.DefaultStereotypeDefinition;
import org.jmolecules.stereotype.support.AnnotationConfiguredStereotype;

/**
 * An APT {@link Processor} implementation to verify DDD rules.
 *
 * @author Oliver Drotbohm
 * @author Tobias Stamann
 */
public class JMoleculesProcessor implements Processor {

	private final List<Verification> verifications = new ArrayList<>();
	private final Set<String> packagesToCheck = new HashSet<>();
	private final List<StereotypeDefinition> definitions = new ArrayList<>();

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

			Set<ElementWrapper<?>> elements = roundEnv.getRootElements().stream()
					.map(ElementWrapper::wrap)
					.collect(Collectors.toSet());

			for (ElementWrapper<?> rootElement : elements) {

				PackageElementWrapper packageElementWrapper = rootElement.unwrap() instanceof TypeElement
						? rootElement.getPackage()
						: PackageElementWrapper.toPackageElement(rootElement);

				packagesToCheck.add(packageElementWrapper.getQualifiedName());

				if (rootElement.isTypeElement()) {
					detectStereotype(rootElement);
				}
			}

		} else {

			// In processing over phase do the checks for all types in collected packages

			packagesToCheck.stream()
					.flatMap(fqn -> getTypesInPackage(fqn))
					.forEach(it -> verifications.stream().forEach(verification -> verification.verify(it)));

			try {

				writeStereotypeFile();

			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		return false;
	}

	private static Stream<TypeElementWrapper> getTypesInPackage(String name) {

		// Cannot use Stream directly, otherwise we get:
		// java.lang.IncompatibleClassChangeError: Inconsistent constant pool data in classfile for class
		// java/util/stream/Stream. Method 'java.util.stream.Stream empty()' at index 267 is CONSTANT_MethodRef and should
		// be CONSTANT_InterfaceMethodRef

		List<TypeElement> result = PackageElementWrapper.getByFqn(name)
				.map(PackageElementWrapper::filterFlattenedEnclosedElementTree)
				.map(FluentElementFilter::getResult)
				.orElseGet(Collections::emptyList)
				.stream()
				.filter(TypeElement.class::isInstance)
				.map(TypeElement.class::cast)
				.collect(Collectors.toList());

		return result.stream().map(TypeElementWrapper::wrap);
	}

	private static TypeMirror getTypeMirror(String fqn) {
		return TypeMirrorWrapper.wrap(fqn).erasure().unwrap();
	}

	private static boolean hasMetaAnnotation(TypeMirrorWrapper element, TypeMirror mirror) {

		return TypeElementWrapper.getByTypeMirror(element.unwrap())
				.map(it -> hasMetaAnnotation(it, mirror))
				.orElse(false);
	}

	private static boolean hasMetaAnnotation(ElementWrapper<? extends Element> element, TypeMirror mirror) {
		return hasMetaAnnotation(element, TypeMirrorWrapper.getQualifiedName(mirror));
	}

	private static boolean hasMetaAnnotation(ElementWrapper<? extends Element> element, String fqn) {

		if (element.hasAnnotation(fqn)) {
			return true;
		}

		return element.getAnnotationMirrors().stream()
				.filter(JMoleculesProcessor::shouldTraverse)
				.anyMatch(it -> hasMetaAnnotation(it.asTypeMirror().getTypeElement().get(), fqn));
	}

	private static boolean shouldTraverse(AnnotationMirrorWrapper annotation) {

		String name = annotation.asTypeMirror().getQualifiedName();

		return name != null && !(name.startsWith("java") || name.startsWith("jakarta") || name.startsWith("kotlin"));
	}

	// Stereotype detection
	private void detectStereotype(ElementWrapper<?> wrapper) {

		wrapper.getAnnotation(org.jmolecules.stereotype.Stereotype.class).ifPresent(it -> {

			var type = wrapper.asType();
			var typeName = type.getQualifiedName();
			var of = AnnotationConfiguredStereotype.of(typeName, it);

			var assignmentType = wrapper.isAnnotation() ? Type.IS_ANNOTATED : Type.IMPLEMENTS;
			var assignment = Assignment.of(typeName, assignmentType);

			definitions.add(DefaultStereotypeDefinition.of(of, assignment, type));
		});
	}

	private void writeStereotypeFile() throws IOException {

		if (definitions.isEmpty()) {
			return;
		}

		var stereotypes = definitions.stream()
				.collect(Collectors.toMap(it -> it.getStereotype().getIdentifier(), JMoleculesProcessor::toMap));

		var content = JSONObject.toJSONString(Map.of("stereotypes", stereotypes));
		var tooling = ToolingProvider.getTooling();

		tooling.getMessager().printMessage(Kind.NOTE,
				"Writing jMolecules stereotype metadata to META-INF/jmolecules-stereotypes.json: "
						+ definitions.stream().map(StereotypeDefinition::getStereotype)
								.map(Stereotype::getIdentifier)
								.collect(Collectors.joining(", ", "[ ", " ]")));

		var filer = tooling.getFiler();
		var resource = filer.createResource(StandardLocation.CLASS_OUTPUT, "",
				"META-INF/jmolecules-stereotypes.json");

		try (Writer writer = resource.openWriter()) {
			writer.append(content);
		}
	}

	private static Map<String, Object> toMap(StereotypeDefinition definition) {

		var stereotype = definition.getStereotype();
		var result = new LinkedHashMap<String, Object>();

		result.put("targets", definition.getAssignments().stream().map(Assignment::getTarget).toArray());
		result.put("groups", stereotype.getGroups());
		result.put("priority", stereotype.getPriority());

		return result;
	}

	private interface Verification {
		void verify(TypeElementWrapper element);
	}

	/**
	 * JMolecules DDD-specific verifications.
	 *
	 * @author Oliver Drotbohm
	 */
	private static class JMoleculesDddVerification implements Verification {

		private static final String PACKAGE = "org.jmolecules.ddd";
		private static final String IDENTITY_TYPE_NAME = PACKAGE + ".annotation.Identity";

		private final TypeMirror AGGREGATE_TYPE = getTypeMirror(PACKAGE + ".types.AggregateRoot");
		private final TypeMirror AGGREGATE_ANNOTATION = getTypeMirror(PACKAGE + ".annotation.AggregateRoot");
		private final TypeMirror ASSOCIATION = getTypeMirror(PACKAGE + ".types.Association");
		private final TypeMirror ENTITY_ANNOTATION = getTypeMirror("org.jmolecules.ddd.annotation.Entity");
		private final TypeMirror IDENTIFIABLE_TYPE = getTypeMirror(PACKAGE + ".types.Identifiable");
		private final TypeMirror IDENTIFIER_TYPE = getTypeMirror(PACKAGE + ".types.Identifier");
		private final TypeMirror VALUE_OBJECT_ANNOTATION = getTypeMirror(PACKAGE + ".annotation.ValueObject");
		private final TypeMirror VALUE_OBJECT_TYPE = getTypeMirror(PACKAGE + ".types.ValueObject");

		public static boolean isAvailable() {
			return getTypeMirror(PACKAGE + ".types.AggregateRoot") != null;
		}

		public void verify(TypeElementWrapper element) {

			if (isIdentifiable(element)) {

				verifyFields(element, it -> !isAggregate(it),
						"Invalid aggregate root reference! Use identifier reference or Association instead!");
			}

			if (isAnnotatedIdentifiable(element.asType())) {

				element.validate().asError()
						.withCustomMessage("${0} needs identity declaration on either field or method!",
								element.asType().getSimpleName())
						.check(__ -> hasIdentityMethodOrField(element))
						.validateAndIssueMessages();
			}

			if (isValueObjectOrIdentifier(element)) {
				verifyFields(element, it -> isAssociation(it) || !isIdentifiable(it),
						"Value object or identifier must not refer to identifiables!");
			}
		}

		private static void verifyFields(TypeElementWrapper element, Predicate<TypeMirrorWrapper> check,
				String message, Object... args) {

			element.getFields().forEach(it -> {

				it.validate().asError()
						.withCustomMessage(message, args)
						.check(inner -> check.test(inner.asType()))
						.validateAndIssueMessages();
			});
		}

		private static boolean hasIdentityMethodOrField(TypeElementWrapper element) {
			return hasMethodOrFieldMatching(element, it -> hasMetaAnnotation(it, IDENTITY_TYPE_NAME));
		}

		private static boolean hasMethodOrFieldMatching(TypeElementWrapper element,
				Predicate<ElementWrapper<? extends Element>> predicate) {

			List<? extends ElementWrapper<? extends Element>> fields = element.getFields();
			List<? extends ElementWrapper<? extends Element>> methods = element.getMethods();

			return Stream.concat(fields.stream(), methods.stream())
					.anyMatch(predicate);
		}

		private boolean isValueObjectOrIdentifier(TypeElementWrapper mirror) {

			TypeMirrorWrapper type = mirror.asType();

			return isValueObject(type) || type.isAssignableTo(IDENTIFIER_TYPE);
		}

		private boolean isValueObject(TypeMirrorWrapper mirror) {

			return mirror.isAssignableTo(VALUE_OBJECT_TYPE)
					|| hasMetaAnnotation(mirror, VALUE_OBJECT_ANNOTATION);
		}

		private boolean isAggregate(TypeMirrorWrapper mirror) {

			return mirror.isAssignableTo(AGGREGATE_TYPE)
					|| hasMetaAnnotation(mirror, AGGREGATE_ANNOTATION);
		}

		private boolean isAssociation(TypeMirrorWrapper mirror) {
			return mirror.isAssignableTo(ASSOCIATION);
		}

		private boolean isIdentifiable(TypeMirrorWrapper mirror) {

			return mirror.isAssignableTo(IDENTIFIABLE_TYPE)
					|| isAnnotatedIdentifiable(mirror);
		}

		private boolean isIdentifiable(TypeElementWrapper element) {
			return isIdentifiable(element.asType());
		}

		private boolean isAnnotatedIdentifiable(TypeMirrorWrapper mirror) {

			return hasMetaAnnotation(mirror, ENTITY_ANNOTATION)
					|| hasMetaAnnotation(mirror, AGGREGATE_ANNOTATION);
		}
	}
}
