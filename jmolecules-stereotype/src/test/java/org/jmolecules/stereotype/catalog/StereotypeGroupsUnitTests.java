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
package org.jmolecules.stereotype.catalog;

import static org.assertj.core.api.Assertions.*;

import java.net.URI;
import java.util.List;
import java.util.TreeSet;

import org.jmolecules.stereotype.catalog.StereotypeGroup.Type;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link StereotypeGroups}.
 *
 * @author Oliver Drotbohm
 */
class StereotypeGroupsUnitTests {

	@Test
	void returnsGroupsByType() {

		var source = URI.create("source");

		var first = new StereotypeGroup("foo", "Foo", Type.ARCHITECTURE, 10, source);
		var second = new StereotypeGroup("bar", "Bar", Type.ARCHITECTURE, 20, source);
		var third = new StereotypeGroup("fooBar", "FooBar", Type.DESIGN, 10, source);

		var groups = new StereotypeGroups(new TreeSet<>(List.of(third, second, first)));

		assertThat(groups.streamByType(Type.ARCHITECTURE)).containsExactly(first, second);
		assertThat(groups.streamByType(Type.DESIGN)).containsExactly(third);
	}
}
