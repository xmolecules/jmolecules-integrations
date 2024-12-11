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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jmolecules.stereotype.api.Stereotype;
import org.jspecify.annotations.Nullable;

/**
 * A {@link NodeHandler} to render the project tree as ASCII art.
 *
 * @author Oliver Drotbohm
 */
public class AsciiArtNodeHandler<A, P, T, M, C> implements NodeHandler<A, P, T, M, C> {

	private final LabelProvider<A, P, T, M, C> labelProvider;
	private final LineWriter writer;
	private final List<NodeContext> contexts;

	private NestingLevel level;

	/**
	 * Creates a new {@link AsciiArtNodeHandler} for the given {@link LabelProvider} and {@link LineWriter}.
	 *
	 * @param labelProvider must not be {@literal null}.
	 * @param writer must not be {@literal null}.
	 */
	public AsciiArtNodeHandler(LabelProvider<A, P, T, M, C> labelProvider, LineWriter writer) {

		this.writer = writer;
		this.labelProvider = labelProvider;
		this.level = NestingLevel.ROOT;
		this.contexts = new ArrayList<>();
	}

	/**
	 * Creates a new {@link AsciiArtNodeHandler} for the given {@link LabelProvider} defaulting the {@link LineWriter} to
	 * a {@link StringBuilderLineWriter}.
	 *
	 * @param writer must not be {@literal null}.
	 * @param labelProvider must not be {@literal null}.
	 */
	public AsciiArtNodeHandler(LabelProvider<A, P, T, M, C> labelProvider) {
		this(labelProvider, new StringBuilderLineWriter(true));
	}

	/**
	 * @return the writer
	 */
	public LineWriter getWriter() {
		return writer;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.NodeHandler#handleApplication(java.lang.Object)
	 */
	@Override
	public void handleApplication(A application) {
		renderAndNest("■ ", labelProvider.getApplicationLabel(application), () -> true);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.NodeHandler#handlePackage(java.lang.Object, org.jmolecules.stereotype.tooling.NodeContext)
	 */
	@Override
	public void handlePackage(P pkg, NodeContext context) {
		renderAndNest("□ ", labelProvider.getPackageLabel(pkg), context);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.NodeHandler#handleType(java.lang.Object)
	 */
	@Override
	public void handleType(T type, NodeContext context) {
		renderAndNest("╴", labelProvider.getTypeLabel(type), context);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.NodeHandler#handleMethod(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void handleMethod(M method, MethodNodeContext<T> context) {
		render("╴", labelProvider.getMethodLabel(method, context.getContextualType()), context);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.NodeHandler#handleStereotype(org.jmolecules.stereotype.api.Stereotype)
	 */
	@Override
	public void handleStereotype(Stereotype stereotype, NodeContext context) {
		renderAndNest("┬⊙ ", labelProvider.getStereotypeLabel(stereotype), context);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.NodeHandler#postGroup()
	 */
	@Override
	public void postGroup() {

		var current = contexts.get(contexts.size() - 1);

		if (current.isLast()) {
			render("", null, current);
		}

		this.level = level.decrease();
		this.contexts.remove(level.getLevel());
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.ExtendedNodeHandler#handleCustom(java.lang.Object, org.jmolecules.stereotype.tooling.NestingLevel)
	 */
	@Override
	public void handleCustom(C custom, NodeContext context) {
		renderAndNest("○ ", labelProvider.getCustomLabel(custom), context);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.NodeHandler#getTypeComparator()
	 */
	@Override
	public @Nullable Comparator<T> getTypeComparator() {
		return Comparator.comparing(labelProvider::getTypeLabel);
	}

	private void render(String prefix, Object source, NodeContext context) {

		if (!level.isRoot() && context != null) {

			var indentation = "";

			for (int i = 1; i < level.getLevel(); i++) {
				indentation += contexts.get(i).isLast() ? "  " : "│ ";
			}

			prefix = indentation + (source == null ? "" : (context.isLast() ? "╰─" : "├─").concat(prefix));
		}

		var content = source == null ? "" : source.toString();

		if (content.isBlank()) {
			writer.write(prefix.replaceAll("\\s+$", ""));
		} else {
			writer.write(prefix + content);
		}
	}

	private void renderAndNest(String prefix, Object source, NodeContext context) {

		render(prefix, source, context);

		this.level = level.increase();
		this.contexts.add(context);
	}
}
