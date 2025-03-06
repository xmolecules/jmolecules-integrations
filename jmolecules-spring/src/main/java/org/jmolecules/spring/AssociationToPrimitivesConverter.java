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
package org.jmolecules.spring;

import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Identifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Converts {@link Association} instances into their backing {@link Identifier} or even to the wrapped value of those.
 *
 * @author Oliver Drotbohm
 * @see IdentifierToPrimitivesConverter
 */
public class AssociationToPrimitivesConverter<T extends AggregateRoot<T, Identifier>>
		implements GenericConverter {

	private static final TypeDescriptor OBJECT_DESCRIPTOR = TypeDescriptor.valueOf(Object.class);
	private static final TypeDescriptor IDENTIFIER_DESCRIPTOR = TypeDescriptor.valueOf(Identifier.class);

	private final IdentifierToPrimitivesConverter delegate;

	/**
	 * Creates a new {@link AssociationToPrimitivesConverter} using the given {@link ConversionService} for intermediate
	 * conversions.
	 *
	 * @param conversionService must not be {@literal null}.
	 */
	public AssociationToPrimitivesConverter(Supplier<? extends ConversionService> conversionService) {
		this(new IdentifierToPrimitivesConverter(conversionService));
	}

	/**
	 * Creates a new {@link AssociationToPrimitivesConverter} using the given {@link IdentifierToPrimitivesConverter} for
	 * intermediate conversions.
	 *
	 * @param delegate must not be {@literal null}.
	 */
	public AssociationToPrimitivesConverter(IdentifierToPrimitivesConverter delegate) {

		Assert.notNull(delegate, "IdentifierToPrimitivesConverter must not be null!");

		this.delegate = delegate;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.GenericConverter#getConvertibleTypes()
	 */
	@NonNull
	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Association.class, Object.class));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.GenericConverter#convert(java.lang.Object, org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	@Nullable
	@Override
	@SuppressWarnings("unchecked")
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

		if (source == null) {
			return null;
		}

		Identifier identifier = ((Association<?, Identifier>) source).getId();

		if (identifier == null) {
			throw new IllegalStateException(
					String.format("Association target identifier must not be null for %s!", source.getClass().getSimpleName()));
		}

		// Target is identifier type and identifier matches?
		if (targetType.isAssignableTo(IDENTIFIER_DESCRIPTOR)
				&& TypeDescriptor.forObject(identifier).isAssignableTo(targetType)) {
			return identifier;
		}

		return delegate.convert(identifier, TypeDescriptor.valueOf(identifier.getClass()), OBJECT_DESCRIPTOR);
	}
}
