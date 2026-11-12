/*
 * Copyright 2021-2025 the original author or authors.
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

import static com.tngtech.archunit.base.DescribedPredicate.*;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.*;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static org.jmolecules.archunit.JMoleculesArchitectureRules.JMoleculesHexagonalArchitecture.*;
import static org.jmolecules.archunit.JMoleculesArchitectureRules.JMoleculesLayeredArchitecture.*;
import static org.jmolecules.archunit.JMoleculesArchitectureRules.JMoleculesOnionArchitecture.*;

import lombok.RequiredArgsConstructor;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jmolecules.architecture.hexagonal.Adapter;
import org.jmolecules.architecture.hexagonal.Application;
import org.jmolecules.architecture.hexagonal.Hexagon;
import org.jmolecules.architecture.hexagonal.Port;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.architecture.layered.ApplicationLayer;
import org.jmolecules.architecture.layered.DomainLayer;
import org.jmolecules.architecture.layered.InfrastructureLayer;
import org.jmolecules.architecture.layered.InterfaceLayer;
import org.jmolecules.architecture.onion.Onion;
import org.jmolecules.architecture.onion.classical.ApplicationServiceRing;
import org.jmolecules.architecture.onion.classical.DomainModelRing;
import org.jmolecules.architecture.onion.classical.DomainServiceRing;
import org.jmolecules.architecture.onion.simplified.ApplicationRing;
import org.jmolecules.architecture.onion.simplified.DomainRing;
import org.jmolecules.architecture.onion.simplified.InfrastructureRing;
import org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.Architectures;
import com.tngtech.archunit.library.Architectures.LayeredArchitecture;

/**
 * ArchUnit rules to verify architectures defined via JMolecules' annotations.
 *
 * @author Oliver Drotbohm
 * @since 0.5
 */
public class JMoleculesArchitectureRules {

	private static final String INTERFACE = "Interface";
	private static final String APPLICATION = "Application";
	private static final String DOMAIN = "Domain";
	private static final String INFRASTRUCTURE = "Infrastructure";

	private static final String ONION_CLASSICAL_DOMAIN_MODEL = "Domain model";
	private static final String ONION_CLASSICAL_DOMAIN_SERVICE = "Domain service";
	private static final String ONION_CLASSICAL_APPLICATION = "Application";
	private static final String ONION_CLASSICAL_INFRASTRUCTURE = "Infrastructure";

	private static final String ONION_SIMPLE_DOMAIN = "Domain";
	private static final String ONION_SIMPLE_APPLICATION = "Application";
	private static final String ONION_SIMPLE_INFRASTRUCTURE = "Infrastructure";

	private static final String HEXAGONAL_APPLICATION = "Application";
	private static final String HEXAGONAL_PORT = "Port";
	private static final String HEXAGONAL_PORT_UNQUALIFIED = "Port (unqualified)";
	private static final String HEXAGONAL_PRIMARY_PORT = "Primary port";
	private static final String HEXAGONAL_SECONDARY_PORT = "Secondary port";
	private static final String HEXAGONAL_ADAPTER = "Adapter";
	private static final String HEXAGONAL_ADAPTER_UNQUALIFIED = "Adapter (unqualified)";
	private static final String HEXAGONAL_PRIMARY_ADAPTER = "Primary adapter";
	private static final String HEXAGONAL_SECONDARY_ADAPTER = "Secondary adapter";

	// Unit annotations introduced in jMolecules 2.1, see HexagonAnnotation
	private static final boolean HEXAGON_PRESENT = ClassUtils.isPresent("org.jmolecules.architecture.hexagonal.Hexagon",
			JMoleculesArchitectureRules.class.getClassLoader());
	private static final boolean ONION_PRESENT = ClassUtils.isPresent("org.jmolecules.architecture.onion.Onion",
			JMoleculesArchitectureRules.class.getClassLoader());

	private static final String UNANNOTATED = "Unannotated";
	private static final String ANNOTATIONS = "Architecture annotations";

	/**
	 * ArchUnit {@link LayeredArchitecture} defined by considering JMolecules layer annotations allowing access of
	 * <em>all</em> layers below.
	 *
	 * @return will never be {@literal null}.
	 * @see InterfaceLayer
	 * @see ApplicationLayer
	 * @see DomainLayer
	 * @see InfrastructureLayer
	 */
	public static LayeredArchitecture ensureLayering() {
		return ensureLayering(LayeredVerificationOptions.defaults());
	}

	/**
	 * ArchUnit {@link LayeredArchitecture} defined by considering JMolecules layer annotations allowing access of
	 * <em>all</em> layers below.
	 *
	 * @param lookup must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @see InterfaceLayer
	 * @see ApplicationLayer
	 * @see DomainLayer
	 * @see InfrastructureLayer
	 * @since 0.22
	 * @deprecated since 0.34, in favor of {@link #ensureLayering(LayeredVerificationOptions)}.
	 */
	@Deprecated(since = "0.34", forRemoval = true)
	public static LayeredArchitecture ensureLayering(StereotypeLookup lookup) {

		LayeredVerificationOptions options = LayeredVerificationOptions.defaults().withStereotypeLookup(lookup);

		return ensureLayering(options);
	}

	/**
	 * ArchUnit {@link LayeredArchitecture} defined by considering JMolecules layer annotations allowing access of
	 * <em>all</em> layers below, using the given {@link LayeredVerificationOptions}.
	 *
	 * @param options must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @see InterfaceLayer
	 * @see ApplicationLayer
	 * @see DomainLayer
	 * @see InfrastructureLayer
	 * @since 0.34
	 */
	public static LayeredArchitecture ensureLayering(LayeredVerificationOptions options) {

		Assert.notNull(options, "LayeredVerificationOptions must not be null!");

		return layeredArchitecture(options)
				.whereLayer(INTERFACE).mayNotBeAccessedByAnyLayer()
				.whereLayer(APPLICATION).mayOnlyBeAccessedByLayers(INTERFACE)
				.whereLayer(DOMAIN).mayOnlyBeAccessedByLayers(APPLICATION, INTERFACE)
				.whereLayer(INFRASTRUCTURE).mayOnlyBeAccessedByLayers(DOMAIN, APPLICATION, INTERFACE);
	}

	/**
	 * ArchUnit {@link LayeredArchitecture} defined by considering JMolecules layer annotations allowing access to the
	 * next lower layer only.
	 *
	 * @return will never be {@literal null}.
	 * @see InterfaceLayer
	 * @see ApplicationLayer
	 * @see DomainLayer
	 * @see InfrastructureLayer
	 */
	public static LayeredArchitecture ensureLayeringStrict() {
		return ensureLayeringStrict(LayeredVerificationOptions.defaults());
	}

	/**
	 * ArchUnit {@link LayeredArchitecture} defined by considering JMolecules layer annotations allowing access to the
	 * next lower layer only.
	 *
	 * @param lookup must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @see InterfaceLayer
	 * @see ApplicationLayer
	 * @see DomainLayer
	 * @see InfrastructureLayer
	 * @since 0.22
	 * @deprecated since 0.34, in favor of {@link #ensureLayeringStrict(LayeredVerificationOptions)}.
	 */
	@Deprecated(since = "0.34", forRemoval = true)
	public static LayeredArchitecture ensureLayeringStrict(StereotypeLookup lookup) {

		LayeredVerificationOptions options = LayeredVerificationOptions.defaults().withStereotypeLookup(lookup);

		return ensureLayeringStrict(options);
	}

	/**
	 * ArchUnit {@link LayeredArchitecture} defined by considering JMolecules layer annotations allowing access to the
	 * next lower layer only, using the given {@link LayeredVerificationOptions}.
	 *
	 * @param options must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @see InterfaceLayer
	 * @see ApplicationLayer
	 * @see DomainLayer
	 * @see InfrastructureLayer
	 * @since 0.34
	 */
	public static LayeredArchitecture ensureLayeringStrict(LayeredVerificationOptions options) {

		Assert.notNull(options, "LayeredVerificationOptions must not be null!");

		return layeredArchitecture(options)
				.whereLayer(INTERFACE).mayNotBeAccessedByAnyLayer()
				.whereLayer(APPLICATION).mayOnlyBeAccessedByLayers(INTERFACE)
				.whereLayer(DOMAIN).mayOnlyBeAccessedByLayers(APPLICATION)
				.whereLayer(INFRASTRUCTURE).mayOnlyBeAccessedByLayers(DOMAIN);
	}

