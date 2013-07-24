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

package org.bibalex.sdxe.xsa.uigen.icefaces;

import java.util.LinkedList;

import org.bibalex.sdxe.binding.SDXEMediatorBean;
import org.bibalex.sdxe.controller.IDomObserver;
import org.bibalex.sdxe.exception.SDXEException;
import org.bibalex.sdxe.suggest.model.dom.SugtnDeclQName;
import org.bibalex.sdxe.suggest.model.dom.SugtnElementNode;
import org.bibalex.sdxe.xsa.application.XSAConstants;
import org.bibalex.sdxe.xsa.application.XSAUiActionListener;
import org.bibalex.sdxe.xsa.exception.XSAException;
import org.bibalex.sdxe.xsa.exception.XSAXMLInvalidException;
import org.bibalex.sdxe.xsa.model.XSAAnnotator;
import org.bibalex.sdxe.xsa.model.XSADataholder;
import org.bibalex.sdxe.xsa.model.XSADocCache;
import org.bibalex.sdxe.xsa.model.XSADocument;
import org.bibalex.sdxe.xsa.model.XSAInstance;
import org.bibalex.sdxe.xsa.model.XSAInstance.XSAAccessTypes;
import org.bibalex.sdxe.xsa.model.XSALocated;
import org.bibalex.sdxe.xsa.model.XSAUiDivisions;
import org.bibalex.uigen.icefaces.DoNothingCompsDecorator;
import org.bibalex.uigen.icefaces.IceCompsFactory;
import org.bibalex.uigen.icefaces.UIGenerationException;
import org.bibalex.util.FacesUtils;
import org.jdom.JDOMException;

import com.icesoft.faces.component.ext.HtmlCommandLink;
import com.icesoft.faces.component.ext.HtmlPanelGroup;

/**
 * Observer to the DOMController, so that UI generation is initiated when the DOM moves vertically (upon entering an
 * Area or leaving it)
 */
public abstract class XSAUiGenDomListener implements IDomObserver {
	/** Internal State */
	protected XSAInstance instForDomCurrent = null;
	protected SugtnElementNode sugtnForDomCurrent = null;
	
	/** Bound to UI */
	protected HtmlPanelGroup container = null;
	protected HtmlPanelGroup toolbarParent = null;
	
	/** Managed Beans */
	protected SDXEMediatorBean sdxeMediator = null;
	protected XSADocument xsaDoc = null;
	protected XSAUiBindersBean bindersBean = null;
	
	/** For delaying UI generation until the container panel is actually set */
	protected SugtnElementNode pendingEventSugtnElt;
	
	/**
	 * Command links for adding different Annotators to contents of fields. They all have the same actionListener, but
	 * different attributes
	 */
	protected LinkedList<HtmlCommandLink> annotatorsLinks;
	
	/**
	 * It was intended that XSA can handle more than one schema in the same time, thus the xsaDocCache was imployed
	 * instead of having a singleton xsaDoc
	 */
	public XSAUiGenDomListener(String schemaFilePath) {
		this.xsaDoc = XSADocCache.getInstance().xsaDocForSchema(schemaFilePath);
		
	}
	
	/**
	 * Called much more than you imagine!
	 */
	public void clearUI() {
		this.bindersBean.clearBinders();
		this.bindersBean.getBaseXPShiftedPanels().clear();
		
		FacesUtils.clearChildrenAndFacets(this.container);
		
	}
	
	/**
	 * For separating UI generation from any specific UI technology
	 */
	protected abstract XSAUiGenFactoryAbstract createUiGenFactory();
	
	/**
	 * Reads annotators from the XSA and creates links to add them
	 */
	private void generateAnnotatorsLinks() throws XSAException, JDOMException,
			UIGenerationException {
		this.annotatorsLinks = new LinkedList<HtmlCommandLink>();
		for (XSAAnnotator annot : this.xsaDoc.getAnnotators()) {
			
			IceCompsFactory compsFactory = new IceCompsFactory(new DoNothingCompsDecorator());
			String eltName = annot.getEltLocalName();
			String displayVal = annot.getLabel("en");
			if ((displayVal == null) || displayVal.isEmpty()) {
				displayVal = eltName;
			}
			
			displayVal += " <" + eltName + ">";
			
			HtmlCommandLink annotCommand = (HtmlCommandLink) compsFactory.command(
					HtmlCommandLink.COMPONENT_TYPE,
					"Annotator_" + eltName, displayVal, "#{"
							+ XSAConstants.BEANNAME_ACTION_LISTENER + ".prepareAnnotator}");
			
			annotCommand.setTitle(annot.getHelp("en"));
			
			annotCommand.getAttributes().put(XSAUiActionListener.PARAM_ANNOTATOR, eltName);
			
			annotCommand.setImmediate(true);
			
			annotCommand.setRenderedOnUserRole(annot.getAccessRoles(XSAAccessTypes.Read));
			annotCommand.setEnabledOnUserRole(annot.getAccessRoles(XSAAccessTypes.Write));
			
			annotCommand.setOnclick("; toggleAnnots();");
			
			this.annotatorsLinks.add(annotCommand);
		}
	}
	
	public LinkedList<HtmlCommandLink> getAnnotatorsLinks() throws XSAException, JDOMException,
			UIGenerationException {
		if (this.annotatorsLinks == null) {
			this.generateAnnotatorsLinks();
		}
		return this.annotatorsLinks;
	}
	
	/**
	 * @return the container
	 */
	public HtmlPanelGroup getContainer() {
		return this.container;
	}
	
