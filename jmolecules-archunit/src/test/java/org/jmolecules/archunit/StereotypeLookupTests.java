/*
 * Copyright 2023-2025 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.util.stream.Stream;

import org.jmolecules.architecture.layered.ApplicationLayer;
import org.jmolecules.architecture.layered.InfrastructureLayer;
import org.jmolecules.archunit.JMoleculesArchitectureRules.IsStereotype;
import org.jmolecules.archunit.JMoleculesArchitectureRules.StereotypeLookup;
import org.jmolecules.archunit.markered.app.AppLayerType;
import org.jmolecules.archunit.markered.app.JMolecules;
import org.jmolecules.archunit.markered.app.nested.AnnotatedNestedAppLayerType;
import org.jmolecules.archunit.markered.app.nested.NestedAppLayerType;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

/**
 * Unit tests for {@link org.jmolecules.archunit.JMoleculesArchitectureRules.StereotypeLookup}.
 *
 * @author Oliver Drotbohm
 * @since 0.22
 */
@AnalyzeClasses(packagesOf = AppLayerType.class)
class StereotypeLookupTests {

	@ArchTest
	void discoveresTypeByMarkerType(JavaClasses classes) {

		Stream.of(
				StereotypeLookup.onMarkerType("JMolecules"),
				StereotypeLookup.onMarkerTypeName(JMolecules.class))
				.forEach(lookup -> {

					IsStereotype forAnnotation = lookup.forAnnotation(ApplicationLayer.class,
							JMoleculesArchitectureRules.JMoleculesLayeredArchitecture.LAYER_ANNOTATIONS);

					assertThat(forAnnotation.test(classes.get(AppLayerType.class))).isTrue();
					assertThat(forAnnotation.test(classes.get(NestedAppLayerType.class))).isTrue();

					// Directly annotated, nested
					JavaClass type = classes.get(AnnotatedNestedAppLayerType.class);

					assertThat(forAnnotation.test(type)).isFalse();
					assertThat(lookup.forAnnotation(InfrastructureLayer.class).test(type)).isTrue();
				});
	}
}
