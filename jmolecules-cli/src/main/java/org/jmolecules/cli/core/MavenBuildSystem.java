/*
 * Copyright 2024 the original author or authors.
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
package org.jmolecules.cli.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.jmolecules.codegen.Dependency;
import org.jmolecules.codegen.ProjectFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.function.SingletonSupplier;

/**
 * @author Oliver Drotbohm
 */
class MavenBuildSystem implements BuildSystem {

	private static final Logger LOG = LoggerFactory.getLogger(MavenBuildSystem.class);

	private final ProjectFiles fileSystem;
	private final MetadataCache cache;
	private final Path mavenExecutable;
	private final Path dependencies, classpath;

	private final Supplier<List<Dependency>> calculatedDependencies;
	private final Supplier<URL[]> calculatedClasspath;

	private final Supplier<Void> dependenciesLookup, classpathLookup;

	private final PomUpdater updater;

	/**
	 * @param publisher
	 */
	MavenBuildSystem(ProjectFiles fileSystem, MetadataCache cache) {

		this.fileSystem = fileSystem;
		this.mavenExecutable = detectMavenExecutable();

		this.cache = cache;

		this.dependencies = cache.resolveInternal("dependencies");
		this.classpath = cache.resolveInternal("classpath");

		this.dependenciesLookup = SingletonSupplier.of(() -> {

			if (!Files.exists(dependencies)) {
				calculateDependencies();
			}

			return null;
		});

		this.classpathLookup = SingletonSupplier.of(() -> {

			if (!Files.exists(classpath)) {
				calculateClasspath();
			}

			return null;
		});

		this.calculatedDependencies = SingletonSupplier.of(() -> {
			dependenciesLookup.get();
			return parseDependencies(dependencies);
		});

		this.calculatedClasspath = SingletonSupplier.of(() -> {

			classpathLookup.get();
			return parseClasspath(classpath);
		});

		this.updater = new PomUpdater(fileSystem);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.cli.build.BuildSystem#getDependencies()
	 */
	@Override
	public List<Dependency> getDependencies() {
		return calculatedDependencies.get();
	}

	@Override
	public void initialize() {
		cache.clearCaches();
	}

	@Override
	public void refresh() {

		calculateDependencies();
		calculateClasspath();
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.cli.build.BuildSystem#getClasspath()
	 */
	@Override
	public URL[] getClasspath() {
		return calculatedClasspath.get();
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.cli.build.BuildSystem#getRawClasspath()
	 */
	@Override
	public String getRawClasspath() {
		try {
			return Files.readString(classpath);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.cli.build.BuildSystem#getClasses()
	 */
	@Override
	public Path getClasses() {
		return fileSystem.resolve("target/classes");
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.cli.build.BuildSystem#getTestClasses()
	 */
	@Override
	public Path getTestClasses() {
		return fileSystem.resolve("target/testClasses");
	}

	private Void calculateDependencies() {

		LOG.info("> Updating Maven dependencies to {}", dependencies);

		try {

			Properties properties = new Properties();
			properties.setProperty("outputFile", dependencies.toFile().getAbsolutePath());

			// Set up the Maven invocation request
			InvocationRequest request = new DefaultInvocationRequest();
			request.setPomFile(new File("./pom.xml"));
			request.addArg("dependency:list");
			request.addArg("-Dsort");
			request.addArg("-Dscan=false");
			request.setInputStream(InputStream.nullInputStream());
			request.setOutputHandler(LOG::debug);
			request.setProperties(properties);

			// Set up the Maven invoker
			Invoker invoker = new DefaultInvoker();
			invoker.setMavenExecutable(mavenExecutable.toFile()); // Adjust this path to your Maven installation

			// Execute the request
			invoker.execute(request);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return null;
	}

	private void calculateClasspath() {

		LOG.info("> Calculating classpath ({})", classpath);

		try {

			Properties properties = new Properties();
			properties.setProperty("mdep.outputFile", classpath.toFile().getAbsolutePath());
			properties.setProperty("scan", "false");

			// Set up the Maven invocation request
			InvocationRequest request = new DefaultInvocationRequest();
			request.setPomFile(new File("./pom.xml"));
			request.addArg("dependency:build-classpath");
			request.setInputStream(InputStream.nullInputStream());
			request.setOutputHandler(LOG::debug);
			request.setProperties(properties);

			// Set up the Maven invoker
			Invoker invoker = new DefaultInvoker();
			invoker.setMavenExecutable(mavenExecutable.toFile());

			// Execute the request
			invoker.execute(request);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Path detectMavenExecutable() {

		String property = null; // configuration.getMavenExecutable();

		if (property != null) {
			return Path.of(property);
		}

		Path mvnw = findMavenWrapperIn(fileSystem.getProjectRoot());

		if (mvnw != null) {
			return mvnw;
		}

		return null;
	}

	private Path findMavenWrapperIn(Path path) {

		Path mvnw = path.resolve("mvnw");

		if (Files.exists(mvnw)) {
			return mvnw;
		}

		var parent = path.toAbsolutePath().getParent();

		return parent == null ? null : findMavenWrapperIn(parent);
	}

	private static List<Dependency> parseDependencies(Path file) {

		Pattern pattern = Pattern.compile("(.*)(:compile|:runtime|:test)");

		try (BufferedReader reader = Files.newBufferedReader(file)) {

			// Skip header
			while (!"The following files have been resolved:".equals(reader.readLine())) {
				;
			}

			String line;
			List<Dependency> dependencies = new ArrayList<>();

			while ((line = reader.readLine()) != null && !line.isEmpty()) {

				Matcher matcher = pattern.matcher(line);

				if (matcher.find()) {
					dependencies.add(MavenDependency.of(matcher.group(0)));
				}
			}

			return dependencies;

		} catch (Exception e) {

			throw new RuntimeException(e);
		}
	}

	private static URL[] parseClasspath(Path file) {

		try {
			var fromFile = Files.readAllLines(file).stream()
					.flatMap(it -> Arrays.stream(it.split(":")));

			var compiledSources = Stream.concat(Stream.of("target/classes"), Stream.of("target/test-classes"));

			return Stream.concat(compiledSources, fromFile)
					.map(MavenBuildSystem::toUrl)
					.toArray(URL[]::new);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static URL toUrl(Path path) {
		return toUrl(path.toUri());
	}

	private static URL toUrl(String entry) {
		return toUrl(new File(entry).toURI());
	}

	private static URL toUrl(URI uri) {

		try {
			return uri.toURL();
		} catch (MalformedURLException e) {
			throw new IllegalStateException(e);
		}
	}

	record MavenDependency(String groupId, String artifactId, String packaging, String version, Scope scope)
			implements Dependency {

		public static MavenDependency of(String source) {

			var parts = source.trim().split(":");

			return new MavenDependency(parts[0], parts[1], parts[2], parts[3], Scope.of(parts[4]));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.cli.build.BuildSystem#getScriptOperations()
	 */
	@Override
	public ScriptOperations getScriptOperations() {
		return updater;
	}
}
