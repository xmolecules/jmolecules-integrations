package org.jmolecules.bytebuddy;

import static org.jmolecules.bytebuddy.JMoleculesElementMatchers.*;
import static org.jmolecules.bytebuddy.PluginUtils.*;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jmolecules.architecture.cqrs.annotation.QueryModel;
import org.jmolecules.bytebuddy.PluginLogger.Log;
import org.jmolecules.ddd.annotation.AggregateRoot;

/**
 * Plugin enriching classes for usage of Axon Framework with Spring.
 *
 * @author Simon Zambrovski
 * @author Oliver Drotbohm
 */
public class JMoleculesAxonSpringPlugin implements LoggingPlugin {

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

	/*
	 * (non-Javadoc)
	 * @see net.bytebuddy.build.Plugin#apply(net.bytebuddy.dynamic.DynamicType.Builder, net.bytebuddy.description.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)
	 */
	@Override
	public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription type,
			ClassFileLocator classFileLocator) {

		Log log = PluginLogger.INSTANCE.getLog(type, "Axon + Spring");

		return mapAnnotationOrInterfaces(builder, type, MAPPINGS, log);
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
