/*
 * Copyright 2020-2024 the original author or authors.
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

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.*;
import static org.assertj.core.api.Assertions.*;
import static org.jmolecules.archunit.TestUtils.*;

import lombok.Value;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Entity;
import org.jmolecules.ddd.types.Identifier;
import org.jmolecules.ddd.types.ValueObject;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.EvaluationResult;

/**
 * Unit tests for DDD rules.
 *
 * @author Oliver Drotbohm
 * @author Torsten Juergeleit
 * @author Hasan Kara
 */
@AnalyzeClasses(packages = "org.jmolecules.archunit")
class JMoleculesDddRulesUnitTest {

	SampleAggregate reference;

	@ArchTest
	void detectsViolations(JavaClasses classes) {

		EvaluationResult result = JMoleculesDddRules.all().evaluate(classes);

		assertThat(result.getFailureReport().getDetails())
				.satisfiesExactlyInAnyOrder( //
						violation(SampleAggregate.class, "invalid", OtherEntity.class, OtherAggregate.class),
						violation(SampleAggregate.class, "invalidAggregate", OtherAggregate.class, Association.class), //
						violation(SampleAggregate.class, "invalidAggregateInCollection", Collection.class, Association.class), //
						violation(SampleAggregate.class, "invalidAggregateInMap", Map.class, Association.class), //
						violation(SampleAggregate.class, "invalidAnnotatedAggregate", AnnotatedAggregate.class, Association.class), //
						violation(SampleAggregate.class, "invalidInCollection", Collection.class, OtherAggregate.class), //
						violation(SampleAggregate.class, "invalidInMap", Map.class, OtherAggregate.class), //
						violation("Type.*%s.*%s.*", AnnotatedAggregate.class.getSimpleName(), Identity.class.getName()),
						violation(SampleValueObject.class, "entity", SampleEntity.class, null),
						violation(SampleValueObject.class, "annotatedEntity", AnnotatedEntity.class, null),
						violation(SampleValueObject.class, "aggregate", SampleAggregate.class, null),
						violation(SampleValueObject.class, "annotatedAggregate", AnnotatedAggregate.class, null),
						violation(SampleGrandChildEntity.class, "otherEntity", OtherEntity.class, OtherAggregate.class), // GH-222
						violation(OtherAnnotatedAggregate.class, "invalidAnnotatedAggregate", AnnotatedAggregate.class, null), //
						violation(OtherAnnotatedAggregate.class, "invalidAnnotatedAggregateInCollection", Collection.class,
								Association.class), //
						violation(OtherAnnotatedAggregate.class, "invalidAnnotatedAggregateInMap", Map.class, Association.class) //
				);
	}

	@ArchTest // GH-301
	void doesNotRejectSyntheticOwnerFieldsOfNonStaticInnerClasses(JavaClasses classes) {

		EvaluationResult result = JMoleculesDddRules.valueObjectsMustNotReferToIdentifiables()
				.evaluate(classes.that(simpleName("MyInnerClass")));

		assertThat(result.hasViolation()).isFalse();
	}

	static class SampleIdentifier implements Identifier {}

	static abstract class SampleAggregate implements AggregateRoot<SampleAggregate, SampleIdentifier> {

		SampleEntity valid;

		OtherEntity invalid;
		Collection<OtherEntity> invalidInCollection;
		Map<String, OtherEntity> invalidInMap;

		OtherAggregate invalidAggregate;
		Collection<OtherAggregate> invalidAggregateInCollection;
		Map<String, OtherAggregate> invalidAggregateInMap;

		AnnotatedAggregate invalidAnnotatedAggregate;

		Association<OtherAggregate, SampleIdentifier> association;

		AnnotatedEntity validAnnotatedEntity;
	}

	static abstract class SampleEntity implements Entity<SampleAggregate, SampleIdentifier> {
		List<SampleChildEntity> childEntities;
	}

	static abstract class SampleChildEntity implements Entity<SampleAggregate, SampleIdentifier> {
		SampleGrandChildEntity grandChildEntity;
	}

	static abstract class SampleGrandChildEntity implements Entity<SampleAggregate, SampleIdentifier> {
		OtherEntity otherEntity;
	}

	static abstract class OtherAggregate implements AggregateRoot<OtherAggregate, SampleIdentifier> {}

	static abstract class OtherEntity implements Entity<OtherAggregate, SampleIdentifier> {}

	@org.jmolecules.ddd.annotation.AggregateRoot
	static class AnnotatedAggregate {}

	@org.jmolecules.ddd.annotation.Entity
	interface AnnotatedEntity {
		@Identity
		Long getId();
	}

	static class SampleValueObject implements ValueObject {

		SampleEntity entity;
		AnnotatedEntity annotatedEntity;

		SampleAggregate aggregate;
		AnnotatedAggregate annotatedAggregate;
	}

	@org.jmolecules.ddd.annotation.AggregateRoot
	static class OtherAnnotatedAggregate {

		@org.jmolecules.ddd.annotation.Identity Long id;
		AnnotatedAggregate invalidAnnotatedAggregate;
		Collection<AnnotatedAggregate> invalidAnnotatedAggregateInCollection;
		Map<String, AnnotatedAggregate> invalidAnnotatedAggregateInMap;
	}

	@org.jmolecules.ddd.annotation.AggregateRoot
	static class ThirdAnnotatedAggregate {

		@org.jmolecules.ddd.annotation.Identity Long id;
		AnnotatedEntity valid;
	}

	// GH-301

	@org.jmolecules.ddd.annotation.AggregateRoot
	static class MyAggregateRoot {

		@Identity UUID id;
		MyInnerClass myInnerClass;

		@Value
		class MyInnerClass implements ValueObject {
			String param;
		}
	}
}
