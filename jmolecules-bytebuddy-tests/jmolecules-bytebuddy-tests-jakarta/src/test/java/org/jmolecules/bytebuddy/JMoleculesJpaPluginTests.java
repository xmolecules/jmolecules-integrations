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
package org.jmolecules.bytebuddy;

import static org.assertj.core.api.Assertions.*;

import example.*;
import jakarta.persistence.*;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.stream.Stream;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author Oliver Drotbohm
 */
class JMoleculesJpaPluginTests {

	@Test
	void defaultsAggregateType() throws Exception {

		// Defaults @Entity
		assertThat(SampleAggregate.class.getDeclaredAnnotations())
				.filteredOn(it -> it.annotationType().equals(Entity.class))
				.hasSize(1);

		// Adds default constructor
		assertThat(SampleEntity.class.getDeclaredConstructors()).isNotNull();
		assertThat(SampleEntity.class.getDeclaredField("id").getAnnotations())
				.filteredOn(it -> it.annotationType().equals(EmbeddedId.class))
				.hasSize(1);

		// Defaults aggregate identifier to @EmbeddedId…
		assertThat(SampleAggregate.class.getDeclaredField("id").getAnnotations())
				.filteredOn(it -> it.annotationType().equals(EmbeddedId.class))
				.hasSize(1);
		// … but no other Identifiers
		assertThat(SampleAggregate.class.getDeclaredField("otherId").getAnnotations())
				.filteredOn(it -> it.annotationType().equals(EmbeddedId.class))
				.isEmpty();

		// Defaults OneToOne annotation
		assertRelationshipDefaults(SampleAggregate.class.getDeclaredField("entity").getAnnotation(OneToOne.class));

		// Defaults collection of entities to @OneToMany(cascade = CascadeType.ALL)
		var listOfEntity = SampleAggregate.class.getDeclaredField("listOfEntity");

		assertRelationshipDefaults(listOfEntity.getAnnotation(OneToMany.class), false);
		assertThat(listOfEntity.getAnnotation(JoinColumn.class)).isNotNull();

		// Assert FetchMode configured for lazy to-many mapping.
		assertThat(listOfEntity.getAnnotation(Fetch.class))
				.extracting(Fetch::value)
				.isEqualTo(FetchMode.SUBSELECT);
	}

	@Test
	void defaultsForEntity() throws Exception {

		// Defaults @Entity
		assertThat(SampleEntity.class.getDeclaredAnnotations())
				.filteredOn(it -> it.annotationType().equals(Entity.class))
				.hasSize(1);

		// Adds default constructor
		assertThat(SampleEntity.class.getDeclaredConstructors()).isNotNull();

		// Defaults OneToOne annotation
		assertRelationshipDefaults(SampleEntity.class.getDeclaredField("nestedEntity").getAnnotation(OneToOne.class));

		// Defaults collection of entities to @OneToMany(cascade = CascadeType.ALL)
		assertRelationshipDefaults(
				SampleEntity.class.getDeclaredField("nestedEntities").getAnnotation(OneToMany.class));

		// Defaults collections of value objects to @ElementCollection(fetch = FetchType.EAGER
		var annotation = SampleEntity.class.getDeclaredField("valueObjects").getAnnotation(ElementCollection.class);

		assertThat(annotation)
				.isNotNull()
				.satisfies(it -> assertThat(it.fetch()).isEqualTo(FetchType.EAGER));
	}

	@Test
	void defaultsIdentifier() {

		assertThat(Serializable.class).isAssignableFrom(SampleAggregateIdentifier.class);
		assertThat(SampleAggregateIdentifier.class.getAnnotations())
				.filteredOn(it -> it.annotationType().equals(Embeddable.class))
				.hasSize(1);
	}

	@Test // #12
	void doesNotAnnotateMappedSuperclassWithAtEntity() {
		assertDoesNotHaveAnnotation(SampleMappedSuperclass.class, Entity.class);
	}

	@Test // #13
	void annotatesSubclassesWithAtEntity() {
		assertHasAnnotation(SampleSubclass.class, Entity.class);
	}

	@Test // #14
	void annotatesAbstractEntityWithAtMappedSuperclass() {
		assertHasAnnotation(SampleBaseClass.class, MappedSuperclass.class);
	}

	@Test // #14
	void doesNotGenerateNullabilityCheckingMethodForAbstractClasses() throws Exception {

		var method = ReflectionUtils.findMethod(SampleMappedSuperclass.class,
				JMoleculesJpaPlugin.NULLABILITY_METHOD_NAME);

		assertThat(method).isNull();
	}

	@Test // #15
	void doesNotAnnotateAlreadyAnnotatedRelationship() throws Exception {

		var annotatedListOfEntity = SampleJpaAggregate.class.getDeclaredField("annotatedListOfEntity");

		assertThat(annotatedListOfEntity.getAnnotation(OneToMany.class)).isNull();
		assertThat(annotatedListOfEntity.getAnnotation(JoinColumn.class)).isNull();

		var annotatedEntity = SampleJpaAggregate.class.getDeclaredField("annotatedEntity");

		assertThat(annotatedEntity.getAnnotation(OneToOne.class)).isNull();
		assertThat(annotatedEntity.getAnnotation(JoinColumn.class)).isNull();
	}

	@Test // #76
	void defaultsJpaMappingsForValueObjects() {

		assertThat(AnnotatedElementUtils.hasAnnotation(SampleValueObject.class, Embeddable.class)).isTrue();
		assertThat(SampleValueObject.class.getDeclaredConstructors()).hasSize(2);
	}

	@Test // #92
	void defaultsMappingsForAnnotatedModel() throws Exception {

		assertThat(SampleAnnotatedEntity.class.getDeclaredField("id").getAnnotation(Id.class)).isNotNull();

		var aggregate = SampleAnnotatedAggregate.class;

		assertThat(aggregate.getDeclaredField("id").getAnnotation(Id.class)).isNotNull();

		assertRelationshipDefaults(aggregate.getDeclaredField("entity").getAnnotation(OneToOne.class));
		assertRelationshipDefaults(aggregate.getDeclaredField("entities").getAnnotation(OneToMany.class));
	}

	@Test // GH-190
	void doesNotProcessCglibProxies() {
		assertDoesNotHaveAnnotation(Sample$$Cglib$$Proxy.class, Entity.class);
	}

	private static void assertDoesNotHaveAnnotation(Class<?> type, Class<? extends Annotation> expected) {

		Stream<Class<?>> annotationTypes = Arrays.stream(type.getAnnotations())
				.map(Annotation::annotationType);

		assertThat(annotationTypes).doesNotContain(expected);
	}

	private static void assertHasAnnotation(Class<?> type, Class<? extends Annotation> expected) {

		Stream<Class<?>> annotationTypes = Arrays.stream(type.getAnnotations())
				.map(Annotation::annotationType);

		assertThat(annotationTypes).contains(expected);
	}

	private static void assertRelationshipDefaults(Annotation annotation) {
		assertRelationshipDefaults(annotation, true);
	}

	private static void assertRelationshipDefaults(Annotation annotation, boolean eager) {

		assertThat(annotation)
				.isNotNull()
				.satisfies(
						it -> assertThat(AnnotationUtils.getValue(it, "fetch"))
								.isEqualTo(eager ? FetchType.EAGER : FetchType.LAZY))
				.satisfies(
						it -> assertThat((CascadeType[]) AnnotationUtils.getValue(it, "cascade"))
								.containsExactly(CascadeType.ALL));
	}
}
