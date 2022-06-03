package org.jmolecules.bytebuddy;

import org.axonframework.modelling.command.AggregateRoot;
import org.axonframework.spring.stereotype.Aggregate;
import org.junit.jupiter.api.*;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JMoleculesAxonAnnotationTests {
    
    @Test
    public void shouldHaveClassAnnotations() {

        assertThat(hasAnnotation(SampleAggregate.class, Aggregate.class)).isTrue();
        assertThat(hasAnnotation(SampleAggregate.class, AggregateRoot.class)).isTrue();

        assertThat(hasAnnotation(SampleAggregateIdentifier.class, AggregateRoot.class)).isFalse();
        assertThat(hasAnnotation(SampleAggregateIdentifier.class, Aggregate.class)).isFalse();

        assertThat(hasAnnotation(SampleQueryModel.class, Component.class)).isTrue();
    }

    private static boolean hasAnnotation(Class<?> target, Class<? extends Annotation> annotation) {
        return target.getAnnotation(annotation) != null;
    }
}
