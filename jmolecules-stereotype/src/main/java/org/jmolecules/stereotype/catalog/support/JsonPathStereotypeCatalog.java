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
package org.jmolecules.stereotype.catalog.support;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.jmolecules.stereotype.catalog.StereotypeDefinition.Assignment;
import org.jmolecules.stereotype.catalog.StereotypeDefinition.Assignment.Type;
import org.jmolecules.stereotype.catalog.StereotypeGroup;
import org.jspecify.annotations.Nullable;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

/**
 * A {@link org.jmolecules.stereotype.catalog.StereotypeCatalog} reading
 * {@link org.jmolecules.stereotype.catalog.StereotypeDefinition}s from a {@link CatalogSource} and parsing them using
 * JSON Path expressions.
 *
 * @author Oliver Drotbohm
 */
public class JsonPathStereotypeCatalog extends AbstractStereotypeCatalog implements StereotypeDetector {

	private final CatalogSource source;

	/**
	 * Creates a new {@link JsonPathStereotypeCatalog} for the given {@link CatalogSource}.
	 *
	 * @param source must not be {@literal null}.
	 */
	public JsonPathStereotypeCatalog(CatalogSource source) {

		if (source == null) {
			throw new IllegalArgumentException("CatalogSource must not be null!");
		}

		this.source = source;
		this.source.forEach(this::read);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.catalog.support.AbstractStereotypeCatalog#toString()
	 */
	@Override
	public String toString() {

		var builder = new StringBuilder(super.toString());
		var headline = "Catalog sources";

		builder.append(headline).append("\n")
				.append("=".repeat(headline.length())).append("\n");

		for (URL url : source) {
			builder.append("- ").append(url).append("\n");
		}

		return builder.toString();
	}

	private void read(URL url) {

		try (var in = url.openStream()) {

			var configuration = Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS);
			var context = JsonPath.parse(in, configuration);

			var stereotypes = context.read("$.stereotypes", JSONObject.class);

			if (stereotypes != null) {
				definitionsFromJson(stereotypes, url).forEach(this::add);
			}

			var groups = context.read("$.groups", JSONObject.class);

			if (groups != null) {
				JsonPathStereotypeCatalog.groupsFromJson(groups, url.toURI()).forEach(this::add);
			}

		} catch (IOException o_O) {
			throw new UncheckedIOException(o_O);
		} catch (Exception o_O) {
			throw new IllegalArgumentException("Failure reading file %s".formatted(url), o_O);
		}
	}

	private static Stream<AugmentableStereotypeDefinition> definitionsFromJson(JSONObject json, Object source) {
		return json.entrySet().stream().map(it -> fromJson(it.getKey(), (JSONObject) it.getValue(), source));
	}

	private static @Nullable AugmentableStereotypeDefinition fromJson(String identifier, JSONObject json, Object source) {

		var definition = DefaultStereotypeDefinition.forIdentifier(identifier, source)
				.andDisplayName(json.getAsString("displayName"));

		var targets = (JSONArray) json.get("assignments");

		if (targets != null) {

			targets.stream()
					.map(String.class::cast)
					.reduce(definition, (id, target) -> definition.addAssignment(Assignment.of(target)), (l, r) -> r);

		} else {

			definition.addAssignment(Assignment.of(identifier, Type.IMPLEMENTS));
			definition.addAssignment(Assignment.of(identifier, Type.IS_ANNOTATED));
		}

		var priority = json.getAsNumber("priority");

		if (priority != null) {
			definition.andPriority(priority.intValue());
		}

		var inherited = json.getAsString("inherited");

		if (inherited != null) {
			definition.andInherited(Boolean.parseBoolean(inherited));
		}

		var groups = (JSONArray) json.get("groups");

		if (groups != null) {
			groups.stream()
					.map(String.class::cast)
					.reduce(definition, (it, group) -> it.addGroup(group), (l, r) -> r);
		}

		return definition.build();
	}

	private static Stream<StereotypeGroup> groupsFromJson(JSONObject groups, URI source) {

		return groups.entrySet().stream()
				.map(it -> groupFromJson(it.getKey(), (Map<String, Object>) it.getValue(), source));
	}

	private static StereotypeGroup groupFromJson(String identifier, Map<String, Object> map, URI source) {

		return new StereotypeGroup(identifier,
				(String) map.get("displayName"),
				parseType(map.get("type")),
				(Integer) map.get("priority"),
				source);
	}

	private static org.jmolecules.stereotype.catalog.StereotypeGroup.@Nullable Type parseType(Object type) {

		return type == null
				? null
				: org.jmolecules.stereotype.catalog.StereotypeGroup.Type.valueOf(type.toString().toUpperCase(Locale.ENGLISH));
	}
}
