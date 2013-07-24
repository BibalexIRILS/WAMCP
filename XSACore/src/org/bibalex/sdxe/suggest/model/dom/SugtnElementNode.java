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

package org.bibalex.sdxe.suggest.model.dom;

import javax.swing.tree.TreeNode;

import org.apache.log4j.Logger;
import org.bibalex.sdxe.controller.SugtnTreeController;
import org.bibalex.sdxe.exception.SugtnException;
import org.bibalex.sdxe.suggest.model.ISugtnTreeNodeVisitor;
import org.bibalex.sdxe.suggest.model.SugtnTreeNode;
import org.bibalex.sdxe.suggest.model.particle.SugtnParticleNode;

import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSType;


/**
 * Class represents element node in XML file.
 *
 */

@SuppressWarnings("serial")
public class SugtnElementNode extends SugtnDeclNode {
	// FIXME: when the element is of a simple type (like boolean) sometimes
	// there isn't a PCData domElt to hold this type.. should we place the PCData domElt
	// or should we store the type?? [the case seen was an element of Simple Content
	// and Complex Type with attributes; addName when changed to boolean for testing]
	
	private static final Logger LOG = Logger.getLogger("org.bibalex.sdxe");
	
	/**
	 * MaxOccurs of the element. Default value is 1.
	 */
	private int maxOccurs = Integer.MIN_VALUE; // 1; // default value
	
	/**
	 * MinOccurs of the element. Default value is 1.
	 */
	private int minOccurs = Integer.MAX_VALUE;// 1; // default value
	
	/**
	 * Boolean value if element is simple or not.
	 */
	private boolean isSimple = false; // normal
	
	// TODO: store these two regularly sought references
	/**
	 * Extra reference to the first PCData node in element's children.
	 */
	private SugtnPCDataNode pcDataNode = null;
	/**
	 * Extra reference to the first particle in element's children.
	 */
	private SugtnParticleNode firstParticle = null;
	
	/**
	 * The sugtnController which controls the tree model and all its nodes.
	 */
	private final SugtnTreeController sugtnController;
	
	
	
