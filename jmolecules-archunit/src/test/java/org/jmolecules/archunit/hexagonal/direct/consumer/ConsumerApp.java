/*
 * Copyright 2026 the original author or authors.
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

package org.jmolecules.archunit.hexagonal.direct.consumer;

import org.jmolecules.architecture.hexagonal.Application;
import org.jmolecules.archunit.hexagonal.direct.plain.Plain;
import org.jmolecules.archunit.hexagonal.direct.provider.ProviderAdapter;
import org.jmolecules.archunit.hexagonal.direct.provider.ProviderApp;
import org.jmolecules.archunit.hexagonal.direct.provider.ProviderPort;
import org.jmolecules.archunit.hexagonal.direct.provider.ProviderSecondaryPort;

@Application
public class ConsumerApp {
	ProviderApp providerApp; // direct application access
	ProviderPort providerPort; // direct application access
	ProviderAdapter providerAdapter; // application code must not refer to other units -> violation
	ProviderSecondaryPort providerSecondaryPort; // application code must not refer to other units -> violation
	Plain plain; // unit not using hexagonal stereotypes, direct application access
}
