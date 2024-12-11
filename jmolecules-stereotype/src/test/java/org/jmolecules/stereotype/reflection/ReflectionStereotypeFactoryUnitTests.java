/*
 * Copyright 2024-2025 the original author or authors.
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
package org.jmolecules.stereotype.reflection;

import static org.assertj.core.api.Assertions.*;

import example.MyController;
import example.application.DescribedStereotype;
import example.application.SomePrimaryPort;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;
import org.jmolecules.stereotype.api.Stereotype;
import org.jmolecules.stereotype.api.Stereotypes;
import org.jmolecules.stereotype.catalog.support.CatalogSource;
import org.jmolecules.stereotype.catalog.support.JsonPathStereotypeCatalog;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Oliver Drotbohm
 */
class ReflectionStereotypeFactoryUnitTests {

	JsonPathStereotypeCatalog catalog = new JsonPathStereotypeCatalog(
			CatalogSource.ofClassLoader(ReflectionStereotypeFactoryUnitTests.class.getClassLoader()));
	ReflectionStereotypeFactory factory = new ReflectionStereotypeFactory(catalog).enableLocalStereotypeDetection();

	@Test
	void detectsInterfaceBasedStereotype() {

		var metadata = factory.fromType(MyClass.class);

		assertThat(metadata).isNotNull();
		assertThat(metadata.getPrimary()).isNotNull();
		assertThat(metadata.getPrimary().getDisplayName()).isEqualTo("My Stereotype");
	}

	@Test
	void detectsAnnotationBasedStereotype() {

		var metadata = factory.fromType(MyAnnotatedClass.class).getPrimary();

		assertThat(metadata).isNotNull();
		assertThat(metadata.getDisplayName()).isEqualTo("My Stereotype Annotation");
	}

	@Test
	void detectsAllAnnotationBasedStereotypes() {

		Stereotypes metadata = factory.fromType(MyAnnotatedAggregateRoot.class);

		assertThat(metadata).hasSize(2)
				.extracting(Stereotype::getIdentifier)
				.containsExactly(
						"ddd.AggregateRoot",
						"ddd.Entity");
	}

	@Test
	void detectsAllInterfaceBasedStereotypes() {

		Stereotypes metadata = factory.fromType(MyAggregateRoot.class);

		assertThat(metadata).hasSize(3)
				.extracting(Stereotype::getIdentifier)
				.containsExactly(
						"ddd.AggregateRoot",
						"ddd.Entity",
						"ddd.Identifiable");
	}

	@Test
	void detectsMetaStereotypes() {

		Stereotypes stereotypes = factory.fromType(AggregateRoot.class);

		assertThat(stereotypes)
				.hasSize(1)
				.extracting(Stereotype::getIdentifier)
				.containsExactly(
						"ddd.Entity");
	}

	@Test
	void detectsPackageStereotype() {

		var pkg = SomePrimaryPort.class.getPackage();

		assertThat(factory.fromPackage(pkg))
				.hasSize(1)
				.extracting(Stereotype::getIdentifier)
				.containsExactly("architecture.hexagonal.Application");

		assertThat(factory.fromType(SomePrimaryPort.class))
				.hasSize(2)
				.extracting(Stereotype::getIdentifier)
				.containsExactly(
						"ddd.ValueObject",
						"architecture.hexagonal.Application");

	}

	@Test
	@Disabled
	void detectsDeclaredStereotypes() {

		var stereotypes = factory.fromType(DescribedStereotype.class);

		stereotypes.stream().map(catalog::getGroupsFor).forEach(System.out::println);

		var fromType = factory.fromType(MyController.class);

		fromType.stream().forEach(System.out::println);
		fromType.stream().map(catalog::getGroupsFor).forEach(System.out::println);
	}

	@Test
	void doesNotDetectNonInheritedStereotypeOnImplementation() {

		System.out.println(catalog);

		var stereotypes = factory.fromType(MyPortImplementation.class);

		assertThat(stereotypes)
				.extracting(Stereotype::getIdentifier)
				.doesNotContain("architecture.hexagonal.PrimaryPort");
	}

	@org.jmolecules.stereotype.Stereotype
	interface MyStereotype {}

	@org.jmolecules.stereotype.Stereotype
	@Retention(RetentionPolicy.RUNTIME)
	@interface MyStereotypeAnnotation {}

	static class MyClass implements MyStereotype {}

	@MyStereotypeAnnotation
	static class MyAnnotatedClass {}

	@AggregateRoot
	static class MyAnnotatedAggregateRoot {}

	static class MyAggregateRoot
			implements org.jmolecules.ddd.types.AggregateRoot<MyAggregateRoot, MyAggregateIdentifier> {

		@Override
		public MyAggregateIdentifier getId() {
			return null;
		}
	}

	static class MyAggregateIdentifier implements Identifier {}

	static class MyDescribedStereotype implements DescribedStereotype {}

	@PrimaryPort
	interface MyPort {

	}

	static class MyPortImplementation implements MyPort {}
}
