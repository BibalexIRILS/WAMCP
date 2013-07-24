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

package org.bibalex.sdxe.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.bibalex.jdom.XPathBuilder;
import org.bibalex.sdxe.exception.DomBindingException;
import org.bibalex.sdxe.exception.DomBuildChangesException;
import org.bibalex.sdxe.exception.DomBuildException;
import org.bibalex.sdxe.exception.DomBuildSugtnTreeSyncException;
import org.bibalex.sdxe.exception.DomEnforceException;
import org.bibalex.sdxe.exception.DomRequiredAttributeException;
import org.bibalex.sdxe.exception.SDXEException;
import org.bibalex.sdxe.suggest.model.SugtnTreeNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnAttributeNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnDeclNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnDeclQName;
import org.bibalex.sdxe.suggest.model.dom.SugtnDomAddableNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnElementNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnMixedNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnPCDataNode;
import org.bibalex.sdxe.suggest.model.particle.SugtnParticleNode;
import org.bibalex.sdxe.xml.DomBuildEnforceVisitor;
import org.bibalex.sdxe.xml.DomBuildVisitor;
import org.bibalex.util.JDOMUtils;
import org.bibalex.util.XPathStrUtils;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

/**
 * This class is used to manipulate an XML DOM tree in a way that keeps it schema compliant. It has two modes of
 * operation; one that uses pointers, and another that uses XPathes. The pointers mode depends on having an Active
 * element, under which new elements can be added, and which would be deleted, .. etc. The XPathes mode uses the XPath
 * to indicate which element would be the active element. The word brothers should mean instances of the same element
 * who have the same designating attributes. In pointers mode designating attributes are not considered at all, so Bros
 * is used to mean siblings with the same element tag disregarding attributes, in pointers mode.
 * 
 * All operations indicate what element or attribute would be added or deleted by sending a Schema Tree node.
 * 
 * The controller can be configured to enforce the minOccurs and maxOccurs defined in the schema or not.
 * If minOccurs is to be enforced, the whole enforcing step must be delayed. This allows adding a group of elements
 * which must come together and then enforcing the schema. Otherwise, enforcing after adding any of them but not the
 * others would fail. This is done by adding new elements to a clone of the currently active element and its
 * predecessors subtree. After adding all the required elements, the subtree is enforced and if it passes, it replaces
 * the original subtree. This is called two phase commit/rollback.
 * 
 * If minOccurs is not to be enforced, the two phase commit is a big overhead, because cloning of parts of a DOM tree
 * could be very expensive. So there is another configuration to turn off two phase commit.
 * 
 * @author Younos.Naga
 * 
 */
public class DomTreeController {
	// TODO: when adding many isntance of element.. active is usually last, or something like that, and this breaks the
	// addition... fix if you are planning to use by moving not XPath
	// TODONE: implement commit/rollback for deletion
	private static final Logger LOG = Logger.getLogger("org.bibalex.DomTreeController");
	
	private static final String DUMMY_PARENT_ELT_NAME = "dummy";
	
	/**
	 * Use this method for namespace object creation, within the DOM tree
	 * 
	 * @param nsURI
	 *            Namespace URI
	 * @param parentElement
	 *            The element under which the namespace would work
	 * @param forXPath
	 *            If true, the namespace is prefixed even if it is the default NS
	 * @param nsPrefixForNewNS
	 *            Use this prefix if the namespace is not already defined
	 * @return A namespace object for the URI passed, under the parentElement.
	 */
	public static Namespace acquireNamespace(String nsURI, Element parentElement,
			boolean forXPath, String nsPrefixForNewNS) {
		
		Namespace result = null;
		
		if ((nsURI == null) || nsURI.equals("")) {
			result = Namespace.NO_NAMESPACE;
		} else if (nsURI.equals(Namespace.XML_NAMESPACE.getURI())) {
			result = Namespace.XML_NAMESPACE;
		} else {
			Namespace parentNamespace = parentElement.getNamespace();
			if (nsURI.equals(parentNamespace.getURI())) {
				if (!forXPath) {
					result = parentNamespace;
				} else { // XPath doesn't allow no prefix namespace (default ns)
					if (parentNamespace.getPrefix().equals("")) {
						result = Namespace
								.getNamespace(XPathBuilder.DEFAULT_NS_XPATH_PREFIX, nsURI);
					} else {
						result = parentNamespace;
					}
				}
			} else {
				// TODO: make sure that passing the empty string to get
				// the namespace as a default namespace doesn't go into
				// this if statement
				if (nsPrefixForNewNS == null) {
					synchronized (DomTreeController.class) {
						nsPrefixForNewNS = "ns" + ++namespaceCounter;
					}
				}
				result = Namespace.getNamespace(nsPrefixForNewNS, nsURI);
			}
		}
		
		return result;
		
	}
	
	/**
	 * Use this method for namespace object creation from a URI
	 * 
	 * @param attrNsUri
	 * @return
	 */
	public static Namespace acquireNamespaceNoParent(String attrNsUri) {
		Namespace attrNs;
		if ((attrNsUri != null) && !attrNsUri.isEmpty()) {
			if (Namespace.XML_NAMESPACE.getURI().equals(attrNsUri)) {
				attrNs = Namespace.XML_NAMESPACE;
			} else {
				attrNs = Namespace.getNamespace(attrNsUri);
			}
		} else {
			attrNs = Namespace.NO_NAMESPACE;
		}
		return attrNs;
	}
	
	/**
	 * Use this method for namespace object creation from a declaration node
	 * 
	 * @param sugtnDecl
	 * @return
	 */
	public static Namespace acquireNamespaceNoParent(SugtnDeclNode sugtnDecl) {
		String attrNsUri = sugtnDecl.getTargetNamespaceURI();
		return acquireNamespaceNoParent(attrNsUri);
	}
	
	/**
	 * Use this ns prefix when creating an XPath for elements in the namespace of the root element
	 */
	private String nsXpPfx = null;
	
	/**
	 * Add this namespace to XPathes
	 */
	private Namespace nsXp = null;
	
	/**
	 * The registered observers, which allow the View to act upon model changes and events
	 */
	private Vector<IDomObserver> domObservers = null;
	
	/**
	 * The DOM Tree being controlled
	 */
	private Document domTotal = null;
	
	/**
	 * The index of the Clone root element inbetween its siblings
	 */
	private int domCloneActiveRootIx = -1;
	
	/**
	 * The Clone root element is attached to this dummy parent
	 */
	private Element domCloneActiveRootParent = null;
	
	/**
	 * The currently active element in the Clone subtree
	 */
	private Element domCloneActiveEl = null;
	
	/**
	 * The currently active element in the real DOM
	 */
	private Element domRealActiveRoot = null;
	
	/**
	 * 
	 */
	private DomBuildVisitor domBuildVisitor = null;
	
	/**
	 * Brothers list; brothers SHOULD be instances of the same XML element with the same designating attributes.
	 * However, this was not yet understood when the pointers mode was being developed. So don't expect this.
	 * To move inbetween brothers of the active element, and to add and remove other brothers, this list is used
	 */
	private ListIterator domActiveBrosItr = null;
	
	/**
	 * The size of the brothers list above
	 */
	private int domActiveBrosSize = -1;
	
	// TODO: refactor this class into two classes.. one with pointers
	// and one with Xpathes
	/**
	 * This field determines if the list of Active Bros needed when
	 * walking the tree using pointers is maintained. For performance
	 * improvement when using XPathes
	 */
	// TODO: private boolean activeBrosMaintained = true;
	
	/**
	 * 
	 */
	private static int namespaceCounter = 0;
	
	/**
	 * Same as public static Namespace acquireNamespace(String nsURI, Element parentElement,
	 * boolean forXPath, String nsPrefixForNewNS)
	 * 
	 * @param nsURI
	 * @param parentElement
	 * @return
	 */
	public static Namespace acquireNamespace(String nsURI, Element parentElement) {
		return acquireNamespace(nsURI, parentElement, false, null);
	}
	
	/**
	 * public static Namespace acquireNamespace(String nsURI, Element parentElement,
	 * boolean forXPath, String nsPrefixForNewNS)
	 * 
	 * @param nsURI
	 * @param parentElement
	 * @param forXPath
	 * @return
	 */
	public static Namespace acquireNamespace(String nsURI, Element parentElement,
			boolean forXPath) {
		return acquireNamespace(nsURI, parentElement, forXPath, null);
		
	}
	
	/**
	 * Configuration that indicates whether to enforce minOccurs or not
	 */
	private boolean configMinOccEnf = false;
	
	/**
	 * Configuration that indicates whether to enforce maxOccurs or not
	 */
	private boolean configMaxOccEnf = false;
	
	/**
	 * Configuration that indicates whether to edit in a clone or not
	 */
	private boolean configTwoPhaseCommit = false;
	
	/**
	 * 
	 * @param configMinOccEnf
	 * @param configMaxOccEnf
	 * @param configTwoPhaseCommit
	 */
	public DomTreeController(boolean configMinOccEnf,
			boolean configMaxOccEnf, boolean configTwoPhaseCommit) {
		
		this.configMaxOccEnf = configMaxOccEnf;
		this.configMinOccEnf = configMinOccEnf;
		this.configTwoPhaseCommit = configTwoPhaseCommit;
		
		this.domCloneActiveRootParent = new Element(DUMMY_PARENT_ELT_NAME);
		this.domObservers = new Vector<IDomObserver>();
	}
	
