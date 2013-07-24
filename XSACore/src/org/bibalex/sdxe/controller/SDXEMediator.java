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

import org.apache.log4j.Logger;
import org.bibalex.sdxe.exception.DomBuildException;
import org.bibalex.sdxe.exception.DomBuildSugtnTreeSyncException;
import org.bibalex.sdxe.exception.SDXEException;
import org.bibalex.sdxe.exception.SugtnException;
import org.bibalex.sdxe.suggest.model.SugtnTreeNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnDeclQName;
import org.bibalex.sdxe.suggest.model.dom.SugtnElementNode;
import org.jdom.JDOMException;

/**
 * Coordinates the DomTreeController and the SugtnTreeController, mainly for use with the pointers mode. However, it
 * contains some methods that are useful also in XPath mode
 * 
 * @author Younos.Naga
 * 
 */
public class SDXEMediator {
	private static final Logger LOG = Logger.getLogger("org.bibalex.sdxe");
	
	protected SugtnTreeController sugtnTreeController = null;
	protected DomTreeController domTreeController = null;
	
	public SDXEMediator() {
		
	}
	
	/**
	 * Add the currently selected node of the sugtnTreeController to the DOM, and maintains the pointers
	 * 
	 * @throws SDXEException
	 */
	public void addSugtnEltToDom() throws SDXEException {
		
		SugtnElementNode sugtnEltNode = this.sugtnTreeController.getSugtnActiveRoot();
		
		boolean firstChange = !this.domTreeController.getChangesExist();
		try {
			
			if (!firstChange) {
				if (sugtnEltNode.getqName().equals(
						this.domTreeController.getCloneActiveQName())) {
					// When adding another instances of elements
					// that are already added, we have to go up
					// in the DOM before adding
					// We do this from the first time
					// when adding to Clone Dom because
					// all changes are direct descendants
					// of each other
					
					// TODONOT: implement adding multiple elements of same type
					this.domTreeController.moveActiveUpToParent();
					
				}
			}
			
			this.domTreeController.addElement(sugtnEltNode);
		} catch (DomBuildSugtnTreeSyncException e) {
			if (firstChange) {
				if (sugtnEltNode.getqName().equals(
						this.domTreeController.getRealActiveRootQName())) {
					
					// When adding new instances of elements
					// that already exist, we have to go up
					// in the DOM before adding
					// We cannot do this from the first time
					// when adding to the Real Dom
					// because maybe this child element
					// is recursively in a parent of its
					// own type (this can be far away too)
					
					this.domTreeController.rollbackToRealDom();
					
					if (this.domTreeController.getChangesExist()) {
						throw new DomBuildException("Changes exist just after Rollback!");
					} else {
						this.domTreeController.moveActiveUpToParent();
					}
					
					if (LOG.isDebugEnabled()) {
						LOG
								.debug("Synchronized Dom and Sugtn trees automatically: Moved domReal up");
					}
					
					this.domTreeController.addElement(sugtnEltNode); // , this.sugtnTreeBean
					// .getSugtnTreeModelTotal()); //, data);
				}
				
			} else {
				throw e;
			}
		}
		
		this.domTreeController.doNotifyDomMovedVertical(sugtnEltNode);
		
	}
	
	/**
	 * Closes both contorllers
	 * 
	 * @throws DomBuildException
	 */
	public void close() throws DomBuildException {
		this.domTreeController.close();
		this.sugtnTreeController.deinitialize();
	}
	
	/**
	 * Used in XPath mode as well, to commit and maintain the pointer of the Real Active in the DOM controller
	 * 
	 * @throws DomBuildException
	 */
	public void commit() throws DomBuildException {
		
		String xpathOfNewest = this.domTreeController.commitToRealDom();
		try {
			this.domTreeController.moveRealToXPath(xpathOfNewest);
		} catch (JDOMException e) {
			throw new DomBuildException(e);
		}
		
	}
	
