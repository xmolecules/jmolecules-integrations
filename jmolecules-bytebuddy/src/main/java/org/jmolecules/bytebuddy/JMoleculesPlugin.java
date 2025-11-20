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

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.build.Plugin.WithPreprocessor;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Oliver Drotbohm
 * @author Simon Zambrovski
 */
@Slf4j
public class JMoleculesPlugin implements LoggingPlugin, WithPreprocessor {

	private final Map<ClassFileLocator, List<LoggingPlugin>> globalPlugins = new HashMap<>();
	private final Map<TypeDescription, List<? extends LoggingPlugin>> delegates = new HashMap<>();
	private final JMoleculesConfiguration configuration;

	public JMoleculesPlugin(File outputFolder) {
		this.configuration = new JMoleculesConfiguration(outputFolder);
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.build.Plugin.WithPreprocessor#onPreprocess(net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public void onPreprocess(TypeDescription typeDescription,
			ClassFileLocator classFileLocator) {

		if (!configuration.include(typeDescription)) {
			return;
		}

		if (PluginUtils.isCglibProxyType(typeDescription)) {
			return;
		}

		List<LoggingPlugin> plugins = globalPlugins.computeIfAbsent(classFileLocator, locator -> {

			ClassWorld world = ClassWorld.of(locator);
			Optional<Jpa> jpa = Jpa.getJavaPersistence(world);

			return Stream.of(

					axonPlugin(world), //
					axonSpringPlugin(world), //
					jpaPlugin(world, jpa), //
					springPlugin(world), //
					springJpaPlugin(world, jpa), //
					springDataPlugin(world), //
					springDataJdbcPlugin(world), //
					springDataJpaPlugin(world, jpa), //
					springDataMongoDbPlugin(world), //
					springAotPlugin(world))
					.flatMap(it -> it)
					.collect(Collectors.toList());
		});

		delegates.computeIfAbsent(typeDescription, it -> {

			return plugins.stream()
					.filter(plugin -> plugin.matches(it))
					.peek(plugin -> {
						if (plugin instanceof WithPreprocessor) {
							((WithPreprocessor) plugin).onPreprocess(it, classFileLocator);
						}
					})
					.collect(Collectors.toList());
		});
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.matcher.ElementMatcher#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(TypeDescription target) {

		if (PluginUtils.isCglibProxyType(target)) {
			return false;
		}

		List<? extends Plugin> plugins = delegates.get(target);

		return plugins != null && !plugins.isEmpty();
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.build.Plugin#apply(net.bytebuddy.dynamic.DynamicType.Builder, net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Builder<?> apply(Builder<?> builder,
			TypeDescription typeDescription, ClassFileLocator classFileLocator) {

		return delegates.get(typeDescription).stream()
				.reduce(builder, (it, plugin) -> (Builder) plugin.apply(it, typeDescription, classFileLocator),
						(left, right) -> right);
	}

	private Stream<? extends LoggingPlugin> jpaPlugin(ClassWorld world, Optional<Jpa> jpa) {

		return jpa.filter(__ -> {

			if (!configuration.supportsPersistence("jpa")) {
				return false;
			}

			boolean jMoleculesJpaAvailable = world.isAvailable("org.jmolecules.jpa.JMoleculesJpa");

			if (!jMoleculesJpaAvailable) {
				log.warn("JMoleculesJpa missing on the classpath! Add org.jmolecules:jmolecules-jpa as dependency!");
			}

			return true;

		}).map(it -> new JMoleculesJpaPlugin(it, world)) //
				.map(Stream::of) //
				.orElseGet(Stream::empty);
	}

	private static Stream<LoggingPlugin> springPlugin(ClassWorld world) {

		return world.isAvailable("org.springframework.stereotype.Component") //
				? Stream.of(new JMoleculesSpringPlugin()) //
				: Stream.empty();
	}

	private static Stream<LoggingPlugin> springDataPlugin(ClassWorld world) {

		return world.isAvailable("org.springframework.data.repository.Repository") //
				? Stream.of(new JMoleculesSpringDataPlugin()) //
				: Stream.empty();
	}

	private Stream<? extends LoggingPlugin> springJpaPlugin(ClassWorld world, Optional<Jpa> jpa) {

		return jpa.filter(__ -> configuration.supportsPersistence("jpa"))
				.filter(__ -> world.isAvailable("org.springframework.stereotype.Component")) //
				.filter(__ -> world.isAvailable("org.jmolecules.spring.jpa.AssociationAttributeConverter")) //
				.map(it -> new JMoleculesSpringJpaPlugin(it)) //
				.map(Stream::of) //
				.orElseGet(Stream::empty);
	}

	private Stream<LoggingPlugin> springDataJdbcPlugin(ClassWorld world) {

		if (!configuration.supportsPersistence("jdbc")) {
			return Stream.empty();
		}

		return world.isAvailable("org.springframework.data.jdbc.core.mapping.AggregateReference") //
				? Stream.of(new JMoleculesSpringDataJdbcPlugin(), new EntityPlugin("JDBC")) //
				: Stream.empty();
	}

