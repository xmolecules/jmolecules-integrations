package org.jmolecules.bytebuddy;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import org.jmolecules.architecture.cqrs.annotation.QueryModel;
import org.jmolecules.ddd.annotation.AggregateRoot;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.jmolecules.bytebuddy.JMoleculesElementMatchers.*;
import static org.jmolecules.bytebuddy.PluginUtils.*;

/**
 * Plugin enriching classes for usage of Axon Framework with Spring.
 */
public class JMoleculesAxonSpringPlugin extends JMoleculesPluginSupport {

	private static final Map<Class<?>, Class<? extends Annotation>> MAPPINGS = new HashMap<>();
	private static final Set<Class<?>> TRIGGERS = new HashSet<>();

	static {

		// jMolecules -> Axon
		MAPPINGS.put(AggregateRoot.class, org.axonframework.spring.stereotype.Aggregate.class);
		MAPPINGS.put(QueryModel.class, org.springframework.stereotype.Component.class);
		// Axon -> jMolecules
		MAPPINGS.put(org.axonframework.spring.stereotype.Aggregate.class, AggregateRoot.class);

		/*
		 * Which annotations trigger the processing?
		 */
		TRIGGERS.add(AggregateRoot.class);
		TRIGGERS.add(QueryModel.class);
	}

	@Override public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription type,
			ClassFileLocator classFileLocator) {
		PluginLogger.Log log = PluginLogger.INSTANCE.getLog(type, "AxonSpring");
		return mapAnnotationOrInterfaces(builder, type, MAPPINGS, log);
	}

	@Override public boolean matches(TypeDescription target) {
		if (residesInAnyPackageStartingWith(target, PACKAGE_PREFIX_TO_SKIP)) {
			return false;
		}
		return TRIGGERS.stream().anyMatch(
				it -> it.isAnnotation() ? isAnnotatedWith(target, it) : target.isAssignableTo(it));
	}
}
