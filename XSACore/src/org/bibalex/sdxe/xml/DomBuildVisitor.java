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

/**
 * 
 */
package org.bibalex.sdxe.xml;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.bibalex.sdxe.controller.DomTreeController;
import org.bibalex.sdxe.exception.DomBuildException;
import org.bibalex.sdxe.exception.DomEnforceException;
import org.bibalex.sdxe.exception.DomMixedBadXmlException;
import org.bibalex.sdxe.exception.DomMixedContentNotAllowedException;
import org.bibalex.sdxe.exception.DomMixedElementNotAllowedException;
import org.bibalex.sdxe.exception.DomMixedTextNotAllowedException;
import org.bibalex.sdxe.exception.SugtnException;
import org.bibalex.sdxe.suggest.model.SugtnTreeModel;
import org.bibalex.sdxe.suggest.model.SugtnTreeNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnAttributeNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnDeclNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnElementNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnMixedNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnPCDataNode;
import org.bibalex.sdxe.suggest.model.particle.ISugtnParticleNodeVisitor;
import org.bibalex.sdxe.suggest.model.particle.SugtnAllNode;
import org.bibalex.sdxe.suggest.model.particle.SugtnChoiceNode;
import org.bibalex.sdxe.suggest.model.particle.SugtnParticleNode;
import org.bibalex.sdxe.suggest.model.particle.SugtnSequenceNode;
import org.bibalex.util.JDOMUtils;
import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;

/**
 * Used by the DomTreeController to add elements and attributes to the DOM Tree by calling its build method. While
 * building the tree, particles encountered are kept, sorted by level, so that they are later enforced using the
 * DomBuildEnforceVisitor. Particles are kept in a special data structure for PandingParticles, which facilitates
 * enforcing all particles encountered while adding many child elements or attributes under one context element.
 * 
 * @author younos
 * 
 */
public class DomBuildVisitor implements ISugtnParticleNodeVisitor {
	/**
	 * The particle is a schema component, representing a rule. This rule must be enforced on the DOM element
	 * corresponding to the declaration that the particle was encountered while it was being materialized, in the
	 * context of this DOM element's parent DOM element. However, this class doesn't store the actual particles for an
	 * element,
	 * because the element declaration used might be coming from a choice that finally will not be chosen (after all
	 * elements are added, and it is clear which choice is the correct one.. if any). This is only an intermediate
	 * structure.
	 * Finally, all the particles at a certain level in the schema tree model of the declaration of the enforcement
	 * context
	 * element(added DOM element's parent DOM element), are merged together and enforced together, each one for its
	 * element.
	 * 
	 * @author Younos.Naga
	 * 
	 */
	private static class PendingParticles {
		/**
		 * The DOM element corresponding to the declaration that the particle was encountered while it was being
		 * materialized
		 */
		private Element forElement;
		/**
		 * The particle (at the time of writing I'm not sure why it is a list, but this most probably because a particle
		 * might be added several times for the same element when adding PCDate and attributes later, or to unify the
		 * structure of particles in maps)
		 */
		private LinkedList<SugtnParticleNode> particles;
		
		/**
		 * 
		 */
		private PendingParticles() {
			this.particles = new LinkedList<SugtnParticleNode>();
		}
	}
	
	private static final Logger LOG = Logger.getLogger("org.bibalex.sdxe");
	
// Cannot reuse because a particle marked as enforced might have been altered wrongfully
// // Reuse the same enforce visitor for the same element
// // while retrying to commit..
// private HashMap<Element, DomBuildEnforceVisitor> enforceVisitorsMap = null;
	
	/**
	 * The actual particles that are enforced come from this data structure. The element (key of outer map) is the
	 * enforcement context
	 * (parent element), the inner Map maps each level (Integer) to a list of pending particles
	 */
	private HashMap<Element, HashMap<Integer, LinkedList<PendingParticles>>> pendingParticleStore = null;
	/**
	 * Intermediate structure for storing particles by level, while they are being encountered on the way from the
	 * declaration of the nearest exisiting ancestor to the declaration that is being added
	 */
	private HashMap<Integer, PendingParticles> comingElementPendingParticles = null;
	/**
	 * Whenever an element is added to DOM it is pushed on this stack. When enforcing schema elements are poped so that
	 * the deepest element is validated first. This way particles are validated only after its children are validated.
	 */
	private Stack<Element> unvalidatedElmStack = null;
	
	/**
	 * Used to return the latest added element, because the visitor does not define a return type
	 */
	private Element parentElement = null;
	
