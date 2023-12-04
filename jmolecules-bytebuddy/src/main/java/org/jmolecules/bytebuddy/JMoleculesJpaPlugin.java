/*
 * Copyright 2020-2021 the original author or authors.
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
import static org.jmolecules.bytebuddy.JMoleculesElementMatchers.*;

import lombok.NoArgsConstructor;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.build.Plugin.WithPreprocessor;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeDescription.ForLoadedType;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.matcher.ElementMatcher;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.jmolecules.bytebuddy.PluginLogger.Log;
import org.jmolecules.jpa.JMoleculesJpa;

@NoArgsConstructor
public class JMoleculesJpaPlugin implements LoggingPlugin, WithPreprocessor {

	static final String NULLABILITY_METHOD_NAME = "__verifyNullability";

	private Jpa jpa;
	private Class<? extends Annotation> embeddableInstantiatorAnnotationType;

	public JMoleculesJpaPlugin(Jpa jpa, ClassWorld world) {
		init(jpa, world);
	}

	private void init(Jpa jpa, ClassWorld world) {

		if (this.jpa != null) {
			return;
		}

		this.jpa = jpa;

		if (world.isAvailable("org.hibernate.annotations.EmbeddableInstantiator")) {
			this.embeddableInstantiatorAnnotationType = jpa.getType("org.hibernate.annotations.EmbeddableInstantiator");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.bytebuddy.JMoleculesPluginSupport#onPreprocess(net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public void onPreprocess(TypeDescription typeDescription, ClassFileLocator classFileLocator) {

		ClassWorld world = ClassWorld.of(classFileLocator);
		init(Jpa.getJavaPersistence(world).get(), world);
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.matcher.ElementMatcher#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(TypeDescription target) {

		if (target.isAnnotation() || PluginUtils.isCglibProxyType(target)) {
			return false;
		}

		boolean implementsJMoleculesInterface = !target.getInterfaces().filter(nameStartsWith("org.jmolecules")).isEmpty();

		boolean hasJMoleculesAnnotation = Stream
				.concat(target.getDeclaredAnnotations().stream(), target.getInheritedAnnotations().stream())
				.anyMatch(it -> it.getAnnotationType().getName().startsWith("org.jmolecules"));

		if (implementsJMoleculesInterface || hasJMoleculesAnnotation) {
			return true;
		}

		Generic superType = target.getSuperClass();

		return superType == null || superType.represents(Object.class) ? false :

				matches(superType.asErasure()) || target.isRecord();
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.build.Plugin#apply(net.bytebuddy.dynamic.DynamicType.Builder, net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public Builder<?> apply(Builder<?> builder, TypeDescription type, ClassFileLocator classFileLocator) {

		Log log = PluginLogger.INSTANCE.getLog(type, "JPA");

		return JMoleculesType.of(log, builder)
				.map(JMoleculesType::isEntity, this::handleEntity)
				.map(JMoleculesType::isAssociation, this::handleAssociation)
				.map(JMoleculesType::isIdentifier, this::handleIdentifier)
				.map(JMoleculesType::isValueObject, this::handleValueObject)
				.map(JMoleculesType::isRecord, it -> it.annotateTypeIfMissing(jpa.getAnnotation("Embeddable")))
				.map(this::applyRecordInstantiator)
				.conclude();
	}

	private <T extends AnnotationSource> ElementMatcher.Junction<T> hasJpaRelationShipAnnotation() {

		return isAnnotatedWith(jpa.getAnnotation("OneToOne"))
				.or(isAnnotatedWith(jpa.getAnnotation("OneToMany")))
				.or(isAnnotatedWith(jpa.getAnnotation("ManyToOne")))
				.or(isAnnotatedWith(jpa.getAnnotation("ManyToMany")));
	}

	private JMoleculesType handleIdentifier(JMoleculesType type) {
		return handleValueObject(type.implement(Serializable.class));
	}

	private JMoleculesType handleAssociation(JMoleculesType type) {

		return type.addDefaultConstructorIfMissing()
				.annotateTypeIfMissing(jpa.getAnnotation("Embeddable"));
	}

	private JMoleculesType handleEntity(JMoleculesType type) {

		Function<TypeDescription, Class<? extends Annotation>> selector = it -> !type.isAggregateRoot()
				&& type.isAbstract() ? jpa.getAnnotation("MappedSuperclass") : jpa.getAnnotation("Entity");

		Class<Annotation> embeddedId = jpa.getAnnotation("EmbeddedId");
		Class<Annotation> id = jpa.getAnnotation("Id");

		return type.addDefaultConstructorIfMissing()
				.annotateTypedIdentifierWith(embeddedId, id)
				.annotateAnnotatedIdentifierWith(id, embeddedId)
				.annotateTypeIfMissing(selector, jpa.getAnnotation("Entity"), jpa.getAnnotation("MappedSuperclass"))
				.map(this::declareNullVerificationMethod)
				.map(this::defaultToEntityAssociations)
				.map(this::defaultCollectionOfValueObjects);
	}

	private JMoleculesType defaultToEntityAssociations(JMoleculesType type) {

		Junction<FieldDescription> doesNotHaveAtJoinColumn = not(isAnnotatedWith(jpa.getAnnotation("JoinColumn")));
		Junction<FieldDescription> doesNotHaveRelationShipAnnotation = not(hasJpaRelationShipAnnotation());

		Junction<FieldDescription> isCollectionOfEntities = genericFieldType(isCollectionOfEntity());
		Junction<FieldDescription> isEntity = fieldType(isEntity());

		Junction<FieldDescription> isUndefaultedEntity = isEntity.and(doesNotHaveRelationShipAnnotation);
		Junction<FieldDescription> isUndefaultCollectionOfEntities = isCollectionOfEntities
				.and(doesNotHaveRelationShipAnnotation);

		boolean mapEager = !type.hasMoreThanOneField(isCollectionOfEntities);

		AnnotationDescription oneToOneDescription = createRelationshipAnnotation(jpa.getAnnotation("OneToOne"), true);
		AnnotationDescription oneToManyDescription = createRelationshipAnnotation(jpa.getAnnotation("OneToMany"), mapEager);

		AnnotationDescription joinColumnAnnotation = getJoinColumnAnnotation();

		// Default @OneToOne
		JMoleculesType result = type.annotateFieldWith(oneToOneDescription, isUndefaultedEntity)

				// Default @JoinColumn if no relationship annotation and no @JoinColumn found
				.annotateFieldWith(joinColumnAnnotation, isUndefaultedEntity.and(doesNotHaveAtJoinColumn))

				// Default @OneToMany
				.annotateFieldWith(oneToManyDescription, isUndefaultCollectionOfEntities)

				// Default @JoinColumn if no relationship annotation and no @JoinColumn found
				.annotateFieldWith(joinColumnAnnotation, isUndefaultCollectionOfEntities.and(doesNotHaveAtJoinColumn));

		// Add @Fetch for lazy, Hibernate-mapped @OneToManys
		if (!mapEager && jpa.isHibernate()) {

			Class<Fetch> fetchType = jpa.getType("org.hibernate.annotations.Fetch");
			Class<FetchMode> fetchModeType = jpa.getType("org.hibernate.annotations.FetchMode");

			AnnotationDescription fetchAnnotation = AnnotationDescription.Builder.ofType(fetchType)
					.define("value", Enum.valueOf(fetchModeType, "SUBSELECT"))
					.build();

			result = result.annotateFieldWith(fetchAnnotation, isCollectionOfEntities);
		}

		return result;
	}

	private JMoleculesType defaultCollectionOfValueObjects(JMoleculesType type) {

		Junction<FieldDescription> matcher = genericFieldType(isCollectionOfValueObject());

		return type.hasField(matcher)
				? type.annotateFieldWith(getElementCollectionAnnotation(), matcher)
				: type;
	}

	@SuppressWarnings("unchecked")
	private <T extends Enum<T>> AnnotationDescription createRelationshipAnnotation(Class<? extends Annotation> type,
			boolean eager) {

		Class<T> cascadeType = jpa.getType("CascadeType");
		T value = jpa.getCascadeTypeAll();
		T fetchType = eager ? jpa.getFetchTypeEager() : jpa.getFetchTypeLazy();

		return AnnotationDescription.Builder.ofType(type)
				.define("fetch", fetchType)
				.define("orphanRemoval", true)
				.defineEnumerationArray("cascade", cascadeType, value)
				.build();
	}

	private AnnotationDescription getJoinColumnAnnotation() {
		return AnnotationDescription.Builder.ofType(jpa.getType("JoinColumn")).build();
	}

	private Builder<?> declareNullVerificationMethod(Builder<?> builder, Log logger) {

		TypeDescription type = builder.toTypeDescription();

		if (type.isAbstract()) {
			logger.info("Not adding nullability verification to abstract type.");
			return builder;
		}

		if (type.getDeclaredMethods().filter(it -> it.getName().equals(NULLABILITY_METHOD_NAME)).size() > 0) {
			logger.info("Nullability verification already added.");
			return builder;
		}

		// Add marker method, so that we know, we already processed the class
		builder = builder.defineMethod(NULLABILITY_METHOD_NAME, void.class, Visibility.PACKAGE_PRIVATE)
				.intercept(StubMethod.INSTANCE);

		Function<String, Advice> advice = it -> {

			logger.info("Adding nullability verification to existing callback method {}().", it);

			return Advice.to(JMoleculesJpa.class);
		};

		Supplier<Implementation> implementation = () -> {

			logger.info("Adding nullability verification using new callback methods.");

			return MethodDelegation.to(JMoleculesJpa.class);
		};

		return new LifecycleMethods(builder, jpa.getAnnotation("PrePersist"), jpa.getAnnotation("PostLoad"))
				.apply(advice, implementation);
	}

	private JMoleculesType handleValueObject(JMoleculesType type) {

		return type.addDefaultConstructorIfMissing()
				.annotateTypeIfMissing(jpa.getAnnotation("Embeddable"));
	}

	private Builder<?> applyRecordInstantiator(Builder<?> builder, Log logger) {

		TypeDescription description = builder.toTypeDescription();

		// No record or Hibernate 6 present
		if (!description.isRecord() || embeddableInstantiatorAnnotationType == null) {
			return builder;
		}

		// Already annotated
		if (description.getDeclaredAnnotations().isAnnotationPresent(embeddableInstantiatorAnnotationType)) {

			logger.info("Found explicit @EmbeddableInstantiator.");

			return builder;
		}

		logger.info("Adding @EmbeddableInstantiator for record.");

		// Parent type information
		Class<?> instantiatorBaseType = jpa.getType("org.jmolecules.hibernate.RecordInstantiator");
		ForLoadedType supeType = new TypeDescription.ForLoadedType(instantiatorBaseType);
		Constructor<?> constructor = getConstructor(instantiatorBaseType, Class.class);

		// Dedicated instantiator class for this particular record type
		Unloaded<?> instantiatorType = new ByteBuddy(ClassFileVersion.JAVA_V8)
				.with(new ReferenceTypePackageNamingStrategy(description))
				.subclass(supeType)
				.defineConstructor(Visibility.PACKAGE_PRIVATE)
				.intercept(MethodCall.invoke(constructor).onSuper().with(description))
				.make();

		builder = builder.require(instantiatorType);

		// Add the annotation
		return builder.annotateType(AnnotationDescription.Builder.ofType(embeddableInstantiatorAnnotationType)
				.define("value", instantiatorType.getTypeDescription())
				.build());
	}

	private static Constructor<?> getConstructor(Class<?> type, Class<?>... parameters) {

		try {
			return type.getDeclaredConstructor(parameters);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private AnnotationDescription getElementCollectionAnnotation() {

		Enum<?> fetchType = jpa.getFetchTypeEager();

		return AnnotationDescription.Builder.ofType(jpa.getType("ElementCollection"))
				.define("fetch", fetchType)
				.build();
	}
}
