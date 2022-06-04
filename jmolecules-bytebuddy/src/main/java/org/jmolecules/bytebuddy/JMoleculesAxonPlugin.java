package org.jmolecules.bytebuddy;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.SuperMethodCall;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.jmolecules.architecture.cqrs.annotation.Command;
import org.jmolecules.architecture.cqrs.annotation.CommandHandler;
import org.jmolecules.architecture.cqrs.annotation.QueryModel;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Association;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.event.annotation.DomainEventHandler;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.jmolecules.bytebuddy.JMoleculesElementMatchers.*;
import static org.jmolecules.bytebuddy.PluginUtils.*;

/**
 * Plugin enriching classes for Axon Framework.
 */
public class JMoleculesAxonPlugin extends JMoleculesPluginSupport {

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
		FIELD_ANNOTATIONS.put(Identity.class, org.axonframework.modelling.command.AggregateIdentifier.class);
		FIELD_ANNOTATIONS.put(Association.class, org.axonframework.modelling.command.TargetAggregateIdentifier.class);
		// Axon -> jMolecules
		FIELD_ANNOTATIONS.put(org.axonframework.modelling.command.AggregateIdentifier.class, Identity.class);
		FIELD_ANNOTATIONS.put(org.axonframework.modelling.command.TargetAggregateIdentifier.class, Association.class);
	}

	@Override public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription type,
			ClassFileLocator classFileLocator) {

		PluginLogger.Log log = PluginLogger.INSTANCE.getLog(type, "Axon");

		builder = mapAnnotationOrInterfaces(builder, type, MAPPINGS, log);

		for (Map.Entry<Class<? extends Annotation>, Class<? extends Annotation>> entry : METHOD_ANNOTATIONS.entrySet()) {
			Class<? extends Annotation> target = entry.getValue();
			builder = builder.method(hasAnnotatedMethod(type, entry.getKey(), target, log)).intercept(
					SuperMethodCall.INSTANCE).annotateMethod(getAnnotation(target));
		}
		for (Map.Entry<Class<? extends Annotation>, Class<? extends Annotation>> entry : FIELD_ANNOTATIONS.entrySet()) {
			Class<? extends Annotation> target = entry.getValue();
			builder = builder.field(hasAnnotatedField(type, entry.getKey(), target, log)).annotateField(
					getAnnotation(target));
		}

		for (Map.Entry<Class<? extends Annotation>, Map<Class<? extends Annotation>, Class<? extends Annotation>>> typeEntry : TYPE_METHOD_ANNOTATIONS.entrySet()) {
			// the type is annotated, look for methods in it
			if (isAnnotatedWith(type, typeEntry.getKey())) {
				for (Map.Entry<Class<? extends Annotation>, Class<? extends Annotation>> entry : typeEntry.getValue()
						.entrySet()) {
					Class<? extends Annotation> target = entry.getValue();
					builder = builder.method(hasAnnotatedMethod(type, entry.getKey(), target, log)).intercept(
							SuperMethodCall.INSTANCE).annotateMethod(getAnnotation(target));
				}
			}
		}

		return builder;
	}

	@Override public boolean matches(TypeDescription target) {
		if (residesInAnyPackageStartingWith(target, PACKAGE_PREFIX_TO_SKIP)) {
			return false;
		}
		return TRIGGERS.stream().anyMatch(
				it -> it.isAnnotation() ? isAnnotatedWith(target, it) : target.isAssignableTo(it));
	}
}
