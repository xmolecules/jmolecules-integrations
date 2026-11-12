/*
 * Copyright 2026 the original author or authors.
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
package org.jmolecules.archunit;

import org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit;
import org.jspecify.annotations.Nullable;

import com.tngtech.archunit.core.domain.JavaClass;

/**
 * A strategy to resolve the {@link ArchitecturalUnit} a {@link JavaClass} belongs to. The default,
 * {@link ArchitecturalUnits}, detects units via packages annotated with an annotation meta-annotated with
 * {@code org.jmolecules.stereotype.ArchitecturalUnit}. Frameworks with their own notion of modules can provide an
 * implementation, creating {@link ArchitecturalUnit}s via
 * {@link ArchitecturalUnit#of(String, String, String, com.tngtech.archunit.core.domain.JavaPackage, ArchitecturalUnit)}.
 *
 * @author Oliver Drotbohm
 * @since 0.34
 * @see UnitAwareVerificationOptions#withUnitResolver(ArchitecturalUnitResolver)
 */
@FunctionalInterface
public interface ArchitecturalUnitResolver {

	/**
	 * Returns an {@link ArchitecturalUnitResolver} that never resolves any {@link ArchitecturalUnit}. Use it to opt out
	 * of the verification of references between units, e.g. to keep the verification behavior for code bases using
	 * jMolecules' {@code Module} annotation that existed before architectural units were introduced.
	 *
	 * @return will never be {@literal null}.
	 */
	static ArchitecturalUnitResolver none() {
		return type -> null;
	}

	/**
	 * Returns the {@link ArchitecturalUnit} the given type belongs to.
	 *
	 * @param type must not be {@literal null}.
	 * @return the unit or {@literal null} if the type does not belong to any unit.
	 */
	@Nullable
	ArchitecturalUnit getUnitFor(JavaClass type);
}
