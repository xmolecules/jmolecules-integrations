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
package org.jmolecules.bytebuddy;

import static org.jmolecules.bytebuddy.PluginUtils.*;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeDescription.ForLoadedType;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jmolecules.bytebuddy.PluginLogger.Log;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.repository.RepositoryDefinition;

/**
 * Plugin to annotate Spring Data repository with JMolecules' {@link Repository} annotation and implement Spring Data's
 * {@link org.springframework.data.repository.Repository} interface for repositories declared as JMolecules
 * {@link org.jmolecules.ddd.types.Repository}.
 *
 * @author Oliver Drotbohm
 */
public class JMoleculesSpringDataPlugin implements LoggingPlugin {

	private static final Class<?> SPRING_DATA_REPOSITORY = org.springframework.data.repository.Repository.class;
	private static final Class<?> JMOLECULES_REPOSITORY = org.jmolecules.ddd.types.Repository.class;

	private static final Map<Class<?>, Class<? extends Annotation>> TYPES;

	static {

		Map<Class<?>, Class<? extends Annotation>> types = new HashMap<>();

		types.put(RepositoryDefinition.class, Repository.class);
		types.put(org.springframework.data.repository.Repository.class, Repository.class);

		TYPES = Collections.unmodifiableMap(types);
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.matcher.ElementMatcher#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(TypeDescription target) {

		return target.isAssignableTo(JMOLECULES_REPOSITORY)
				|| TYPES.keySet().stream().anyMatch(it -> it.isAnnotation()
						? PluginUtils.isAnnotatedWith(target, it)
						: target.isAssignableTo(it));
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.build.Plugin#apply(net.bytebuddy.dynamic.DynamicType.Builder, net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public Builder<?> apply(Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {

		Log log = PluginLogger.INSTANCE.getLog(typeDescription, "Spring Data");

		Builder<?> result = builder;

		if (!typeDescription.isAssignableTo(SPRING_DATA_REPOSITORY)) {
			result = translateRepositoryInterfaces(result, typeDescription, typeDescription.asGenericType(), log);
		}

		return mapAnnotationOrInterfaces(result, typeDescription, TYPES, log);
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {}

	private static Builder<?> translateRepositoryInterfaces(Builder<?> builder, TypeDescription original, Generic type,
			Log log) {

		if (type.asErasure().represents(JMOLECULES_REPOSITORY)) {

			try {

				Generic aggregateType = type.getTypeArguments().get(0);
				Generic idType = type.getTypeArguments().get(1);

				ForLoadedType loadedType = new TypeDescription.ForLoadedType(SPRING_DATA_REPOSITORY);
				Generic repositoryType = Generic.Builder.parameterizedType(loadedType, aggregateType, idType).build();

				log.info("Implement {}<{}, {}>.",
						PluginUtils.abbreviate(repositoryType.asErasure()), PluginUtils.abbreviate(aggregateType.asErasure()),
						PluginUtils.abbreviate(idType.asErasure()));

				return builder.implement(repositoryType);

			} catch (Exception o_O) {

				log.info("No generics declared. Cannot translate into Spring Data repository!");

				return builder;
			}
		}

		if (type.isInterface()) {
			for (Generic parent : type.getInterfaces()) {
				builder = translateRepositoryInterfaces(builder, original, parent, log);
			}
		}

		return builder;
	}
}