	/**
	 * ArchUnit {@link ArchRule} defining a simplified variant of the Onion Architecture.
	 *
	 * @return will never be {@literal null}.
	 * @see ApplicationRing
	 * @see DomainRing
	 * @see InfrastructureRing
	 * @see #ensureOnionSimple(OnionVerificationOptions)
	 */
	public static ArchRule ensureOnionSimple() {
		return ensureOnionSimple(OnionVerificationOptions.defaults());
	}

	/**
	 * ArchUnit {@link ArchRule} defining a simplified variant of the Onion Architecture.
	 *
	 * @param lookup must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @see ApplicationRing
	 * @see DomainRing
	 * @see InfrastructureRing
	 * @see #ensureOnionSimple(OnionVerificationOptions)
	 * @since 0.22
	 * @deprecated since 0.34, in favor of {@link #ensureOnionSimple(OnionVerificationOptions)}.
	 */
	@Deprecated(since = "0.34", forRemoval = true)
	public static ArchRule ensureOnionSimple(StereotypeLookup lookup) {

		OnionVerificationOptions options = OnionVerificationOptions.defaults().withStereotypeLookup(lookup);

		return ensureOnionSimple(options);
	}

	/**
	 * ArchUnit {@link ArchRule} defining a simplified variant of the Onion Architecture using the given
	 * {@link OnionVerificationOptions}.
	 * <p>
	 * {@link ArchitecturalUnit}s (packages annotated with an annotation meta-annotated with
	 * {@link org.jmolecules.stereotype.ArchitecturalUnit}, like {@link org.jmolecules.architecture.onion.Onion} or
	 * {@link org.jmolecules.ddd.annotation.Module}) are verified in isolation. References between them are only
	 * restricted for units using rings: by default, their infrastructure ring types, application ring types and types
	 * not assigned to a ring may refer to other units, and other units may refer to their application ring types, as
	 * well as their infrastructure ring types from infrastructure ring types or units not using rings. See
	 * {@link OnionVerificationOptions#withExposure(Onion.Exposure...)} and
	 * {@link OnionVerificationOptions#withReferrers(Onion.Referrer...)} to restrict that globally, and
	 * {@link Onion#exposes()} and {@link Onion#refersVia()} to do so for individual units.
	 *
	 * @param options must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @see ApplicationRing
	 * @see DomainRing
	 * @see InfrastructureRing
	 * @since 0.34
	 */
	public static ArchRule ensureOnionSimple(OnionVerificationOptions options) {

		Assert.notNull(options, "OnionVerificationOptions must not be null!");

		return CompositeArchRule.of(onionArchitectureSimple(options)

				.whereLayer(ONION_SIMPLE_INFRASTRUCTURE)
				.mayNotBeAccessedByAnyLayer()

				.whereLayer(ONION_SIMPLE_APPLICATION)
				.mayOnlyBeAccessedByLayers(ONION_SIMPLE_INFRASTRUCTURE)

				.whereLayer(ONION_SIMPLE_DOMAIN)
				.mayOnlyBeAccessedByLayers(ONION_SIMPLE_APPLICATION, ONION_SIMPLE_INFRASTRUCTURE))
				.and(onionUnitBoundaries(options, ONION_SIMPLE_ANNOTATIONS, InfrastructureRing.class, ApplicationRing.class,
						ONION_CLASSICAL_ANNOTATIONS));
	}

	/**
	 * ArchUnit {@link ArchRule} defining the classic Onion Architecture.
	 *
	 * @return will never be {@literal null}.
	 * @see ApplicationServiceRing
	 * @see DomainServiceRing
	 * @see DomainModelRing
	 * @see org.jmolecules.architecture.onion.classical.InfrastructureRing
	 * @see #ensureOnionClassical(OnionVerificationOptions)
	 */
	public static ArchRule ensureOnionClassical() {
		return ensureOnionClassical(OnionVerificationOptions.defaults());
	}

	/**
	 * ArchUnit {@link ArchRule} defining the classic Onion Architecture.
	 *
	 * @param lookup must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @see ApplicationServiceRing
	 * @see DomainServiceRing
	 * @see DomainModelRing
	 * @see org.jmolecules.architecture.onion.classical.InfrastructureRing
	 * @see #ensureOnionClassical(OnionVerificationOptions)
	 * @since 0.22
	 * @deprecated since 0.34, in favor of {@link #ensureOnionClassical(OnionVerificationOptions)}.
	 */
	@Deprecated(since = "0.34", forRemoval = true)
	public static ArchRule ensureOnionClassical(StereotypeLookup lookup) {

		OnionVerificationOptions options = OnionVerificationOptions.defaults().withStereotypeLookup(lookup);

		return ensureOnionClassical(options);
	}

	/**
	 * ArchUnit {@link ArchRule} defining the classic Onion Architecture using the given
	 * {@link OnionVerificationOptions}.
	 * <p>
	 * {@link ArchitecturalUnit}s (packages annotated with an annotation meta-annotated with
	 * {@link org.jmolecules.stereotype.ArchitecturalUnit}, like {@link org.jmolecules.architecture.onion.Onion} or
	 * {@link org.jmolecules.ddd.annotation.Module}) are verified in isolation. References between them are only
	 * restricted for units using rings: by default, their infrastructure ring types, application ring types and types
	 * not assigned to a ring may refer to other units, and other units may refer to their application ring types, as
	 * well as their infrastructure ring types from infrastructure ring types or units not using rings. See
	 * {@link OnionVerificationOptions#withExposure(Onion.Exposure...)} and
	 * {@link OnionVerificationOptions#withReferrers(Onion.Referrer...)} to restrict that globally, and
	 * {@link Onion#exposes()} and {@link Onion#refersVia()} to do so for individual units.
	 *
	 * @param options must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @see ApplicationServiceRing
	 * @see DomainServiceRing
	 * @see DomainModelRing
	 * @see org.jmolecules.architecture.onion.classical.InfrastructureRing
	 * @since 0.34
	 */
	public static ArchRule ensureOnionClassical(OnionVerificationOptions options) {

		Assert.notNull(options, "OnionVerificationOptions must not be null!");

		return CompositeArchRule.of(onionArchitecture(options)

				.whereLayer(ONION_CLASSICAL_INFRASTRUCTURE)
				.mayNotBeAccessedByAnyLayer()

				.whereLayer(ONION_CLASSICAL_APPLICATION)
				.mayOnlyBeAccessedByLayers(ONION_CLASSICAL_INFRASTRUCTURE)

				.whereLayer(ONION_CLASSICAL_DOMAIN_SERVICE)
				.mayOnlyBeAccessedByLayers(ONION_CLASSICAL_APPLICATION, ONION_CLASSICAL_INFRASTRUCTURE)

				.whereLayer(ONION_CLASSICAL_DOMAIN_MODEL)
				.mayOnlyBeAccessedByLayers(ONION_CLASSICAL_DOMAIN_SERVICE, ONION_CLASSICAL_APPLICATION,
						ONION_CLASSICAL_INFRASTRUCTURE))
				.and(onionUnitBoundaries(options, ONION_CLASSICAL_ANNOTATIONS,
						org.jmolecules.architecture.onion.classical.InfrastructureRing.class, ApplicationServiceRing.class,
						ONION_SIMPLE_ANNOTATIONS));
	}

	/**
	 * ArchUnit {@link ArchRule} for Hexagonal Architecture as defined
	 * <a href="https://alistair.cockburn.us/hexagonal-architecture/">here</a>.
	 *
	 * @see Application
	 * @see Adapter
	 * @see Port
	 * @see PrimaryAdapter
	 * @see PrimaryPort
	 * @see SecondaryAdapter
	 * @see SecondaryPort
	 * @see #ensureHexagonal(HexagonalVerificationOptions)
	 * @return will never be {@literal null}.
	 */
	public static ArchRule ensureHexagonal() {
		return ensureHexagonal(HexagonalVerificationOptions.defaults());
	}

