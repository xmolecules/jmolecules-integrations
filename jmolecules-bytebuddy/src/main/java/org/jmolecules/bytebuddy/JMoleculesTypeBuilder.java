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
import org.jmolecules.ddd.types.Entity;
import org.jmolecules.ddd.types.Identifiable;
import org.jmolecules.ddd.types.Identifier;

/**
 * A wrapper around a {@link Builder} to allow issuing bytecode manipulations working with JMolecules abstractions like
 * aggregates etc.
 *
 * @author Oliver Drotbohm
 */
class JMoleculesTypeBuilder extends JMoleculesType {

	private final Log logger;
	private final Builder<?> builder;

	/**
	 * @param type
	 * @param logger
	 * @param builder
	 */
	JMoleculesTypeBuilder(Log logger, Builder<?> builder) {

		super(builder.toTypeDescription());

		this.logger = logger;
		this.builder = builder;
	}

	/**
	 * Creates a new {@link JMoleculesType} for the given {@link ModuleLogger} and {@link Builder}.
	 *
	 * @param logger must not be {@literal null}.
	 * @param builder must not be {@literal null}.
	 * @return
	 */
	public static JMoleculesTypeBuilder of(Log logger, Builder<?> builder) {

		if (logger == null) {
			throw new IllegalArgumentException("PluginLogger must not be null!");
		}

		if (builder == null) {
			throw new IllegalArgumentException("Builder must not be null!");
		}

		return new JMoleculesTypeBuilder(logger, builder);
	}

	public JMoleculesTypeBuilder implement(Class<?> interfaze) {

		if (type.isAssignableTo(interfaze)) {
			return this;
		}

		logger.info("{} - Implement {}.", PluginUtils.abbreviate(type),
				PluginUtils.abbreviate(interfaze));

		return mapBuilder((it, log) -> it.implement(interfaze));
	}

	public JMoleculesTypeBuilder implement(Class<?> interfaze, TypeDefinition... generics) {

		TypeDescription loadedType = Generic.Builder.rawType(interfaze).build().asErasure();
		Generic build = Generic.Builder.parameterizedType(loadedType, generics).build();

		String types = Arrays.stream(generics)
				.map(PluginUtils::abbreviate)
				.collect(Collectors.joining(", "));

		logger.info("Implementing {}.", PluginUtils.abbreviate(interfaze).concat("<").concat(types).concat(">"));

		return mapBuilder(builder -> builder.implement(build));
	}

	@SafeVarargs
	public final JMoleculesTypeBuilder annotateTypeIfMissing(Class<? extends Annotation> annotation,
			Class<? extends Annotation>... additionalFilters) {

		return addAnnotationIfMissing(annotation, additionalFilters);
	}

	@SafeVarargs
	public final JMoleculesTypeBuilder annotateTypeIfMissing(
			Function<TypeDescription, Class<? extends Annotation>> producer,
			Class<? extends Annotation>... additionalFilters) {
		return addAnnotationIfMissing(producer, additionalFilters);
	}

	public JMoleculesTypeBuilder implementPersistable(PersistableOptions options) {
		return PersistableImplementor.of(options).implementPersistable(this, logger);
	}

	@SafeVarargs
	public final JMoleculesTypeBuilder annotateIdentifierWith(Class<? extends Annotation> annotation,
			Class<? extends Annotation>... filterAnnotations) {

		Junction<FieldDescription> isIdentifierField = fieldType(isSubTypeOf(Identifier.class))
				.or(isAnnotatedWith(Identity.class));

		return annotateFieldWith(annotation, isIdentifierField, filterAnnotations);
	}

