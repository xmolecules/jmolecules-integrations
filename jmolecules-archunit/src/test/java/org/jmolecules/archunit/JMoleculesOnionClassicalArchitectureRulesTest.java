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

import org.jmolecules.archunit.onion.classical.AppServiceByAnnotation;
import org.jmolecules.archunit.onion.classical.DomainServiceByAnnotation;
import org.jmolecules.archunit.onion.classical.InfraByAnnotation;
import org.jmolecules.archunit.onion.classical.OnionClassical;
import org.jmolecules.archunit.onion.classical.app.AppType;
import org.jmolecules.archunit.onion.classical.domainmodel.DomainModelType;
import org.jmolecules.archunit.onion.classical.domainservice.DomainServiceType;
import org.jmolecules.archunit.onion.classical.infra.InfraType;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.EvaluationResult;

/**
 * Unit tests for Onion Architecture classic rules.
 *
 * @author Oliver Drotbohm
 */
@AnalyzeClasses(packagesOf = OnionClassical.class)
class JMoleculesOnionClassicalArchitectureRulesTest {

	@ArchTest
	void ensureOnionClasical(JavaClasses types) {

		EvaluationResult result = JMoleculesArchitectureRules.ensureOnionClassical().evaluate(types);

		assertThat(result.getFailureReport().getDetails()).satisfiesExactlyInAnyOrder( //
				violation(AppType.class, InfraType.class), //
				violation(AppType.class, InfraByAnnotation.class), //
				violation(DomainServiceType.class, AppType.class), //
				violation(DomainServiceType.class, AppServiceByAnnotation.class), //
				violation(DomainServiceType.class, InfraType.class), //
				violation(DomainServiceType.class, InfraByAnnotation.class), //
				violation(DomainModelType.class, AppType.class), //
				violation(DomainModelType.class, AppServiceByAnnotation.class), //
				violation(DomainModelType.class, InfraType.class), //
				violation(DomainModelType.class, InfraByAnnotation.class), //
				violation(DomainModelType.class, DomainServiceType.class), //
				violation(DomainModelType.class, DomainServiceByAnnotation.class) //
		);
	}
}
