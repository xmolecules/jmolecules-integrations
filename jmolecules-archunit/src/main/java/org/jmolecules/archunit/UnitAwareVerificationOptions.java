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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jmolecules.archunit.JMoleculesArchitectureRules.StereotypeLookup;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;

/**
 * Base class for the options of architectural languages that can also verify the references between
 * {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit}s. A unit is considered to use a language if at
 * least one of its types is assigned a stereotype of it, or if it is declared via the language's unit annotation
 * ({@link org.jmolecules.architecture.hexagonal.Hexagon}, {@link org.jmolecules.architecture.onion.Onion},
 * {@link org.jmolecules.architecture.layered.Layered}). For the Onion Architecture, the latter only applies to the
 * variant whose rings the unit uses, if any. For units using the language, the options define:
 * <ul>
 * <li>which roles of the language a unit exposes to other units, i.e. which of its types other units may refer to
 * ({@code withExposure(…)} on the language-specific options),</li>
 * <li>which roles of the language may refer to other units ({@code withReferrers(…)}).</li>
 * </ul>
 * The defaults are language-specific (see the {@code defaults()} method of the concrete options) and can be overridden
 * per unit via the {@code exposes} and {@code refersVia} attributes of the language's unit annotation, which replace
 * the global setting for the unit. Units not using the language are not restricted in what they refer to, but have to
 * use the types exposed by the units using it, which also applies to types not part of any unit. References from or to
 * types outside the scope configured via {@link #withScope(DescribedPredicate)} are not verified.
 *
 * @param <SELF> the concrete options type.
 * @author Oliver Drotbohm
 * @since 0.34
 */
public abstract class UnitAwareVerificationOptions<SELF extends UnitAwareVerificationOptions<SELF>>
		extends VerificationOptions<SELF> {

	private final @Nullable Set<String> exposure, referrers;
	private final @Nullable ArchitecturalUnitResolver unitResolver;
	private final boolean directApplicationAccess;

	UnitAwareVerificationOptions(StereotypeLookup stereotypeLookup, @Nullable DescribedPredicate<JavaClass> scope,
			@Nullable Set<String> exposure, @Nullable Set<String> referrers,
			@Nullable ArchitecturalUnitResolver unitResolver, boolean directApplicationAccess) {

		super(stereotypeLookup, scope);

		this.exposure = exposure;
		this.referrers = referrers;
		this.unitResolver = unitResolver;
		this.directApplicationAccess = directApplicationAccess;
	}

	/**
	 * Returns new options using the given {@link ArchitecturalUnitResolver} to detect the
	 * {@link org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit} a type belongs to. By default, units are
	 * detected via packages annotated with an annotation meta-annotated with
	 * {@code org.jmolecules.stereotype.ArchitecturalUnit}.
	 *
	 * @param unitResolver must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @see org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit#of(String, String, String,
	 *      com.tngtech.archunit.core.domain.JavaPackage, org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit)
	 */
	public SELF withUnitResolver(ArchitecturalUnitResolver unitResolver) {

		Assert.notNull(unitResolver, "ArchitecturalUnitResolver must not be null!");

		return with(getStereotypeLookup(), getScope(), exposure, referrers, unitResolver, directApplicationAccess);
	}

	abstract SELF with(StereotypeLookup stereotypeLookup, @Nullable DescribedPredicate<JavaClass> scope,
			@Nullable Set<String> exposure, @Nullable Set<String> referrers,
			@Nullable ArchitecturalUnitResolver unitResolver, boolean directApplicationAccess);

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.archunit.VerificationOptions#with(...)
	 */
	@Override
	SELF with(StereotypeLookup stereotypeLookup, @Nullable DescribedPredicate<JavaClass> scope) {
		return with(stereotypeLookup, scope, exposure, referrers, unitResolver, directApplicationAccess);
	}

	SELF withExposureKeys(Object[] values) {
		return with(getStereotypeLookup(), getScope(), keys(values), referrers, unitResolver, directApplicationAccess);
	}

	SELF withReferrerKeys(Object[] values) {
		return with(getStereotypeLookup(), getScope(), exposure, keys(values), unitResolver, directApplicationAccess);
	}

	SELF withAdditionalExposureKeys(Object[] values) {
		return with(getStereotypeLookup(), getScope(), merge(exposure, keys(values)), referrers, unitResolver,
				directApplicationAccess);
	}

	SELF withAdditionalReferrerKeys(Object[] values) {
		return with(getStereotypeLookup(), getScope(), exposure, merge(referrers, keys(values)), unitResolver,
				directApplicationAccess);
	}

	/**
	 * Returns the union of the given role names, {@literal null} (i.e. all roles) if the current ones are.
	 */
	private static @Nullable Set<String> merge(@Nullable Set<String> current, Set<String> additional) {

		if (current == null) {
			return null;
		}

		return Stream.concat(current.stream(), additional.stream()).collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * Returns new options allowing or prohibiting application code to refer to the application code of other units (and
	 * the language-specific equivalents, see the concrete options), independently of the exposure and referrers
	 * configured.
	 */
	SELF withDirectApplicationAccess(boolean enabled) {
		return with(getStereotypeLookup(), getScope(), exposure, referrers, unitResolver, enabled);
	}

	/**
	 * Returns the names of the jMolecules 2.1 enum constants given. Takes an {@link Object} array so that the callers
	 * don't need to refer to the enum types in a way that would make the JVM load them when verifying the options types,
	 * as they might not be present.
	 */
	private static Set<String> keys(Object[] values) {

		Assert.notEmpty(values, "Values must not be null or empty!");

		return Arrays.stream(values)
				.map(it -> ((Enum<?>) it).name())
				.collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * Returns the names of the roles exposed by default or {@literal null} for all of them.
	 */
	@Nullable
	Set<String> getExposure() {
		return exposure;
	}

	/**
	 * Returns the names of the roles allowed to refer to other units by default or {@literal null} for all of them.
	 */
	@Nullable
	Set<String> getReferrers() {
		return referrers;
	}

	@Nullable
	ArchitecturalUnitResolver getUnitResolver() {
		return unitResolver;
	}

	/**
	 * Returns whether application code may refer to the application code of other units, independently of the exposure
	 * and referrers configured.
	 */
	boolean isDirectApplicationAccess() {
		return directApplicationAccess;
	}
}
