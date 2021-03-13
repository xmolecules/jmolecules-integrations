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

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.function.Function;

import javax.persistence.*;

import org.jmolecules.jpa.JMoleculesJpa;

public class JMoleculesJpaPlugin extends JMoleculesPluginSupport {

	private static final PluginLogger logger = new PluginLogger("JPA");

	static final String NULLABILITY_METHOD_NAME = "__verifyNullability";

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
				.mapBuilder(JMoleculesType::isAggregateRoot, this::handleAggregateRoot)
				.map(JMoleculesType::isEntity, this::handleEntity)
				.map(JMoleculesType::isAssociation, this::handleAssociation)
				.map(JMoleculesType::isIdentifier, this::handleIdentifier)
				.conclude();
	}

	private static <T extends AnnotationSource> ElementMatcher.Junction<T> hasJpaRelationShipAnnotation() {

		return isAnnotatedWith(OneToOne.class)
				.or(isAnnotatedWith(OneToMany.class))
				.or(isAnnotatedWith(ManyToOne.class))
				.or(isAnnotatedWith(ManyToMany.class));
	}

	private JMoleculesType handleIdentifier(JMoleculesType type) {

		return type.implement(Serializable.class)
				.addDefaultConstructorIfMissing()
				.annotateTypeIfMissing(Embeddable.class);
	}

	private JMoleculesType handleAssociation(JMoleculesType type) {

		return type.addDefaultConstructorIfMissing()
				.annotateTypeIfMissing(Embeddable.class);
	}

	private JMoleculesType handleEntity(JMoleculesType type) {

		Function<TypeDescription, Class<? extends Annotation>> selector = it -> !type.isAggregateRoot()
				&& type.isAbstract()
						? MappedSuperclass.class
						: javax.persistence.Entity.class;

		return type.addDefaultConstructorIfMissing()
				.annotateIdentifierWith(EmbeddedId.class, Id.class)
				.annotateTypeIfMissing(selector, javax.persistence.Entity.class, MappedSuperclass.class)
				.map(this::declareNullVerificationMethod);
	}

	private Builder<?> handleAggregateRoot(Builder<?> builder) {

		// Default entity references to OneToOne mapping
		AnnotationDescription oneToOneDescription = createCascadingAnnotation(OneToOne.class);

		builder = builder.field(PluginUtils.defaultMapping(logger, fieldType(isEntity())
				.and(not(hasJpaRelationShipAnnotation())), oneToOneDescription))
				.annotateField(oneToOneDescription);

		// Default collection entity references to @OneToMany mapping
		AnnotationDescription oneToManyDescription = createCascadingAnnotation(OneToMany.class);

		return builder.field(PluginUtils.defaultMapping(logger, genericFieldType(isCollectionOfEntity())
				.and(not(hasJpaRelationShipAnnotation())), oneToManyDescription))
				.annotateField(oneToManyDescription);
	}

	private static AnnotationDescription createCascadingAnnotation(Class<? extends Annotation> type) {
		return AnnotationDescription.Builder.ofType(type)
				.defineEnumerationArray("cascade", CascadeType.class, CascadeType.ALL)
				.build();
	}

	private Builder<?> declareNullVerificationMethod(Builder<?> builder, PluginLogger logger) {

		TypeDescription type = builder.toTypeDescription();
		String typeName = PluginUtils.abbreviate(type);

		if (type.isAbstract()) {
			logger.info("{} - Not generating nullability method for abstract type.", typeName);
			return builder;
		}

		if (type.getDeclaredMethods().filter(it -> it.getName().equals(NULLABILITY_METHOD_NAME)).size() > 0) {
			logger.info("{} - Found existing nullability method.", typeName);
			return builder;
		}

		logger.info("{} - Adding nullability verification method.", typeName);

		return builder.defineMethod(NULLABILITY_METHOD_NAME, void.class, Visibility.PACKAGE_PRIVATE)
				.intercept(MethodDelegation.to(JMoleculesJpa.class))
				.annotateMethod(PluginUtils.getAnnotation(PrePersist.class))
				.annotateMethod(PluginUtils.getAnnotation(PostLoad.class));
	}
}
