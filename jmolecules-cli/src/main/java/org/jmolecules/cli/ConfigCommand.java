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
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

import org.jmolecules.codegen.ProjectContext;

/**
 * @author Oliver Drotbohm
 */
@Command(name = "config")
@RequiredArgsConstructor
public class ConfigCommand implements Callable<Void> {

	private final ProjectContext context;

	/*
	 * (non-Javadoc)
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public Void call() throws Exception {

		System.out.println(context.getConfiguration());

		return null;
	}
}