	/**
	 * Creates a namepsapce object for the URI passed, under the active element
	 * 
	 * @param nsURI
	 * @param forXPath
	 * @return
	 */
	public Namespace acquireNamespaceInActive(String nsURI, boolean forXPath) {
		Element parentElement = null;
		if (this.getChangesExist()) {
			parentElement = this.domCloneActiveRootParent;
		} else {
			parentElement = this.domTotal.getRootElement();
		}
		return acquireNamespace(nsURI, parentElement, forXPath);
	}
	
	/**
	 * Creates a namepsapce object for the URI passed, under the root element
	 * 
	 * @param nsURI
	 * @param forXPath
	 * @return
	 */
	public Namespace acquireNamespaceInDocRoot(String nsURI, boolean forXPath) {
		Element root = this.domTotal.getRootElement();
		
		return acquireNamespace(nsURI, root, forXPath);
	}
	
	/**
	 * In two phase commit mode, this method creates a clone of the currently active element and its subtree, if one is
	 * not already created.
	 * If two phase commit is disabled, it points the clone to the original, if it is not already pointed.
	 * 
	 * @return true if a new clone is created or a pointed.
	 */
	private boolean activateClone() {
		if (!this.configTwoPhaseCommit) {
			// YA 20100627: removing 2 phase addition
			// since the minoccurs problem prevents us from checking required
			// elements there is no point in have 2 phase addition
			if (this.domCloneActiveEl == null) {
				
				// Use the orig with No cloning: .clone();
				Element domCloneActiveRoot = (Element) this.domRealActiveRoot;
				
				// and use the parent to refer to the orig.. misnaming is fine
				this.domCloneActiveRootParent = domCloneActiveRoot;
				
				Element domRealActiveRootParent = this.domRealActiveRoot
						.getParentElement();
				if (domRealActiveRootParent != null) {
					this.domCloneActiveRootIx = domRealActiveRootParent
							.indexOf(this.domRealActiveRoot);
				}
				
				// This is the pointer to the currently active element in the clone
				this.domCloneActiveEl = domCloneActiveRoot;
				return true;
			}
		} else {
			// if the minOccurs XSOM problem is fixed use this code (YA 20100627)
			// This clone is to hold the additions until the Doc is updated
			if (this.domCloneActiveEl == null) {
				Element domCloneActiveRoot = (Element) this.domRealActiveRoot
						.clone();
				this.domCloneActiveRootParent.addContent(domCloneActiveRoot); // shouldn't need: .detach());
				
				Element domRealActiveRootParent = this.domRealActiveRoot
						.getParentElement();
				if (domRealActiveRootParent != null) {
					this.domCloneActiveRootIx = domRealActiveRootParent
							.indexOf(this.domRealActiveRoot);
				}
				
				// This is the pointer to the currently active element in the clone
				this.domCloneActiveEl = domCloneActiveRoot;
				return true;
			}
		}
		return false;
		
	}
	
	/**
	 * Same as private Element addSuggested(SugtnDomAddableNode sugtnNode,
	 * String pcData,
	 * boolean moveToNew)
	 * 
	 * @param sugtnNode
	 * @throws DomBuildException
	 */
	protected void addElement(SugtnElementNode sugtnNode)
			throws DomBuildException {
		this.addSuggested(sugtnNode, null);
	}
	
	/**
	 * Use this to add the namespace prefix to each XPath step
	 * 
	 * @param sugtnPathString
	 * @return
	 */
	public String addNsPfxToXPath(String sugtnPathString) {
		return XPathBuilder.addNamespacePrefix(sugtnPathString, this.nsXpPfx);
	}
	
	/**
	 * Same as private Element addSuggested(SugtnDomAddableNode sugtnNode,
	 * String pcData,
	 * boolean moveToNew)
	 * 
	 * @param sugtnNode
	 * @param pcData
	 * @return
	 * @throws DomBuildException
	 */
	private Element addSuggested(SugtnDomAddableNode sugtnNode,
			String pcData) throws DomBuildException {
		
		return this.addSuggested(sugtnNode, pcData, true);
		
	}
	
	/**
	 * For pointers mode. Use this method to materialize a schema tree node (element, attribute, or PCdate), into an XML
	 * DOM node. The PC
	 * data is added to its as a value. If it has parent elements according to the schema, but not in the XML, all of
	 * these elements are also added.
	 * 
	 * @param sugtnNode
	 * @param pcData
	 *            The value to add to the new node
	 * @param moveToNew
	 *            Whether to keep the clone active pointer at its place or move it to the newly added element
	 * @return The first newly added parent element (not necessarily the direct parent of the newly added node). If no
	 *         new parent element are added. the current active element in the clone subtree (which would be the direct
	 *         parent element of the added node) is returned.
	 * @throws DomBuildException
	 */
	private Element addSuggested(SugtnDomAddableNode sugtnNode,
			String pcData,
			boolean moveToNew)
			throws DomBuildException {
		Element result = null;
		this.guardUninit();
		
		Stack<SugtnTreeNode> suggestnStack = null;
		boolean unactivate = false;
		try {
			suggestnStack = new Stack<SugtnTreeNode>();
			SugtnTreeNode suggestnParent;
			
			unactivate = this.activateClone();
			
			// Fill the stack with the suggested addition and any
			// particles above it:
			// (could be also elements if adding multiple
			// elements at once)
			suggestnStack.push(sugtnNode);
			
			suggestnParent = sugtnNode;
			do {
				suggestnParent = (SugtnTreeNode) suggestnParent.getParent();
				if (suggestnParent == null) {
					throw new DomBuildSugtnTreeSyncException(
							"No parent for sugtnTreeNode! Probably two elements of the same type are being created at a time.");
				} else {
					
					if (suggestnParent instanceof SugtnElementNode) {
						SugtnElementNode sugtnParentAsElm = (SugtnElementNode) suggestnParent;
						if (this.domCloneActiveEl.getName().equals(
								sugtnParentAsElm.getLocalName()) && this.domCloneActiveEl
								.getNamespaceURI().equals(sugtnParentAsElm
										.getTargetNamespaceURI())) {
							
							// We have reached the sugtnNode equivalent to the
							// XML element where addition has started
							break;
							
						} else if (suggestnParent.isRoot()) {// == suggestnRoot) {
							throw new DomBuildSugtnTreeSyncException(
									"Suggestion model root reached without finding the corresponding suggestion element for the element: "
											+ this.xpathForActiveInDom());
						}
					}
					
					suggestnStack.push(suggestnParent);
				}
				
			} while (true);
			
			// Build according to stack:
			
			this.domBuildVisitor.setNextPCData(pcData);
			
			Element domLastNewParent = this.domCloneActiveEl;
			
			SugtnTreeNode suggestnNode = null;
			
			// We don't always add to direct parent then we need this
			// stack and the loops above and below
			while (!suggestnStack.empty()) {
				suggestnNode = suggestnStack.pop();
				
				domLastNewParent = this.domBuildVisitor.build(suggestnNode,
						domLastNewParent);
				
				if ((result == null) && (domLastNewParent != this.domCloneActiveEl)) {
					result = domLastNewParent; // This will be the FIRST new parent
				}
			}
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("Element (" + domLastNewParent
						+ ") added to DOM tree according to suggestion (" + sugtnNode + ")");
			}
			
			if (moveToNew) {
				// move to the new context
				// this.domCloneActiveEl = domLastNewParent;
				this.moveCloneToElement(domLastNewParent);
			}
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("DOM Clone is now at: " + this.domCloneActiveEl);
			}
			
