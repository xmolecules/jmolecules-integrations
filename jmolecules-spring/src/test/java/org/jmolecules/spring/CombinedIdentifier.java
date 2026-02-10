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
package org.jmolecules.spring;

import lombok.Value;

import org.jmolecules.ddd.types.Identifier;

@Value
class CombinedIdentifier implements Identifier {

	Integer first, second;

	public static CombinedIdentifier of(String source) {

		var parts = source.split(":");

		return new CombinedIdentifier(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]));
	}

	@Override
	public String toString() {
		return first + ":" + second;
	}
}
