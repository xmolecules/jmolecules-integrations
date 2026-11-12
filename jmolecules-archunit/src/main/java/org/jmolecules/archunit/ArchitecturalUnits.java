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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaPackage;

/**
 * The architectural units found in a code base, i.e. packages annotated with an annotation that is meta-annotated with
 * {@code org.jmolecules.stereotype.ArchitecturalUnit} (for example jMolecules' {@code Module}, {@code Hexagon} or
 * {@code Onion}). Allows looking up the unit a {@link JavaClass} belongs to. As units can be nested, a type belongs to
 * the unit declared on the nearest package, i.e. its own package or the closest parent package.
 *
 * @author Oliver Drotbohm
 * @since 0.34
 */
@NullMarked
public class ArchitecturalUnits implements Iterable<ArchitecturalUnits.ArchitecturalUnit>, ArchitecturalUnitResolver {

	static final String ARCHITECTURAL_UNIT_ANNOTATION = "org.jmolecules.stereotype.ArchitecturalUnit";

	private final Map<JavaPackage, @Nullable ArchitecturalUnit> cache = new HashMap<>();
	private final Set<ArchitecturalUnit> units = new LinkedHashSet<>();

	ArchitecturalUnits() {}

	/**
	 * Detects all {@link ArchitecturalUnit}s declared on the packages of the given {@link JavaClasses} and their parent
	 * packages.
	 *
	 * @param classes must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public static ArchitecturalUnits of(JavaClasses classes) {

		Assert.notNull(classes, "JavaClasses must not be null!");

		ArchitecturalUnits result = new ArchitecturalUnits();

		classes.stream()
				.map(JavaClass::getPackage)
				.distinct()
				.forEach(result::getUnitFor);

		return result;
	}

	/**
	 * Returns the {@link ArchitecturalUnit} the given type belongs to, i.e. the one declared on the type's package or the
	 * closest parent package.
	 *
	 * @param type must not be {@literal null}.
	 * @return the {@link ArchitecturalUnit} or {@literal null} if the type does not belong to any unit.
	 */
	@Override
	public @Nullable ArchitecturalUnit getUnitFor(JavaClass type) {

		Assert.notNull(type, "JavaClass must not be null!");

		return getUnitFor(type.getPackage());
	}

	/**
	 * Returns the {@link ArchitecturalUnit} the given package belongs to, i.e. the one declared on the package itself or
	 * the closest parent package.
	 *
	 * @param pkg must not be {@literal null}.
	 * @return the {@link ArchitecturalUnit} or {@literal null} if the package does not belong to any unit.
	 */
	public @Nullable ArchitecturalUnit getUnitFor(JavaPackage pkg) {

		Assert.notNull(pkg, "JavaPackage must not be null!");

		if (cache.containsKey(pkg)) {
			return cache.get(pkg);
		}

		ArchitecturalUnit parent = pkg.getParent().map(this::getUnitFor).orElse(null);
		JavaAnnotation<JavaPackage> annotation = unitAnnotation(pkg);
		ArchitecturalUnit result = parent;

		if (annotation != null) {
			result = DefaultArchitecturalUnit.of(pkg, annotation, parent);
			units.add(result);
		}

		cache.put(pkg, result);

		return result;
	}

