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
package org.jmolecules.archunit.layered.domain;

import org.jmolecules.archunit.layered.AppByAnnotation;
import org.jmolecules.archunit.layered.InfraByAnnotation;
import org.jmolecules.archunit.layered.UiByAnnotation;
import org.jmolecules.archunit.layered.app.AppType;
import org.jmolecules.archunit.layered.infra.InfraType;
import org.jmolecules.archunit.layered.ui.UiType;

/**
 * @author Oliver Drotbohm
 */
public class DomainType {

	UiType ui;
	UiByAnnotation uiByAnnotation;

	AppType app;
	AppByAnnotation appByAnnotation;

	InfraType infra;
	InfraByAnnotation infraByAnnotation;
}
