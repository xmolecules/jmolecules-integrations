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

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Oliver Drotbohm
 */
public class LabelUtils {

	public static String abbreviate(String fullyQualifiedTypeName) {

		var typeIndex = fullyQualifiedTypeName.lastIndexOf(".");
		var packageName = fullyQualifiedTypeName.substring(0, typeIndex);
		var typeName = fullyQualifiedTypeName.substring(typeIndex);

		return abbreviate(packageName, "", typeName);
	}

	public static String abbreviate(String fullyQualifiedTypeName, String basePackage) {

		var typeIndex = fullyQualifiedTypeName.lastIndexOf(".");
		var typeName = fullyQualifiedTypeName.substring(typeIndex);
		var extension = fullyQualifiedTypeName.substring(basePackage.length(), typeIndex);

		return abbreviate(basePackage, extension, typeName);
	}

	private static String abbreviate(String base, String extension, String type) {

		var abbreviatedPackage = Arrays.stream(base.split("\\."))
				.map(it -> it.substring(0, 1))
				.collect(Collectors.joining("."));

		return abbreviatedPackage + extension + type;
	}
}
