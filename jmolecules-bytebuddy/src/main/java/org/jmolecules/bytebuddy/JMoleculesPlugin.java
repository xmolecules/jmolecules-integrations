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
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Oliver Drotbohm
 */
@Slf4j public class JMoleculesPlugin extends JMoleculesPluginSupport {

	private final Map<TypeDescription, List<? extends Plugin>> delegates = new HashMap<>();

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.build.Plugin.WithPreprocessor#onPreprocess(net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override @SuppressWarnings("unchecked") public void onPreprocess(TypeDescription typeDescription,
			ClassFileLocator classFileLocator) {

		delegates.computeIfAbsent(typeDescription, it -> {

			ClassWorld world = ClassWorld.of(classFileLocator);
			Optional<Jpa> jpa = Jpa.getJavaPersistence(world);

			return Stream.of(axonPlugin(world), axonSpringPlugin(world),
					jpaPlugin(world, jpa), springPlugin(world), springJpaPlugin(world, jpa), springDataPlugin(world),
					springDataJdbcPlugin(world), springDataJpaPlugin(world, jpa), springDataMongDbPlugin(world)).flatMap(
					plugins -> (Stream<Plugin>) plugins).filter(plugin -> plugin.matches(typeDescription)).collect(
					Collectors.toList());
		});
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.matcher.ElementMatcher#matches(java.lang.Object)
	 */
	@Override public boolean matches(TypeDescription target) {

		List<? extends Plugin> plugins = delegates.get(target);

		return (plugins != null) && !plugins.isEmpty();
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.build.Plugin#apply(net.bytebuddy.dynamic.DynamicType.Builder, net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override @SuppressWarnings({ "unchecked", "rawtypes" }) public Builder<?> apply(Builder<?> builder,
			TypeDescription typeDescription, ClassFileLocator classFileLocator) {

		return delegates.get(typeDescription).stream().reduce(builder,
				(it, plugin) -> (Builder) plugin.apply(it, typeDescription, classFileLocator), (left, right) -> right);
	}

	private static Stream<? extends Plugin> jpaPlugin(ClassWorld world, Optional<Jpa> jpa) {

		return jpa.filter(__ -> {

			boolean jMoleculesJpaAvailable = world.isAvailable("org.jmolecules.jpa.JMoleculesJpa");

			if (!jMoleculesJpaAvailable) {
				log.warn("JMoleculesJpa missing on the classpath! Add org.jmolecules:jmolecules-jpa as dependency!");
			}

			return true;

		}).map(JMoleculesJpaPlugin::new).map(Stream::of).orElseGet(Stream::empty);
	}

	private static Stream<Plugin> springPlugin(ClassWorld world) {

		return world.isAvailable("org.springframework.stereotype.Component") ?
				Stream.of(new JMoleculesSpringPlugin()) :
				Stream.empty();
	}

	private static Stream<Plugin> springDataPlugin(ClassWorld world) {

		return world.isAvailable("org.springframework.data.repository.Repository") ? Stream.of(
				new JMoleculesSpringDataPlugin()) : Stream.empty();
	}

	private static Stream<? extends Plugin> springJpaPlugin(ClassWorld world, Optional<Jpa> jpa) {

		return jpa.filter(__ -> world.isAvailable("org.springframework.stereotype.Component")).filter(
				__ -> world.isAvailable("org.jmolecules.spring.jpa.AssociationAttributeConverter")).map(
				it -> new JMoleculesSpringJpaPlugin(it, world)).map(Stream::of).orElseGet(Stream::empty);
	}

	private static Stream<Plugin> springDataJdbcPlugin(ClassWorld world) {

		return world.isAvailable("org.springframework.data.jdbc.core.mapping.AggregateReference") ? Stream.of(
				new JMoleculesSpringDataJdbcPlugin()) : Stream.empty();
	}

	private static Stream<? extends Plugin> springDataJpaPlugin(ClassWorld world, Optional<Jpa> jpa) {

		return jpa.filter(__ -> world.isAvailable("org.springframework.data.jpa.repository.JpaRepository")).map(
				JMoleculesSpringDataJpaPlugin::new).map(Stream::of).orElseGet(Stream::empty);
	}

	private static Stream<Plugin> springDataMongDbPlugin(ClassWorld world) {

		return world.isAvailable("org.springframework.data.mongodb.core.mapping.Document") ? Stream.of(
				new JMoleculesSpringDataMongoDbPlugin()) : Stream.empty();
	}

	private static Stream<Plugin> axonPlugin(ClassWorld world) {
		if (!world.isAvailable("org.axonframework.commandhandling.CommandHandler")) {
			return Stream.empty();
		}

		return Stream.of(new JMoleculesAxonPlugin());
	}

	private static Stream<Plugin> axonSpringPlugin(ClassWorld world) {
		if (!world.isAvailable("org.axonframework.spring.stereotype.Aggregate")) {
			return Stream.empty();
		}

		return Stream.of(new JMoleculesAxonSpringPlugin());
	}
}
