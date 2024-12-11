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

import org.jmolecules.stereotype.catalog.StereotypeGroup.Type;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link StereotypeGroup}.
 *
 * @author Oliver Drotbohm
 */
class StereotypeGroupUnitTests {

	static final URI SOURCE = URI.create("source");

	@Test
	void defaultsTypeAndPriority() {

		var group = new StereotypeGroup("id", "Display Name", null, null, SOURCE);

		assertThat(group.hasType(Type.TECHNOLOGY)).isTrue();
		assertThat(group.getPriority()).isEqualTo(0);
	}

	@Test
	void ordersParentFirst() {

		var first = new StereotypeGroup("foo", "Foo", Type.TECHNOLOGY, 20, SOURCE);
		var second = new StereotypeGroup("foo.bar", "Foo", Type.TECHNOLOGY, 10, SOURCE);

		assertOrdering(first, second);
	}

	private static void assertOrdering(StereotypeGroup first, StereotypeGroup second) {

		var comparator = StereotypeGroup.prioritized();

		assertThat(comparator.compare(first, second)).isLessThan(0);
		assertThat(comparator.compare(second, first)).isGreaterThan(0);
		assertThat(comparator.compare(first, first)).isEqualTo(0);
		assertThat(comparator.compare(second, second)).isEqualTo(0);
	}
}