	/**
	 * @return the instForDomCurrent
	 */
	public XSAInstance getInstForDomCurrent() {
		return this.instForDomCurrent;
	}
	
	/**
	 * @return the sugtnForDomCurrent
	 */
	public SugtnElementNode getSugtnForDomCurrent() {
		return this.sugtnForDomCurrent;
	}
	
	public HtmlPanelGroup getToolbarParent() {
		return this.toolbarParent;
	}
	
	public void init() {
		this.sdxeMediator.getDomTreeController().registerObserver(this);
	}
	
	/**
	 * This proved useless because the index passed is the index between brothers of the same element, not of the same
	 * designating attributes
	 */
	@Override
	public void notifyDomMovedHorizontal(SugtnElementNode sugtnElt, int index)
			throws SDXEException {
		
// for (DomXPathRelToActiveBinder binder :
// this.bindersBean.getDomValueBinders().values()) {
//
// binder.setIxBetBros(index);
//
		
	}
	
	/**
	 * This method is called when the DOM moves vertically (upon entering an
	 * Area or leaving it), it initiates UI generation
	 */
	@Override
	public void notifyDomMovedVertical(SugtnElementNode sugtnEltNew)
			throws SDXEException {
		if (this.container == null) {
			this.pendingEventSugtnElt = sugtnEltNew;
			return;
		}
		// Clear first
		this.clearUI();
		
		// And gen
		
		XSAInstance instForDomNew = null;
		
		// Moving vertically we will go to the first instance of
		// each Sugtn Path step in the Dom
		// This means that site locator cannot contain [ix]es
		// Because there won't be Olds to get them from
		String locatorNew = null;
		if ((this.sugtnForDomCurrent != null) && (this.instForDomCurrent != null)) {
			String sugtnPathDiff = sugtnEltNew
					.getSugtnPathRelative(this.sugtnForDomCurrent);
			
			String locatorBase;
			
			try {
				assert this.instForDomCurrent.getUiDivision().equals(
						XSAUiDivisions.site) : "This inst must be of type Site.. this is the only use now";
			} catch (JDOMException e) {
				e.printStackTrace();
			}
			
			locatorBase = this.instForDomCurrent.getLocator().asString();
			
			locatorNew = locatorBase + sugtnPathDiff;
		} else {
			locatorNew = sugtnEltNew.getSugtnPath();
		}
		
		XSALocated locatedForDomNew = null;
		try {
			locatedForDomNew = this.xsaDoc.getLocated(locatorNew);
			if (locatedForDomNew == null) {
				// NO XSA Rendering
			} else {
				if (locatedForDomNew instanceof XSADataholder) {
					XSADataholder dhForDomNew = (XSADataholder) locatedForDomNew;
					
					instForDomNew = dhForDomNew.getInstance(1);
					
					XSAUiGenFactoryAbstract uiGenFactory = this.createUiGenFactory();
					
					XSAUiGenBuilder uiGenBuilder = new XSAUiGenBuilder(
							this.xsaDoc, (XSAUiGenFactoryAbstract) uiGenFactory,
							this.sdxeMediator.getSugtnTreeController());
					
					try {
						uiGenBuilder.generateUi(this.container, sugtnEltNew,
								instForDomNew);
					} catch (UIGenerationException e) {
						throw new XSAException(e);
					}
					
				} else {
					throw new XSAXMLInvalidException(
							"Expected dataHolder for Locator: "
									+ locatorNew
									+ " and found different domElt type");
				}
			}
			
			// just in case we need the old in some step
			this.instForDomCurrent = instForDomNew;
			this.sugtnForDomCurrent = sugtnEltNew;
		} catch (XSAException e) {
			e.printStackTrace();
		} catch (JDOMException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * This method is called in case that the DOM and Suggestion trees are not pointing to the same elements. 
	 * 
	 */
	@Override
	public void notifyDomNSugtnOutOfSync(SugtnDeclQName currDomEltName)
			throws SDXEException {
		this.clearUI();
	}
	
	/**
	 * @param bindersBean
	 *            the bindersBean to set
	 */
	public void setBindersBean(XSAUiBindersBean bindersBean) {
		this.bindersBean = bindersBean;
	}
	
	/**
	 * @param container
	 *            the container to set
	 * @throws SDXEException
	 */
	public void setContainer(HtmlPanelGroup container) throws SDXEException {
		this.container = container;
		
		if (this.pendingEventSugtnElt != null) {
			this.notifyDomMovedVertical(this.pendingEventSugtnElt);
			this.pendingEventSugtnElt = null;
		}
	}
	
	/**
	 * @param sdxeMediator
	 *            the sdxeMediator to set
	 */
	public void setSdxeMediator(SDXEMediatorBean sdxeMediator) {
		if (this.sdxeMediator != null) {
			if (this.sdxeMediator == sdxeMediator) {
				// same.. don't do anything
				return;
			} else {
				this.sdxeMediator.getDomTreeController().unregisterObserver(
						this);
			}
		}
		
		this.sdxeMediator = sdxeMediator;
		
	}
	
	public void setToolbarParent(HtmlPanelGroup toolbarParent) {
		this.toolbarParent = toolbarParent;
	}
	
	/**
	 * @param xsaDoc
	 *            the xsaDoc to set
	 */
	public void setXsaDoc(XSADocument xsaDoc) {
		this.xsaDoc = xsaDoc;
	}
	
	public void uninit() {
		this.sdxeMediator.getDomTreeController().unregisterObserver(this);
	}
	
}
