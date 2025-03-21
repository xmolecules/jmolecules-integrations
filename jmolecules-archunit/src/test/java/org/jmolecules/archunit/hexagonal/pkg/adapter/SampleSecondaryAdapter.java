/*
 * Copyright 2022-2025 the original author or authors.
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
package org.jmolecules.archunit.hexagonal.pkg.adapter;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.archunit.hexagonal.pkg.adapter.primary.SamplePrimaryAdapterPackage;
import org.jmolecules.archunit.hexagonal.pkg.adapter.secondary.SampleSecondaryAdapterPackage;
import org.jmolecules.archunit.hexagonal.pkg.application.SampleApplication;
import org.jmolecules.archunit.hexagonal.pkg.port.SamplePort;
import org.jmolecules.archunit.hexagonal.pkg.port.SamplePrimaryPort;
import org.jmolecules.archunit.hexagonal.pkg.port.SampleSecondaryPort;
import org.jmolecules.archunit.hexagonal.pkg.port.primary.SamplePrimaryPortPackage;
import org.jmolecules.archunit.hexagonal.pkg.port.secondary.SampleSecondaryPortPackage;

/**
 * @author Oliver Drotbohm
 */
@SecondaryAdapter
public abstract class SampleSecondaryAdapter implements SampleSecondaryPort {

	// Valid
	SamplePort port;
	SampleAdapter adapter;
	SampleSecondaryAdapter secondaryAdapter;
	SampleSecondaryAdapterPackage sampleSecondaryAdapterPackage;
	SampleSecondaryPort secondaryPort;
	SampleSecondaryPortPackage secondaryPortPackage;

	// Invalid in strict
	SampleApplication application;

	// Invalid
	SamplePrimaryAdapter primaryAdapter;
	SamplePrimaryAdapterPackage primaryAdapterPackage;
	SamplePrimaryPort primaryPort;
	SamplePrimaryPortPackage primaryPortPackage;
}
