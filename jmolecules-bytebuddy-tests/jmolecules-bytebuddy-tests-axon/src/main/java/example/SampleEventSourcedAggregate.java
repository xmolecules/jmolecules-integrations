/*
 * Copyright 2022 the original author or authors.
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

import org.axonframework.modelling.command.AggregateLifecycle;
import org.jmolecules.architecture.cqrs.CommandHandler;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.event.annotation.DomainEventHandler;
import org.jmolecules.event.annotation.DomainEventPublisher;

@AggregateRoot
public class SampleEventSourcedAggregate {

	@Identity //
	private SampleAggregateIdentifier identifier;

	@CommandHandler(namespace = "axon", name = "PerformSampleCommand")
	@DomainEventPublisher(publishes = "axon.SampleEventOccurred")
	public static SampleEventSourcedAggregate handle(PerformSampleCommand command) {
		AggregateLifecycle.apply(
				new SampleEventOccurred(command.getIdentifier(), command.getValue()));
		return new SampleEventSourcedAggregate();
	}

	@CommandHandler(namespace = "axon", name = "RevokeSampleCommand")
	@DomainEventPublisher(publishes = "axon.SampleRevokedEventOccurred")
	public void handle(RevokeSampleCommand command) {

		AggregateLifecycle.apply(
				new SampleRevokedEventOccurred(command.getIdentifier()));
	}

	@DomainEventHandler(namespace = "axon", name = "SampleEventOccurred")
	public void on(SampleEventOccurred event) {
		this.identifier = event.getIdentifier();
	}
}
