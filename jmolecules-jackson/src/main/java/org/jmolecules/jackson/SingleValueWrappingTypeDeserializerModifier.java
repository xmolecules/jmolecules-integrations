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
package org.jmolecules.jackson;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

/**
 * {@link BeanDeserializerModifier} to use a static factory method named {@code of} on single-attribute
 * {@link ValueObject}s and {@link Identifier}s.
 *
 * @author Oliver Drotbohm
 */
class SingleValueWrappingTypeDeserializerModifier extends BeanDeserializerModifier {

	private static final AnnotationDetector DETECTOR = AnnotationDetector.getAnnotationDetector();

	/*
	 * (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.deser.BeanDeserializerModifier#modifyDeserializer(com.fasterxml.jackson.databind.DeserializationConfig, com.fasterxml.jackson.databind.BeanDescription, com.fasterxml.jackson.databind.JsonDeserializer)
	 */
	@Override
	public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription descriptor,
			JsonDeserializer<?> deserializer) {

		List<BeanPropertyDefinition> properties = descriptor.findProperties();

		if (properties.size() != 1) {
			return super.modifyDeserializer(config, descriptor, deserializer);
		}

		Class<?> type = descriptor.getBeanClass();

		if (DETECTOR.hasAnnotation(type, ValueObject.class)
				|| org.jmolecules.ddd.types.ValueObject.class.isAssignableFrom(type)
				|| Identifier.class.isAssignableFrom(type)) {

			BeanPropertyDefinition definition = properties.get(0);
			Method method = findFactoryMethodOn(type, definition.getRawPrimaryType());

			return method != null
					? new FactoryMethodDeserializer(method, definition.getPrimaryType())
					: super.modifyDeserializer(config, descriptor, deserializer);
		}

		return super.modifyDeserializer(config, descriptor, deserializer);
	}

	private static Method findFactoryMethodOn(Class<?> type, Class<?> parameterType) {

		try {

			Method method = type.getDeclaredMethod("of", parameterType);
			return Modifier.isStatic(method.getModifiers()) ? method : null;

		} catch (Exception e) {
			return null;
		}
	}

	private static class FactoryMethodDeserializer extends StdDeserializer<Object> {

		private static final long serialVersionUID = -874251080013013301L;

		private final Method method;
		private final JavaType parameterType;

		/**
		 * Create a new {@link FactoryMethodDeserializer} for the given Method and parameter type.
		 *
		 * @param method must not be {@literal null}.
		 * @param parameterType must not be {@literal null}.
		 */
		public FactoryMethodDeserializer(Method method, JavaType parameterType) {

			super(Object.class);

			method.setAccessible(true);

			this.method = method;
			this.parameterType = parameterType;
		}

		/*
		 * (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
		 */
		@Override
		public Object deserialize(JsonParser parser, DeserializationContext context)
				throws IOException, JsonProcessingException {

			JsonDeserializer<Object> deserializer = context.findNonContextualValueDeserializer(parameterType);
			Object nested = deserializer.deserialize(parser, context);

			try {
				return method.invoke(null, nested);
			} catch (Exception o_O) {
				throw new JsonParseException(parser, String.format("Failed to invoke factory method %s!", method), o_O);
			}
		}
	}
}
