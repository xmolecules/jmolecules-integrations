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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

/**
 * A {@link Stereotype} based on a fully-qualified type name.
 *
 * @author Oliver Drotbohm
 */
public class StringBasedStereotype extends AbstractStereotype {

	private static Pattern CAMEL_CASE_SPLIT = Pattern.compile("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])");

	private final String fullyQualifiedName;
	private final int priority;
	private @Nullable String displayName;
	private @Nullable List<String> groups;
	private final boolean inherited;

	/**
	 * Creates a new {@link StringBasedStereotype} for the given fully-qualified name, priority, groups and display name.
	 *
	 * @param fullyQualifiedName will never be {@literal null}.
	 * @param priority
	 * @param groups can be {@literal null}.
	 * @param displayName can be {@literal null}.
	 */
	private StringBasedStereotype(String fullyQualifiedName, int priority, @Nullable List<String> groups,
			@Nullable String displayName, @Nullable Boolean inherited) {

		if (fullyQualifiedName == null || fullyQualifiedName.trim().isEmpty()) {
			throw new IllegalArgumentException("Name must not be null or empty!");
		}

		this.fullyQualifiedName = fullyQualifiedName;
		this.priority = priority;
		this.groups = groups;
		this.displayName = displayName;
		this.inherited = inherited == null ? true : inherited;
	}

	/**
	 * Creates a new {@link Stereotype} based on the given fully-qualified name.
	 *
	 * @param fullyQualifiedName must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 */
	public static StringBasedStereotype of(String fullyQualifiedName) {
		return of(fullyQualifiedName, DEFAULT_PRIORITY);
	}

	/**
	 * Creates a new {@link Stereotype} based on the given fully-qualified name and priority.
	 *
	 * @param fullyQualifiedName must not be {@literal null}.
	 * @param priority
	 * @return will never be {@literal null}.
	 */
	public static StringBasedStereotype of(String fullyQualifiedName, int priority) {
		return new StringBasedStereotype(fullyQualifiedName, priority, null, null, null);
	}

	public StringBasedStereotype addGroup(String group) {

		var groups = new ArrayList<String>();

		if (this.groups != null) {
			groups.addAll(this.groups);
		}

		groups.add(group);

		return new StringBasedStereotype(fullyQualifiedName, priority, groups, displayName, inherited);
	}

	public StringBasedStereotype withPriority(int priority) {
		return new StringBasedStereotype(fullyQualifiedName, priority, groups, displayName, inherited);
	}

	public StringBasedStereotype withInherited(boolean inherited) {
		return new StringBasedStereotype(fullyQualifiedName, priority, groups, displayName, inherited);
	}

	public StringBasedStereotype withDisplayName(String displayName) {
		return new StringBasedStereotype(fullyQualifiedName, priority, groups, displayName, inherited);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.Stereotype#getIdentifier()
	 */
	@Override
	public String getIdentifier() {

		if (fullyQualifiedName.startsWith("org.jmolecules")) {

			var withoutBase = fullyQualifiedName.substring("org.jmolecules".length() + 1);

			return Stream.of(withoutBase.split("\\."))
					.filter(it -> !it.equals("annotation"))
					.filter(it -> !it.equals("types"))
					.collect(Collectors.joining("."));
		}

		return fullyQualifiedName;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.Stereotype#getDisplayName()
	 */
	@Override
	public String getDisplayName() {

		if (displayName == null) {

			var identifier = getIdentifier();

			var localNameIndex = identifier.lastIndexOf(".");
			var localName = identifier.substring(localNameIndex + 1);
			var nestedClassIndex = localName.indexOf("$");

			localName = nestedClassIndex != -1 ? localName.substring(nestedClassIndex + 1) : localName;

			this.displayName = Stream.of(CAMEL_CASE_SPLIT.split(localName)).collect(Collectors.joining(" "));
		}

		return displayName;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.Stereotype#getGroups()
	 */
	@Override
	public List<String> getGroups() {

		if (groups == null) {

			var identifier = getIdentifier();

			this.groups = List.of(identifier.substring(0, identifier.lastIndexOf(".")));
		}

		return groups;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.core.Stereotype#getPriority()
	 */
	@Override
	public int getPriority() {
		return priority;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.api.Stereotype#isInherited()
	 */
	@Override
	public boolean isInherited() {
		return inherited;
	}
}
