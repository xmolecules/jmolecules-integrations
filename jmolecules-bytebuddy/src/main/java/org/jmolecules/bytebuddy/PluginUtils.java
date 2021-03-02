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
package org.jmolecules.bytebuddy;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Utility methods to be used from different {@link Plugin} implementations
 *
 * @author Oliver Drotbohm
 */
@Slf4j
class PluginUtils {

	/**
	 * Returns whether the given {@link TypeDescription} is annotated with the given annotation.
	 *
	 * @param type must not be {@literal null}.
	 * @param annotationType must not be {@literal null}.
	 * @return
	 */
	static boolean isAnnotatedWith(TypeDescription type, Class<?> annotationType) {

		return type.getDeclaredAnnotations() //
				.asTypeList() //
				.stream() //
				.anyMatch(it -> it.isAssignableTo(annotationType));
	}

	/**
	 * Returns an {@link AnnotationDescription} for an empty (i.e. no attributes defined) annotation of the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	static AnnotationDescription getAnnotation(Class<? extends Annotation> type) {
		return AnnotationDescription.Builder.ofType(type).build();
	}

	/**
	 * Applies the given map of source type or annotation to annotation onto the given {@link Builder}.
	 *
	 * @param prefix the prefix to use for log output.
	 * @param builder the current {@link Builder}.
	 * @param type the currently described type.
	 * @param mappings the annotation or type mappings.
	 * @return
	 */
	static Builder<?> mapAnnotationOrInterfaces(String prefix, Builder<?> builder, TypeDescription type,
			Map<Class<?>, Class<? extends Annotation>> mappings) {

		for (Entry<Class<?>, Class<? extends Annotation>> entry : mappings.entrySet()) {

			Class<?> source = entry.getKey();

			if (source.isAnnotation() ? isAnnotatedWith(type, source) : type.isAssignableTo(source)) {
				builder = addAnnotationIfMissing(prefix, entry.getValue(), builder, type);
			}
		}

		return builder;
	}

	private static Builder<?> addAnnotationIfMissing(String prefix, Class<? extends Annotation> annotation,
			Builder<?> builder,
			TypeDescription type) {

		if (isAnnotatedWith(type, annotation)) {
			return builder;
		}

		log.info("{} - {} - Adding @{}.", prefix, type.getSimpleName(), annotation.getName());

		return builder.annotateType(getAnnotation(annotation));
	}

}
