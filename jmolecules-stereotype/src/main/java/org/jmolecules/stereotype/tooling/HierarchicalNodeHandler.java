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

import net.minidev.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jmolecules.stereotype.api.Stereotype;

/**
 * @author Oliver Drotbohm
 */
public class HierarchicalNodeHandler<A, P, T, M, C> implements NodeHandler<A, P, T, M, C> {

	public static final String ICON = "icon";
	public static final String TEXT = "text";

	private final Node root;
	private final LabelProvider<A, P, T, M, C> labels;
	private final BiConsumer<Node, C> customHandler;
	private Node current;

	public HierarchicalNodeHandler(LabelProvider<A, P, T, M, C> labels, BiConsumer<Node, C> customHandler) {

		this.labels = labels;
		this.root = new Node(null);
		this.customHandler = customHandler;
		this.current = root;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.NodeHandler#handleApplication(java.lang.Object)
	 */
	@Override
	public void handleApplication(A application) {
		this.root.withAttribute(TEXT, labels.getApplicationLabel(application));
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.NodeHandler#handlePackage(java.lang.Object, org.jmolecules.stereotype.tooling.NodeContext)
	 */
	@Override
	public void handlePackage(P pkg, NodeContext context) {

		addChild(node -> node
				.withAttribute(ICON, "fa-package")
				.withAttribute(TEXT, labels.getPackageLabel(pkg)));
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.NodeHandler#handleStereotype(org.jmolecules.stereotype.api.Stereotype, org.jmolecules.stereotype.tooling.NestingLevel)
	 */
	@Override
	public void handleStereotype(Stereotype stereotype, NodeContext context) {

		addChild(node -> node.withAttribute(ICON, "fa-stereotype")
				.withAttribute(TEXT, labels.getStereotypeLabel(stereotype)));
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.NodeHandler#handleType(java.lang.Object, org.jmolecules.stereotype.tooling.NestingLevel)
	 */
	@Override
	public void handleType(T type, NodeContext context) {
		addChild(node -> node.withAttribute(TEXT, labels.getTypeLabel(type)));
	}

	public Node createNested() {
		return new Node(this.current);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.NodeHandler#handleMethod(java.lang.Object, org.jmolecules.stereotype.tooling.NestingLevel, boolean)
	 */
	@Override
	public void handleMethod(M method, MethodNodeContext<T> context) {
		addChildFoo(node -> node.withAttribute("title", labels.getMethodLabel(method, context.getContextualType())));
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.NodeHandler#handleCustom(java.lang.Object, org.jmolecules.stereotype.tooling.NodeContext)
	 */
	@Override
	public void handleCustom(C custom, NodeContext context) {
		addChild(node -> customHandler.accept(node, custom));
	}

	/*
	 * (non-Javadoc)
	 * @see org.jmolecules.stereotype.tooling.NodeHandler#postGroup()
	 */
	@Override
	public void postGroup() {
		this.current = this.current.parent;
	}

	private void addChild(Consumer<Node> consumer) {
		this.current = addChildFoo(consumer);
	}

	private Node addChildFoo(Consumer<Node> consumer) {

		var node = new Node(this.current);
		consumer.accept(node);

		this.current.children.add(node);

		return node;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return render(root).toJSONString();
	}

	private JSONObject render(Node node) {

		var object = new JSONObject();

		object.putAll(node.attributes);

		if (!node.children.isEmpty()) {
			object.put("children", node.children.stream().map(this::render).toList());
		}

		return object;
	}

	public static class Node {

		private final Node parent;
		private final Map<String, Object> attributes;
		private final List<Node> children;

		Node(Node parent) {
			this.parent = parent;
			this.attributes = new LinkedHashMap<>();
			this.children = new ArrayList<>();
		}

		public Node withAttribute(String key, Object value) {
			this.attributes.put(key, value);
			return this;
		}
	}
}