			// Why would we return null? Even if we'd return domClone.. better than null
			if (result == null) {
				result = this.domCloneActiveEl;
			}
		} finally {
			while (!suggestnStack.empty()) {
				suggestnStack.pop();
			}
			
			// YA 20100627: removing 2 phase addition
			if (!this.configTwoPhaseCommit && unactivate) {
				this.commitToRealDom();
			}
		}
		
		return result;
	}
	
	/**
	 * For XPathes mode. Internally uses private Element addSuggested(SugtnDomAddableNode sugtnNode,
	 * String pcData,
	 * boolean moveToNew) of pointers mode
	 * 
	 * @param xPathAbs
	 *            The absolute XPath of the element under which the declaration node is to be materialized. If the XPath
	 *            element is still not present, it will be created. The addition will start from the nearest existing
	 *            ancestor of the XPath element.
	 * @param sugtnDecl
	 *            The declaration node. Can be an attribute or an element declaration.
	 * @return The first parent element added, or the direct parent of the newly added node
	 * @throws JDOMException
	 * @throws SDXEException
	 */
	public Element addSugtnAtXPathRelToActive(String xPathAbs, SugtnDeclNode sugtnDecl)
			throws JDOMException, SDXEException {
		Element result = null;
		this.guardUninit();
		this.guardXpToElement(xPathAbs);
		
		this.activateClone();
		// YA 20100627: removing 2 phase addition
		try {
			// END // YA 20100627: removing 2 phase addition
			
			Object xpRes = null;
// String xpStr = this.addNsPfxToXPath(xPathAbs);
			
			do {
				xpRes = this.singleNodeAtXPathRelative(xPathAbs);
				
				if (xpRes == null) {
					String xpStrNew = XPathStrUtils.removeLastStep(xPathAbs);
					if (xPathAbs.equals(xpStrNew)) {
						break;
					} else {
						
						xPathAbs = xpStrNew;
						
					}
				}
				
			} while (xpRes == null);
			
			if (xpRes != null) {
				Element origPos = this.domCloneActiveEl;
				try {
					
					this.domCloneActiveEl = (Element) xpRes;
					
					String pcData = null;
					if (sugtnDecl instanceof SugtnAttributeNode) {
						pcData = " "; // otherwise it will not be added
					}
					
					result = this.addSuggested(sugtnDecl, pcData,
							(sugtnDecl instanceof SugtnAttributeNode));
					
					if (sugtnDecl instanceof SugtnAttributeNode) {
						this.domCloneActiveEl.setAttribute(sugtnDecl.getLocalName(), "",
								acquireNamespaceNoParent(sugtnDecl.getTargetNamespaceURI()));
						
						// YA20100729 Adding an attribute to an existing element Bug Fix
						if (result == xpRes) {
							// no new element added.. just an attribute in an existing element
							result = null;
						}
						// END YA
					}
					
				} finally {
					this.domCloneActiveEl = origPos;
				}
			}
			// YA 20100627: removing 2 phase addition
		} finally {
			if (!this.configTwoPhaseCommit) {
				this.commitToRealDom();
			}
		}
		// END // YA 20100627: removing 2 phase addition
		return result;
	}
	
	/**
	 * Release pointers to the DOM tree being controlled, so that it gets garbage collected
	 * 
	 * @throws DomBuildException
	 */
	protected void close() throws DomBuildException {
		this.guardChanges();
		
		this.domTotal = null;
		
		this.domRealActiveRoot = null;
		
		this.domBuildVisitor = null;
		this.domActiveBrosItr = null;
		this.domActiveBrosSize = -1;
		this.domCloneActiveRootIx = -1;
		this.domCloneActiveEl = null;
		
		this.nsXp = null;
		this.nsXpPfx = null;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("XML file closed");
		}
		
	}
	
	/**
	 * If two phase commit is active, this commits! This must be called eitherway to enforce pending particles like
	 * sequence order
	 * 
	 * @return
	 * @throws DomBuildException
	 */
	protected String commitToRealDom() throws DomBuildException {
		// YA 20100627: removing 2 phase addition
		if (!this.configTwoPhaseCommit) {
			try {
				this.guardNoChanges();
			} catch (DomBuildException e) {
				return this.xpathForElementInDom(this.domRealActiveRoot);
			}
			
			String result = null;
			
			Element domCloneActiveRoot = this.domCloneActiveRootParent;
			
			// Enforce the pending particles:
			this.domBuildVisitor.enforceParticles(domCloneActiveRoot);
			
			result = this.xpathForElementInDom(this.domCloneActiveEl);
			this.domCloneActiveEl = null; // must be nulled before we try to move
			
			// Reposition Real to the newly created element
			// because the old Real is now not part of Dom Tree
			this.moveRealToElement(domCloneActiveRoot);
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("DOM changes committed successfully");
			}
			
			return result;
		} else {
			
			this.guardNoChanges();
			
			String result = null;
			
			Element domCloneActiveRoot = this.domCloneActiveRootParent
					.getChild(this.domRealActiveRoot.getName(), this.domRealActiveRoot
							.getNamespace());
			
			// Enforce the pending particles:
			this.domBuildVisitor.enforceParticles(domCloneActiveRoot);
			
			Element domRealActiveRootParent = this.domRealActiveRoot
					.getParentElement();
			if (domRealActiveRootParent == null) {
				// Document is the parent
				this.domTotal.removeContent(this.domRealActiveRoot);
				this.domTotal.addContent(domCloneActiveRoot.detach());
			} else {
				domRealActiveRootParent.removeContent(this.domRealActiveRoot);
				
				domRealActiveRootParent.addContent(this.domCloneActiveRootIx,
						domCloneActiveRoot.detach());
			}
			
			result = this.xpathForElementInDom(this.domCloneActiveEl);
			this.domCloneActiveEl = null; // must be nulled before we try to move
			
			// Reposition Real to the newly created element
			// because the old Real is now not part of Dom Tree
			this.moveRealToElement(domCloneActiveRoot);
			
			// //TODONE: do we always need to reposition to newly created element
			// // or is this side effect sometimes undesireable? YESSSS!
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("DOM changes committed successfully");
			}
			
			return result;
		}
		// END // YA 20100627: removing 2 phase addition
	}
	
	/**
	 * Creates an XPath from a Sugtn Path depending that the Sugtn Path
	 * will have all nodes from the same target namespace, which is that
	 * of the root element. So the NS and its prefix will be those of Root
	 * However, this XPath doesn't necessarily return occurrences of a dataholder instance, because it doesn't contain
	 * designating attributes.
	 * 
	 * @param sugtnPathString
	 * @return
	 * @throws JDOMException
	 */
	public XPath createXPathWithNS(String sugtnPathString) throws JDOMException {
		XPath result = null;
		
		String xpStr = this.addNsPfxToXPath(sugtnPathString);
		
		result = XPath.newInstance(xpStr);
		result.addNamespace(this.nsXp);
		return result;
	}
	
	/**
	 * For the pointers mode. Deletes the currently active element, either from the clone or the real dom
	 * 
	 * @param sugtnEltToDelete
	 * @throws DomBuildException
	 */
	protected void deleteActive(SugtnElementNode sugtnEltToDelete)
			throws DomBuildException {
		// FIXME: This is not tested after removing two phase commit
		// TODONE: enforce particles on deletion to prevent messing up the DOM
		
		String domName = null;
		String eltName = null;
		
		Element postDeleteTarget = null;
		
		if (this.getChangesExist()) {
			domName = "Clone";
			eltName = this.domCloneActiveEl.toString();
			
			postDeleteTarget = this.deleteCloneActiveElement();
			
		} else {
			domName = "Real";
			eltName = this.domRealActiveRoot.toString();
			
			postDeleteTarget = this.deleteRealActiveRoot(sugtnEltToDelete);
		}
		
		if (postDeleteTarget != null) {
			
			Element newActive = this.moveToElement(postDeleteTarget);
			
			if (newActive != null) {
				
				if (this.getChangesExist()) {
					
					this.domCloneActiveEl = newActive;
					
				} else {
					
					this.domRealActiveRoot = newActive;
					
				}
				
			} else {
				throw new DomBuildException(
						"Deletion succeeded but Dom " + domName
								+ " is now not correctly positioned!");
			}
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("Element " + eltName + " deleted successfully from Dom " + domName);
			}
			
		} else {
			throw new DomBuildException("Element " + eltName + " couldn't be deleted from Dom "
					+ domName);
		}
	}
	
	/**
	 * Detaches an element from the clone DOM. Used by the public delete.
	 * 
	 * @return The element which is now the clone active
	 * @throws DomBuildException
	 */
	private Element deleteCloneActiveElement() throws DomBuildException {
		this.guardNoChanges();
		
		Element result = null;
		
		Element eltToDelete = this.domCloneActiveEl;
		
		Element deleteParent = eltToDelete.getParentElement();
		
		int ix = this.getActiveIxBetBros();
		
		if (deleteParent.removeContent(eltToDelete)) {
			result = this.getBroBeforeIx(deleteParent, eltToDelete.getName(), eltToDelete
					.getNamespaceURI(), ix);
			if (result == null) {
				result = deleteParent;
			}
		}
		
// TODONOT: we could implement automatic rollback effect if we implmented element deep
// comparison with all of children compared.. too much overhead for something optional
		
		return result;
		
	}
	
	/**
	 * For XPaths mode. Deletes the element which the XPath points to.
	 * 
	 * @param xPathAbs
	 * @param sugtnEltToDelete
	 *            Must be passed, so that the schema is enforced upon deletion
	 * @return The parent element of the deleted element, or null if nothing is deleted.
	 * @throws JDOMException
	 * @throws SDXEException
	 */
	public Element deleteEltAtXPathRelToActive(String xPathAbs, SugtnElementNode sugtnEltToDelete)
			throws JDOMException, SDXEException {
// TODO: Will this allow replacing an element that makes the schema valid with another that still makes it
// valid? A ta7taha B 3awez a7l`eeha A ta7taha C.. wel 2 structures valid.. yenfa3?
		
		Element result = null;
		
		this.guardXpToElement(xPathAbs);
		
		Element xpTarget = (Element) this.singleNodeAtXPathRelative(xPathAbs);
		if (xpTarget != null) {
			result = xpTarget.getParentElement();
			
			if (this.getChangesExist()) {
				
				xpTarget.detach();
				
			} else {
				
				Element origPos = this.domRealActiveRoot;
				try {
					this.moveRealToElement(xpTarget);
					this.deleteRealActiveRoot(sugtnEltToDelete);
				} finally {
					this.moveRealToElement(origPos);
				}
				
			}
		}
		
		return result;
	}
	
	/**
	 * Deletes all elements in DOM which don't have any children. Used for cleaning up the records before release. This
	 * is different from deleting empty occurrences, or actually it completes its work. The delete empty occurrences
	 * removes all instances occurrences, but might leave base shifter elements.
	 * 
	 * @param execlusionList
	 * @throws DomBuildException
	 */
	public void deleteEmptyElements(List<String> execlusionList) throws DomBuildException {
		
		this.guardChanges();
		
		try {
			Set<String> execlusionSet = new HashSet<String>(execlusionList);
			// This cannot be made aggressive, but must be very coarse grained to avoid deletion of
			// data in attributes.. this should be fine because the checkForRequiredVals would have removed
			// any instances with empty text or empty attributes
			XPath totallyEmptyElementsXP = XPath
					.newInstance("//*[not(child::node()) and not(child::text()) and not(attribute::*)]");
			List totallyEmptyElementsList;
			boolean loopAgain;
			do {
				totallyEmptyElementsList = totallyEmptyElementsXP.selectNodes(this.domTotal
						.getRootElement());
				loopAgain = false;
				for (Object obj : totallyEmptyElementsList) {
					if (obj instanceof Element) {
						Element elt = ((Element) obj);
						if (!execlusionSet.contains(elt.toString())) {
							elt.detach();
							loopAgain = true;
						}
					}
				}
			} while (loopAgain); // loop to delete parents that used to have just a totally
			// empty child
		} catch (JDOMException e) {
			throw new DomBuildException(e);
		}
		
	}
	
	/**
	 * Deletes the active element from the real DOM, but if this makes the DOM invalid it attches it again and fail.
	 * 
	 * @param sugtnEltToDelete
	 * @return The previous brother (sibling with same designating attrs), or the parent if not bros are left.
	 * @throws DomBuildException
	 */
	private Element deleteRealActiveRoot(SugtnElementNode sugtnEltToDelete)
			throws DomBuildException {
		this.guardUninit();
		this.guardSugtnTreeSync();
		
		Element result = null;
		
		Element eltToDelete = this.domRealActiveRoot;
		
		if (eltToDelete == this.domTotal.getRootElement()) {
			throw new DomBuildException("Dom Real root deletetion is prohibited!");
		}
		
		Element domParent = eltToDelete.getParentElement();
		
		int ix = this.getActiveIxBetBros(); // domParent.indexOf(eltToDelete);
		
		try {
			eltToDelete.detach();
			
			DomBuildEnforceVisitor localEnforcer = new DomBuildEnforceVisitor(domParent,
					this.configMinOccEnf,
					this.configMaxOccEnf, this.configTwoPhaseCommit);
			
			SugtnTreeNode sugtnParent = (SugtnTreeNode) sugtnEltToDelete.getParent();
			
			while (sugtnParent != null) {
				
				if (sugtnParent instanceof SugtnParticleNode) {
					
					localEnforcer.enforce((SugtnParticleNode) sugtnParent);
					
				} else {
					
					break; // Particles to parent are enough!
					
				}
				
				sugtnParent = (SugtnTreeNode) sugtnParent.getParent();
			}
			
			result = this.getBroBeforeIx(domParent, eltToDelete.getName(), eltToDelete
					.getNamespaceURI(), ix);
			if (result == null) {
				result = domParent;
			}
			
		} catch (DomEnforceException e) {
			// undo delete
			// ix is ixBetBros.. so it is 1 based
			domParent.addContent(ix - 1, eltToDelete);
			
			Element oldReal = (Element) domParent.getContent(ix - 1);
			
			oldReal = this.moveToElement(oldReal);
			
			if (oldReal != null) {
				
				this.domRealActiveRoot = oldReal;
				
			} else {
				
				throw new DomBuildException(
						"Deletion illegal and cannot undo it!");
				
			}
			
			throw e;
		}
		
		return result;
	}
	
	/**
	 * Notifies the DOM listeners that a save action will occur.. passing a clone of the DOM that will be saved. (Used
	 * in WAMCP)
	 * 
	 * @param domTotalClone
	 * @throws DomBuildException
	 */
	protected void doNotifyBeforeSave(Document domTotalClone) throws DomBuildException {
		for (IDomObserver observer : this.domObservers) {
			observer.notifyBeforeSave(domTotalClone);
		}
	}
	
	/**
	 * Notifies the DOM listener that the active moved to a bro. (Never used in WAMCP)
	 * 
	 * @param sugtnElt
	 * @param index
	 * @throws SDXEException
	 */
	protected void doNotifyDomMovedHorizontal(SugtnElementNode sugtnElt, int index)
			throws SDXEException {
		for (IDomObserver observer : this.domObservers) {
			observer.notifyDomMovedHorizontal(sugtnElt, index);
		}
	}
	
	/**
	 * Notifies the DOM Listener that the active moved to a child or parent. (Used to initiate UI generation in WAMCP)
	 * 
	 * @param sugtnElt
	 * @throws SDXEException
	 */
	protected void doNotifyDomMovedVertical(SugtnElementNode sugtnElt) throws SDXEException {
		for (IDomObserver observer : this.domObservers) {
			observer.notifyDomMovedVertical(sugtnElt);
		}
	}
	
	/**
	 * The DOM and Suggestion trees have nodes corresponding to the same elements, and the DOM tree has a pointer to the
	 * active element, while the suggestion tree has the current select. They can be kept in sync by moving the
	 * suggestion tree's selection to the element corresponing to current active whenever it changes.
	 * 
	 * @param currDomEltName
	 * @throws SDXEException
	 */
	protected void doNotifyDomNSugtnOutOfSync(SugtnDeclQName currDomEltName) throws SDXEException {
		for (IDomObserver observer : this.domObservers) {
			observer.notifyDomNSugtnOutOfSync(currDomEltName);
		}
	}
	
	/**
	 * Relatives an absolute XPath, and evaluates it relative to either the real DOM or the Clone, depending on whether
	 * a clone exists or not.
	 * 
	 * @param xPathAbs
	 * @param singleNode
	 *            If true returns the first match
	 * @return A List of objects which are the element or attribute nodes returned.
	 * @throws JDOMException
	 * @throws DomBindingException
	 */
	private List evaluateXPathRelative(String xPathAbs, boolean singleNode)
			throws JDOMException, DomBindingException {
		List result;
		Element xpRoot = null;
		
		if (this.getChangesExist()) {
			// YA 20100627: removing 2 phase addition
			if (this.configTwoPhaseCommit) {
				xpRoot = (Element) this.domCloneActiveRootParent.getChildren().get(0);
			} else {
				xpRoot = this.domCloneActiveRootParent;
				// END // YA 20100627: removing 2 phase addition
			}
			
		} else {
			
			xpRoot = this.domRealActiveRoot;
			
		}
		
		if (!xPathAbs.startsWith("/")) {
			xPathAbs = "/" + xPathAbs;
		}
		
		String xPathBase = this.xpathForElementInDom(this.domRealActiveRoot);
		
		String xPathRel = xPathAbs.replaceFirst(xPathBase, "");
		
		if (xPathRel.equals(xPathAbs)) {
			
			xPathAbs = this.addNsPfxToXPath(xPathAbs);
			xPathRel = xPathAbs.replaceFirst(xPathBase, "");
			
			if (xPathRel.equals(xPathAbs)) {
				throw new DomBindingException("XPath passed is not relative to current active: "
						+ xPathAbs);
			}
		}
		
		if (xPathRel.startsWith("/")) {
			xPathRel = xPathRel.replaceFirst("/", "");
		}
		
		if (xPathRel.isEmpty()) {
			
			result = new ArrayList();
			result.add(xpRoot);
			
		} else {
			if (xPathRel.startsWith("[")) {
				xPathRel = "self::domElt()" + xPathRel;
			}
			XPath xPath = XPath.newInstance(xPathRel);
			
			String nsUri = xpRoot.getNamespaceURI();
			
			Namespace ns = DomTreeController.acquireNamespace(nsUri, xpRoot, true);
			
			xPath.addNamespace(ns);
			xPath.addNamespace(Namespace.XML_NAMESPACE);
			
			if (singleNode) {
				
				result = new ArrayList();
				result.add(xPath.selectSingleNode(xpRoot));
				
			} else {
				
				result = xPath.selectNodes(xpRoot);
				
			}
		}
		
		return result;
	}
	
	/**
	 * Used for printing log messages
	 * 
	 * @return A string representation of the currently active element
	 */
	public String getActiveAsString() {
		if (this.getChangesExist()) {
			return this.getCloneActiveElAsString();
		} else {
			return this.getRealActiveRootAsString();
		}
	}
	
	/**
	 * For pointer mode.
	 * 
	 * @return
	 */
	public boolean getActiveHasNextBro() {
		// TODONE: The result is true when we are in the first and/or last element.. NOT HARMFUL somehow!!!
		
		boolean result = false;
		if (this.domActiveBrosItr != null) {
			result = this.domActiveBrosItr.hasNext()
					&& (this.domActiveBrosItr.nextIndex() != this.domActiveBrosSize);
			
		}
		return result;
	}
	
	/**
	 * For pointer mode.
	 * 
	 * @return
	 */
	public boolean getActiveHasPrevBro() {
		// TODONE: The result is true when we are in the first and/or last element.. NOT HARMFUL somehow!!!
		boolean result = false;
		if (this.domActiveBrosItr != null) {
			result = this.domActiveBrosItr.hasPrevious() && (this.domActiveBrosItr
					.previousIndex() > 0);
		}
		return result;
	}
	
	/**
	 * For pointer mode.
	 * 
	 * @return
	 */
	public int getActiveIxBetBros() {
		int ix = -1;
		if (this.domActiveBrosSize != -1) {
			ix = this.domActiveBrosItr.nextIndex(); // - 1;
			if (ix == -1) {
				ix = this.domActiveBrosItr.previousIndex() + 2;
			}
		}
		return ix;
	}
	
	/**
	 * For pointer mode. Returns the Qualified Name of the active element.
	 * 
	 * @return
	 * @throws DomBuildException
	 */
	public SugtnDeclQName getActiveQName() throws DomBuildException {
		if (this.getChangesExist()) {
			return this.getCloneActiveQName();
		} else {
			return this.getRealActiveRootQName();
		}
	}
	
	/**
	 * For pointer mdoe. Gets an attribute value from the attributes of currently active elements.
	 * 
	 * @param sugtnAttr
	 * @return
	 * @throws DomBuildException
	 */
	public String getAttributeForActive(SugtnAttributeNode sugtnAttr) throws DomBuildException {
		this.guardUninit();
		
		Element active;
		if (this.getChangesExist()) {
			active = this.domCloneActiveEl;
		} else {
			active = this.domRealActiveRoot;
		}
		
		Namespace ns = acquireNamespace(sugtnAttr.getTargetNamespaceURI(), active);
		
		org.jdom.Attribute domAttr = active.getAttribute(sugtnAttr.getLocalName(), ns);
		
		if (domAttr != null) {
			return domAttr.getValue();
		} else {
			return "";
		}
		
	}
	
	/**
	 * Not working, maybe because it doesn't honor designating attributes
	 * 
	 * @deprecated
	 * @param parent
	 * @param broName
	 * @param broNs
	 * @param ix
	 * @return
	 */
	@Deprecated
	private Element getBroBeforeIx(Element parent, String broName, String broNs, int ix) {
		
		// FIXME: Degub getBroBeforeIx.. many bugs can be there...
		
		Element result = null;
		if (ix >= 1) {
			
			List bros = parent.getChildren(broName, acquireNamespace(broNs, parent));
			
			if (bros.size() >= ix) {
				
				result = (Element) bros.get(ix - 1);
				
			}
		}
		return result;
	}
	
	/**
	 * When a clone exists this means that there are uncommitted (not merged into real DOM) and thus unsaved changes
	 * 
	 * @return
	 */
	public boolean getChangesExist() {
		return this.domCloneActiveEl != null;
	}
	
	/**
	 * For toString.
	 * 
	 * @return
	 */
	public String getCloneActiveElAsString() {
		if (!this.getIsInitialized()) {
			return "Uninitialized! Load an XML file first!";
		}
		if (!this.getChangesExist()) {
			return "No clone yet.. this works when there are changes!";
		}
		
		StringBuilder result = new StringBuilder(this.domCloneActiveEl
				.getName());
		
		result.append(this.strActiveIxOverBrosNum());
		
		return result.toString();
	}
	
	public SugtnDeclQName getCloneActiveQName() throws DomBuildException {
		
		this.guardNoChanges();
		
		return new SugtnDeclQName(this.domCloneActiveEl.getNamespaceURI(), this.domCloneActiveEl
				.getName());
		
	}
	
	/**
	 * Returns the DOM tree that is being controlled, thus making it possible that the programmer messes everything up.
	 * However, this was needed in many cases.
	 * 
	 * @return the domTotal
	 * @throws DomBuildChangesException
	 * @deprecated It is not deprecated, but use with caution, only when you know what you are doing
	 *             specially when changes exist; the domRealParent element shouldn't be touched then!
	 */
	@Deprecated
	public Document getDomTotal() throws DomBuildChangesException {
		this.guardChanges();
		return this.domTotal;
	}
	
	/**
	 * Evaluates an absolute XPath and returns a list of clones of its results. Thus, the caller cannot mess things up
	 * when given control on the actual elements.
	 * 
	 * @param xPathAbs
	 * @return
	 * @throws JDOMException
	 * @throws SDXEException
	 */
	public LinkedList<Element> getElementCloneListAtXPathRelToActive(String xPathAbs)
			throws JDOMException, SDXEException {
		LinkedList<Element> result = new LinkedList<Element>();
		
		List xpTargetList = this.evaluateXPathRelative(xPathAbs, false);
		
		for (Object xpTarget : xpTargetList) {
			if ((xpTarget != null) && (xpTarget instanceof Element)) {
				result.add((Element) ((Element) xpTarget).clone());
			}
		}
		
		return result;
	}
	
	/**
	 * Evaluates a count XPath on an absolute XPath after relativizing it. If the XPath contains an index predicate at
	 * the last step, it is removed. Otherwise the result would be 1.
	 * 
	 * @param xPathAbs
	 * @return
	 * @throws JDOMException
	 * @throws SDXEException
	 */
	public int getElementCountAtXPathRelToActive(String xPathAbs) throws JDOMException,
			SDXEException {
		int result;
		
		int lastIx = XPathStrUtils.getLastIx(xPathAbs);
		if (lastIx == -2) {
			// we shouldn't remove the last PREDICATE
		} else {
			xPathAbs = XPathStrUtils.removeLastIx(xPathAbs);
		}
		
		Element xpRoot = null;
		
		if (this.getChangesExist()) {
			// YA 20100627: removing 2 phase addition
			if (this.configTwoPhaseCommit) {
				xpRoot = (Element) this.domCloneActiveRootParent.getChildren().get(0);
			} else {
				xpRoot = this.domCloneActiveRootParent;
				// END // YA 20100627: removing 2 phase addition
			}
			
		} else {
			
			xpRoot = this.domRealActiveRoot;
			
		}
		
		if (!xPathAbs.startsWith("/")) {
			xPathAbs = "/" + xPathAbs;
		}
		
		String xPathBase = this.xpathForElementInDom(this.domRealActiveRoot);
		
		String xPathRel = xPathAbs.replaceFirst(xPathBase, "");
		
		if (xPathRel.equals(xPathAbs)) {
			
			xPathAbs = this.addNsPfxToXPath(xPathAbs);
			xPathRel = xPathAbs.replaceFirst(xPathBase, "");
			
			if (xPathRel.equals(xPathAbs)) {
				throw new SDXEException("XPath passed is not relative to current active: "
						+ xPathAbs);
			}
		}
		
		if (xPathRel.startsWith("/")) {
			xPathRel = xPathRel.replaceFirst("/", "");
		}
		
		if (xPathRel.isEmpty()) {
			result = 1; // The active element is the one wanted
		} else {
			if (xPathRel.startsWith("[")) {
				xPathRel = "self::domElt()" + xPathRel;
			}
			xPathRel = "count(" + xPathRel + ")";
			
			XPath xPath = XPath.newInstance(xPathRel);
			
			String nsUri = xpRoot.getNamespaceURI();
			
			Namespace ns = DomTreeController.acquireNamespace(nsUri, xpRoot, true);
			
			xPath.addNamespace(ns);
			
			result = xPath.numberValueOf(xpRoot).intValue();
		}
		
		return result;
		
	}
	
	/**
	 * Returns the contents of elements that match an XPath, each content attached to a new object of its parent
	 * element's tag.
	 * 
	 * @param xPathAbs
	 * @return
	 * @throws JDOMException
	 * @throws SDXEException
	 * @throws IOException
	 */
	public LinkedList<Element> getElementTextListAtXPathRelToActive(String xPathAbs)
			throws JDOMException, SDXEException, IOException {
		LinkedList<Element> result = new LinkedList<Element>();
		
		List xpTargetList = this.evaluateXPathRelative(xPathAbs, false);
		
		for (Object xpTarget : xpTargetList) {
			if ((xpTarget != null) && (xpTarget instanceof Element)) {
				Element resElt = new Element(((Element) xpTarget).getName(), ((Element) xpTarget)
						.getNamespace());
				resElt.addContent(JDOMUtils.getElementContentAsString((Element) xpTarget, Format
						.getPrettyFormat()));
				result.add(resElt);
			}
		}
		
		return result;
	}
	
	/**
	 * 
	 * @return true of a DOM tree is loaded
	 */
	public boolean getIsInitialized() {
		return this.domTotal != null;
	}
	
	/**
	 * 
	 * @return The namespace that should be used for XPathes
	 */
	public Namespace getNamespaceForXPath() {
		return this.nsXp;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getRealActiveRootAsString() {
		if (!this.getIsInitialized()) {
			return "Uninitialized! Load an XML file first!";
		}
		
		StringBuilder result = new StringBuilder(this.domRealActiveRoot
				.getName());
		
		result.append(this.strActiveIxOverBrosNum());
		
		return result.toString();
	}
	
	/**
	 * 
	 * @return
	 * @throws DomBuildException
	 */
	public SugtnDeclQName getRealActiveRootQName()
			throws DomBuildException {
		this.guardUninit();
		return new SugtnDeclQName(this.domRealActiveRoot.getNamespaceURI(), this.domRealActiveRoot
				.getName());
	}
	
	/**
	 * For pointers mode. Gets the content of active.
	 * 
	 * @return
	 */
	public String getTextOfActive() {
		Element active;
		if (this.getChangesExist()) {
			active = this.domCloneActiveEl;
		} else {
			active = this.domRealActiveRoot;
		}
		
		try {
			return JDOMUtils.getElementContentAsString(active, Format.getCompactFormat());
		} catch (IOException e) {
			return "EXCEPTION: " + e.getMessage();
		}
		
	}
	
	/**
	 * Returns the content of the node that corresponds to the Dom Addable Node passed, at the absolute XPath passed.
	 * That is if the attribute value of an element is needed, you can either pass the attribute XPath or the element
	 * XPath, and the Attrbiute SugtnNode.
	 * 
	 * @param xPathAbs
	 * @param sugtnNodeForTarget
	 * @return The value, including textual representation of child elements and their values.
	 * @throws JDOMException
	 * @throws SDXEException
	 */
	public String getValueAtXPathRelToActive(String xPathAbs, SugtnDomAddableNode sugtnNodeForTarget)
			throws JDOMException,
			SDXEException {
		String result = null;
		
		// TODO check if the element domElt carries PCData
		if (sugtnNodeForTarget instanceof SugtnAttributeNode) {
			
			if (XPathStrUtils.getLastStep(xPathAbs).startsWith("@")) {
				xPathAbs = XPathStrUtils.removeLastStep(xPathAbs);
			}
			
			SugtnAttributeNode sugtnAttr = (SugtnAttributeNode) sugtnNodeForTarget;
			
			String attrQName = "";
			Namespace attrNs = acquireNamespaceNoParent(sugtnAttr);
			
			attrQName = attrNs.getPrefix();
			
			if (!attrQName.isEmpty()) {
				attrQName += ":";
			}
			
			attrQName += sugtnAttr.getLocalName();
			xPathAbs += "/@" + attrQName;
		}
		
		Object xpTarget = this.singleNodeAtXPathRelative(xPathAbs);
		
		if (xpTarget != null) {
			if (xpTarget instanceof Element) {
				
				try {
					result = JDOMUtils.getElementContentAsString(((Element) xpTarget), Format
							.getCompactFormat());
				} catch (IOException e) {
					throw new DomBindingException(e);
				}
				
			} else if (xpTarget instanceof org.jdom.Attribute) {
				
				result = ((org.jdom.Attribute) xpTarget).getValue();
				
			} else {
				throw new DomBuildException("XPath \"" + xPathAbs
						+ "\" returned an unkonwn domElt type!");
			}
		} else {
			
			result = ""; // Possibly no such domElt yet
		}
		
		return result;
		
	}
	
	/**
	 * Call to prevent loosing changes
	 * 
	 * @throws DomBuildChangesException
	 */
	private void guardChanges() throws DomBuildChangesException {
		if (this.getChangesExist()) {
			throw new DomBuildChangesException(
					"Uncommitted changes will be lost. Commit or rollback first");
		}
		
	}
	
	/**
	 * Call before actions that requires that changes exist (manipulates clone)
	 * 
	 * @throws DomBuildException
	 */
	private void guardNoChanges() throws DomBuildException {
		if (!this.getChangesExist()) {
			throw new DomBuildException(
					"No changes exist, use of DOM Clone will result in Null Pointer Exceptions");
		}
		
	}
	
	/**
	 * 
	 * @throws DomBuildSugtnTreeSyncException
	 */
	private void guardSugtnTreeSync() throws DomBuildSugtnTreeSyncException {
		if (this.getChangesExist()) {
			throw new DomBuildSugtnTreeSyncException(
					"Cannot move while non committed changed exist!");
		}
		
	}
	
	/**
	 * 
	 * @throws DomBuildException
	 */
	private void guardUninit() throws DomBuildException {
		if (!this.getIsInitialized()) {
			throw new DomBuildException(
					"Uninitialized! Load an XML file first!");
		}
	}
	
	/**
	 * Call to assuret that this XPath is that of an element
	 * 
	 * @param xPathAbs
	 * @throws DomBuildException
	 */
	private void guardXpToElement(String xPathAbs) throws DomBuildException {
		if (XPathStrUtils.getLastStep(xPathAbs).startsWith("@")) {
			throw new DomBuildException(
					"Xpath passed must always be that of an element");
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
	 * Opens an XML file and loads its DOM tree to control it.
	 */
	protected void load(File xmlFile) throws JDOMException, IOException,
			DomBuildException {
		this.guardChanges();
		
		SAXBuilder saxBuilder = new SAXBuilder();
		
		saxBuilder.setFeature("http://xml.org/sax/features/validation", false);
		saxBuilder.setFeature("http://xml.org/sax/features/namespaces", true);
		saxBuilder.setFeature("http://xml.org/sax/features/namespace-prefixes",
				true);
// Unsupported: saxBuilder.setFeature("http://xml.org/sax/features/xmlns-uris", false);
		
		this.domTotal = saxBuilder.build(xmlFile);
		
		this.domRealActiveRoot = this.domTotal.getRootElement();
		
		this.nsXp = acquireNamespace(this.domRealActiveRoot.getNamespaceURI(),
				this.domRealActiveRoot,
				true);
		this.nsXpPfx = this.nsXp.getPrefix() + ":";
		
		this.domBuildVisitor = new DomBuildVisitor(this.configMinOccEnf,
				this.configMaxOccEnf, this.configTwoPhaseCommit);
		this.domActiveBrosItr = null;
		this.domActiveBrosSize = -1;
		this.domCloneActiveRootIx = -1;
		this.domCloneActiveEl = null;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("XML file " + xmlFile.getAbsolutePath() + " loaded");
		}
		
	}
	
	/**
	 * Pointers mode.
	 * 
	 * @param name
	 * @param nsURI
	 * @throws DomBuildException
	 */
	protected void moveActiveIntoChild(String name, String nsURI) throws DomBuildException {
		String domName = null;
		String eltName = null;
		
		if (this.getChangesExist()) {
			this.moveCloneIntoChild(name, nsURI);
			domName = "Clone";
			eltName = this.domCloneActiveEl.toString();
		} else {
			this.moveRealIntoChild(name, nsURI);
			domName = "Real";
			eltName = this.domRealActiveRoot.toString();
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("DOM " + domName + " moved to its child < " + eltName + "> ("
					+ this.getActiveIxBetBros() + "/" + this.domActiveBrosSize + ")");
		}
		
	}
	
	/**
	 * Pointers mode.
	 * 
	 * @param sugtnChild
	 * @throws DomBuildException
	 */
	protected void moveActiveIntoChild(SugtnElementNode sugtnChild) throws DomBuildException {
		this.moveActiveIntoChild(sugtnChild.getLocalName(), sugtnChild.getTargetNamespaceURI());
	}
	
	/**
	 * Pointers mode.
	 * 
	 * @throws DomBuildException
	 */
	protected void moveActiveToNextBro() throws DomBuildException {
		String domName = null;
		String eltName = null;
		if (this.getChangesExist()) {
			this.moveCloneToNextBro();
			domName = "Clone";
			eltName = this.domCloneActiveEl.toString();
			
		} else {
			this.moveRealToNextBro();
			domName = "Real";
			eltName = this.domRealActiveRoot.toString();
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("DOM " + domName + " moved forward to < " + eltName + "> ("
					+ this.getActiveIxBetBros() + "/" + this.domActiveBrosSize + ")");
		}
		
	}
	
	/**
	 * Pointers mode.
	 * 
	 * @throws DomBuildException
	 */
	protected void moveActiveToPrevBro() throws DomBuildException {
		String domName = null;
		String eltName = null;
		if (this.getChangesExist()) {
			this.moveCloneToPrevBro();
			domName = "Clone";
			eltName = this.domCloneActiveEl.toString();
			
		} else {
			this.moveRealToPrevBro();
			domName = "Real";
			eltName = this.domRealActiveRoot.toString();
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("DOM " + domName + " moved back to < " + eltName + "> ("
					+ this.getActiveIxBetBros() + "/" + this.domActiveBrosSize + ")");
		}
		
	}
	
	/**
	 * Pointers mode.
	 * 
	 * @throws DomBuildException
	 */
	protected void moveActiveUpToParent() throws DomBuildException {
		String domName = null;
		String eltName = null;
		if (this.getChangesExist()) {
			this.moveCloneUpToParent();
			domName = "Clone";
			eltName = this.domCloneActiveEl.toString();
			
		} else {
			this.moveRealUptoParent();
			domName = "Real";
			eltName = this.domRealActiveRoot.toString();
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("DOM " + domName + " moved to its parent < " + eltName + "> ("
					+ this.getActiveIxBetBros() + "/" + this.domActiveBrosSize + ")");
		}
		
	}
	
	/**
	 * Pointers mode.
	 * 
	 * @param name
	 * @param nsURI
	 * @throws DomBuildException
	 */
	private void moveCloneIntoChild(String name, String nsURI)
			throws DomBuildException {
		this.guardNoChanges();
		
		Element newClone = this.moveIntoChild(this.domCloneActiveEl, name, nsURI);
		
		if (newClone != null) {
			this.domCloneActiveEl = newClone;
		}
		
	}
	
	/**
	 * Pointers mode
	 * 
	 * @param target
	 * @throws DomBuildException
	 */
	private void moveCloneToElement(Element target) throws DomBuildException {
		this.guardNoChanges();
		
		Element newClone = this.moveToElement(target);
		
		if (newClone != null) {
			this.domCloneActiveEl = newClone;
		}
		
	}
	
	/**
	 * Pointers mode.
	 * 
	 * @throws DomBuildException
	 */
	private void moveCloneToNextBro() throws DomBuildException {
		this.guardNoChanges();
		
		if (this.getActiveHasNextBro()) {
			
			this.domCloneActiveEl = (Element) this.domActiveBrosItr.next();
			
		}
		
	}
	
	/**
	 * Pointers mode.
	 * 
	 * @throws DomBuildException
	 */
	private void moveCloneToPrevBro() throws DomBuildException {
		this.guardNoChanges();
		
		if (this.getActiveHasPrevBro()) {
			
			this.domCloneActiveEl = (Element) this.domActiveBrosItr.previous();
			
		}
		
	}
	
	/**
	 * Pointers mode.
	 * 
	 * @throws DomBuildException
	 */
	private void moveCloneUpToParent() throws DomBuildException {
		this.guardNoChanges();
		
		Element parent = this.domCloneActiveEl.getParentElement();
		if (parent != this.domCloneActiveRootParent) {// .getName() != DUMMY_PARENT_ELT_NAME) {
			// this.domCloneActiveEl = parent;
			this.moveCloneToElement(parent);
		} else {
			throw new DomBuildSugtnTreeSyncException(
					"Cannot move beyond the root of the current changes");
		}
		
	}
	
	/**
	 * Pointers mode.
	 * 
	 * @param parentElt
	 * @param childName
	 * @param childNsURI
	 * @return
	 * @throws DomBuildException
	 */
	private Element moveIntoChild(Element parentElt, String childName, String childNsURI)
			throws DomBuildException {
		
		Element result = null;
		
		Namespace ns = DomTreeController.acquireNamespace(childNsURI, parentElt);
		List tempList = parentElt.getChildren(childName, ns);
		
		ListIterator tempIter = tempList.listIterator();
		
		if (!tempIter.hasNext()) {
			result = null;
		} else {
			this.domActiveBrosItr = tempIter;
			this.domActiveBrosSize = tempList.size();
			result = (Element) this.domActiveBrosItr
					.next();
		}
		
		return result;
	}
	
	/**
	 * XPaths mode. Used to move the last (because it is newly added) Brother (element with some designating attributes)
	 * to a different location in between its brothers.
	 * 
	 * @param xPathAbs
	 * @param newIxBetBros
	 * @throws DomBindingException
	 * @throws JDOMException
	 * @throws DomBuildException
	 */
	public void moveLastEltToIxBetBros(String xPathAbs, int newIxBetBros)
			throws DomBindingException,
			JDOMException, DomBuildException {
		this.guardUninit();
		this.guardXpToElement(xPathAbs);
		
		if (newIxBetBros < 2) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Neglected request to move last element to an 'index between brothers' less than 2: "
						+ xPathAbs);
			}
			return;
		}
		
		if (XPathStrUtils.getLastIx(xPathAbs) != -2) { // not a predicate
			xPathAbs = XPathStrUtils.removeLastIx(xPathAbs);
		}
		
		List allBros = this.evaluateXPathRelative(xPathAbs, false);
		
