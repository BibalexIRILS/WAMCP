//   Copyright 2013 Bibliotheca Alexandrina, Wellcome Trust Library
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

package org.bibalex.sdxe.suggest.model;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import org.bibalex.sdxe.suggest.model.dom.SugtnDeclNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnElementNode;
import org.bibalex.sdxe.suggest.model.particle.SugtnParticleNode;

import com.sun.org.apache.bcel.internal.generic.ReturnaddressType;


/**
 * Class is a Swing Tree Model that holds the schema tree nodes.
 * 
 * Schema model is the same for all the application as no one changes it. 
 * 
 */

@SuppressWarnings("serial")
public class SugtnTreeModel extends DefaultTreeModel implements Cloneable {
	
	/**
	 * Finds the most shallow tree node with the name passed, under the passed node 
	 * (passed node is not included in the search scope).
	 * 
	 * It first searches in the direct descendants of the node. 
	 * If no node matched and returnOnlyDirectDescendents is false, it searches in each tree of those descendants. 
	 * 
	 * @param localName
	 * 			local name of the node to be found.
	 * @param returnOnlyDirectDescendents
	 *			True if search will be done in descendants of the node passed as argument only.
	 *			False if search will include each tree of descendants of the node passed as argument.  
	 * 
	 * @return The node with local name passed as argument.
	 *
	 */
	public static SugtnDeclNode findNodeByLocalNameRecursive(SugtnTreeNode node,
			String localName, boolean returnOnlyDirectDescendents) {
		
		SugtnDeclNode result = null;
		
		// this loop to search in the direct descendants of the node only
		for (int i = 0; (result == null) && (i < node.getChildCount()); ++i) {
			
			TreeNode child = node.getChildAt(i);
			
			if ((child instanceof SugtnDeclNode)
					&& localName.equals(((SugtnDeclNode) child).getLocalName())) {
				
				result = (SugtnDeclNode) child;
				
			}
		}
		
		// this loop to search in the tree of each descendant if no matching node was found from the previous loop
		for (int i = 0; (result == null) && (i < node.getChildCount()); ++i) {
			
			TreeNode child = node.getChildAt(i);
			
			if ((!returnOnlyDirectDescendents && (child instanceof SugtnElementNode))
					|| (child instanceof SugtnParticleNode)) {
				
				result = findNodeByLocalNameRecursive(
						(SugtnTreeNode) child, localName, returnOnlyDirectDescendents);
				
			}
		}
		
		return result;
		
	}
	
	/**
	 * Constructs a new tree model with the node passed as argument being the root of the new tree.
	 * 
	 * @param root
	 * 			Root node for the constructed tree model.
	 * 	
	 */
	public SugtnTreeModel(SugtnElementNode root) {
		super(root);
	}
	
	/**
	 * Clone the element. The returned object's root is a cloned {@linkplain SugtnTreeNode tree node} of the element's root. 
	 * 
	 * @return A cloned object of the element.
	 */
	@Override
	public Object clone() {
		SugtnTreeModel result = null;
		try {
			result = (SugtnTreeModel) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		result.root = (TreeNode) ((SugtnTreeNode) this.root).clone();
		return result;
	}
	
	/**
	 * Finds the node with a certain ID. 
	 * 
	 * Each node has a serial ID. Searches for the node in the tree starting from root of the node 
	 * (root and siblings of the node).
	 * 
	 * @param nodeId
	 * 			ID of the node to be found.
	 * 
	 * @return The node with ID passed as argument.
	 *
	 * @see #findNodeByIdRecursive(SugtnTreeNode, int)
	 */
	public SugtnTreeNode findNodeById(int nodeId) {
		SugtnTreeNode root, result = null;
		root = (SugtnTreeNode) this.getRoot();
		result = this.findNodeByIdRecursive(root, nodeId);
		return result;
	}
	
	/**
	 * Finds the node with a certain ID recursively. 
	 * 
	 * Each node has a serial ID. Searches for the node in the tree starting from the node passed as argument.
	 * 
	 * @param node
	 * 			Node where search will start from.
	 * @param nodeId
	 * 			ID of the node to be found.
	 * 
	 * @return The node with ID passed as arguments.
	 */
	private SugtnTreeNode findNodeByIdRecursive(SugtnTreeNode node, int nodeId) {
		SugtnTreeNode result = null;
		if (node.getNodeId() == nodeId) {
			result = node;
		} else {
			for (int i = 0; i < node.getChildCount(); ++i) {
				result = this.findNodeByIdRecursive((SugtnTreeNode) node
						.getChildAt(i), nodeId);
				if (result != null) {
					break;
				}
			}
		}
		return result;
	}
	
	
	/**
	 * Finds the node with a certain local name. 
	 * 
	 * Searches for the node in the tree starting from root of the node (root of the node is not included, only siblings of the node and trees of them).
	 * 
	 * @param localName
	 * 			local name of the node to be found.
	 * @param returnOnlyDirectDescendents
	 *			True if search will be done in siblings of this node only.
	 *			False if search will include each tree of the siblings of this node.  

	 * 
	 * @return The node with local name passed as argument.
	 *
	 * @see #findNodeByLocalNameRecursive(SugtnTreeNode, String, boolean)
	 */
	public SugtnDeclNode findNodeByLocalName(String localName, boolean returnOnlyDirectDescendents) {
		SugtnTreeNode root;
		
		SugtnDeclNode result = null;
		root = (SugtnTreeNode) this.getRoot();
		
		// We never want the root, do we? (recursive elements)
		
// if (localName.equals(((SugtnElementNode) root).getLocalName())) {
//
// result = (SugtnDeclNode) root;
//
// } else {
		
		result = findNodeByLocalNameRecursive(root, localName, returnOnlyDirectDescendents);
// }
		
		return result;
		
	}
	
}
