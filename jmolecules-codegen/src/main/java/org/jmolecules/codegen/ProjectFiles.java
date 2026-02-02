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
package org.jmolecules.codegen;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jmolecules.codegen.SourceFile.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.util.function.ThrowingConsumer;

/**
 * @author Oliver Drotbohm
 */
public class ProjectFiles {

	private static final Logger LOG = LoggerFactory.getLogger(ProjectFiles.class);

	private final Path root, srcMainJava, srcTestJava;
	private final Supplier<ProjectConfiguration> configuration;

	public ProjectFiles(String projectRoot) {
		this(Path.of(projectRoot));
	}

	ProjectFiles(Path projectRoot) {

		this.root = projectRoot;
		this.srcMainJava = root.resolve("src/main/java");
		this.srcTestJava = root.resolve("src/test/java");
		this.configuration = SingletonSupplier.of(() -> new ProjectConfiguration(this::resolve));
	}

	public ProjectConfiguration getConfiguration() {
		return configuration.get();
	}

	public Path getProjectRoot() {
		return root;
	}

	public Path resolve(String file) {
		return root.resolve(file);
	}

	public List<String> findSourceFileContaining(String reference, boolean useRegex) {

		List<String> results = new ArrayList<>();

		try (Stream<Path> walk = Files.walk(srcMainJava)) {

			walk.filter(Files::isRegularFile)
					.filter(p -> p.toString().endsWith(".java"))
					.forEach(ThrowingConsumer.of(p -> {

						try (var reader = Files.newBufferedReader(p)) {
							String line;

							while ((line = reader.readLine()) != null) {

								var matches = useRegex
										? line.matches(reference)
										: line.contains(reference);

								if (matches) {
									results.add(sourceFileToClassName(p));
									break;
								}
							}
						}
					}));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return results;
	}

	private String sourceFileToClassName(Path path) {

		var result = srcMainJava.relativize(path).toString();
		result = result.substring(0, result.lastIndexOf('.'));

		return ClassUtils.convertResourcePathToClassName(result);
	}

	public void writeSource(SourceFile source) {

		var path = from(source.type());
		var file = source.file();
		var fullPath = path.resolve(source.toPath());
		var params = new Object[] { source.getIcon(), fullPath };

		if (Files.exists(fullPath)) {

			LOG.info("{} Skipping creation of {} as it already exists.", params);
			return;
		}

		LOG.info("{} ️Writing source file {}.", params);

		try {
			file.writeToPath(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void writeSource(Path path, Type type, String content) {

		var fullPath = from(type).resolve(path);
		var params = new Object[] { NamedFile.getIcon(fullPath.toString()), fullPath };

		if (Files.exists(fullPath)) {
			LOG.info("{} Skipping creation of {} as it already exists.", params);
		}

		createDirectoriesIfNecessary(fullPath.getParent());

		LOG.info("{} Writing source file {}.", params);

		try {

			Files.writeString(fullPath, content);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private Path from(Type type) {

		return switch (type) {
			case SOURCE -> srcMainJava;
			case TEST_SOURCE -> srcTestJava;
			default -> throw new IllegalArgumentException("Unexpected value: " + type);
		};
	}

	private static void createDirectoriesIfNecessary(Path path) {

		if (!Files.exists(path)) {
			try {
				Files.createDirectories(path);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}
}
