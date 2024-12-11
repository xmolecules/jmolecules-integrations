/*
 * Copyright 2024-2025 the original author or authors.
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
package org.jmolecules.stereotype.catalog;

import java.util.Objects;
import java.util.Set;

import org.jmolecules.stereotype.api.Stereotype;

/**
 * A combination of a {@link Stereotype} and a set of {@link Assignment}s.
 *
 * @author Oliver Drotbohm
 */
public interface StereotypeDefinition extends Comparable<StereotypeDefinition> {

	/**
	 * Returns the {@link Stereotype}.
	 *
	 * @return the stereotype
	 */
	Stereotype getStereotype();

	/**
	 * Returns all {@link Assignment}s defined.
	 *
	 * @return the assignment
	 */
	Set<Assignment> getAssignments();

	Set<Object> getSources();

	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	default int compareTo(StereotypeDefinition that) {
		return this.getStereotype().compareTo(that.getStereotype());
	}

	/**
	 * Returns whether the {@link StereotypeDefinition} returns the same {@link Stereotype} of the given one.
	 *
	 * @param definition must not be {@literal null}.
	 */
	default boolean refersToSameStereotypeAs(StereotypeDefinition definition) {
		return refersTo(definition.getStereotype());
	}

	/**
	 * Returns whether the {@link StereotypeDefinition} refers to the given {@link Stereotype}.
	 *
	 * @param stereotype must not be {@literal null}.
	 */
	default boolean refersTo(Stereotype stereotype) {
		return this.getStereotype().hasSameIdentifierAs(stereotype);
	}

	/**
	 * An abstraction of a target na
	 *
	 * @author Oliver Drotbohm
	 */
	public static class Assignment {

		private final String target;
		private final Type type;

		/**
		 * @param target
		 * @param type
		 */
		private Assignment(String target, Type type) {

			this.target = target;
			this.type = type;
		}

		/**
		 * Creates a new {@link Assignment} based on the given target identifier, usually a fully-qualified type name.
		 * Identifiers starting with {@code @} will be considered annotation based assignments.
		 *
		 * @param target must not be {@literal null} or empty.
		 * @return
		 */
		public static Assignment of(String target) {

			if (target == null) {
				throw new IllegalArgumentException("Target must not be null!");
			}

			if (target.isBlank()) {
				throw new IllegalArgumentException("Target must not be empty!");
			}

			var forAnnotation = target.startsWith("@");
			var fqn = forAnnotation ? target.substring(1) : target;

			return new Assignment(fqn, forAnnotation ? Type.IS_ANNOTATED : Type.IMPLEMENTS);
		}

		/**
		 * Creates a new {@link Assignment} for the target identifier, usually a fully-qualified type name and assignment
		 * {@link Type}.
		 *
		 * @param target must not be {@literal null} or empty.
		 * @param type must not be {@literal null}.
		 * @return
		 */
		public static Assignment of(String target, Type type) {

			if (target == null) {
				throw new IllegalArgumentException("Target must not be null!");
			}

			if (target.isBlank()) {
				throw new IllegalArgumentException("Target must not be empty!");
			}

			if (type == null) {
				throw new IllegalArgumentException("Assignment type must not be null!");
			}

			return new Assignment(target, type);
		}

		/**
		 * Returns the assignment target that we look for in the candidates.
		 *
		 * @return will never be {@literal null}.
		 */
		public String getTarget() {
			return target;
		}

		/**
		 * Returns whether the {@link Assignment} has the given {@link Type}.
		 *
		 * @param type must not be {@literal null}.
		 */
		public boolean hasType(Type type) {
			return this.type.equals(type);
		}

		/**
		 * @return the type
		 */
		Type getType() {
			return type;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return (type == Type.IS_ANNOTATED ? "types annotated with @" : "types assignable to ") + target;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {

			if (obj == this) {
				return true;
			}

			if (!(obj instanceof Assignment)) {
				return false;
			}

			Assignment that = (Assignment) obj;

			return Objects.equals(this.target, that.target)
					&& Objects.equals(this.type, that.type);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return Objects.hash(target, type);
		}

		/**
		 * An assignment type.
		 *
		 * @author Oliver Drotbohm
		 */
		public enum Type {

			/**
			 * An implements assignment, meaning the candidate has to implement the target.
			 */
			IMPLEMENTS,

			/**
			 * An annotation-based assignment, meaning that the candidate has to be annotated with the target.
			 */
			IS_ANNOTATED;
		}
	}
}
