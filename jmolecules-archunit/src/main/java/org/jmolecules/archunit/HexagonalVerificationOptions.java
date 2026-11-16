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

import org.jmolecules.architecture.hexagonal.Hexagon;
import org.jmolecules.archunit.JMoleculesArchitectureRules.HexagonAnnotation;
import org.jmolecules.archunit.JMoleculesArchitectureRules.StereotypeLookup;
import org.jmolecules.archunit.JMoleculesArchitectureRules.VerificationDepth;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;

/**
 * Options to customize the verification of Hexagonal Architectures.
 *
 * @author Oliver Drotbohm
 * @since 0.34
 * @see JMoleculesArchitectureRules#ensureHexagonal(HexagonalVerificationOptions)
 */
public final class HexagonalVerificationOptions
		extends UnitAwareVerificationOptions<HexagonalVerificationOptions> {

	private static final HexagonalVerificationOptions DEFAULTS = new HexagonalVerificationOptions(
			StereotypeLookup.defaultLookup(), null, Set.of(HexagonAnnotation.EXPOSES_PRIMARY_ADAPTERS),
			Set.of(HexagonAnnotation.REFERS_VIA_SECONDARY_ADAPTERS), null, false, VerificationDepth.STRICT);

	private final VerificationDepth depth;

	private HexagonalVerificationOptions(StereotypeLookup stereotypeLookup,
			@Nullable DescribedPredicate<JavaClass> scope, @Nullable Set<String> exposure,
			@Nullable Set<String> referrers, @Nullable ArchitecturalUnitResolver unitResolver,
			boolean directApplicationAccess, VerificationDepth depth) {

		super(stereotypeLookup, scope, exposure, referrers, unitResolver, directApplicationAccess);

		this.depth = depth;
	}

	/**
	 * Returns the default {@link HexagonalVerificationOptions}, i.e. {@link StereotypeLookup#defaultLookup()},
	 * {@link VerificationDepth#STRICT}, {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit}s only
	 * exposing their primary adapters and only allowing their secondary adapters to refer to other units,
	 * annotation-based detection of units and no scope restriction.
	 *
	 * @return will never be {@literal null}.
	 */
	public static HexagonalVerificationOptions defaults() {
		return DEFAULTS;
	}

	/**
	 * Returns new {@link HexagonalVerificationOptions} using the given {@link VerificationDepth}. Defaults to
	 * {@link VerificationDepth#STRICT}.
	 *
	 * @param depth must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public HexagonalVerificationOptions withDepth(VerificationDepth depth) {

		Assert.notNull(depth, "VerificationDepth must not be null!");

		return new HexagonalVerificationOptions(getStereotypeLookup(), getScope(), getExposure(), getReferrers(),
				getUnitResolver(), isDirectApplicationAccess(), depth);
	}

	/**
	 * Returns new {@link HexagonalVerificationOptions} exposing the types of the given roles of
	 * {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit}s using hexagonal stereotypes to other units.
	 * Primary adapters are only ever exposed to secondary adapters of other units, or units not using hexagonal
	 * stereotypes. Defaults to {@link Hexagon.Exposure#PRIMARY_ADAPTERS}. Units can override the setting via
	 * {@link Hexagon#exposes()}.
	 *
	 * @param exposure must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 */
	public HexagonalVerificationOptions withExposure(Hexagon.Exposure... exposure) {
		return withExposureKeys(exposure);
	}

	/**
	 * Returns new {@link HexagonalVerificationOptions} only allowing the types of the given roles of
	 * {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit}s using hexagonal stereotypes to refer to other
	 * units. Defaults to {@link Hexagon.Referrer#SECONDARY_ADAPTERS}. Units can override the setting via
	 * {@link Hexagon#refersVia()}.
	 *
	 * @param referrers must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 */
	public HexagonalVerificationOptions withReferrers(Hexagon.Referrer... referrers) {
		return withReferrerKeys(referrers);
	}

	/**
	 * Returns new {@link HexagonalVerificationOptions} exposing the types of the given roles in addition to the ones
	 * currently configured. The programmatic equivalent of {@link Hexagon#additionallyExposes()}.
	 *
	 * @param exposure must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 * @see #withExposure(Hexagon.Exposure...)
	 */
	public HexagonalVerificationOptions withAdditionalExposure(Hexagon.Exposure... exposure) {
		return withAdditionalExposureKeys(exposure);
	}

	/**
	 * Returns new {@link HexagonalVerificationOptions} allowing the types of the given roles to refer to other
	 * {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit}s in addition to the ones currently configured.
	 * The programmatic equivalent of {@link Hexagon#additionallyRefersVia()}.
	 *
	 * @param referrers must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 * @see #withReferrers(Hexagon.Referrer...)
	 */
	public HexagonalVerificationOptions withAdditionalReferrers(Hexagon.Referrer... referrers) {
		return withAdditionalReferrerKeys(referrers);
	}

	/**
	 * Returns new {@link HexagonalVerificationOptions} allowing the application core, i.e. application code (including
	 * types without hexagonal stereotypes) as well as primary and secondary ports, to refer to the application code and
	 * primary ports of other {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit}s using hexagonal
	 * stereotypes, as well as to all types of units not using them. That access is granted independently of the exposure
	 * and referrers configured, globally or for individual units via {@link Hexagon#exposes()} and
	 * {@link Hexagon#refersVia()}. Other roles still only get access to the roles exposed, i.e. secondary adapters may
	 * only refer to the primary ports of other units if those are exposed explicitly. Disabled by default.
	 *
	 * @return will never be {@literal null}.
	 * @see #withoutDirectApplicationAccess()
	 * @see #withExposure(Hexagon.Exposure...)
	 * @see #withReferrers(Hexagon.Referrer...)
	 */
	public HexagonalVerificationOptions withDirectApplicationAccess() {
		return withDirectApplicationAccess(true);
	}

	/**
	 * Returns new {@link HexagonalVerificationOptions} not granting the application core any access to other
	 * {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit}s beyond the exposure and referrers configured.
	 *
	 * @return will never be {@literal null}.
	 * @see #withDirectApplicationAccess()
	 */
	public HexagonalVerificationOptions withoutDirectApplicationAccess() {
		return withDirectApplicationAccess(false);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.archunit.UnitAwareVerificationOptions#with(...)
	 */
	@Override
	HexagonalVerificationOptions with(StereotypeLookup stereotypeLookup, @Nullable DescribedPredicate<JavaClass> scope,
			@Nullable Set<String> exposure, @Nullable Set<String> referrers,
			@Nullable ArchitecturalUnitResolver unitResolver, boolean directApplicationAccess) {
		return new HexagonalVerificationOptions(stereotypeLookup, scope, exposure, referrers, unitResolver,
				directApplicationAccess, depth);
	}

	VerificationDepth getDepth() {
		return depth;
	}
}
