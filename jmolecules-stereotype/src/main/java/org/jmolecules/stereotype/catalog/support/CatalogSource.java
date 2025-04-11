/*
 * Copyright 2024-2025 the original author or authors.
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
package org.jmolecules.stereotype.catalog.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A source of a {@link org.jmolecules.stereotype.catalog.StereotypeCatalog}.
 *
 * @author Oliver Drotbohm
 */
public interface CatalogSource extends Iterable<URL> {

	static final String DEFAULT_STEREOTYPE_LOCATION = "META-INF/jmolecules-stereotypes.json";
	static final String DEFAULT_GROUP_LOCATION = "META-INF/jmolecules-stereotype-groups.json";

	Stream<URL> getSources();

	/**
	 * Creates a new {@link CatalogSource} reading a catalog from resources available to the given {@link ClassLoader}.
	 *
	 * @param classLoader must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public static CatalogSource ofClassLoader(ClassLoader classLoader) {

		if (classLoader == null) {
			throw new IllegalArgumentException("ClassLoader must not be null!");
		}

		try {

			var stereotypes = Collections.list(classLoader.getResources(DEFAULT_STEREOTYPE_LOCATION));
			var groups = Collections.list(classLoader.getResources(DEFAULT_GROUP_LOCATION));

			Logger LOG = LoggerFactory.getLogger(CatalogSource.class);

			LOG.info("Loading jMolecules stereotypes from {} and {}.", stereotypes, groups);

			return () -> Stream.concat(stereotypes.stream(), groups.stream());

		} catch (IOException o_O) {
			throw new UncheckedIOException(o_O);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	default Iterator<URL> iterator() {
		return getSources().iterator();
	}
}