	/**
	 * Constructor of {@link SugtnElementNode}.
	 * 
	 * It extracts element's information from element's declaration from schema.
	 * 
	 * @param decl
	 * 		Schema declaration of the element.
	 * @param minOccurs
	 * 		MinOccurs of the element.
	 * @param maxOccurs
	 * 		MaxOccurse of the element.
	 * @param sugtnController
	 * 		The sugtnController which controls the tree model and all its nodes.
	 */
	public SugtnElementNode(XSElementDecl decl, int minOccurs, int maxOccurs,
			SugtnTreeController sugtnController) {
		super(decl);
		
		this.sugtnController = sugtnController;
// this.isSimple = decl.getType().isSimpleType();
		
		this.sugtnDeclQName.targetNamespaceURI = decl.getTargetNamespace();
		
		this.sugtnDeclQName.localName = decl.getName();
// StringBuilder str = new StringBuilder();
// str.append(this.sugtnDeclQName.localName);
		
		if (decl.getDefaultValue() != null) {
			this.defaultValue = decl.getDefaultValue().toString();
// str.append(" - Default=\"").append(this.defaultValue).append("\"");
		}
		if (decl.getFixedValue() != null) {
			this.fixedValue = decl.getFixedValue().toString();
// str.append(" - Fixed to \"").append(this.fixedValue).append("\"");
		}
		
		XSType elmType = decl.getType();
		
		if (elmType.isComplexType()) {
			XSType bType = elmType;
			
			do {
				
				XSParticle elmParticle = bType.asComplexType().getContentType()
						.asParticle();
				
				if (elmParticle != null) {
					if (this.maxOccurs != Integer.MIN_VALUE) {
						
						if ((this.maxOccurs != -1) && ((elmParticle.getMaxOccurs() == -1)
								|| (elmParticle.getMaxOccurs() > this.maxOccurs))) {
							
							if (LOG.isDebugEnabled()) {
								LOG.debug(this.sugtnDeclQName.localName
										+ " - using MaxOccurs from Base Type ("
										+ elmParticle.getMaxOccurs() + " instead of current ("
										+ this.maxOccurs + ")");
							}
							this.maxOccurs = elmParticle.getMaxOccurs();
						}
						
					} else {
						this.maxOccurs = elmParticle.getMaxOccurs();
					}
					
					if (this.minOccurs != Integer.MAX_VALUE) {
						
						if (elmParticle.getMinOccurs() < this.minOccurs) {
							
							if (LOG.isDebugEnabled()) {
								LOG.debug(this.sugtnDeclQName.localName
										+ " - using MinOccurs from Base Type ("
										+ elmParticle.getMinOccurs() + " instead of current ("
										+ this.maxOccurs + ")");
							}
							
							this.minOccurs = elmParticle.getMinOccurs();
						}
						
					} else {
						this.minOccurs = elmParticle.getMinOccurs();
					}
				}
				
				bType = bType.getBaseType();
				
			} while (bType.isComplexType()
					&& !bType.getName().equals(SugtnTypeDeclaration.XSD_BASE_TYPE_NAME));
			
// str.append(" - ").append(this.minOccurs).append(" to ").append(
// this.maxOccurs);
		} else {
			this.isSimple = elmType.isSimpleType();
		}
		
		// Override Min and Max Occurs obtained from complex type
		// with the particle's min and max occurence according to those
		// passed from the model group the particle is part of
		
		// Propagate min and max occurs from group down to domElt
		// taking the more restrictive value for max occurs (least)
		// and the more permissive value for min occurs (least)
		
		// Always use the value passed even if it is the default
		
// //TODONOT: if 1 is explicitly set in the domElt then it has to be used
// if (maxOccurs != 1) { //Default value will be already set or overridden
//		
// if (LOG.isDebugEnabled()) {
// LOG.debug(this.sugtnDeclQName.localName
// + " - Overriding MaxOccurs Value of Element (" + this.maxOccurs
// + ") by the MaxOccurs passed from Model Group (" + maxOccurs + ")");
// }
//			
// this.maxOccurs = maxOccurs;
//			
// } else {
// if (LOG.isTraceEnabled()) {
// LOG.trace(this.sugtnDeclQName.localName
// + " - MaxOccurs Value from element (" + this.maxOccurs
// + ") kept as was got. Model Group Value is (" + maxOccurs + ")");
// }
//			
// }
		
		if (maxOccurs != this.maxOccurs) {
// //TODONOT: if 1 is explicitly set then it has to be used, but how can we tell!
// if (maxOccurs != 1) {
			
			// TODO: find out when exactly to restrict or permit
// //Restrict
// if (maxOccurs != -1 && maxOccurs < this.maxOccurs) {
			
// //Permit
			if ((this.maxOccurs != -1) && ((maxOccurs == -1) || (maxOccurs > this.maxOccurs))) {
				
				if (LOG.isDebugEnabled()) {
					LOG.debug(this.sugtnDeclQName.localName
							+ " - Overriding MaxOccurs Value of Element (" + this.maxOccurs
							+ ") by the MaxOccurs passed from Model Group (" + maxOccurs + ")");
				}
				
				this.maxOccurs = maxOccurs;
				
			} else {
				
				if (LOG.isTraceEnabled()) {
					LOG.trace(this.sugtnDeclQName.localName
							+ " - MaxOccurs Value from element (" + this.maxOccurs
							+ ") kept as was got. Model Group Value is (" + maxOccurs + ")");
				}
				
			}
// }
		}
		// ///////////////////
		
		if (minOccurs != this.minOccurs) {
			// Always use the value even if it is the default
// if (childMinOccurs != 1) {
			
			// TODO: find out when exactly to restrict or permit
// //Restrict
// if (minOccurs > this.minOccurs) {
			// Permit
			if (minOccurs < this.minOccurs) {
				
				if (LOG.isDebugEnabled()) {
					LOG.debug(this.sugtnDeclQName.localName
							+ " - Overriding MinOccurs value of Element (" + this.minOccurs
							+ ") by the MinOccurs passed from Model Group (" + minOccurs + ")");
				}
				
				this.minOccurs = minOccurs;
				
			} else {
				
				if (LOG.isTraceEnabled()) {
					LOG.trace(this.sugtnDeclQName.localName
							+ " - MinOccurs Value from element (" + this.minOccurs
							+ ") kept as was got. Model Group Value is (" + minOccurs + ")");
				}
				
			}
// }
		}
		
		if (this.minOccurs == Integer.MAX_VALUE) {
			this.minOccurs = 1;
		}
		if (this.maxOccurs == Integer.MIN_VALUE) {
			this.maxOccurs = 1;
		}
		
// if (minOccurs != 1) { //Default value will be already set or overridden
//		
// if (LOG.isDebugEnabled()) {
// LOG.debug(this.sugtnDeclQName.localName
// + " - Overriding MinOccurs value of Element (" + this.minOccurs
// + ") by the MinOccurs passed from Model Group (" + minOccurs + ")");
// }
//			
// this.minOccurs = minOccurs;
//			
// } else {
// if (LOG.isTraceEnabled()) {
// LOG.trace(this.sugtnDeclQName.localName
// + " - MinOccurs Value from element (" + this.minOccurs
// + ") kept as was got. Model Group Value is (" + minOccurs + ")");
// }
//			
// }
		
// this.setUserObject(str.toString());
		
		this.constructLabel();
	}
	
	
	/**
	 * Clone the element.
	 * 
	 * @return A cloned object of the element with its {@link #pcDataNode} and {@link #firstParticle} set to null.
	 */
	@Override
	public Object clone() {
		SugtnElementNode result = (SugtnElementNode) super.clone();
		result.pcDataNode = null;
		result.firstParticle = null;
		return result;
		
	}
	
