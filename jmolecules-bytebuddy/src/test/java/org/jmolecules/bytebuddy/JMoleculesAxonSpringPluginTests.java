package org.jmolecules.bytebuddy;

import org.axonframework.spring.stereotype.Aggregate;
import org.junit.jupiter.api.*;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JMoleculesAxonSpringPluginTests {

    @Test
    public void shouldHaveClassAnnotations() {
        assertThat(hasAnnotation(SampleAnnotatedAggregate.class, Aggregate.class)).isTrue();
        assertThat(hasAnnotation(SampleQueryModel.class, Component.class)).isTrue();
    }

    private static boolean hasAnnotation(Class<?> target, Class<? extends Annotation> annotation) {
        return target.getAnnotation(annotation) != null;
    }
}
