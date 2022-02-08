/*
 * Copyright 2021 the original author or authors.
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

import java.lang.annotation.Annotation;

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

		return layeredArchitecture()
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

		return layeredArchitecture()
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

		return onionArchitectureSimple()

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

		return onionArchitecture()

				.whereLayer(ONION_CLASSICAL_INFRASTRUCTURE)
				.mayNotBeAccessedByAnyLayer()

				.whereLayer(ONION_CLASSICAL_APPLICATION)
				.mayOnlyBeAccessedByLayers(ONION_CLASSICAL_INFRASTRUCTURE)

				.whereLayer(ONION_CLASSICAL_DOMAIN_SERVICE)
				.mayOnlyBeAccessedByLayers(ONION_CLASSICAL_APPLICATION, ONION_CLASSICAL_INFRASTRUCTURE)

				.whereLayer(ONION_CLASSICAL_DOMAIN_MODEL)
				.mayOnlyBeAccessedByLayers(ONION_CLASSICAL_DOMAIN_SERVICE, ONION_CLASSICAL_DOMAIN_SERVICE,
						ONION_CLASSICAL_APPLICATION, ONION_CLASSICAL_INFRASTRUCTURE);
	}

	private static LayeredArchitecture layeredArchitecture() {

		return Architectures.layeredArchitecture()
				.withOptionalLayers(true)
				.layer(INFRASTRUCTURE).definedBy(layerType(InfrastructureLayer.class))
				.layer(DOMAIN).definedBy(layerType(DomainLayer.class))
				.layer(APPLICATION).definedBy(layerType(ApplicationLayer.class))
				.layer(INTERFACE).definedBy(layerType(InterfaceLayer.class));
	}

	private static LayeredArchitecture onionArchitecture() {

		return Architectures.layeredArchitecture()
				.withOptionalLayers(true)
				.layer(ONION_CLASSICAL_INFRASTRUCTURE)
				.definedBy(layerType(org.jmolecules.architecture.onion.classical.InfrastructureRing.class))
				.layer(ONION_CLASSICAL_APPLICATION).definedBy(layerType(ApplicationServiceRing.class))
				.layer(ONION_CLASSICAL_DOMAIN_SERVICE).definedBy(layerType(DomainServiceRing.class))
				.layer(ONION_CLASSICAL_DOMAIN_MODEL).definedBy(layerType(DomainModelRing.class));
	}

	private static LayeredArchitecture onionArchitectureSimple() {

		return Architectures.layeredArchitecture()
				.layer(ONION_SIMPLE_INFRASTRUCTURE).definedBy(layerType(InfrastructureRing.class))
				.layer(ONION_SIMPLE_APPLICATION).definedBy(layerType(ApplicationRing.class))
				.layer(ONION_SIMPLE_DOMAIN).definedBy(layerType(DomainRing.class));
	}

	private static DescribedPredicate<JavaClass> layerType(Class<? extends Annotation> annotation) {
		return new IsLayerType(annotation);
	}

	private static class IsLayerType extends DescribedPredicate<JavaClass> {

		private final Class<? extends Annotation> annotation;

		public IsLayerType(Class<? extends Annotation> annotation) {

			super("(meta-) annotated with %s or residing in package (meta-)annotated with %s", //
					annotation.getTypeName(), annotation.getTypeName());

			this.annotation = annotation;
		}

		/*
		 * (non-Javadoc)
		 * @see com.tngtech.archunit.base.Predicate#apply(java.lang.Object)
		 */
		@Override
		public boolean apply(JavaClass input) {

			return input.isAnnotatedWith(annotation)
					|| input.isMetaAnnotatedWith(annotation)
					|| hasAnnotationOnPackageOrParent(input.getPackage());
		}

		private boolean hasAnnotationOnPackageOrParent(JavaPackage javaPackage) {

			if (javaPackage.isAnnotatedWith(annotation)
					|| javaPackage.isMetaAnnotatedWith(annotation)) {
				return true;
			}

			return javaPackage.getParent()
					.map(this::hasAnnotationOnPackageOrParent)
					.orElse(false);
		}
	}
}
