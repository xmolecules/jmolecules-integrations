/*
 * Copyright 2021-2024 the original author or authors.
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

import lombok.RequiredArgsConstructor;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jmolecules.architecture.hexagonal.Adapter;
import org.jmolecules.architecture.hexagonal.Application;
import org.jmolecules.architecture.hexagonal.Port;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.architecture.layered.ApplicationLayer;
import org.jmolecules.architecture.layered.DomainLayer;
import org.jmolecules.architecture.layered.InfrastructureLayer;
import org.jmolecules.architecture.layered.InterfaceLayer;
import org.jmolecules.architecture.onion.classical.ApplicationServiceRing;
import org.jmolecules.architecture.onion.classical.DomainModelRing;
import org.jmolecules.architecture.onion.classical.DomainServiceRing;
import org.jmolecules.architecture.onion.simplified.ApplicationRing;
import org.jmolecules.architecture.onion.simplified.DomainRing;
import org.jmolecules.architecture.onion.simplified.InfrastructureRing;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.lang.ArchRule;
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

	static List<Class<? extends Annotation>> LAYER_ANNOTATIONS = Arrays.asList(InfrastructureLayer.class,
			DomainLayer.class, ApplicationLayer.class, InterfaceLayer.class);

	static List<Class<? extends Annotation>> ONION_SIMPLE_ANNOTATIONS = Arrays.asList(DomainRing.class,
			ApplicationRing.class, InfrastructureRing.class);

	static List<Class<? extends Annotation>> ONION_CLASSICAL_ANNOTATIONS = Arrays.asList(DomainModelRing.class,
			DomainServiceRing.class, ApplicationServiceRing.class,
			org.jmolecules.architecture.onion.classical.InfrastructureRing.class);

	static List<Class<? extends Annotation>> HEXAGONAL_ANNOTATIONS = Arrays.asList(Application.class, Port.class,
			PrimaryPort.class, SecondaryPort.class, Adapter.class, PrimaryAdapter.class, SecondaryAdapter.class);

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
		return ensureLayering(StereotypeLookup.DEFAULT_LOOKUP);
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
	 */
	public static LayeredArchitecture ensureLayering(StereotypeLookup lookup) {

		return layeredArchitecture(lookup)
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
		return ensureLayeringStrict(StereotypeLookup.defaultLookup());
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
	 */
	public static LayeredArchitecture ensureLayeringStrict(StereotypeLookup lookup) {

		return layeredArchitecture(lookup)
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
	 */
	public static ArchRule ensureOnionSimple() {
		return ensureOnionSimple(StereotypeLookup.defaultLookup());
	}

	/**
	 * ArchUnit {@link ArchRule} defining a simplified variant of the Onion Architecture.
	 *
	 * @param lookup must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @see ApplicationRing
	 * @see DomainRing
	 * @see InfrastructureRing
	 * @since 0.22
	 */
	public static ArchRule ensureOnionSimple(StereotypeLookup lookup) {

		return onionArchitectureSimple(lookup)

				.whereLayer(ONION_SIMPLE_INFRASTRUCTURE)
				.mayNotBeAccessedByAnyLayer()

				.whereLayer(ONION_SIMPLE_APPLICATION)
				.mayOnlyBeAccessedByLayers(ONION_SIMPLE_INFRASTRUCTURE)

				.whereLayer(ONION_SIMPLE_DOMAIN)
				.mayOnlyBeAccessedByLayers(ONION_SIMPLE_APPLICATION, ONION_SIMPLE_INFRASTRUCTURE);
	}

	/**
	 * ArchUnit {@link ArchRule} defining the classic Onion Architecture.
	 *
	 * @return will never be {@literal null}.
	 * @see ApplicationServiceRing
	 * @see DomainServiceRing
	 * @see DomainModelRing
	 * @see org.jmolecules.architecture.onion.classical.InfrastructureRing
	 */
	public static ArchRule ensureOnionClassical() {
		return ensureOnionClassical(StereotypeLookup.defaultLookup());
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
	 * @since 0.22
	 */
	public static ArchRule ensureOnionClassical(StereotypeLookup lookup) {

		return onionArchitecture(lookup)

				.whereLayer(ONION_CLASSICAL_INFRASTRUCTURE)
				.mayNotBeAccessedByAnyLayer()

				.whereLayer(ONION_CLASSICAL_APPLICATION)
				.mayOnlyBeAccessedByLayers(ONION_CLASSICAL_INFRASTRUCTURE)

				.whereLayer(ONION_CLASSICAL_DOMAIN_SERVICE)
				.mayOnlyBeAccessedByLayers(ONION_CLASSICAL_APPLICATION, ONION_CLASSICAL_INFRASTRUCTURE)

				.whereLayer(ONION_CLASSICAL_DOMAIN_MODEL)
				.mayOnlyBeAccessedByLayers(ONION_CLASSICAL_DOMAIN_SERVICE, ONION_CLASSICAL_APPLICATION,
						ONION_CLASSICAL_INFRASTRUCTURE);
	}

	/**
	 * ArchUnit {@link ArchRule} defining Hexagonal Architecture.
	 *
	 * @see Adapter
	 * @see Port
	 * @see PrimaryAdapter
	 * @see PrimaryPort
	 * @see SecondaryAdapter
	 * @see SecondaryPort
	 * @return will never be {@literal null}.
	 */
	public static ArchRule ensureHexagonal() {

		return hexagonalArchitecture(StereotypeLookup.defaultLookup())

				.whereLayer(HEXAGONAL_PRIMARY_PORT)
				.mayOnlyBeAccessedByLayers(APPLICATION, HEXAGONAL_PORT_UNQUALIFIED, HEXAGONAL_ADAPTER_UNQUALIFIED,
						HEXAGONAL_PRIMARY_ADAPTER)

				.whereLayer(HEXAGONAL_SECONDARY_PORT)
				.mayOnlyBeAccessedByLayers(APPLICATION, HEXAGONAL_PORT_UNQUALIFIED, HEXAGONAL_ADAPTER_UNQUALIFIED,
						HEXAGONAL_SECONDARY_ADAPTER)

				.whereLayer(HEXAGONAL_PORT)
				.mayOnlyBeAccessedByLayers(APPLICATION, HEXAGONAL_ADAPTER)

				.whereLayer(HEXAGONAL_ADAPTER_UNQUALIFIED)
				.mayOnlyBeAccessedByLayers(HEXAGONAL_PRIMARY_ADAPTER, HEXAGONAL_SECONDARY_ADAPTER)

				.whereLayer(HEXAGONAL_PRIMARY_ADAPTER)
				.mayOnlyBeAccessedByLayers(HEXAGONAL_ADAPTER_UNQUALIFIED)

				.whereLayer(HEXAGONAL_SECONDARY_ADAPTER)
				.mayOnlyBeAccessedByLayers(HEXAGONAL_ADAPTER_UNQUALIFIED)

				.whereLayer(APPLICATION)
				.mayNotBeAccessedByAnyLayer();
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
		private static StereotypeLookup DEFAULT_LOOKUP = new StereotypeLookup(DEFAULT_DESCRIPTION, null) {

			@Override
			IsStereotype forAnnotation(Class<? extends Annotation> annotation) {
				return new IsStereotype(annotation);
			}

			@Override
			IsStereotype forAnnotation(Class<? extends Annotation> annotation,
					Collection<Class<? extends Annotation>> exclusions) {

				return new IsStereotype(annotation, allBut(exclusions, annotation));
			}
		};

		private final String description;
		private final @Nullable BiPredicate<Class<? extends Annotation>, JavaClass> lookup;

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

			BiPredicate<Class<? extends Annotation>, JavaClass> lookup = (annotation,
					type) -> new IsStereotype(annotation).test(type)
							|| containsAnnotatedMarkerType(type.getPackage(), name, annotation);

			return new StereotypeLookup(String.format("Annotated marker type %s", name), lookup);
		}

		/**
		 * Creates a {@link StereotypeLookup} trying to find the stereotype annotation on marker types with the same simple
		 * name as the given one located in the reference type's package or parent package.
		 *
		 * @param name will never be {@literal null} or empty.
		 * @return will never be {@literal null}.
		 */
		public static StereotypeLookup onMarkerTypeName(Class<?> type) {
			return onMarkerType(type.getSimpleName());
		}

		/**
		 * Returns whether the given package contains a marker type with the given simple name annotated with the given
		 * annotation type. If none is found, we traverse the given package's parent packages.
		 *
		 * @param pkg must not be {@literal null}.
		 * @param name must not be {@literal null} or empty.
		 * @param annotationType must not be {@literal null}.
		 */
		private static boolean containsAnnotatedMarkerType(JavaPackage pkg, String name,
				Class<? extends Annotation> annotationType) {

			return pkg.getClasses().stream()
					.filter(it -> it.getSimpleName().equals(name))
					.anyMatch(candidate -> candidate.isMetaAnnotatedWith(annotationType))
					|| pkg.getParent()
							.map(it -> containsAnnotatedMarkerType(it, name, annotationType))
							.orElse(false);
		}

		IsStereotype forAnnotation(Class<? extends Annotation> annotations) {
			return forAnnotation(annotations, Collections.emptySet());
		}

		IsStereotype forAnnotation(Class<? extends Annotation> annotation,
				Collection<Class<? extends Annotation>> exclusions) {

			return new IsStereotype(annotation, lookup, description, allBut(exclusions, annotation));
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

			Package pkg = type.getPackage();

			return pkg != null && Stream.of("java", "jdk").anyMatch(pkg.getName()::startsWith);
		}
	}

	private static LayeredArchitecture layeredArchitecture(StereotypeLookup lookup) {

		return Architectures.layeredArchitecture()
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
	}

	private static LayeredArchitecture onionArchitecture(StereotypeLookup lookup) {

		return Architectures.layeredArchitecture()
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
	}

	private static LayeredArchitecture onionArchitectureSimple(StereotypeLookup lookup) {

		return Architectures.layeredArchitecture()
				.consideringOnlyDependenciesInLayers()
				.withOptionalLayers(true)

				.layer(ONION_SIMPLE_INFRASTRUCTURE)
				.definedBy(lookup.forAnnotation(InfrastructureRing.class, ONION_SIMPLE_ANNOTATIONS))

				.layer(ONION_SIMPLE_APPLICATION)
				.definedBy(lookup.forAnnotation(ApplicationRing.class, ONION_SIMPLE_ANNOTATIONS))

				.layer(ONION_SIMPLE_DOMAIN)
				.definedBy(lookup.forAnnotation(DomainRing.class, ONION_SIMPLE_ANNOTATIONS));
	}

	private static LayeredArchitecture hexagonalArchitecture(StereotypeLookup lookup) {

		return Architectures.layeredArchitecture()
				.consideringOnlyDependenciesInLayers()
				.withOptionalLayers(true)

				.layer(HEXAGONAL_APPLICATION)
				.definedBy(lookup.forAnnotation(Application.class, HEXAGONAL_ANNOTATIONS))

				.layer(HEXAGONAL_PORT)
				.definedBy(lookup.forAnnotation(Port.class, HEXAGONAL_ANNOTATIONS))

				.layer(HEXAGONAL_PORT_UNQUALIFIED)
				.definedBy(lookup.forAnnotation(Port.class, HEXAGONAL_ANNOTATIONS)
						.withExclusions(PrimaryPort.class, SecondaryPort.class))

				.layer(HEXAGONAL_PRIMARY_PORT)
				.definedBy(lookup.forAnnotation(PrimaryPort.class, HEXAGONAL_ANNOTATIONS))

				.layer(HEXAGONAL_SECONDARY_PORT)
				.definedBy(lookup.forAnnotation(SecondaryPort.class, HEXAGONAL_ANNOTATIONS))

				.layer(HEXAGONAL_ADAPTER)
				.definedBy(lookup.forAnnotation(Adapter.class, HEXAGONAL_ANNOTATIONS))

				.layer(HEXAGONAL_ADAPTER_UNQUALIFIED)
				.definedBy(lookup.forAnnotation(Adapter.class, HEXAGONAL_ANNOTATIONS)
						.withExclusions(PrimaryAdapter.class, SecondaryAdapter.class))

				.layer(HEXAGONAL_PRIMARY_ADAPTER)
				.definedBy(lookup.forAnnotation(PrimaryAdapter.class, HEXAGONAL_ANNOTATIONS))

				.layer(HEXAGONAL_SECONDARY_ADAPTER)
				.definedBy(lookup.forAnnotation(SecondaryAdapter.class, HEXAGONAL_ANNOTATIONS));
	}

	static class IsStereotype extends DescribedPredicate<JavaClass> {

		private final Class<? extends Annotation> annotation;
		private final String description;
		private final BiPredicate<Class<? extends Annotation>, JavaClass> filter;
		private final Collection<Class<? extends Annotation>> exclusions;

		public IsStereotype(Class<? extends Annotation> annotation) {
			this(annotation, Collections.emptySet());
		}

		public IsStereotype(Class<? extends Annotation> annotation,
				Collection<Class<? extends Annotation>> exclusions) {

			this(annotation, null,
					String.format(StereotypeLookup.DEFAULT_DESCRIPTION, annotation.getName(), annotation.getName()), exclusions);
		}

		public IsStereotype(Class<? extends Annotation> annotation,
				@Nullable BiPredicate<Class<? extends Annotation>, JavaClass> filter,
				String description, Collection<Class<? extends Annotation>> exclusions) {

			super(description);

			this.annotation = annotation;
			this.description = description;
			this.exclusions = exclusions;

			this.filter = filter != null
					? filter
					: (it, type) -> type.isMetaAnnotatedWith(it) || hasAnnotationOnPackageOrParent(type.getPackage());
		}

		@SafeVarargs
		public final IsStereotype withExclusions(Class<? extends Annotation>... exclusions) {

			Collection<Class<? extends Annotation>> newExclusions = new HashSet<>(this.exclusions);
			newExclusions.addAll(Arrays.asList(exclusions));

			return new IsStereotype(annotation, filter, description, newExclusions);
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.function.Predicate#test(java.lang.Object)
		 */
		@Override
		public boolean test(JavaClass type) {

			return !exclusions.stream().anyMatch(type::isMetaAnnotatedWith)
					&& filter.test(annotation, type);
		}

		private boolean hasAnnotationOnPackageOrParent(JavaPackage javaPackage) {

			if (exclusions.stream().anyMatch(javaPackage::isMetaAnnotatedWith)) {
				return false;
			}

			if (javaPackage.isMetaAnnotatedWith(annotation)) {
				return true;
			}

			return javaPackage.getParent()
					.map(this::hasAnnotationOnPackageOrParent)
					.orElse(false);
		}
	}
}
