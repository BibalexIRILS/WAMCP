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

import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.bibalex.sdxe.exception.SugtnException;
import org.bibalex.sdxe.suggest.SugtnTreeBuilder;
import org.bibalex.sdxe.suggest.SugtnTreeBuilderCache;
import org.bibalex.sdxe.suggest.model.SugtnTreeModel;
import org.bibalex.sdxe.suggest.model.SugtnTreeNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnDeclNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnElementNode;
import org.bibalex.util.XPathStrUtils;

/**
 * Controls the Tree Model of the XML schema. It handles two different things; how the view should display the model
 * (only in visible mode), and manipulating the model (incrementally building it for example).
 * 
 * A sugtn path is not a tree path.. it doesn't contain particles.
 * 
 * @author Younos.Naga
 * 
 */
public class SugtnTreeController {
	private static final Logger LOG = Logger.getLogger("org.bibalex.sdxe");
	/**
	 * The tree model of the whole schema
	 */
	private SugtnTreeModel sugtnTreeModelTotal = null;
	
	/**
	 * The tree model of the currently active element
	 */
	private SugtnTreeModel sugtnTreeModelActive = null;
	
	/**
	 * The node selected
	 */
	private SugtnTreeNode sugtnNodeActive = null;
	
	/**
	 * Ids of nodes that were selected to reach where active mode is now
	 */
	private Stack<Integer> eltSelectionHistory = null;
	
	// TODO: refactor this class into two classes; one with
	// visible model and the other not
	/**
	 * Set visible to true to handle how the view should be showing the tree model
	 */
	private boolean visible = false;
	
	/**
	 * This is a clone of the sugtnTreeModelActive suitable for passing it to the Tree component
	 */
	private SugtnTreeModel sugtnTreeModelVisible = null;
	
	/**
	 * Callbacks so that view acts upon model changes and events (not used in WAMCP)
	 */
	private Vector<ISugtnObserver> observers = null;
	
	/**
	 * When using certain view technologies this must
	 * be set with a listener (only one allowed though)
	 * that will stylize nodes as they are added
	 */
	private ISugtnTreeStylingListener stylingListener = null;
	
	/**
	 * The builder which this controller controls
	 */
	private SugtnTreeBuilder sugtnTreeBuilder = null;
	
	/**
	 * Used to get hold of a builder
	 */
	private SugtnTreeBuilderCache sugtTreeBuilderCache = null;
	
	/**
	 * Visible defaults to false!
	 */
	public SugtnTreeController() {
		this.visible = false;
		this.eltSelectionHistory = new Stack<Integer>();
		this.observers = new Vector<ISugtnObserver>();
	}
	
	/**
	 * @param sugtTreeBuilderCache
	 */
	public SugtnTreeController(SugtnTreeBuilderCache sugtTreeBuilderCache) {
		this();
		this.sugtTreeBuilderCache = sugtTreeBuilderCache;
	}
	
	/**
	 * For visible mode. This method incrementally build the Tree Model according to the node clicked.
	 * 
	 * @param nodeId
	 * @throws SugtnException
	 */
	protected void activateDescendantNode(int nodeId) throws SugtnException {
		
		this.guardUninit();
		
		SugtnTreeNode targetNode = this.sugtnTreeModelActive
				.findNodeById(nodeId);
		
		if (targetNode != null) {
			this.activateDescendantNode(targetNode);
		} else {
			throw new SugtnException(
					"Node with id: "
							+ nodeId
							+ " cannot be found in active model. Probably you have to move back in the model to select this domElt.");
		}
		
		if (LOG.isTraceEnabled()) {
			LOG.trace("Activated Sugtn Node (" + targetNode + ")");
		}
	}
	
	/**
	 * Selects the sugtnNodes along the relative XPath.
	 * 
	 * @param xpString
	 *            An XPath relative to the currently active node
	 * @throws SugtnException
	 */
	protected void activateDescendantNode(String xpString) throws SugtnException {
		
		this.guardUninit();
		
		StringTokenizer tokens = new StringTokenizer(xpString, "/", false);
		while (tokens.hasMoreTokens()) {
			String elementName = tokens.nextToken();
			
			int bracketIx = elementName.indexOf("[");
			if (bracketIx != -1) {
				
				elementName = elementName.substring(0, bracketIx);
			}
			
			if (elementName.startsWith("@")) {
				elementName = elementName.substring(1);
			}
			
			int colonIx = elementName.indexOf(':');
			if (colonIx != -1) {
				// TODO check that the pfx is bounded: String pfx = elementName.substring(0, colonIx - 1);
				
				elementName = elementName.substring(colonIx + 1);
				
			}
			
			SugtnDeclNode targetNode = null;
			targetNode = this.sugtnTreeModelActive.findNodeByLocalName(elementName, true);
			
			if (targetNode != null) {
				this.activateDescendantNode(targetNode);
			} else {
				throw new SugtnException("Node for " + elementName
						+ " cannot be found in active model!");
			}
		}
		
		if (LOG.isTraceEnabled()) {
			LOG.trace("Activated Sugtn Node at " + xpString);
		}
	}
	
