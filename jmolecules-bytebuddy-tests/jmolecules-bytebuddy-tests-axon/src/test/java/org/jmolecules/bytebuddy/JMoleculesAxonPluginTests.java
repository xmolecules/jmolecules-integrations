/*
 * Copyright 2022-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmolecules.bytebuddy;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.springframework.util.ReflectionUtils.*;

import example.PerformSampleCommand;
import example.RevokeSampleCommand;
import example.SampleEventOccurred;
import example.SampleEventSourcedAggregate;
import example.SampleQueryModel;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateRoot;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

class JMoleculesAxonPluginTests {

	@Test
	public void shouldHaveClassAnnotations() {
		assertThat(hasAnnotation(SampleEventSourcedAggregate.class, AggregateRoot.class)).isTrue();
	}

	@Test
	public void shouldHaveFieldAnnotations() {

		Field identifier = ReflectionUtils.findField(SampleEventSourcedAggregate.class, "identifier");
		assertThat(identifier).isNotNull();
		assertThat(hasAnnotation(identifier, AggregateIdentifier.class)).isTrue();

		Field association = ReflectionUtils.findField(PerformSampleCommand.class, "identifier");
		assertThat(association).isNotNull();
		assertThat(hasAnnotation(association, TargetAggregateIdentifier.class)).isTrue();
	}

	@Test
	public void shouldHaveMethodAnnotations() {

		Method factoryHandler = findMethod(SampleEventSourcedAggregate.class, "handle", PerformSampleCommand.class);
		assertThat(factoryHandler).isNotNull();
		assertThat(hasAnnotation(factoryHandler, CommandHandler.class)).isTrue();

		Method commandHandler = findMethod(SampleEventSourcedAggregate.class, "handle", RevokeSampleCommand.class);
		assertThat(commandHandler).isNotNull();
		assertThat(hasAnnotation(commandHandler, CommandHandler.class)).isTrue();

		Method sourcingHandler = findMethod(SampleEventSourcedAggregate.class, "on", SampleEventOccurred.class);
		assertThat(sourcingHandler).isNotNull();
		assertThat(hasAnnotation(sourcingHandler, EventSourcingHandler.class)).isTrue();

		Method eventHandler = findMethod(SampleQueryModel.class, "on", SampleEventOccurred.class);
		assertThat(eventHandler).isNotNull();
		assertThat(hasAnnotation(eventHandler, EventHandler.class)).isTrue();
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
