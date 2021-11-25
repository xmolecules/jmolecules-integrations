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

import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Identifier;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Jackson {@link JsonSerializer} for JMolecules {@link Association} instances.
 *
 * @author Oliver Drotbohm
 */
class AssociationSerializer extends StdSerializer<Association<?, ?>> {

	private static final long serialVersionUID = 3548768237319398094L;

	/**
	 * Creates a new {@link AssociationSerializer}.
	 */
	public AssociationSerializer() {
		super(Association.class, true);
	}

	/*
	 * (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.ser.std.StdSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
	 */
	@Override
	public void serialize(Association<?, ?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {

		Identifier identifier = value.getId();

		provider.findValueSerializer(identifier.getClass())
				.serialize(identifier, gen, provider);
	}
}