	/**
	 * Deletes the current Sugtn node from DOM, maintaining pointers
	 * 
	 * @throws SDXEException
	 */
	public void deleteCurrDomNode() throws SDXEException {
		
		if (this.getIsSugtnAndDomActiveEquivalent()) {
			
			this.domTreeController.deleteActive(this.sugtnTreeController.getSugtnActiveRoot());
			
			if (this.getIsSugtnAndDomActiveEquivalent()) {
				
				this.domTreeController.doNotifyDomMovedHorizontal(this.sugtnTreeController
						.getSugtnActiveRoot(),
						this.domTreeController.getActiveIxBetBros());
				
			} else {
				
				this.domTreeController.doNotifyDomNSugtnOutOfSync(this.domTreeController
						.getActiveQName());
				this.prevInSugtnTreeHistory();
				
			}
			
		} else {
			
			throw new DomBuildSugtnTreeSyncException(
					"Cannot delete while Sugtn and Dom trees are not in Sync");
			
		}
	}
	
	/**
	 * Get the current selection of the Sugtn Tree
	 * 
	 * @return
	 * @throws SugtnException
	 */
	public SugtnElementNode getCurrentSugtnElement() throws SugtnException {
		return this.sugtnTreeController.getSugtnActiveRoot();
	}
	
	/**
	 * 
	 * @return True if the sugtn active and the dom active both correspond to the same XML tag
	 * @throws SugtnException
	 * @throws DomBuildException
	 */
	public boolean getIsSugtnAndDomActiveEquivalent() throws SugtnException, DomBuildException {
		return this.sugtnTreeController.getSugtnActiveRoot().getqName().equals(
				this.domTreeController.getActiveQName());
	}
	
	/**
	 * Moves both sugtn and dom into a child
	 * 
	 * @param nodeId
	 *            The id of the node in the Sugtn Tree
	 * @return The newly selected node
	 * @throws SDXEException
	 */
	public SugtnTreeNode moveIntoChild(int nodeId) throws SDXEException {
		this.sugtnTreeController.activateDescendantNode(nodeId);
		
		SugtnTreeNode node = this.sugtnTreeController.getSugtnNodeActive();
		
		if ((node != null) && (node instanceof SugtnElementNode)) {
			this.domTreeController.moveActiveIntoChild((SugtnElementNode) node);
			if (this.getIsSugtnAndDomActiveEquivalent()) {
				this.domTreeController.doNotifyDomMovedVertical(this.getCurrentSugtnElement());
			} else {
				this.domTreeController.doNotifyDomNSugtnOutOfSync(this.domTreeController
						.getActiveQName());
			}
		}
		
		return node;
	}
	
	/**
	 * Moves the DOM and the Sugtn tree to a certain element hierarchy, not taking into consideration designating
	 * attributes
	 * 
	 * @param sugtnPathStr
	 * @throws SDXEException
	 */
	public void moveToSugtnPath(String sugtnPathStr) throws SDXEException {
		
		try {
			this.domTreeController.moveRealToDeepestExiting(sugtnPathStr);
		} catch (JDOMException e) {
			throw new SDXEException(e);
		}
		
		String pathBase = this.sugtnTreeController.getSugtnActiveRoot().getSugtnPath();
		String pathDiff = null;
		
		if (sugtnPathStr.startsWith(pathBase)) {
			// Realtive
			pathDiff = sugtnPathStr.replaceFirst(pathBase, "");
		} else {
			// Absolute
			if (!this.sugtnTreeController.getEltSelHistoryEmpty()) {
				throw new SDXEException(
						"The active element must be the root before moving to a sugtn path that is not deeper in the tree");
			}
			pathDiff = sugtnPathStr;
		}
		
		this.sugtnTreeController.activateDescendantNode(pathDiff);
		
		this.domTreeController.doNotifyDomMovedVertical(this.getCurrentSugtnElement());
	}
	
	public void nextInDom() throws SDXEException {
		
		this.domTreeController.moveActiveToNextBro();
		
		this.domTreeController.doNotifyDomMovedHorizontal(this.sugtnTreeController
				.getSugtnActiveRoot(),
				this.domTreeController.getActiveIxBetBros());
	}
	
