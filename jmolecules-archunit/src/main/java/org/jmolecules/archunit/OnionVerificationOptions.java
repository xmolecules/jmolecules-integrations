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

import java.util.Set;

import org.jmolecules.architecture.onion.Onion;
import org.jmolecules.archunit.JMoleculesArchitectureRules.StereotypeLookup;
import org.jspecify.annotations.Nullable;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;

/**
 * Options to customize the verification of Onion Architectures, both the classic and the simplified variant.
 *
 * @author Oliver Drotbohm
 * @since 0.34
 * @see JMoleculesArchitectureRules#ensureOnionSimple(OnionVerificationOptions)
 * @see JMoleculesArchitectureRules#ensureOnionClassical(OnionVerificationOptions)
 */
public final class OnionVerificationOptions extends UnitAwareVerificationOptions<OnionVerificationOptions> {

	private static final OnionVerificationOptions DEFAULTS = new OnionVerificationOptions(
			StereotypeLookup.defaultLookup(), null, null, null, null);

	private OnionVerificationOptions(StereotypeLookup stereotypeLookup, @Nullable DescribedPredicate<JavaClass> scope,
			@Nullable Set<String> exposure, @Nullable Set<String> referrers,
			@Nullable ArchitecturalUnitResolver unitResolver) {
		super(stereotypeLookup, scope, exposure, referrers, unitResolver);
	}

	/**
	 * Returns the default {@link OnionVerificationOptions}, i.e. {@link StereotypeLookup#defaultLookup()}, all rings
	 * exposed to and allowed to refer to other {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit}s,
	 * annotation-based detection of units and no scope restriction.
	 *
	 * @return will never be {@literal null}.
	 */
	public static OnionVerificationOptions defaults() {
		return DEFAULTS;
	}

	/**
	 * Returns new {@link OnionVerificationOptions} exposing the types of the given rings of
	 * {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit}s using rings to other units. Infrastructure
	 * ring types are only ever exposed to infrastructure ring types of other units, or units not using rings. Defaults
	 * to all rings. Units can override the setting via {@link Onion#exposes()}.
	 *
	 * @param exposure must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 */
	public OnionVerificationOptions withExposure(Onion.Exposure... exposure) {
		return withExposureKeys(exposure);
	}

	/**
	 * Returns new {@link OnionVerificationOptions} only allowing the types of the given rings of
	 * {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit}s using rings to refer to other units.
	 * Defaults to all rings. Units can override the setting via {@link Onion#refersVia()}.
	 *
	 * @param referrers must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 */
	public OnionVerificationOptions withReferrers(Onion.Referrer... referrers) {
		return withReferrerKeys(referrers);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.archunit.UnitAwareVerificationOptions#with(...)
	 */
	@Override
	OnionVerificationOptions with(StereotypeLookup stereotypeLookup, @Nullable DescribedPredicate<JavaClass> scope,
			@Nullable Set<String> exposure, @Nullable Set<String> referrers,
			@Nullable ArchitecturalUnitResolver unitResolver) {
		return new OnionVerificationOptions(stereotypeLookup, scope, exposure, referrers, unitResolver);
	}
}
