package org.jmolecules.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import org.jmolecules.architecture.layered.ApplicationLayer;
import org.jmolecules.archunit.layered.Layered;
import org.jmolecules.archunit.layered.app.AppType;
import org.jmolecules.archunit.layered.domain.DomainType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jmolecules.archunit.JMoleculesArchitectureRules.resideInLayer;

/**
 * Unit tests for exposed predicates.
 *
 * @author Roland Weisleder
 */
@AnalyzeClasses(packagesOf = Layered.class)
class JMoleculesArchitecturePredicatesTest {

    @ArchTest
    void predicate_resideInLayer(JavaClasses types) {

        DescribedPredicate<JavaClass> predicate = resideInLayer(ApplicationLayer.class);
        assertThat(predicate)
                .accepts(types.get(AppType.class))
                .rejects(types.get(DomainType.class));
    }
}
