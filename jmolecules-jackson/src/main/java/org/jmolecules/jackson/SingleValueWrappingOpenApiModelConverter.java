/*
 * Copyright 2026 the original author or authors.
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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;

/**
 * {@link ModelConverter} that resolves single-value jMolecules {@link ValueObject}s and {@link Identifier}s to their
 * inner type's schema, so that OpenAPI documentation reflects the flat serialization produced by
 * {@link SingleValueWrappingTypeSerializerModifier}.
 *
 * @author Hasan Kara
 * @see SingleValueWrappingTypeSerializerModifier
 * @see <a href="https://github.com/xmolecules/jmolecules-integrations/issues/310">GH-310</a>
 */
public class SingleValueWrappingOpenApiModelConverter implements ModelConverter {

	private static final AnnotationDetector DETECTOR = AnnotationDetector.getAnnotationDetector();

	/*
	 * (non-Javadoc)
	 * @see io.swagger.v3.core.converter.ModelConverter#resolve(io.swagger.v3.core.converter.AnnotatedType, io.swagger.v3.core.converter.ModelConverterContext, java.util.Iterator)
	 */
	@Override
	public Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {

		Class<?> rawType = getRawType(type.getType());

		if (rawType != null) {

			Field singleField = getSingleValueField(rawType);

			if (singleField != null && isSupportedType(rawType)) {
				return context.resolve(new AnnotatedType(singleField.getGenericType()));
			}
		}

		if (chain.hasNext()) {
			return chain.next().resolve(type, context, chain);
		}

		return null;
	}

	private static boolean isSupportedType(Class<?> type) {

		return DETECTOR.hasAnnotation(type, ValueObject.class)
				|| org.jmolecules.ddd.types.ValueObject.class.isAssignableFrom(type)
				|| Identifier.class.isAssignableFrom(type);
	}

	private static Field getSingleValueField(Class<?> type) {

		List<Field> fields = Arrays.stream(type.getDeclaredFields())
				.filter(it -> !Modifier.isStatic(it.getModifiers()))
				.toList();

		return fields.size() == 1 ? fields.get(0) : null;
	}

	private static Class<?> getRawType(Type type) {

		if (type instanceof Class<?> clazz) {
			return clazz;
		}

		if (type instanceof ParameterizedType parameterized) {

			Type rawType = parameterized.getRawType();

			if (rawType instanceof Class<?> clazz) {
				return clazz;
			}
		}

		return null;
	}
}
