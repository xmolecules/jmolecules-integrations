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

import picocli.CommandLine;
import picocli.CommandLine.Command;

import org.jmolecules.cli.core.BuildSystem;
import org.jmolecules.cli.core.DefaultProjectContext;
import org.jmolecules.cli.core.MetadataCache;
import org.jmolecules.codegen.ProjectFiles;
import org.jmolecules.codegen.generator.JMoleculesCodeGenerator;

/**
 * @author Oliver Drotbohm
 */
@Command(name = "jm", mixinStandardHelpOptions = true)
public class CliApplication {

	static {
		System.setProperty("picocli.trace", "OFF");
	}

	public static void main(String[] args) throws Exception {

		var commandLine = createCommandLine(".");

		System.exit(commandLine.execute(args));
	}

	public static CommandLine createCommandLine(String root) throws Exception {

		var files = new ProjectFiles(root);
		var cache = new MetadataCache(files);
		var buildSystem = BuildSystem.of(files, cache);
		var context = new DefaultProjectContext(files, cache, buildSystem);

		var code = new JMoleculesCodeGenerator(context);

		var commandLine = new CommandLine(new CliApplication());
		commandLine.addSubcommand(new AddAggregateCommand(code, context));
		commandLine.addSubcommand(new AddModuleCommand(code));
		commandLine.addSubcommand(new ConfigCommand(context));
		commandLine.addSubcommand(new InitCommand(buildSystem, context, cache));

		return commandLine;
	}
}
