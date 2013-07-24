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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIPanel;

import org.apache.log4j.Logger;
import org.bibalex.sdxe.controller.SugtnTreeController;
import org.bibalex.sdxe.exception.SugtnException;
import org.bibalex.sdxe.suggest.model.dom.ISugtnTypedNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnAttributeNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnDeclNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnElementNode;
import org.bibalex.sdxe.xsa.application.XSAConstants;
import org.bibalex.sdxe.xsa.application.XSAUiActionListener.ValueTypes;
import org.bibalex.sdxe.xsa.exception.XSAException;
import org.bibalex.sdxe.xsa.exception.XSAXMLInvalidException;
import org.bibalex.sdxe.xsa.model.XSADocument;
import org.bibalex.sdxe.xsa.model.XSAGroup;
import org.bibalex.sdxe.xsa.model.XSAInstance;
import org.bibalex.sdxe.xsa.model.XSAInstance.XSAAccessTypes;
import org.bibalex.sdxe.xsa.model.XSAUiDivisions;
import org.bibalex.uigen.icefaces.DoNothingCompsDecorator;
import org.bibalex.uigen.icefaces.IceCompsFactory;
import org.bibalex.uigen.icefaces.UIGenerationException;
import org.bibalex.util.FacesUtils;
import org.jdom.JDOMException;

import com.icesoft.faces.component.ext.HtmlPanelGroup;
import com.icesoft.faces.component.paneltabset.PanelTabSet;

/**
 * Drives the building of the UI of an instance, according to the structure desribed in XSA
 */
public class XSAUiGenBuilder {
	private static final Logger LOG = Logger.getLogger("org.bibalex.xsa");
	
	private final XSADocument xsaDoc;
	private final XSAUiGenFactoryAbstract uiFactory;
	private final SugtnTreeController sugtnTreeController;
	
	private final HashSet<XSAInstance> instsProccessed;
	
	private boolean applyRevertCreated = false;
	private HashMap<String, HtmlPanelGroup> groupPanelsMap = null;
	
	/**
	 * Call this to generate the UI
	 * 
	 * @param xsaDoc
	 * @param uiFactory
	 *            Can be null for any UI division, except fields
	 * @param sugtnTreeController
	 */
	public XSAUiGenBuilder(XSADocument xsaDoc, XSAUiGenFactoryAbstract uiFactory,
			SugtnTreeController sugtnTreeController) {
		this.xsaDoc = xsaDoc;
		this.uiFactory = uiFactory;
		this.sugtnTreeController = sugtnTreeController;
		
		this.instsProccessed = new HashSet<XSAInstance>();
		this.applyRevertCreated = false;
		
		this.groupPanelsMap = new HashMap<String, HtmlPanelGroup>();
	}
	
	/**
	 * Calls the methods of the UI factory that create the suitable UI for the UI division of the instance, this method continue
	 * its work recursively starting from the instance sent to it ending to the most deepest element it can reach 
	 * @param container the UI Element to which the generated UI will be added to
	 * @param declNode the XSA document model
	 * @param xsaInst the instance of the document to start the UI generation from
	 */
	public void generateUi(UIPanel container,
			SugtnDeclNode declNode,
			XSAInstance xsaInst) throws JDOMException, XSAException, SugtnException,
			UIGenerationException {
		
		HtmlPanelGroup applyRevertPanel = null;
		IceCompsFactory compsFactory = new IceCompsFactory(new DoNothingCompsDecorator());
		HtmlPanelGroup resultCont = (HtmlPanelGroup) compsFactory.panel(
				HtmlPanelGroup.COMPONENT_TYPE,
				null);
		List<XSAInstance> subDivList = null;
		
		switch (xsaInst.getUiDivision()) {
			case site:

				subDivList = this.xsaDoc.getSubDivisionsUnder(XSAUiDivisions.area, xsaInst);
				
				if (subDivList.size() > 0) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Areas found under: " + xsaInst);
					}
					UIPanel sitePanel = resultCont;
					int areaIxInUi = 1;
					for (XSAInstance areaInst : subDivList) {
						
						UIPanel subDivPnl = this.instOrderedPanel(sitePanel,
								areaIxInUi,
								areaInst.getGroup(),
								null, areaInst.getAccessRoles(XSAAccessTypes.Read));
						
						UIComponent toDecorate = this.uiFactory.createArea(subDivPnl,
								areaInst);
						
						areaIxInUi++;
					}
					
					break;
					
				} // else fallthrough to see if there are sections directly under site
				
			case area:

				String baseShiftPattern = xsaInst.getBaseShiftPattern();
				if ((baseShiftPattern != null) && !baseShiftPattern.isEmpty()) {
					this.uiFactory.addBaseShifters(xsaInst);
					
					HtmlPanelGroup baseShiftersParent = (HtmlPanelGroup) compsFactory.panel(
							HtmlPanelGroup.COMPONENT_TYPE,
							null);
					
					this.uiFactory.applyFacelet(baseShiftersParent,
							"/templates/xsa/baseShiftersParent.xhtml");
					
					container.getChildren().add(baseShiftersParent);
				}
				
