/*******************************************************************************
 * Copyright (c) 2014 Liviu Ionescu.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Liviu Ionescu - initial implementation.
 *******************************************************************************/

package org.eclipse.embedcdt.packs.ui.views;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.embedcdt.packs.core.tree.Leaf;
import org.eclipse.embedcdt.packs.core.tree.PackNode;
import org.eclipse.embedcdt.packs.core.tree.Property;
import org.eclipse.embedcdt.packs.core.tree.Selector;
import org.eclipse.embedcdt.packs.core.tree.Type;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class PacksFilter extends ViewerFilter {

	private IStructuredSelection fSelection;
	private String fSelectorType;

	public void setSelection(String selectorType, IStructuredSelection selection) {
		fSelection = selection;
		fSelectorType = selectorType;
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {

		// 'element' is the node to be tested by the filter.

		if (fSelection == null || fSelection.isEmpty())
			return true; // Nothing selected, all nodes visible

		// System.out.println(parentElement + " " + element);
		Leaf node = (Leaf) element;
		// String nodeType = node.getType();

		// For folder nodes (vendor), if there is no child visible,
		// make the entire parent invisible.
		if (Type.VENDOR.equals(node.getType())) {
			StructuredViewer sviewer = (StructuredViewer) viewer;
			ITreeContentProvider provider = (ITreeContentProvider) sviewer.getContentProvider();
			for (Object child : provider.getChildren(element)) {
				if (select(viewer, element, child))
					return true;
			}
			return false;
		}

		if (!Type.PACKAGE.equals(node.getType())) {
			return true;
		}

		if (!(node instanceof PackNode)) {
			return false;
		}

		// If the node has no restricting conditions at all,
		// then it is not visible.
		if (!((PackNode) node).hasSelectors()) {
			return false;
		}

		List<Selector> selectors = ((PackNode) node).getSelectors();
		List<Selector> filteredSelectors = new LinkedList<>();
		for (Selector selector : selectors) {
			if (fSelectorType.equals(selector.getType())) {
				filteredSelectors.add(selector);
			}
		}

		// If the node has no restricting conditions of the given type,
		// then it is not visible.
		if (filteredSelectors.size() == 0) {
			return false; // true;
		}

		// If the node has selectors, enumerate them and check one by one.
		// If at least one is true, the node is visible.
		for (Selector condition : filteredSelectors) {

			// Enumerate all selections.
			for (Object selectionNode : fSelection.toList()) {

				// Must be leaf, since keywords are leaf.
				if (selectionNode instanceof Leaf) {

					if (isNodeVisible(condition, (Leaf) selectionNode)) {
						// System.out.println("accepted, match " +
						// condition);
						return true;
					}
				}
			}
		}

		// No condition fulfilled, reject.
		return false;
	}

	private boolean isNodeVisible(Selector selector, Leaf selectionNode) {

		// The selector is fetched from the evaluated node.
		String selectorType = selector.getType();

		String selectionNodeType = selectionNode.getType();
		if (Selector.BOARD_TYPE.equals(selectorType)) {

			// Check board selectors (generic vendor string and
			// board name).
			if (Type.VENDOR.equals(selectionNodeType)) {

				// Compare the selected node name with the condition
				// vendor.
				if (selector.getVendor().equals(selectionNode.getName())) {
					return true;
				}
			} else if (Type.BOARD.equals(selectionNodeType)) {

				if (selector.getVendor().equals(selectionNode.getProperty(Property.VENDOR_NAME))
						&& selector.getValue().equals(selectionNode.getName())) {
					return true;
				}
			}

		} else if (Selector.DEVICEFAMILY_TYPE.equals(selectorType)) {

			// Check device selectors (numeric vendor id and
			// family name).
			if (Type.VENDOR.equals(selectionNodeType)) {

				// compare the selectors vendor id with the selection vendor id
				if (selector.getVendorId().equals(selectionNode.getProperty(Property.VENDOR_ID, ""))) {
					return true;
				}
			} else if (Type.FAMILY.equals(selectionNodeType)) {

				// Compare the condition vendor id with the selection vendor id
				// and the condition name with selection family name.
				if (selector.getVendorId().equals(selectionNode.getProperty(Property.VENDOR_ID, ""))
						&& selector.getValue().equals(selectionNode.getName())) {
					return true;
				}
			}
		} else if (Selector.KEYWORD_TYPE.equals(selectorType)) {

			// Check keyword name.
			if (Type.KEYWORD.equals(selectionNode.getType())) {
				if (selector.getValue().equals(selectionNode.getName())) {
					return true;
				}
			}
		}
		return false;
	}
}
