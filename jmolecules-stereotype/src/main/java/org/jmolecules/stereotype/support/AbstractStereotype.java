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
package org.jmolecules.stereotype.support;

import java.util.Objects;

import org.jmolecules.stereotype.api.Stereotype;

/**
 * Base class for {@link Stereotype} implementations defining equality characteristics.
 *
 * @author Oliver Drotbohm
 */
abstract class AbstractStereotype implements Stereotype {

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.api.Stereotype#toDetailedString()
	 */
	public String toDetailedString() {
		return "%s - %s (%s, %s) - %s".formatted(getIdentifier(), getDisplayName(), getPriority(),
				isInherited() ? "inherited" : "not inherited", getGroups());
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof AbstractStereotype)) {
			return false;
		}

		AbstractStereotype that = (AbstractStereotype) obj;

		return Objects.equals(this.getIdentifier(), that.getIdentifier())
				&& Objects.equals(this.getGroups(), that.getGroups())
				&& Objects.equals(this.getDisplayName(), that.getDisplayName())
				&& Objects.equals(this.getPriority(), that.getPriority())
				&& this.isInherited() == that.isInherited();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(getIdentifier(), getGroups(), getDisplayName(), getPriority(), isInherited());
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getIdentifier();
	}
}
