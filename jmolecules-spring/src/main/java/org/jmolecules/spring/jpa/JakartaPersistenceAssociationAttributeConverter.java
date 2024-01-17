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
package org.jmolecules.spring.jpa;

import jakarta.persistence.AttributeConverter;

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Identifier;
import org.springframework.core.convert.TypeDescriptor;

/**
 * Jakarta Persistence 3.0 flavor of the {@link AssociationAttributeConverter}.
 *
 * @author Oliver Drotbohm
 */
public class JakartaPersistenceAssociationAttributeConverter<T extends AggregateRoot<T, ID>, ID extends Identifier, S>
		extends AssociationAttributeConverter<T, ID, S>
		implements AttributeConverter<Association<T, ID>, S> {

	/**
	 * Creates a new {@link JakartaPersistenceAssociationAttributeConverter} for the given id type.
	 *
	 * @param idType must not be {@literal null}.
	 */
	public JakartaPersistenceAssociationAttributeConverter(Class<ID> idType) {
		super(idType);
	}

	/*
	 * (non-Javadoc)
	 * @see javax.persistence.AttributeConverter#convertToDatabaseColumn(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public S convertToDatabaseColumn(Association<T, ID> attribute) {
		return (S) toPrimitive.convert(attribute, TypeDescriptor.forObject(attribute), OBJECT_TYPE_DESCRIPTOR);
	}

	/*
	 * (non-Javadoc)
	 * @see javax.persistence.AttributeConverter#convertToEntityAttribute(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Association<T, ID> convertToEntityAttribute(S dbData) {
		return (Association<T, ID>) toAssociation.convert(dbData, TypeDescriptor.forObject(dbData), idTypeDescriptor);
	}
}
