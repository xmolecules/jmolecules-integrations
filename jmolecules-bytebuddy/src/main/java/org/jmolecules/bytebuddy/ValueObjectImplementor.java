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

import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.EqualsMethod;
import net.bytebuddy.implementation.HashCodeMethod;

import org.jmolecules.bytebuddy.PluginLogger.Log;

/**
 * Adds {@link Object#equals(Object)} and {@link Object#hashCode()} methods to the given value object type based on all
 * declared fields.
 *
 * @author Oliver Drotbohm
 */
class ValueObjectImplementor {

	JMoleculesTypeBuilder implementValueObject(JMoleculesTypeBuilder type, Log log) {

		return type
				.mapBuilder(it -> !it.hasMethod(isEquals()), it -> generateEquals(it, log))
				.mapBuilder(it -> !it.hasMethod(isHashCode()), it -> generateHashCode(it, log));
	}

	private static Builder<?> generateEquals(Builder<?> builder, Log log) {

		log.info("Implementing equals(…) based on value object fields.");

		return PluginUtils.markGenerated(builder.defineMethod("equals", boolean.class, Visibility.PUBLIC)
				.withParameter(Object.class)
				.intercept(EqualsMethod.isolated()));
	}

	private static Builder<?> generateHashCode(Builder<?> builder, Log log) {

		log.info("Implementing hashCode() based on value object fields.");

		return builder.defineMethod("hashCode", int.class, Visibility.PUBLIC)
				.intercept(HashCodeMethod.usingDefaultOffset());
	}
}
