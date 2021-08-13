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

import static org.assertj.core.api.Assertions.*;
import static org.jmolecules.archunit.TestUtils.*;

import org.jmolecules.archunit.layered.AppByAnnotation;
import org.jmolecules.archunit.layered.DomainByAnnotation;
import org.jmolecules.archunit.layered.InfraByAnnotation;
import org.jmolecules.archunit.layered.Layered;
import org.jmolecules.archunit.layered.UiByAnnotation;
import org.jmolecules.archunit.layered.app.AppType;
import org.jmolecules.archunit.layered.domain.DomainType;
import org.jmolecules.archunit.layered.infra.InfraType;
import org.jmolecules.archunit.layered.ui.UiType;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.EvaluationResult;

/**
 * Unit tests for Layered Architecture rules.
 *
 * @author Oliver Drotbohm
 */
@AnalyzeClasses(packagesOf = Layered.class)
class JMoleculesLayeredRulesTest {

	@ArchTest
	void ensureLayering(JavaClasses types) {

		EvaluationResult result = JMoleculesArchitectureRules.ensureLayering().evaluate(types);

		assertThat(result.getFailureReport().getDetails()).satisfiesExactlyInAnyOrder( //
				violation(AppType.class, UiType.class), //
				violation(AppType.class, UiByAnnotation.class), //
				violation(DomainType.class, AppType.class), //
				violation(DomainType.class, AppByAnnotation.class), //
				violation(DomainType.class, UiType.class), //
				violation(DomainType.class, UiByAnnotation.class), //
				violation(InfraType.class, DomainType.class), //
				violation(InfraType.class, DomainByAnnotation.class), //
				violation(InfraType.class, AppType.class), //
				violation(InfraType.class, AppByAnnotation.class), //
				violation(InfraType.class, UiType.class), //
				violation(InfraType.class, UiByAnnotation.class) //
		);
	}

	@ArchTest
	void ensureStrictLayering(JavaClasses types) {

		EvaluationResult result = JMoleculesArchitectureRules.ensureLayeringStrict().evaluate(types);

		assertThat(result.getFailureReport().getDetails()).satisfiesExactlyInAnyOrder(
				violation(AppType.class, InfraType.class), //
				violation(AppType.class, InfraByAnnotation.class), //
				violation(AppType.class, UiType.class), //
				violation(AppType.class, UiByAnnotation.class), //
				violation(DomainType.class, AppType.class), //
				violation(DomainType.class, AppByAnnotation.class), //
				violation(DomainType.class, UiType.class), //
				violation(DomainType.class, UiByAnnotation.class), //
				violation(InfraType.class, DomainType.class), //
				violation(InfraType.class, DomainByAnnotation.class), //
				violation(InfraType.class, AppType.class), //
				violation(InfraType.class, AppByAnnotation.class), //
				violation(InfraType.class, UiType.class), //
				violation(InfraType.class, UiByAnnotation.class), //
				violation(UiType.class, DomainType.class), //
				violation(UiType.class, DomainByAnnotation.class), //
				violation(UiType.class, InfraType.class), //
				violation(UiType.class, InfraByAnnotation.class) //
		);
	}
}
