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

import static org.assertj.core.api.Assertions.*;

import org.jmolecules.stereotype.catalog.StereotypeGroup;
import org.jmolecules.stereotype.catalog.StereotypeGroup.Type;
import org.junit.jupiter.api.Test;

/**
 * @author Oliver Drotbohm
 */
public class JsonPathStereotypesCatalogUnitTests {

	@Test
	void testName() {

		var source = CatalogSource.ofClassLoader(getClass().getClassLoader());
		var catalog = new JsonPathStereotypeCatalog(source);

		var group = catalog.getGroupsFor("org.springframework.boot.jackson.JsonMixin");

		System.out.println(group);

		System.out.println();
		catalog.forEach(System.out::println);

		System.out.println();
		catalog.asMap().entrySet().forEach(System.out::println);

		assertThat(catalog.getGroups().streamByType(Type.TECHNOLOGY))
				.extracting(StereotypeGroup::getIdentifier)
				.containsExactly("custom.group", "spring.web", "spring.web.rest", "java", "java.jaxb");

		assertThat(catalog.getGroups("java").and(catalog.getGroups("spring")).streamPrioritized())
				.extracting(StereotypeGroup::getIdentifier)
				.containsExactly("spring.web", "spring.web.rest", "java", "java.jaxb");
	}
}
