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

package org.bibalex.sdxe.suggest;

import java.util.Stack;

import org.apache.log4j.Logger;
import org.bibalex.sdxe.controller.SugtnTreeController;
import org.bibalex.sdxe.suggest.model.SugtnTreeModel;
import org.bibalex.sdxe.suggest.model.SugtnTreeNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnElementNode;
import org.bibalex.sdxe.suggest.model.particle.SugtnParticleNode;

import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSModelGroup.Compositor;
import com.sun.xml.xsom.XSModelGroupDecl;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSWildcard;
import com.sun.xml.xsom.visitor.XSTermVisitor;

/**
 * An implementation of XSOM's term visitor. A visitor is a design pattern than XSOM makes use of while traversing the
 * object model. There are other visitor interfaces in XSOM but the Term visitor is the most adequate one for our needs,
 * since we only care about listing allowed children of an element. A term is what a complex type's particle contains;
 * either an element declaration, or a model group (we care about these two), or a model group declaration, or a
 * wildcard (we don't care about these)
 * 
 * @author Younos.Naga
 * 
 */

public class SugtnBuildVisitor implements XSTermVisitor {
	@SuppressWarnings("serial")
	// Used for debugging
// private boolean fireWatchPoint;
	private static final Logger LOG = Logger.getLogger("org.bibalex.sdxe");
	
	/**
	 * The element for which the builder builds a tree model
	 */
	protected XSElementDecl parentElt;
	
	/**
	 * The latest node which has been created as a particle (rule from the schema) is being visited
	 */
	protected SugtnParticleNode currNode;
	
	/**
	 * The tree model to which nodes are to be added
	 */
	protected SugtnTreeModel model;
	
	/**
	 * Min and max occurs are not calculated correctly, ignore these
	 */
	private Stack<Integer> maxOccursStack = null;
	private Stack<Integer> minOccursStack = null;
	
	/**
	 * The sugtnController which controls the tree model and all its nodes.
	 */
	private SugtnTreeController sugtnController;
	
	/**
	 * Constructs a new build visitor using parameters passed as arguments. 
	 * 
	 * @param parentElt
	 * 			The element for which the builder builds a tree model
	 * @param model
	 * 			The tree model to which nodes are to be added
	 * @param sugtnController
	 * 			The sugtnController which controls the tree model and all its nodes
	 */
	public SugtnBuildVisitor(XSElementDecl parentElt, SugtnTreeModel model,
			SugtnTreeController sugtnController) {
		this.sugtnController = sugtnController;
		this.parentElt = parentElt;
		this.model = model;
		this.currNode = null;
		
		this.minOccursStack = new Stack<Integer>();
// this.minOccursStack.push(minOccursFromType);
		
		this.maxOccursStack = new Stack<Integer>();
// this.maxOccursStack.push(maxOccursFromType);
	}
	
	/**
	 * If the term is an element declaration, create node(s) that correspond to it, and place it in the model. 
	 * 
	 * One node corresponds to a regular element, while an abstract element (or substitution group) would 
	 * correspond to several element nodes.
	 * 
	 * @param decl
	 * 			Element declaration node
	 * 
	 */
	@Override
	public void elementDecl(XSElementDecl decl) {
		
		// go into abstract types, using substitution groups
		if (decl.isAbstract()) {
			for (XSElementDecl sub : decl.getSubstitutables()) {
				if (sub.getName().equals(decl.getName())) {
					continue; // The substitutables contains the element itself.
				}
				sub.visit(this);
			}
		} else {
			
			// This is wrong, so ignore from here until further notice
			int nextEltMaxOccurs = Integer.MIN_VALUE;
			int nextEltMinOccurs = Integer.MAX_VALUE;
			
			// Take the value from parent groups
			if (!this.maxOccursStack.isEmpty()) {
				nextEltMaxOccurs = this.maxOccursStack.peek();
			}
			
			if (!this.minOccursStack.isEmpty()) {
				nextEltMinOccurs = this.minOccursStack.peek();
			}
			
			int passedMinOccurs = (nextEltMinOccurs != Integer.MAX_VALUE ? nextEltMinOccurs
					: 1);
			int passedMaxOccurs = (nextEltMaxOccurs != Integer.MIN_VALUE ? nextEltMaxOccurs
					: 1);
			
			// Further notice! Yeah... we now create the element tree node and add it
			SugtnElementNode elmNode = new SugtnElementNode(decl,
					passedMinOccurs,
					passedMaxOccurs,
					this.sugtnController);
			
			this.model.insertNodeInto(elmNode, this.currNode, this.currNode
					.getChildCount());
		}
	}
	
