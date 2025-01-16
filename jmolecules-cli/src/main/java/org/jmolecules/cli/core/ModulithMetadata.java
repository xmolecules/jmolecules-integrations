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
package org.jmolecules.cli.core;

import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.util.function.SingletonSupplier;

import com.jayway.jsonpath.JsonPath;

/**
 * @author Oliver Drotbohm
 */
@RequiredArgsConstructor
public class ModulithMetadata {

	public static final String MODULES_CACHE = "modules";

	private final Supplier<Map<String, String>> modulePackages;

	public ModulithMetadata(MetadataCache workspace) {
		this.modulePackages = SingletonSupplier.of(() -> ModulithMetadata.parseModulithMetadata(workspace));
	}

	public Set<String> getModuleIdentifiers() {
		return modulePackages.get().keySet();
	}

	public String getPackageFor(String moduleIdentifier) {
		return modulePackages.get().get(moduleIdentifier);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> parseModulithMetadata(MetadataCache cache) {

		var document = JsonPath.parse(cache.readInternal(MODULES_CACHE));
		Set<String> moduleIdentifiers = document.read("$.keys()", Set.class);

		return moduleIdentifiers.stream()
				.collect(
						Collectors.toMap(Object::toString, it -> document.read("$.%s.basePackage".formatted(it)).toString()));
	}
}