	/**
	 * Returns all {@link ArchitecturalUnit}s detected so far.
	 *
	 * @return will never be {@literal null}.
	 */
	public Stream<ArchitecturalUnit> stream() {
		return units.stream();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<ArchitecturalUnit> iterator() {
		return units.iterator();
	}

	private static @Nullable JavaAnnotation<JavaPackage> unitAnnotation(JavaPackage pkg) {

		if (!pkg.isMetaAnnotatedWith(ARCHITECTURAL_UNIT_ANNOTATION)) {
			return null;
		}

		for (JavaAnnotation<JavaPackage> annotation : pkg.getAnnotations()) {

			JavaClass type = annotation.getRawType();

			if (type.isAnnotatedWith(ARCHITECTURAL_UNIT_ANNOTATION) || type.isMetaAnnotatedWith(ARCHITECTURAL_UNIT_ANNOTATION)) {
				return annotation;
			}
		}

		return null;
	}

	/**
	 * An architectural unit, i.e. a {@link JavaPackage} and its sub-packages that an architectural language is applied
	 * to. Units can be nested and expose a subset of their types to other units. Implementations must consider units
	 * equal if their identifiers are, so that they can be combined, for example to further restrict the types exposed by
	 * a unit.
	 *
	 * @author Oliver Drotbohm
	 * @see ArchitecturalUnitResolver
	 */
	public interface ArchitecturalUnit {

		/**
		 * Creates a new {@link ArchitecturalUnit} for the given values exposing all of its types. Meant to be used by
		 * {@link ArchitecturalUnitResolver} implementations.
		 *
		 * @param id must not be {@literal null} or empty.
		 * @param name must not be {@literal null} or empty.
		 * @param description can be {@literal null}.
		 * @param pkg the package the unit is declared on, must not be {@literal null}.
		 * @param parent the unit the new one is nested in, can be {@literal null}.
		 * @return will never be {@literal null}.
		 */
		static ArchitecturalUnit of(String id, String name, @Nullable String description, JavaPackage pkg,
				@Nullable ArchitecturalUnit parent) {

			Assert.hasText(id, "Id must not be null or empty!");
			Assert.hasText(name, "Name must not be null or empty!");
			Assert.notNull(pkg, "JavaPackage must not be null!");

			return new DefaultArchitecturalUnit(pkg, id, name, description, parent);
		}

		/**
		 * Returns the identifier of the unit. Units with the same identifier are considered equal.
		 *
		 * @return will never be {@literal null} or empty.
		 */
		String getId();

		/**
		 * Returns the human-readable name of the unit.
		 *
		 * @return will never be {@literal null} or empty.
		 */
		String getName();

		/**
		 * Returns the human-readable description of the unit, if declared.
		 *
		 * @return the description or {@literal null} if none declared.
		 */
		@Nullable
		String getDescription();

		/**
		 * Returns the package the unit is declared on.
		 *
		 * @return will never be {@literal null}.
		 */
		JavaPackage getPackage();

		/**
		 * Returns the unit the current one is nested in, if any.
		 *
		 * @return the parent unit or {@literal null} if the current one is not nested in another unit.
		 */
		@Nullable
		ArchitecturalUnit getParent();

		/**
		 * Returns whether the given type resides in the package of the unit or any sub-package. Note that this includes
		 * types of nested units.
		 *
		 * @param type must not be {@literal null}.
		 */
		default boolean contains(JavaClass type) {

			String packageName = type.getPackageName();
			String unitPackage = getPackage().getName();

			return packageName.equals(unitPackage) || packageName.startsWith(unitPackage + ".");
		}

		/**
		 * Returns whether the unit exposes the given type, i.e. whether types of other units may refer to it. Only
		 * consulted for types contained in the unit. Exposes all types by default, architectural languages might restrict
		 * the selection further.
		 *
		 * @param type must not be {@literal null}.
		 */
		default boolean exposes(JavaClass type) {
			return true;
		}
	}

	/**
	 * An {@link ArchitecturalUnit} declared on a {@link JavaPackage}. Identifier, name and description are taken from the
	 * {@code id}, {@code name} (or {@code value}) and {@code description} attributes of the declaring annotation, if
	 * present and not empty. Identifier and name default to the fully qualified and simple name of the package
	 * respectively. Exposes all of its types.
	 *
	 * @author Oliver Drotbohm
	 */
	private static class DefaultArchitecturalUnit implements ArchitecturalUnit {

		private final JavaPackage pkg;
		private final String id, name;
		private final @Nullable String description;
		private final @Nullable ArchitecturalUnit parent;

		DefaultArchitecturalUnit(JavaPackage pkg, String id, String name, @Nullable String description,
				@Nullable ArchitecturalUnit parent) {

			this.pkg = pkg;
			this.id = id;
			this.name = name;
			this.description = description;
			this.parent = parent;
		}

		static ArchitecturalUnit of(JavaPackage pkg, JavaAnnotation<?> annotation, @Nullable ArchitecturalUnit parent) {

			String id = attribute(annotation, "id");
			String name = attribute(annotation, "name");

			if (name == null) {
				name = attribute(annotation, "value");
			}

			return new DefaultArchitecturalUnit(pkg, id == null ? pkg.getName() : id,
					name == null ? pkg.getRelativeName() : name, attribute(annotation, "description"), parent);
		}

		/*
		 * (non-Javadoc)
		 * @see org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit#getId()
		 */
		@Override
		public String getId() {
			return id;
		}

		/*
		 * (non-Javadoc)
		 * @see org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit#getName()
		 */
		@Override
		public String getName() {
			return name;
		}

		/*
		 * (non-Javadoc)
		 * @see org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit#getDescription()
		 */
		@Override
		public @Nullable String getDescription() {
			return description;
		}

		/*
		 * (non-Javadoc)
		 * @see org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit#getPackage()
		 */
		@Override
		public JavaPackage getPackage() {
			return pkg;
		}

		/*
		 * (non-Javadoc)
		 * @see org.jmolecules.archunit.ArchitecturalUnits.ArchitecturalUnit#getParent()
		 */
		@Override
		public @Nullable ArchitecturalUnit getParent() {
			return parent;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(@Nullable Object obj) {
			return ArchitecturalUnits.isEqual(this, obj);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return ArchitecturalUnits.hash(this);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return ArchitecturalUnits.describe(this);
		}

		private static @Nullable String attribute(JavaAnnotation<?> annotation, String name) {

			Object value = annotation.get(name).orElse(null);

			return value instanceof String && !((String) value).isBlank() ? (String) value : null;
		}
	}

	/**
	 * Equality of {@link ArchitecturalUnit}s, based on their identifiers, for use by implementations.
	 */
	static boolean isEqual(ArchitecturalUnit unit, @Nullable Object obj) {
		return unit == obj || obj instanceof ArchitecturalUnit && unit.getId().equals(((ArchitecturalUnit) obj).getId());
	}

	static int hash(ArchitecturalUnit unit) {
		return Objects.hash(unit.getId());
	}

	static String describe(ArchitecturalUnit unit) {

		String name = unit.getName();
		String id = unit.getId();

		return name.equals(id) ? name : name + " (" + id + ")";
	}
}
