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
package org.jmolecules.jackson3;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.BeanDescription.Supplier;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.introspect.BeanPropertyDefinition;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link BeanDeserializerModifier} to use a static factory method named {@code of} on single-attribute
 * {@link ValueObject}s and {@link Identifier}s.
 *
 * @author Oliver Drotbohm
 */
class SingleValueWrappingTypeDeserializerModifier extends ValueDeserializerModifier {

	private static final long serialVersionUID = 5297887920996219863L;
	private static final AnnotationDetector DETECTOR = AnnotationDetector.getAnnotationDetector();

	/*
	 * (non-Javadoc)
	 * @see tools.jackson.databind.deser.ValueDeserializerModifier#modifyDeserializer(tools.jackson.databind.DeserializationConfig, tools.jackson.databind.BeanDescription.Supplier, tools.jackson.databind.ValueDeserializer)
	 */
	@Override
	public ValueDeserializer<?> modifyDeserializer(DeserializationConfig config, Supplier supplier,
			ValueDeserializer<?> deserializer) {

		BeanDescription descriptor = supplier.get();
		List<BeanPropertyDefinition> properties = descriptor.findProperties();

		if (properties.size() != 1) {
			return super.modifyDeserializer(config, supplier, deserializer);
		}

		Class<?> type = descriptor.getBeanClass();

		if (!DETECTOR.hasAnnotation(type, ValueObject.class)
				&& !org.jmolecules.ddd.types.ValueObject.class.isAssignableFrom(type)
				&& !Identifier.class.isAssignableFrom(type)) {
			return super.modifyDeserializer(config, supplier, deserializer);
		}

		BeanPropertyDefinition definition = properties.get(0);
		Method method = findFactoryMethodOn(type, definition.getRawPrimaryType());

		if (method != null) {

			ReflectionUtils.makeAccessible(method);
			ThrowingFunction instantiator = it -> method.invoke(null, it);

			return new InstantiatorDeserializer(descriptor.getType(), instantiator, definition.getPrimaryType());
		}

		Constructor<?> findConstructor = findConstructor(type, definition.getRawPrimaryType());

		if (findConstructor != null) {

			ReflectionUtils.makeAccessible(findConstructor);
			ThrowingFunction instantiator = it -> BeanUtils.instantiateClass(findConstructor, it);

			return new InstantiatorDeserializer(descriptor.getType(), instantiator, definition.getPrimaryType());
		}

		return super.modifyDeserializer(config, supplier, deserializer);
	}

	private static Method findFactoryMethodOn(Class<?> type, Class<?> parameterType) {

		try {

			Method method = type.getDeclaredMethod("of", parameterType);
			return Modifier.isStatic(method.getModifiers()) ? method : null;

		} catch (Exception e) {
			return null;
		}
	}

	private static Constructor<?> findConstructor(Class<?> type, Class<?> parameterType) {

		try {
			return type.getDeclaredConstructor(parameterType);
		} catch (Exception e) {
			return null;
		}
	}

	private interface ThrowingFunction {
		Object apply(Object source) throws Exception;
	}

	private static class InstantiatorDeserializer extends StdDeserializer<Object> {

		private final ThrowingFunction instantiator;
		private final JavaType parameterType, targetType;

		/**
		 * Create a new {@link InstantiatorDeserializer} for the given Method and parameter type.
		 *
		 * @param target must not be {@literal null}.
		 * @param instantiator must not be {@literal null}.
		 * @param parameterType must not be {@literal null}.
		 */
		public InstantiatorDeserializer(JavaType target, ThrowingFunction instantiator, JavaType parameterType) {

			super(Object.class);

			this.targetType = target;
			this.instantiator = instantiator;
			this.parameterType = parameterType;
		}

		/*
		 * (non-Javadoc)
		 * @see tools.jackson.databind.ValueDeserializer#deserialize(tools.jackson.core.JsonParser, tools.jackson.databind.DeserializationContext)
		 */
		@Override
		public Object deserialize(JsonParser parser, DeserializationContext context) {

			ValueDeserializer<Object> deserializer = context.findNonContextualValueDeserializer(parameterType);
			Object nested = deserializer.deserialize(parser, context);

			try {
				return instantiator.apply(nested);
			} catch (Exception o_O) {
				RuntimeException exception = new RuntimeException(String.format("Failed to instantiate %s!", targetType), o_O);
				throw JacksonException.wrapWithPath(exception, parser.currentValue(), parser.currentName());
			}
		}
	}
}
