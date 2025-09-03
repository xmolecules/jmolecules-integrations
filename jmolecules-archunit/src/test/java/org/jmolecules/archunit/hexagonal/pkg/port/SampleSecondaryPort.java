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
package org.jmolecules.archunit.hexagonal.pkg.port;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.archunit.hexagonal.pkg.adapter.SampleAdapter;
import org.jmolecules.archunit.hexagonal.pkg.adapter.SamplePrimaryAdapter;
import org.jmolecules.archunit.hexagonal.pkg.adapter.SampleSecondaryAdapter;
import org.jmolecules.archunit.hexagonal.pkg.adapter.primary.SamplePrimaryAdapterPackage;
import org.jmolecules.archunit.hexagonal.pkg.adapter.secondary.SampleSecondaryAdapterPackage;
import org.jmolecules.archunit.hexagonal.pkg.application.SampleApplication;
import org.jmolecules.archunit.hexagonal.pkg.port.primary.SamplePrimaryPortPackage;
import org.jmolecules.archunit.hexagonal.pkg.port.secondary.SampleSecondaryPortPackage;

/**
 * @author Oliver Drotbohm
 */
@SecondaryPort
public interface SampleSecondaryPort {

	// Valid

	SamplePort port();

	SampleSecondaryPort secondaryPort();

	SampleSecondaryPortPackage secondaryPortPackage();

	// Invalid

	SampleApplication application();

	SamplePrimaryPort primaryPort();

	SamplePrimaryPortPackage primaryPortPackage();

	SampleAdapter adapter();

	SamplePrimaryAdapter primaryAdapter();

	SamplePrimaryAdapterPackage primaryAdapterPackage();

	SampleSecondaryAdapter secondaryAdapter();

	SampleSecondaryAdapterPackage secondaryAdapterPackage();

}
