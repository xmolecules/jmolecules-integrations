package org.jmolecules.bytebuddy;

import org.axonframework.modelling.command.AggregateRoot;
import org.axonframework.spring.stereotype.Aggregate;
import org.junit.jupiter.api.*;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JMoleculesAxonSpringPluginTests {

    @Test
    public void shouldHaveClassAnnotations() {

        assertThat(hasAnnotation(SampleAnnotatedAggregate.class, Aggregate.class)).isTrue();
        assertThat(hasAnnotation(SampleAnnotatedAggregate.class, AggregateRoot.class)).isTrue();
        assertThat(hasAnnotation(SampleQueryModel.class, Component.class)).isTrue();
    }

    @Test
    public void shouldHaveMethodAnnotations() {
        Field isNewField = ReflectionUtils.findField(SampleAggregate.class, PersistableImplementor.IS_NEW_FIELD);
    }

    private static boolean hasAnnotation(Class<?> target, Class<? extends Annotation> annotation) {
        return target.getAnnotation(annotation) != null;
    }
}