	private Stream<? extends LoggingPlugin> springDataJpaPlugin(ClassWorld world, Optional<Jpa> jpa) {

		return jpa.filter(__ -> configuration.supportsPersistence("jpa"))
				.filter(__ -> world.isAvailable("org.springframework.data.jpa.repository.JpaRepository")) //
				.map(JMoleculesSpringDataJpaPlugin::new) //
				.map(Stream::of) //
				.orElseGet(Stream::empty);
	}

	private Stream<LoggingPlugin> springDataMongoDbPlugin(ClassWorld world) {

		if (!configuration.supportsPersistence("mongodb")) {
			return Stream.empty();
		}

		return world.isAvailable("org.springframework.data.mongodb.core.mapping.Document") //
				? Stream.of(new JMoleculesSpringDataMongoDbPlugin(), new EntityPlugin("MongoDB")) //
				: Stream.empty();
	}

	private static Stream<LoggingPlugin> axonPlugin(ClassWorld world) {

		return world.isAvailable("org.axonframework.commandhandling.CommandHandler") //
				? Stream.of(new JMoleculesAxonPlugin()) //
				: Stream.empty();
	}

	private static Stream<LoggingPlugin> axonSpringPlugin(ClassWorld world) {

		return world.isAvailable("org.axonframework.spring.stereotype.Aggregate") //
				? Stream.of(new JMoleculesAxonSpringPlugin()) //
				: Stream.empty();
	}

	private static Stream<LoggingPlugin> springAotPlugin(ClassWorld world) {

		return world.isAvailable("org.jmolecules.ddd.annotation.Entity")
				&& world.isAvailable("org.springframework.boot.autoconfigure.SpringBootApplication")
						? Stream.of(new JMoleculesSpringAotPlugin())
						: Stream.empty();
	}

	@Slf4j
	static class JMoleculesConfiguration {

		private final Properties properties;

		public JMoleculesConfiguration(File outputFolder) {
			this(loadProperties(detectConfiguration(detectProjectRoot(outputFolder)), outputFolder));
		}

		public JMoleculesConfiguration(Properties properties) {

			this.properties = properties;

			String toInclude = getPackagesToInclude();

			if (toInclude != null) {
				log.info("Applying code generation to types located in package(s): {}.", toInclude);
			}
		}

		String getProperty(String key) {
			return properties.getProperty(key);
		}

		private static Properties loadProperties(File configuration, File outputFolder) {

			Properties result = new Properties();

			if (configuration == null) {

				logNoConfigFound(outputFolder);
				return result;
			}

			try {

				result.load(new FileInputStream(configuration));

				return result;

			} catch (FileNotFoundException o_O) {

				logNoConfigFound(outputFolder);
				return result;

			} catch (IOException o_O) {
				throw new UncheckedIOException(o_O);
			}
		}

		public boolean include(TypeDescription description) {

			String value = properties.getProperty("bytebuddy.include", "");

			if (value.trim().isEmpty()) {
				return true;
			}

			String[] parts = value.split("\\,");

			return Stream.of(parts)
					.map(String::trim)
					.map(it -> it.concat("."))
					.anyMatch(description.getName()::startsWith);
		}

		public boolean supportsPersistence(String persistence) {

			String value = properties.getProperty("bytebuddy.persistence");
			String trimmed = value == null ? null : value.trim();

			if (trimmed == null || trimmed.isEmpty()) {
				return true;
			}

			if (trimmed.equals("none")) {
				return false;
			}

			return Stream.of(trimmed.split("\\,")).map(String::trim).anyMatch(persistence::equals);
		}

		private String getPackagesToInclude() {
			return properties.getProperty("bytebuddy.include");
		}

		private static void logNoConfigFound(File outputFolder) {

			log.info("No jmolecules.config found traversing {}", outputFolder.getAbsolutePath());
			return;
		}

		private static Path detectProjectRoot(File file) {

			String path = file.getAbsolutePath();

			return Stream.of("target/classes", "build/classes")
					.filter(path::contains)
					.map(it -> new File(path.substring(0, path.indexOf(it) - 1)))
					.map(File::toPath)
					.findFirst()
					.orElseGet(file::toPath);
		}

		private static File detectConfiguration(Path folder) {

			if (!hasBuildFile(folder)) {
				return null;
			}

			File candidate = folder.resolve("jmolecules.config").toFile();

			if (candidate.exists()) {

				log.info("Found jmolecules.configuration at {}", candidate.getAbsolutePath());
				return candidate;
			}

			return detectConfiguration(folder.getParent());
		}
	}

	private static boolean hasBuildFile(Path folder) {

		return Stream.of("pom.xml", "build.gradle", "build.gradle.kts")
				.map(folder::resolve)
				.anyMatch(it -> it.toFile().exists());
	}
}
