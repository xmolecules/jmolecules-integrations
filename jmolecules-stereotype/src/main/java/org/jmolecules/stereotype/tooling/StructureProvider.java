/*
 * Copyright 2025 the original author or authors.
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
package org.jmolecules.stereotype.tooling;

import java.util.Collection;

/**
 * @author Oliver Drotbohm
 */
public interface StructureProvider<A, P, T, M> {

	Collection<P> extractPackages(A application);

	Collection<M> extractMethods(T type);

	interface SimpleStructureProvider<A, P, T, M> extends StructureProvider<A, P, T, M> {
		Collection<T> extractTypes(P pkg);
	}

	interface GroupingStructureProvider<A, P, T, M, C> extends StructureProvider<A, P, T, M> {
		Grouped<C, T> groupTypes(P pkg);
	}
}
