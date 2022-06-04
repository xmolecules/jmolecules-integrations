package org.jmolecules.example.axonframework;

import org.axonframework.serialization.Revision;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.event.annotation.DomainEvent;

@DomainEvent(namespace = "axon", name = "SampleEvent") @Revision("1") @ValueObject public class SampleEventOccurred {

	private final SampleAggregateIdentifier identifier;
	private final String value;

	public SampleEventOccurred(SampleAggregateIdentifier identifier, String value) {
		this.identifier = identifier;
		this.value = value;
	}

	public SampleAggregateIdentifier getIdentifier() {
		return identifier;
	}

	public String getValue() {
		return value;
	}

	@Override public String toString() {
		return "SampleEventOccurred{" + "identifier=" + identifier + ", value='" + value + '\'' + '}';
	}
}
