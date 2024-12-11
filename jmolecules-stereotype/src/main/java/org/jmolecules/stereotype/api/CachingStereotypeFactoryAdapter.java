/*
 * Copyright 2025-2025 the original author or authors.
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
package org.jmolecules.stereotype.api;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link StereotypeFactory} adapter caching the {@link Stereotypes} detected by a delegate to avoid repeated
 * inspection.
 *
 * @author Oliver Drotbohm
 */
class CachingStereotypeFactoryAdapter<P, T, M> implements StereotypeFactory<P, T, M> {

	private final Map<P, Stereotypes> packageStereotypes = new HashMap<>();
	private final Map<T, Stereotypes> typeStereotypes = new HashMap<>();
	private final Map<M, Stereotypes> methodStereotypes = new HashMap<>();

	private final StereotypeFactory<P, T, M> delegate;

	/**
	 * Creates a new {@link CachingStereotypeFactoryAdapter} for the given delegate.
	 *
	 * @param delegate must not be {@literal null}.
	 */
	CachingStereotypeFactoryAdapter(StereotypeFactory<P, T, M> delegate) {

		if (delegate == null) {
			throw new IllegalArgumentException("Delegate StereotypFactory must not be null!");
		}

		this.delegate = delegate;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.api.StereotypeFactory#fromMethod(java.lang.Object)
	 */
	@Override
	public Stereotypes fromMethod(M method) {
		return methodStereotypes.computeIfAbsent(method, delegate::fromMethod);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.api.StereotypeFactory#fromType(java.lang.Object)
	 */
	@Override
	public Stereotypes fromType(T type) {
		return typeStereotypes.computeIfAbsent(type, delegate::fromType);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.api.StereotypeFactory#fromPackage(java.lang.Object)
	 */
	@Override
	public Stereotypes fromPackage(P pkg) {
		return packageStereotypes.computeIfAbsent(pkg, delegate::fromPackage);
	}
}
