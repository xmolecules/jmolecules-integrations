/*
 * Copyright 2021 the original author or authors.
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
package org.jmolecules.bytebuddy;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;

/**
 * @author Oliver Drotbohm
 */
@With
@AllArgsConstructor
@RequiredArgsConstructor(staticName = "of")
class PersistableOptions {

	final @NonNull Class<? extends Annotation> isNewPropertyAnnotation;
	Class<?> callbackInterface;

	@SuppressWarnings("unchecked") Class<? extends Annotation>[] callbackAnnotations = (Class<? extends Annotation>[]) Array
			.newInstance(Class.class, 0);

	@SafeVarargs
	public final PersistableOptions withCallbackAnnotations(Class<? extends Annotation>... callbackAnnotations) {
		return new PersistableOptions(isNewPropertyAnnotation, callbackInterface, callbackAnnotations);
	}
}
