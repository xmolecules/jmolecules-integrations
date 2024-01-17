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

import static org.assertj.core.api.Assertions.*;
import static org.jmolecules.archunit.TestUtils.*;

import org.jmolecules.archunit.onion.simple.AppByAnnotation;
import org.jmolecules.archunit.onion.simple.InfraByAnnotation;
import org.jmolecules.archunit.onion.simple.OnionSimple;
import org.jmolecules.archunit.onion.simple.app.AppType;
import org.jmolecules.archunit.onion.simple.domain.DomainType;
import org.jmolecules.archunit.onion.simple.infra.InfraType;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.EvaluationResult;

/**
 * Unit tests for Onion Architecture simplified rules.
 *
 * @author Oliver Drotbohm
 */
@AnalyzeClasses(packagesOf = OnionSimple.class)
class JMoleculesOnionSimpleArchitectureRulesTest {

	@ArchTest
	void ensureOnionSimple(JavaClasses types) {

		EvaluationResult result = JMoleculesArchitectureRules.ensureOnionSimple().evaluate(types);

		assertThat(result.getFailureReport().getDetails()).satisfiesExactlyInAnyOrder( //
				violation(AppType.class, InfraType.class), //
				violation(AppType.class, InfraByAnnotation.class), //
				violation(DomainType.class, AppType.class), //
				violation(DomainType.class, AppByAnnotation.class), //
				violation(DomainType.class, InfraType.class), //
				violation(DomainType.class, InfraByAnnotation.class) //
		);
	}
}
