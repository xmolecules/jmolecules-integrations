/*
 * Copyright 2020-2021 the original author or authors.
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
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.matcher.ElementMatcher;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.jmolecules.ddd.annotation.Repository;
import org.jmolecules.ddd.annotation.Service;
import org.jmolecules.event.annotation.DomainEventHandler;
import org.springframework.context.event.EventListener;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.stereotype.Component;

@Slf4j
public class JMoleculesSpringPlugin implements Plugin {

	private static final Map<Class<?>, Class<? extends Annotation>> TYPES;
	private static final Map<Class<? extends Annotation>, Class<? extends Annotation>> METHOD_ANNOTATIONS;
	private static final Set<MethodDescription> SEEN_METHODS;
	private static final Set<String> TYPES_TO_SKIP;

	static {

		// jMolecules -> Spring
		Map<Class<?>, Class<? extends Annotation>> types = new HashMap<>();
		types.put(Service.class, org.springframework.stereotype.Service.class);
		types.put(Repository.class, org.springframework.stereotype.Repository.class);
		types.put(org.jmolecules.ddd.annotation.Factory.class, Component.class);

		// Spring -> jMolecules
		types.put(org.springframework.stereotype.Service.class, Service.class);
		types.put(org.springframework.stereotype.Repository.class, Repository.class);

		// Spring Data -> jMolecules
		types.put(RepositoryDefinition.class, Repository.class);
		types.put(org.springframework.data.repository.Repository.class, Repository.class);

		TYPES = Collections.unmodifiableMap(types);

		Set<String> toSkip = new HashSet<>();
		toSkip.add("java.");
		toSkip.add("javax.");

		TYPES_TO_SKIP = Collections.unmodifiableSet(toSkip);

		Map<Class<? extends Annotation>, Class<? extends Annotation>> methods = new HashMap<>();
		methods.put(DomainEventHandler.class, EventListener.class);

		methods.put(EventListener.class, DomainEventHandler.class);

		METHOD_ANNOTATIONS = Collections.unmodifiableMap(methods);

		SEEN_METHODS = new HashSet<>();
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.matcher.ElementMatcher#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(TypeDescription type) {

		if (TYPES_TO_SKIP.stream().anyMatch(it -> type.getPackage().getName().startsWith(it))) {
			return false;
		}

		return TYPES.keySet().stream().anyMatch(it -> it.isAnnotation()
				? isAnnotatedWith(type, it)
				: type.isAssignableTo(it));
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.build.Plugin#apply(net.bytebuddy.dynamic.DynamicType.Builder, net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public Builder<?> apply(Builder<?> builder, TypeDescription type, ClassFileLocator classFileLocator) {

		for (Entry<Class<?>, Class<? extends Annotation>> entry : TYPES.entrySet()) {

			Class<?> source = entry.getKey();

			boolean processingRequired = source.isAnnotation()
					? isAnnotatedWith(type, source)
					: type.isAssignableTo(source);

			if (processingRequired) {
				builder = addAnnotationIfMissing(entry.getValue(), builder, type);
			}
		}

		for (Entry<Class<? extends Annotation>, Class<? extends Annotation>> entry : METHOD_ANNOTATIONS.entrySet()) {

			Class<? extends Annotation> target = entry.getValue();

			builder = builder
					.method(hasAnnotatedMethod(type, entry.getKey(), target))
					.intercept(SuperMethodCall.INSTANCE)
					.annotateMethod(getAnnotation(target));
		}

		return builder;
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {}

	private static Builder<?> addAnnotationIfMissing(Class<? extends Annotation> annotation, Builder<?> builder,
			TypeDescription type) {

		if (isAnnotatedWith(type, annotation)) {
			return builder;
		}

		log.info("jMolecules Spring Plugin - Annotating {} with @{}.", type.getSimpleName(), annotation.getName());

		return builder.annotateType(getAnnotation(annotation));
	}

	private static AnnotationDescription getAnnotation(Class<? extends Annotation> type) {
		return AnnotationDescription.Builder.ofType(type).build();
	}

	private static boolean isAnnotatedWith(TypeDescription type, Class<?> annotationType) {

		return type.getDeclaredAnnotations() //
				.asTypeList() //
				.stream() //
				.anyMatch(it -> it.isAssignableTo(annotationType)); // || isAnnotatedWith(it, annotationType));
	}

	private static ElementMatcher<? super MethodDescription> hasAnnotatedMethod(TypeDescription type,
			Class<? extends Annotation> source, Class<? extends Annotation> target) {

		return method -> {

			if (!method.getDeclaringType().equals(type)) {
				return false;
			}

			if (TYPES_TO_SKIP.stream().anyMatch(it -> method.getDeclaringType().getTypeName().startsWith(it))) {
				return false;
			}

			AnnotationList annotations = method.getDeclaredAnnotations();
			String signature = toLog(method);

			if (annotations.isAnnotationPresent(target)) {
				log.debug("jMolecules Spring Plugin - Method {} already annotated with @{}.", signature, target.getName());
				return false;
			}

			if (!annotations.isAnnotationPresent(source)) {
				log.debug("jMolecules Spring Plugin - Annotation {} not found on method {}.", source.getName(), signature);
				return false;
			}

			log.info("jMolecules Spring Plugin - Annotating {} with {}.", signature, target.getName());

			return true;
		};
	}

	private static String toLog(MethodDescription method) {

		TypeDefinition type = method.getDeclaringType();
		String parameterTypes = method.getParameters()
				.asTypeList()
				.asErasures()
				.stream()
				.map(TypeDescription::getSimpleName)
				.collect(Collectors.joining(", "));

		return type.asErasure().getSimpleName()
				.concat(".")
				.concat(method.getName())
				.concat("(")
				.concat(parameterTypes)
				.concat(")");
	}
}