	/**
	 * ArchUnit {@link ArchRule} for Hexagonal Architecture as defined
	 * <a href="https://alistair.cockburn.us/hexagonal-architecture/">here</a> and a given {@link VerificationDepth}.
	 *
	 * @see Application
	 * @see Adapter
	 * @see Port
	 * @see PrimaryAdapter
	 * @see PrimaryPort
	 * @see SecondaryAdapter
	 * @see SecondaryPort
	 * @see #ensureHexagonal(HexagonalVerificationOptions)
	 * @return will never be {@literal null}.
	 * @since 0.25
	 * @deprecated since 0.34, in favor of {@link #ensureHexagonal(HexagonalVerificationOptions)}.
	 */
	@Deprecated(since = "0.34", forRemoval = true)
	public static ArchRule ensureHexagonal(VerificationDepth depth) {

		HexagonalVerificationOptions options = HexagonalVerificationOptions.defaults().withDepth(depth);

		return ensureHexagonal(options);
	}

	/**
	 * ArchUnit {@link ArchRule} for Hexagonal Architecture as defined
	 * <a href="https://alistair.cockburn.us/hexagonal-architecture/">here</a> using the given {@link StereotypeLookup}.
	 *
	 * @see Application
	 * @see Adapter
	 * @see Port
	 * @see PrimaryAdapter
	 * @see PrimaryPort
	 * @see SecondaryAdapter
	 * @see SecondaryPort
	 * @see #ensureHexagonal(HexagonalVerificationOptions)
	 * @return will never be {@literal null}.
	 * @deprecated since 0.34, in favor of {@link #ensureHexagonal(HexagonalVerificationOptions)}.
	 */
	@Deprecated(since = "0.34", forRemoval = true)
	public static ArchRule ensureHexagonal(StereotypeLookup lookup) {

		HexagonalVerificationOptions options = HexagonalVerificationOptions.defaults()
				.withDepth(VerificationDepth.LENIENT)
				.withStereotypeLookup(lookup);

		return ensureHexagonal(options);
	}

	/**
	 * ArchUnit {@link ArchRule} for Hexagonal Architecture as defined
	 * <a href="https://alistair.cockburn.us/hexagonal-architecture/">here</a> using the given {@link VerificationDepth}
	 * and {@link StereotypeLookup}.
	 *
	 * @param depth must not be {@literal null}.
	 * @param lookup must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @see #ensureHexagonal(HexagonalVerificationOptions)
	 * @deprecated since 0.34, in favor of {@link #ensureHexagonal(HexagonalVerificationOptions)}.
	 */
	@Deprecated(since = "0.34", forRemoval = true)
	public static ArchRule ensureHexagonal(VerificationDepth depth, StereotypeLookup lookup) {

		HexagonalVerificationOptions options = HexagonalVerificationOptions.defaults()
				.withDepth(depth)
				.withStereotypeLookup(lookup);

		return ensureHexagonal(options);
	}

	/**
	 * ArchUnit {@link ArchRule} for Hexagonal Architecture as defined
	 * <a href="https://alistair.cockburn.us/hexagonal-architecture/">here</a> using the given
	 * {@link HexagonalVerificationOptions}.
	 * <p>
	 * {@link ArchitecturalUnit}s (packages annotated with an annotation meta-annotated with
	 * {@link org.jmolecules.stereotype.ArchitecturalUnit}, like {@link org.jmolecules.architecture.hexagonal.Hexagon} or
	 * {@link org.jmolecules.ddd.annotation.Module}) are verified in isolation. References between them are only
	 * restricted for units using hexagonal stereotypes: by default, their secondary adapters, application code and types
	 * without a hexagonal stereotype may refer to other units, and other units may refer to their application code and
	 * primary ports, as well as their primary adapters from secondary adapters or units not using hexagonal stereotypes.
	 * Secondary adapters may only refer to primary adapters of other units. See
	 * {@link HexagonalVerificationOptions#withExposure(Hexagon.Exposure...)} and
	 * {@link HexagonalVerificationOptions#withReferrers(Hexagon.Referrer...)} to restrict that globally, and
	 * {@link Hexagon#exposes()} and {@link Hexagon#refersVia()} to do so for individual units.
	 *
	 * @param options must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @see ArchitecturalUnits
	 * @since 0.34
	 */
	public static ArchRule ensureHexagonal(HexagonalVerificationOptions options) {

		Assert.notNull(options, "HexagonalVerificationOptions must not be null!");

		return CompositeArchRule.of(hexagonalLayers(options))
				.and(hexagonalUnitBoundaries(options));
	}


	private static ArchRule hexagonalLayers(HexagonalVerificationOptions options) {

		VerificationDepth depth = options.getDepth();

		return hexagonalArchitecture(options)

				// Adapters

				.whereLayer(HEXAGONAL_ADAPTER_UNQUALIFIED)
				.mayOnlyAccessLayers(
						depth.augmentPrimary(ANNOTATIONS, UNANNOTATED, HEXAGONAL_PORT, HEXAGONAL_PRIMARY_ADAPTER,
								HEXAGONAL_SECONDARY_ADAPTER))

				// Access from secondary adapters is restricted to other architectural units
				.whereLayer(HEXAGONAL_PRIMARY_ADAPTER)
				.mayOnlyBeAccessedByLayers(HEXAGONAL_ADAPTER_UNQUALIFIED, UNANNOTATED, HEXAGONAL_SECONDARY_ADAPTER)

				.whereLayer(HEXAGONAL_PRIMARY_ADAPTER)
				.mayOnlyAccessLayers(
						depth.augmentPrimary(ANNOTATIONS, UNANNOTATED, HEXAGONAL_PRIMARY_PORT, HEXAGONAL_PORT_UNQUALIFIED,
								HEXAGONAL_ADAPTER_UNQUALIFIED))

				.whereLayer(HEXAGONAL_SECONDARY_ADAPTER)
				.mayOnlyBeAccessedByLayers(HEXAGONAL_ADAPTER_UNQUALIFIED, UNANNOTATED)

				.whereLayer(HEXAGONAL_SECONDARY_ADAPTER)
				.mayOnlyAccessLayers(
						depth.augmentSecondary(ANNOTATIONS, UNANNOTATED, HEXAGONAL_SECONDARY_PORT,
								HEXAGONAL_PORT_UNQUALIFIED, HEXAGONAL_ADAPTER_UNQUALIFIED, HEXAGONAL_PRIMARY_ADAPTER))

				// Ports

				.whereLayer(HEXAGONAL_PRIMARY_PORT)
				.mayOnlyAccessLayers(
						depth.augmentPrimary(ANNOTATIONS, UNANNOTATED, HEXAGONAL_PORT_UNQUALIFIED,
								HEXAGONAL_SECONDARY_PORT))

				.whereLayer(HEXAGONAL_SECONDARY_PORT)
				.mayOnlyAccessLayers(depth.augmentSecondary(ANNOTATIONS, UNANNOTATED, HEXAGONAL_PORT_UNQUALIFIED))

				.whereLayer(HEXAGONAL_PORT_UNQUALIFIED)
				.mayOnlyAccessLayers(ANNOTATIONS, UNANNOTATED, HEXAGONAL_PORT, HEXAGONAL_PRIMARY_PORT, HEXAGONAL_SECONDARY_PORT)

				// Application

				.whereLayer(HEXAGONAL_APPLICATION)
				.mayOnlyAccessLayers(ANNOTATIONS, UNANNOTATED, HEXAGONAL_PRIMARY_PORT, HEXAGONAL_SECONDARY_PORT,
						HEXAGONAL_PORT_UNQUALIFIED);
	}

	private static LayeredArchitecture layeredArchitecture(LayeredVerificationOptions options) {

		StereotypeLookup lookup = options.getStereotypeLookup();

		LayeredArchitecture architecture = Architectures.layeredArchitecture()
				.consideringOnlyDependenciesInLayers()
				.withOptionalLayers(true)

				.layer(INFRASTRUCTURE)
				.definedBy(lookup.forAnnotation(InfrastructureLayer.class, LAYER_ANNOTATIONS))

				.layer(DOMAIN)
				.definedBy(lookup.forAnnotation(DomainLayer.class, LAYER_ANNOTATIONS))

				.layer(APPLICATION)
				.definedBy(lookup.forAnnotation(ApplicationLayer.class, LAYER_ANNOTATIONS))

				.layer(INTERFACE)
				.definedBy(lookup.forAnnotation(InterfaceLayer.class, LAYER_ANNOTATIONS));

		return scoped(architecture, options);
	}

