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

import java.util.Comparator;

import org.jmolecules.stereotype.api.Stereotype;
import org.jspecify.annotations.Nullable;

public interface NodeHandler<A, P, T, M, C> {

	void handleApplication(A application);

	void handlePackage(P pkg, NodeContext context);

	void handleStereotype(Stereotype stereotype, NodeContext context);

	void handleType(T type, NodeContext context);

	void handleMethod(M method, MethodNodeContext<T> context);

	void handleCustom(C custom, NodeContext context);

	default @Nullable Comparator<T> getTypeComparator() {
		return null;
	}

	default void postGroup() {}
}
