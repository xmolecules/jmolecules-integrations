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

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;

/**
 * Options to customize the verification of Layered Architectures.
 *
 * @author Oliver Drotbohm
 * @since 0.34
 * @see JMoleculesArchitectureRules#ensureLayering(LayeredVerificationOptions)
 * @see JMoleculesArchitectureRules#ensureLayeringStrict(LayeredVerificationOptions)
 */
public final class LayeredVerificationOptions extends VerificationOptions<LayeredVerificationOptions> {

	private static final LayeredVerificationOptions DEFAULTS = new LayeredVerificationOptions(
			StereotypeLookup.defaultLookup(), null);

	private LayeredVerificationOptions(StereotypeLookup stereotypeLookup,
			@Nullable DescribedPredicate<JavaClass> scope) {
		super(stereotypeLookup, scope);
	}

	/**
	 * Returns the default {@link LayeredVerificationOptions}, i.e. {@link StereotypeLookup#defaultLookup()} and no
	 * scope restriction.
	 *
	 * @return will never be {@literal null}.
	 */
	public static LayeredVerificationOptions defaults() {
		return DEFAULTS;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.archunit.VerificationOptions#with(...)
	 */
	@Override
	LayeredVerificationOptions with(StereotypeLookup stereotypeLookup, @Nullable DescribedPredicate<JavaClass> scope) {
		return new LayeredVerificationOptions(stereotypeLookup, scope);
	}
}
