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

import javax.swing.tree.DefaultMutableTreeNode;

import org.bibalex.sdxe.suggest.model.dom.SugtnAttributeNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnDeclNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnElementNode;

import com.sun.org.apache.xalan.internal.xsltc.DOM;

/**
 * Class is parent of all types of Schema Tree nodes. There are several types, those in the package 
 * {@link org.bibalex.sdxe.suggest.model.dom} can be added to the DOM tree, 
 * and those in the package {@link org.bibalex.sdxe.suggest.model.particle} 
 * represent the rules for XML nodes from the schema.
 */
@SuppressWarnings("serial")
public abstract class SugtnTreeNode extends DefaultMutableTreeNode implements
		Cloneable {
	
	/**
	 * ID of the node
	 */
	private int nodeId;
	/**
	 * Sequence if the node.
	 */
	private static int nodeIdSeq = 0;
	
	/**
	 * Constructs a new empty SugtnTreeNode with the next sequence.
	 */
	public SugtnTreeNode() {
		super();
		// Assign the serial ID
		synchronized (SugtnTreeNode.class) {
			this.nodeId = nodeIdSeq++;
		}
	}
	
	/**
	 * Clones DOM element and its children. Sets the parent to null because normally after cloning a DOM element,
	 * it must be added to a parent.
	 * 
	 * @return A clone of the DOM element and its children
	 */
	@Override
	public Object clone() {
// TODONOT enforce cloning only roots somehow:
// -->Actually if this is enforced the method will fail
// as it clones child nodes!
// throws CloneNotSupportedException {
// //only clone root nodes
// if (!this.isRoot()) {
// throw new CloneNotSupportedException(
// "SugtnTree Clone is supported only for root nodes");
// }
		SugtnTreeNode result = (SugtnTreeNode) super.clone();
		
		// Deep copy the children
		
		for (int i = 0; i < this.getChildCount(); ++i) {
			SugtnTreeNode child = (SugtnTreeNode) this.getChildAt(i);
			child = (SugtnTreeNode) child.clone();
			// insert removes the domElt from its parent if any
			result.insert(child, result.getChildCount()); // add(child);
			child.setParent(result); // because somehow this
		}
		
		// But not the parent because this should be root domElt
		result.parent = null;
		return result;
	}
	
	/**
	 * Gets ID of the node.
	 * 
	 * @return ID of the node.
	 */
	public int getNodeId() {
		return this.nodeId;
	}
	
	/**
	 * Gets the parent element of the node, not a particle parent.
	 * 
	 * @return parent element of the node.
	 */
	public SugtnElementNode getParentSugtnElement() {
		SugtnTreeNode result = (SugtnTreeNode) this.getParent();
		while ((result != null) && !(result instanceof SugtnElementNode)) {
			result = (SugtnTreeNode) result.getParent();
		}
		return (SugtnElementNode) result;
	}
	
	/**
	 * Gets the SugtnPath. 
	 * 
	 * A sugntPath is not a tree path; it skips all particles.
	 * 
	 * @return A string that holds relative path of the current node.
	 * 
	 * @see #getSugtnPathRelative(SugtnElementNode)
	 * 
	 */
	public String getSugtnPath() {
		
		return this.getSugtnPathRelative(null);
	}
	
	
	/**
	 * Gets the SugtnPath. 
	 * 
	 * A sugntPath is not a tree path; it skips all particles.
	 * 
	 * @return A string that holds relative path of the current node.
	 * 
	 */
	
	public String getSugtnPathRelative(SugtnElementNode root) {
		StringBuilder result = new StringBuilder();
		
		for (SugtnTreeNode node = this; !node.equals(root); node = (SugtnTreeNode) node.getParent()) {
			
			if (node instanceof SugtnElementNode) {
				result.insert(0, "/" + ((SugtnDeclNode) node).getLocalName());
			} else if (node instanceof SugtnAttributeNode) {
				result.insert(0, "/@" + ((SugtnDeclNode) node).getLocalName());
			}
			
			if (node.isRoot()) {
				// will not have a parent and will cause exception
				break;
			}
		}
		
		return result.toString();
	}
	
	@Override
	public int hashCode() {
		return this.nodeId;// <-- same as: Integer.valueOf(this.nodeId).hashCode();
	}
	
	/**
	 * Invite method of the visitor design pattern.
	 * 
	 *  @param visitor
	 *  		Tree node visitor interface.
	 *  
	 */
	public abstract void invite(ISugtnTreeNodeVisitor visitor);
}
