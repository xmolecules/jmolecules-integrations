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

import java.util.SortedSet;

import org.jmolecules.stereotype.api.Stereotype;
import org.jmolecules.stereotype.catalog.StereotypeGroup;

public interface NodeHandler<P, T, M> {

	NestingLevel handlePackage(P pkg, NestingLevel context);

	NestingLevel handleStereotypeGroup(Stereotype stereotype, SortedSet<StereotypeGroup> groups,
			StereotypeGrouped<?> grouped, NestingLevel context);

	default void postStereotypeGroup(StereotypeGrouped<?> grouped) {}

	NestingLevel handleType(T type, NestingLevel context);

	NestingLevel handleMethod(M method, StereotypeGrouped<M> grouped, NestingLevel context);

}
