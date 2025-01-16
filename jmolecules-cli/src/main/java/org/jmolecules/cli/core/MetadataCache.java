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
package org.jmolecules.cli.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.jmolecules.codegen.ProjectFiles;

/**
 * @author Oliver Drotbohm
 */
public class MetadataCache {

	public static final String CACHE_LOCATION = ".jm";

	private final Path cache;

	public MetadataCache(ProjectFiles files) {

		this.cache = files.resolve(CACHE_LOCATION);

		createDirectoriesIfNecessary(cache);
	}

	public void clearCaches() {

		try (Stream<Path> walk = Files.walk(cache)) {

			walk.sorted(Comparator.reverseOrder())
					.forEach(p -> {
						try {
							Files.delete(p);
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Path resolveInternal(String file) {
		return cache.resolve(file);
	}

	public String readInternal(String file) {
		return wrap(() -> Files.readString(resolveInternal(file)));
	}

	public void writeInternal(String fileName, String content) {

		var path = resolveInternal(fileName);

		try {
			Files.writeString(path, content);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void createDirectoriesIfNecessary(Path path) {

		if (!Files.exists(path)) {
			try {
				Files.createDirectories(path);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	private <T> T wrap(IoSupplier<T> supplier) {

		try {
			return supplier.get();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private interface IoSupplier<T> {
		T get() throws IOException;
	}
}
