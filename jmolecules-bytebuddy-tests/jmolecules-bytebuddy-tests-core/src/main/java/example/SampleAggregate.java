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
package example;

import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Association;

/**
 * @author Oliver Drotbohm
 */
@Getter
public class SampleAggregate implements AggregateRoot<SampleAggregate, SampleAggregateIdentifier> {

	private final SampleAggregateIdentifier id;
	private SampleOtherIdentifier otherId;

	private @Setter SampleEntity entity;
	private Collection<SampleEntity> listOfEntity;
	private Collection<SampleEntity> anotherListOfEntity;

	private Association<SampleAggregate, SampleAggregateIdentifier> association;

	public SampleAggregate(SampleAggregateIdentifier id) {
		this.id = id;
	}

	public SampleAggregate wither() {
		return new SampleAggregate(id) {};
	}
}
