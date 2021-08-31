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
package org.jmolecules.spring.jpa;

import java.util.function.Supplier;

import javax.persistence.AttributeConverter;

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Identifier;
import org.jmolecules.spring.AssociationToPrimitivesConverter;
import org.jmolecules.spring.PrimitivesToAssociationConverter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.Assert;

/**
 * A JPA {@link AttributeConverter} to convert between {@link Association}s and {@link Identifier} primitives.
 *
 * @author Oliver Drotbohm
 * @see PrimitivesToAssociationConverter
 * @see AssociationToPrimitivesConverter
 */
public class AssociationAttributeConverter<T extends AggregateRoot<T, ID>, ID extends Identifier<T, ID>, S>
		implements AttributeConverter<Association<T, ID>, S> {

	private static ConversionService CONVERSION_SERVICE = DefaultConversionService.getSharedInstance();
	private static TypeDescriptor OBJECT_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(Object.class);

	private final PrimitivesToAssociationConverter<?, ?> toAssociation;
	private final AssociationToPrimitivesConverter<?, ?> toPrimitive;
	private final TypeDescriptor idTypeDescriptor;

	/**
	 * Creates a new {@link AssociationAttributeConverter} for the given {@link Identifier} type.
	 *
	 * @param itType must not be {@literal null}.
	 */
	protected AssociationAttributeConverter(Class<ID> itType) {

		Assert.notNull(itType, "Identifier type must not be null!");

		Supplier<ConversionService> conversionService = () -> CONVERSION_SERVICE;

		this.toAssociation = new PrimitivesToAssociationConverter<>(conversionService);
		this.toPrimitive = new AssociationToPrimitivesConverter<>(conversionService);

		ResolvableType associationType = ResolvableType.forClassWithGenerics(Association.class, Object.class, itType);

		this.idTypeDescriptor = new TypeDescriptor(associationType, null, null);
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
