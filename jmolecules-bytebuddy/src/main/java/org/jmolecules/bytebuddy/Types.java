/*
 * Copyright 2022 the original author or authors.
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

import java.lang.annotation.Annotation;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Helper to access jMolecules types depending on whether they're on the classpath or not.
 *
 * @author Oliver Drotbohm
 */
class Types {

	public static Class<? extends Annotation> DOMAIN_EVENT_HANDLER;

	static {
		DOMAIN_EVENT_HANDLER = loadIfPresent("org.jmolecules.event.annotation.DomainEventHandler");
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private static <T> Class<T> loadIfPresent(String type) {

		ClassLoader classLoader = Types.class.getClassLoader();

		if (!ClassUtils.isPresent(type, classLoader)) {
			return null;
		}

		try {
			return (Class<T>) ClassUtils.forName(type, classLoader);
		} catch (Exception o_O) {
			return null;
		}
	}
}
