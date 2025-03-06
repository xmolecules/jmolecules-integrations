/*
 * Copyright 2022-2025 the original author or authors.
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

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Logger to aggregate information about transformations applied to individual {@link TypeDescription}s.
 *
 * @author Oliver Drotbohm
 */
@Slf4j
enum PluginLogger {

	INSTANCE;

	private Map<String, Set<LogEntry>> logs = new ConcurrentSkipListMap<>();

	/**
	 * Obtains the {@link Log} for the given {@link TypeDescription} and module name.
	 *
	 * @param description must not be {@literal null}.
	 * @param name must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 */
	public Log getLog(TypeDescription description, String name) {

		if (description == null) {
			throw new IllegalArgumentException("TypeDescription must not be null!");
		}

		if (name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException("Module name must not be null or empty!");
		}

		Set<LogEntry> moduleLogs = logs.computeIfAbsent(description.getName(), it -> new TreeSet<>());

		return (message, parameters) -> moduleLogs.add(new LogEntry(name, message, parameters));
	}

	public void flush() {

		try {

			if (!logs.isEmpty()) {
				log.info("");
			}

			logs.forEach((description, moduleLogs) -> {

				if (moduleLogs.isEmpty()) {
					return;
				}

				log.info("□─ " + description);
				int i = 0;

				for (LogEntry logEntry : moduleLogs) {

					String module = logEntry.getModule();
					String prefix = i + 1 == moduleLogs.size() ? "└─ " : "├─ ";

					log.info(String.format("%s%s - %s", prefix, module, logEntry.getMessage()),
							logEntry.getParameters());

					i++;
				}

				log.info("");
			});

		} finally {
			logs = new TreeMap<>();
		}
	}

	public interface Log {
		void info(String message, Object... parameters);
	}

	@Value
	private static class LogEntry implements Comparable<LogEntry> {

		String module;
		String message;
		Object[] parameters;

		/*
		 * (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(LogEntry o) {

			Comparator<LogEntry> comparator = Comparator
					.comparing(LogEntry::getModule)
					.thenComparing(LogEntry::getExpandedMessage);

			return comparator.compare(this, o);
		}

		private String getExpandedMessage() {
			return String.format(message.replace("{}", "%s"), parameters);
		}
	}
}