	private static LayeredArchitecture onionArchitecture(OnionVerificationOptions options) {

		StereotypeLookup lookup = options.getStereotypeLookup();

		LayeredArchitecture architecture = Architectures.layeredArchitecture()
				.consideringOnlyDependenciesInLayers()
				.withOptionalLayers(true)

				.layer(ONION_CLASSICAL_INFRASTRUCTURE)
				.definedBy(lookup.forAnnotation(org.jmolecules.architecture.onion.classical.InfrastructureRing.class,
						ONION_CLASSICAL_ANNOTATIONS))

				.layer(ONION_CLASSICAL_APPLICATION)
				.definedBy(lookup.forAnnotation(ApplicationServiceRing.class, ONION_CLASSICAL_ANNOTATIONS))

				.layer(ONION_CLASSICAL_DOMAIN_SERVICE)
				.definedBy(lookup.forAnnotation(DomainServiceRing.class, ONION_CLASSICAL_ANNOTATIONS))

				.layer(ONION_CLASSICAL_DOMAIN_MODEL)
				.definedBy(lookup.forAnnotation(DomainModelRing.class, ONION_CLASSICAL_ANNOTATIONS));

		return scoped(architecture, options);
	}

	private static LayeredArchitecture onionArchitectureSimple(OnionVerificationOptions options) {

		StereotypeLookup lookup = options.getStereotypeLookup();

		LayeredArchitecture architecture = Architectures.layeredArchitecture()
				.consideringOnlyDependenciesInLayers()
				.withOptionalLayers(true)

				.layer(ONION_SIMPLE_INFRASTRUCTURE)
				.definedBy(lookup.forAnnotation(InfrastructureRing.class, ONION_SIMPLE_ANNOTATIONS))

				.layer(ONION_SIMPLE_APPLICATION)
				.definedBy(lookup.forAnnotation(ApplicationRing.class, ONION_SIMPLE_ANNOTATIONS))

				.layer(ONION_SIMPLE_DOMAIN)
				.definedBy(lookup.forAnnotation(DomainRing.class, ONION_SIMPLE_ANNOTATIONS));

		return scoped(architecture, options);
	}

	private static LayeredArchitecture hexagonalArchitecture(HexagonalVerificationOptions options) {

		StereotypeLookup lookup = options.getStereotypeLookup();

		String hexagonalPackage = Application.class.getPackage().getName();

		LayeredArchitecture architecture = Architectures.layeredArchitecture()
				.consideringOnlyDependenciesInLayers()
				.withOptionalLayers(true)

				.layer(ANNOTATIONS)
				.definedBy(resideInAPackage(hexagonalPackage))

				.layer(UNANNOTATED)
				.definedBy(not(lookup.hasAnnotationFromPackageOnItselfOrPackage(hexagonalPackage)))

				.layer(HEXAGONAL_APPLICATION)
				.definedBy(lookup.forAnnotation(Application.class, HEXAGONAL_ANNOTATIONS))

				.layer(HEXAGONAL_PORT)
				.definedBy(lookup.forAnnotation(Port.class, HEXAGONAL_ANNOTATIONS))

				.layer(HEXAGONAL_PORT_UNQUALIFIED)
				.definedBy(lookup.forAnnotation(Port.class, HEXAGONAL_ANNOTATIONS)
						.withExclusions(PrimaryPort.class, SecondaryPort.class))

				.layer(HEXAGONAL_PRIMARY_PORT)
				.definedBy(lookup.forAnnotation(PrimaryPort.class))

				.layer(HEXAGONAL_SECONDARY_PORT)
				.definedBy(lookup.forAnnotation(SecondaryPort.class))

				.layer(HEXAGONAL_ADAPTER)
				.definedBy(lookup.forAnnotation(Adapter.class, HEXAGONAL_ANNOTATIONS))

				.layer(HEXAGONAL_ADAPTER_UNQUALIFIED)
				.definedBy(lookup.forAnnotation(Adapter.class, HEXAGONAL_ANNOTATIONS)
						.withExclusions(PrimaryAdapter.class, SecondaryAdapter.class))

				.layer(HEXAGONAL_PRIMARY_ADAPTER)
				.definedBy(lookup.forAnnotation(PrimaryAdapter.class, HEXAGONAL_ANNOTATIONS)
						.withoutExclusion(Adapter.class))

				.layer(HEXAGONAL_SECONDARY_ADAPTER)
				.definedBy(lookup.forAnnotation(SecondaryAdapter.class, HEXAGONAL_ANNOTATIONS)
						.withoutExclusion(Adapter.class));

		return scoped(architecture, options);
	}

	/**
	 * Verifies the references between {@link ArchitecturalUnit}s for the Hexagonal Architecture, considering only units
	 * using hexagonal stereotypes. See {@link #ensureHexagonal(HexagonalVerificationOptions)} for details.
	 * Complements the layer definition in {@link #hexagonalLayers(HexagonalVerificationOptions)} that generally
	 * permits secondary adapters to access primary adapters, as {@link LayeredArchitecture} cannot express constraints
	 * between individual origin and target types.
	 *
	 * @param options must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	private static ArchRule hexagonalUnitBoundaries(HexagonalVerificationOptions options) {

		StereotypeLookup lookup = options.getStereotypeLookup();

		IsStereotype secondaryAdapter = lookup.forAnnotation(SecondaryAdapter.class, HEXAGONAL_ANNOTATIONS)
				.withoutExclusion(Adapter.class);
		IsStereotype primaryAdapter = lookup.forAnnotation(PrimaryAdapter.class, HEXAGONAL_ANNOTATIONS)
				.withoutExclusion(Adapter.class);
		IsStereotype application = lookup.forAnnotation(Application.class, HEXAGONAL_ANNOTATIONS);
		IsStereotype primaryPort = lookup.forAnnotation(PrimaryPort.class);

		Predicate<JavaClass> stereotyped = anyOf(lookup, HEXAGONAL_ANNOTATIONS);
		boolean declarable = HEXAGON_PRESENT;

		List<UnitRole> exposures = List.of(
				new UnitRole(declarable ? HexagonAnnotation.EXPOSES_PRIMARY_ADAPTERS : null, "primary adapters",
						primaryAdapter, true),
				new UnitRole(declarable ? HexagonAnnotation.EXPOSES_APPLICATION : null, "application code", application,
						false),
				new UnitRole(declarable ? HexagonAnnotation.EXPOSES_PRIMARY_PORTS : null, "primary ports", primaryPort,
						false));

		List<UnitRole> referrers = List.of(
				new UnitRole(declarable ? HexagonAnnotation.REFERS_VIA_SECONDARY_ADAPTERS : null, "secondary adapters",
						secondaryAdapter, false),
				new UnitRole(declarable ? HexagonAnnotation.REFERS_VIA_APPLICATION : null,
						"application code (including types without hexagonal stereotypes)", application.or(stereotyped.negate()),
						false));

		UnitRoles roles = new UnitRoles("hexagonal stereotypes", stereotyped, secondaryAdapter, "secondary adapters",
				primaryAdapter, "primary adapters", true, declarable ? HexagonAnnotation.NAME : null, null, exposures,
				referrers);

		return unitBoundaries(roles, options);
	}

	/**
	 * Verifies the references between {@link ArchitecturalUnit}s for the Onion Architecture, considering only units using
	 * rings of the given variant. See {@link #ensureOnionSimple(OnionVerificationOptions)} for details.
	 *
	 * @param options must not be {@literal null}.
	 * @param annotations the ring annotations of the Onion Architecture variant, must not be {@literal null}.
	 * @param infrastructure the infrastructure ring annotation of the variant, must not be {@literal null}.
	 * @param application the application ring annotation of the variant, must not be {@literal null}.
	 * @param otherVariant the ring annotations of the other variant, sharing the {@link Onion} unit annotation, must not
	 *          be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	private static ArchRule onionUnitBoundaries(OnionVerificationOptions options,
			Collection<Class<? extends Annotation>> annotations, Class<? extends Annotation> infrastructure,
			Class<? extends Annotation> application, Collection<Class<? extends Annotation>> otherVariant) {

		StereotypeLookup lookup = options.getStereotypeLookup();

		IsStereotype infrastructureRing = lookup.forAnnotation(infrastructure, annotations);
		IsStereotype applicationRing = lookup.forAnnotation(application, annotations);

		Predicate<JavaClass> stereotyped = anyOf(lookup, annotations);
		boolean declarable = ONION_PRESENT;

		List<UnitRole> exposures = List.of(
				new UnitRole(declarable ? OnionAnnotation.EXPOSES_INFRASTRUCTURE : null, "infrastructure ring types",
						infrastructureRing, true),
				new UnitRole(declarable ? OnionAnnotation.EXPOSES_APPLICATION : null, "application ring types",
						applicationRing, false));

		List<UnitRole> referrers = List.of(
				new UnitRole(declarable ? OnionAnnotation.REFERS_VIA_INFRASTRUCTURE : null, "infrastructure ring types",
						infrastructureRing, false),
				new UnitRole(declarable ? OnionAnnotation.REFERS_VIA_APPLICATION : null,
						"application ring types (including types not assigned to a ring)", applicationRing.or(stereotyped.negate()),
						false));

		UnitRoles roles = new UnitRoles("onion rings", stereotyped, infrastructureRing, "infrastructure ring types",
				infrastructureRing, "infrastructure ring types", false, declarable ? OnionAnnotation.NAME : null,
				anyOf(lookup, otherVariant), exposures, referrers);

		return unitBoundaries(roles, options);
	}

	private static ArchRule unitBoundaries(UnitRoles roles, UnitAwareVerificationOptions<?> options) {
		return classes().should(new UnitBoundaries(roles, options)).allowEmptyShould(true);
	}

	/**
	 * Restricts the given {@link LayeredArchitecture} to the scope configured in the given options by ignoring all
	 * dependencies from or to types outside of it.
	 */
	private static LayeredArchitecture scoped(LayeredArchitecture architecture, VerificationOptions<?> options) {

		DescribedPredicate<JavaClass> scope = options.getScope();

		if (scope == null) {
			return architecture;
		}

		return architecture
				.ignoreDependency(DescribedPredicate.<JavaClass> alwaysTrue(), not(scope))
				.ignoreDependency(not(scope), DescribedPredicate.<JavaClass> alwaysTrue());
	}

