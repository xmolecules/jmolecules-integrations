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

package org.jmolecules.archunit.hexagonal.additional.consumer;

import org.jmolecules.architecture.hexagonal.Application;
import org.jmolecules.archunit.hexagonal.additional.provider.ProviderApp;
import org.jmolecules.archunit.hexagonal.additional.provider.ProviderPort;
import org.jmolecules.archunit.hexagonal.additional.provider.ProviderRepo;
import org.jmolecules.archunit.hexagonal.additional.replaced.ReplacedAdapter;
import org.jmolecules.archunit.hexagonal.additional.replaced.ReplacedApp;
import org.jmolecules.archunit.hexagonal.additional.replaced.ReplacedPort;

@Application
public class ConsumerApp {
	ProviderPort providerPort; // not exposed -> violation
	ProviderRepo providerRepo; // additionally exposed -> allowed
	ProviderApp providerApp; // not exposed -> violation
	ReplacedPort replacedPort; // exposed -> allowed
	ReplacedApp replacedApp; // additionally exposed -> allowed
	ReplacedAdapter replacedAdapter; // not exposed -> violation
}
