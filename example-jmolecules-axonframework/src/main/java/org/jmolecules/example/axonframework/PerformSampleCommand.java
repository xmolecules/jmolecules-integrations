package org.jmolecules.example.axonframework;

import org.jmolecules.architecture.cqrs.annotation.Command;
import org.jmolecules.ddd.annotation.Association;
import org.jmolecules.ddd.annotation.ValueObject;

@Command(namespace = "axon", name = "PerformSampleCommand") @ValueObject public class PerformSampleCommand {

	@Association(identifiableType = SampleEventSourcedAggregate.class) private final SampleAggregateIdentifier identifier;
	private final String value;

	public PerformSampleCommand(SampleAggregateIdentifier identifier, String value) {
		this.identifier = identifier;
		this.value = value;
	}

	public SampleAggregateIdentifier getIdentifier() {
		return identifier;
	}

	public String getValue() {
		return value;
	}
}