	/**
	 * Expands (and builds if necessary) the tree model of the targetNode, attaches it to the total model, and replaces
	 * the currenlty active model with the new one.
	 * 
	 * @param targetNode
	 * @return The newly active node
	 * @throws SugtnException
	 */
	private SugtnTreeNode activateDescendantNode(SugtnTreeNode targetNode) throws SugtnException {
		if (targetNode == null) {
			throw new SugtnException("Cannot activate a null domElt!");
		} else {
			
			this.sugtnNodeActive = targetNode;
			
			if ((this.sugtnNodeActive instanceof SugtnElementNode)
					&& (this.sugtnNodeActive != this.sugtnTreeModelActive.getRoot())) {
				SugtnElementNode eltNode = (SugtnElementNode) this.sugtnNodeActive;
				if (this.sugtnNodeActive.isLeaf()) { // the first time we go into this
				
					this.sugtnTreeModelActive = this.sugtnTreeBuilder.modelForElement(
							eltNode.getTargetNamespaceURI(),
							eltNode.getLocalName(),
							this.stylingListener, eltNode.getMinOccurs(), eltNode.getMaxOccurs());
					// TODO: move the visible cloning to when the visible is requested
					if (this.visible) {
						this.sugtnTreeModelVisible = (SugtnTreeModel) this.sugtnTreeModelActive
								.clone();
					}
					
					SugtnTreeNode parent = (SugtnTreeNode) this.sugtnNodeActive
							.getParent();
					int ix = parent.getIndex(this.sugtnNodeActive);
					
					this.sugtnNodeActive.removeFromParent();
					this.sugtnNodeActive = (SugtnElementNode) this.sugtnTreeModelActive.getRoot();
					parent.insert(this.sugtnNodeActive, ix);
					
				} else { // already went into this element
					// just create a model around the newly active domElt
					this.sugtnTreeModelActive = new SugtnTreeModel(eltNode);
					// Expand the newly active domElt
					if (this.stylingListener != null) {
						this.stylingListener.setNodeExpanded(eltNode, true);
					}
					// TODO: move the visible cloning to when the visible is requested
					if (this.visible) {
						this.sugtnTreeModelVisible = (SugtnTreeModel) this.sugtnTreeModelActive
								.clone();
					}
				}
				
				this.eltSelectionHistory.push(Integer
						.valueOf(this.sugtnNodeActive.getNodeId()));
			}
			
			// FIXMENOT: Mediator is responsible for all communication with outside world
			// so move the responsibility of saying when to notify to it
			this.doNotifyActiveChanged();
			
			return this.sugtnNodeActive;
		}
	}
	
	/**
	 * Selects the sugtnNodes along the absolute XPath. The active node must be the root
	 * 
	 * @param xpString
	 *            absolute XPath.
	 * @throws SugtnException
	 */
	protected void activateNode(String xpString) throws SugtnException {
		while (!this.getEltSelHistoryEmpty()) {
			this.backInEltSelHistory();
		}
		
		// YA20100729 Not Descendent.. must start by root
		SugtnElementNode root = (SugtnElementNode) this.sugtnTreeModelTotal.getRoot();
		String rootName = root.getLocalName();
		if (xpString.startsWith("/" + rootName)) {
			this.activateDescendantNode(root);
		} else {
			throw new SugtnException("Absolute locator string must start by root elt: " + rootName);
		}
		
		xpString = xpString.substring(rootName.length() + 1); // +1 for the "/"
		
		// END //YA20100729
		
		this.activateDescendantNode(xpString);
	}
	
