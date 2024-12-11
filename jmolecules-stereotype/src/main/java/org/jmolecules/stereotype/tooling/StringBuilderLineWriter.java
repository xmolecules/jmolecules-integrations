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

import lombok.RequiredArgsConstructor;

/**
 * @author Oliver Drotbohm
 */
@RequiredArgsConstructor
public class StringBuilderLineWriter implements LineWriter {

	private final StringBuilder builder = new StringBuilder();

	private final boolean deduplicateLines;

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.LineWriter#write(java.lang.String)
	 */
	@Override
	public void write(String content) {

		if (deduplicateLines && builder.toString().endsWith(content.concat("\n"))) {
			return;
		}

		builder.append(content)
				.append("\n");
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.LineWriter#write(java.lang.String, org.jmolecules.stereotype.tooling.NestingLevel)
	 */
	@Override
	public void write(String content, NestingLevel level) {

		builder.append(getIndentation(level))
				.append(content)
				.append("\n");
	}

	public void writeEmptyLine() {

		if (!builder.toString().endsWith("\n\n")) {
			write("", NestingLevel.ROOT);
		}
	}

	private String getIndentation(NestingLevel level) {
		return "  ".repeat(level.getLevel());
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return builder.toString();
	}
}
