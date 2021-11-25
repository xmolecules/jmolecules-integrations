/*
 * Copyright 2016-2021 the original author or authors.
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

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Identifier;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * A Jackson {@link Module} to support JMolecules' {@link ValueObject}, {@link org.jmolecules.ddd.types.ValueObject} and
 * {@link Identifier} types.
 *
 * @author Oliver Drotbohm
 */
public class JMoleculesModule extends SimpleModule {

	private static final long serialVersionUID = -9056870165574640994L;

	public JMoleculesModule() {

		super("jmolecules-module");

		setDeserializerModifier(new SingleValueWrappingTypeDeserializerModifier());
		setSerializerModifier(new SingleValueWrappingTypeSerializerModifier());

		addSerializer(new AssociationSerializer());
		addDeserializer(Association.class, new AssociationDeserializer());
	}
}