	/**
	 * Sets user object of the element to the label of the element which is the concatenation of the following parts:
	 * <ul>
	 * <li> localName
	 * <li>  - Default="defaultValue"  if element has a default value
	 * <li>  - Fixed to "fixedValue"   if element has a fixed value
	 * <li>  - minOccurs to maxOccurs
	 * </ul>
	 * 
	 */
	protected void constructLabel() {
		StringBuilder str = new StringBuilder();
		str.append(this.sugtnDeclQName.localName);
		if (this.defaultValue != null) {
			str.append(" - Default=\"").append(this.defaultValue).append("\"");
		}
		if (this.fixedValue != null) {
			str.append(" - Fixed to \"").append(this.fixedValue).append("\"");
		}
		str.append(" - ").append(this.minOccurs).append(" to ").append(
				this.maxOccurs);
		this.setUserObject(str.toString());
	}
	
	/**
	 * Gets child attribute node with the qualified name given.
	 *  
	 * @param attrQName
	 * 			Qualified name of the attribute node.
	 *  
	 * @return Child attribute node.
	 */
	public SugtnAttributeNode getChildAttribute(SugtnDeclQName attrQName) {
		TreeNode node = this.getFirstChild();
		while ((node != null) && (node instanceof SugtnAttributeNode)) {
			if (attrQName.equals(((SugtnAttributeNode) node).sugtnDeclQName)) {
				return (SugtnAttributeNode) node;
			}
			node = this.getChildAfter(node);
		}
		return null;
	}
	
	/**
	 * Gets the first particle in element's children.
	 * 
	 * @return FirstParticle in element's children.
	 */
	public SugtnParticleNode getFirstParticle() {
		if (this.firstParticle == null) {
			for (int i = 0; i < this.getChildCount(); ++i) {
				TreeNode child = this.getChildAt(i);
				if (child instanceof SugtnParticleNode) {
					this.firstParticle = (SugtnParticleNode) child;
					break;
				}
			}
		}
		return this.firstParticle;
	}
	
	/**
	 * Gets the maxOccurs of the element.
	 * 
	 * @return MaxOccurs of the element.
	 */
	public int getMaxOccurs() {
		return this.maxOccurs;
	}
	
	/**
	 * Gets the minOccurs of the element.
	 * 
	 * @return MinOccurs of the element.
	 */
	public int getMinOccurs() {
		return this.minOccurs;
	}
	
	/**
	 * Gets the first PCData node in element's children.
	 * 
	 * @return First PCData node in element's children.
	 */
	public SugtnPCDataNode getPcDataNode() {
		if (this.pcDataNode == null) {
			for (int i = 0; i < this.getChildCount(); ++i) {
				TreeNode child = this.getChildAt(i);
				if (child instanceof SugtnPCDataNode) {
					this.pcDataNode = (SugtnPCDataNode) child;
					break;
				}
			}
		}
		return this.pcDataNode;
	}
	
	/**
	 * Gets simple type of the first child PCData node.
	 *   
	 * @return Type declaration of the first PCData node child.
	 */
	public SugtnTypeDeclaration getSimpleType() {
		
// TreeNode child = null;
// for (int i = 0; i < this.getChildCount(); ++i) {
// child = this.getChildAt(i);
// if (child instanceof SugtnPCDataNode) {
// return ((SugtnPCDataNode) child).getType();
// }
//			
// }
// return null;
		if (this.pcDataNode != null) {
			return this.pcDataNode.getType();
		} else {
			return null;
		}
	}
	
	/**
	 * Invite method of the visitor design pattern.
	 * 
	 *  @param visitor
	 *  		Tree node visitor interface.
	 *  
	 */
	@Override
	public void invite(ISugtnTreeNodeVisitor visitor) {
		visitor.elementNode(this);
	}
	
	
	/**
	 * Test if element allows multivalues or not.
	 * 
	 * @return true if element allows multivalues, false if otherwise.
	 */
	public boolean isMultivalued() {
		
		return (this.maxOccurs == -1) || (this.maxOccurs > 1);
	}
	
// protected void setMaxOccurs(int maxOccurs) {
// this.maxOccurs = maxOccurs;
// this.constructLabel();
// }
//	
// protected void setMinOccurs(int minOccurs) {
// this.minOccurs = minOccurs;
// this.constructLabel();
// }
	
	/**
	 * Test if element is of simple type or not.
	 * 
	 * @return True if element is of simple type. 
	 * 		   False if otherwise.
	 */
	public boolean isSimple() {
		return this.isSimple;
	}
	
	/**
	 * 
	 * @return
	 * @throws SugtnException
	 */
	public SugtnTreeNode populateChildren() throws SugtnException {
		SugtnTreeNode result;
		if (this.isLeaf()) {
			result = this.sugtnController.populateModelForDescendant(this);
		} else {
			result = this;
		}
		return result;
	}
	
	/**
	 * Sets the first particle in element's children.
	 * 
	 * @param firstParticle
	 *            First particle to set.
	 */
	public void setFirstParticle(SugtnParticleNode firstParticle) {
		
		this.firstParticle = firstParticle;
	}
	
	/**
	 * Sets the first PCData node in element's children.
	 * @param pcDataNode
	 *            PCData node to set.
	 */
	public void setPcDataNode(SugtnPCDataNode pcDataNode) {
		
		this.pcDataNode = pcDataNode;
	}
}
