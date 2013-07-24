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

package org.bibalex.sdxe.xml;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.bibalex.jdom.XPathBuilder;
import org.bibalex.sdxe.controller.DomTreeController;
import org.bibalex.sdxe.exception.DomChoiceException;
import org.bibalex.sdxe.exception.DomEnforceException;
import org.bibalex.sdxe.exception.DomMaxOccursException;
import org.bibalex.sdxe.exception.DomMinOccursException;
import org.bibalex.sdxe.exception.DomRequiredAttributeException;
import org.bibalex.sdxe.suggest.model.SugtnTreeNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnAttributeNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnDeclQName;
import org.bibalex.sdxe.suggest.model.dom.SugtnElementNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnMixedNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnPCDataNode;
import org.bibalex.sdxe.suggest.model.particle.ISugtnParticleNodeVisitor;
import org.bibalex.sdxe.suggest.model.particle.SugtnAllNode;
import org.bibalex.sdxe.suggest.model.particle.SugtnChoiceNode;
import org.bibalex.sdxe.suggest.model.particle.SugtnParticleNode;
import org.bibalex.sdxe.suggest.model.particle.SugtnSequenceNode;
import org.jdom.Content;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;

/**
 * Enforces that:
 * only one child of a choice occurs (rarely the actual reason why a choice is there in the XSD),
 * required attributes are there (configMinOccEnf=true),
 * required elements are there (configMinOccEnf=true),
 * the sequence of elements is correct (MOST USEFUL)
 * 
 * @author Younos.Naga
 * 
 */
public class DomBuildEnforceVisitor implements ISugtnParticleNodeVisitor {
	
	// TODONE: check that we enforce minOccurs and maxOccurs of Particles not only elements
	// --> Yes, by overriding the min and max occurs of elements by those of Particles
	
	private static final Logger LOG = Logger.getLogger("org.bibalex.DomBuildEnforceVisitor");
	
	/**
	 * The DOM element under which the schema is enforced
	 */
	private Element enforcementContext = null;
	
	/**
	 * To prevent enforcing a node many times
	 */
	private HashSet<SugtnTreeNode> enforcedNodes = null;
	
	/**
	 * To know which of the children of a CHOICE occurred
	 */
	private HashSet<SugtnParticleNode> occuringParticles = null;
	
	/**
	 * Sequence of child elements is the aggregate sequence of the hierarchy of SEQUENCE nodes
	 */
	private Stack<LinkedList<SugtnElementNode>> seqChildrenListsTemp = null;
	
	/**
	 * Store the sequences of child elements for each SEQUENCE node.
	 */
	private HashMap<SugtnSequenceNode, LinkedList<SugtnElementNode>> seqChildrenListsStore = null;
	
	/**
	 * Used while reordering
	 */
	private int seqChildrenNextPos = -1;
	
	/**
	 * To prevent the reordering of the same element because of the same sequence node twice
	 */
	private HashMap<SugtnElementNode, SugtnSequenceNode> orderedElements = null;
	
	/**
	 * The mutually execlusive children of a CHOICE node
	 * Storage in hash map is possible because Choice matters only when direct parent
	 */
	private HashMap<SugtnChoiceNode, LinkedList<SugtnElementNode>> mutExChildElmsLists = null;
	/**
	 * The mutually execlusive children of a CHOICE node
	 * Storage in hash map is possible because Choice matters only when direct parent
	 */
	private HashMap<SugtnChoiceNode, LinkedList<SugtnParticleNode>> mutExChildParticlesLists = null;
	
	/**
	 * This is not the configuration to check for minOccurs or not.. while enforcing choice a pass is done on all its
	 * children without enforcing minOccurs by setting this to true.
	 */
	private boolean enforceMinOccurs = true;
	/**
	 * This is because the visitor interface doesn't have throws and override cannot add throws as I have read
	 */
	private DomEnforceException exception = null;
	
	/**
	 * 
	 */
	private boolean configMinOccEnf = false;
	/**
	 * 
	 */
	private boolean configMaxOccEnf = false;
	/**
	 * 
	 */
	private boolean configTwoPhaseCommit = false;
	
