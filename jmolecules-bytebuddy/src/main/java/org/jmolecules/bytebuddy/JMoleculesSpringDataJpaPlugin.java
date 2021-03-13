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

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;

import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Transient;

import org.jmolecules.ddd.types.AggregateRoot;
import org.springframework.data.domain.Persistable;

/**
 * Plugin to implement {@link Persistable} for all {@link AggregateRoot}s.
 *
 * @author Oliver Drotbohm
 */
public class JMoleculesSpringDataJpaPlugin extends JMoleculesPluginSupport {

	private static final PluginLogger logger = new PluginLogger("Spring Data JPA");
	private final PersistableOptions options;

	public JMoleculesSpringDataJpaPlugin() {

		this.options = PersistableOptions.of(Transient.class)
				.withCallbackAnnotations(PrePersist.class, PostLoad.class);
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

		return JMoleculesType.of(logger, builder)
				.implementPersistable(options)
				.conclude();
	}
}
