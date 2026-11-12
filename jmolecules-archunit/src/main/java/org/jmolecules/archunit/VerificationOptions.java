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

import org.jmolecules.archunit.JMoleculesArchitectureRules.StereotypeLookup;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;

/**
 * Base class for the options to customize the verification of an architectural language. Instances are immutable, use
 * the {@code defaults()} method of the concrete type as starting point and the wither methods to derive customized
 * ones.
 *
 * @param <T> the concrete options type.
 * @author Oliver Drotbohm
 * @since 0.34
 * @see HexagonalVerificationOptions
 * @see OnionVerificationOptions
 * @see LayeredVerificationOptions
 */
public abstract class VerificationOptions<T extends VerificationOptions<T>> {

	private final StereotypeLookup stereotypeLookup;
	private final @Nullable DescribedPredicate<JavaClass> scope;

	VerificationOptions(StereotypeLookup stereotypeLookup, @Nullable DescribedPredicate<JavaClass> scope) {

		this.stereotypeLookup = stereotypeLookup;
		this.scope = scope;
	}

	/**
	 * Returns new options using the given {@link StereotypeLookup} to find the stereotypes assigned to types. Defaults to
	 * {@link StereotypeLookup#defaultLookup()}.
	 *
	 * @param stereotypeLookup must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public T withStereotypeLookup(StereotypeLookup stereotypeLookup) {

		Assert.notNull(stereotypeLookup, "StereotypeLookup must not be null!");

		return with(stereotypeLookup, scope);
	}

	/**
	 * Returns new options restricting the verification to the types matching the given scope. Only references between
	 * types in the scope are considered, references from or to types outside of it are ignored. Use this to verify the
	 * inside of a module in isolation, for example. By default, all types are verified.
	 *
	 * @param scope must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public T withScope(DescribedPredicate<JavaClass> scope) {

		Assert.notNull(scope, "Scope must not be null!");

		return with(stereotypeLookup, scope);
	}

	abstract T with(StereotypeLookup stereotypeLookup, @Nullable DescribedPredicate<JavaClass> scope);

	StereotypeLookup getStereotypeLookup() {
		return stereotypeLookup;
	}

	@Nullable
	DescribedPredicate<JavaClass> getScope() {
		return scope;
	}
}
