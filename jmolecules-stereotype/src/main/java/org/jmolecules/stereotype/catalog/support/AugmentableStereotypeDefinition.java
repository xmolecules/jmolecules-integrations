/*
 * Copyright 2025-2025 the original author or authors.
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

import java.util.Collection;

import org.jmolecules.stereotype.api.Stereotype;
import org.jmolecules.stereotype.catalog.StereotypeDefinition;

/**
 * A {@link StereotypeDefinition} that can get additional {@link Assignment}s registered.
 *
 * @author Oliver Drotbohm
 */
interface AugmentableStereotypeDefinition extends StereotypeDefinition {

	/**
	 * Adds the given {@link Assignment}s to the current {@link StereotypeDefinition}.
	 *
	 * @param assignment must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	AugmentableStereotypeDefinition add(Collection<Assignment> assignment);

	AugmentableStereotypeDefinition addSource(Object source);

	/**
	 * Verifies the current {@link StereotypeDefinition} against the given {@link Stereotype} to detect potential illegal
	 * redefinitions.
	 *
	 * @param stereotype must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	default AugmentableStereotypeDefinition verify(Stereotype stereotype) {

		var current = getStereotype();

		if (!current.equals(stereotype)) {
			throw new IllegalStateException(
					"Illegal redefinition of stereotype %s as %s!".formatted(stereotype.toDetailedString(),
							current.toDetailedString()));
		}

		return this;
	}
}
