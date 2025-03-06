/*
 * Copyright 2021-2025 the original author or authors.
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
package org.jmolecules.archunit.onion.simple.app;

import org.jmolecules.archunit.onion.simple.DomainByAnnotation;
import org.jmolecules.archunit.onion.simple.InfraByAnnotation;
import org.jmolecules.archunit.onion.simple.domain.DomainType;
import org.jmolecules.archunit.onion.simple.infra.InfraType;

/**
 * @author Oliver Drotbohm
 */
public class AppType {

	DomainType domain;
	DomainByAnnotation domainByAnnotation;

	InfraType infra;
	InfraByAnnotation infraByAnnotation;
}
