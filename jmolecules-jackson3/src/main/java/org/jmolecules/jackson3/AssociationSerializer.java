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
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Identifier;

/**
 * Jackson {@link JsonSerializer} for JMolecules {@link Association} instances.
 *
 * @author Oliver Drotbohm
 */
class AssociationSerializer extends StdSerializer<Association<?, ?>> {

	AssociationSerializer() {
		super(Association.class);
	}

	/*
	 * (non-Javadoc)
	 * @see tools.jackson.databind.ValueSerializer#serialize(java.lang.Object, tools.jackson.core.JsonGenerator, tools.jackson.databind.SerializationContext)
	 */
	@Override
	public void serialize(Association<?, ?> value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {

		Identifier identifier = value.getId();

		ctxt.findValueSerializer(identifier.getClass()).serialize(identifier, gen, ctxt);
	}
}
