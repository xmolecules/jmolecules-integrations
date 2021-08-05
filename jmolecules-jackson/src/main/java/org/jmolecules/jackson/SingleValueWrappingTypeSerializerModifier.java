/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmolecules.jackson;

import java.io.IOException;
import java.util.List;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;
import org.springframework.core.annotation.AnnotatedElementUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * {@link BeanSerializerModifier} to serialize properties that are {@link ValueObject}s which in turn only carry a
 * single attribute as just that attribute.
 *
 * @author Oliver Drotbohm
 */
class SingleValueWrappingTypeSerializerModifier extends BeanSerializerModifier {

	/*
	 * (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.ser.BeanSerializerModifier#modifySerializer(com.fasterxml.jackson.databind.SerializationConfig, com.fasterxml.jackson.databind.BeanDescription, com.fasterxml.jackson.databind.JsonSerializer)
	 */
	@Override
	public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription description,
			JsonSerializer<?> serializer) {

		List<BeanPropertyDefinition> properties = description.findProperties();

		if (properties.size() != 1) {
			return super.modifySerializer(config, description, serializer);
		}

		Class<?> type = description.getBeanClass();

		if (AnnotatedElementUtils.hasAnnotation(type, ValueObject.class)
				|| org.jmolecules.ddd.types.ValueObject.class.isAssignableFrom(type)
				|| Identifier.class.isAssignableFrom(type)) {
			return new SingleAttributeSerializer(properties.get(0).getAccessor());
		}

		return super.modifySerializer(config, description, serializer);
	}

	private static class SingleAttributeSerializer extends StdSerializer<Object> {

		private static final long serialVersionUID = 3242761376607559434L;

		private final AnnotatedMember member;

		public SingleAttributeSerializer(AnnotatedMember member) {

			super(Object.class);

			this.member = member;
		}

		/*
		 * (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.ser.std.StdSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
		 */
		@Override
		public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {

			Object nested = member.getValue(value);

			provider.findValueSerializer(nested.getClass()).serialize(nested, gen, provider);
		}
	}
}
