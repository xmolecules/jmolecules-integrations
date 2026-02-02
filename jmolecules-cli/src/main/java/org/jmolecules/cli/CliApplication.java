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
import picocli.CommandLine.IVersionProvider;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.jmolecules.cli.CliApplication.PropertiesVersionProvider;
import org.jmolecules.cli.core.BuildSystem;
import org.jmolecules.cli.core.DefaultProjectContext;
import org.jmolecules.cli.core.MetadataCache;
import org.jmolecules.codegen.ProjectFiles;
import org.jmolecules.codegen.generator.JMoleculesCodeGenerator;

/**
 * @author Oliver Drotbohm
 */
@Command(name = "jm",
		mixinStandardHelpOptions = true,
		versionProvider = PropertiesVersionProvider.class,
		description = "⚗️ jMolecules Command Line ♥️")
public class CliApplication {

	static {

		System.setProperty("picocli.trace", "OFF");

		try {

			LogManager.getLogManager()
					.readConfiguration(CliApplication.class.getResourceAsStream("/logging.properties"));

		} catch (IOException | SecurityException | ExceptionInInitializerError ex) {

			Logger.getLogger(CliApplication.class.getName())
					.log(Level.SEVERE, "Failed to read logging.properties file", ex);
		}
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

		var initCommand = new InitCommand(buildSystem, context, cache);

		commandLine.addSubcommand(initCommand);
		commandLine.addSubcommand(new AddAggregateCommand(code, context));
		commandLine.addSubcommand(new AddModuleCommand(code, buildSystem, initCommand));
		commandLine.addSubcommand(new ConfigCommand(context));

		return commandLine;
	}

	static class PropertiesVersionProvider implements IVersionProvider {

		private static final String VERSION;

		static {

			var props = new Properties();

			try {

				props.load(PropertiesVersionProvider.class.getResourceAsStream("/version.properties"));

				VERSION = props.getProperty("version");

			} catch (IOException o_O) {
				throw new RuntimeException(o_O);
			}
		}

		/*
		 * (non-Javadoc)
		 * @see picocli.CommandLine.IVersionProvider#getVersion()
		 */
		@Override
		public String[] getVersion() throws Exception {
			return new String[] { VERSION };
		}
	}
}
