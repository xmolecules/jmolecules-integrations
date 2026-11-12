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
@Hexagon(name = "Declared", exposes = Exposure.PRIMARY_PORTS, refersVia = Referrer.SECONDARY_ADAPTERS)
package org.jmolecules.archunit.hexagonal.exposure.declared;

import org.jmolecules.architecture.hexagonal.Hexagon;
import org.jmolecules.architecture.hexagonal.Hexagon.Exposure;
import org.jmolecules.architecture.hexagonal.Hexagon.Referrer;
