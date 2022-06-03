package org.jmolecules.axonframework;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import org.jmolecules.architecture.cqrs.annotation.QueryModel;
import org.jmolecules.bytebuddy.JMoleculesPluginSupport;
import org.jmolecules.ddd.annotation.AggregateRoot;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;

import static io.holixon.jmolecules.bytebuddy.PluginSupport.*;

public class JMoleculesAxonSpringPlugin extends JMoleculesPluginSupport {

    private static final Map<Class<?>, Class<? extends Annotation>> MAPPINGS;
    private static final Set<Class<?>> TRIGGERS;

    static {

        MAPPINGS = Map.of(
                // jMolecules -> Axon
                AggregateRoot.class, org.axonframework.spring.stereotype.Aggregate.class,
                QueryModel.class, org.springframework.stereotype.Component.class,
                // Axon -> jMolecules
                org.axonframework.spring.stereotype.Aggregate.class, AggregateRoot.class
        );

        /*
         * Which annotations trigger the processing?
         */
        TRIGGERS = Set.of(
                AggregateRoot.class,
                QueryModel.class
        );
    }

    @Override
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription type,
                                        ClassFileLocator classFileLocator) {
        return mapAnnotationOrInterfaces("jMolecules Axon Spring", builder, type, MAPPINGS);
    }

    @Override
    public boolean matches(TypeDescription target) {
        if (TYPES_TO_SKIP.stream().anyMatch(it -> target.getPackage().getName().startsWith(it))) {
            return false;
        }
        return TRIGGERS.stream().anyMatch(it -> it.isAnnotation()
                ? isAnnotatedWith(target, it)
                : target.isAssignableTo(it));
    }
}