	/**
	 * Opens an XML file as well as a schema file
	 * 
	 * @param xmlFilePath
	 * @param schemaFilePath
	 * @throws SDXEException
	 */
	public void open(String xmlFilePath, String schemaFilePath) throws SDXEException {
		
		try {
			// reinitialize
			this.close();
			
			this.domTreeController.load(new File(xmlFilePath));
			
			SugtnDeclQName domRootQName = this.domTreeController.getActiveQName();
			
			this.sugtnTreeController.initialize(schemaFilePath, domRootQName
					.getTargetNamespaceURI(),
					domRootQName.getLocalName());
			
			this.domTreeController.doNotifyDomMovedVertical(this.getCurrentSugtnElement());
			
		} catch (JDOMException e) {
			throw new DomBuildException(e);
		} catch (IOException e) {
			throw new DomBuildException(e);
		}
		
	}
	
	/**
	 * Pointers mode
	 * 
	 * @throws SDXEException
	 */
	public void prevInDom() throws SDXEException {
		
		this.domTreeController.moveActiveToPrevBro();
		
		this.domTreeController.doNotifyDomMovedHorizontal(this.sugtnTreeController
				.getSugtnActiveRoot(),
				this.domTreeController.getActiveIxBetBros());
	}
	
	/**
	 * Both modes.. pops up a selection from the history of selections made to reach the currently selected Sugtn node.
	 * 
	 * @throws SDXEException
	 */
	public void prevInSugtnTreeHistory() throws SDXEException {
		
		SugtnDeclQName activeElmQName = this.sugtnTreeController.getSugtnActiveRoot().getqName();
		// Move dom up first:
		if (activeElmQName.equals(this.domTreeController.getActiveQName())) {
			this.domTreeController.moveActiveUpToParent();
		}
		
		// if we could move Dom, then proceed to move the suggestion
		this.sugtnTreeController.backInEltSelHistory();
		
		if (this.getIsSugtnAndDomActiveEquivalent()) {
			
			this.domTreeController.doNotifyDomMovedVertical(this.getCurrentSugtnElement());
		} else {
			
			this.domTreeController.doNotifyDomNSugtnOutOfSync(this.domTreeController
					.getActiveQName());
		}
	}
	
	/**
	 * Calls rollback of DOM
	 * 
	 * @throws SDXEException
	 */
	public void rollback() throws SDXEException {
		
		this.domTreeController.rollbackToRealDom();
		
		while (!this.getIsSugtnAndDomActiveEquivalent()) {
			this.sugtnTreeController.backInEltSelHistory();
		}
		
		this.domTreeController.doNotifyDomMovedHorizontal(this.sugtnTreeController
				.getSugtnActiveRoot(),
				this.domTreeController.getActiveIxBetBros());
		
	}
	
	/**
	 * Calls save of DOM
	 * 
	 * @param filePath
	 * @throws DomBuildException
	 * @throws IOException
	 */
	public void saveRealDom(String filePath) throws DomBuildException, IOException {
		FileWriter writer = new FileWriter(filePath);
		this.domTreeController.save(writer);
		writer.close();
		
		if (LOG.isInfoEnabled()) {
			LOG.info("DOM written to file: " + filePath);
		}
		
	}
	
	/**
	 * @param domTreeController
	 *            the domTreeController to set
	 */
	public void setDomTreeController(DomTreeController domTreeController) {
		this.domTreeController = domTreeController;
	}
	
	/**
	 * @param formsManager
	 *            the formsManager to set
	 */
	@Deprecated
	public void setFormsManager(IDomObserver formsManager) {
		this.domTreeController.registerObserver(formsManager);
	}
	
	/**
	 * @param sugtnTreeController
	 *            the sugtnTreeController to set
	 */
	public void setSugtnTreeController(SugtnTreeController sugtnTreeBean) {
		this.sugtnTreeController = sugtnTreeBean;
	}
	
}
