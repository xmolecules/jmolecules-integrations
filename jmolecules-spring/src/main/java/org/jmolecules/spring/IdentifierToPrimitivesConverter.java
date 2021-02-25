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
package org.jmolecules.spring;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jmolecules.ddd.types.Identifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

/**
 * A Spring {@link Converter} to convert from jMolecules {@link Identifier} types to primitives like {@link UUID} and
 * {@link String} assuming the {@link Identifier} implementation contains a single property of that type.
 *
 * @author Oliver Drotbohm
 */
public class IdentifierToPrimitivesConverter implements ConditionalGenericConverter {

	private static final Map<Class<?>, Optional<Field>> CACHE = new ConcurrentReferenceHashMap<>();
	private static final Set<Class<?>> DEFAULT_PRIMITIVES = new HashSet<>(Arrays.asList(String.class, UUID.class));

	private final Supplier<ConversionService> conversionService;
	private Set<Class<?>> primitives;

	public IdentifierToPrimitivesConverter(Supplier<ConversionService> conversionService) {

		this.primitives = new HashSet<>(DEFAULT_PRIMITIVES);
		this.conversionService = conversionService;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.GenericConverter#getConvertibleTypes()
	 */
	@NonNull
	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {

		return primitives.stream()
				.map(it -> new ConvertiblePair(Identifier.class, it))
				.collect(Collectors.toSet());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.ConditionalConverter#matches(org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {

		return CACHE.computeIfAbsent(sourceType.getType(), it -> detectIdentifierField(it))
				.filter(it -> isAssignableOrConvertable(it.getType(), targetType.getType()))
				.isPresent();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.GenericConverter#convert(java.lang.Object, org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	@Nullable
	@Override
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

		if (source == null) {
			return source;
		}

		Class<? extends Object> type = source.getClass();

		Field idField = CACHE.computeIfAbsent(type, it -> detectIdentifierField(type))
				.orElseThrow(() -> new IllegalStateException("Unable to find identifier field on " + type + "!"));

		Object id = ReflectionUtils.getField(idField, source);

		if (id == null) {
			throw new IllegalStateException(String.format("No identifier found on instance %s!", source.toString()));
		}

		if (targetType.getType().isInstance(id)) {
			return id;
		}

		return conversionService.get().convert(id, TypeDescriptor.forObject(id), targetType);
	}

	private Optional<Field> detectIdentifierField(Class<?> source) {

		return CACHE.computeIfAbsent(source, type -> {
			return Arrays.stream(type.getDeclaredFields())
					.filter(it -> !Modifier.isStatic(it.getModifiers()))
					.filter(it -> primitives.contains(it.getType()))
					.peek(ReflectionUtils::makeAccessible)
					.findFirst();
		});
	}

	private boolean isAssignableOrConvertable(Class<?> source, Class<?> target) {

		return source.isAssignableFrom(target)
				|| conversionService.get().canConvert(source, target);
	}
}
