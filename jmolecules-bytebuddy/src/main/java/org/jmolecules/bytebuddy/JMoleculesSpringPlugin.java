/*
 * Copyright 2020-2022 the original author or authors.
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

import static org.jmolecules.bytebuddy.PluginUtils.*;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
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

import org.jmolecules.bytebuddy.PluginLogger.Log;
import org.jmolecules.ddd.annotation.Repository;
import org.jmolecules.ddd.annotation.Service;
import org.jmolecules.event.annotation.DomainEventHandler;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

public class JMoleculesSpringPlugin extends JMoleculesPluginSupport {

	private static final Map<Class<?>, Class<? extends Annotation>> MAPPINGS;
	private static final Set<Class<?>> TRIGGERS;
	private static final Map<Class<? extends Annotation>, Class<? extends Annotation>> METHOD_ANNOTATIONS;
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

		MAPPINGS = Collections.unmodifiableMap(types);

		/**
		 * Which annotations trigger the processing?
		 */
		Set<Class<?>> triggers = new HashSet<>(MAPPINGS.keySet());
		triggers.add(Component.class);

		TRIGGERS = Collections.unmodifiableSet(triggers);

		Set<String> toSkip = new HashSet<>();
		toSkip.add("java.");
		toSkip.add("javax.");

		TYPES_TO_SKIP = Collections.unmodifiableSet(toSkip);

		Map<Class<? extends Annotation>, Class<? extends Annotation>> methods = new HashMap<>();
		methods.put(DomainEventHandler.class, EventListener.class);

		methods.put(EventListener.class, DomainEventHandler.class);

		METHOD_ANNOTATIONS = Collections.unmodifiableMap(methods);
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

		return TRIGGERS.stream().anyMatch(it -> it.isAnnotation()
				? isAnnotatedWith(type, it)
				: type.isAssignableTo(it));
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.build.Plugin#apply(net.bytebuddy.dynamic.DynamicType.Builder, net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public Builder<?> apply(Builder<?> builder, TypeDescription type, ClassFileLocator classFileLocator) {

		Log log = PluginLogger.INSTANCE.getLog(type, "Spring");
		Builder<?> result = mapAnnotationOrInterfaces(builder, type, MAPPINGS, log);

		for (Entry<Class<? extends Annotation>, Class<? extends Annotation>> entry : METHOD_ANNOTATIONS.entrySet()) {

			Class<? extends Annotation> target = entry.getValue();

			result = result
					.method(hasAnnotatedMethod(type, entry.getKey(), target, log))
					.intercept(SuperMethodCall.INSTANCE)
					.annotateMethod(getAnnotation(target));
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {}

	private static ElementMatcher<? super MethodDescription> hasAnnotatedMethod(TypeDescription type,
			Class<? extends Annotation> source, Class<? extends Annotation> target, Log log) {

		return method -> {

			if (!method.getDeclaringType().equals(type)) {
				return false;
			}

			if (TYPES_TO_SKIP.stream().anyMatch(it -> method.getDeclaringType().getTypeName().startsWith(it))) {
				return false;
			}

			AnnotationList annotations = method.getDeclaredAnnotations();

			if (annotations.isAnnotationPresent(target)) {
				// log.info("Already annotated with @{}.", PluginUtils.abbreviate(target));
				return false;
			}

			if (!annotations.isAnnotationPresent(source)) {
				// log.info("Annotation {} not found.", PluginUtils.abbreviate(source));
				return false;
			}

			log.info("Adding @{}.", PluginUtils.abbreviate(target));

			return true;
		};
	}
}
