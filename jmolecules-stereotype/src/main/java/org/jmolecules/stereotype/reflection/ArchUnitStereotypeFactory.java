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

import lombok.RequiredArgsConstructor;

import org.jmolecules.stereotype.api.StereotypeFactory;
import org.jmolecules.stereotype.api.Stereotypes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaPackage;

/**
 * @author Oliver Drotbohm
 */
@RequiredArgsConstructor
public class ArchUnitStereotypeFactory implements StereotypeFactory<JavaPackage, JavaClass, JavaMethod> {

	private final ReflectionStereotypeFactory delegate;

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.api.StereotypeFactory#fromPackage(java.lang.Object)
	 */
	@Override
	public Stereotypes fromPackage(JavaPackage pkg) {

		var classes = pkg.getClasses();

		if (classes.isEmpty()) {
			return Stereotypes.NONE;
		}

		return delegate.fromPackage(classes.iterator().next().reflect().getPackage());
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.api.StereotypeFactory#fromType(java.lang.Object)
	 */
	@Override
	public Stereotypes fromType(JavaClass type) {
		return delegate.fromType(type.reflect());
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.api.StereotypeFactory#fromMethod(java.lang.Object)
	 */
	@Override
	public Stereotypes fromMethod(JavaMethod method) {
		return delegate.fromMethod(method.reflect());
	}

}
