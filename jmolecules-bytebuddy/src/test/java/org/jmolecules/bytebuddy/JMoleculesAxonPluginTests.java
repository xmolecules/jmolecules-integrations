package org.jmolecules.bytebuddy;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateRoot;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.junit.jupiter.api.*;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.util.ReflectionUtils.findMethod;

public class JMoleculesAxonPluginTests {

    @Test
    public void shouldHaveClassAnnotations() {
        assertThat(hasAnnotation(SampleEventSourcedAggregate.class, AggregateRoot.class)).isTrue();
    }

//    @Test
    public void shouldHaveFieldAnnotations() {
        Field identifier = ReflectionUtils.findField(SampleEventSourcedAggregate.class, "identifier");
        assertThat(identifier).isNotNull();
        assertThat(hasAnnotation(identifier, AggregateIdentifier.class)).isTrue();

        Field association = ReflectionUtils.findField(PerformSampleCommand.class, "identifier");
        assertThat(identifier).isNotNull();
        assertThat(hasAnnotation(identifier, TargetAggregateIdentifier.class)).isTrue();
    }

  //  @Test
    public void shouldHaveMethodAnnotations() {
        Method factoryHandler = findMethod(SampleEventSourcedAggregate.class, "handle", PerformSampleCommand.class);
        assertThat(factoryHandler).isNotNull();
        assertThat(hasAnnotation(factoryHandler, CommandHandler.class)).isTrue();

        Method commandHandler = findMethod(SampleEventSourcedAggregate.class, "handle", RevokeSampleCommand.class);
        assertThat(commandHandler).isNotNull();
        assertThat(hasAnnotation(commandHandler, CommandHandler.class)).isTrue();

        Method sourcingHandler = findMethod(SampleEventSourcedAggregate.class, "on", SampleEventOccurred.class);
        assertThat(sourcingHandler).isNotNull();
        assertThat(hasAnnotation(commandHandler, EventSourcingHandler.class)).isTrue();

        Method eventHandler = findMethod(SampleQueryModel.class, "on", SampleEventOccurred.class);
        assertThat(eventHandler).isNotNull();
        assertThat(hasAnnotation(commandHandler, EventHandler.class)).isTrue();

    }

    private static boolean hasAnnotation(Class<?> target, Class<? extends Annotation> annotation) {
        return target.getAnnotation(annotation) != null;
    }

    private static boolean hasAnnotation(Field field, Class<? extends Annotation> annotation) {
        return field.getAnnotation(annotation) != null;
    }

    private static boolean hasAnnotation(Method method, Class<? extends Annotation> annotation) {
        return method.getAnnotation(annotation) != null;
    }
}
