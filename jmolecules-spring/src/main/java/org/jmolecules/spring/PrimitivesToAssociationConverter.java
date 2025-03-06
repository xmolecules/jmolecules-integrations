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

import lombok.Value;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Identifier;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

/**
 * Converter to create {@link Association} instances from either {@link Identifier} primitives or {@link Identifier}
 * instances themselves.
 *
 * @author Oliver Drotbohm
 */
public class PrimitivesToAssociationConverter<T extends AggregateRoot<T, Identifier>>
		implements GenericConverter {

	private static final Map<FactoryMethodKey, Method> CACHE = new ConcurrentReferenceHashMap<>();

	private final PrimitivesToIdentifierConverter delegate;

	/**
	 * Creates a new {@link PrimitivesToAssociationConverter} using the given {@link ConversionService} for intermediate
	 * conversions.
	 *
	 * @param conversionService must not be {@literal null}.
	 */
	public PrimitivesToAssociationConverter(Supplier<? extends ConversionService> conversionService) {
		this(new PrimitivesToIdentifierConverter(conversionService));
	}

	/**
	 * Creates a new {@link PrimitivesToAssociationConverter} using the given delegate
	 * {@link PrimitivesToIdentifierConverter} for intermediate conversions.
	 *
	 * @param delegate must not be {@literal null}.
	 */
	public PrimitivesToAssociationConverter(PrimitivesToIdentifierConverter delegate) {

		Assert.notNull(delegate, "PrimitivesToIdentifierConverter must not be null!");

		this.delegate = delegate;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.GenericConverter#getConvertibleTypes()
	 */
	@NonNull
	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object.class, Association.class));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.GenericConverter#convert(java.lang.Object, org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	@Nullable
	@Override
	public Association<?, ?> convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

		if (source == null) {
			return null;
		}

		ResolvableType type = targetType.getResolvableType();
		Class<?> identifierType = type.as(Association.class) //
				.getGeneric(1) //
				.resolve(Identifier.class);
		Identifier id = resolveIdentifier(source, type, identifierType);

		if (type.resolve(Object.class).equals(Association.class)) {
			return Association.forId(id);
		}

		Class<?> associationType = type.resolve(Association.class);
		FactoryMethodKey key = FactoryMethodKey.of(associationType, identifierType);
		Method method = CACHE.computeIfAbsent(key, it -> it.findFactoryMethod());

		return (Association<?, ?>) ReflectionUtils.invokeMethod(method, null, id);
	}

	private Identifier resolveIdentifier(Object source, ResolvableType type, Class<?> identifierType) {

		if (Identifier.class.isInstance(source)) {
			return Identifier.class.cast(source);
		}

		TypeDescriptor sourceDescriptor = TypeDescriptor.forObject(source);
		TypeDescriptor identifierDescriptor = TypeDescriptor.valueOf(identifierType);

		return (Identifier) delegate.convert(source, sourceDescriptor, identifierDescriptor);
	}

	@Value(staticConstructor = "of")
	private static class FactoryMethodKey {

		Class<?> type, parameterType;

		Method findFactoryMethod() {

			Method method = ReflectionUtils.findMethod(type, "of", parameterType);
			ReflectionUtils.makeAccessible(method);

			return method;
		}
	}
}
