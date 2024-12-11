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
import java.net.URL;
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
public class JsonPathStereotypeCatalog extends AbstractStereotypeCatalog {

	/**
	 * Creates a new {@link JsonPathStereotypeCatalog} for the given {@link CatalogSource}.
	 *
	 * @param source must not be {@literal null}.
	 */
	public JsonPathStereotypeCatalog(CatalogSource source) {

		if (source == null) {
			throw new IllegalArgumentException("CatalogSource must not be null!");
		}

		source.getSources().forEach(this::read);
	}

	private void read(URL url) {

		try (var in = url.openStream()) {

			var configuration = Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS);
			var context = JsonPath.parse(in, configuration);

			var stereotypes = context.read("$.stereotypes", JSONObject[].class);

			if (stereotypes != null) {

				for (var source : stereotypes) {

					var definition = fromJson(source);

					if (definition != null) {
						add(definition);
					}
				}
			}

			var groups = context.read("$.groups", JSONArray.class);

			if (groups != null) {
				Stream.of(groups)
						.flatMap(JsonPathStereotypeCatalog::groupFromJson)
						.forEach(this::add);
			}

		} catch (IOException o_O) {
			throw new UncheckedIOException(o_O);
		} catch (Exception o_O) {
			throw new IllegalArgumentException("Failure reading file %s".formatted(url), o_O);
		}
	}

	private static @Nullable AugmentableStereotypeDefinition fromJson(JSONObject json) {

		var identifier = json.getAsString("id");

		if (identifier == null) {
			return null;
		}

		var definition = DefaultStereotypeDefinition.forIdentifier(identifier)
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

		var groups = (JSONArray) json.get("groups");

		if (groups != null) {
			groups.stream()
					.map(String.class::cast)
					.reduce(definition, (it, group) -> it.addGroup(group), (l, r) -> r);
		}

		return definition.build();
	}

	private static Stream<StereotypeGroup> groupFromJson(JSONArray groups) {

		return groups.stream()
				.map(Map.class::cast)
				.map(group -> {

					var identifier = (JSONArray) group.get("ids");
					var displayName = group.get("displayName").toString();

					return new StereotypeGroup(identifier.stream().map(Object::toString).toList(), displayName);
				});
	}
}
