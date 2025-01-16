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
package org.jmolecules.cli;

import picocli.AutoComplete;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * @author Oliver Drotbohm
 */
public class AutocompletionGenerationTest {

	@Test
	void generateAutoCompletionScript() throws Exception {

		Stream.of("jm", "jmn").forEach(jm -> {

			try {

				var completionScript = AutoComplete.bash(jm, CliApplication.createCommandLine("integration-test"));
				var placeholder = AddAggregateCommand.ModuleReferencePlaceholder.class.getName();

				completionScript = completionScript.replace("\"%s\"".formatted(placeholder),
						"$([ -f .jm/modules ] && jq -c -r 'keys | join(\" \")' .jm/modules)");

				Files.writeString(Path.of("target/%s.completion".formatted(jm)), completionScript);

			} catch (Exception o_O) {
				throw new RuntimeException(o_O);
			}
		});

	}
}