	private static Predicate<JavaClass> anyOf(StereotypeLookup lookup,
			Collection<Class<? extends Annotation>> annotations) {

		return annotations.stream()
				.<Predicate<JavaClass>> map(lookup::forAnnotation)
				.reduce(Predicate::or)
				.orElse(it -> false);
	}

	/**
	 * References to {@link Hexagon}, introduced in jMolecules 2.1. As the jMolecules architecture artifacts are optional
	 * dependencies that might be present in older versions, the class must only be used if {@link #HEXAGON_PRESENT}.
	 * They are {@link String}s so that callers don't refer to {@link Hexagon} types themselves, which the JVM would load
	 * when verifying them.
	 */
	private static class HexagonAnnotation {

		static final String NAME = Hexagon.class.getName();

		static final String EXPOSES_PRIMARY_ADAPTERS = Hexagon.Exposure.PRIMARY_ADAPTERS.name();
		static final String EXPOSES_PRIMARY_PORTS = Hexagon.Exposure.PRIMARY_PORTS.name();
		static final String EXPOSES_APPLICATION = Hexagon.Exposure.APPLICATION.name();

		static final String REFERS_VIA_SECONDARY_ADAPTERS = Hexagon.Referrer.SECONDARY_ADAPTERS.name();
		static final String REFERS_VIA_APPLICATION = Hexagon.Referrer.APPLICATION.name();
	}

	/**
	 * References to {@link Onion}, introduced in jMolecules 2.1. See {@link HexagonAnnotation} for details.
	 */
	private static class OnionAnnotation {

		static final String NAME = Onion.class.getName();

		static final String EXPOSES_INFRASTRUCTURE = Onion.Exposure.INFRASTRUCTURE.name();
		static final String EXPOSES_APPLICATION = Onion.Exposure.APPLICATION.name();

		static final String REFERS_VIA_INFRASTRUCTURE = Onion.Referrer.INFRASTRUCTURE.name();
		static final String REFERS_VIA_APPLICATION = Onion.Referrer.APPLICATION.name();
	}

	/**
	 * The roles of an architectural language's stereotypes for references between {@link ArchitecturalUnit}s.
	 *
	 * @author Oliver Drotbohm
	 */
	private static class UnitRoles {

		private static final String EXPOSES = "exposes", REFERS_VIA = "refersVia";

		private final String language;
		private final Predicate<JavaClass> stereotyped, outgoing, incoming;
		private final String outgoingName, incomingName;
		private final boolean outgoingToIncomingAcrossUnitsOnly;
		private final @Nullable String unitAnnotation;
		private final @Nullable Predicate<JavaClass> competing;
		private final List<UnitRole> exposures, referrers;

		/**
		 * @param language a human-readable name of the language's stereotypes.
		 * @param stereotyped whether a type is assigned any stereotype of the language.
		 * @param outgoing the types designated to refer to the incoming ones of other units (e.g. secondary adapters).
		 * @param incoming the types designated to be referred to by the outgoing ones of other units (e.g. primary
		 *          adapters).
		 * @param outgoingToIncomingAcrossUnitsOnly whether outgoing types may refer to incoming ones across units only.
		 * @param unitAnnotation the fully qualified name of the language's unit annotation, declaring the roles of a unit
		 *          in its {@code exposes} and {@code refersVia} attributes, or {@literal null} if the annotation is not
		 *          present. Units declared via the annotation are considered to use the language.
		 * @param competing the stereotypes of another language sharing the unit annotation (e.g. the other variant of the
		 *          Onion Architecture). Units declared via the unit annotation using those are not considered to use the
		 *          current language. Can be {@literal null}.
		 * @param exposures all roles a unit of the language can expose, in the order they are to be described in.
		 * @param referrers all roles of a unit of the language that can refer to other units, in the order they are to be
		 *          described in.
		 */
		UnitRoles(String language, Predicate<JavaClass> stereotyped, Predicate<JavaClass> outgoing, String outgoingName,
				Predicate<JavaClass> incoming, String incomingName, boolean outgoingToIncomingAcrossUnitsOnly,
				@Nullable String unitAnnotation, @Nullable Predicate<JavaClass> competing, List<UnitRole> exposures,
				List<UnitRole> referrers) {

			this.language = language;
			this.stereotyped = stereotyped;
			this.outgoing = outgoing;
			this.outgoingName = outgoingName;
			this.incoming = incoming;
			this.incomingName = incomingName;
			this.outgoingToIncomingAcrossUnitsOnly = outgoingToIncomingAcrossUnitsOnly;
			this.unitAnnotation = unitAnnotation;
			this.competing = competing;
			this.exposures = exposures;
			this.referrers = referrers;
		}

		/**
		 * Returns whether the given {@link ArchitecturalUnit} is declared via the language's unit annotation, either
		 * directly or via a custom annotation meta-annotated with it.
		 */
		boolean isDeclaredVia(ArchitecturalUnit unit) {

			JavaPackage pkg = unit.getPackage();

			return unitAnnotation != null
					&& (pkg.isAnnotatedWith(unitAnnotation) || pkg.isMetaAnnotatedWith(unitAnnotation));
		}

		/**
		 * Returns the roles exposed by default, i.e. the ones selected via the given keys, all of them if {@literal null}.
		 */
		List<UnitRole> getDefaultExposures(@Nullable Set<String> keys) {
			return UnitRole.select(exposures, keys);
		}

		/**
		 * Returns the roles allowed to refer to other units by default, i.e. the ones selected via the given keys, all of
		 * them if {@literal null}.
		 */
		List<UnitRole> getDefaultReferrers(@Nullable Set<String> keys) {
			return UnitRole.select(referrers, keys);
		}

		/**
		 * Returns the roles exposed by the given {@link ArchitecturalUnit} as declared in the {@code exposes} attribute of
		 * the language's unit annotation on the unit's package.
		 *
		 * @return the declared roles or {@literal null} if none declared.
		 */
		@Nullable
		List<UnitRole> getDeclaredExposures(ArchitecturalUnit unit) {
			return getDeclared(unit, EXPOSES, exposures);
		}

		/**
		 * Returns the roles of the given {@link ArchitecturalUnit} allowed to refer to other units as declared in the
		 * {@code refersVia} attribute of the language's unit annotation on the unit's package.
		 *
		 * @return the declared roles or {@literal null} if none declared.
		 */
		@Nullable
		List<UnitRole> getDeclaredReferrers(ArchitecturalUnit unit) {
			return getDeclared(unit, REFERS_VIA, referrers);
		}