	/**
	 * Activates the previously active node, making its model the active one
	 * 
	 * @throws SugtnException
	 */
	protected void backInEltSelHistory() throws SugtnException {
		this.guardUninit();
		if (this.getEltSelHistoryEmpty()) {
			throw new SugtnException("History is empty!");
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("Activating previous Sugtn Node in selection history");
		}
		
		int prevNodeId = this.eltSelectionHistory.pop().intValue();
		
		SugtnTreeNode prevNode = this.sugtnTreeModelActive
				.findNodeById(prevNodeId);
		
		// if prevNode is null let the exception happen.. coz this is bad!! :)
		if (this.stylingListener != null) {
			this.stylingListener.setNodeExpanded(prevNode, false);
		}
		
		int prePrevNodeId = this.eltSelectionHistory.peek().intValue();
		
		SugtnElementNode prePrevNode = (SugtnElementNode) this.sugtnTreeModelTotal
				.findNodeById(prePrevNodeId);
		
		this.sugtnTreeModelActive = new SugtnTreeModel(prePrevNode);
		if (this.stylingListener != null) {
			this.stylingListener.styleModelRoot(this.sugtnTreeModelActive);
		}
		if (this.visible) {
			this.sugtnTreeModelVisible = (SugtnTreeModel) this.sugtnTreeModelActive
					.clone();
		}
		this.sugtnNodeActive = (SugtnTreeNode) this.sugtnTreeModelActive.getRoot();
		
		if (LOG.isTraceEnabled()) {
			LOG.trace("Actiaved Sugtn Node: (" + this.sugtnNodeActive + ")");
		}
		
		// FIXMENOT: Mediator is responsible for all communication with outside world
		// so move the responsibility of saying when to notify to it
		this.doNotifyActiveChanged();
		
	}
	
	/**
	 * Releases pointers (close)
	 */
	protected void deinitialize() {
		this.eltSelectionHistory.clear();
		this.sugtnTreeModelTotal = null;
		this.sugtnTreeModelActive = null;
		this.sugtnTreeModelVisible = null;
		this.sugtnNodeActive = null;
		this.sugtnTreeBuilder = null;
		if (LOG.isDebugEnabled()) {
			LOG.debug("Sugtn Tree de-initialized");
		}
	}
	
	/**
	 * Calls callbacks
	 */
	protected void doNotifyActiveChanged() {
		for (ISugtnObserver observer : this.observers) {
			observer.notifySugtnActiveChanged(this.sugtnNodeActive);
		}
	}
	
