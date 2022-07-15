/*
 * Copyright 2022 the original author or authors.
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

import static org.jmolecules.bytebuddy.JMoleculesElementMatchers.*;
import static org.jmolecules.bytebuddy.PluginUtils.*;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.SuperMethodCall;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.jmolecules.architecture.cqrs.annotation.Command;
import org.jmolecules.architecture.cqrs.annotation.CommandHandler;
import org.jmolecules.architecture.cqrs.annotation.QueryModel;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Association;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.event.annotation.DomainEventHandler;

/**
 * Plugin enriching classes for Axon Framework.
 *
 * @author Simon Zambrovski
 * @author Oliver Drotbohm
 */
public class JMoleculesAxonPlugin implements LoggingPlugin {

	private static final Map<Class<?>, Class<? extends Annotation>> MAPPINGS = new HashMap<>();
	private static final Set<Class<?>> TRIGGERS = new HashSet<>();
	private static final Map<Class<? extends Annotation>, Class<? extends Annotation>> METHOD_ANNOTATIONS = new HashMap<>();
	private static final Map<Class<? extends Annotation>, Map<Class<? extends Annotation>, Class<? extends Annotation>>> TYPE_METHOD_ANNOTATIONS = new HashMap<>();
	private static final Map<Class<? extends Annotation>, Class<? extends Annotation>> FIELD_ANNOTATIONS = new HashMap<>();

	static {

		// jMolecules -> Axon
		MAPPINGS.put(AggregateRoot.class, org.axonframework.modelling.command.AggregateRoot.class);
		// Axon -> jMolecules
		MAPPINGS.put(org.axonframework.modelling.command.AggregateRoot.class, AggregateRoot.class);

		/*
		 * Which annotations trigger the processing?
		 */
		TRIGGERS.add(AggregateRoot.class);
		TRIGGERS.add(QueryModel.class);
		TRIGGERS.add(Command.class);

		// jMolecules -> Axon
		METHOD_ANNOTATIONS.put(CommandHandler.class, org.axonframework.commandhandling.CommandHandler.class);
		METHOD_ANNOTATIONS.put(DomainEventHandler.class, org.axonframework.eventhandling.EventHandler.class);

		// Axon -> jMolecules
		METHOD_ANNOTATIONS.put(org.axonframework.commandhandling.CommandHandler.class, CommandHandler.class);
		METHOD_ANNOTATIONS.put(org.axonframework.eventhandling.EventHandler.class, DomainEventHandler.class);

		Map<Class<? extends Annotation>, Class<? extends Annotation>> aggregateMethods = new HashMap<>();

		// jMolecules -> Axon
		aggregateMethods.put(DomainEventHandler.class, EventSourcingHandler.class);

		// Axon -> jMolecules
		aggregateMethods.put(EventSourcingHandler.class, DomainEventHandler.class);

		TYPE_METHOD_ANNOTATIONS.put(AggregateRoot.class, aggregateMethods);

		// jMolecules -> Axon
		FIELD_ANNOTATIONS.put(Association.class, org.axonframework.modelling.command.TargetAggregateIdentifier.class);

		// Axon -> jMolecules
		FIELD_ANNOTATIONS.put(org.axonframework.modelling.command.AggregateIdentifier.class, Identity.class);
		FIELD_ANNOTATIONS.put(org.axonframework.modelling.command.TargetAggregateIdentifier.class, Association.class);
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.build.Plugin#apply(net.bytebuddy.dynamic.DynamicType.Builder, net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription type,
			ClassFileLocator classFileLocator) {

		PluginLogger.Log log = PluginLogger.INSTANCE.getLog(type, "Axon");

		builder = mapAnnotationOrInterfaces(builder, type, MAPPINGS, log);

		for (Map.Entry<Class<? extends Annotation>, Class<? extends Annotation>> entry : METHOD_ANNOTATIONS.entrySet()) {

			Class<? extends Annotation> target = entry.getValue();

			builder = builder
					.method(hasAnnotatedMethod(type, entry.getKey(), target, log))
					.intercept(SuperMethodCall.INSTANCE)
					.annotateMethod(getAnnotation(target));
		}

		for (Map.Entry<Class<? extends Annotation>, Class<? extends Annotation>> entry : FIELD_ANNOTATIONS.entrySet()) {

			Class<? extends Annotation> target = entry.getValue();

			builder = builder
					.field(hasAnnotatedField(type, entry.getKey(), target, log))
					.annotateField(getAnnotation(target));
		}

		for (Entry<Class<? extends Annotation>, Map<Class<? extends Annotation>, Class<? extends Annotation>>> typeEntry : TYPE_METHOD_ANNOTATIONS
				.entrySet()) {

			if (isAnnotatedWith(type, typeEntry.getKey())) {

				for (Entry<Class<? extends Annotation>, Class<? extends Annotation>> entry : typeEntry.getValue().entrySet()) {

					Class<? extends Annotation> target = entry.getValue();

					builder = builder
							.method(hasAnnotatedMethod(type, entry.getKey(), target, log))
							.intercept(SuperMethodCall.INSTANCE)
							.annotateMethod(getAnnotation(target));
				}
			}
		}

		return JMoleculesType.of(log, builder)
				.annotateIdentifierWith(AggregateIdentifier.class)
				.conclude();
	}

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.matcher.ElementMatcher#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(TypeDescription target) {

		if (residesInPlatformPackage(target)) {
			return false;
		}

		return TRIGGERS.stream().anyMatch(
				it -> it.isAnnotation() ? isAnnotatedWith(target, it) : target.isAssignableTo(it));
	}
}
