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

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jmolecules.ddd.types.Identifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

/**
 * A Spring {@link Converter} to convert from jMolecules {@link Identifier} types to primitives like {@link UUID} and
 * {@link String} assuming the {@link Identifier} implementation contains a single property of that type.
 *
 * @author Oliver Drotbohm
 */
public class IdentifierToPrimitivesConverter implements ConditionalGenericConverter {

	private static final Map<Class<?>, Optional<ConvertibleExtractor>> CACHE = new ConcurrentReferenceHashMap<>();

	private final Supplier<? extends ConversionService> conversionService;

	/**
	 * Creates a new {@link IdentifierToPrimitivesConverter} for the given {@link ConversionService}.
	 *
	 * @param conversionService must not be {@literal null}.
	 */
	public IdentifierToPrimitivesConverter(Supplier<? extends ConversionService> conversionService) {

		Assert.notNull(conversionService, "ConversionService must not be null!");

		this.conversionService = conversionService;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.GenericConverter#getConvertibleTypes()
	 */
	@NonNull
	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Set.of(new ConvertiblePair(Identifier.class, Object.class));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.ConditionalConverter#matches(org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {

		if (sourceType.getType().equals(Identifier.class) && targetType.getType().equals(String.class)) {
			return true;
		}

		return CACHE.computeIfAbsent(sourceType.getType(), it -> detectAndCacheIdentifierExtractor(it))
				.filter(it -> isAssignableOrConvertable(it.type(), targetType.getType()))
				.isPresent();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.GenericConverter#convert(java.lang.Object, org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	@Nullable
	@Override
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

		if (source == null || sourceType.equals(targetType)) {
			return source;
		}

		var type = source.getClass();

		var extractor = CACHE.computeIfAbsent(type, it -> detectAndCacheIdentifierExtractor(type))
				.orElseThrow(() -> new IllegalStateException("Unable to find identifier field on " + type + "!"));

		var extracted = extractor.function().apply(source);

		if (extracted == null) {
			throw new IllegalStateException(String.format("No identifier found on instance %s!", source.toString()));
		}

		if (targetType.getType().isInstance(extracted)) {
			return extracted;
		}

		return conversionService.get().convert(extracted, TypeDescriptor.forObject(extracted), targetType);
	}

	private boolean isAssignableOrConvertable(Class<?> source, Class<?> target) {

		if (source.isAssignableFrom(target)) {
			return true;
		}

		if (String.class.equals(source)) {
			return String.class.equals(target);
		}

		return conversionService.get().canConvert(source, target);
	}

	private static Optional<ConvertibleExtractor> detectAndCacheIdentifierExtractor(Class<?> source) {
		return CACHE.computeIfAbsent(source, type -> detectExtractor(source));
	}

	private static Optional<ConvertibleExtractor> detectExtractor(Class<?> source) {

		if (source.isInterface() || source.equals(Object.class)) {
			return Optional.empty();
		}

		var result = Arrays.stream(source.getDeclaredFields())
				.filter(it -> !Modifier.isStatic(it.getModifiers()))
				.toList();

		if (result.isEmpty()) {
			return detectExtractor(source.getSuperclass());
		}

		if (result.size() > 1) {
			return Optional.of(new ConvertibleExtractor(Object::toString, String.class));
		}

		var field = result.get(0);
		ReflectionUtils.makeAccessible(field);

		return Optional.of(new ConvertibleExtractor(it -> ReflectionUtils.getField(field, it), field.getType()));
	}

	private record ConvertibleExtractor(Function<Object, Object> function, Class<?> type) {}
}
