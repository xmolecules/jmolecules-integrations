package org.jmolecules.example.axonframework;

import org.axonframework.modelling.command.AggregateLifecycle;
import org.jmolecules.architecture.cqrs.annotation.CommandHandler;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.event.annotation.DomainEventHandler;
import org.jmolecules.event.annotation.DomainEventPublisher;

@AggregateRoot public class SampleEventSourcedAggregate {

	@Identity private SampleAggregateIdentifier identifier;

	@CommandHandler(namespace = "axon", name = "PerformSampleCommand")
	@DomainEventPublisher(publishes = "axon.SampleEventOccurred")
	public static SampleEventSourcedAggregate handle(PerformSampleCommand command) {
		AggregateLifecycle.apply(new SampleEventOccurred(command.getIdentifier(), command.getValue()));
		return new SampleEventSourcedAggregate();
	}

	@CommandHandler(namespace = "axon", name = "RevokeSampleCommand")
	@DomainEventPublisher(publishes = "axon.SampleRevokedEventOccurred") public void handle(RevokeSampleCommand command) {
		AggregateLifecycle.apply(new SampleRevokedEventOccurred(command.getIdentifier()));
	}

	@DomainEventHandler(namespace = "axon", name = "SampleEventOccurred") public void on(SampleEventOccurred event) {
		this.identifier = event.getIdentifier();
	}
}