	/**
	 * 
	 * @param enforcementContext
	 * @param configMinOccEnf
	 * @param configMaxOccEnf
	 * @param configTwoPhaseCommit
	 */
	public DomBuildEnforceVisitor(Element enforcementContext, boolean configMinOccEnf,
			boolean configMaxOccEnf, boolean configTwoPhaseCommit) {
		
		this.enforcementContext = enforcementContext;
		this.configMaxOccEnf = configMaxOccEnf;
		this.configMinOccEnf = configMinOccEnf;
		this.configTwoPhaseCommit = configTwoPhaseCommit;
		
		this.enforcedNodes = new HashSet<SugtnTreeNode>();
		this.occuringParticles = new HashSet<SugtnParticleNode>();
		
		this.seqChildrenNextPos = 0;
		this.seqChildrenListsStore = new HashMap<SugtnSequenceNode, LinkedList<SugtnElementNode>>();
		this.seqChildrenListsTemp = new Stack<LinkedList<SugtnElementNode>>();
		
		this.orderedElements = new HashMap<SugtnElementNode, SugtnSequenceNode>();
		
		this.mutExChildElmsLists = new HashMap<SugtnChoiceNode, LinkedList<SugtnElementNode>>();
		this.mutExChildParticlesLists = new HashMap<SugtnChoiceNode, LinkedList<SugtnParticleNode>>();
		this.enforceMinOccurs = true;
		
		if (LOG.isTraceEnabled()) {
			LOG.trace("New DomBuildEnforceVisitor for the context element " + enforcementContext);
		}
	}
	
	/**
	 * Nothing is enforced for this particle
	 */
	@Override
	public void allNode(SugtnAllNode node) {
		// TODO: Test this method.. never encountered
		if (LOG.isTraceEnabled()) {
			LOG.trace("Enforcing: (" + node + ")");
		}
		
		this.enforceChildren(node);
		
		if (this.enforcedNodes.contains(node)) {
			if (LOG.isTraceEnabled()) {
				LOG.trace("Already enforced: (" + node + ")");
			}
			return;
		}
		
		if ((this.exception == null)
				&& (this.enforceMinOccurs)) {
			// This is all what it takes to be totally enforced??
			this.enforcedNodes.add(node);
			if (LOG.isTraceEnabled()) {
				LOG.trace("Enforced: (" + node + ")");
			}
		}
	}
	
	/**
	 * @see private void unexpectedNode(SugtnTreeNode node)
	 */
	@Override
	public void attributeNode(SugtnAttributeNode node) {
		this.unexpectedNode(node);
	}
	
	/**
	 * Build a set of all children of this particle and its occurring child particles
	 */
	private HashSet<SugtnDeclQName> buildSetOfOccuringChildElmsForParicle(
			SugtnParticleNode particle) {
		HashSet<SugtnDeclQName> result = new HashSet<SugtnDeclQName>();
		
		this.fillSetWithOccuringChildElmsRecursive(
				particle, result);
		
		return result;
		
	}
	
