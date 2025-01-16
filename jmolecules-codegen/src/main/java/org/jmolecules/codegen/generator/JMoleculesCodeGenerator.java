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
package org.jmolecules.codegen.generator;

import java.util.List;

import org.jmolecules.codegen.ProjectContext;
import org.jmolecules.codegen.SourceFile;

/**
 * @author Oliver Drotbohm
 */
public class JMoleculesCodeGenerator {

	private final ProjectContext context;

	/**
	 * @param context
	 */
	public JMoleculesCodeGenerator(ProjectContext context) {
		this.context = context;
	}

	public List<SourceFile> generateAggregate(String name, String pkg) {

		var model = new AggregateModel(name, pkg);
		var generator = new Generator(context.getConfiguration(), context);

		var result = generator.createFiles(model);

		for (SourceFile file : result) {
			context.getFiles().writeSource(file);
		}

		return result;
	}

	public void createModule(String name) {

		var configuration = context.getConfiguration();
		var model = new ModuleModel(name, configuration.getBasePackage());
		var files = context.getFiles();

		model.createJavaFiles(configuration, files).forEach(files::writeSource);
	}
}
