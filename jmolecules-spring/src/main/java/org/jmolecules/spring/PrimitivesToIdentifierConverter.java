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

import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jmolecules.ddd.types.Identifier;
import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

/**
 * A Spring {@link Converter} to convert primitives like {@link String} and {@link UUID} values to jMolecules
 * {@link Identifier} instances, assuming they expose a static factory method {@code of(…)}.
 *
 * @author Oliver Drotbohm
 */
public class PrimitivesToIdentifierConverter implements ConditionalGenericConverter {

	private static final Map<Class<?>, Optional<Instantiator>> CREATORS = new ConcurrentReferenceHashMap<>();

	private static final Set<Class<?>> DEFAULT_PRIMITIVES = new HashSet<>(Arrays.asList(String.class, UUID.class, Long.class, long.class, Integer.class, int.class));
	private static final Set<String> DEFAULT_FACTORY_METHOD_NAMES = new HashSet<>(Arrays.asList("of"));

	private final Supplier<? extends ConversionService> conversionService;
	private Set<Class<?>> primitives;
	private Set<String> factoryMethodNames;

	private final BiFunction<Object, Executable, Object> preparer;

	/**
	 * Creates a new {@link PrimitivesToIdentifierConverter} for the given {@link ConversionService}.
	 *
	 * @param conversionService must not be {@literal null}.
	 */
	public PrimitivesToIdentifierConverter(Supplier<? extends ConversionService> conversionService) {

		Assert.notNull(conversionService, "ConversionService must not be null!");

		this.primitives = new HashSet<>(DEFAULT_PRIMITIVES);
		this.factoryMethodNames = new HashSet<>(DEFAULT_FACTORY_METHOD_NAMES);
		this.conversionService = conversionService;
		this.preparer = this::prepareSource;
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

		Class<?> sourceType = source.getType();
		Class<?> targetType = target.getType();

		return CREATORS.computeIfAbsent(targetType, it -> lookupInstantiator(sourceType, it)) //
				.filter(it -> conversionService.get().canConvert(sourceType, it.getIdSourceType()))
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

		Instantiator instantiator = CREATORS
				.computeIfAbsent(type, it -> lookupInstantiator(valueType, it))
				.orElseThrow(() -> new IllegalStateException(
						String.format("No factory method taking a parameter of type %s on %s!", valueType.getSimpleName(),
								type.getSimpleName())));

		return instantiator.creator.apply(source);
	}

	private Optional<Instantiator> lookupInstantiator(Class<?> source, Class<?> target) {

		Optional<Instantiator> creatorMethod = detectCreatorMethod(target, source);

		return creatorMethod.isPresent() ? creatorMethod : detectConstructor(target, source);
	}

	/**
	 * Detects a static factory method on the given type that can use the parameter of the given type as source to create
	 * instances of it (directly or via a preparing conversion).
	 *
	 * @param type must not be {@literal null}.
	 * @param parameterType must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	private Optional<Instantiator> detectCreatorMethod(Class<?> type, Class<?> parameterType) {

		return factoryMethodNames.stream()
				.flatMap(name -> primitives.stream().map(primitive -> new Signature(name, primitive)))
				.map(it -> ClassUtils.getStaticMethod(type, it.name, it.argumentType))
				.filter(it -> it != null)
				.peek(ReflectionUtils::makeAccessible)
				.filter(it -> isAssignableOrConvertable(it.getParameterTypes()[0], parameterType))
				.findFirst()
				.map(it -> new Instantiator(it,
						param -> ReflectionUtils.invokeMethod(it, parameterType, preparer.apply(param, it))));
	}

	/**
	 * Detects a constructor on the given type that can use the parameter of the given type as source to create instances
	 * of it (directly or via a preparing conversion).
	 *
	 * @param type must not be {@literal null}.
	 * @param parameterType must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	private Optional<Instantiator> detectConstructor(Class<?> type, Class<?> parameterType) {

		return Arrays.stream(type.getDeclaredConstructors())
				.filter(it -> it.getParameterCount() == 1)
				.filter(it -> primitives.contains(it.getParameterTypes()[0]))
				.filter(it -> isAssignableOrConvertable(parameterType, it.getParameterTypes()[0]))
				.peek(ReflectionUtils::makeAccessible)
				.findFirst()
				.map(it -> new Instantiator(it, param -> BeanUtils.instantiateClass(it, preparer.apply(param, it))));
	}

	private boolean isAssignableOrConvertable(Class<?> source, Class<?> target) {
		return source.isAssignableFrom(target) || conversionService.get().canConvert(source, target);
	}

	private Object prepareSource(Object value, Executable executable) {

		Class<?> sourceType = executable.getParameterTypes()[0];

		return sourceType.isInstance(value)
				? value
				: conversionService.get()
						.convert(value, TypeDescriptor.forObject(value), TypeDescriptor.valueOf(sourceType));
	}

	@Value
	private static class Signature {

		String name;
		Class<?> argumentType;
	}

	@Value
	private static class Instantiator {

		Class<?> idSourceType;
		Function<Object, Object> creator;

		public Instantiator(Executable executable, Function<Object, Object> creator) {

			this.idSourceType = executable.getParameterTypes()[0];
			this.creator = creator;
		}
	}
}
