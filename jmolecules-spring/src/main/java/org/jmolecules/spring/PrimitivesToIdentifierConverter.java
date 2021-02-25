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

import lombok.Value;

import java.lang.reflect.Method;
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
 * A Spring {@link Converter} to convert primitives like {@link String} and {@link UUID} values to jMolecules
 * {@link Identifier} instances, assuming they expose a static factory method {@code of(…)}.
 *
 * @author Oliver Drotbohm
 */
public class PrimitivesToIdentifierConverter implements ConditionalGenericConverter {

	private static final Map<Class<?>, Optional<Method>> CACHE = new ConcurrentReferenceHashMap<>();
	private static final Set<Class<?>> DEFAULT_PRIMITIVES = new HashSet<>(Arrays.asList(String.class, UUID.class));
	private static final Set<String> DEFAULT_FACTORY_METHOD_NAMES = new HashSet<>(Arrays.asList("of"));

	private final Supplier<ConversionService> conversionService;
	private Set<Class<?>> primitives;
	private Set<String> factoryMethodNames;

	public PrimitivesToIdentifierConverter(Supplier<ConversionService> conversionService) {

		this.primitives = new HashSet<>(DEFAULT_PRIMITIVES);
		this.factoryMethodNames = new HashSet<>(DEFAULT_FACTORY_METHOD_NAMES);
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
				.map(it -> new ConvertiblePair(it, Identifier.class))
				.collect(Collectors.toSet());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.ConditionalConverter#matches(org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	@Override
	public boolean matches(TypeDescriptor source, TypeDescriptor target) {

		return CACHE.computeIfAbsent(target.getType(), this::detectCreatorMethod)
				.filter(it -> isAssignableOrConverable(it.getParameterTypes()[0], source.getType()))
				.isPresent();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.GenericConverter#convert(java.lang.Object, org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
	 */
	@Nullable
	@Override
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor target) {

		if (source == null) {
			return null;
		}

		Class<?> type = target.getType();
		Class<?> valueType = source.getClass();

		Method method = CACHE.computeIfAbsent(type, this::detectCreatorMethod)
				.filter(it -> isAssignableOrConverable(it.getParameterTypes()[0], valueType))
				.orElseThrow(() -> new IllegalStateException(
						String.format("No factory method taking a parameter of type %s on %s!", valueType.getSimpleName(),
								type.getSimpleName())));

		Class<?> parameterType = method.getParameterTypes()[0];

		if (!parameterType.isInstance(source)) {
			source = conversionService.get().convert(source, TypeDescriptor.forObject(source),
					TypeDescriptor.valueOf(parameterType));
		}

		return ReflectionUtils.invokeMethod(method, null, source);
	}

	private Optional<Method> detectCreatorMethod(Class<?> type) {

		return factoryMethodNames.stream()
				.flatMap(name -> primitives.stream().map(primitive -> new Signature(name, primitive)))
				.map(it -> ReflectionUtils.findMethod(type, it.name, it.argumentType))
				.filter(it -> it != null)
				.peek(ReflectionUtils::makeAccessible)
				.findFirst();
	}

	private boolean isAssignableOrConverable(Class<?> source, Class<?> target) {

		return source.isAssignableFrom(target)
				|| conversionService.get().canConvert(source, target);
	}

	@Value
	private static class Signature {

		String name;
		Class<?> argumentType;
	}
}
