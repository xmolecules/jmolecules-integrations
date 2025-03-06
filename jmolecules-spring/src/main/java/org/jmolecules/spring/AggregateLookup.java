/*
 * Copyright 2020-2025 the original author or authors.
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
package org.jmolecules.spring;

import java.util.Optional;

/**
 * Lookup interface to be implemented e.g. some repository to find an aggregate by their identifiers.
 *
 * @author Oliver Drotbohm
 * @deprecated prefer {@code org.jmolecules.ddd.integration.AggregateLookup}.
 */
@Deprecated(since = "0.15", forRemoval = true)
public interface AggregateLookup<T, ID> {

	/**
	 * Returns the
	 *
	 * @param id
	 * @return
	 */
	Optional<T> findById(ID id);
}
