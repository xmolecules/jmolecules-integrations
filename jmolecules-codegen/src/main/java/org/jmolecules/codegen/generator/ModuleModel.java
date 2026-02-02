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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jmolecules.codegen.NamedFile;
import org.jmolecules.codegen.PackageInfo;
import org.jmolecules.codegen.ProjectConfiguration;
import org.jmolecules.codegen.ProjectFiles;
import org.jmolecules.codegen.SourceFile;
import org.jmolecules.codegen.SourceFile.Type;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;

/**
 * @author Oliver Drotbohm
 */
class ModuleModel {

	private static final String MODULITH_BASE_PACKAGE = "org.springframework.modulith";
	private static final String MODULITH_TEST_PACKAGE = MODULITH_BASE_PACKAGE + ".test";

	static final ClassName APPLICATION_MODULE_TEST = ClassName.get(MODULITH_TEST_PACKAGE,
			"ApplicationModuleTest");
	private static final ClassName APPLICATION_MODULE = ClassName.get(MODULITH_BASE_PACKAGE, "ApplicationModule");

	private final String name, basePackage;

	/**
	 * @param name
	 * @param basePackage
	 */
	public ModuleModel(String name, String basePackage) {
		this.name = name;
		this.basePackage = basePackage;
	}

	private String getPackageName() {
		return basePackage + "." + name;
	}

	public List<NamedFile> createFiles(ProjectConfiguration configuration, ProjectFiles workspace) {

		var result = new ArrayList<NamedFile>();

		var packageInfoPath = Path
				.of(ClassUtils.convertClassNameToResourcePath(getPackageName() + ".package-info").concat(".java"));

		var template = """
				@%s
				package %s;
				""";

		var content = template.formatted(APPLICATION_MODULE.topLevelClassName(), getPackageName());

		workspace.writeSource(packageInfoPath, Type.SOURCE, content);

		result.add(new PackageInfo(getPackageName()));

		// Placeholder component
		result.add(toFile(TypeSpec.classBuilder(ClassName.get(getPackageName(), StringUtils.capitalize(name)))
				.addAnnotation(ClassName.get("org.springframework.stereotype", "Component"))
				.build(), Type.SOURCE));

		if (configuration.isSpringModulithEnabled()) {

			result.add(toFile(TypeSpec
					.classBuilder(ClassName.get(getPackageName(), StringUtils.capitalize(name) + "IntegrationTest"))
					.addAnnotation(APPLICATION_MODULE_TEST)
					.build(), Type.TEST_SOURCE));
		}

		return result;
	}

	SourceFile toFile(TypeSpec spec, SourceFile.Type type) {
		return new SourceFile(JavaFile.builder(getPackageName(), spec).indent("\t").build(), type);
	}
}
