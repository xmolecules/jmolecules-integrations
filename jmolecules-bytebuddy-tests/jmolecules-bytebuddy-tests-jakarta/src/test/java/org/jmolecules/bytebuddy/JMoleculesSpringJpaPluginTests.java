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
package org.jmolecules.bytebuddy;

import static org.assertj.core.api.Assertions.*;

import example.SampleAggregate;
import example.SampleAggregateIdentifier;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;

import org.hibernate.annotations.EmbeddableInstantiator;
import org.jmolecules.hibernate.RecordInstantiator;
import org.jmolecules.spring.jpa.JakartaPersistenceAssociationAttributeConverter;
import org.junit.jupiter.api.Test;
import org.springframework.core.ResolvableType;
import org.springframework.util.ReflectionUtils;

/**
 * Tests for {@link JMoleculesSpringJpaPlugin}.
 *
 * @author Oliver Drotbohm
 */
class JMoleculesSpringJpaPluginTests {

	@Test // #27, #36, #71
	void registersAssociationAttributeConverter() {

		var field = ReflectionUtils.findField(SampleAggregate.class, "association");
		var annotation = field.getDeclaredAnnotation(Convert.class);

		var converterType = annotation.converter();

		assertThat(converterType.getPackage().getName())
				.isEqualTo(SampleAggregate.class.getPackage().getName());
		assertThat(JakartaPersistenceAssociationAttributeConverter.class).isAssignableFrom(converterType);

		var boundGenerics = ResolvableType.forClass(converterType)
				.as(JakartaPersistenceAssociationAttributeConverter.class);

		assertThat(boundGenerics.getGeneric(0).getRawClass()).isEqualTo(SampleAggregate.class);
		assertThat(boundGenerics.getGeneric(1).getRawClass()).isEqualTo(SampleAggregateIdentifier.class);
		assertThat(boundGenerics.getGeneric(2).getRawClass()).isEqualTo(String.class);
	}

	@Test // #98
	void registersEmbeddableInstantiatorForRecord() {

		assertThat(example.SampleRecord.class).hasAnnotations(Embeddable.class, EmbeddableInstantiator.class);

		var annotation = example.SampleRecord.class.getAnnotation(EmbeddableInstantiator.class);

		assertThat(RecordInstantiator.class).isAssignableFrom(annotation.value());
	}
}
