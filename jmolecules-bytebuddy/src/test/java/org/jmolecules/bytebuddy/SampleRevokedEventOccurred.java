package org.jmolecules.bytebuddy;

import org.axonframework.serialization.Revision;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.event.annotation.DomainEvent;

@DomainEvent(namespace = "axon", name = "SampleRevokedEvent")
@Revision("1")
@ValueObject
public class SampleRevokedEventOccurred {

    private final SampleAggregateIdentifier identifier;

    public SampleRevokedEventOccurred(SampleAggregateIdentifier identifier) {
        this.identifier = identifier;
    }

    public SampleAggregateIdentifier getIdentifier() {
        return identifier;
    }


    @Override
    public String toString() {
        return "SampleRevokedEventOccurred{" +
                "identifier=" + identifier +
                '}';
    }
}
