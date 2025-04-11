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

import java.util.SortedSet;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jmolecules.stereotype.api.Stereotype;
import org.jmolecules.stereotype.catalog.StereotypeGroup;

/**
 * @author Oliver Drotbohm
 */
public class AsciiArtNodeHandler<P, T, M> implements NodeHandler<P, T, M> {

	private final LineWriter writer;
	private final Function<P, String> packageHeadline;
	private final BiFunction<Stereotype, SortedSet<StereotypeGroup>, String> stereotypeHeadline;
	private final Function<T, String> typeHeadline;
	private final Function<M, String> methodHeadline;

	/**
	 * @param stereotypeHeadline
	 * @param typeHeadline
	 * @param methodHeadline
	 */
	public AsciiArtNodeHandler(LineWriter writer, Function<P, String> packageHeadline,
			BiFunction<Stereotype, SortedSet<StereotypeGroup>, String> stereotypeHeadline,
			Function<T, String> typeHeadline,
			Function<M, String> methodHeadline) {

		this.writer = writer;
		this.packageHeadline = packageHeadline;
		this.stereotypeHeadline = stereotypeHeadline;
		this.typeHeadline = typeHeadline;
		this.methodHeadline = methodHeadline;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.NodeHandler#handlePackage(java.lang.Object, org.jmolecules.stereotype.tooling.NodeContext)
	 */
	@Override
	public NestingLevel handlePackage(P pkg, NestingLevel level) {

		render("□ ", packageHeadline.apply(pkg), level);

		return level.increase();
	}

	@Override
	public NestingLevel handleStereotypeGroup(Stereotype stereotype, SortedSet<StereotypeGroup> groups,
			StereotypeGrouped<?> grouped, NestingLevel level) {

		if (grouped.hasOnlyOtherStereotype()) {
			return level;
		}

		render("+ ", stereotypeHeadline.apply(stereotype, groups), level);

		return level.increase();
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.NodeHandler#postStereotypeGroup(org.jmolecules.stereotype.tooling.StereotypeGrouped)
	 */
	@Override
	public void postStereotypeGroup(StereotypeGrouped<?> grouped) {
		writer.writeEmptyLine();
	}

	@Override
	public NestingLevel handleType(T type, NestingLevel level) {
		render("- ", typeHeadline.apply(type), level);
		return level.increase();
	}

	@Override
	public NestingLevel handleMethod(M method, StereotypeGrouped<M> grouped, NestingLevel level) {
		render("- ", methodHeadline.apply(method), level);
		return level.increase();
	}

	private void render(String prefix, Object source, NestingLevel level) {
		writer.write(prefix + source.toString(), level);
	}
}
