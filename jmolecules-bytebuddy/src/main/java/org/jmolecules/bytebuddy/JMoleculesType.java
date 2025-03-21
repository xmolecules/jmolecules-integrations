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
package org.jmolecules.bytebuddy;

import net.bytebuddy.description.field.FieldDescription.InDefinedShape;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Entity;
import org.jmolecules.ddd.types.Identifier;
import org.jmolecules.ddd.types.ValueObject;

/**
 * Logical information about a JMolecules-specific {@link TypeDescription}.
 *
 * @author Oliver Drotbohm
 */
class JMoleculesType {

	protected final TypeDescription type;

	/**
	 * Creates a new {@link JMoleculesType}.
	 *
	 * @param type must not be {@literal null}.
	 */
	JMoleculesType(TypeDescription type) {

		if (type == null) {
			throw new IllegalArgumentException("TypeDescription must not be null!");
		}

		this.type = type;
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

	public TypeDescription getTypeDescription() {
		return type;
	}
}
