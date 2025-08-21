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

import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;

import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Identifier;
import org.springframework.util.Assert;

/**
 * Jackson {@link JsonDeserializer} for jMolecules {@link Association} instances.
 *
 * @author Oliver Drotbohm
 */
class AssociationDeserializer extends StdDeserializer<Association<?, ?>> {

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
	 * @see tools.jackson.databind.ValueDeserializer#deserialize(tools.jackson.core.JsonParser, tools.jackson.databind.DeserializationContext)
	 */
	@Override
	public Association<?, ?> deserialize(JsonParser p, DeserializationContext ctxt) {

		Assert.state(property != null, "Can only deserialize Association properties.");

		JavaType[] typeParameters = property.getType().findTypeParameters(Association.class);
		ValueDeserializer<Object> deserializer = ctxt.findRootValueDeserializer(typeParameters[1]);
		Identifier deserialize = (Identifier) deserializer.deserialize(p, ctxt);

		return Association.forId(deserialize);
	}

	/*
	 * (non-Javadoc)
	 * @see tools.jackson.databind.ValueDeserializer#createContextual(tools.jackson.databind.DeserializationContext, tools.jackson.databind.BeanProperty)
	 */
	@Override
	public AssociationDeserializer createContextual(DeserializationContext ctxt, BeanProperty property) {
		return new AssociationDeserializer(property);
	}
}