		private @Nullable List<UnitRole> getDeclared(ArchitecturalUnit unit, String attribute, List<UnitRole> roles) {

			if (unitAnnotation == null) {
				return null;
			}

			Object value = unit.getPackage().tryGetAnnotationOfType(unitAnnotation)
					.flatMap(it -> it.get(attribute))
					.orElse(null);

			if (!(value instanceof Object[]) || ((Object[]) value).length == 0) {
				return null;
			}

			Set<String> declared = Arrays.stream((Object[]) value)
					.map(it -> it instanceof JavaEnumConstant ? ((JavaEnumConstant) it).name() : it.toString())
					.collect(Collectors.toSet());

			return UnitRole.select(roles, declared);
		}
	}

	/**
	 * A role of an architectural language whose types an {@link ArchitecturalUnit} can expose to other units or allow
	 * to refer to other units.
	 *
	 * @author Oliver Drotbohm
	 */
	private static class UnitRole {

		private final @Nullable String key;
		private final String name;
		private final Predicate<JavaClass> types;
		private final boolean designatedOriginsOnly;

		/**
		 * @param key the name of the constant representing the role in the corresponding attribute of the language's unit
		 *          annotation, {@literal null} if the annotation is not present.
		 * @param name a human-readable name of the role's types.
		 * @param types the types of the role.
		 * @param designatedOriginsOnly whether the types are only exposed to the types of other units designated to refer
		 *          to them (e.g. primary adapters to secondary adapters).
		 */
		UnitRole(@Nullable String key, String name, Predicate<JavaClass> types, boolean designatedOriginsOnly) {

			this.key = key;
			this.name = name;
			this.types = types;
			this.designatedOriginsOnly = designatedOriginsOnly;
		}

		boolean exposes(JavaClass type, boolean designatedOrigin) {
			return (designatedOrigin || !designatedOriginsOnly) && types.test(type);
		}

		static List<UnitRole> select(List<UnitRole> roles, @Nullable Set<String> keys) {

			return keys == null
					? roles
					: roles.stream().filter(it -> it.key != null && keys.contains(it.key)).collect(Collectors.toList());
		}

		/**
		 * Describes the given roles, qualifying the ones only exposed to designated origins with the given name of those.
		 *
		 * @param roles must not be {@literal null}.
		 * @param designatedOrigins the human-readable name of the types designated to refer to other units.
		 * @return e.g. "primary adapters (to secondary adapters), application code and primary ports", an empty
		 *         {@link String} if no roles are given.
		 */
		static String describe(List<UnitRole> roles, String designatedOrigins) {

			List<String> names = roles.stream()
					.map(it -> it.designatedOriginsOnly ? it.name + " (to " + designatedOrigins + ")" : it.name)
					.collect(Collectors.toList());

			return names.size() < 2
					? String.join("", names)
					: String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.get(names.size() - 1);
		}
	}

	/**
	 * An {@link ArchitecturalUnit} narrowing the types exposed by another one to the roles of an architectural language
	 * it exposes. Also captures the roles of the language allowed to refer to other units.
	 *
	 * @author Oliver Drotbohm
	 */
	private static class LanguageUnit implements ArchitecturalUnit {

		private final ArchitecturalUnit delegate;
		private final List<UnitRole> exposures, referrers;
		private final @Nullable LanguageUnit parent;

		LanguageUnit(ArchitecturalUnit delegate, List<UnitRole> exposures, List<UnitRole> referrers,
				@Nullable LanguageUnit parent) {

			this.delegate = delegate;
			this.exposures = exposures;
			this.referrers = referrers;
			this.parent = parent;
		}

		/*
		 * (non-Javadoc)
		 * @see org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit#getId()
		 */
		@Override
		public String getId() {
			return delegate.getId();
		}

		/*
		 * (non-Javadoc)
		 * @see org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit#getName()
		 */
		@Override
		public String getName() {
			return delegate.getName();
		}

		/*
		 * (non-Javadoc)
		 * @see org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit#getDescription()
		 */
		@Override
		public @Nullable String getDescription() {
			return delegate.getDescription();
		}

		/*
		 * (non-Javadoc)
		 * @see org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit#getPackage()
		 */
		@Override
		public JavaPackage getPackage() {
			return delegate.getPackage();
		}

		/*
		 * (non-Javadoc)
		 * @see org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit#getParent()
		 */
		@Override
		public @Nullable ArchitecturalUnit getParent() {
			return parent;
		}

		/*
		 * (non-Javadoc)
		 * @see org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit#contains(com.tngtech.archunit.core.domain.JavaClass)
		 */
		@Override
		public boolean contains(JavaClass type) {
			return delegate.contains(type);
		}

		/*
		 * (non-Javadoc)
		 * @see org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit#exposes(com.tngtech.archunit.core.domain.JavaClass)
		 */
		@Override
		public boolean exposes(JavaClass type) {
			return exposesTo(type, true);
		}

		/**
		 * Returns whether the given type is exposed to a type of another unit.
		 *
		 * @param type must not be {@literal null}.
		 * @param designatedOrigin whether the referring type is designated to refer to the unit's designated entry points.
		 */
		boolean exposesTo(JavaClass type, boolean designatedOrigin) {
			return delegate.exposes(type) && exposures.stream().anyMatch(it -> it.exposes(type, designatedOrigin));
		}

		/**
		 * Returns whether the given type of the unit may refer to other units.
		 *
		 * @param type must not be {@literal null}.
		 */
		boolean allowsReferencesFrom(JavaClass type) {
			return referrers.stream().anyMatch(it -> it.types.test(type));
		}

		String describeExposures(String designatedOrigins) {
			return UnitRole.describe(exposures, designatedOrigins);
		}

		String describeReferrers() {
			return UnitRole.describe(referrers, "");
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(@Nullable Object obj) {
			return ArchitecturalUnits.isEqual(this, obj);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return ArchitecturalUnits.hash(this);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return ArchitecturalUnits.describe(this);
		}
	}

	/**
	 * An {@link ArchCondition} to verify the references between types of different {@link ArchitecturalUnit}s. Only units
	 * that contain at least one type assigned a stereotype of the architectural language verified are considered: their
	 * outgoing references are restricted to the types allowed to refer to other units, their incoming ones to the types
	 * they expose. Those are the ones declared in the {@code refersVia} and {@code exposes} attributes of the language's
	 * unit annotation, or the defaults configured in the options, narrowed to the types exposed by the resolved
	 * {@link ArchitecturalUnit} itself. References from or to types residing outside of any unit are not considered.
	 *
	 * @author Oliver Drotbohm
	 */
	private static class UnitBoundaries extends ArchCondition<JavaClass> {

		private final UnitRoles roles;
		private final List<UnitRole> defaultExposures, defaultReferrers;

		private final @Nullable ArchitecturalUnitResolver customResolver;
		private final @Nullable Predicate<JavaClass> scope;

		private ArchitecturalUnitResolver units = new ArchitecturalUnits();
		private final Map<ArchitecturalUnit, LanguageUnit> languageUnits = new HashMap<>();
		private Set<ArchitecturalUnit> unitsUsingLanguage = Collections.emptySet();

		UnitBoundaries(UnitRoles roles, UnitAwareVerificationOptions<?> options) {

			super("");

			this.customResolver = options.getUnitResolver();
			this.scope = options.getScope();
			this.roles = roles;
			this.defaultExposures = roles.getDefaultExposures(options.getExposure());
			this.defaultReferrers = roles.getDefaultReferrers(options.getReferrers());
		}

		/*
		 * (non-Javadoc)
		 * @see com.tngtech.archunit.lang.ArchCondition#getDescription()
		 */
		@Override
		public String getDescription() {
			return String.format("only refer to architectural units using %s via the types allowed to (by default %s) and "
					+ "the types they expose (by default %s)", roles.language, UnitRole.describe(defaultReferrers, ""),
					UnitRole.describe(defaultExposures, roles.outgoingName));
		}

		/*
		 * (non-Javadoc)
		 * @see com.tngtech.archunit.lang.ArchCondition#init(java.util.Collection)
		 */
		@Override
		public void init(Collection<JavaClass> allObjectsToTest) {

			this.units = customResolver != null ? customResolver : new ArchitecturalUnits();
			this.languageUnits.clear();
			this.unitsUsingLanguage = detectUnitsUsingLanguage(allObjectsToTest);
		}

