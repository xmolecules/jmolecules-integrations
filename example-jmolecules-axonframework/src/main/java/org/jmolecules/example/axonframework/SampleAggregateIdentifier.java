package org.jmolecules.example.axonframework;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.Objects;

@ValueObject
public class SampleAggregateIdentifier implements Identifier {

    private final String id;

    private SampleAggregateIdentifier(String id) {
        this.id = id;
    }

    public static SampleAggregateIdentifier of(String id) {
        return new SampleAggregateIdentifier(id);
    }

    public String getValue() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SampleAggregateIdentifier that = (SampleAggregateIdentifier) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "SampleAggregateIdentifier{" +
                "id='" + id + '\'' +
                '}';
    }
}
