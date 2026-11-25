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

import org.jmolecules.architecture.layered.Layered;
import org.jmolecules.archunit.JMoleculesArchitectureRules.LayeredAnnotation;
import org.jmolecules.archunit.JMoleculesArchitectureRules.StereotypeLookup;
import org.jspecify.annotations.Nullable;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;

/**
 * Options to customize the verification of Layered Architectures.
 *
 * @author Oliver Drotbohm
 * @since 0.34
 * @see JMoleculesArchitectureRules#ensureLayering(LayeredVerificationOptions)
 * @see JMoleculesArchitectureRules#ensureLayeringStrict(LayeredVerificationOptions)
 */
public final class LayeredVerificationOptions extends UnitAwareVerificationOptions<LayeredVerificationOptions> {

	private static final LayeredVerificationOptions DEFAULTS = new LayeredVerificationOptions(
			StereotypeLookup.defaultLookup(), null,
			Set.of(LayeredAnnotation.EXPOSES_APPLICATION, LayeredAnnotation.EXPOSES_DOMAIN),
			Set.of(LayeredAnnotation.REFERS_VIA_APPLICATION, LayeredAnnotation.REFERS_VIA_DOMAIN), null, false);

	private LayeredVerificationOptions(StereotypeLookup stereotypeLookup, @Nullable DescribedPredicate<JavaClass> scope,
			@Nullable Set<String> exposure, @Nullable Set<String> referrers,
			@Nullable ArchitecturalUnitResolver unitResolver, boolean directApplicationAccess) {
		super(stereotypeLookup, scope, exposure, referrers, unitResolver, directApplicationAccess);
	}

	/**
	 * Returns the default {@link LayeredVerificationOptions}, i.e. {@link StereotypeLookup#defaultLookup()}, only the
	 * application and domain layer being allowed to refer to other
	 * {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit}s, and only to their application and domain
	 * layer ({@link Layered.Referrer#APPLICATION}, {@link Layered.Referrer#DOMAIN}, {@link Layered.Exposure#APPLICATION},
	 * {@link Layered.Exposure#DOMAIN}), annotation-based detection of units and no scope restriction. Other layers have to be opted into via {@link #withExposure(Layered.Exposure...)} and
	 * {@link #withReferrers(Layered.Referrer...)}.
	 *
	 * @return will never be {@literal null}.
	 */
	public static LayeredVerificationOptions defaults() {
		return DEFAULTS;
	}

	/**
	 * Returns new {@link LayeredVerificationOptions} exposing the types of the given layers of
	 * {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit}s using layers to other units. Defaults to
	 * {@link Layered.Exposure#APPLICATION} and {@link Layered.Exposure#DOMAIN}. Units can override the setting via {@link Layered#exposes()}.
	 *
	 * @param exposure must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 */
	public LayeredVerificationOptions withExposure(Layered.Exposure... exposure) {
		return withExposureKeys(exposure);
	}

	/**
	 * Returns new {@link LayeredVerificationOptions} only allowing the types of the given layers of
	 * {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit}s using layers to refer to other units.
	 * Defaults to {@link Layered.Referrer#APPLICATION} and {@link Layered.Referrer#DOMAIN}. Units can override the setting via
	 * {@link Layered#refersVia()}.
	 *
	 * @param referrers must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 */
	public LayeredVerificationOptions withReferrers(Layered.Referrer... referrers) {
		return withReferrerKeys(referrers);
	}

	/**
	 * Returns new {@link LayeredVerificationOptions} exposing the types of the given layers in addition to the ones
	 * currently configured. The programmatic equivalent of {@link Layered#additionallyExposes()}.
	 *
	 * @param exposure must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 * @see #withExposure(Layered.Exposure...)
	 */
	public LayeredVerificationOptions withAdditionalExposure(Layered.Exposure... exposure) {
		return withAdditionalExposureKeys(exposure);
	}

	/**
	 * Returns new {@link LayeredVerificationOptions} allowing the types of the given layers to refer to other
	 * {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit}s in addition to the ones currently configured.
	 * The programmatic equivalent of {@link Layered#additionallyRefersVia()}.
	 *
	 * @param referrers must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 * @see #withReferrers(Layered.Referrer...)
	 */
	public LayeredVerificationOptions withAdditionalReferrers(Layered.Referrer... referrers) {
		return withAdditionalReferrerKeys(referrers);
	}

	/**
	 * Returns new {@link LayeredVerificationOptions} allowing application layer types (including types not assigned to
	 * a layer) to refer to the application layer types of other
	 * {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit}s using layers, as well as to all types of
	 * units not using them. That access is granted independently of the exposure and referrers configured, globally or
	 * for individual units via {@link Layered#exposes()} and {@link Layered#refersVia()}. Other layers still only get
	 * access to the layers exposed. Disabled by default.
	 *
	 * @return will never be {@literal null}.
	 * @see #withoutDirectApplicationAccess()
	 * @see #withExposure(Layered.Exposure...)
	 * @see #withReferrers(Layered.Referrer...)
	 */
	public LayeredVerificationOptions withDirectApplicationAccess() {
		return withDirectApplicationAccess(true);
	}

	/**
	 * Returns new {@link LayeredVerificationOptions} not granting application layer types any access to other
	 * {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit}s beyond the exposure and referrers configured.
	 *
	 * @return will never be {@literal null}.
	 * @see #withDirectApplicationAccess()
	 */
	public LayeredVerificationOptions withoutDirectApplicationAccess() {
		return withDirectApplicationAccess(false);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.archunit.UnitAwareVerificationOptions#with(...)
	 */
	@Override
	LayeredVerificationOptions with(StereotypeLookup stereotypeLookup, @Nullable DescribedPredicate<JavaClass> scope,
			@Nullable Set<String> exposure, @Nullable Set<String> referrers,
			@Nullable ArchitecturalUnitResolver unitResolver, boolean directApplicationAccess) {
		return new LayeredVerificationOptions(stereotypeLookup, scope, exposure, referrers, unitResolver,
				directApplicationAccess);
	}
}