	/**
	 * To be used as the value of the PCData or Attribute node when it is encountered.
	 */
	private String nextPCData = null;
	
	/**
	 * Used to pass PCData as list of DOM nodes from the PCData method to the Mixed method
	 */
	private List nextMixedContent = null;
	
	/**
	 * This is a work around to that the visitor interface doesn't allow exceptions to be thrown. The build method
	 * checks to see if it contains something, and subsequently throws it.
	 */
	private Exception exception = null;
	
	/**
	 * Configuration of enforcing min occurs
	 */
	private boolean configMinOccEnf = false;
	
	/**
	 * Configuration of enforcing max occurs
	 */
	private boolean configMaxOccEnf = false;
	
	/**
	 * Configuration of two phase commit, to be passed to the Enforce visitor
	 */
	private boolean configTwoPhaseCommit = false;
	
	/**
	 * 
	 * @param configMinOccEnf
	 * @param configMaxOccEnf
	 * @param configTwoPhaseCommit
	 */
	public DomBuildVisitor(boolean configMinOccEnf,
			boolean configMaxOccEnf, boolean configTwoPhaseCommit) {
		
		this.configMaxOccEnf = configMaxOccEnf;
		this.configMinOccEnf = configMinOccEnf;
		this.configTwoPhaseCommit = configTwoPhaseCommit;
		this.pendingParticleStore = new HashMap<Element, HashMap<Integer, LinkedList<PendingParticles>>>();
		this.unvalidatedElmStack = new Stack<Element>();
	}
	
	/**
	 * Adds a particle to the data structure of its level for the next element to be encountered
	 * 
	 * @param node
	 */
	private void addPendingParticle(SugtnParticleNode node) {
		
		int levelFromParentElm = 1;
		SugtnTreeNode sugtnParent = (SugtnTreeNode) node.getParent();
		
		while (!(sugtnParent instanceof SugtnElementNode)) {
			
			sugtnParent = (SugtnTreeNode) sugtnParent.getParent();
			levelFromParentElm++;
			
		}
		
		if (this.comingElementPendingParticles == null) {
			
			this.comingElementPendingParticles = new HashMap<Integer, PendingParticles>();
			
		}
		
		PendingParticles pendingParticles = null;
		
		if (this.comingElementPendingParticles.containsKey(Integer.valueOf(levelFromParentElm))) {
			
			pendingParticles = this.comingElementPendingParticles.get(Integer
					.valueOf(levelFromParentElm));
			
		} else {
			
			pendingParticles = new PendingParticles();
			this.comingElementPendingParticles.put(Integer.valueOf(levelFromParentElm),
					pendingParticles);
			
		}
		
		pendingParticles.particles.add(node);
	}
	
	/**
	 * 
	 */
	@Override
	public void allNode(SugtnAllNode node) {
		this.addPendingParticle(node);
		
	}
	
	/**
	 * Set the attribute of parent element by nextPcdata
	 */
	@Override
	public void attributeNode(SugtnAttributeNode node) {
		
		if ((this.nextPCData == null) || this.nextPCData.isEmpty()) {
			// Do not add empty attributes
			return;
		}
		
		String name = node.getLocalName();
		Namespace ns = this.resolveTargetNamespace(node);
		
		// TODO: consider providing type information to JDOM
		Attribute attribute = new Attribute(name, this.nextPCData, ns);
		
		this.parentElement.setAttribute(attribute);
		if (LOG.isDebugEnabled()) {
			LOG
					.debug("Attribute (" + node + ") added to element ("
							+ this.parentElement.toString());
		}
	}
	
	/**
	 * Entry point. Just to make up for that the visitor interface doesn't allow returning or throwing.
	 * 
	 * @param suggestnNode
	 * @param domElParent
	 * @return
	 * @throws DomBuildException
	 */
	public Element build(SugtnTreeNode suggestnNode, Element domElParent) throws DomBuildException {
		this.parentElement = domElParent;
		suggestnNode.invite(this);
		if (this.exception != null) {
			DomBuildException exTemp = (DomBuildException) (this.exception instanceof DomBuildException ? this.exception
					: new DomBuildException(this.exception));
			this.exception = null;
			throw exTemp;
		}
		return this.parentElement;
	}
	
	/**
	 * 
	 */
	@Override
	public void choiceNode(SugtnChoiceNode node) {
		this.addPendingParticle(node);
	}
	
