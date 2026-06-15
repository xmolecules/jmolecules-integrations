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

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;

import org.jmolecules.bytebuddy.PluginLogger.Log;

/**
 * {@link LoggingPlugin} implementing DDD-specific defaults. Generates {@link Object#equals(Object)} and
 * {@link Object#hashCode()} for types implementing {@link org.jmolecules.ddd.types.ValueObject} or annotated with
 * {@link org.jmolecules.ddd.annotation.ValueObject}, based on all declared fields.
 *
 * @author Oliver Drotbohm
 */
class DddPlugin implements LoggingPlugin {

	private static final ValueObjectImplementor IMPLEMENTOR = new ValueObjectImplementor();

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.matcher.ElementMatcher#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(TypeDescription target) {

		if (target.isAnnotation() || PluginUtils.isCglibProxyType(target)) {
			return false;
		}

		return new JMoleculesType(target).isValueObject();
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.build.Plugin#apply(net.bytebuddy.dynamic.DynamicType.Builder, net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public Builder<?> apply(Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {

		Log log = PluginLogger.INSTANCE.getLog(typeDescription, "DDD");

		return JMoleculesTypeBuilder.of(log, builder)
				.map(JMoleculesTypeBuilder::isValueObject, this::handleValueObject)
				.conclude();
	}

	private JMoleculesTypeBuilder handleValueObject(JMoleculesTypeBuilder type) {
		return type.map(IMPLEMENTOR::implementValueObject);
	}
}
