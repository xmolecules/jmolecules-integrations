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
package org.jmolecules.jackson;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * API to abstract annotation detection depending on whether were running with Spring on the classpath or not. In the
 * former case, we can use its synthesized annotation detection support to detect the annotations in composed annotation
 * scenarios.
 *
 * @author Oliver Drotbohm
 */
abstract class AnnotationDetector {

	public static AnnotationDetector getAnnotationDetector() {

		if (isPresent("org.springframework.core.annotation.AnnotatedElementUtils")) {
			return new SpringAnnotationDetector();
		} else {
			return new SimpleAnnotationDetector();
		}
	}

	abstract boolean hasAnnotation(AnnotatedElement element, Class<? extends Annotation> annotationType);

	private static boolean isPresent(String type) {

		try {
			Class.forName(type);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	private static class SimpleAnnotationDetector extends AnnotationDetector {

		/*
		 * (non-Javadoc)
		 * @see org.jmolecules.jackson.AnnotationDetector#hasAnnotation(java.lang.reflect.AnnotatedElement, java.lang.Class)
		 */
		@Override
		public boolean hasAnnotation(AnnotatedElement element, Class<? extends Annotation> annotationType) {
			return element.getAnnotation(annotationType) != null;
		}
	}

	private static class SpringAnnotationDetector extends AnnotationDetector {

		/*
		 * (non-Javadoc)
		 * @see org.jmolecules.jackson.AnnotationDetector#hasAnnotation(java.lang.reflect.AnnotatedElement, java.lang.Class)
		 */
		@Override
		public boolean hasAnnotation(AnnotatedElement element, Class<? extends Annotation> annotationType) {
			return AnnotatedElementUtils.hasAnnotation(element, annotationType);
		}
	}
}
