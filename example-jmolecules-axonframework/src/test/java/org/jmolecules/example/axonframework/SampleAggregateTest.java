package org.jmolecules.example.axonframework;

import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.*;

import java.util.UUID;

public class SampleAggregateTest {

    private final AggregateTestFixture<SampleEventSourcedAggregate> aggregate = new AggregateTestFixture<>(
            SampleEventSourcedAggregate.class);

    @Test
    public void shouldCreateAggregate() {
        SampleAggregateIdentifier id = SampleAggregateIdentifier.of(UUID.randomUUID().toString());
        aggregate
                .givenNoPriorActivity()
                .when(new PerformSampleCommand(id, "value"))
                .expectEvents(new SampleEventOccurred(id, "value"));
    }

    @Test
    public void shouldDeliverRevokeToAggregate() {
        SampleAggregateIdentifier id = SampleAggregateIdentifier.of(UUID.randomUUID().toString());
        aggregate
                .given(new SampleEventOccurred(id, "value"))
                .when(new RevokeSampleCommand(id))
                .expectEvents(new SampleRevokedEventOccurred(id));
    }
}
