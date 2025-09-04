/*
 * Copyright 2025 the original author or authors.
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
package org.jmolecules.stereotype.tooling;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.With;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.jmolecules.stereotype.api.Stereotype;
import org.springframework.lang.Nullable;

/**
 * @author Oliver Drotbohm
 */
@With
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SimpleLabelProvider<A, P, T, M, C> implements LabelProvider<A, P, T, M, C> {

	private final Function<A, String> applicationLabel;
	private final Function<P, String> packageLabel;
	private final @Nullable Function<Stereotype, String> stereotypeLabel;
	private final Function<T, String> typeLabel;
	private final BiFunction<M, T, String> methodLabel;
	private final Function<C, String> customLabel;

	/**
	 * @param packageLabel
	 * @param typeLabel
	 * @param methodLabel
	 * @param customLabel
	 */
	public SimpleLabelProvider(Function<A, String> applicationLabel, Function<P, String> packageLabel,
			Function<T, String> typeLabel, BiFunction<M, T, String> methodLabel, Function<C, String> customLabel) {
		this.applicationLabel = applicationLabel;
		this.packageLabel = packageLabel;
		this.typeLabel = typeLabel;
		this.methodLabel = methodLabel;
		this.customLabel = customLabel;
		this.stereotypeLabel = null;
	}

	public static <P, T, M, C> LabelProvider<P, P, T, M, C> forPackage(Function<P, String> packageLabel,
			Function<T, String> typeLabel, BiFunction<M, T, String> methodLabel, Function<C, String> customLabel) {
		return new SimpleLabelProvider<>(packageLabel, packageLabel, typeLabel, methodLabel, customLabel);
	}

	public static <A, P, T, M, C> LabelProvider<A, P, T, M, C> forToString() {
		return new SimpleLabelProvider<>(Object::toString, Object::toString, Object::toString,
				(m, t) -> m.toString(), Object::toString);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.LabelProvider#getApplicationLabel(java.lang.Object)
	 */
	@Override
	public String getApplicationLabel(A application) {
		return applicationLabel.apply(application);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.LabelProvider#getPackageLabel(java.lang.Object)
	 */
	@Override
	public String getPackageLabel(P pkg) {
		return packageLabel.apply(pkg);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.LabelProvider#getTypeLabel(java.lang.Object)
	 */
	@Override
	public String getTypeLabel(T type) {
		return typeLabel.apply(type);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.LabelProvider#getMethodLabel(java.lang.Object, java.lang.Object)
	 */
	@Override
	public String getMethodLabel(M method, T context) {
		return methodLabel.apply(method, context);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.LabelProvider#getCustomLabel(java.lang.Object)
	 */
	@Override
	public String getCustomLabel(C custom) {
		return customLabel.apply(custom);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.LabelProvider#getSterotypeLabel(org.jmolecules.stereotype.api.Stereotype)
	 */
	@Override
	public String getStereotypeLabel(Stereotype stereotype) {

		return stereotypeLabel != null
				? stereotypeLabel.apply(stereotype)
				: LabelProvider.super.getStereotypeLabel(stereotype);
	}
}
