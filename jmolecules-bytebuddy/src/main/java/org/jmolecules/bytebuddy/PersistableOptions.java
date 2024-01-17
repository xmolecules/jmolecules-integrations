/*
 * Copyright 2021-2024 the original author or authors.
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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.With;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;

/**
 * @author Oliver Drotbohm
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class PersistableOptions {

	@NonNull Class<? extends Annotation> isNewPropertyAnnotation;
	@With Class<?> callbackInterface;
	Class<? extends Annotation>[] callbackAnnotations;

	@SuppressWarnings("unchecked")
	public static PersistableOptions of(Class<? extends Annotation> isNewPropertyAnnotation) {
		return new PersistableOptions(isNewPropertyAnnotation, null, (Class<? extends Annotation>[]) Array
				.newInstance(Class.class, 0));
	}

	boolean hasCallbackInterface() {
		return callbackInterface != null;
	}

	boolean hasCallbackAnnotations() {
		return callbackAnnotations.length > 0;
	}

	@SafeVarargs
	public final PersistableOptions withCallbackAnnotations(Class<? extends Annotation>... callbackAnnotations) {
		return new PersistableOptions(isNewPropertyAnnotation, callbackInterface, callbackAnnotations);
	}
}
