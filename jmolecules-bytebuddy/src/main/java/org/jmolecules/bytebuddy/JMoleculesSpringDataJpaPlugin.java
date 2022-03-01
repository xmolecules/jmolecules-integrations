/*
 * Copyright 2021-2022 the original author or authors.
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

import lombok.NoArgsConstructor;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;

import org.jmolecules.bytebuddy.PluginLogger.Log;
import org.jmolecules.ddd.types.AggregateRoot;
import org.springframework.data.domain.Persistable;

/**
 * Plugin to implement {@link Persistable} for all {@link AggregateRoot}s.
 *
 * @author Oliver Drotbohm
 */
@NoArgsConstructor
public class JMoleculesSpringDataJpaPlugin extends JMoleculesPluginSupport {

	private PersistableOptions options;

	public JMoleculesSpringDataJpaPlugin(Jpa jpa) {
		this.options = getOptions(jpa);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.bytebuddy.JMoleculesPluginSupport#onPreprocess(net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public void onPreprocess(TypeDescription typeDescription, ClassFileLocator classFileLocator) {

		Jpa jpa = Jpa.getJavaPersistence(ClassWorld.of(classFileLocator)).get();

		this.options = getOptions(jpa);
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.matcher.ElementMatcher#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(TypeDescription target) {
		return target.isAssignableTo(AggregateRoot.class);
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.build.Plugin#apply(net.bytebuddy.dynamic.DynamicType.Builder, net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public Builder<?> apply(Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {

		Log log = PluginLogger.INSTANCE.getLog(typeDescription, "Spring Data JPA");

		return JMoleculesType.of(log, builder)
				.implementPersistable(options)
				.conclude();
	}

	private static PersistableOptions getOptions(Jpa jpa) {

		return PersistableOptions.of(jpa.getAnnotation("Transient"))
				.withCallbackAnnotations(jpa.getAnnotation("PrePersist"), jpa.getAnnotation("PostLoad"));
	}
}
