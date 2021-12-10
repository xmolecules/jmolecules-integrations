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

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.matcher.ElementMatcher;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jmolecules.jpa.JMoleculesJpa;

@NoArgsConstructor
@AllArgsConstructor
public class JMoleculesJpaPlugin extends JMoleculesPluginSupport {

	private static final PluginLogger logger = new PluginLogger("JPA");
	static final String NULLABILITY_METHOD_NAME = "__verifyNullability";

	private Jpa jpa;

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.bytebuddy.JMoleculesPluginSupport#onPreprocess(net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public void onPreprocess(TypeDescription typeDescription, ClassFileLocator classFileLocator) {

		if (jpa != null) {
			return;
		}

		this.jpa = Jpa.getJavaPersistence(ClassWorld.of(classFileLocator)).get();
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.matcher.ElementMatcher#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(TypeDescription target) {

		if (target.isAnnotation()) {
			return false;
		}

		if (!target.getInterfaces().filter(nameStartsWith("org.jmolecules")).isEmpty()) {
			return true;
		}

		Generic superType = target.getSuperClass();

		return superType == null || superType.represents(Object.class) ? false : matches(superType.asErasure());
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.build.Plugin#apply(net.bytebuddy.dynamic.DynamicType.Builder, net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public Builder<?> apply(Builder<?> builder, TypeDescription type, ClassFileLocator classFileLocator) {

		return JMoleculesType.of(logger, builder)
				.map(JMoleculesType::isEntity, this::handleEntity)
				.map(JMoleculesType::isAssociation, this::handleAssociation)
				.map(JMoleculesType::isIdentifier, this::handleIdentifier)
				.map(JMoleculesType::isValueObject, this::handleValueObject)
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

		return type.addDefaultConstructorIfMissing()
				.annotateIdentifierWith(jpa.getAnnotation("EmbeddedId"), jpa.getAnnotation("Id"))
				.annotateTypeIfMissing(selector, jpa.getAnnotation("Entity"), jpa.getAnnotation("MappedSuperclass"))
				.map(this::declareNullVerificationMethod)
				.map(this::defaultToEntityAssociations);
	}

	private Builder<?> defaultToEntityAssociations(Builder<?> builder, PluginLogger logger) {

		// Default entity references to OneToOne mapping
		AnnotationDescription oneToOneDescription = createRelationshipAnnotation(jpa.getAnnotation("OneToOne"));

		builder = builder.field(PluginUtils.defaultMapping(logger, fieldType(isEntity())
				.and(not(hasJpaRelationShipAnnotation())), oneToOneDescription))
				.annotateField(oneToOneDescription);

		// Default collection entity references to @OneToMany mapping
		AnnotationDescription oneToManyDescription = createRelationshipAnnotation(jpa.getAnnotation("OneToMany"));

		return builder.field(PluginUtils.defaultMapping(logger, genericFieldType(isCollectionOfEntity())
				.and(not(hasJpaRelationShipAnnotation())), oneToManyDescription))
				.annotateField(oneToManyDescription);
	}

	@SuppressWarnings("unchecked")
	private <T extends Enum<T>> AnnotationDescription createRelationshipAnnotation(Class<? extends Annotation> type) {

		Class<T> cascadeType = jpa.getType("CascadeType");
		T value = jpa.getCascadeTypeAll();
		T fetchType = jpa.getFetchTypeEager();

		return AnnotationDescription.Builder.ofType(type)
				.define("fetch", fetchType)
				.defineEnumerationArray("cascade", cascadeType, value)
				.build();
	}

	private Builder<?> declareNullVerificationMethod(Builder<?> builder, PluginLogger logger) {

		TypeDescription type = builder.toTypeDescription();
		String typeName = PluginUtils.abbreviate(type);

		if (type.isAbstract()) {
			logger.info("{} - Not adding nullability verification to abstract type.", typeName);
			return builder;
		}

		if (type.getDeclaredMethods().filter(it -> it.getName().equals(NULLABILITY_METHOD_NAME)).size() > 0) {
			logger.info("{} - Nullability verification already added.", typeName);
			return builder;
		}

		// Add marker method, so that we know, we already processed the class
		builder = builder.defineMethod(NULLABILITY_METHOD_NAME, void.class, Visibility.PACKAGE_PRIVATE)
				.intercept(StubMethod.INSTANCE);

		Supplier<Advice> advice = () -> {

			logger.info("{} - Adding nullability verification to existing callback methods.", typeName);

			return Advice.to(JMoleculesJpa.class);
		};

		Supplier<Implementation> implementation = () -> {

			logger.info("{} - Adding nullability verification using new callback methods.", typeName);

			return MethodDelegation.to(JMoleculesJpa.class);
		};

		return new LifecycleMethods(builder, jpa.getAnnotation("PrePersist"), jpa.getAnnotation("PostLoad"))
				.apply(advice, implementation);
	}

	private JMoleculesType handleValueObject(JMoleculesType type) {

		return type.addDefaultConstructorIfMissing()
				.annotateTypeIfMissing(jpa.getAnnotation("Embeddable"));
	}
}