		/*
		 * (non-Javadoc)
		 * @see com.tngtech.archunit.lang.ArchCondition#check(java.lang.Object, com.tngtech.archunit.lang.ConditionEvents)
		 */
		@Override
		public void check(JavaClass type, ConditionEvents events) {

			if (!isInScope(type)) {
				return;
			}

			for (Dependency dependency : type.getDirectDependenciesFromSelf()) {

				JavaClass target = dependency.getTargetClass();

				if (target.equals(type) || !isInScope(target)) {
					continue;
				}

				String reason = verify(type, getUnitFor(type), target, getUnitFor(target));

				if (reason != null) {
					events.add(SimpleConditionEvent.violated(dependency,
							String.format("%s – %s.", dependency.getDescription(), reason)));
				}
			}
		}

		/**
		 * Detects the units using the language, i.e. the ones containing at least one type assigned a stereotype of it, or
		 * declared via the language's unit annotation unless they contain types assigned a stereotype of a competing
		 * language sharing that annotation.
		 */
		private Set<ArchitecturalUnit> detectUnitsUsingLanguage(Collection<JavaClass> types) {

			Set<ArchitecturalUnit> result = unitsOf(types, roles.stereotyped);
			Set<ArchitecturalUnit> competing = roles.competing == null
					? Collections.emptySet()
					: unitsOf(types, roles.competing);

			unitsOf(types, it -> true).stream()
					.filter(roles::isDeclaredVia)
					.filter(it -> !competing.contains(it))
					.forEach(result::add);

			return result;
		}

		private Set<ArchitecturalUnit> unitsOf(Collection<JavaClass> types, Predicate<? super JavaClass> filter) {

			return types.stream()
					.filter(filter)
					.<ArchitecturalUnit> map(this::getUnitFor)
					.filter(Objects::nonNull)
					.collect(Collectors.toCollection(HashSet::new));
		}

		private boolean isInScope(JavaClass type) {
			return scope == null || scope.test(type);
		}

		private @Nullable LanguageUnit getUnitFor(JavaClass type) {

			ArchitecturalUnit unit = units.getUnitFor(type);

			return unit == null ? null : toLanguageUnit(unit);
		}

		private LanguageUnit toLanguageUnit(ArchitecturalUnit unit) {

			LanguageUnit existing = languageUnits.get(unit);

			if (existing != null) {
				return existing;
			}

			ArchitecturalUnit parent = unit.getParent();
			List<UnitRole> exposures = roles.getDeclaredExposures(unit);
			List<UnitRole> referrers = roles.getDeclaredReferrers(unit);

			LanguageUnit result = new LanguageUnit(unit, exposures == null ? defaultExposures : exposures,
					referrers == null ? defaultReferrers : referrers, parent == null ? null : toLanguageUnit(parent));

			languageUnits.put(unit, result);

			return result;
		}

		/**
		 * Verifies the reference from the given origin to the given target.
		 *
		 * @return the reason for the violation or {@literal null} if the reference is valid.
		 */
		private @Nullable String verify(JavaClass origin, @Nullable LanguageUnit originUnit, JavaClass target,
				@Nullable LanguageUnit targetUnit) {

			if (originUnit == null || targetUnit == null || originUnit.equals(targetUnit)) {
				return roles.outgoingToIncomingAcrossUnitsOnly && roles.outgoing.test(origin) && roles.incoming.test(target)
						? describeWithinUnit(origin, originUnit, target, targetUnit)
						: null;
			}

			boolean originUsesLanguage = unitsUsingLanguage.contains(originUnit);

			// Outgoing references
			if (originUsesLanguage && !originUnit.allowsReferencesFrom(origin)) {

				return String.format("Type %s of architectural unit %s must not refer to type %s of architectural unit %s. Only %s may "
						+ "refer to other architectural units", origin.getSimpleName(), originUnit, target.getSimpleName(),
						targetUnit, originUnit.describeReferrers());
			}

			// Incoming references
			boolean designatedOrigin = !originUsesLanguage || roles.outgoing.test(origin);

			if (!unitsUsingLanguage.contains(targetUnit) || targetUnit.exposesTo(target, designatedOrigin)) {
				return null;
			}

			String exposed = targetUnit.describeExposures(roles.outgoingName);

			return exposed.isEmpty()
					? String.format("%s belongs to architectural unit %s, which does not expose any types",
							target.getSimpleName(), targetUnit)
					: String.format("%s belongs to architectural unit %s, which only exposes %s", target.getSimpleName(),
							targetUnit, exposed);
		}

		private String describeWithinUnit(JavaClass origin, @Nullable ArchitecturalUnit originUnit, JavaClass target,
				@Nullable ArchitecturalUnit targetUnit) {

			String reason;

			if (originUnit == null) {
				reason = String.format("%s is not part of any architectural unit", origin.getName());
			} else if (targetUnit == null) {
				reason = String.format("%s is not part of any architectural unit", target.getName());
			} else {
				reason = String.format("%s and %s both belong to architectural unit %s", origin.getSimpleName(),
						target.getSimpleName(), originUnit);
			}

			return reason + String.format(". %s may only refer to %s of other architectural units",
					StringUtils.capitalize(roles.outgoingName), roles.incomingName);
		}
	}

	/**
	 * A strategy how to look up stereotypes for the classes to be analyzed. Can either be on the types themselves
	 * including a package-level assignment (via {@link #defaultLookup()}) or via a marker type that acts as replacement
	 * for the package-level lookup.
	 *
	 * @author Oliver Drotbohm
	 * @since 0.22
	 */
	@RequiredArgsConstructor(staticName = "of")
	public static class StereotypeLookup {

		private static final String DEFAULT_DESCRIPTION = "(meta-)annotated with %s or residing in package (meta-)annotated with %s";
		private static final Function<JavaClass, Stream<JavaClass>> DEFAULT_MARKER_LOOKUP = it -> Stream.of(it);
		private static final StereotypeLookup DEFAULT_LOOKUP = new StereotypeLookup(DEFAULT_MARKER_LOOKUP, false);

		private final Function<JavaClass, Stream<JavaClass>> markerLookup;
		private final boolean traverseParentPackages;

		private StereotypeLookup(String name) {
			this(name, false);
		}

		private StereotypeLookup(String name, boolean traverseParentPackages) {

			this.markerLookup = type -> markerTypes(type.getPackage(), name);
			this.traverseParentPackages = traverseParentPackages;
		}

		/**
		 * Creates a default {@link StereotypeLookup}, which means it for each type, it will try to find the annotation on
		 * the type itself or any meta annotations and falls back to traversing the package annotations.
		 *
		 * @return will never be {@literal null}.
		 */
		public static StereotypeLookup defaultLookup() {
			return DEFAULT_LOOKUP;
		}

		/**
		 * Creates a {@link StereotypeLookup} trying to find the stereotype annotation on a marker type located in the
		 * reference type's package or parent package.
		 *
		 * @param name will never be {@literal null} or empty.
		 * @return will never be {@literal null}.
		 */
		public static StereotypeLookup onMarkerType(String name) {

			Assert.hasText(name, "Name must not be null or empty!");

			return new StereotypeLookup(name);
		}

		/**
		 * Creates a {@link StereotypeLookup} trying to find the stereotype annotation on marker types with the same simple
		 * name as the given one located in the reference type's package or parent package.
		 *
		 * @param type will never be {@literal null} or empty.
		 * @return will never be {@literal null}.
		 */
		public static StereotypeLookup onMarkerTypeName(Class<?> type) {
			return onMarkerType(type.getSimpleName());
		}

		/**
		 * Configures the {@link StereotypeLookup} to also look for package-based stereotypes in parent packages. Defaults
		 * to {@literal false} to be able to freely reassign stereotypes to types in nested packages.
		 *
		 * @return will never be {@literal null}.
		 * @since 0.30
		 */
		public StereotypeLookup withParentPackageTraversal() {
			return new StereotypeLookup(markerLookup, true);
		}

		IsStereotype forAnnotation(Class<? extends Annotation> annotations) {
			return forAnnotation(annotations, Collections.emptySet());
		}

		IsStereotype forAnnotation(Class<? extends Annotation> annotation,
				Collection<Class<? extends Annotation>> exclusions) {

			return new IsStereotype(annotation, markerLookup, allBut(exclusions, annotation), traverseParentPackages);
		}

