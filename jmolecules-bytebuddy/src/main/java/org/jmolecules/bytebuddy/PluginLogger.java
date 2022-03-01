/*
 * Copyright 2022 the original author or authors.
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

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.description.type.TypeDescription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.util.Assert;

/**
 * Logger to aggregate information about transformations applied to individual {@link TypeDescription}s.
 *
 * @author Oliver Drotbohm
 */
@Slf4j
enum PluginLogger {

	INSTANCE;

	private static boolean flushed = false;

	private final Map<String, List<LogEntry>> logs = new TreeMap<>();

	/**
	 * Obtains the {@link Log} for the given {@link TypeDescription} and module name.
	 *
	 * @param description must not be {@literal null}.
	 * @param name must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 */
	public Log getLog(TypeDescription description, String name) {

		Assert.notNull(description, "TypeDescription must not be null!");
		Assert.hasText(name, "Module name must not be null or empty!");

		List<LogEntry> moduleLogs = logs.computeIfAbsent(description.getName(), it -> new ArrayList<>());

		return (message, parameters) -> moduleLogs.add(new LogEntry(name, message, parameters));
	}

	public void flush() {

		if (flushed) {
			return;
		}

		try {

			logs.forEach((description, moduleLogs) -> {

				if (moduleLogs.isEmpty()) {
					return;
				}

				Collections.sort(moduleLogs, Comparator.comparing(LogEntry::getModule).thenComparing(LogEntry::getMessage));

				log.info(description);

				for (int i = 0; i < moduleLogs.size(); i++) {

					LogEntry logEntry = moduleLogs.get(i);
					String module = logEntry.getModule();

					String prefix = (i + 1) == moduleLogs.size() ? "└─ " : "├─ ";

					log.info(String.format("%s%s - %s", prefix, module, logEntry.getMessage()),
							logEntry.getParameters());
				}

				log.info("");
			});

		} finally {
			flushed = true;
		}
	}

	public interface Log {
		void info(String message, Object... parameters);
	}

	@Value
	private static class LogEntry {
		String module;
		String message;
		Object[] parameters;
	}
}