				subDivList = this.xsaDoc
						.getSubDivisionsUnder(XSAUiDivisions.section, xsaInst); // xsaDH
				
				if (subDivList.size() > 0) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Sections found under: " + xsaInst);
					}
					
					UIPanel areaOrderedPanel = this.instOrderedPanel(resultCont,
							1, // YA20100810 Ordering: yes, only one tabSeries
							null, // YA20100810 Orderind: tabs cannot be in groups! xsaInst.getGroup(),
							null, null);
					
					PanelTabSet areaPanelTabSet = (PanelTabSet) compsFactory.series(
							PanelTabSet.COMPONENT_TYPE,
							null);
					areaOrderedPanel.getChildren().add(areaPanelTabSet);
					
					for (XSAInstance secInst : subDivList) {
						this.applyRevertCreated = true;
						
						// PanelTab
						UIPanel secPanel = this.uiFactory.createSection(areaPanelTabSet,
														secInst);
						
						this.generateUi(secPanel,
								null, // It's ok to pass null for all ui divisions except Field
								secInst);
						
// meaning less because no fall through always yet: this.instsProccessed.add(secInst);
						
					}
					
					break;
				} // else fallthrough to see if there are subsections... etc directly
				
				// //////////////////// Different types of processing //////////
				
			case section:

				// Apply revert for areas with no sections that contain fields
				if (!this.applyRevertCreated) {
					applyRevertPanel = (HtmlPanelGroup) this.uiFactory.addApplyRevert(
							XSAConstants.ID_pnlAppRevForm,
							this.uiFactory.generateIdForInstance(xsaInst),
							ValueTypes.all, xsaInst, false);
					this.applyRevertCreated = true;
				}
				
				// Cont in Cont:
			case container:
				if (this.guardInstNotProccessedYet(xsaInst)) {
					subDivList = this.xsaDoc.getSubDivisionsUnder(XSAUiDivisions.container,
							xsaInst); // xsaDH);
					
					if (subDivList.size() > 0) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("Containers found under: " + xsaInst);
						}
						UIPanel subsecPanel = resultCont;
						
						for (XSAInstance containerInst : subDivList) {
							SugtnElementNode sugtnEltForSubDiv = this
									.sugtnEltNodeForInstance(containerInst);
							
							UIPanel subDivCont = this.instOrderedPanel(subsecPanel,
									containerInst.getIxInUi(),
									containerInst.getGroup(),
									null, containerInst.getAccessRoles(XSAAccessTypes.Read));
							
							UIPanel subDivPnl = this.uiFactory.createContainer(
									subDivCont,
									sugtnEltForSubDiv,
									containerInst, -1);
							
							// YA 20100811 Don't eat Vertical Space
							// If a container contains only one field, the container box is not rendered
							if (containerInst.isNomixed() && !containerInst.isFacsRequired()
									&& (containerInst.getInstsCountUnderByIdRef() <= 1)) {
								FacesUtils.clearChildrenAndFacets(subDivCont);
								subDivPnl = subDivCont;
							}
							// END YA 20100811
							
							this.generateUi(subDivPnl, sugtnEltForSubDiv,
									containerInst);
							
						}
						// FALL THROUGH ALWAYS
					}
					
					subDivList = this.xsaDoc.getSubDivisionsUnder(XSAUiDivisions.field,
							xsaInst);
					
					if (subDivList.size() > 0) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("Fields found under: " + xsaInst);
						}
						UIPanel containerPnl = resultCont;
						
						for (XSAInstance fieldInst : subDivList) {
							SugtnDeclNode sugtnForField = this.sugtnTreeController
									.getNodeAtSugtnPath(fieldInst.getLocator().asString());
							
							UIPanel subDivCont = this.instOrderedPanel(containerPnl,
									fieldInst.getIxInUi(),
									fieldInst.getGroup(),
									null, fieldInst.getAccessRoles(XSAAccessTypes.Read));
							
							this.generateUi(subDivCont, sugtnForField,
									fieldInst);
						}