		DescribedPredicate<JavaClass> hasAnnotationFromPackageOnItselfOrPackage(String name) {
			return hasAnnotationFromPackage(markerLookup, name);
		}

		Collection<Class<? extends Annotation>> allBut(Collection<Class<? extends Annotation>> source,
				Class<? extends Annotation> filter) {

			return source.stream()
					.filter(it -> !isEqualOrAnnotated(it, filter))
					.collect(Collectors.toSet());
		}

		boolean isEqualOrAnnotated(Class<? extends Annotation> candidate, Class<? extends Annotation> reference) {

			if (isJdkType(candidate)) {
				return false;
			}

			return candidate.equals(reference)
					|| candidate.getAnnotation(reference) != null
					|| Arrays.stream(candidate.getAnnotations()).anyMatch(it -> isEqualOrAnnotated(it.getClass(), reference));
		}

		private static boolean isJdkType(Class<?> type) {

			String pkg = type.getName();

			return pkg != null && Stream.of("java", "jdk", "com.sun").anyMatch(pkg::startsWith);
		}

		private static Stream<JavaClass> markerTypes(JavaPackage pkg, String name) {

			Stream<JavaClass> result = pkg.containsClassWithSimpleName(name)
					? Stream.of(pkg.getClassWithSimpleName(name))
					: Stream.empty();

			return pkg.getParent()
					.map(it -> Stream.concat(markerTypes(it, name), result))
					.orElse(result);
		}
	}

	/**
	 * Defines the depth of verification of Hexagonal Architectures, in particular the relationship between adapters and
	 * the application core.
	 *
	 * @author Oliver Drotbohm
	 * @since 0.25
	 */
	public enum VerificationDepth {

		/**
		 * Allows access to non-port application code from primary and secondary adapters and ports. Also, primary adapters
		 * are allowed to refer to secondary ports directly.
		 */
		LENIENT,

		/**
		 * Allows access to non-port application code from secondary but not primary adapters and ports.
		 */
		SEMI_STRICT,

		/**
		 * Does not allow access to non-port application code from any adapters.
		 */
		STRICT;

		String[] augmentPrimary(String... allowed) {

			switch (this) {
				case LENIENT:
					return augmentAccess(allowed);
				case SEMI_STRICT:
				default:
					return allowed;
			}
		}

		String[] augmentSecondary(String... allowed) {

			switch (this) {
				case LENIENT:
				case SEMI_STRICT:
					return augmentAccess(allowed);
				default:
					return allowed;
			}
		}

		private static String[] augmentAccess(String... abstractions) {

			List<String> result = new ArrayList<>(Arrays.asList(abstractions));
			result.add(HEXAGONAL_APPLICATION);

			if (result.contains(HEXAGONAL_PRIMARY_PORT)) {
				result.add(HEXAGONAL_SECONDARY_PORT);
			}

			return result.toArray(new String[result.size()]);
		}
	}

	/**
	 * A {@link DescribedPredicate} that tests whether a {@link JavaClass} is of a particular stereotype.
	 *
	 * @author Oliver Drotbohm
	 */
	static class IsStereotype extends DescribedPredicate<JavaClass> {

		private final Class<? extends Annotation> annotation;
		private final String description;
		private final Function<JavaClass, Stream<JavaClass>> markerLookup;
		private final BiPredicate<Class<? extends Annotation>, JavaClass> filter = (it,
				type) -> type.isMetaAnnotatedWith(it) || hasAnnotationOnPackageOrParent(type.getPackage());
		private final Collection<Class<? extends Annotation>> exclusions;
		private final boolean traverseParentPackages;

		IsStereotype(Class<? extends Annotation> annotation, Function<JavaClass, Stream<JavaClass>> markerLookup,
				Collection<Class<? extends Annotation>> exclusions, boolean traverseParentPackages) {

			super("");

			String description = String.format(StereotypeLookup.DEFAULT_DESCRIPTION, annotation.getName(),
					annotation.getName());

			if (!exclusions.isEmpty()) {
				description += String.format(", but not (meta-)annotated with %s.",
						exclusions.stream().map(Class::getName).collect(Collectors.joining(", ")));
			}

			this.annotation = annotation;
			this.description = description;
			this.exclusions = exclusions;
			this.markerLookup = it -> Stream.concat(Stream.of(it), markerLookup.apply(it));
			this.traverseParentPackages = traverseParentPackages;
		}

		/*
		 * (non-Javadoc)
		 * @see com.tngtech.archunit.base.DescribedPredicate#getDescription()
		 */
		@Override
		public String getDescription() {
			return description;
		}

		@SafeVarargs
		public final IsStereotype withExclusions(Class<? extends Annotation>... exclusions) {

			Collection<Class<? extends Annotation>> newExclusions = new HashSet<>(this.exclusions);
			newExclusions.addAll(Arrays.asList(exclusions));

			return new IsStereotype(annotation, markerLookup, newExclusions, traverseParentPackages);
		}

		public final IsStereotype withoutExclusion(Class<? extends Annotation> exclusion) {

			Collection<Class<? extends Annotation>> newExclusions = new HashSet<>(this.exclusions);
			newExclusions.remove(exclusion);

			return new IsStereotype(annotation, markerLookup, newExclusions, traverseParentPackages);
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.function.Predicate#test(java.lang.Object)
		 */
		@Override
		public boolean test(JavaClass type) {

			// Is not annotated with exclusion
			return !markerLookup.apply(type).anyMatch(it -> exclusions.stream().anyMatch(it::isMetaAnnotatedWith))

					// Matches what we're looking for
					&& markerLookup.apply(type).anyMatch(it -> filter.test(annotation, it));
		}

		private boolean hasAnnotationOnPackageOrParent(JavaPackage javaPackage) {

			if (exclusions.stream().anyMatch(javaPackage::isMetaAnnotatedWith)) {
				return false;
			}

			if (javaPackage.isMetaAnnotatedWith(annotation)) {
				return true;
			}

			return traverseParentPackages && javaPackage.getParent()
					.map(this::hasAnnotationOnPackageOrParent)
					.orElse(false);
		}
	}

	static class JMoleculesOnionArchitecture {

		static final Collection<Class<? extends Annotation>> ONION_SIMPLE_ANNOTATIONS = Arrays.asList(DomainRing.class,
				ApplicationRing.class, InfrastructureRing.class);

		static final Collection<Class<? extends Annotation>> ONION_CLASSICAL_ANNOTATIONS = Arrays.asList(
				DomainModelRing.class, DomainServiceRing.class, ApplicationServiceRing.class,
				org.jmolecules.architecture.onion.classical.InfrastructureRing.class);
	}

	static class JMoleculesLayeredArchitecture {

		static final Collection<Class<? extends Annotation>> LAYER_ANNOTATIONS = Arrays.asList(InfrastructureLayer.class,
				DomainLayer.class, ApplicationLayer.class, InterfaceLayer.class);
	}

	static class JMoleculesHexagonalArchitecture {

		static final List<Class<? extends Annotation>> HEXAGONAL_ANNOTATIONS = Arrays.asList(Application.class, Port.class,
				PrimaryPort.class, SecondaryPort.class, Adapter.class, PrimaryAdapter.class, SecondaryAdapter.class);
	}

	static DescribedPredicate<JavaClass> hasAnnotationFromPackage(Function<JavaClass, Stream<JavaClass>> markerLookup,
			String name) {

		return new DescribedPredicate<JavaClass>("has annotation from package %s", name) {

			private final Map<HasAnnotations<?>, Boolean> CACHE = new HashMap<>();

			/*
			 * (non-Javadoc)
			 * @see java.util.function.Predicate#test(java.lang.Object)
			 */
			@Override
			public boolean test(JavaClass t) {

				return markerLookup.apply(t)
						.flatMap(it -> Stream.of(it, it.getPackage()))
						.anyMatch(it -> hasAnnotationFromPackage(it, name));
			}

			private boolean hasAnnotationFromPackage(HasAnnotations<?> target, String name) {

				return CACHE.computeIfAbsent(target, it -> {
					return it.getAnnotations().stream()
							.map(JavaAnnotation::getType)
							.map(JavaType::getName)
							.anyMatch(candidate -> candidate.startsWith(name + "."));
				});
			}
		};
	}
}
