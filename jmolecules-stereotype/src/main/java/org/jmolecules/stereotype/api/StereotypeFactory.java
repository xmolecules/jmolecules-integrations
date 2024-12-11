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
package org.jmolecules.stereotype.api;

/**
 * An SPI to implement factories that can detect {@link Stereotype}s based on an arbitrary abstraction of packages,
 * types and methods.
 *
 * @author Oliver Drotbohm
 * @param <P> the package abstraction
 * @param <T> the type abstraction
 * @param <M> the method abstraction
 */
public interface StereotypeFactory<P, T, M> {

	/**
	 * Detects all {@link Stereotypes} assigned to the given package. This should include stereotypes declared on parent
	 * packages if the type system allows traversing the package hierarchy.
	 *
	 * @param pkg must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	Stereotypes fromPackage(P pkg);

	/**
	 * Detects all {@link Stereotypes} assigned to the given type. This should include stereotypes declared on the type's
	 * package and potentially parent packages if the type system allows traversing the package hierarchy.
	 *
	 * @param type must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	Stereotypes fromType(T type);

	/**
	 * Detects all {@link Stereotypes} assigned to the given method.
	 *
	 * @param method must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	Stereotypes fromMethod(M method);

	/**
	 * Creates a new {@link StereotypeFactory} adapter that caches the {@link Stereotypes} calculated.
	 *
	 * @param <P> the package abstraction
	 * @param <T> the type abstraction
	 * @param <M> the method abstraction
	 * @param delegate must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public static <P, T, M> StereotypeFactory<P, T, M> caching(StereotypeFactory<P, T, M> delegate) {
		return new CachingStereotypeFactoryAdapter<>(delegate);
	}
}