	/**
	 * 
	 * @return True if there are no more selections since the root was the active node
	 */
	public boolean getEltSelHistoryEmpty() {
		// there is always the root in the history.. yeah!! --> return
		// this.eltSelectionHistory.isEmpty();
		return this.eltSelectionHistory.size() == 1;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean getIsInitialized() {
		return this.sugtnTreeModelTotal != null;
	}
	
	/**
	 * Returns the SugtnNode at the path passed, without changing the currently active node.
	 * Actually it activates the nodes along the path, then activates the one originally active.
	 * 
	 * @param sugtnPathAbsolute
	 *            Like an XPath but without any predicates. The method tests to see whether it is relative or absolute.
	 * @return
	 * @throws SugtnException
	 */
	public SugtnDeclNode getNodeAtSugtnPath(String sugtnPathAbsolute) throws SugtnException {
		SugtnDeclNode result = null;
		
		// To get the domElt we must walk into the tree nodes (activate them)
		// so that we build models that are not already built!
		
		sugtnPathAbsolute = XPathStrUtils.removeLastIx(sugtnPathAbsolute);
		String sugtPathActive = this.sugtnNodeActive.getSugtnPath();
		String sugtnPathRel = sugtnPathAbsolute.replaceFirst(sugtPathActive, "");
		
		if (sugtnPathRel.equals(sugtnPathAbsolute)) {
			this.activateNode(sugtnPathRel);
		} else {
			this.activateDescendantNode(sugtnPathRel);
		}
		
		if (this.sugtnNodeActive instanceof SugtnDeclNode) {
			result = (SugtnDeclNode) this.sugtnNodeActive;
		}
		
		this.activateNode(sugtPathActive);
		
		return result;
		
	}
	
	/**
	 * @return the stylingListener
	 */
	public ISugtnTreeStylingListener getStylingListener() {
		return this.stylingListener;
	}
	
	/**
	 * 
	 * @return The element declaration for which the current model corresponds
	 * @throws SugtnException
	 */
	protected SugtnElementNode getSugtnActiveRoot() throws SugtnException {
		this.guardUninit();
		
		return (SugtnElementNode) this.sugtnTreeModelActive.getRoot();
		
	}
	
	/**
	 * For printing
	 * 
	 * @return
	 */
	public String getSugtnActiveRootName() {
		if (!this.getIsInitialized()) {
			return "Uninitialized!";
		}
		String result = null;
		
		result = ((SugtnElementNode) this.sugtnTreeModelActive.getRoot()).getLocalName();
		
		return result;
	}
	
	/**
	 * The current selection
	 * 
	 * @return the sugtnNodeActive
	 */
	protected SugtnTreeNode getSugtnNodeActive() {
		return this.sugtnNodeActive;
	}
	
	/**
	 * The sugtn path of the current selection (not the tree path)
	 * 
	 * @return
	 */
	public String getSugtnNodeActivePath() {
		return this.sugtnNodeActive.getSugtnPath();
	}
	
	/**
	 * @return the sugtnTreeModelActive
	 */
	protected SugtnTreeModel getSugtnTreeModelActive() {
		return this.sugtnTreeModelActive;
	}
	
	/**
	 * 
	 * @return
	 */
	protected SugtnTreeModel getSugtnTreeModelTotal() {
		return this.sugtnTreeModelTotal;
	}
	
	/**
	 * 
	 * @return
	 */
	public SugtnTreeModel getSugtnTreeModelVisible() {
		return this.sugtnTreeModelVisible;
	}
	
	/**
	 * Makes sure that the controller is intialized
	 * 
	 * @throws SugtnException
	 */
	private void guardUninit() throws SugtnException {
		if (!this.getIsInitialized()) {
			throw new SugtnException("Uninitialized!");
		}
	}
	
	/**
	 * Initialized the controller on the passed schema, with the passed element name and namespace as root
	 * 
	 * @param schemaFilePath
	 * @param nameSpaceURI
	 * @param rootEltName
	 * @throws SugtnException
	 */
	protected void initialize(String schemaFilePath, String nameSpaceURI,
			String rootEltName)
			throws SugtnException {
		if (this.sugtTreeBuilderCache == null) {
			throw new SugtnException(
					"Sugtn Tree Builder Cache must be set before this controller can be initialized");
		}
		
		this.sugtnTreeBuilder = this.sugtTreeBuilderCache.builderForSchema(schemaFilePath);// SugtnTreeBuilder.getInstance(filePath);
		this.sugtnTreeBuilder.setSugtnController(this);
		
		this.sugtnTreeModelTotal = this.sugtnTreeBuilder.modelForElement(nameSpaceURI, rootEltName,
				this.stylingListener, 1, 1);
		this.sugtnTreeModelActive = this.sugtnTreeModelTotal;
		if (this.visible) {
			this.sugtnTreeModelVisible = (SugtnTreeModel) this.sugtnTreeModelActive
					.clone();
		}
		this.sugtnNodeActive = (SugtnTreeNode) this.sugtnTreeModelActive
				.getRoot();
		
		this.eltSelectionHistory.push(Integer.valueOf(this.sugtnNodeActive.getNodeId()));
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("Sugtn Tree initialized for root element <" + rootEltName + " ["
					+ nameSpaceURI + "]>");
		}
	}
	
	/**
	 * @return the visible
	 */
	public boolean isVisible() {
		return this.visible;
	}
	
	/**
	 * Makes sure that a descendant element model is already built, without changing the currently active element
	 * 
	 * @param target
	 * @return The descendant tree node (now root of the model)
	 * @throws SugtnException
	 */
	public SugtnTreeNode populateModelForDescendant(SugtnElementNode target) throws SugtnException {
		SugtnTreeNode result = null;
		try {
			result = this.activateDescendantNode(target);
		} finally {
			this.backInEltSelHistory();
		}
		return result;
	}
	
	public boolean registerObserver(ISugtnObserver observer) {
		return this.observers.add(observer);
	}
	
	/**
	 * @param stylingListener
	 *            the stylingListener to set
	 */
	public void setStylingListener(ISugtnTreeStylingListener stylingListener) {
		this.stylingListener = stylingListener;
	}
	
	/**
	 * @param sugtTreeBuilderCache
	 *            the sugtTreeBuilderCache to set
	 */
	public void setSugtTreeBuilderCache(SugtnTreeBuilderCache sugtTreeBuilderCache) {
		this.sugtTreeBuilderCache = sugtTreeBuilderCache;
	}
	
	/**
	 * @param visible
	 *            the visible to set
	 */
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	/**
	 * 
	 * @param observer
	 * @return
	 */
	public boolean unregisterObserver(ISugtnObserver observer) {
		return this.observers.remove(observer);
	}
	
}
