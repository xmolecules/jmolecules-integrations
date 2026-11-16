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
package org.jmolecules.archunit.onion.outside;

import org.jmolecules.architecture.onion.simplified.InfrastructureRing;
import org.jmolecules.archunit.onion.units.b.AppB;
import org.jmolecules.archunit.onion.units.b.DomainB;
import org.jmolecules.archunit.onion.units.b.HelperB;
import org.jmolecules.archunit.onion.units.b.InfraB;

/**
 * An infrastructure ring type outside of any unit.
 */
@InfrastructureRing
class InfraOut {

	InfraB infraB; // infrastructure ring of a unit -> allowed if exposed
	AppB appB; // application ring of a unit -> allowed if exposed
	DomainB domainB; // domain ring of a unit -> violation, never exposed
	HelperB helperB; // unannotated type of a unit -> violation, never exposed
}