// fall through always
					}
					
				}
			case field:

				if (xsaInst.getUiDivision().equals(XSAUiDivisions.field)) { //
				
					if (this.guardInstNotProccessedYet(xsaInst)) {
						
						ISugtnTypedNode sugtnTyped = null;
						SugtnElementNode eltNode = null;
						
						if (declNode instanceof SugtnElementNode) {
							eltNode = (SugtnElementNode) declNode;
						}
						
						if (eltNode != null) {
							sugtnTyped = ((SugtnElementNode) eltNode).getPcDataNode();
						} else {
							sugtnTyped = (SugtnAttributeNode) declNode;
						}
						
						if (sugtnTyped == null) {
							throw new XSAException(
									"Field UI type must be an Attribute or an Element allowed to have PCData children");
						}
						
						UIPanel fieldPnl = this.instOrderedPanel(resultCont,
								xsaInst.getIxInUi(),
								xsaInst.getGroup(),
								null, xsaInst.getAccessRoles(XSAAccessTypes.Read)); // , eltNode);//, xsaInst);
						
						this.uiFactory.createField(
								fieldPnl, sugtnTyped,
								xsaInst);
						
					}
				}
				
				break;
			
			default:
				throw new XSAXMLInvalidException("Unkown UI Division for instance at locator: "
						+ xsaInst);
				
		}
		
		if (applyRevertPanel != null) {
			applyRevertPanel.setStyle("display: block; padding-bottom: 30px;");
			resultCont.getChildren().add(applyRevertPanel);
		}
		
		container.getChildren().add(resultCont);
		
		this.instsProccessed.add(xsaInst);
		
	}
	
	/**
	 * @return the groupPanelsMap
	 */
	public HashMap<String, HtmlPanelGroup> getGroupPanelsMap() {
		return this.groupPanelsMap;
	}
	
	/**
	 * Makes sure that the instance for which UI is being generated was not processed in an earlier step of recursion
	 */
	private boolean guardInstNotProccessedYet(XSAInstance xsaInst) {
		
		if (this.instsProccessed.contains(xsaInst)) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Skipping generation of ui for instance: "
						+ xsaInst);
			}
			return false;
		} else {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Generating ui for Instance: "
						+ xsaInst);
			}
			
			return true;
		}
	}
	
	/**
	 * Gets the panel in which the UI gerenated for the instance should be placed. This panel is placed in the correct
	 * order among panels for other instances under the same containing instance. This function takes into consideration
	 * the order of the instance in the XSA.xml and the group to which it belongs.
	 */
	private UIPanel instOrderedPanel(UIPanel container,
			int instIxInUi,
			String groupId,
			String id, String grpReadAccRole)
			throws XSAException, JDOMException {
		
		if ((groupId != null) && !groupId.isEmpty()) {
			HtmlPanelGroup grpPanel = null;
			if (this.groupPanelsMap.containsKey(groupId)) {
				grpPanel = this.groupPanelsMap.get(groupId);
			} else {
				XSAGroup subSecInst = this.xsaDoc.getGroup(groupId);
				
				HtmlPanelGroup grpPanelHolder = (HtmlPanelGroup) this.instOrderedPanel(
						container,
						this.xsaDoc.getGroup(groupId).getIxInUi(), null, id, null);
				
				grpPanel = (HtmlPanelGroup) this.uiFactory.createSubSec(
						grpPanelHolder,
						subSecInst);
				
				if (grpPanelHolder.findComponent(XSAConstants.IDCONVENTION_GRP_PREFIX + groupId) == null) {
					// The new panel was not added to the holder.. remove the holder
					boolean removed = container.getChildren().remove(grpPanelHolder);
					assert removed : "Wrong Parent to remove the panel from!!";
					
				}
				
				this.groupPanelsMap.put(groupId, grpPanel);
				
			}
			
// TODO render the group only when there are visible components under it
// if ((grpReadAccRole != null) && !grpReadAccRole.isEmpty()) {
//
// String currRoles = grpPanel.getRenderedOnUserRole();
//
// if ((currRoles == null) || currRoles.isEmpty()) {
// currRoles = grpReadAccRole;
// } else {
// currRoles += "," + grpReadAccRole;
// }
//
// grpPanel.setRenderedOnUserRole(currRoles);
//
// }
			
			return this.instOrderedPanel(grpPanel, instIxInUi, null, id, null);
		}
		
		if ((id != null) && !id.isEmpty()) {
			id = "instance-" + instIxInUi + "-" + id;
		}
		
		UIPanel result = null;
		
		int ix = instIxInUi - 1;
		
		if (ix < container.getChildCount()) {
			
			result = (UIPanel) container.getChildren().get(ix);
			
		} else {
			
			while (ix > container.getChildCount()) {
				this
						.instOrderedPanel(container, container.getChildCount() + 1, null, id, null);
			}
			IceCompsFactory compsFactory = new IceCompsFactory(new DoNothingCompsDecorator());
			result = (HtmlPanelGroup) compsFactory.panel(
					HtmlPanelGroup.COMPONENT_TYPE, id);
			
			container.getChildren().add(ix, result);
		}
		
		return result;
	}
	
	
	/**
	 * Generates a 	SugtnElementNode for a given XSAInstance
	 *
	 */
	private SugtnElementNode sugtnEltNodeForInstance(XSAInstance xsaInst)
			throws SugtnException, XSAException {
		SugtnDeclNode resultDeclNode;
		resultDeclNode = this.sugtnTreeController
				.getNodeAtSugtnPath(xsaInst.getLocator().asString());
		
		if (!(resultDeclNode instanceof SugtnElementNode)) {
			throw new XSAXMLInvalidException(
					"Locator of instance must lead to a sugtn element domElt");
		}
		
		return (SugtnElementNode) resultDeclNode;
		
	}
}
