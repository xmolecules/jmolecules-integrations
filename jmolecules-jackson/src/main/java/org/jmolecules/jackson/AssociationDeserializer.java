/*
 * Copyright 2021-2024 the original author or authors.
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

import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Identifier;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Jackson {@link JsonDeserializer} for jMolecules {@link Association} instances.
 *
 * @author Oliver Drotbohm
 */
class AssociationDeserializer extends StdDeserializer<Association<?, ?>> implements ContextualDeserializer {

	private static final long serialVersionUID = 2278790059349867093L;

	private final BeanProperty property;

	/**
	 * Creates a new {@link AssociationDeserializer}.
	 */
	AssociationDeserializer() {
		this(null);
	}

	/**
	 * Creates a new {@link AssociationDeserializer} for the given {@link BeanProperty}.
	 *
	 * @param property must not be {@literal null}.
	 */
	AssociationDeserializer(BeanProperty property) {

		super(Association.class);

		this.property = property;
	}

	/*
	 * (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
	 */
	@Override
	public Association<?, ?> deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {

		Assert.state(property != null, "Can only deserialize Association properties.");

		JavaType[] typeParameters = property.getType().findTypeParameters(Association.class);
		JsonDeserializer<Object> deserializer = ctxt.findRootValueDeserializer(typeParameters[1]);
		Identifier deserialize = (Identifier) deserializer.deserialize(p, ctxt);

		return Association.forId(deserialize);
	}

	/*
	 * (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.deser.ContextualDeserializer#createContextual(com.fasterxml.jackson.databind.DeserializationContext, com.fasterxml.jackson.databind.BeanProperty)
	 */
	@Override
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
			throws JsonMappingException {
		return new AssociationDeserializer(property);
	}
}