	/**
	 * Visits children to see which of them occurred. The mutually execlusive children must not occur together. This
	 * doesn't apply to an occurring element and an occurring particle when the element is part of a particle.
	 */
	@Override
	public void choiceNode(SugtnChoiceNode node) {
		String nodeExpanded = node.expandToString();
		if (LOG.isTraceEnabled()) {
			LOG.trace("Enforcing: (" + nodeExpanded + ")");
		}
		
		boolean enforceMinOccursOld = true;
		try {
			// We should deal only with occuring children,
			// i.e. ignore minOccurrs exceptions
			enforceMinOccursOld = this.enforceMinOccurs;
			this.enforceMinOccurs = false;
			if (LOG.isTraceEnabled()) {
				LOG.trace("Min Occurs enforcement turned OFF");
			}
			
			LinkedList<SugtnElementNode> occuringChildElmsList = new LinkedList<SugtnElementNode>();
			this.mutExChildElmsLists.put(node, occuringChildElmsList);
			
			LinkedList<SugtnParticleNode> occuringChildParticlesList = new LinkedList<SugtnParticleNode>();
			this.mutExChildParticlesLists.put(node, occuringChildParticlesList);
			
			this.enforceChildren(node);
			
			if (this.enforcedNodes.contains(node)) {
				if (LOG.isTraceEnabled()) {
					LOG.trace("Already enforced: (" + nodeExpanded + ")");
				}
				return;
			}
			
			// Children didn't cause trouble.. proceed!
			if (this.exception == null) {
				
				// FIXME Something under a choice is not necessarily mutually execlusive.. revisit this minOccurs
				// FIXME The minOccurs in the case of a model group
// //Get the occurrence constraints of the particle from its children
// // (we override the element occurrence by the particle's)
// SugtnTreeNode firstEltChild = (SugtnTreeNode) domElt.getFirstChild();
// while ((firstEltChild != null) && !(firstEltChild instanceof SugtnElementNode)) {
// firstEltChild = (SugtnTreeNode) firstEltChild.getFirstChild();
// }
//
// assert (firstEltChild != null) : "An Empty particle!!";
//
// int nodeMinOccurs = ((SugtnElementNode) firstEltChild).getMinOccurs();
// int nodeMaxOccurs = ((SugtnElementNode) firstEltChild).getMaxOccurs();
				
				int nodeMaxOccurs = node.getMaxOccurs();
				int nodeMinOccurs = node.getMinOccurs();
				
				int nodeActOccurs = 0;
				
				// If maxOccurs is unbounded then the choice is an inclusive or..
				// no execlusion!
				if (nodeMaxOccurs != -1) {
					SugtnTreeNode[] exclusiveChildren = new SugtnTreeNode[nodeMaxOccurs];
					
					// if a particle occured
					if (occuringChildParticlesList.size() > 0) {
						if (LOG.isTraceEnabled()) {
							LOG
									.trace("Child particle(s) of choice: (" + nodeExpanded
											+ ") occured");
						}
						for (SugtnParticleNode listParticle : occuringChildParticlesList) {
							// Up to nodeMaxOccurs children can occur
							if (nodeActOccurs >= nodeMaxOccurs) {
								this.exception = new DomChoiceException(
										((SugtnParticleNode) exclusiveChildren[nodeActOccurs - 1])
												.expandToString(), // exclusiveParticle.expandToString(),
										listParticle.expandToString(),
										this.enforcementContext.getName());
								
								break;
							} else {
								
								nodeActOccurs++;
								exclusiveChildren[nodeActOccurs - 1] = listParticle;
								
							}
						}
					}
					
					// If particles didn't cause trouble
					if (this.exception == null) {
						// if an element occured
						if (occuringChildElmsList.size() > 0) {
							if (LOG.isTraceEnabled()) {
								LOG.trace("Child element(s) of choice: (" + nodeExpanded
										+ ") occured");
							}
							
							for (SugtnTreeNode exclusiveChild : exclusiveChildren) {
								
								SugtnParticleNode exclusiveParticle = (SugtnParticleNode) exclusiveChild;
								
								if (exclusiveParticle != null) {
									
									// An all or nothing particle has occured, and maybe
									// some of its elements are possible outside of it
									// and have seemed to occur as well.. but they did
									// only as part of it.. so no exception!
									
									HashSet<SugtnDeclQName> exclusiveParticleElms = this
											.buildSetOfOccuringChildElmsForParicle(exclusiveParticle);
									
									// Check if some of the elements that occurred are not
									// children of the particle.. we only find the
									// first one and report it
									for (SugtnElementNode listElm : occuringChildElmsList) {
										if (!exclusiveParticleElms
												.contains(((SugtnElementNode) listElm)
														.getqName())) {
											this.exception = new DomChoiceException(
													listElm.getLocalName(), exclusiveParticle
															.expandToString(),
													this.enforcementContext.getName());
										} else {
											if (LOG.isTraceEnabled()) {
												LOG
														.trace("While enforcing choice, we will consider that Element "
																+ listElm
																+ " is just a part of the particle "
																+ exclusiveParticle
																		.expandToString());
											}
											// TODO: should we? -> occuringChildElmsList.remove();
										}
										
									}
									if (this.exception == null) {
										LOG
												.trace("Occuring child element(s) are actually children of occuring particle "
														+ exclusiveParticle);
									}
									
								} // else { <-- not else.. just another if
							}
							
							if (this.exception == null) {
								// Enforce choice of Mutually exclusive child elements
								for (SugtnElementNode listElm : occuringChildElmsList) {
									
									if (nodeActOccurs >= nodeMaxOccurs) {
										
										boolean alreadyAdded = false;
										for (SugtnTreeNode exclusiveChild : exclusiveChildren) {
											alreadyAdded = ((SugtnElementNode) exclusiveChild)
													.isQNamesEqual(listElm);
											if (alreadyAdded) {
												break;
											}
										}
										
										if (!alreadyAdded) {
											this.exception = new DomChoiceException(
													((SugtnElementNode) exclusiveChildren[nodeActOccurs - 1])
															.getLocalName(),
													listElm.getLocalName(),
													this.enforcementContext.getName());
											break;
										} // else, no problem :
											// this is another instance of the same child element type
									} else {
										nodeActOccurs++;
										exclusiveChildren[nodeActOccurs - 1] = listElm;
									}
								}
							}
						}
					}
				} else {
					if (LOG.isTraceEnabled()) {
						LOG
								.trace("Choice: (" + nodeExpanded
										+ ") is an Inclusive OR.. nothing to enforce!");
					}
				}
				
				if ((occuringChildParticlesList.size() == 0)
						&& (occuringChildElmsList.size() == 0)) {
					
					if (LOG.isTraceEnabled()) {
						LOG.trace("No children of choice: (" + nodeExpanded + ") occured.");
					}
					
					// Nothing occurred, so check if occurrence is required
					
					if (this.configMinOccEnf && (nodeMinOccurs > 0)) { // FIXME: find a way to make MinOccurs work
					
						// it is required so check for minOccurs
						this.enforceMinOccurs = true;
						if (LOG.isTraceEnabled()) {
							LOG.trace("Min Occurs enforcement turned ON");
						}
						
						this.enforceChildren(node);
					} else {
						if (LOG.isTraceEnabled()) {
							LOG
									.trace("Min occurs will not be enforced for choice "
											+ nodeExpanded);
						}
					}
					
				}
				
				if (this.exception == null) {
					this.enforcedNodes.add(node);
					
					if (LOG.isTraceEnabled()) {
						LOG.trace("Enforced: " + nodeExpanded);
					}
				} // else the error message will already be descriptive
			}
			
		} finally {
			this.enforceMinOccurs = enforceMinOccursOld; // true;
			if (LOG.isTraceEnabled()) {
				LOG.trace("Min Occurs enforcement turned ON");
			}
			
			this.mutExChildElmsLists.remove(node);
			
			this.mutExChildParticlesLists.remove(node);
		}
	}
	
