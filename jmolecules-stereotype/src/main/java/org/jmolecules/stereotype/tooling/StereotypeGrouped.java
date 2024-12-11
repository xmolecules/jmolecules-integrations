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
package org.jmolecules.stereotype.tooling;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import org.jmolecules.stereotype.api.Stereotype;
import org.jmolecules.stereotype.tooling.StereotypeGrouper.OtherStereotype;

class StereotypeGrouped<T> extends Grouped<Stereotype, T> {

	private static final Comparator<Stereotype> BY_NAME_WITH_OTHER_LAST = (left, right) -> {

		if (left.equals(OtherStereotype.INSTANCE)) {
			return right.equals(OtherStereotype.INSTANCE) ? 0 : 1;
		}

		if (right.equals(OtherStereotype.INSTANCE)) {
			return left.equals(OtherStereotype.INSTANCE) ? 0 : -1;
		}

		return Comparator.comparing(Stereotype::getDisplayName).compare(left, right);
	};

	StereotypeGrouped(Map<Stereotype, Collection<T>> groups) {
		super(groups, BY_NAME_WITH_OTHER_LAST);
	}

	StereotypeGrouped(Collection<T> elements) {
		this(Map.of(OtherStereotype.INSTANCE, elements));
	}

	boolean hasOnlyOtherStereotype() {
		return hasOnlyOneEntry() && getKeys().contains(OtherStereotype.INSTANCE);
	}
}
