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
import org.jmolecules.archunit.JMoleculesArchitectureRules.OnionAnnotation;
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
			StereotypeLookup.defaultLookup(), null, Set.of(OnionAnnotation.EXPOSES_INFRASTRUCTURE),
			Set.of(OnionAnnotation.REFERS_VIA_INFRASTRUCTURE), null, false);

	private OnionVerificationOptions(StereotypeLookup stereotypeLookup, @Nullable DescribedPredicate<JavaClass> scope,
			@Nullable Set<String> exposure, @Nullable Set<String> referrers,
			@Nullable ArchitecturalUnitResolver unitResolver, boolean directApplicationAccess) {
		super(stereotypeLookup, scope, exposure, referrers, unitResolver, directApplicationAccess);
	}

	/**
	 * Returns the default {@link OnionVerificationOptions}, i.e. {@link StereotypeLookup#defaultLookup()}, only the
	 * infrastructure ring being exposed to and allowed to refer to other
	 * {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit}s ({@link Onion.Exposure#INFRASTRUCTURE},
	 * {@link Onion.Referrer#INFRASTRUCTURE}), annotation-based detection of units and no scope restriction. References
	 * between application rings have to be opted into via {@link #withExposure(Onion.Exposure...)} and
	 * {@link #withReferrers(Onion.Referrer...)}.
	 *
	 * @return will never be {@literal null}.
	 */
	public static OnionVerificationOptions defaults() {
		return DEFAULTS;
	}

	/**
	 * Returns new {@link OnionVerificationOptions} exposing the types of the given rings of
	 * {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit}s using rings to other units. Infrastructure
	 * ring types are only ever exposed to infrastructure ring types of other units, or units not using rings. Defaults to
	 * all rings. Units can override the setting via {@link Onion#exposes()}.
	 *
	 * @param exposure must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 */
	public OnionVerificationOptions withExposure(Onion.Exposure... exposure) {
		return withExposureKeys(exposure);
	}

	/**
	 * Returns new {@link OnionVerificationOptions} only allowing the types of the given rings of
	 * {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit}s using rings to refer to other units. Defaults
	 * to {@link Onion.Referrer#INFRASTRUCTURE}. Units can override the setting via {@link Onion#refersVia()}.
	 *
	 * @param referrers must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 */
	public OnionVerificationOptions withReferrers(Onion.Referrer... referrers) {
		return withReferrerKeys(referrers);
	}

	/**
	 * Returns new {@link OnionVerificationOptions} exposing the types of the given rings in addition to the ones
	 * currently configured. The programmatic equivalent of {@link Onion#additionallyExposes()}.
	 *
	 * @param exposure must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 * @see #withExposure(Onion.Exposure...)
	 */
	public OnionVerificationOptions withAdditionalExposure(Onion.Exposure... exposure) {
		return withAdditionalExposureKeys(exposure);
	}

	/**
	 * Returns new {@link OnionVerificationOptions} allowing the types of the given rings to refer to other
	 * {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit}s in addition to the ones currently configured.
	 * The programmatic equivalent of {@link Onion#additionallyRefersVia()}.
	 *
	 * @param referrers must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 * @see #withReferrers(Onion.Referrer...)
	 */
	public OnionVerificationOptions withAdditionalReferrers(Onion.Referrer... referrers) {
		return withAdditionalReferrerKeys(referrers);
	}

	/**
	 * Returns new {@link OnionVerificationOptions} allowing application ring types (including types not assigned to a
	 * ring) to refer to the application ring types of other
	 * {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit}s using rings, as well as to all types of units
	 * not using them. That access is granted independently of the exposure and referrers configured, globally or for
	 * individual units via {@link Onion#exposes()} and {@link Onion#refersVia()}. Other rings still only get access to
	 * the rings exposed. Disabled by default.
	 *
	 * @return will never be {@literal null}.
	 * @see #withoutDirectApplicationAccess()
	 * @see #withExposure(Onion.Exposure...)
	 * @see #withReferrers(Onion.Referrer...)
	 */
	public OnionVerificationOptions withDirectApplicationAccess() {
		return withDirectApplicationAccess(true);
	}

	/**
	 * Returns new {@link OnionVerificationOptions} not granting application ring types any access to other
	 * {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit}s beyond the exposure and referrers configured.
	 *
	 * @return will never be {@literal null}.
	 * @see #withDirectApplicationAccess()
	 */
	public OnionVerificationOptions withoutDirectApplicationAccess() {
		return withDirectApplicationAccess(false);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.archunit.UnitAwareVerificationOptions#with(...)
	 */
	@Override
	OnionVerificationOptions with(StereotypeLookup stereotypeLookup, @Nullable DescribedPredicate<JavaClass> scope,
			@Nullable Set<String> exposure, @Nullable Set<String> referrers,
			@Nullable ArchitecturalUnitResolver unitResolver, boolean directApplicationAccess) {
		return new OnionVerificationOptions(stereotypeLookup, scope, exposure, referrers, unitResolver,
				directApplicationAccess);
	}
}
