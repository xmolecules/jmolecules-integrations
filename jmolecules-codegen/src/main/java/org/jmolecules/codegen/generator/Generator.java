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

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;

import org.jmolecules.codegen.ProjectConfiguration;
import org.jmolecules.codegen.ProjectContext;
import org.jmolecules.codegen.SourceFile;
import org.jmolecules.codegen.Dependency.Scope;
import org.jmolecules.codegen.SourceFile.Type;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeSpec.Builder;

/**
 * @author Oliver Drotbohm
 */
class Generator {

	private static final String JMOLECULES_PACKAGE = "org.jmolecules";
	private static final String JMOLECULES_DDD_PACKAGE = JMOLECULES_PACKAGE + ".ddd.types";
	private static final String JMOLECULES_EVENTS_PACKAGE = JMOLECULES_PACKAGE + ".event.types";

	private static final String SPRING_PACKAGE = "org.springframework";
	private static final String REPOSITORY_PACKAGE = SPRING_PACKAGE + ".data.repository";
	private static final String DOMAIN_PACKAGE = SPRING_PACKAGE + ".data.domain";

	private static final TypeName UUID_TYPE = ClassName.get("java.util", "UUID");

	private static final TypeName IDENTIFIER_TYPE = ClassName.get(JMOLECULES_DDD_PACKAGE, "Identifier");
	private static final ClassName AGGREGATE_TYPE = ClassName.get(JMOLECULES_DDD_PACKAGE, "AggregateRoot");
	private static final ClassName DOMAIN_EVENT_TYPE = ClassName.get(JMOLECULES_EVENTS_PACKAGE, "DomainEvent");

	private static final ClassName SPRING_DATA_CRUD_REPOSITORY_TYPE = ClassName.get(REPOSITORY_PACKAGE, "CrudRepository");

	private static final ClassName SPRING_BOOT_TEST = ClassName.get(SPRING_PACKAGE + ".boot.test.context",
			"SpringBootTest");

	private final ProjectConfiguration configuration;
	private final ProjectContext context;

	/**
	 * @param configuration
	 */
	public Generator(ProjectConfiguration configuration, ProjectContext context) {

		this.configuration = configuration;
		this.context = context;
	}

	public List<SourceFile> createFiles(AggregateModel model) {

		var identifierType = createIdentifier(model);
		var files = new ArrayList<SourceFile>();

		files.add(model.toFile(createAggregateType(model, identifierType), Type.SOURCE));
		files.add(model.toFile(createAggregateUnitTest(model), Type.TEST_SOURCE));

		if (configuration.isSpringDataEnabled()) {

			files.add(model.toFile(createRepositoryType(model), Type.SOURCE));
			files.add(model.toFile(createRepositoryIntegrationTest(model), Type.TEST_SOURCE));
		}

		return files;
	}

	private TypeSpec createRepositoryIntegrationTest(AggregateModel model) {

		var builder = TypeSpec.classBuilder(model.getRepositoryIntegrationTestName())
				.addAnnotation(
						configuration.isSpringModulithEnabled() ? ModuleModel.APPLICATION_MODULE_TEST : SPRING_BOOT_TEST);

		var repositoryType = model.getRepositoryName();

		if (configuration.isLombokEnabled()) {

			builder = builder.addAnnotation(RequiredArgsConstructor.class);
		} else {

			builder = builder.addMethod(MethodSpec.constructorBuilder()
					.addParameter(repositoryType, "repository")
					.addStatement("this.repository = repository")
					.build());
		}

		return builder
				.addField(
						FieldSpec.builder(repositoryType, "repository").addModifiers(Modifier.PRIVATE, Modifier.FINAL).build())
				.build();
	}

	private static TypeSpec createRepositoryType(AggregateModel model) {

		ClassName repositoryName = model.getRepositoryName();

		ParameterizedTypeName parameterizedCrudRepository = ParameterizedTypeName.get(SPRING_DATA_CRUD_REPOSITORY_TYPE,
				model.getAggregateName(), model.getIdentifierName());

		return TypeSpec.interfaceBuilder(repositoryName)
				.addSuperinterface(parameterizedCrudRepository)
				.build();
	}

	private TypeSpec createAggregateType(AggregateModel model, TypeSpec identifier) {

		var aggregateName = model.getAggregateName();
		var identifierName = model.getIdentifierName();

		var type = TypeSpec.classBuilder(aggregateName)
				.addSuperinterface(ParameterizedTypeName.get(AGGREGATE_TYPE, aggregateName, identifierName))
				.addField(identifierName, "id", Modifier.PRIVATE, Modifier.FINAL)
				.addMethod(MethodSpec.methodBuilder("getId")
						.addModifiers(Modifier.PUBLIC)
						.addAnnotation(Override.class)
						.returns(identifierName)
						.addStatement("return id")
						.build())
				.addType(identifier);

		var constructorBuilder = MethodSpec.constructorBuilder()
				.addCode("$W")
				.addStatement("this.id = new $T(UUID.randomUUID())", identifierName);

		if (configuration.isSpringDataEnabled()) {

			var createdEventName = model.getCreationEventName();

			var createdEventType = createSinglePropertyWrapper(createdEventName, identifierName, "id");

			// Add jMolecules DomainEvent marker interface if present
			if (context.hasDependency("jmolecules-events", Scope.COMPILE)) {
				createdEventType = createdEventType.addSuperinterface(DOMAIN_EVENT_TYPE);
			}

			var superclass = ParameterizedTypeName.get(ClassName.get(DOMAIN_PACKAGE, "AbstractAggregateRoot"),
					aggregateName);

			constructorBuilder = constructorBuilder
					.addStatement("registerEvent(new $T(id))", createdEventName);

			type = type.superclass(superclass).addType(createdEventType.build());
		}

		type.addMethod(constructorBuilder.build());

		return type.build();
	}

	private TypeSpec createAggregateUnitTest(AggregateModel model) {
		return TypeSpec.classBuilder(model.getAggregateUnitTestName()).build();
	}

	private TypeSpec createIdentifier(AggregateModel model) {

		return createSinglePropertyWrapper(model.getIdentifierName(), UUID_TYPE, "id")
				.addSuperinterface(IDENTIFIER_TYPE)
				.build();
	}

	private Builder createSinglePropertyWrapper(ClassName type, TypeName parameterType, String parameterName) {

		var builder = configuration.supportsRecords()
				? TypeSpec.recordBuilder(type)
						.recordConstructor(MethodSpec.constructorBuilder()
								.addParameter(parameterType, parameterName)
								.build())
						.addModifiers(Modifier.PUBLIC)
				: TypeSpec.classBuilder(type)
						.addField(FieldSpec.builder(parameterType, parameterName, Modifier.PRIVATE, Modifier.FINAL).build())
						.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
						.addMethod(MethodSpec.constructorBuilder()
								.addStatement("this.$F = $F", parameterName, parameterName)
								.build());

		return builder
				.addMethod(MethodSpec.methodBuilder("toString")
						.addAnnotation(Override.class)
						.addModifiers(Modifier.PUBLIC)
						.returns(String.class)
						.addStatement("return $N.toString()", parameterName)
						.build());
	}
}
