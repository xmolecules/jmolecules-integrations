package org.jmolecules.axonframework;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.SuperMethodCall;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.jmolecules.architecture.cqrs.annotation.Command;
import org.jmolecules.architecture.cqrs.annotation.CommandHandler;
import org.jmolecules.architecture.cqrs.annotation.QueryModel;
import org.jmolecules.bytebuddy.JMoleculesPluginSupport;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Association;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.event.annotation.DomainEventHandler;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;

import static io.holixon.jmolecules.bytebuddy.PluginSupport.*;

public class JMoleculesAxonPlugin extends JMoleculesPluginSupport {

    private static final Map<Class<?>, Class<? extends Annotation>> MAPPINGS;
    private static final Set<Class<?>> TRIGGERS;
    private static final Map<Class<? extends Annotation>, Class<? extends Annotation>> METHOD_ANNOTATIONS;
    private static final Map<Class<? extends Annotation>, Map<Class<? extends Annotation>, Class<? extends Annotation>>> TYPE_METHOD_ANNOTATIONS;
    private static final Map<Class<? extends Annotation>, Class<? extends Annotation>> FIELD_ANNOTATIONS;

    static {

        MAPPINGS = Map.of(
                // jMolecules -> Axon
                AggregateRoot.class, org.axonframework.modelling.command.AggregateRoot.class,
                // Axon -> jMolecules
                org.axonframework.modelling.command.AggregateRoot.class, AggregateRoot.class
        );

        /*
         * Which annotations trigger the processing?
         */
        TRIGGERS = Set.of(
                AggregateRoot.class,
                QueryModel.class,
                Command.class
        );

        METHOD_ANNOTATIONS = Map.of(
                // jMolecules -> Axon
                CommandHandler.class, org.axonframework.commandhandling.CommandHandler.class,
                DomainEventHandler.class, org.axonframework.eventhandling.EventHandler.class,
                // Axon -> jMolecules
                org.axonframework.commandhandling.CommandHandler.class, CommandHandler.class,
                org.axonframework.eventhandling.EventHandler.class, DomainEventHandler.class
        );

        TYPE_METHOD_ANNOTATIONS = Map.of(
                AggregateRoot.class, Map.of(
                        // jMolecules -> Axon
                        DomainEventHandler.class, EventSourcingHandler.class,
                        // Axon -> jMolecules
                        EventSourcingHandler.class, DomainEventHandler.class
                )
        );

        FIELD_ANNOTATIONS = Map.of(
                // jMolecules -> Axon
                Identity.class, org.axonframework.modelling.command.AggregateIdentifier.class,
                Association.class, org.axonframework.modelling.command.TargetAggregateIdentifier.class,
                // Axon -> jMolecules
                org.axonframework.modelling.command.AggregateIdentifier.class, Identity.class,
                org.axonframework.modelling.command.TargetAggregateIdentifier.class, Association.class
        );
    }

    @Override
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription type,
                                        ClassFileLocator classFileLocator) {
        builder = mapAnnotationOrInterfaces("jMolecules Axon", builder, type, MAPPINGS);

        for (Map.Entry<Class<? extends Annotation>, Class<? extends Annotation>> entry : METHOD_ANNOTATIONS.entrySet()) {
            Class<? extends Annotation> target = entry.getValue();
            builder = builder
                    .method(hasAnnotatedMethod(type, entry.getKey(), target))
                    .intercept(SuperMethodCall.INSTANCE)
                    .annotateMethod(getAnnotation(target));
        }
        for (Map.Entry<Class<? extends Annotation>, Class<? extends Annotation>> entry : FIELD_ANNOTATIONS.entrySet()) {
            Class<? extends Annotation> target = entry.getValue();
            builder = builder
                    .field(hasAnnotatedField(type, entry.getKey(), target))
                    .annotateField(getAnnotation(target));
        }

        for (Map.Entry<Class<? extends Annotation>, Map<Class<? extends Annotation>, Class<? extends Annotation>>> typeEntry : TYPE_METHOD_ANNOTATIONS.entrySet()) {
            // the type is annotated, look for methods in it
            if (isAnnotatedWith(type, typeEntry.getKey())) {
                for (Map.Entry<Class<? extends Annotation>, Class<? extends Annotation>> entry : typeEntry.getValue().entrySet()) {
                    Class<? extends Annotation> target = entry.getValue();
                    builder = builder
                            .method(hasAnnotatedMethod(type, entry.getKey(), target))
                            .intercept(SuperMethodCall.INSTANCE)
                            .annotateMethod(getAnnotation(target));
                }
            }
        }

        return builder;
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