	@SafeVarargs
	public final JMoleculesTypeBuilder annotateTypedIdentifierWith(Class<? extends Annotation> annotation,
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
	public final JMoleculesTypeBuilder annotateFieldWith(Class<? extends Annotation> annotation,
			Junction<FieldDescription> selector, Class<? extends Annotation>... filterAnnotations) {
		return annotateFieldWith(PluginUtils.getAnnotation(annotation), selector, filterAnnotations);
	}

	@SafeVarargs
	public final JMoleculesTypeBuilder annotateFieldWith(AnnotationDescription annotation,
			Junction<FieldDescription> selector, Class<? extends Annotation>... filterAnnotations) {

		Junction<AnnotationSource> alreadyAnnotated = ElementMatchers.isAnnotatedWith(annotation.getAnnotationType());

		for (Class<? extends Annotation> filterAnnotation : filterAnnotations) {
			alreadyAnnotated = alreadyAnnotated.or(ElementMatchers.isAnnotatedWith(filterAnnotation));
		}

		AsmVisitorWrapper annotationSpec = new MemberAttributeExtension.ForField()
				.annotate(annotation)
				.on(PluginUtils.defaultMapping(logger, selector.and(not(alreadyAnnotated)), annotation));

		return JMoleculesTypeBuilder.of(logger, builder.visit(annotationSpec));
	}

	@SafeVarargs
	public final JMoleculesTypeBuilder annotateAnnotatedIdentifierWith(Class<? extends Annotation> annotation,
			Class<? extends Annotation>... filterAnnotations) {
		return annotateFieldWith(annotation, isAnnotatedWith(Identity.class), filterAnnotations);
	}

	public JMoleculesTypeBuilder map(Function<JMoleculesTypeBuilder, JMoleculesTypeBuilder> function) {
		return function.apply(this);
	}

	public JMoleculesTypeBuilder map(BiFunction<JMoleculesTypeBuilder, Log, JMoleculesTypeBuilder> mapper) {
		return mapper.apply(this, logger);
	}

	public JMoleculesTypeBuilder mapBuilder(Function<Builder<?>, Builder<?>> mapper) {
		return JMoleculesTypeBuilder.of(logger, mapper.apply(builder));
	}

	public JMoleculesTypeBuilder mapBuilder(BiFunction<Builder<?>, Log, Builder<?>> mapper) {
		return JMoleculesTypeBuilder.of(logger, mapper.apply(builder, logger));
	}

	public JMoleculesTypeBuilder mapBuilder(Predicate<JMoleculesTypeBuilder> filter,
			Function<Builder<?>, Builder<?>> mapper) {

		return filter.test(this)
				? JMoleculesTypeBuilder.of(logger, mapper.apply(builder))
				: this;
	}

	public JMoleculesTypeBuilder map(Predicate<JMoleculesTypeBuilder> filter,
			Function<JMoleculesTypeBuilder, JMoleculesTypeBuilder> mapper) {

		return filter.test(this)
				? mapper.apply(this)
				: this;
	}

	public JMoleculesTypeBuilder mapLogged(Predicate<JMoleculesTypeBuilder> filter,
			BiFunction<JMoleculesTypeBuilder, Log, JMoleculesTypeBuilder> mapper) {

		return filter.test(this)
				? mapper.apply(this, logger)
				: this;
	}

	public JMoleculesType mapIdField(BiFunction<InDefinedShape, JMoleculesTypeBuilder, JMoleculesTypeBuilder> mapper) {
		return findIdField().map(it -> mapper.apply(it, this)).orElse(this);
	}

	public JMoleculesTypeBuilder addDefaultConstructorIfMissing() {

		return mapBuilder((builder, logger) -> {

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
	private final JMoleculesTypeBuilder addAnnotationIfMissing(Class<? extends Annotation> annotation,
			Class<? extends Annotation>... exclusions) {

		return addAnnotationIfMissing(__ -> annotation, exclusions);
	}

	@SafeVarargs
	private final JMoleculesTypeBuilder addAnnotationIfMissing(
			Function<TypeDescription, Class<? extends Annotation>> producer,
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

		return JMoleculesTypeBuilder.of(logger, builder.annotateType(PluginUtils.getAnnotation(annotation)));
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
}
