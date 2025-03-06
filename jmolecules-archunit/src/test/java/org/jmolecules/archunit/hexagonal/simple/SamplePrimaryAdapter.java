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
package org.jmolecules.archunit.hexagonal.simple;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;

/**
 * @author Oliver Drotbohm
 */
@PrimaryAdapter
class SamplePrimaryAdapter {

	// Valid
	SamplePort port;
	SamplePrimaryPort primaryPort;

	// Invalid
	SampleApplication application;
	SampleSecondaryPort secondaryPort;
}