	/**
	 * Must be called after the enforcement is complete, so that the sequences that actually occurred are known. The
	 * order of these occurring sequences is already known and stored in a data structure, and the elements that these
	 * sequences (and maybe others that didn't occur) should order are known.. so enforce!
	 * 
	 * @param element
	 */
	private void doReorder(SugtnElementNode element) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("Doing Reorder for: <" + element.getqName() + ">");
		}
		String elmName = element.getLocalName();
		Namespace ns = DomTreeController.acquireNamespace(element.getTargetNamespaceURI(),
				this.enforcementContext);
		
		List childrenList = this.enforcementContext.getChildren(elmName, ns);
		// Enforce order
		int currIx;
		Element childElm;
		Content tempCont;
		for (int i = 0; i < childrenList.size(); ++i) {
			childElm = (Element) childrenList.get(i);
			currIx = this.enforcementContext.indexOf(childElm);
			if (currIx > this.seqChildrenNextPos) {
				tempCont = this.enforcementContext.getContent(
						this.seqChildrenNextPos).detach();
				
				if (LOG.isDebugEnabled()) {
					childElm.setAttribute("DEBUG",
							"Moved to Content from Ix: " + currIx + " to ix: "
									+ this.seqChildrenNextPos
									+ " - while enforcing: " + element.getSugtnPath() + " - on: "
									+ new XPathBuilder().buildXPathString(childElm)
									+ " - children list size: "
									+ childrenList.size());
				} else {
					childElm.removeAttribute("DEBUG");
				}
				
				this.enforcementContext.addContent(this.seqChildrenNextPos,
						childElm.detach());
				this.enforcementContext.addContent(currIx, tempCont);
				
				// The list must be regenerated after changing
				childrenList = this.enforcementContext.getChildren(elmName, ns);
			}
			this.seqChildrenNextPos++;
		}
		if (LOG.isTraceEnabled()) {
			LOG.trace("Did reorder for: <" + element.getqName() + ">");
		}
	}
	
	/**
	 * An element declaration can have the following rules:
	 * That the element must occur a min occurs times (configMinOccEnf)
	 * That the element cannot occur more than a max occurs times (configMaxOccEnf=true_)
	 * Some required attributes (configTwoPhaseCommit=true)
	 * 
	 * And if this element occurred any number of times it adds itself and its parent particle to the occurring elements
	 * and particles data structures (tentatively), and to the order data structure
	 */
	@Override
	public void elementNode(SugtnElementNode node) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("Enforcing: (" + node + ")");
		}
		
		if (this.enforcedNodes.contains(node)) {
			if (LOG.isTraceEnabled()) {
				LOG.trace("Already enforced: (" + node + ")");
			}
			return;
		}
		
		int currentOccurs, minOccurs, maxOccurs;
		Namespace eltNs;
		String eltNsPrefix;
		try {
			
			eltNs = DomTreeController.acquireNamespace(node.getTargetNamespaceURI(),
					this.enforcementContext, true);
			
			eltNsPrefix = null;
			if (eltNs.equals(Namespace.NO_NAMESPACE)) {
				eltNsPrefix = "";
			} else {
				eltNsPrefix = eltNs.getPrefix() + ":";
			}
			
			String xpEltCounterStr = "count(" + eltNsPrefix + node.getLocalName() + ")";
			XPath xpEltCounter = XPath.newInstance(xpEltCounterStr);
			
			xpEltCounter.addNamespace(eltNs);
			
			currentOccurs = xpEltCounter.numberValueOf(this.enforcementContext)
					.intValue();
			
			if (LOG.isTraceEnabled()) {
				LOG.trace("Current occurs for element <" + node.getLocalName() + "> is "
						+ currentOccurs);
			}
		} catch (JDOMException e) {
			this.exception = new DomEnforceException(e);
			return;
		}
		
		// When enforcing children of choice a round without enforcement of MinOccurs is needed
		if (this.enforceMinOccurs) {
			minOccurs = node.getMinOccurs();
			if (LOG.isTraceEnabled()) {
				LOG.trace("Min Occurs for element <" + node.getLocalName() + "> is " + minOccurs);
			}
			// YA 20100627 Removing two phase addition
			// Now that commit is instantaniout we cannot check for min occurs
			// of other elements because they cannot exist..
			
			if (this.configMinOccEnf && (minOccurs > 0) && (minOccurs > currentOccurs)) {
				String missing = node.getLocalName();
				
				SugtnTreeNode tempParticleNode = (SugtnTreeNode) node
						.getParent();
				
				if (tempParticleNode.getParent() instanceof SugtnElementNode) {
					
					this.exception = new DomMinOccursException(missing, minOccurs,
							this.enforcementContext.getName());
					
				} else if (tempParticleNode instanceof SugtnSequenceNode) {
					
					this.exception = new DomMinOccursException(missing, minOccurs,
							((SugtnSequenceNode) tempParticleNode).expandToString());
					
				} else if (tempParticleNode instanceof SugtnChoiceNode) {
					
					this.exception = new DomMinOccursException(((SugtnChoiceNode) tempParticleNode)
							.expandToString(), minOccurs, this.enforcementContext.getName());
					
				}
				
				return;
			}
		}
		
		if (currentOccurs > 0) {
			
			// check for max occurs
			maxOccurs = node.getMaxOccurs();
			if (LOG.isTraceEnabled()) {
				LOG.trace("Max Occurs for element <" + node.getLocalName() + "> is " + maxOccurs);
			}
			if (this.configMaxOccEnf && (maxOccurs > -1) && (maxOccurs < currentOccurs)) {
				this.exception = new DomMaxOccursException(node.getLocalName(),
						maxOccurs, this.enforcementContext.getName());
				return;
			}
			// enforce required attributes
			SugtnTreeNode child = null;
			
			// YA 20100627 Removing two phase addition
			// Now that commit is instantanious we cannot check for min occurs
			// of other nodes (attributes) because they cannot exist yet..
			// FIXME if minOccurs problem gets fixed change configTwoPhaseCommit to true
			for (int i = 0; this.configTwoPhaseCommit && (i < node.getChildCount()); ++i) {
				
				child = (SugtnTreeNode) node.getChildAt(i);
				
				if (child instanceof SugtnAttributeNode) {
					
					SugtnAttributeNode attrNode = (SugtnAttributeNode) child;
					
					if (attrNode.isRequired()) {
						
						Namespace attrNs = DomTreeController.acquireNamespace(attrNode
								.getTargetNamespaceURI(),
								this.enforcementContext, true);
						
						String attrNsPrefix = null;
						if (attrNs.equals(Namespace.NO_NAMESPACE)) {
							attrNsPrefix = "";
						} else {
							attrNsPrefix = attrNs.getPrefix() + ":";
						}
						
						String xpAttrCounterStr = "count(" + eltNsPrefix
								+ node.getLocalName() + "/@" + attrNsPrefix
								+ attrNode.getLocalName() + ")";
						try {
							XPath xpAttrCounter = XPath.newInstance(xpAttrCounterStr);
							
							xpAttrCounter.addNamespace(eltNs);
							xpAttrCounter.addNamespace(attrNs);
							
							int attrOccurs = xpAttrCounter.numberValueOf(this.enforcementContext)
									.intValue();
							
							if (attrOccurs != currentOccurs) {
								this.exception = new DomRequiredAttributeException(node
										.getLocalName(),
										attrNode.getLocalName(), this.enforcementContext.getName());
								return;
							} else {
								if (LOG.isTraceEnabled()) {
									LOG.trace("Attribute @" + attrNode.getLocalName()
											+ " is required and present in element <"
											+ node.getLocalName() + "> ");
								}
							}
						} catch (JDOMException e) {
							this.exception = new DomEnforceException(e);
							return;
						}
						
					}
				} else {
					break; // we are done.. the attributes are placed first
				}
			}
			
			// ///////Done enforcing////////////
			
			if (node.getParent() instanceof SugtnParticleNode) {
				// This element actually occurs so its parent particle
				// must be added to occurring particles
				SugtnParticleNode parentNode = (SugtnParticleNode) node.getParent();
				this.occuringParticles.add(parentNode);
				
				if (LOG.isTraceEnabled()) {
					LOG.trace("Occuring particle (" + parentNode + ") added");
				}
				
				// If direct parent is Choice we have to add this to Mutex Children
				// FIXME something under a choice is not necessearily mutex.. check minOccurs
				if (parentNode instanceof SugtnChoiceNode) {
					
					this.mutExChildElmsLists.get(parentNode)
							.add(node);
					if (LOG.isTraceEnabled()) {
						LOG.trace("Mutually exclusive child element <" + node.getLocalName()
								+ "> added to particle (" + parentNode + ")");
					}
				}
			}
			// Put only occuring children in sequential children so
			// add to sequential children of last known Sequence Node
			if (!this.seqChildrenListsTemp.isEmpty()) {
				this.seqChildrenListsTemp.peek().add(node);
			}
			
		}
		
		if ((this.exception == null) && this.enforceMinOccurs) {
			this.enforcedNodes.add((SugtnTreeNode) node);
			if (LOG.isTraceEnabled()) {
				LOG.trace("Enforced: (" + node + ")");
			}
		}
		
	}
	
	/**
	 * Entry point. Work around for no return and no throws in interface
	 * 
	 * @param node
	 * @throws DomEnforceException
	 */
	public void enforce(SugtnParticleNode node) throws DomEnforceException {
		node.invite(this);
		if (this.exception != null) {
			DomEnforceException exTemp = this.exception;
			this.exception = null;
			throw exTemp;
		}
	}
	
	/**
	 * Iterates over children calling enforce for them
	 * 
	 * @param node
	 */
	private void enforceChildren(SugtnParticleNode node) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("Enforcing children of: (" + node + ")");
		}
		SugtnTreeNode child = null;
		for (int i = 0; i < node.getChildCount(); ++i) {
			child = (SugtnTreeNode) node.getChildAt(i);
			child.invite(this);
			if (this.exception != null) {
				break;
			} else {
				if ((node instanceof SugtnChoiceNode) && (child instanceof SugtnParticleNode)) {
					if (this.occuringParticles.contains(child)) {
						this.mutExChildParticlesLists.get(node).add((SugtnParticleNode) child);
						if (LOG.isTraceEnabled()) {
							LOG.trace("Mutually exclusive child particle (" + child
									+ ") added to choice (" + node + ")");
						}
					}
				}
			}
		}
		
		if (this.exception == null) {
			if (node.getParent() instanceof SugtnParticleNode) {
				if ((this.exception == null) && (this.occuringParticles.contains(node))) {
					SugtnParticleNode parentNode = (SugtnParticleNode) node.getParent();
					this.occuringParticles.add(parentNode);
				}
			}
			
			if (LOG.isTraceEnabled()) {
				LOG.trace("Enforced children of:( " + node + ")");
			}
		}
	}
	
	public void enforceOrder(SugtnParticleNode sugtnParent)
			throws DomEnforceException {
		
		if (LOG.isTraceEnabled()) {
			LOG.trace("Ordering: (" + sugtnParent + ")");
		}
		
		Iterator<SugtnElementNode> seqItr = null;
		SugtnElementNode nextExpectedElm = null;
		
		// FIXME A very small bug in the order caused by inderect inclusion of elements.. use the order stat structure
// instead of the fix below
// YA20110323 Didn't I say before implementing this reorder "bug fix" that it is so dangerous?
// // YA20110314 Enforcing order for elements not directly under the sequence
// SugtnTreeNode sugtnAncestorSequence = null;
// for (sugtnAncestorSequence = sugtnParent; (sugtnAncestorSequence != null)
// && (sugtnAncestorSequence instanceof SugtnParticleNode); sugtnAncestorSequence = (SugtnTreeNode)
// sugtnAncestorSequence
// .getParent()) {
// if (sugtnAncestorSequence instanceof SugtnSequenceNode) {
// // a null pointer exception once happened here somehow..
// // but the logic is correct as the results show.. so just
// // add a guard and TODONE: understand why later
// // seqLst is null when we are adding more children to an
// // old elm which is already ordered.. but we must reorder the whole
// // thing element because seqNextPos will be 0 otherwise
// // seqItr = this.seqChildrenListsStore.get(sugtnParent).iterator();
//
// LinkedList<SugtnElementNode> seqLst = this.seqChildrenListsStore
// .get(sugtnAncestorSequence);
// if (seqLst == null) {
// throw new DomEnforceException(
// "Didn't find a sequential children list for the occuring sequence domElt "
// + sugtnAncestorSequence.toString(),
// this.enforcementContext.toString());
// } else {
// seqItr = seqLst.iterator();
// }
// if (seqItr.hasNext()) {
// nextExpectedElm = seqItr.next();
// }
// break;
// }
// }
		if (sugtnParent instanceof SugtnSequenceNode) {
			// a null pointer exception once happened here somehow..
			// but the logic is correct as the results show.. so just
			// add a guard and TODONE: understand why later
			// seqLst is null when we are adding more children to an
			// old elm which is already ordered.. but we must reorder the whole
			// thing element because seqNextPos will be 0 otherwise
			// seqItr = this.seqChildrenListsStore.get(sugtnParent).iterator();
			
			LinkedList<SugtnElementNode> seqLst = this.seqChildrenListsStore.get(sugtnParent);
			if (seqLst == null) {
				throw new DomEnforceException(
						"Didn't find a sequential children list for the occuring sequence domElt "
								+ sugtnParent.toString(), this.enforcementContext.toString());
			} else {
				seqItr = seqLst.iterator();
			}
			if (seqItr.hasNext()) {
				nextExpectedElm = seqItr.next();
			}
		}
// END YA20110315
// END YA20110323
		for (int j = 0; j < sugtnParent.getChildCount(); ++j) {
			SugtnTreeNode sugtnChild = (SugtnTreeNode) sugtnParent
					.getChildAt(j);
			if (sugtnChild instanceof SugtnParticleNode) {
				if (this.occuringParticles.contains(sugtnChild)) {
					this.enforceOrder((SugtnParticleNode) sugtnChild);
				}
			} else if (sugtnChild instanceof SugtnElementNode) {
				// YA20110323 Didn't I say before implementing this reorder "bug fix" that it is so dangerous?
// // YA20110314 Enforcing order for elements not directly under the sequence
// if (sugtnAncestorSequence != null) {
//
// if (nextExpectedElm != null) { // we are still expecting elements for this particle
// if (((SugtnElementNode) sugtnChild).isQNamesEqual(nextExpectedElm)) {
// if (LOG.isTraceEnabled()) {
// LOG.trace("Expected element encountered: <"
// + nextExpectedElm.getqName() + ">");
// }
// if (!this.orderedElements
// .containsKey(nextExpectedElm)) {
//
// this.doReorder(nextExpectedElm);
//
// this.orderedElements.put(nextExpectedElm,
// (SugtnSequenceNode) sugtnAncestorSequence);
// if (LOG.isTraceEnabled()) {
// LOG.trace("Ordered element <" + nextExpectedElm.getqName()
// + "> added");
// }
// } else {
// // Already ordered.. log that!
// SugtnSequenceNode tempSeqNode = this.orderedElements
// .get(nextExpectedElm);
// if (LOG.isTraceEnabled()) {
// LOG.trace("Aleady ordered: <" + nextExpectedElm.getqName()
// + "> while ordering particle (" + tempSeqNode + ")");
// }
// }
//
// // We found the next expected element so whatever we did to it
// // we must proceed to the one after it
// if (seqItr.hasNext()) {
// nextExpectedElm = seqItr.next();
// } else {
// // NO WE ARE NOT: break; //we are done!
// // Proceed to re-order children of child particles
// // we don't expect any elements in this particle!
// nextExpectedElm = null;
// }
// } // else this element didn't occur for that particle.. NOTHING
// } // else we are still parsing non-occuring child elements of the particle.. NOTHING
// } // else we might be in a choice particle so just keep going (not needed for "all" ones)
// } else {
// this.unexpectedNode(sugtnChild);
// }
// }
// if (LOG.isTraceEnabled()) {
// LOG.trace("Ordered: (" + sugtnAncestorSequence + ")");
// }
				if (sugtnParent instanceof SugtnSequenceNode) {
					if (nextExpectedElm != null) { // we are still expecting elements for this particle
						if (((SugtnElementNode) sugtnChild).isQNamesEqual(nextExpectedElm)) {
							if (LOG.isTraceEnabled()) {
								LOG.trace("Expected element encountered: <"
										+ nextExpectedElm.getqName() + ">");
							}
							if (!this.orderedElements
									.containsKey(nextExpectedElm)) {
								
								this.doReorder(nextExpectedElm);
								
								this.orderedElements.put(nextExpectedElm,
										(SugtnSequenceNode) sugtnParent);
								if (LOG.isTraceEnabled()) {
									LOG.trace("Ordered element <" + nextExpectedElm.getqName()
											+ "> added");
								}
							} else {
								// Already ordered.. log that!
								SugtnSequenceNode tempSeqNode = this.orderedElements
										.get(nextExpectedElm);
								if (LOG.isTraceEnabled()) {
									LOG.trace("Aleady ordered: <" + nextExpectedElm.getqName()
											+ "> while ordering particle (" + tempSeqNode + ")");
								}
							}
							
							// We found the next expected element so whatever we did to it
							// we must proceed to the one after it
							if (seqItr.hasNext()) {
								nextExpectedElm = seqItr.next();
							} else {
								// NO WE ARE NOT: break; //we are done!
								// Proceed to re-order children of child particles
								// we don't expect any elements in this particle!
								nextExpectedElm = null;
							}
						} // else this element didn't occur for that particle.. NOTHING
					} // else we are still parsing non-occuring child elements of the particle.. NOTHING
				} // else we might be in a choice particle so just keep going (not needed for "all" ones)
			} else {
				this.unexpectedNode(sugtnChild);
			}
		}
		if (LOG.isTraceEnabled()) {
			LOG.trace("Ordered: (" + sugtnParent + ")");
		}
// END YA20110315
// END YA20110323
	}
	
	/**
	 * 
	 * @param particle
	 * @param particleElms
	 */
	private void fillSetWithOccuringChildElmsRecursive(SugtnParticleNode particle,
			HashSet<SugtnDeclQName> particleElms) {
		
		for (int j = 0; j < particle.getChildCount(); ++j) {
			SugtnTreeNode particleChild = (SugtnTreeNode) particle
					.getChildAt(j);
			if (particleChild instanceof SugtnElementNode) {
				particleElms.add(((SugtnElementNode) particleChild).getqName());
			} else if (particleChild instanceof SugtnParticleNode) {
				if (this.occuringParticles.contains(particleChild)) {
					this.fillSetWithOccuringChildElmsRecursive(
							(SugtnParticleNode) particleChild, particleElms);
				}
			} else {
				this.unexpectedNode(particleChild);
			}
		}
		
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isConfigMaxOccEnf() {
		return this.configMaxOccEnf;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isConfigMinOccEnf() {
		return this.configMinOccEnf;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isConfigTwoPhaseCommit() {
		return this.configTwoPhaseCommit;
	}
	
	/**
	 * @see private void unexpectedNode(SugtnTreeNode node)
	 */
	@Override
	public void mixedNode(SugtnMixedNode node) {
		this.unexpectedNode(node);
	}
	
	/**
	 * @see private void unexpectedNode(SugtnTreeNode node)
	 */
	@Override
	public void pcdataNode(SugtnPCDataNode node) {
		this.unexpectedNode(node);
	}
	
	/**
	 * Prepares order data structures
	 */
	@Override
	public void sequenceNode(SugtnSequenceNode node) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("Enforcing: (" + node + ")");
		}
		LinkedList<SugtnElementNode> seqChildrenTemp = null;
		try { // No throws in enforceChildren for now.. but who knows
				// maybe later it will be added. Safety at no price!
			this.seqChildrenListsTemp.push(new LinkedList<SugtnElementNode>());// String>());
			this.enforceChildren(node);
		} finally {
			seqChildrenTemp = this.seqChildrenListsTemp.pop();
		}
		
		if (this.enforcedNodes.contains(node)) {
			if (LOG.isTraceEnabled()) {
				LOG.trace("Already enforced: (" + node + ")");
			}
			return;
		}
		
		if (this.exception == null) {
			// Add to sequences of children that will be enforced
			// when the time is right
			
			this.seqChildrenListsStore.put(node, seqChildrenTemp);
			
			// See if it is time to enforce sequence of children
// YOU NEVER KNOW.. driver must tell you
			
			// IS The particle totally enforced?
			if (this.enforceMinOccurs) { // && this.enforceSequence) {
				this.enforcedNodes.add(node);
				if (LOG.isTraceEnabled()) {
					LOG.trace("Enforced: (" + node + ")");
				}
			}
			
		}
	}
	
	/**
	 * 
	 * @param configMaxOccEnf
	 * @return
	 */
	public DomBuildEnforceVisitor setConfigMaxOccEnf(boolean configMaxOccEnf) {
		this.configMaxOccEnf = configMaxOccEnf;
		return this;
	}
	
	/**
	 * 
	 * @param configMinOccEnf
	 * @return
	 */
	public DomBuildEnforceVisitor setConfigMinOccEnf(boolean configMinOccEnf) {
		this.configMinOccEnf = configMinOccEnf;
		return this;
	}
	
	/**
	 * 
	 * @param configTwoPhaseCommit
	 * @return
	 */
	public DomBuildEnforceVisitor setConfigTwoPhaseCommit(boolean configTwoPhaseCommit) {
		this.configTwoPhaseCommit = configTwoPhaseCommit;
		return this;
	}
	
	/**
	 * Not all node types of this visitor should be present
	 * 
	 * @param node
	 */
	private void unexpectedNode(SugtnTreeNode node) {
		this.exception = new DomEnforceException("An unexpected domElt: "
				+ node.toString() + ".. Expecting only particles and elements!",
				this.enforcementContext.toString());
	}
	
}
