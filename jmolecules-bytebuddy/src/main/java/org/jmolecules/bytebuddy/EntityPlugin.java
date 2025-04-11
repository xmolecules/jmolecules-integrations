/*
 * Copyright 2025 the original author or authors.
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

import static net.bytebuddy.matcher.ElementMatchers.*;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;

import java.util.stream.Stream;

import org.jmolecules.bytebuddy.PluginLogger.Log;

/**
 * @author Oliver Drotbohm
 */
class EntityPlugin implements LoggingPlugin {

	private static final EntityImplementor ENTITY_IMPLEMENTOR = new EntityImplementor();

	private final String moduleName;

	/**
	 * @param moduleName
	 */
	public EntityPlugin(String moduleName) {
		this.moduleName = moduleName;
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.matcher.ElementMatcher#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(TypeDescription target) {

		if (target.isAnnotation() || PluginUtils.isCglibProxyType(target)) {
			return false;
		}

		boolean implementsJMoleculesInterface = !target.getInterfaces().filter(nameStartsWith("org.jmolecules")).isEmpty();

		boolean hasJMoleculesAnnotation = Stream
				.concat(target.getDeclaredAnnotations().stream(), target.getInheritedAnnotations().stream())
				.anyMatch(it -> it.getAnnotationType().getName().startsWith("org.jmolecules"));

		if (implementsJMoleculesInterface || hasJMoleculesAnnotation) {
			return true;
		}

		Generic superType = target.getSuperClass();

		return superType == null || superType.represents(Object.class) ? false
				: matches(superType.asErasure()) || target.isRecord();
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.build.Plugin#apply(net.bytebuddy.dynamic.DynamicType.Builder, net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public Builder<?> apply(Builder<?> builder, TypeDescription type, ClassFileLocator classFileLocator) {

		Log log = PluginLogger.INSTANCE.getLog(type, moduleName);

		return JMoleculesTypeBuilder.of(log, builder)
				.map(JMoleculesTypeBuilder::isEntity, this::handleEntity)
				.conclude();
	}

	private JMoleculesTypeBuilder handleEntity(JMoleculesTypeBuilder type) {
		return type.map(ENTITY_IMPLEMENTOR::implementEntity);
	}
}