// always move so that "brothers" are grouped together
// if (allBros.size() <= newIxBetBros) {
// if (LOG.isDebugEnabled()) {
// LOG.debug("Neglected request to move last element to an 'index between brothers' greater than or equal the number of 'brothers': "
// + xPathAbs);
// }
// return;
// }
		
		if (newIxBetBros == allBros.size()) {
			return; // nothing to do!
		}
		
		Element lastElt = (Element) allBros.get(allBros.size() - 1);
		
		Element prevBro = (Element) allBros.get(newIxBetBros - 2); // 2 because xpath starts at 1, and we want the one
// before
		
		Element eltsParent = lastElt.getParentElement();
		
		int targetContentIx = eltsParent.getContent().indexOf(prevBro) + 1;
		
		lastElt.detach();
		
		if (LOG.isDebugEnabled()) {
			lastElt.setAttribute("DEBUG", "Moved to Content Ix: " + targetContentIx
					+ " - while processing XPath: " + xPathAbs + " - to move it to index: "
					+ newIxBetBros);
		} else {
			lastElt.removeAttribute("DEBUG");
		}
		
		eltsParent.addContent(targetContentIx, lastElt);
	}
	
	/**
	 * Pointers mode.
	 * 
	 * @param name
	 * @param nsURI
	 * @throws DomBuildException
	 */
	private void moveRealIntoChild(String name, String nsURI)
			throws DomBuildException {
		this.guardUninit();
		this.guardSugtnTreeSync();
		
		Element newReal = this.moveIntoChild(this.domRealActiveRoot, name, nsURI);
		
		if (newReal != null) {
			this.domRealActiveRoot = newReal;
		}
	}
	
	/**
	 * This is used to find the deepest existing element of the path of elements passed.
	 * 
	 * @param sugtnPathStr
	 * @throws DomBuildException
	 * @throws JDOMException
	 */
	protected void moveRealToDeepestExiting(String sugtnPathStr) throws DomBuildException,
			JDOMException {
		
		this.guardUninit();
		this.guardSugtnTreeSync();
		this.guardXpToElement(sugtnPathStr);
		
		Element xpRes = null;
		String xpStr = this.addNsPfxToXPath(sugtnPathStr);
		
		do {
			XPath xp = XPath.newInstance(xpStr);
			xp.addNamespace(this.nsXp);
			
			xpRes = (Element) xp.selectSingleNode(this.domTotal);
			
			if (xpRes == null) {
				
				String xpStrNew = XPathStrUtils.removeLastStep(xpStr);
				if (xpStr.equals(xpStrNew)) {
					break;
				} else {
					xpStr = xpStrNew;
				}
			}
			
		} while (xpRes == null);
		
		if (xpRes != null) {
			this.moveRealToElement(xpRes);
		}
	}
	
	/**
	 * Relocates the Active pointer of the Real DOM to a certain element.
	 * 
	 * @param target
	 * @throws DomBuildException
	 */
	private void moveRealToElement(Element target) throws DomBuildException {
		
		this.guardUninit();
		this.guardSugtnTreeSync();
		
		Element targetParent = target.getParentElement();
		
		if (targetParent == null) { // The domTarget element is doc root
			// i.e. it can't have any Bros
			this.domRealActiveRoot = this.domTotal.getRootElement();
			this.domActiveBrosItr = null;
			this.domActiveBrosSize = -1;
		} else {
			this.domRealActiveRoot = targetParent;
			this.moveRealIntoChild(target.getName(), target.getNamespaceURI());
			// We need to move to a specific child
			while ((this.domRealActiveRoot != target) && this.getActiveHasNextBro()) {
				this.moveActiveToNextBro();
			}
		}
		
	}
	
	/**
	 * Pointers mode.
	 * 
	 * @throws DomBuildException
	 */
	private void moveRealToNextBro() throws DomBuildException {
		
		this.guardUninit();
		this.guardSugtnTreeSync();
		if (this.getActiveHasNextBro()) {
			this.domRealActiveRoot = (Element) this.domActiveBrosItr
					.next();
			
		}
	}
	
	/**
	 * Pointers mode.
	 * 
	 * @throws DomBuildException
	 */
	private void moveRealToPrevBro() throws DomBuildException {
		
		this.guardUninit();
		this.guardSugtnTreeSync();
		if (this.getActiveHasPrevBro()) {
			this.domRealActiveRoot = (Element) this.domActiveBrosItr
					.previous();
			
		}
	}
	
	/**
	 * 
	 * @param xPathStr
	 * @throws JDOMException
	 * @throws DomBuildException
	 */
	protected void moveRealToXPath(String xPathStr) throws JDOMException,
			DomBuildException {
		
		Element domRoot = this.domTotal.getRootElement();
		
		XPath xPath = XPath.newInstance(xPathStr);
		String nsUri = domRoot.getNamespaceURI();
		
		Namespace ns = DomTreeController.acquireNamespace(nsUri, domRoot, true);
		xPath.addNamespace(ns);
		
		this.moveRealToXPath(xPath);
	}
	
	/**
	 * 
	 * @param xPath
	 * @throws JDOMException
	 * @throws DomBuildException
	 */
	protected void moveRealToXPath(XPath xPath) throws JDOMException, DomBuildException {
		
		Object xpTarget = xPath.selectSingleNode(this.domTotal);
		
		if ((xpTarget != null) && (xpTarget instanceof Element)) {
			this.moveRealToElement((Element) xpTarget);
		} else {
			throw new DomBuildException("XPath \"" + xPath.getXPath()
					+ "\" didnot return an element domElt");
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("Dom Real moved by XPath String '" + xPath.getXPath() + "' to <"
					+ this.domRealActiveRoot.toString() + ">");
		}
		
	}
	
	/**
	 * Pointers mode.
	 * 
	 * @throws DomBuildException
	 */
	private void moveRealUptoParent() throws DomBuildException {
		
		this.guardUninit();
		this.guardSugtnTreeSync();
		Element tempParent = this.domRealActiveRoot.getParentElement();
		if (tempParent == null) { // ActiveRoot was and still is Document Root element
		
		} else {
			
			this.moveRealToElement(tempParent);
		}
		
	}
	
	/**
	 * Pointers mode.
	 * 
	 * @param target
	 * @return
	 * @throws DomBuildException
	 */
	private Element moveToElement(Element target) throws DomBuildException {
		Element targetParent = target.getParentElement();
		if (targetParent == null) {
			// the target is the root
			this.domActiveBrosItr = null;
			this.domActiveBrosSize = 1;
			return (Element) target;
		}
		
		Element result = this.moveIntoChild(targetParent, target.getName(), target
				.getNamespaceURI());
		
		// We need to move to a specific child
		while ((result != target) && this.getActiveHasNextBro()) {
			result = (Element) this.domActiveBrosItr.next();
		}
		
		return result;
		
	}
	
	/**
	 * 
	 * @param observer
	 * @return
	 */
	public boolean registerObserver(IDomObserver observer) {
		return this.domObservers.add(observer);
	}
	
	/**
	 * Two phase commit; rollback
	 * 
	 * @throws DomBuildException
	 */
	protected void rollbackToRealDom() throws DomBuildException {
		// YA 20100627: removing 2 phase addition
		if (!this.configTwoPhaseCommit) {
			return;
		}
		// If the XSOM minOccurs problem is fixed, use the following code
		if (!this.getChangesExist()) {
			return;
		}
		
		// The pending particles are now irrelevant
		this.domBuildVisitor.purgeParticles();
		
		// The clone will not be the correct context
		this.domCloneActiveEl = null;
		
		// And the new elements must be GCed
		this.domCloneActiveRootParent.removeContent();
		
		// And the Active element must be the Real Dom root now
		this.moveRealToElement(this.domRealActiveRoot);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("DOM changes rolled back");
		}
		// END YA 20100627: removing 2 phase addition
	}
	
	/**
	 * Save the DOM to file
	 * 
	 * @param fileWriter
	 * @throws IOException
	 * @throws DomBuildException
	 */
	protected void save(FileWriter fileWriter) throws IOException,
			DomBuildException {
		this.guardUninit();
		
		// TODONOT: will this be better? Document domTotalClone = (Document) domTotal.clone();
		
		// TODO move this to the Mediator
		this.doNotifyBeforeSave(this.domTotal);
		
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat()
				.setEncoding("UTF-8"));
		outputter.output(this.domTotal, fileWriter);
		
		// TODO notify after save
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("DOM written to fileWriter: " + fileWriter.toString());
		}
	}
	
	/**
	 * Pointers mode.
	 * 
	 * @param sugtnAttr
	 * @param value
	 * @throws DomBuildException
	 */
	public void setAttributeForActive(SugtnAttributeNode sugtnAttr, String value)
			throws DomBuildException {
		this.guardUninit();
		
		Element active;
		if (this.getChangesExist()) {
			active = this.domCloneActiveEl;
		} else {
			active = this.domRealActiveRoot;
			
			// Prevent deletion of required attributes
			if (sugtnAttr.isRequired() && ((value == null) || value.equals(""))) {
				throw new DomRequiredAttributeException(active.getName(), sugtnAttr.getLocalName(),
						active.getName());
			}
		}
		
		// Prevent duplicate attributes and allow deletion
		// by deleting existing first
		Namespace ns = acquireNamespace(sugtnAttr.getTargetNamespaceURI(), active);
		
		org.jdom.Attribute domAttr = active.getAttribute(sugtnAttr.getLocalName(), ns);
		
		if (domAttr != null) {
			String currVal = domAttr.getValue();
			
			if ((currVal != null) && currVal.equals(value)) {
				return; // nothing to change
			}
			
			active.removeAttribute(domAttr);
			
		}
		
// Must call addsuggested, even if what was meant was deletion.. to enforce!
		
		this.addSuggested(sugtnAttr, value);
		
	}
	
	/**
	 * 
	 * @param configMaxOccEnf
	 * @return
	 */
	public DomTreeController setConfigMaxOccEnf(boolean configMaxOccEnf) {
		this.configMaxOccEnf = configMaxOccEnf;
		return this;
	}
	
	/**
	 * 
	 * @param configMinOccEnf
	 * @return
	 */
	public DomTreeController setConfigMinOccEnf(boolean configMinOccEnf) {
		this.configMinOccEnf = configMinOccEnf;
		return this;
	}
	
	/**
	 * 
	 * @param configTwoPhaseCommit
	 * @return
	 */
	public DomTreeController setConfigTwoPhaseCommit(boolean configTwoPhaseCommit) {
		this.configTwoPhaseCommit = configTwoPhaseCommit;
		return this;
	}
	
	/**
	 * Pointers mode
	 * 
	 * @param sugtnPCData
	 * @param text
	 * @throws DomBuildException
	 */
	public void setTextOfActive(SugtnPCDataNode sugtnPCData, String text)
			throws DomBuildException {
		this.addSuggested(sugtnPCData, text); // null, text);
		
	}
	
	/**
	 * Sets the content of the node that corresponds to the Dom Addable Node passed, at the absolute XPath passed.
	 * That is if the attribute value of an element is needed, you MUST pass the element XPath, and the Attrbiute
	 * SugtnNode. The element must already exist before setting the value.
	 * The addition enforces the schema's restrictions on simple data type and mixed elements allowed
	 * 
	 * @param xPathAbs
	 * @param value
	 * @param sugtnNodeForTarget
	 * @return true -> value set successfully. false -> no element in this XPath.. add element first
	 * @throws JDOMException
	 * @throws DomBindingException
	 * @throws SDXEException
	 */
	public Element setValueAtXPathRelToActive(String xPathAbs, String value,
			SugtnDomAddableNode sugtnNodeForTarget) throws JDOMException, DomBuildException,
			DomBindingException {
		this.guardXpToElement(xPathAbs);
		
		boolean result = false;
		Element cloneOrig = this.domCloneActiveEl;
		
		this.activateClone();
		// YA 20100627: removing 2 phase addition
		try {
			// END // YA 20100627: removing 2 phase addition
			
			// TODONE: remove this redundant check because the check in guardXpToElement prevents it
// if (sugtnNodeForTarget instanceof SugtnAttributeNode) {
// if (XPathStrUtils.getLastStep(xPathAbs).startsWith("@")) {
// xPathAbs = XPathStrUtils.removeLastStep(xPathAbs);
// }
// }
			
			Element xpTarget = (Element) this.singleNodeAtXPathRelative(xPathAbs);
			
			Attribute oldAttr = null;
			Element oldElt = null;
			
			if (xpTarget != null) {
				// Delete old (to prevent duplication and allow deletion)
				String oldVal = null;
				
				if (sugtnNodeForTarget instanceof SugtnElementNode) {
					sugtnNodeForTarget = ((SugtnElementNode) sugtnNodeForTarget).getPcDataNode();
				}
				
				// use the PCDataNode of the element not the Element
				// itself to be able to check for Mixed and to addSuggested
				if (sugtnNodeForTarget instanceof SugtnPCDataNode) {
					oldElt = (Element) xpTarget;
					
					try {
						oldVal = JDOMUtils.getElementContentAsString(oldElt, Format
								.getCompactFormat());
						// oldElt.getTextTrim();
					} catch (IOException e) {
						throw new DomBindingException(e);
					}
					
					if ((oldVal != null) && oldVal.equals(value)) {
						
						result = true; // no changes, we are done
						
					} else {
						
						// TODONE? test mixed nodes
						if (!(sugtnNodeForTarget instanceof SugtnMixedNode)
								&& (oldElt.getChildren().size() > 0)) {
							
							throw new DomBuildException(
									"The element returned by XPath is not a simple value holder, cannot use setValue with it or else children will be lost!");
							
						}
						
						// Don't detach element.. we'll just change pcdata!
						// oldElt.detach();
						
					}
					
				} else if (sugtnNodeForTarget instanceof SugtnAttributeNode) {
					SugtnAttributeNode sugtnAttr = (SugtnAttributeNode) sugtnNodeForTarget;
					
					oldAttr = xpTarget.getAttribute(sugtnAttr.getLocalName(),
							acquireNamespaceNoParent(sugtnAttr));
					
					if (oldAttr != null) {
						oldVal = oldAttr.getValue();
						
						if ((oldVal != null) && oldVal.equals(value)) {
							result = true; // no changes, we are done
						} else {
							oldAttr.detach();
						}
					}
					
				} else {
					
					throw new DomBuildException(
							"Only attributes and PCData (mixed or not) can be set using this method");
				}
				
				if (!result) {
					// Add new value
					Element cloneTemp = this.domCloneActiveEl;
					try {
						this.domCloneActiveEl = xpTarget;
						this.addSuggested(sugtnNodeForTarget, value, false); // null, value);
						result = true;
					} finally {
						this.domCloneActiveEl = cloneTemp;
					}
				} else {
					if (cloneOrig == null) {
						this.rollbackToRealDom();
					}
				}
			} // else failure to set the value of a nonexisting element..
			
			return xpTarget;
			
			// YA 20100627: removing 2 phase addition
		} finally {
			if (!this.configTwoPhaseCommit) {
				this.commitToRealDom();
			}
		}
		// END // YA 20100627: removing 2 phase addition
		
	}
	
	/**
	 * 
	 * @param xPathAbs
	 * @return
	 * @throws JDOMException
	 * @throws DomBindingException
	 */
	private Object singleNodeAtXPathRelative(String xPathAbs)
			throws JDOMException, DomBindingException {
		List allNodes = this.evaluateXPathRelative(xPathAbs, true);
		return allNodes.get(0);
	}
	
	/**
	 * 
	 * @return
	 */
	private String strActiveIxOverBrosNum() {
		StringBuilder result = new StringBuilder();
		int ix = this.getActiveIxBetBros();
		if (ix != -1) {
			result.append(" (").append(ix).append("/").append(
					this.domActiveBrosSize).append(")");
		} else {
			result.append(" (1/1)");
		}
		return result.toString();
	}
	
	/**
	 * 
	 * @param observer
	 * @return
	 */
	public boolean unregisterObserver(IDomObserver observer) {
		return this.domObservers.remove(observer);
	}
	
	/**
	 * @see private String xpathForElementInDom(Element target)
	 * @return
	 */
	protected String xpathForActiveInDom() {
		
		if (this.getChangesExist()) {
			return this.xpathForElementInDom(this.domCloneActiveEl);
		} else {
			return this.xpathForElementInDom(this.domRealActiveRoot);
		}
	}
	
	/**
	 * This doesn't generate predicates for designating attributes
	 * 
	 * @param target
	 * @return The XPath to the element, with index predicated.
	 */
	private String xpathForElementInDom(Element target) {
		String result = "";
		
		Element parent = target.getParentElement();
		Element child = target;
		
		while (child != null) {
			
			String childXPathName = null;
			if (child.getNamespace().equals(Namespace.NO_NAMESPACE)) {
				childXPathName = child.getName();
			} else {
				String nsPrefix = child.getNamespacePrefix();
				if (nsPrefix.equals("")) {
					nsPrefix = XPathBuilder.DEFAULT_NS_XPATH_PREFIX;
				}
				
				if ((nsPrefix != null) && !nsPrefix.isEmpty()) {
					nsPrefix = nsPrefix + ":";
				}
				
				childXPathName = nsPrefix + child.getName();
			}
			
			int ix = parent != null ? parent.getChildren(child.getName(), child.getNamespace())
					.indexOf(child) + 1 : 1;
			
			String temp = "/" + childXPathName;
			
			if (ix > 1) {
				temp += "[" + ix + "]";
			}
			
			result = temp + result;
			
			child = parent;
			if (parent != null) {
				parent = parent.getParentElement();
			}
			
		}
		
		return result;
	}
}
