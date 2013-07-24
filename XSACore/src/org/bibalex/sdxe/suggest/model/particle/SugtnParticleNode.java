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

package org.bibalex.sdxe.suggest.model.particle;

import org.apache.log4j.Logger;
import org.bibalex.sdxe.suggest.model.ISugtnTreeNodeVisitor;
import org.bibalex.sdxe.suggest.model.SugtnTreeNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnElementNode;

import com.sun.xml.xsom.XSModelGroup.Compositor;

/**
 * Class to represent all particle nodes of any type (All, Choice and Sequence particle nodes).
 *  
 * It uses factory design pattern. Its constructor is protected. New object of this class can be 
 * instantiated by calling the function {@link SugtnParticleNode#nodeFactory(Compositor)}.
 */
@SuppressWarnings("serial")
public abstract class SugtnParticleNode extends SugtnTreeNode {
	private static final Logger LOG = Logger.getLogger("org.bibalex.sdxe");
	
	/**
	 * MinOccurs of the particle. Default value is 1.
	 */
	private int minOccurs = 1;
	
	/**
	 * MaxOccurs of the particle. Default value is 1.
	 */
	private int maxOccurs = 1;
	
	
	//TODONOT change to include min and max occurs as constructor params
	// We can't pass them 
	/**
	 * Represents Factory method of {@link SugtnParticleNode}. 
	 * It constructs a new object SugtnParticleNode based on compositor type.
	 * 
	 * @param comp
	 * 			Compositor on which type of the returned node will be decided.
	 * 
	 * @return If comp equals Compositor.SEQUENCE, {@link SugtnSequenceNode} will be returned.  
	 * 		   If comp equals Compositor.ALL, {@link SugtnAllNode} will be returned.
	 *  	   If comp equals Compositor.CHOICE, {@link SugtnChoiceNode} will be returned.
	 * 
	 */
	public static SugtnParticleNode nodeFactory(Compositor comp) {
		SugtnParticleNode result = null;
		
		if (comp.equals(Compositor.SEQUENCE)) {
			result = new SugtnSequenceNode();
		} else if (comp.equals(Compositor.ALL)) {
			result = new SugtnAllNode();
		} else if (comp.equals(Compositor.CHOICE)) {
			result = new SugtnChoiceNode();
		}
		
		return result;
	}
	
	/**
	 * Hidden constructor.
	 */
	protected SugtnParticleNode() {
		// Hide constructors
	}
	
//	protected void constructLabel() {
//		String type = "";
//		if (this instanceof SugtnChoiceNode) {
//			type = "Choice";
//		} else if (this instanceof SugtnSequenceNode) {
//			type = "Sequence";
//		} else {
//			type = "All (test!)";
//		}
//		
//		this.userObject = type + " - " + this.minOccurs + " to " + this.maxOccurs;
//	}
	
	/**
	 * Expands all nodes in the tree of the node as a string.
	 * 
	 * It uses the following substitution pattern 
	 * for each expanded node:
	 * <ul>
	 * 	 <li> If expanded node is instance of {@link SugtnParticleNode}:
	 * 		<ul>
	 * 			<li> "All/Any: " if node is of type {@link SugtnAllNode}
	 * 			<li> "Sequence: " if node is of type {@link SugtnSequenceNode}
	 * 			<li> "One of: " if node is of type {@link SugtnChoiceNode}
	 * 		</ul>
	 * 	 <li> If expanded node is instance of {@link SugtnElementNode}:
	 * 		<ul>
	 * 			<li> local name of the node followed by a space.
	 * 		</ul>
	 * </ul>
	 * 
	 * The returned string starts with "{" and ends with "}"
	 * 
	 * @return All nodes in the tree of the node expanded in a string.
	 */
	public String expandToString() {
		StringBuilder result = new StringBuilder("{ ");
		if (this instanceof SugtnAllNode) {
			result.append("All/Any: ");
		} else if (this instanceof SugtnSequenceNode) {
			result.append("Sequence: ");
		} else if (this instanceof SugtnChoiceNode) {
			result.append("One of: ");
		} else {
			throw new UnsupportedOperationException();
		}
		
		SugtnTreeNode child = null;
		for (int i = 0; i < this.getChildCount(); ++i) {
			child = (SugtnTreeNode) this.getChildAt(i);
			if (child instanceof SugtnElementNode) {
				result.append(((SugtnElementNode) child).getLocalName())
						.append(" ");
			} else if (child instanceof SugtnParticleNode) {
				result.append(((SugtnParticleNode) child).expandToString());
			}
		}
		
		result.append("} ");
		return result.toString();
		
	}
	
	/**
	 * Gets maxOccurs of the particle.
	 * 
	 * @return the maxOccurs of the particle.
	 */
	public int getMaxOccurs() {
		return this.maxOccurs;
	}
	
	/**
	 * Gets minOccurs of the particle.
	 * 
	 * @return the minOccurs of the particle.
	 * 
	 */
	public int getMinOccurs() {
		return this.minOccurs;
	}
	
	/**
	 * Invite method of the visitor design pattern.
	 * 
	 *  @param visitor
	 *  		Particle node visitor interface.
	 *  
	 */
	
	public abstract void invite(ISugtnParticleNodeVisitor visitor);
	
	@Override
	public final void invite(ISugtnTreeNodeVisitor visitor) {
		this.invite((ISugtnParticleNodeVisitor) visitor);
	}
	
	/**
	 * Sets maxOccurs of the particle.
	 *
	 * @param maxOccurs
	 *            the maxOccurs to set
	 */
	public void setMaxOccurs(int maxOccurs) {
//		if (maxOccurs < this.maxOccurs) {
//			if (LOG.isDebugEnabled()) {
//				LOG.debug("Max Occurs of Particle " + this.expandToString() + " set from "
//						+ this.maxOccurs + " to " + maxOccurs);
//			}
		this.maxOccurs = maxOccurs;
//		} else {
//			if (LOG.isDebugEnabled()) {
//				LOG.debug("Max Occurs of Particle " + this.expandToString() + " NOT set from "
//						+ this.maxOccurs + " to " + maxOccurs);
//			}
//		}
//		this.constructLabel();
	}
	
	/**
	 * Sets minOccurs of the particle.
	 *
	 * @param minOccurs
	 *            the minOccurs to set
	 */
	public void setMinOccurs(int minOccurs) {
//		if (minOccurs < this.minOccurs) {
//			if (LOG.isDebugEnabled()) {
//				LOG.debug("Min Occurs of Particle " + this.expandToString() + " set from "
//						+ this.minOccurs + " to " + minOccurs);
//			}
//			
		this.minOccurs = minOccurs;
//		} else {
//			if (LOG.isDebugEnabled()) {
//				LOG.debug("Min Occurs of Particle " + this.expandToString() + " NOT set from "
//						+ this.minOccurs + " to " + minOccurs);
//			}
//		}
//		this.constructLabel();
	}
	
}