	/**
	 * An element declaration node has been encountered. Create the corresponding element and add it to the parent
	 * element. It will be the parent element of subsequent additions, so return it as the parent element. And merge its
	 * pending particles to the actual pending particles structure of the enforcement context.
	 */
	@Override
	public void elementNode(SugtnElementNode node) {
		String name = node.getLocalName();
		Namespace ns = this.resolveTargetNamespace(node);
		
		Element element = new Element(name, ns);
		
		this.parentElement.addContent(element);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("Element (" + element.toString() + ") added to parent element ("
					+ this.parentElement + ")");
		}
		
		// /// Add this element's pending particles to it's context's particles
		
		// Get the context particles
		HashMap<Integer, LinkedList<PendingParticles>> contextParticles = null;
		
		if (this.pendingParticleStore.containsKey(this.parentElement)) {
			
			contextParticles = this.pendingParticleStore.get(this.parentElement);
			
		} else {
			
			contextParticles = new HashMap<Integer, LinkedList<PendingParticles>>();
			this.pendingParticleStore.put(this.parentElement, contextParticles);
			
		}
		
		// Merge particles with context particles
		// Traversing the hashmap without depending on its structure
		// but depending that levels are from 1 to the the map size
		for (int i = 1; i <= this.comingElementPendingParticles.size(); ++i) {
			
			PendingParticles levelParticlesNew = this.comingElementPendingParticles
					.get(Integer.valueOf(i));
			
			LinkedList<PendingParticles> levelParticlesOldList = null;
			
			if (contextParticles.containsKey(Integer.valueOf(i))) {
				
				levelParticlesOldList = contextParticles.get(Integer.valueOf(i));
				
			} else {
				
				levelParticlesOldList = new LinkedList<PendingParticles>();
				contextParticles.put(Integer.valueOf(i), levelParticlesOldList);
				
			}
			
			levelParticlesNew.forElement = element;
			levelParticlesOldList.add(levelParticlesNew);
			
		}
		
		this.comingElementPendingParticles.clear();
		this.comingElementPendingParticles = null;
		
		// ////////place element in unvalidated elements stack
		this.unvalidatedElmStack.push(element);
		
		// ////// the new element is the new parent element
		this.parentElement = element;
	}
	
	/**
	 * Entry point. This should be called when adding elements and attributes is finished, so that it runs the
	 * enforcers. If it fails, changes can be made to fix the DOM then it can be called again.
	 * 
	 * @param newElmsRoot
	 * @throws DomEnforceException
	 */
	public void enforceParticles(Element newElmsRoot) throws DomEnforceException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Enforcing validity of all unvalidated elements");
		}
		
		HashMap<Element, DomBuildEnforceVisitor> enforceVisitorStore = new HashMap<Element, DomBuildEnforceVisitor>();
		
		while (!this.unvalidatedElmStack.isEmpty()) {
			Element context = null, unvalidated = null;
			
			try {
				unvalidated = this.unvalidatedElmStack.pop();
				context = unvalidated.getParentElement();
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("Enforcing validity of element (" + unvalidated);
				}
				
				// Check if this unvalidated element is still
				// in the DOM tree
				
				Element eParent = context;
				
				while ((eParent != null) && (eParent != newElmsRoot)) {
					eParent = eParent.getParentElement();
				}
				
				if ((eParent == null) || (eParent != newElmsRoot)) {
					
					// this element has been detached.. continue
					if (LOG.isDebugEnabled()) {
						LOG.debug("Element (" + unvalidated
								+ ") isnot part of the DOM tree any more.. skipping.");
					}
					
					continue;
				}
				
				DomBuildEnforceVisitor enforceVisitor = enforceVisitorStore.get(context);
				if (enforceVisitor == null) {
					enforceVisitor = new DomBuildEnforceVisitor(context, this.configMinOccEnf,
							this.configMaxOccEnf, this.configTwoPhaseCommit);
					
					enforceVisitorStore.put(context,
							enforceVisitor);
				}
				
				// PPendingParticles instead of SugtnParticleNode
				HashMap<Integer, LinkedList<PendingParticles>> elmParticleLevels;
				elmParticleLevels = this.pendingParticleStore.get(context);
				
				// Traversing the Hashmap without depending on its structure
				// but rather the knowledge that levels are continuous
				for (int i = elmParticleLevels.size(); i > 0; --i) {
					
					LinkedList<PendingParticles> levelPendingParticles;
					levelPendingParticles = elmParticleLevels.get(Integer.valueOf(i));
					
					Iterator<PendingParticles> ppIter = levelPendingParticles.iterator();
					while (ppIter.hasNext()) {
						
						PendingParticles pp = ppIter.next();
						
						// Check if this particle list is still relevant
						
						Element ppParent = pp.forElement;
						while ((ppParent != null) && (ppParent != newElmsRoot)) {
							ppParent = ppParent.getParentElement();
						}
						
						if ((ppParent == null) || (ppParent != newElmsRoot)) {
							
							// this element has been detached
							if (LOG.isDebugEnabled()) {
								LOG
										.debug("Element ("
												+ unvalidated
												+ ") for pending particle list, isnot part of the DOM tree any more.. skipping particles.");
							}
							
							// delete these pending particles
							ppIter.remove();
							
							// and continue
							continue;
						}
						
						// enforce the particles because they are relevant
						
						for (SugtnParticleNode particle : pp.particles) {
							
							if (LOG.isDebugEnabled()) {
								LOG.debug("Enfocing level " + i
										+ " particle (" + particle + ")"
										+ " in context <" + context + ">"
										+ " to validate <" + unvalidated + ">");
							}
							
							enforceVisitor.enforce(particle);
							
							// When we reach the top particle for an element
							// we know all the children (old and new) that occurs
							// and can now reorder its children..
							if (i == 1) { // Equivalent To: particle.getParent() instanceof SugtnElementNode) {
								enforceVisitor.enforceOrder(particle);
							}
						}
					}
				}
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("Element validated: " + unvalidated);
				}
				
			} catch (DomEnforceException e) {
				// This element is still not valid
				this.unvalidatedElmStack.push(unvalidated);
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("Returned element (" + unvalidated + ") to stack for revalidation");
				}
				
				// Rethrowwwww
				throw e;
			}
			
		}
		
		// The enforcement was successful,
