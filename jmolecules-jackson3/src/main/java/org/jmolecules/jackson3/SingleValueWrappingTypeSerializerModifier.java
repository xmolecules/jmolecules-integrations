/*
 * Copyright 2016-2025 the original author or authors.
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
package org.jmolecules.jackson3;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.BeanDescription.Supplier;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.ser.ValueSerializerModifier;
import tools.jackson.databind.ser.std.StdSerializer;

import java.util.List;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * {@link BeanSerializerModifier} to serialize properties that are {@link ValueObject}s which in turn only carry a
 * single attribute as just that attribute.
 *
 * @author Oliver Drotbohm
 */
class SingleValueWrappingTypeSerializerModifier extends ValueSerializerModifier {

	private static final long serialVersionUID = -7923324296771766248L;

	/*
	 * (non-Javadoc)
	 * @see tools.jackson.databind.ser.ValueSerializerModifier#modifySerializer(tools.jackson.databind.SerializationConfig, tools.jackson.databind.BeanDescription.Supplier, tools.jackson.databind.ValueSerializer)
	 */
	@Override
	public ValueSerializer<?> modifySerializer(SerializationConfig config, Supplier supplier,
			ValueSerializer<?> serializer) {

		BeanDescription description = supplier.get();
		List<BeanPropertyDefinition> properties = description.findProperties();

		if (properties.size() != 1) {
			return super.modifySerializer(config, supplier, serializer);
		}

		Class<?> type = description.getBeanClass();

		if (AnnotatedElementUtils.hasAnnotation(type, ValueObject.class)
				|| org.jmolecules.ddd.types.ValueObject.class.isAssignableFrom(type)
				|| Identifier.class.isAssignableFrom(type)) {
			return new SingleAttributeSerializer(properties.get(0).getAccessor());
		}

		return super.modifySerializer(config, supplier, serializer);
	}

	private static class SingleAttributeSerializer extends StdSerializer<Object> {

		private final AnnotatedMember member;

		public SingleAttributeSerializer(AnnotatedMember member) {

			super(Object.class);

			this.member = member;
		}

		/*
		 * (non-Javadoc)
		 * @see tools.jackson.databind.ser.std.StdSerializer#serialize(java.lang.Object, tools.jackson.core.JsonGenerator, tools.jackson.databind.SerializationContext)
		 */
		@Override
		public void serialize(Object value, JsonGenerator gen, SerializationContext provider) throws JacksonException {

			Object nested = member.getValue(value);

			provider.findValueSerializer(nested.getClass()).serialize(nested, gen, provider);
		}
	}
}