	/**
	 * If the term is a model group, we create a particle and visit its children.
	 * 
	 * Model group is either choice, sequence or all.
	 * Compositor is added to the tree, and also group's children whether they are model groups or elements.
	 * Compositor and group's children are visited at the end. 
	 * 
	 * @param group
	 * 			model group node
	 * 
	 */
	@Override
	public void modelGroup(XSModelGroup group) {
		Compositor comp = group.getCompositor(); // it is the one that can determine if choice, sequence or all
		
		SugtnParticleNode node = SugtnParticleNode.nodeFactory(comp);
		
		if (this.currNode != null) {
			this.model.insertNodeInto(node, this.currNode, this.currNode
					.getChildCount());
		} else {
			SugtnTreeNode root = (SugtnTreeNode) this.model.getRoot();
			this.model.insertNodeInto(node, root, root.getChildCount());
		}
		
		this.currNode = node;
		
		for (XSParticle child : group.getChildren()) {
			
			if (LOG.isTraceEnabled()) {
				LOG.trace("Processing particle: " + child.getLocator().getLineNumber()
						+ " in model group: "
						+ group.getLocator().getLineNumber());
			}
			
			// IGNORE
			int nextEltMaxOccurs = Integer.MIN_VALUE;
			int nextEltMinOccurs = Integer.MAX_VALUE;
			
			// Take the value from parent groups
			if (!this.maxOccursStack.isEmpty()) {
				nextEltMaxOccurs = this.maxOccursStack.peek();
			}
			
			if (!this.minOccursStack.isEmpty()) {
				nextEltMinOccurs = this.minOccursStack.peek();
			}
			
			// Root element must recieve these values as well
			
			// Propagate min and max occurs from group down to domElt
			// taking the more permissive value for Max Occurs (biggest Val)
			// and the more permissive value for Min Occurs (least val)
			
			int childMaxOccurs = child.getMaxOccurs();
			if (nextEltMaxOccurs != Integer.MIN_VALUE) {
				if ((nextEltMaxOccurs != -1)
						&& ((childMaxOccurs == -1) || (childMaxOccurs >= nextEltMaxOccurs))) {
					nextEltMaxOccurs = childMaxOccurs;
				} else {
					if (LOG.isTraceEnabled()) {
						LOG.trace("MaxOccurs (" + childMaxOccurs + ") from Particle " + child
								+ " ignored in favor of the earlier (" + nextEltMaxOccurs
								+ ")");
					}
				}
			} else {
				nextEltMaxOccurs = childMaxOccurs;
			}
			
			// ///////////////////
			
			int childMinOccurs = child.getMinOccurs();
			if (nextEltMinOccurs != Integer.MAX_VALUE) {
				if (childMinOccurs <= nextEltMinOccurs) {
					nextEltMinOccurs = childMinOccurs;
				} else {
					if (LOG.isTraceEnabled()) {
						LOG.trace("MinOccurs (" + childMinOccurs + ") from Particle " + child
								+ " ignored in favor of the earlier (" + nextEltMinOccurs
								+ ")");
					}
				}
			} else {
				nextEltMinOccurs = childMinOccurs;
			}
			
			this.maxOccursStack.push(nextEltMaxOccurs);
			this.minOccursStack.push(nextEltMinOccurs);
			
			// END IGNORE
			
			child.getTerm().visit(this);
			
			// IGNORE AGAIN
			this.maxOccursStack.pop();
			this.minOccursStack.pop();
			
			// FIXME: use these logical lines instead of the ones below marked WORKAROUND
			// that is, when XSOM replies with why they give diff values
// domElt.setMaxOccurs(nextEltMaxOccurs); //childMaxOccurs);
// domElt.setMinOccurs(nextEltMinOccurs); //childMinOccurs);
			
			// START WORKAROUND
			// Set them to the most permissice of the first children nodes WEKHALAS
			SugtnTreeNode newestNode = (SugtnTreeNode) this.currNode.getFirstChild();
			int newNodeMinOccurs = Integer.MAX_VALUE;
			int newNodeMaxOccurs = Integer.MIN_VALUE;
			
			if (newestNode instanceof SugtnParticleNode) {
				
				newNodeMinOccurs = ((SugtnParticleNode) newestNode).getMinOccurs();
				newNodeMaxOccurs = ((SugtnParticleNode) newestNode).getMaxOccurs();
				
			} else if (newestNode instanceof SugtnElementNode) {
				
// if ("locus".equals(((SugtnElementNode) newestNode).getLocalName())) {
// this.fireWatchPoint = true;
// }
				
				newNodeMinOccurs = ((SugtnElementNode) newestNode).getMinOccurs();
				newNodeMaxOccurs = ((SugtnElementNode) newestNode).getMaxOccurs();
				
			} else {
				
				LOG.debug("ERROR! Newest domElt is neither Particle nor Element!!!");
				
			}
			
			if (newNodeMinOccurs < node.getMinOccurs()) {
				node.setMinOccurs(newNodeMinOccurs);
			}
			
			if ((node.getMaxOccurs() != -1)
					&& ((newNodeMaxOccurs == -1) || (newNodeMaxOccurs > node.getMaxOccurs()))) {
				node.setMaxOccurs(newNodeMaxOccurs);
			}
			// END WORKAROUND
			// END IGNORE AGAIN
			
			this.currNode = node;
			
		}
		
	}
	
	/**
	 * If the term is a model group declaration, we visit the model group itself
	 * 
	 * @param decl
	 * 			model group declaration group
	 */
	@Override
	public void modelGroupDecl(XSModelGroupDecl decl) {
		decl.getModelGroup().visit(this);
		
	}
	
	/**
	 * Wildcards are extension points which are only ignored here
	 * 
	 * @param wc
	 * 		wild card node
	 */
	@Override
	public void wildcard(XSWildcard wc) {
		// Do nothing.. if the extended schema is meant to be used then it,
		// itself, should be parsed
		if (LOG.isDebugEnabled()) {
			LOG
					.debug("Sugtn build visitor encountered a Wildcard and ignored it.. if the extended schema is meant to be used then it, itself, should be parsed");
		}
	}
	
}
