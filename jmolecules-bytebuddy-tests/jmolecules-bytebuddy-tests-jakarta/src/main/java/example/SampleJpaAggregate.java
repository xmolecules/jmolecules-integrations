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
package example;

import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

import java.util.Collection;

import org.jmolecules.ddd.types.AggregateRoot;

/**
 * @author Oliver Drotbohm
 */
@Getter
public class SampleJpaAggregate implements AggregateRoot<SampleJpaAggregate, SampleAggregateIdentifier> {

	private final SampleAggregateIdentifier id;

	private @ManyToOne SampleEntity annotatedEntity;
	private @ManyToMany Collection<SampleEntity> annotatedListOfEntity;

	public SampleJpaAggregate(SampleAggregateIdentifier id) {
		this.id = id;
	}

	public SampleJpaAggregate wither() {
		return new SampleJpaAggregate(id) {};
	}
}
