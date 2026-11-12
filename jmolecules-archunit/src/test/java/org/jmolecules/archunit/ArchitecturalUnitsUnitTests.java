/*
 * Copyright 2026 the original author or authors.
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
package org.jmolecules.archunit;

import static org.assertj.core.api.Assertions.*;

import org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit;
import org.jmolecules.archunit.hexagonal.units.a.PrimaryAdapterA;
import org.jmolecules.archunit.hexagonal.units.a.nested.PrimaryAdapterNested;
import org.jmolecules.archunit.hexagonal.units.b.PrimaryAdapterB;
import org.jmolecules.archunit.hexagonal.units.c.PrimaryAdapterC;
import org.jmolecules.archunit.hexagonal.units.none.PrimaryAdapterNone;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

/**
 * Unit tests for {@link ArchitecturalUnits}.
 *
 * @author Oliver Drotbohm
 */
class ArchitecturalUnitsUnitTests {

	static final String BASE = "org.jmolecules.archunit.hexagonal.units";

	JavaClasses classes = new ClassFileImporter().importPackages(BASE);
	ArchitecturalUnits units = ArchitecturalUnits.of(classes);

	@Test // GH-418
	void detectsAllUnits() {

		assertThat(units.stream().map(ArchitecturalUnit::getId))
				.containsExactlyInAnyOrder("a", BASE + ".a.nested", BASE + ".b", BASE + ".c", BASE + ".d", BASE + ".e");
	}

	@Test // GH-418
	void usesAttributesOfUnitAnnotation() {

		ArchitecturalUnit unit = unitFor(PrimaryAdapterA.class);

		assertThat(unit.getId()).isEqualTo("a");
		assertThat(unit.getName()).isEqualTo("Unit A");
		assertThat(unit.getDescription()).isEqualTo("The first unit.");
		assertThat(unit.getPackage().getName()).isEqualTo(BASE + ".a");
		assertThat(unit.getParent()).isNull();
		assertThat(unit).hasToString("Unit A (a)");
	}

	@Test // GH-418
	void usesValueAttributeAsNameFallback() {

		ArchitecturalUnit unit = unitFor(PrimaryAdapterB.class);

		assertThat(unit.getId()).isEqualTo(BASE + ".b");
		assertThat(unit.getName()).isEqualTo("Unit B");
		assertThat(unit.getDescription()).isNull();
	}

	@Test // GH-418
	void fallsBackToPackageForCustomAnnotationWithoutAttributes() {

		ArchitecturalUnit unit = unitFor(PrimaryAdapterC.class);

		assertThat(unit.getId()).isEqualTo(BASE + ".c");
		assertThat(unit.getName()).isEqualTo("c");
		assertThat(unit.getDescription()).isNull();
	}

	@Test // GH-418
	void resolvesNearestUnitForNestedOnes() {

		ArchitecturalUnit nested = unitFor(PrimaryAdapterNested.class);
		ArchitecturalUnit parent = unitFor(PrimaryAdapterA.class);

		assertThat(nested.getId()).isEqualTo(BASE + ".a.nested");
		assertThat(nested.getName()).isEqualTo("nested");
		assertThat(nested).isNotEqualTo(parent);
		assertThat(nested.getParent()).isEqualTo(parent);

		assertThat(parent.contains(classes.get(PrimaryAdapterNested.class))).isTrue();
		assertThat(nested.contains(classes.get(PrimaryAdapterA.class))).isFalse();
	}

	@Test // GH-418
	void exposesAllTypesByDefault() {

		ArchitecturalUnit unit = unitFor(PrimaryAdapterA.class);

		assertThat(unit.exposes(classes.get(PrimaryAdapterA.class))).isTrue();
	}

	@Test // GH-418
	void considersUnitsWithSameIdEqual() {

		ArchitecturalUnit unit = unitFor(PrimaryAdapterA.class);
		ArchitecturalUnit other = ArchitecturalUnit.of("a", "Other", null, unit.getPackage(), null);

		assertThat(other).isEqualTo(unit).hasSameHashCodeAs(unit);
	}

	@Test // GH-418
	void returnsNoUnitForTypeOutsideOfUnits() {
		assertThat(units.getUnitFor(classes.get(PrimaryAdapterNone.class))).isNull();
	}

	private ArchitecturalUnit unitFor(Class<?> type) {

		JavaClass javaClass = classes.get(type);

		ArchitecturalUnit unit = units.getUnitFor(javaClass);

		assertThat(unit).describedAs("No unit found for %s", type).isNotNull();

		return unit;
	}
}
