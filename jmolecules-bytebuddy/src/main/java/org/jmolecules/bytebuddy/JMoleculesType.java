/*
 * Copyright 2021-2025 the original author or authors.
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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.asm.MemberAttributeExtension;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldDescription.InDefinedShape;
import net.bytebuddy.description.method.MethodDescription.InGenericShape;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jmolecules.bytebuddy.PluginLogger.Log;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Entity;
import org.jmolecules.ddd.types.Identifiable;
import org.jmolecules.ddd.types.Identifier;
import org.jmolecules.ddd.types.ValueObject;

/**
 * A wrapper around a {@link Builder} to allow issuing bytecode manipulations working with JMolecules abstractions like
 * aggregates etc.
 *
 * @author Oliver Drotbohm
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class JMoleculesType {

	private final Log logger;
	private final Builder<?> builder;
	private final TypeDescription type;

	/**
	 * Creates a new {@link JMoleculesType} for the given {@link ModuleLogger} and {@link Builder}.
	 *
	 * @param logger must not be {@literal null}.
	 * @param builder must not be {@literal null}.
	 * @return
	 */
	public static JMoleculesType of(Log logger, Builder<?> builder) {

		if (logger == null) {
			throw new IllegalArgumentException("PluginLogger must not be null!");
		}

		if (builder == null) {
			throw new IllegalArgumentException("Builder must not be null!");
		}

		return new JMoleculesType(logger, builder, builder.toTypeDescription());
	}

	/**
	 * Returns whether the type implements the given type or is annotated with it.
	 *
	 * @param types either interfaces or annotations, must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean hasOrImplements(Class<?>... types) {

		return Arrays.stream(types).anyMatch(it -> {

			return it.isAnnotation() && hasAnnotation((Class<? extends Annotation>) it)
					|| isAssignableTo(it);
		});
	}

	public boolean hasAnnotation(Class<? extends Annotation> annotation) {
		return JMoleculesElementMatchers.hasAnnotation(type, annotation);
	}

	public boolean hasMethod(
			ElementMatcher<net.bytebuddy.description.method.MethodDescription.InDefinedShape> matcher) {
		return !type.getDeclaredMethods().filter(matcher).isEmpty();
	}

	public boolean hasField(ElementMatcher<? super InDefinedShape> matcher) {
		return !type.getDeclaredFields().filter(matcher).isEmpty();
	}

	public boolean hasMoreThanOneField(ElementMatcher<? super InDefinedShape> matcher) {
		return type.getDeclaredFields().filter(matcher).size() > 1;
	}

	public boolean isAssignableTo(Class<?> candidate) {
		return type.isAssignableTo(candidate);
	}

	public boolean isAbstract() {
		return type.isAbstract();
	}

	public boolean isAggregateRoot() {
		return hasOrImplements(AggregateRoot.class, org.jmolecules.ddd.annotation.AggregateRoot.class);
	}

	public boolean isEntity() {
		return hasOrImplements(Entity.class, org.jmolecules.ddd.annotation.Entity.class);
	}

	public boolean isAssociation() {
		return isAssignableTo(Association.class);
	}

	public boolean isIdentifier() {
		return isAssignableTo(Identifier.class);
	}

	public boolean isValueObject() {
		return hasOrImplements(ValueObject.class, org.jmolecules.ddd.annotation.ValueObject.class);
	}

	public TypeDescription getTypeDescription() {
		return type;
	}

	public JMoleculesType implement(Class<?> interfaze) {

		if (type.isAssignableTo(interfaze)) {
			return this;
		}

		logger.info("{} - Implement {}.", PluginUtils.abbreviate(type),
				PluginUtils.abbreviate(interfaze));

		return map((it, log) -> it.implement(interfaze));
	}

	public JMoleculesType implement(Class<?> interfaze, TypeDefinition... generics) {

		TypeDescription loadedType = Generic.Builder.rawType(interfaze).build().asErasure();
		Generic build = Generic.Builder.parameterizedType(loadedType, generics).build();

		String types = Arrays.stream(generics)
				.map(PluginUtils::abbreviate)
				.collect(Collectors.joining(", "));

		logger.info("Implementing {}.", PluginUtils.abbreviate(interfaze).concat("<").concat(types).concat(">"));

		return mapBuilder(builder -> builder.implement(build));
	}

	@SafeVarargs
	public final JMoleculesType annotateTypeIfMissing(Class<? extends Annotation> annotation,
			Class<? extends Annotation>... additionalFilters) {

		return addAnnotationIfMissing(annotation, additionalFilters);
	}

	@SafeVarargs
	public final JMoleculesType annotateTypeIfMissing(Function<TypeDescription, Class<? extends Annotation>> producer,
			Class<? extends Annotation>... additionalFilters) {
		return addAnnotationIfMissing(producer, additionalFilters);
	}

	public JMoleculesType implementPersistable(PersistableOptions options) {
		return PersistableImplementor.of(options).implementPersistable(this);
	}

	@SafeVarargs
	public final JMoleculesType annotateIdentifierWith(Class<? extends Annotation> annotation,
			Class<? extends Annotation>... filterAnnotations) {

		Junction<FieldDescription> isIdentifierField = fieldType(isSubTypeOf(Identifier.class))
				.or(isAnnotatedWith(Identity.class));

		return annotateFieldWith(annotation, isIdentifierField, filterAnnotations);
	}

	@SafeVarargs
	public final JMoleculesType annotateTypedIdentifierWith(Class<? extends Annotation> annotation,
			Class<? extends Annotation>... filterAnnotations) {

		// Find identifier type based on Identifiable declaration and annotate
		return findIdField()
				.map(InDefinedShape::getType)
				.map(Generic::asErasure)
				.map(ElementMatchers::isSubTypeOf)
				.map(ElementMatchers::fieldType)
				.map(it -> annotateFieldWith(annotation, it, filterAnnotations))
				.orElse(this);
	}

	@SafeVarargs
	public final JMoleculesType annotateFieldWith(Class<? extends Annotation> annotation,
			Junction<FieldDescription> selector, Class<? extends Annotation>... filterAnnotations) {
		return annotateFieldWith(PluginUtils.getAnnotation(annotation), selector, filterAnnotations);
	}

	@SafeVarargs
	public final JMoleculesType annotateFieldWith(AnnotationDescription annotation,
			Junction<FieldDescription> selector, Class<? extends Annotation>... filterAnnotations) {

		Junction<AnnotationSource> alreadyAnnotated = ElementMatchers.isAnnotatedWith(annotation.getAnnotationType());

		for (Class<? extends Annotation> filterAnnotation : filterAnnotations) {
			alreadyAnnotated = alreadyAnnotated.or(ElementMatchers.isAnnotatedWith(filterAnnotation));
		}

		AsmVisitorWrapper annotationSpec = new MemberAttributeExtension.ForField()
				.annotate(annotation)
				.on(PluginUtils.defaultMapping(logger, selector.and(not(alreadyAnnotated)), annotation));

		return JMoleculesType.of(logger, builder.visit(annotationSpec));
	}

	@SafeVarargs
	public final JMoleculesType annotateAnnotatedIdentifierWith(Class<? extends Annotation> annotation,
			Class<? extends Annotation>... filterAnnotations) {
		return annotateFieldWith(annotation, isAnnotatedWith(Identity.class), filterAnnotations);
	}

	public JMoleculesType map(BiFunction<Builder<?>, Log, Builder<?>> mapper) {
		return JMoleculesType.of(logger, mapper.apply(builder, logger));
	}

	public JMoleculesType map(Function<JMoleculesType, JMoleculesType> function) {
		return function.apply(this);
	}

	public JMoleculesType mapBuilder(Function<Builder<?>, Builder<?>> mapper) {
		return JMoleculesType.of(logger, mapper.apply(builder));
	}

	public JMoleculesType mapBuilder(Predicate<JMoleculesType> filter, Function<Builder<?>, Builder<?>> mapper) {

		return filter.test(this)
				? JMoleculesType.of(logger, mapper.apply(builder))
				: this;
	}

	public JMoleculesType map(Predicate<JMoleculesType> filter, Function<JMoleculesType, JMoleculesType> mapper) {

		return filter.test(this)
				? mapper.apply(this)
				: this;
	}

	public JMoleculesType mapIdField(BiFunction<InDefinedShape, JMoleculesType, JMoleculesType> mapper) {
		return findIdField().map(it -> mapper.apply(it, this)).orElse(this);
	}

	public JMoleculesType addDefaultConstructorIfMissing() {

		return map((builder, logger) -> {

			boolean hasDefaultConstructor = !type.getDeclaredMethods()
					.filter(it -> it.isConstructor())
					.filter(it -> it.getParameters().size() == 0)
					.isEmpty();

			if (hasDefaultConstructor) {

				logger.info("Default constructor already present.");

				return builder;
			}

			Generic superClass = type.getSuperClass();
			Iterator<InGenericShape> superClassConstructors = superClass.getDeclaredMethods()
					.filter(it -> it.isConstructor())
					.filter(it -> it.getParameters().size() == 0).iterator();

			InGenericShape superClassConstructor = superClassConstructors.hasNext() ? superClassConstructors.next()
					: null;
			String superClassName = PluginUtils.abbreviate(superClass);

			if (superClassConstructor == null) {
				logger.info(
						"No default constructor found on superclass {}. Skipping default constructor creation.",
						superClassName);

				return builder;
			}

			logger.info("Adding default constructor.");

			return builder.defineConstructor(Visibility.PUBLIC)
					.intercept(MethodCall.invoke(superClassConstructor));

		});
	}

	public Builder<?> conclude() {
		return builder;
	}

	@SafeVarargs
	private final JMoleculesType addAnnotationIfMissing(Class<? extends Annotation> annotation,
			Class<? extends Annotation>... exclusions) {

		return addAnnotationIfMissing(__ -> annotation, exclusions);
	}

	@SafeVarargs
	private final JMoleculesType addAnnotationIfMissing(Function<TypeDescription, Class<? extends Annotation>> producer,
			Class<? extends Annotation>... exclusions) {

		AnnotationList existing = type.getDeclaredAnnotations();
		Class<? extends Annotation> annotation = producer.apply(type);

		String annotationName = PluginUtils.abbreviate(annotation);

		if (existing.isAnnotationPresent(annotation)) {

			logger.info("Not adding @{} because type is already annotated with it.", annotationName);

			return this;
		}

		boolean existingFound = Stream.of(exclusions).anyMatch(it -> {

			boolean found = existing.isAnnotationPresent(it);

			if (found) {
				logger.info("Not adding @{} because type is already annotated with @{}.", annotationName,
						PluginUtils.abbreviate(it));
			}

			return found;
		});

		if (existingFound) {
			return this;
		}

		logger.info("Adding @{}.", annotationName);

		return JMoleculesType.of(logger, builder.annotateType(PluginUtils.getAnnotation(annotation)));
	}

	public Optional<InDefinedShape> findIdField() {

		TypeDescription type = builder.toTypeDescription();

		Generic superType = type.getInterfaces().stream()
				.filter(it -> it.asErasure().represents(AggregateRoot.class) || it.asErasure().represents(Entity.class))
				.findFirst().orElse(null);

		if (superType != null) {

			int index = superType.asErasure().represents(Identifiable.class) ? 0 : 1;

			try {
				Generic aggregateIdType = superType.asGenericType().getTypeArguments().get(index);

				return type.getDeclaredFields().stream()
						.filter(it -> it.getType().equals(aggregateIdType))
						.findFirst();
			} catch (IllegalStateException o_O) {
				// Raw type declaration
			}
		}

		return type.getDeclaredFields()
				.filter(isAnnotatedWith(Identity.class))
				.stream().findFirst();
	}

	public boolean isRecord() {
		return builder.toTypeDescription().isRecord();
	}
}
