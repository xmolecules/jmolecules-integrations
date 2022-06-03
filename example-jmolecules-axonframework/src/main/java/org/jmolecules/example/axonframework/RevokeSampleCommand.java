package org.jmolecules.example.axonframework;

import org.jmolecules.architecture.cqrs.annotation.Command;
import org.jmolecules.ddd.annotation.Association;
import org.jmolecules.ddd.annotation.ValueObject;

@Command(namespace = "axon", name = "RevokeSampleCommand")
@ValueObject
public class RevokeSampleCommand {

    @Association(identifiableType = SampleEventSourcedAggregate.class)
    private final SampleAggregateIdentifier identifier;

    public RevokeSampleCommand(SampleAggregateIdentifier identifier) {
        this.identifier = identifier;
    }

    public SampleAggregateIdentifier getIdentifier() {
        return identifier;
    }
}
