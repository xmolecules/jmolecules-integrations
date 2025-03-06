/*
 * Copyright 2023-2025 the original author or authors.
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
package org.jmolecules.bytebuddy;

import net.bytebuddy.NamingStrategy.SuffixingRandom;
import net.bytebuddy.description.type.TypeDescription;

/**
 * A naming strategy that returns a name for a type to be created so that it will be generated into the package of a
 * reference type.
 *
 * @author Oliver Drotbohm
 */
class ReferenceTypePackageNamingStrategy extends SuffixingRandom {

	ReferenceTypePackageNamingStrategy(TypeDescription contextualType) {

		super("jMolecules", new Suffixing.BaseNameResolver() {

			/*
			 * (non-Javadoc)
			 * @see net.bytebuddy.NamingStrategy.SuffixingRandom.BaseNameResolver#resolve(net.bytebuddy.description.type.TypeDescription)
			 */
			public String resolve(TypeDescription type) {

				return contextualType.getPackage().getName()
						.concat(".")
						.concat(type.getSimpleName());
			}
		});
	}
}