// //we don't need to keep the enofrcers for retying
// this.enforceVisitorsMap.clear();
// Clear all pending particles
		this.pendingParticleStore.clear();
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("Enforced validaty of all unvalidated elements successfully");
		}
	}
	
	public boolean isConfigMinOccEnf() {
		return this.configMinOccEnf;
	}
	
	/**
	 * This method depends on that PCDataNode will be called before it, because the Mixed node is a PCData node, so the
	 * invite method of Mixed calls both.
	 * 
	 * If there is no nextMixedContent it just returns, and this is why nextMixedContent is moved to localMixedContent
	 * in the begining of the method (to prevent possible infinite recursive calls).
	 * 
	 * This method takes the list of Dom nodes in nextMixedContent, it makes sure that each element node of them is
	 * allowed in the schema tree, and that each text node lies under an element node that allows PCData. It uses this
	 * class's build method and calls it in a similar fashion like DomTreeController.addSgutn, just with slight
	 * modifications.
	 * 
	 * Mixed content can be coming from a mixed field which contains text interleaved with elements, or a mixed
	 * container which contains text then elements.
	 */
	@Override
	public void mixedNode(SugtnMixedNode node) {
		if (this.nextMixedContent == null) {
			// The PCData was not valid mixed content
			return;
		}
		
		List localMixedContent = this.nextMixedContent;
		this.nextMixedContent = null;
		
		this.parentElement.removeContent();
		
		for (Object obj : localMixedContent) {
			
			if (obj instanceof Element) {
				
				Element elt = (Element) obj;
				
				String eltName = elt.getName();
				
				SugtnElementNode sugtnParent = node.getParentSugtnElement();
				SugtnTreeNode sugtnNode = SugtnTreeModel.findNodeByLocalNameRecursive(sugtnParent,
						eltName, true);
				
				if ((sugtnNode != null) && (sugtnNode instanceof SugtnElementNode)) {
					
					try {
						this.setNextPCData(JDOMUtils.getElementContentAsString(elt, Format
								.getCompactFormat()));
						
					} catch (IOException e) {
						// TODO what to do??
						e.printStackTrace();
					}
					
					Element currParent = this.parentElement;
					try {
						Stack<SugtnTreeNode> particlesAndElt = new Stack<SugtnTreeNode>();
						
						// TODONE: we need to move to the new domElt sugtn domElt to build its tree
						// Start from the PCData domElt so that the content gets added
						try {
							sugtnNode = ((SugtnElementNode) sugtnNode).populateChildren();
						} catch (SugtnException e) {
							this.exception = e;
							return;
						}
						
						SugtnPCDataNode pcDataNode = ((SugtnElementNode) sugtnNode).getPcDataNode();
						if (pcDataNode != null) {
							sugtnNode = pcDataNode;
						} else {
							boolean containsText = ((elt.getText() != null) && !elt.getText()
									.isEmpty());
							if (containsText) {
								this.exception = new DomMixedTextNotAllowedException(elt.getName());
								return;
							} else {
								// No text and no PCData.. not a big problem, and probably
								// this is not an annotator, but a child of a mixed container.. add as is
								// TODO: always doing this for mixed containers would be perfect, but detecting the case
								// needs some work
								this.parentElement.addContent(elt);
								continue;
							}
						}
						
						while (sugtnNode != sugtnParent) {
							particlesAndElt.add(sugtnNode);
							sugtnNode = (SugtnTreeNode) sugtnNode.getParent();
						}
						
						while (!particlesAndElt.isEmpty()) {
							try {
								this.build(particlesAndElt.pop(),
										this.parentElement);
							} catch (DomBuildException e) {
								this.exception = e;
								return;
							}
						}
						
						Element newElt = this.parentElement;
						for (Object attrObj : elt.getAttributes()) {
							Attribute attr = (Attribute) attrObj;
							newElt.setAttribute(attr.getName(), attr.getValue(), attr
									.getNamespace());
						}
						
					} finally {
						this.parentElement = currParent;
					}
				} else {
					this.exception = new DomMixedElementNotAllowedException(eltName, sugtnParent);
					return;
				}
				
			} else {
				
				// TODO is this safe or should we check for and remove CDATA, Comment, ProcessingInstruction, and
				// EntityRef.
				this.parentElement.addContent((Content) obj);
				
			}
		}
		
	}
	
	/**
	 * If the node is really just a PCData node this method makes sure that the nextPCdata is only text data and then
	 * adds it to the DOM. If the node is a Mixed node prepares the PCData as mixed content for the subseuqnet call of
	 * mixedNode.
	 */
	@Override
	public void pcdataNode(SugtnPCDataNode node) {
		SAXBuilder saxBuilder = new SAXBuilder(false);
		
		String pfx = this.parentElement.getNamespacePrefix();
		String xmlns = "xmlns";
		
		if ((pfx != null) && !pfx.isEmpty()) {
			xmlns += ":" + pfx;
			pfx += ":";
		}
		
		xmlns += "=\"" + this.parentElement.getNamespaceURI() + "\"";
		
		String xmlString = "<" + pfx + "mixedRoot " + xmlns + ">" + this.nextPCData + "</" + pfx
				+ "mixedRoot>";
		
		Reader charStream = new StringReader(xmlString);
		try {
			Document xmlDoc = saxBuilder.build(charStream);
			List xmlContent = xmlDoc.detachRootElement().cloneContent();
			xmlDoc = null;
			if (node instanceof SugtnMixedNode) {
				// Defer handling this to the mixed domElt call that will be coming
				this.nextMixedContent = xmlContent;
			} else {
				// not mixed allowed.. so must be only a text domElt
				if ((xmlContent.size() == 1) && ((xmlContent.get(0)) instanceof Text)) {
					this.parentElement.setContent(xmlContent);
				} else {
					this.exception = new DomMixedContentNotAllowedException(node);
					return;
				}
			}
		} catch (JDOMException e) {
			this.exception = new DomMixedBadXmlException(node, this.nextPCData, e);
			return;
		} catch (IOException e) {
			// TODO what to do??
			e.printStackTrace();
		}
	}
	
	/**
	 * Clears the particles data structures (call when rolling back)
	 */
	public void purgeParticles() {
		this.pendingParticleStore.clear();
		this.unvalidatedElmStack.clear();
		if (LOG.isDebugEnabled()) {
			LOG.debug("Pending particles purged");
		}
	}
	
	/**
	 * 
	 * @param node
	 * @return
	 */
	private Namespace resolveTargetNamespace(SugtnDeclNode node) {
		
		Namespace result = null;
		
		result = DomTreeController.acquireNamespace(node.getTargetNamespaceURI(),
				this.parentElement);
		
		return result;
		
	}
	
	/**
	 * 
	 */
	@Override
	public void sequenceNode(SugtnSequenceNode node) {
		this.addPendingParticle(node);
	}
	
	/**
	 * 
	 * @param configMinOccEnf
	 */
	public void setConfigMinOccEnf(boolean configMinOccEnf) {
		this.configMinOccEnf = configMinOccEnf;
	}
	
	/**
	 * 
	 * @param nextPCData
	 */
	public void setNextPCData(String nextPCData) {
		this.nextPCData = nextPCData;
	}
	
}
