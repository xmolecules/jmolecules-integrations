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
package org.jmolecules.stereotype.reflection;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jmolecules.stereotype.tooling.SimpleLabelProvider;
import org.jmolecules.stereotype.tooling.StructureProvider;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaPackage;

/**
 * @author Oliver Drotbohm
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ArchUnitStructureProvider
		implements StructureProvider.SimpleStructureProvider<JavaPackage, JavaPackage, JavaClass, JavaMethod> {

	private final boolean asSinglePackage;

	public static ArchUnitStructureProvider asSinglePackage() {
		return new ArchUnitStructureProvider(true);
	}

	public static ArchUnitStructureProvider asIndividualPackages() {
		return new ArchUnitStructureProvider(false);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.StructureProvider#extractPackages(java.lang.Object)
	 */
	@Override
	public Collection<JavaPackage> extractPackages(JavaPackage pkg) {

		if (asSinglePackage) {
			return List.of(pkg);
		}

		var result = new ArrayList<>(pkg.getSubpackagesInTree());
		result.add(pkg);

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.StructureProvider.SimpleStructureProvider#extractTypes(java.lang.Object)
	 */
	@Override
	public Collection<JavaClass> extractTypes(JavaPackage pkg) {

		var source = asSinglePackage ? pkg.getClassesInPackageTree() : pkg.getClasses();

		return source.stream()
				.filter(it -> !it.isArray())
				.toList();
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.StructureProvider#extractMethods(java.lang.Object)
	 */
	@Override
	public Collection<JavaMethod> extractMethods(JavaClass type) {
		return type.getAllMethods();
	}

	public SimpleLabelProvider<JavaPackage, JavaPackage, JavaClass, JavaMethod, Object> getLabelProvider() {

		return new SimpleLabelProvider<>(JavaPackage::getName, JavaPackage::getName, JavaClass::getName,
				(m, t) -> m.getName(), Object::toString);
	}
}
