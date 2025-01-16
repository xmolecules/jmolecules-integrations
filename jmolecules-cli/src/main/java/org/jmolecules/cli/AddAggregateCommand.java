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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import org.jmolecules.codegen.ProjectContext;
import org.jmolecules.codegen.generator.JMoleculesCodeGenerator;

/**
 * @author Oliver Drotbohm
 */
@Command(name = "add-aggregate", mixinStandardHelpOptions = true)
@Slf4j
@RequiredArgsConstructor
public class AddAggregateCommand implements Callable<Void> {

	private final JMoleculesCodeGenerator generator;
	private final ProjectContext context;

	@Parameters //
	public String name;

	@Option(names = { "-p", "--package" },
			description = "The package to add the aggregate to.") //
	public String pkg;

	@Option(names = { "-m", "--module" },
			description = "The Spring Modulith module to add the aggregate to. Essentially causes --package to be configured to the module's base package. Requires spring-modulith.enabled to be configured to true.",
			completionCandidates = ModuleReferencePlaceholder.class) //
	public String module;

	static class ModuleReferencePlaceholder implements Iterable<String> {

		/*
		 * (non-Javadoc)
		 * @see java.lang.Iterable#iterator()
		 */
		@Override
		public Iterator<String> iterator() {
			return List.of(getClass().getName()).iterator();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public Void call() throws Exception {

		if (!context.getConfiguration().isSpringModulithEnabled() && pkg == null) {

			log.info("> No Spring Modulith configured, Explicit package (--package, -p) required!");

			System.exit(1);
		}

		var packageName = pkg != null ? pkg : context.getPackageForModule(module);

		generator.generateAggregate(name, packageName);

		return null;
	}
}
