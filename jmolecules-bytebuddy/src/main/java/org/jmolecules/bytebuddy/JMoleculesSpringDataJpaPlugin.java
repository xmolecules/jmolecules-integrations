/*
 * Copyright 2021 the original author or authors.
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

import static org.jmolecules.bytebuddy.PluginUtils.*;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.Advice.FieldValue;
import net.bytebuddy.asm.Advice.OnMethodExit;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.field.FieldDescription.InDefinedShape;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.EqualsMethod;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.HashCodeMethod;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.IOException;
import java.util.Optional;

import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Transient;

import org.jmolecules.ddd.types.AggregateRoot;
import org.springframework.data.domain.Persistable;

/**
 * Plugin to implement {@link Persistable} for all {@link AggregateRoot}s.
 *
 * @author Oliver Drotbohm
 */
@Slf4j
public class JMoleculesSpringDataJpaPlugin implements Plugin {

	static final String IS_NEW_METHOD = "isNew";
	static final String MARK_NOT_NEW_METHOD = "__jMolecules__markNotNew";
	static final String IS_NEW_FIELD = "__jMolecules__isNew";

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.matcher.ElementMatcher#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(TypeDescription target) {
		return target.isAssignableTo(AggregateRoot.class);
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.build.Plugin#apply(net.bytebuddy.dynamic.DynamicType.Builder, net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public Builder<?> apply(Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {

		if (builder.toTypeDescription().isAssignableTo(Persistable.class)) {
			return builder;
		}

		InDefinedShape idField = findIdField(builder).orElse(null);

		if (idField == null) {
			return builder;
		}

		log.info("jMolecules Spring Data JPA - {} - Implementing Persistable.", typeDescription.getSimpleName());

		// Implement Persistable
		TypeDescription loadedType = Generic.Builder.rawType(Persistable.class).build().asErasure();
		builder = builder.implement(Generic.Builder.parameterizedType(loadedType, idField.getType()).build());

		// Add isNew field
		builder = builder.defineField(IS_NEW_FIELD, boolean.class, Visibility.PRIVATE)
				.value(true)
				.annotateField(getAnnotation(Transient.class));

		// Tweak constructors to set the newly introduced field to true.
		builder = builder.visit(Advice.to(Helper.class).on(ElementMatchers.isConstructor()));

		// Add lifecycle callbacks to flip isNew flag
		builder = new LifecycleMethods(builder, PrePersist.class, PostLoad.class)
				.apply(() -> Advice.to(Helper.class), () -> FieldAccessor.ofField(IS_NEW_FIELD).setsValue(false));

		// Add isNew() method
		builder = builder.defineMethod(IS_NEW_METHOD, boolean.class, Visibility.PUBLIC)
				.intercept(FieldAccessor.ofField(IS_NEW_FIELD));

		// Equals and hashCode method
		ElementMatcher<? super InDefinedShape> isIdField = it -> !it.getName().equals(idField.getName());

		builder = builder.defineMethod("equals", boolean.class, Visibility.PUBLIC)
				.withParameter(Object.class)
				.intercept(EqualsMethod.isolated().withIgnoredFields(isIdField));

		builder = builder.defineMethod("hashCode", int.class, Visibility.PUBLIC)
				.intercept(HashCodeMethod.usingDefaultOffset().withIgnoredFields(isIdField));

		return builder;
	}

	private static Optional<InDefinedShape> findIdField(Builder<?> builder) {

		TypeDescription type = builder.toTypeDescription();

		Generic superType = type.getInterfaces().stream()
				.filter(it -> it.asErasure().represents(AggregateRoot.class))
				.findFirst().orElse(null);

		if (superType == null) {
			return Optional.empty();
		}

		Generic aggregateIdType = superType.asGenericType().getTypeArguments().get(1);

		return type.getDeclaredFields().stream()
				.filter(it -> it.getType().equals(aggregateIdType))
				.findFirst();
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {}

	public static class Helper {

		@OnMethodExit
		public static void initIsNewAsTrue(@FieldValue(value = IS_NEW_FIELD, readOnly = false) boolean value) {
			value = true;
		}
	}
}
