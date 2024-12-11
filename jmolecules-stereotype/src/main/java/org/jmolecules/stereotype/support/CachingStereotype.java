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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jmolecules.stereotype.api.Stereotype;

/**
 * An adapter that caches the computed results of all methods of a delegate {@link Stereotype}.
 *
 * @author Oliver Drotbohm
 */
class CachingStereotype extends AbstractStereotype {

	private static final Map<String, Stereotype> CACHE = new ConcurrentHashMap<>();

	private final Stereotype delegate;

	private String identifier, displayName;
	private List<String> groups;
	private Integer priority;
	private Boolean inherited;

	private CachingStereotype(Stereotype delegate) {
		this.delegate = delegate;
	}

	static Stereotype of(Stereotype metadata) {
		return CACHE.computeIfAbsent(metadata.getIdentifier(), __ -> new CachingStereotype(metadata));
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.Stereotype#getIdentifier()
	 */
	@Override
	public String getIdentifier() {

		if (identifier == null) {
			this.identifier = delegate.getIdentifier();
		}

		return this.identifier;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.Stereotype#getDisplayName()
	 */
	@Override
	public String getDisplayName() {

		if (displayName == null) {
			this.displayName = delegate.getDisplayName();
		}

		return this.displayName;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.Stereotype#getGroups()
	 */
	@Override
	public List<String> getGroups() {

		if (groups == null) {
			this.groups = delegate.getGroups();
		}

		return this.groups;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.Stereotype#getPriority()
	 */
	@Override
	public int getPriority() {

		if (priority == null) {
			this.priority = delegate.getPriority();
		}

		return priority;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.api.Stereotype#isInherited()
	 */
	@Override
	public boolean isInherited() {

		if (inherited == null) {
			this.inherited = delegate.isInherited();
		}

		return inherited;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return delegate.toString();
	}
}
