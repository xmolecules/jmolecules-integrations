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

import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.jmolecules.bytebuddy.PluginUtils.*;

import lombok.RequiredArgsConstructor;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.Advice.FieldValue;
import net.bytebuddy.asm.Advice.OnMethodExit;
import net.bytebuddy.description.field.FieldDescription.InDefinedShape;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.EqualsMethod;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.HashCodeMethod;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import org.jmolecules.spring.data.MutablePersistable;
import org.springframework.data.domain.Persistable;
import org.springframework.stereotype.Component;

/**
 * @author Oliver Drotbohm
 */
@RequiredArgsConstructor(staticName = "of")
class PersistableImplementor {

	static final String IS_NEW_METHOD = "isNew";
	static final String MARK_NOT_NEW_METHOD = "__jMolecules__markNotNew";
	static final String IS_NEW_FIELD = "__jMolecules__isNew";

	private final PersistableOptions options;

	JMoleculesType implementPersistable(JMoleculesType type) {

		if (type.isAssignableTo(Persistable.class)) {
			return type;

		}

		return type.findIdField()
				.map(field -> {

					// Select only identifier field
					ElementMatcher<? super InDefinedShape> isIdField = candidate -> !candidate.getName()
							.equals(field.getName());

					return type.implement(MutablePersistable.class, type.getTypeDescription(), field.getType())
							.mapBuilder(this::generateCallbacks)
							.mapBuilder(it -> !it.hasField(named(IS_NEW_FIELD)), this::generateIsNewField)
							.mapBuilder(it -> !it.hasMethod(isEquals()), it -> generateEquals(it, isIdField))
							.mapBuilder(it -> !it.hasMethod(isHashCode()), it -> generateHashCode(it, isIdField))
							.mapBuilder(it -> !it.hasMethod(hasMethodName(IS_NEW_METHOD)), this::generateIsNewMethod);

				}).orElse(type);
	}

	private Builder<?> generateCallbacks(Builder<?> builder) {

		if (options.callbackInterface != null) {

			builder = builder.defineMethod(MARK_NOT_NEW_METHOD, void.class, Visibility.PUBLIC)
					.intercept(FieldAccessor.ofField(IS_NEW_FIELD).setsValue(false))
					.require(createCallbackComponent(builder.toTypeDescription()));
		}

		if (options.callbackAnnotations.length > 0) {
			builder = new LifecycleMethods(builder, options.callbackAnnotations)
					.apply(() -> Advice.to(NotNewSetter.class),
							() -> FieldAccessor.ofField(IS_NEW_FIELD).setsValue(false));
		}

		return builder;
	}

	private Builder<?> generateIsNewField(Builder<?> builder) {

		// Add isNew field
		builder = builder.defineField(IS_NEW_FIELD, boolean.class, Visibility.PRIVATE)
				.value(true)
				.annotateField(getAnnotation(options.isNewPropertyAnnotation));

		// Tweak constructors to set the newly introduced field to true.
		return builder.visit(Advice.to(IsNewInitializer.class).on(ElementMatchers.isConstructor()));
	}

	private Builder<?> generateIsNewMethod(Builder<?> builder) {

		return builder.defineMethod(IS_NEW_METHOD, boolean.class, Visibility.PUBLIC)
				.intercept(FieldAccessor.ofField(IS_NEW_FIELD));
	}

	private Builder<?> generateEquals(Builder<?> builder, ElementMatcher<? super InDefinedShape> idField) {

		return builder.defineMethod("equals", boolean.class, Visibility.PUBLIC)
				.withParameter(Object.class)
				.intercept(EqualsMethod.isolated().withIgnoredFields(idField));
	}

	private Builder<?> generateHashCode(Builder<?> builder, ElementMatcher<? super InDefinedShape> idField) {

		return builder.defineMethod("hashCode", int.class, Visibility.PUBLIC)
				.intercept(HashCodeMethod.usingDefaultOffset().withIgnoredFields(idField));
	}

	private DynamicType createCallbackComponent(TypeDescription typeDescription) {

		Generic callbackType = Generic.Builder
				.parameterizedType(new TypeDescription.ForLoadedType(options.callbackInterface), typeDescription).build();

		return new ByteBuddy(ClassFileVersion.JAVA_V8)
				.subclass(callbackType)
				.name(typeDescription.getName().concat("JMoleculesCallbacks"))
				.annotateType(PluginUtils.getAnnotation(Component.class))
				.make();
	}

	public static class IsNewInitializer {

		@OnMethodExit
		public static void initIsNewAsTrue(@FieldValue(value = IS_NEW_FIELD, readOnly = false) boolean value) {
			value = true;
		}
	}

	public static class NotNewSetter {

		@OnMethodExit
		public static void initIsNewAsTrue(@FieldValue(value = IS_NEW_FIELD, readOnly = false) boolean value) {
			value = false;
		}
	}
}
