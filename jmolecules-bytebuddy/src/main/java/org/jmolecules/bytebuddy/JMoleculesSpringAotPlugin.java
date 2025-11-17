/*
 * Copyright 2020-2025 the original author or authors.
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

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.jmolecules.bytebuddy.PluginLogger.Log;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.annotation.RegisterReflection;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ReflectiveScan;

public class JMoleculesSpringAotPlugin implements LoggingPlugin {

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.matcher.ElementMatcher#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(TypeDescription type) {

		JMoleculesType jMoleculesType = new JMoleculesType(type);

		return jMoleculesType.isValueObject()
				|| jMoleculesType.isEntity()
				|| jMoleculesType.hasAnnotation(SpringBootApplication.class);
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.build.Plugin#apply(net.bytebuddy.dynamic.DynamicType.Builder, net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public Builder<?> apply(Builder<?> builder, TypeDescription type, ClassFileLocator classFileLocator) {

		Log log = PluginLogger.INSTANCE.getLog(type, "Spring AOT");

		if (isAnnotatedWith(type, SpringBootApplication.class)) {

			Map<Class<?>, Class<? extends Annotation>> mappings = new HashMap<>();
			mappings.put(SpringBootApplication.class, ReflectiveScan.class);

			builder = mapAnnotationOrInterfaces(builder, type, mappings, log);
		}

		return JMoleculesTypeBuilder.of(log, builder)
				.map(JMoleculesType::isEntity, this::addReflectiveAnnotation)
				.map(JMoleculesType::isValueObject, this::addReflectiveAnnotation)
				.conclude();
	}

	private JMoleculesTypeBuilder addReflectiveAnnotation(JMoleculesTypeBuilder builder) {

		AnnotationDescription annotation = getAnnotation(RegisterReflection.class, it -> it
				.defineEnumerationArray("memberCategories", MemberCategory.class,
						MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));

		return builder.annotateTypeIfMissing(annotation);
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {}
}
