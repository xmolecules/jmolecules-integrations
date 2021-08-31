/*
 * Copyright 2021 the original author or authors.
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
package org.jmolecules.spring.data;

import org.jmolecules.ddd.types.Identifiable;
import org.jmolecules.ddd.types.Identifier;
import org.springframework.data.domain.Persistable;

/**
 * Synthetic extension of {@link Persistable} to allow marking an entity instance as not new.
 *
 * @author Oliver Drotbohm
 */
public interface MutablePersistable<T extends Identifiable<T, ID>, ID extends Identifier<T, ID>>
		extends Persistable<ID>, Identifiable<T, ID> {

	void __jMolecules__markNotNew();
}
