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

package org.bibalex.sdxe.xsa.application;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UIPanel;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import javax.servlet.ServletContext;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.log4j.Logger;
import org.bibalex.icefaces.jdomtree.IDTBBean;
import org.bibalex.icefaces.jdomtree.IDTUserObject;
import org.bibalex.jsf.JSFValueGetter;
import org.bibalex.sdxe.binding.DomValueBinder;
import org.bibalex.sdxe.binding.SDXEMediatorBean;
import org.bibalex.sdxe.controller.DomTreeController;
import org.bibalex.sdxe.exception.DomBuildChangesException;
import org.bibalex.sdxe.exception.DomBuildException;
import org.bibalex.sdxe.exception.DomGeneralCorrectableException;
import org.bibalex.sdxe.exception.DomMixedBadXmlException;
import org.bibalex.sdxe.exception.DomMixedContentNotAllowedException;
import org.bibalex.sdxe.exception.DomMixedElementNotAllowedException;
import org.bibalex.sdxe.exception.DomMixedTextNotAllowedException;
import org.bibalex.sdxe.exception.SDXEException;
import org.bibalex.sdxe.exception.SugtnException;
import org.bibalex.sdxe.suggest.model.dom.SugtnAttributeNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnDeclNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnElementNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnTypeDeclaration;
import org.bibalex.sdxe.xsa.exception.XSAEnforceException;
import org.bibalex.sdxe.xsa.exception.XSAException;
import org.bibalex.sdxe.xsa.exception.XSASessionBeanException;
import org.bibalex.sdxe.xsa.model.XSAAnnotator;
import org.bibalex.sdxe.xsa.model.XSADocCache;
import org.bibalex.sdxe.xsa.model.XSADocument;
import org.bibalex.sdxe.xsa.model.XSAInstance;
import org.bibalex.sdxe.xsa.model.XSALocator;
import org.bibalex.sdxe.xsa.storage.XSAIStorage;
import org.bibalex.sdxe.xsa.uigen.icefaces.IXSASugtnCompsDecoratorFactory;
import org.bibalex.sdxe.xsa.uigen.icefaces.XSAUiBindersBean;
import org.bibalex.sdxe.xsa.uigen.icefaces.XSAUiGenBuilder;
import org.bibalex.sdxe.xsa.uigen.icefaces.XSAUiGenDomListener;
import org.bibalex.sdxe.xsa.uigen.icefaces.XSAUiGenFactoryAbstract;
import org.bibalex.uigen.icefaces.UIGenerationException;
import org.bibalex.util.FacesUtils;
import org.bibalex.util.XPathStrUtils;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.springframework.beans.factory.annotation.Required;

import com.icesoft.faces.component.ext.HtmlCommandLink;
import com.icesoft.faces.component.ext.HtmlInputTextarea;
import com.icesoft.faces.component.ext.HtmlOutputLabel;
import com.icesoft.faces.component.ext.HtmlPanelGroup;
import com.icesoft.faces.component.panelpopup.PanelPopup;
import com.icesoft.faces.context.effects.JavascriptContext;

/**
 * A class to collect the Action Listeners, so that a controller can extend or delegate to this
 * 
 * @author Younos.Naga
 * 
 */
public abstract class XSAUiActionListener extends XSARequestBackingBean {
	
	// ICEFaces is a BIG lie.. they say they support Asychronous DOM update, and when I try it, eveything breaks down..
// DUH!
// private static class BaseShiftCallable implements Callable<Void> {
// private XSAUiActionListener parentActionListener;
// private String targetPart;
// private EnumDoShiftModes mode;
// private String areaId;
//
// public BaseShiftCallable(XSAUiActionListener parentActionListener, String targetPart,
// EnumDoShiftModes mode, String areaId) {
// super();
// this.parentActionListener = parentActionListener;
// this.targetPart = targetPart;
// this.mode = mode;
// this.areaId = areaId;
// }
//
// @Override
// public Void call() throws Exception {
// this.doBaseShift(this.mode, this.targetPart, this.areaId);
// return null;
// }
//
// protected void doBaseShift(EnumDoShiftModes mode, String targetPart, String areaId)
// throws XSAEnforceException {
//
// int targetIx = -1;
// int partMax = this.parentActionListener.bindersBean.getBaseXPShiftedMaxes().get(
// targetPart);
// switch (mode) {
// case move:
// targetIx = this.parentActionListener.bindersBean.getBaseXPShiftedTargets().get(
// targetPart);
//
// if ((targetIx < 1) || (targetIx > partMax)) {
// throw new XSAEnforceException("Invalid number for the base element");
// }
//
// break;
// case create:
// targetIx = partMax + 1;
// break;
// case delete:
// if (partMax == 1) {
// throw new XSAEnforceException(
// "Cannot delete the only instance of a base element");
// }
// targetIx = this.parentActionListener.bindersBean.getbaseXPShiftedCurrs().get(
// targetPart);
// if ((targetIx >= partMax)) {
// targetIx = partMax - 1;
// }
// break;
// }
//
// try {
//
// // YA20110306 Solving the base shifters mystery
// this.parentActionListener.sdxeMediator.getDomTreeController().unregisterObserver(
// this.parentActionListener.uiGenDomListener);
//
// this.parentActionListener.doCommitToRealDom();
//
// XSALocator siteLoc = this.parentActionListener.xsaDoc.getSiteLocator();
//
// while (!this.parentActionListener.sdxeMediator.getSugtnTreeController()
// .getEltSelHistoryEmpty()) {
// this.parentActionListener.sdxeMediator.prevInSugtnTreeHistory();
// }
// this.parentActionListener.sdxeMediator.moveToSugtnPath(siteLoc.asString());
//
// DomTreeController domCont = this.parentActionListener.sdxeMediator
// .getDomTreeController();
//
// String xPath = "";
// String sugtnPath = "";
// boolean partFound = false;
//
// for (String part : this.parentActionListener.bindersBean
// .getBaseXPShiftPatternParts()) {
// if (!partFound) {
// xPath += part;
// sugtnPath += part;
//
// if (part.equals(targetPart)) {
//
// if (!mode.equals(EnumDoShiftModes.move)) {
//
// SugtnDeclNode sugtnNode = this.parentActionListener.sdxeMediator
// .getSugtnTreeController()
// .getNodeAtSugtnPath(sugtnPath);
//
// if (mode.equals(EnumDoShiftModes.create)) {
//
// domCont.addSugtnAtXPathRelToActive(XPathStrUtils
// .removeLastStep(xPath),
// sugtnNode);
//
// this.parentActionListener.sdxeMediator.commit();
//
// } else if (mode.equals(EnumDoShiftModes.delete)) {
// // This is the base shifter so it doesn't have containing instances
// // so we can call directly not through XSASDXEDriver.deleteFromDom
// domCont.deleteEltAtXPathRelToActive(xPath
// + "["
// + this.parentActionListener.bindersBean
// .getbaseXPShiftedCurrs().get(targetPart)
// + "]",
// (SugtnElementNode) sugtnNode);
//
// // Since the base shifter might have some parents that are now redundant,
// // now is a good time for cleaning empty elements
//
// XSASDXEDriver xsaSdxeDriver = new XSASDXEDriver(
// this.parentActionListener.sdxeMediator,
// this.parentActionListener.xsaDoc);
// xsaSdxeDriver.deleteEmptyElements();
//
// // No commit needed after delete
//
// }
//
// }
//
// this.parentActionListener.bindersBean.getbaseXPShiftedCurrs().put(part,
// targetIx);
//
// partFound = true;
// }
//
// xPath += "["
// + this.parentActionListener.bindersBean.getbaseXPShiftedCurrs()
// .get(part) + "]";
// } else {
// this.parentActionListener.bindersBean.getbaseXPShiftedCurrs().put(part, 1);
// }
// }
//
// // And gen
// this.parentActionListener.uiGenDomListener.clearUI();
//
// XSAInstance areaInst = this.parentActionListener.xsaDoc.getInstanceById(areaId);
//
// if (areaInst != null) {
// XSAUiGenFactoryAbstract uiGenFactory = this.parentActionListener
// .createUiGenFactory();
//
// XSAUiGenBuilder uiGenBuilder = new XSAUiGenBuilder(
// this.parentActionListener.xsaDoc, uiGenFactory,
// this.parentActionListener.sdxeMediator.getSugtnTreeController());
//
// uiGenBuilder.generateUi(
// this.parentActionListener.uiGenDomListener.getContainer(), null,
// areaInst);
//
// if (LOG.isDebugEnabled()) {
// HtmlOutputLabel debugOutText = new HtmlOutputLabel();
// debugOutText.setValue("UI for base shifters: "
// + this.parentActionListener.bindersBean.getbaseXPShiftedCurrs()
// .toString());
// this.parentActionListener.uiGenDomListener.getContainer().getChildren()
// .add(debugOutText);
// }
// } else {
// LOG.warn("Error in base shifter operation! Cannot get area instance for Area ID: "
// + areaId);
// this.parentActionListener.goToAreas(null);
// }
//
// } catch (Exception e) {
// this.parentActionListener.handleException(e);
// } finally {
// // YA20110306 Solving the base shifters mystery
// this.parentActionListener.sdxeMediator.getDomTreeController().registerObserver(
// this.parentActionListener.uiGenDomListener);
//
// }
// }
// }
	/**
	 * Used when call doBaseShift
	 */
	protected enum EnumDoShiftModes {
		move, create, delete
	}
	
	/**
	 * Used when syncing with DOM
	 * 
	 * @author Younos.Naga
	 * 
	 */
	public enum ValueTypes {
		attrsOnly, pcdataOnly, all
	}
	
// Left as an example of how to use the ICEfaces lie
// protected ExecutorService baseShiftExecutor = Executors.newSingleThreadExecutor();
	
	// /*
// * (non-Javadoc)
// *
// * @see org.bibalex.sdxe.xsa.application.IXSAUiActionListener#moveDown(javax.faces.event.ActionEvent)
// */
// public void moveDown(ActionEvent ev) {
// // Map<String, String> requestParams = FacesContext.getCurrentInstance().getExternalContext()
// // .getRequestParameterMap();
// // String sugtnPathString = requestParams.get(PARAM_SUGTNPATH);
// String sugtnPathString = (String) ev.getComponent().getAttributes().get(PARAM_SUGTNPATH);
//
// //TODO Should? -> doSyncBinderNDom(idForElt, ValueTypes.all, true);
//
// try {
// this.sdxeMediator.moveToSugtnPath(sugtnPathString);
// } catch (Exception e) {
// this.handleException(e);
// }
// }
	protected XSASessionBBean sessionBBean;
	
	protected XSAUiGenDomListener uiGenDomListener = null;
	
	private static final Logger LOG = Logger.getLogger("org.bibalex.xsa.actionListener");
	
	// TODONOT move all these to the interface
	public static String PARAM_ID_FOR_ELT = "PARAM_ID_FOR_ELT";
	
	public static String PARAM_ID_FOR_OCC = "PARAM_ID_FOR_OCC";
	public static final String METHOD_SYNCWDOM = "syncBinderNDom";
	public static final String PARAM_VALUETYPE = "valueType";
	
	public static final String PARAM_PARENTID = "parentEltId";
	public static final String PARAM_BAPPLY = "bApply";
	public static final String METHOD_MOVEDOWN = "moveDown";
	public static final String PARAM_SUGTNPATH = "sugtnPath";
	
	public static final String METHOD_PREVINDOM = "prevInDom";
	public static final String METHOD_NEXTINDOM = "nextInDom";
	
	public static final String METHOD_NEWINDOM = "newInDom";
	public static final String PARAM_XSALOCATOR = "PARAM_XSALOCATOR";
	public static final String PARAM_ANNOTATOR = "PARAM_ANNOTATOR";
	public static final String PARAM_ANNOTAT_CANCEL = "PARAM_ANNOTAT_CANCEL";
	
	protected XSAUiBindersBean bindersBean;
	
	protected SDXEMediatorBean sdxeMediator = null;
	
	protected XSADocument xsaDoc;
	public final String CONFIG_SCHEMA_FILE;// = "/Schema/enrich-wamcp.xsd";
	public final String CONFIG_ROOTELTNAME;// = "TEI";
	
// private String bindersBeanName;
	
	public final String CONFIG_NAMESPACE;// = "http://www.tei-c.org/ns/1.0";
	
	public static final String PARAM_OCCIX = "PARAM_OCCIX";
	private static final String JUMP_ROOTELT_NAME = "Occerences";
	private IDTBBean jumpXMLTreeBBean;
	
	protected XSAIStorage storage;
	
	/**
	 * Beside the schema file schema.xsd, there must be a file schema.xsd.xsa.xml
	 * 
	 * @param cONFIGSCHEMAFILE
	 *            The schema file path
	 * @param cONFIGROOTELTNAME
	 *            The name of the element to be used as a root of the XMLs
	 * @param cONFIGNAMESPACE
	 *            The namespace of the element to be used as a root of the XMLs
	 */
	public XSAUiActionListener(String cONFIGSCHEMAFILE,
			String cONFIGROOTELTNAME, String cONFIGNAMESPACE) {
		super();
		
		this.CONFIG_SCHEMA_FILE = cONFIGSCHEMAFILE;
		this.CONFIG_ROOTELTNAME = cONFIGROOTELTNAME;
		this.CONFIG_NAMESPACE = cONFIGNAMESPACE;
		
		this.xsaDoc = XSADocCache.getInstance().xsaDocForSchema(
				this.CONFIG_SCHEMA_FILE);
	}
	
	/**
	 * Bound to actions in the floating DIV for populating children of an annotator XML node
	 * It does the actual addition of the Annotator and its children to the DOM.
	 * It uses the values in the hidden fields for knowing the field in which the annotator should be added, and the
	 * selection start and selection end when the annotate command was clicked.
	 * 
	 * @param ev 
	 * 		represents the activation of a user interface component (such as a UICommand).
	 */
	public void annotate(ActionEvent ev) {
		try {
			Map<String, Object> compAttrs = ev.getComponent().getAttributes();
			String annotatOrCancel = (String) compAttrs.get(PARAM_ANNOTAT_CANCEL);
			
			if (!"CANCEL".equals(annotatOrCancel)) {
				
				String clientId = JSFValueGetter.getString(this.bindersBean.getLastActiveIdInpt());
				
				// find the component by its client id
				// this will get it by its faces id?
				// FacesContext.getCurrentInstance().getViewRoot().findComponent(targetId)
				String jsfId = clientId.substring(clientId.lastIndexOf(':') + 1);
				UIComponent targetComp = FacesUtils.findComponentInRoot(jsfId);
				
				if ((targetComp != null) && (targetComp instanceof HtmlInputTextarea)) {
					
					HtmlInputTextarea targetArea = (HtmlInputTextarea) targetComp;
					
					if (!targetArea.isDisabled()) {
						
						String annotatorName = (String) compAttrs.get(PARAM_ANNOTATOR);
						
						String locatorStr = JSFValueGetter.getString(this.bindersBean
								.getLastActiveLocatorInpt());
						
						locatorStr = locatorStr + "/" + annotatorName;
						
						SugtnElementNode sugtnElt = (SugtnElementNode) this.sdxeMediator
								.getSugtnTreeController()
								.getNodeAtSugtnPath(locatorStr);
						
						// Prepare annotation string
						
						// Will noparent always work??
						String pfx = DomTreeController.acquireNamespaceNoParent(sugtnElt)
								.getPrefix();
						if ((pfx != null) && !pfx.isEmpty()) {
							pfx += ":";
						}
						
						String startTag = "<" + pfx + sugtnElt.getLocalName();
						
						HashMap<String, Object> valMap = this.bindersBean
								.getAnnotatorAttrsValueMap();
						for (String attrQName : valMap.keySet()) {
							
							// YA20101111 value field
							if ("TEXT".equals(attrQName)) {
								continue;
							}
							// END YA20101111
							Object attrValObj = valMap.get(attrQName);
							if (attrValObj == null) {
								continue;
							}
							String attrVal = null;
							// TODO support other value classes
							if (attrValObj instanceof String[]) {
								String result = "";
								
								for (String str : (String[]) attrValObj) {
									result += str + " ";
								}
								
								if (result.length() > 0) {
									result = result.substring(0, result.length() - 1);
								}
								
								attrVal = result;
							} else {
								attrVal = attrValObj.toString();
							}
							
							if ((attrVal != null) && !attrVal.isEmpty()) {
								
								String attNsUri = attrQName.substring(attrQName.indexOf('@') + 1);
								String attLocalName = attrQName
										.substring(0, attrQName.indexOf('@'));
								
								String attrPfx = DomTreeController.acquireNamespaceNoParent(
										attNsUri).getPrefix();
								
								if ((attrPfx != null) && !attrPfx.isEmpty()) {
									attrPfx += ":";
								}
								
								startTag += " " + attrPfx + attLocalName + "='" + attrVal + "'";
							}
						}
						
						startTag += ">";
						
						String endTag = "</" + pfx + sugtnElt.getLocalName() + ">";
						
						// Annotate
						String selStartStr = JSFValueGetter.getString(this.bindersBean
								.getSelectionStartInpt());
						int selStart = ((selStartStr == null) || selStartStr.isEmpty() ? 0
								: Integer
										.parseInt(selStartStr));
						String selEndStr = JSFValueGetter.getString(this.bindersBean
								.getSelectionEndInpt());
						int selEnd = ((selEndStr == null) || selEndStr.isEmpty() ? 0 : Integer
								.parseInt(selEndStr));
						
// String origVal = JSFValueGetter.getString(targetArea);
						String origVal = (String) targetArea.getValue();
						
						String newVal = null;
						
						String before = origVal.substring(0, selStart); // (selStart > 0 ? selStart - 1 : 0));
						String selected = ""; // origVal.substring(selStart, selEnd);
						if (this.bindersBean.getAnnotatorAttrsValueMap().containsKey("TEXT")) {
							selected = (String) this.bindersBean.getAnnotatorAttrsValueMap().get(
									"TEXT");
						}
						String after = origVal.substring(selEnd); // (selEnd < origVal.length() ? selEnd + 1 : selEnd));
						
						if ((selected != null) && !selected.isEmpty()) {
							newVal = before + startTag + selected + endTag + after;
						} else {
							startTag = startTag.replace(">", " />");
							newVal = before + startTag + after;
						}
						
						targetArea.setValue(newVal);
						
						// YA20100809 Explicitly set the value in the value binder.. it is not set if no changes were
						// done in the field
						FacesContext fCtx = FacesContext.getCurrentInstance();
						ELContext elCtx = fCtx.getELContext();
						ExpressionFactory exprFactory = fCtx.getApplication()
								.getExpressionFactory();
						
						ValueExpression valueBinderEL = exprFactory.createValueExpression(elCtx,
								"#{" +
										XSAConstants.BEANNAME_BINDERS_BEAN + ".domValueBinders['"
										+ jsfId
										+ "']}",
								DomValueBinder.class);
						DomValueBinder valueBinder = (DomValueBinder) valueBinderEL
								.getValue(FacesContext.getCurrentInstance().getELContext());
						valueBinder.setValue(newVal);
						// END YA20100809
						
// This makes the focus only at this area: targetArea.requestFocus();
						JavascriptContext.focus(fCtx, clientId);
						
						int caretPos = newVal.length() - after.length();
						String jsToSetCaretPosition =
										" var pos = "
												+ caretPos
												+ ";"
												+
												" var ctrl = $('"
												+ clientId
												+ "');"
												+ " if(ctrl.setSelectionRange) "
																+ " { "
																+ " 	ctrl.focus(); "
																+ " 	ctrl.setSelectionRange(pos,pos); "
																+ " } "
																+ " else if (ctrl.createTextRange) { "
																+ " 	var range = ctrl.createTextRange(); "
																+ " 	range.collapse(true); "
																+ " 	range.moveEnd('character', pos); "
																+ " 	range.moveStart('character', pos); "
																+ " 	range.select(); "
																+ " } ";
						
						JavascriptContext.addJavascriptCall(fCtx, jsToSetCaretPosition);
					}
				}
			}
			// destroy popup
			UIComponent popup = ev.getComponent().getParent();
			while (!(popup instanceof PanelPopup)) { // TODO refactor to remove the dependency on the type
				popup = popup.getParent();
			}
			popup.getParent().getChildren().remove(popup);
			
			// remove values map
			this.bindersBean.setAnnotatorAttrsValueMap(null);
			
		} catch (Exception e) {
			this.handleException(e);
		}
	}
	
	/**
	 * Bound to the Apply button
	 * 
	 * @param ev
	 */
	public void commitToRealDom(ActionEvent ev) {
		try {
			this.doCommitToRealDom();
		} catch (Exception e) {
			this.handleException(e);
		}
		
	}
	
	/**
	 * Bound to the link for lazy creating the popup for editing attributes of the XML element corresponding to a field
	 * or a container. This is envoked the first time the edit atrributes link is clicked.
	 * 
	 * @param ev
	 */
	public void createAttrsPopup(ActionEvent ev) {
		Map<String, Object> compAttrs = ev.getComponent().getAttributes();
		
		String locatorStr = (String) compAttrs.get(PARAM_XSALOCATOR);
		
		String idForEltOccurence = (String) compAttrs.get(XSAUiActionListener.PARAM_ID_FOR_ELT);
		
		int eltOccurenceIx = Integer.parseInt((String) compAttrs.get(PARAM_OCCIX));
		try {
			XSAInstance xsaInst = this.xsaDoc.getInstanceByLocator(locatorStr);
			SugtnElementNode sugtnElt = (SugtnElementNode) this.sdxeMediator
					.getSugtnTreeController()
					.getNodeAtSugtnPath(locatorStr);
			
			XSAUiGenFactoryAbstract uiFactory = this.createUiGenFactory();
			UIComponent container = ev.getComponent().getParent();
			HtmlCommandLink newLink = uiFactory.createAttrsPopup((UIPanel) container, sugtnElt,
					xsaInst,
					idForEltOccurence, eltOccurenceIx, false);
			newLink.setImmediate(true);
			
			HtmlCommandLink oldLink = (HtmlCommandLink) ev.getComponent();
			
			ActionListener[] actLisnrArr = oldLink.getActionListeners();
			for (ActionListener actLisnt : actLisnrArr) {
				oldLink.removeActionListener(actLisnt);
			}
			
			actLisnrArr = newLink.getActionListeners();
			for (ActionListener actLisnt : actLisnrArr) {
				oldLink.addActionListener(actLisnt);
			}
			
			oldLink.getAttributes().clear();
			oldLink.getAttributes().putAll(newLink.getAttributes());
			
// int linkIx = container.getChildren().indexOf(ev.getComponent());
// container.getChildren().remove(linkIx);
// container.getChildren().add(linkIx, newLink);
			
		} catch (Exception e) {
			this.handleException(e);
		}
		
	}
	
	/**
	 * Must be ovverridden to return the adequate decorator factory
	 * 
	 * @return
	 */
	protected abstract IXSASugtnCompsDecoratorFactory createCompsDecorator();
	
	/**
	 * 
	 * @return The adequate UIGenFactory
	 */
	protected abstract XSAUiGenFactoryAbstract createUiGenFactory();
	
	/**
	 * Bound to the delete command of the base shift part
	 * 
	 * @param ev
	 */
	public void deleteFromDomBaseShifter(ActionEvent ev) {
		try {
			Map<String, Object> attrs = ev.getComponent().getAttributes();
			String targetPart = (String) attrs.get(XSAConstants.PARAM_BASESHIFT_PART);
			String areaId = (String) attrs.get(XSAConstants.PARAM_BASESHIFT_AREAID);
			
			int targetIx = (Integer) attrs.get(XSAConstants.PARAM_BASESHIFT_TARGET_IX);
			
			this.doBaseShift(EnumDoShiftModes.delete, targetPart, targetIx, areaId);
// this.baseShiftExecutor.submit(new BaseShiftCallable(this, targetPart,
// EnumDoShiftModes.delete, areaId));
		} catch (Exception e) {
			this.handleException(e);
		}
	}
	
	/**
	 * Bound to the delete command of a container
	 * 
	 * @param ev
	 */
	public void deleteFromDomContainer(ActionEvent ev) {
		try {
			
			Map<String, Object> compAttrs = ev.getComponent().getAttributes();
			
			String locatorStr = (String) compAttrs.get(PARAM_XSALOCATOR);
			String idForElt = (String) compAttrs.get(PARAM_ID_FOR_ELT);
			
			SugtnElementNode eltToDelete = (SugtnElementNode) this.sdxeMediator
					.getSugtnTreeController().getNodeAtSugtnPath(locatorStr);
			XSAInstance xsaInst = this.xsaDoc.getInstanceByLocator(locatorStr);
			String xPathAbs = xsaInst.getXPathStr(this.bindersBean
					.getIxBetBrosPath(idForElt), this.sdxeMediator.getDomTreeController());
			
			this.doSyncBinderNDom(idForElt, ValueTypes.all, true);
			
			// DOM
			XSASDXEDriver sdxeDriver = new XSASDXEDriver(this.sdxeMediator, this.xsaDoc);
			sdxeDriver.deleteFromDom(xPathAbs, eltToDelete, xsaInst, true);
			
			// UI and binders
			
			UIComponent parent = ev.getComponent();
			
			while (!this.bindersBean.getIxBetBrosMap().containsKey(parent.getId())) {
				parent = parent.getParent();
			}
			
			this.moveContainerInDom((UIPanel) parent, null, xsaInst.getLocator().asString(),
					eltToDelete, xsaInst);
			
		} catch (Exception e) {
			this.handleException(e);
		}
		
	}
	
	/**
	 * Bound to the del comm of an occ
	 * 
	 * @param ev
	 */
	public void deleteFromDomField(ActionEvent ev) {
		try {
			
			Map<String, Object> compAttrs = ev.getComponent().getAttributes();
			
			String locatorStr = (String) compAttrs.get(PARAM_XSALOCATOR);
			String idForOcc = (String) compAttrs.get(XSAUiActionListener.PARAM_ID_FOR_OCC);
			String idForElt = (String) compAttrs.get(XSAUiActionListener.PARAM_ID_FOR_ELT);
			
			SugtnDeclNode declToDelete = this.sdxeMediator
					.getSugtnTreeController().getNodeAtSugtnPath(locatorStr);
			
			XSAInstance xsaInst = this.xsaDoc.getInstanceByLocator(locatorStr);
			String xPathAbs = xsaInst.getXPathStr(this.bindersBean
					.getIxBetBrosPath(idForOcc), this.sdxeMediator.getDomTreeController());
			
			this.doSyncBinderNDom(idForElt, ValueTypes.all, true);
			
			// DOM
			XSASDXEDriver sdxeDriver = new XSASDXEDriver(this.sdxeMediator, this.xsaDoc);
			sdxeDriver.deleteFromDom(xPathAbs, declToDelete, xsaInst, true);
			
			// Will be done with UI
			// Binders
// this.bindersBean.removeFromIxBetBrosMap(idForElt, idForOcc);
//
// this.bindersBean.getDomValueBinders().remove(idForOcc);
			
			// UI
			
			UIComponent pnlFld = ev.getComponent();
			
			while (!pnlFld.getId().equals(idForElt)) { // YA DelCont:"pnl-" + idForElt))) {//idForOcc))) {
				pnlFld = pnlFld.getParent();
			}
			
			this.removeContainerChildrenBindings(pnlFld);
			UIComponent parent = pnlFld.getParent();
// while (!this.bindersBean.getIxBetBrosMap().containsKey(parent.getId())) {
// parent = parent.getParent();
// }
			
			int ixInParent = parent.getChildren().indexOf(pnlFld);
			parent.getChildren().remove(pnlFld);
			
			XSAUiGenFactoryAbstract uiFactory = this.createUiGenFactory();
			XSAUiGenBuilder uiBuilder = new XSAUiGenBuilder(this.xsaDoc, uiFactory,
					this.sdxeMediator.getSugtnTreeController());
			
			// To prevent the re-genration of group panels
			String grp = xsaInst.getGroup();
			if ((grp != null) && !grp.isEmpty()) {
				uiBuilder.getGroupPanelsMap().put(grp, (HtmlPanelGroup) parent);
			}
			
			this.doCommitToRealDom();
			uiBuilder.generateUi((UIPanel) parent, declToDelete, xsaInst);
			
			pnlFld = parent.findComponent(idForElt); // YA DelCont:"pnl-" + idForElt);
			parent.getChildren().remove(pnlFld);
			parent.getChildren().add(ixInParent, pnlFld);
			
			// Cannot just remove the field.. ids will be messed up
// if (!parent.getChildren().remove(occPanel)) {
// throw new XSAException("Cannot remove the fields occurence panel from its parent!!");
// }
			
		} catch (Exception e) {
			this.handleException(e);
		}
		
	}
	
	/**
	 * Does the actual base shift work
	 * 
	 * @param mode
	 * @param targetPart
	 * @param actionTargetIx
	 * @param areaId
	 * @throws XSAEnforceException
	 */
	protected void doBaseShift(EnumDoShiftModes mode, String targetPart, int actionTargetIx,
			String areaId)
			throws XSAEnforceException {
		
// YA 20110322 Why synchronized
// synchronized (this) {
		int moveTargetIx = -1;
		int partMax = -1;
		try {
			partMax = this.bindersBean.getBaseXPShiftedMaxes().get(targetPart);
		} catch (NullPointerException e) {
			LOG.error("Null pointer exception when accessing BaseXPShiftedMaxes. bindersBean: "
						+ this.bindersBean
						+ ", this.bindersBean.getBaseXPShiftedMaxes(): "
						+ (this.bindersBean == null ? "[Exception]" : this.bindersBean
								.getBaseXPShiftedMaxes()) + ", targetPart: " + targetPart);
			try {
				this.doGoToAreas(true);
			} catch (SDXEException e1) {
				this.handleException(e1);
			} catch (JDOMException e1) {
				this.handleException(e1);
			}
		}
		switch (mode) {
			case move:
				if (actionTargetIx != -1) {
					LOG.warn("doBaseShift doesn't use the actionTargetIx when the action is move");
				}
				moveTargetIx = this.bindersBean.getBaseXPShiftedTargets().get(targetPart);
				
				if ((moveTargetIx < 1) || (moveTargetIx > partMax)) {
					throw new XSAEnforceException("Invalid number for the base element");
				}
				
				break;
			case create:
				moveTargetIx = partMax + 1;
				// TODONE: make this reentrant by sending the targetIx explicitly
				if (actionTargetIx != moveTargetIx) {
					LOG.warn("doBaseShift want to create a part " + targetPart + " with index "
							+ actionTargetIx + " while there it should be " + moveTargetIx);
					return;
				}
				
				break;
			case delete:
				if (partMax == 1) {
					throw new XSAEnforceException(
								"Cannot delete the only instance of a base element");
				}
				moveTargetIx = this.bindersBean.getbaseXPShiftedCurrs().get(targetPart);
				
				// TODONE: make this reentrant by sending the targetIx explicitly
				if (actionTargetIx != moveTargetIx) {
					LOG.warn("doBaseShift want to delete a part " + targetPart + " with index "
							+ actionTargetIx + " while there it should be " + moveTargetIx);
					return;
				}
				
				if ((moveTargetIx >= partMax)) {
					moveTargetIx = partMax - 1;
				}
				break;
		}
		
		try {
			// YA 20110518 Save before base shift
			try {
				this.doCommitToRealDom();
			} catch (SDXEException e2) {
				throw new XSAEnforceException(e2);
			} catch (JDOMException e2) {
				throw new XSAEnforceException(e2);
			}
			
			// YA20110306 Solving the base shifters mystery
			this.sdxeMediator.getDomTreeController().unregisterObserver(this.uiGenDomListener);
			
			this.uiGenDomListener.clearUI();
			
			this.doGoToAreas(false);
			
			DomTreeController domCont = this.sdxeMediator.getDomTreeController();
			
			String xPath = "";
			String sugtnPath = "";
			boolean partFound = false;
			
			for (String part : this.bindersBean.getBaseXPShiftPatternParts()) {
				// YA20110518 New base shifter creates new deeper shifters
				
				xPath += part;
				sugtnPath += part;
				
				if (part.equals(targetPart)) {
					
					// YA20110518 New base shifter creates new deeper shifters
					
					SugtnDeclNode sugtnNode = this.sdxeMediator
										.getSugtnTreeController()
										.getNodeAtSugtnPath(sugtnPath);
// YA20110518 New base shifter creates new deeper shifters
					if (mode.equals(EnumDoShiftModes.delete)) {
						// This is the base shifter so it doesn't have containing instances
						// so we can call directly not through XSASDXEDriver.deleteFromDom
						domCont.deleteEltAtXPathRelToActive(
											xPath
													+ "["
													+ this.bindersBean.getbaseXPShiftedCurrs().get(
															targetPart)
													+ "]",
											(SugtnElementNode) sugtnNode);
						
						// Since the base shifter might have some parents that are now redundant,
						// now is a good time for cleaning empty elements
						
						XSASDXEDriver xsaSdxeDriver = new XSASDXEDriver(
											this.sdxeMediator,
											this.xsaDoc);
						xsaSdxeDriver.deleteEmptyElements();
						
						// No commit needed after delete
						
					}
					
					this.bindersBean.getbaseXPShiftedCurrs().put(part, moveTargetIx);
					
					partFound = true;
					
					// YA20110518 New base shifter creates new deeper shifters
				} else if (partFound) {
					// deeper base shift parts should be at index 1
					this.bindersBean.getbaseXPShiftedCurrs().put(part, 1);
				}
				
				// YA20110518 New base shifter creates new deeper shifters
				if (partFound && mode.equals(EnumDoShiftModes.create)) {
					// This is the target part of a deeper base shifter.. create one
					SugtnDeclNode sugtnNode = this.sdxeMediator
									.getSugtnTreeController()
									.getNodeAtSugtnPath(sugtnPath);
					
					domCont.addSugtnAtXPathRelToActive(XPathStrUtils
										.removeLastStep(xPath),
										sugtnNode);
					
					this.sdxeMediator.commit();
				}
				
				xPath += "[" + this.bindersBean.getbaseXPShiftedCurrs().get(part) + "]";
				
			}
			
			// And gen
			this.doGoIntoArea(areaId);
			
		} catch (Exception e) {
			this.handleException(e);
		} finally {
			// YA20110306 Solving the base shifters mystery
			this.sdxeMediator.getDomTreeController().registerObserver(this.uiGenDomListener);
			
		}
	}
	
	/**
	 * 
	 * @return
	 * @throws SDXEException
	 * @throws JDOMException
	 */
	protected boolean doCommitToRealDom() throws SDXEException, JDOMException {
		
		// Iterate blindly
		boolean result = this.doSyncBinderNDom("", ValueTypes.all, true);
		
		if (this.sdxeMediator.getDomTreeController().getChangesExist()) {
			this.sdxeMediator.commit();
		}
		
		return result;
		
	}
	
	/**
	 * 
	 * @param areaId
	 */
	protected void doGoIntoArea(String areaId) {
		
		// And gen
		XSAInstance areaInst;
		try {
			areaInst = this.xsaDoc.getInstanceById(areaId);
			
			if (areaInst != null) {
				XSAUiGenFactoryAbstract uiGenFactory = this.createUiGenFactory();
				
				XSAUiGenBuilder uiGenBuilder = new XSAUiGenBuilder(this.xsaDoc, uiGenFactory,
						this.sdxeMediator.getSugtnTreeController());
				
				uiGenBuilder.generateUi(this.uiGenDomListener.getContainer(), null, areaInst);
				
				if (LOG.isDebugEnabled()) {
					HtmlOutputLabel debugOutText = new HtmlOutputLabel();
					debugOutText.setValue("UI for base shifters: "
							+ this.bindersBean.getbaseXPShiftedCurrs().toString());
					this.uiGenDomListener.getContainer().getChildren().add(debugOutText);
				}
			} else {
				LOG.warn("Cannot get area instance for Area ID: "
						+ areaId);
				this.doGoToAreas(true);
			}
			
		} catch (XSASessionBeanException e) {
			
			try {
				this.doGoToAreas(true);
			} catch (Exception e1) {
				this.handleException(e1);
			}
			
		} catch (Exception e) {
			this.handleException(e);
		}
	}
	
	/**
	 * 
	 * @param clearBaseShifters
	 * @throws JDOMException
	 * @throws SDXEException
	 */
	protected void doGoToAreas(boolean clearBaseShifters) throws JDOMException, SDXEException {
		
// We always just auto commit.. that's more usable:
// this.gaurdUnsavedChanges();
		this.doCommitToRealDom();
		
		// YA 20110306 solving the base shifter mystery
		if (clearBaseShifters) {
			this.bindersBean.clearShifters();
		}
		
		XSALocator siteLoc = this.xsaDoc.getSiteLocator();
		
		while (!this.sdxeMediator.getSugtnTreeController()
				.getEltSelHistoryEmpty()) {
			this.sdxeMediator.prevInSugtnTreeHistory();
		}
		this.sdxeMediator.moveToSugtnPath(siteLoc.asString());
		
	}
	
	/**
	 * Moves between container instances
	 * 
	 * @param ev
	 * @param diff
	 * @param locatorStr
	 * @throws XSAException
	 * @throws UIGenerationException
	 * @throws SugtnException
	 * @throws JDOMException
	 */
	protected void doNextPrevInDom(ActionEvent ev, Integer diff,
			String locatorStr) throws XSAException, UIGenerationException,
			SugtnException, JDOMException {
		UIComponent parent = ev.getComponent();
		
		while (!this.bindersBean.getIxBetBrosMap().containsKey(parent.getId())) {
			parent = parent.getParent();
		}
		
		this.doSyncBinderNDom(parent.getId(), ValueTypes.all, true);
		
		SugtnElementNode sugtnElt = (SugtnElementNode) this.sdxeMediator
				.getSugtnTreeController().getNodeAtSugtnPath(locatorStr);
		XSAInstance xsaInst = this.xsaDoc.getInstanceByLocator(locatorStr);
		
		this.moveContainerInDom((UIPanel) parent, diff, locatorStr, sugtnElt, xsaInst);
	}
	
	/**
	 * Takse the XML in the DOM and serialize it in a file
	 * 
	 * @return False if something went wrong
	 * @throws IOException
	 * @throws SDXEException
	 * @throws JDOMException
	 */
	protected boolean doSave() throws IOException, SDXEException, JDOMException {
		if (!this.doCommitToRealDom()) {
			this.setHeaderMsg(
					"Unacceptable mixed values exist, revise all tabs using the verify button",
					StyledMsgTypeEnum.ERROR);
			return false; // To show the messages
		}
		
		XSAInstance instForDomCurr = this.uiGenDomListener.getInstForDomCurrent();
		XSASDXEDriver xsaSdxeDriver = new XSASDXEDriver(this.sdxeMediator, this.xsaDoc);
		
		xsaSdxeDriver.checkPresenceInInstance(instForDomCurr, false, true);
		
		String filePathAbs = this.storage.getWorkingFile().getCanonicalPath();
		this.sdxeMediator.saveRealDom(filePathAbs);
		
		// Because the empty elements are gone so we have to regenerate UI
		this.doGoToAreas(true);
		
		return true;
		
	}
	
	/**
	 * Takes the values from the fields and put them in the dom
	 * 
	 * @param parentEltId
	 * @param valueType
	 * @param bApply
	 * @return
	 */
	protected boolean doSyncBinderNDom(String parentEltId, ValueTypes valueType, boolean bApply) {
		boolean result = true;
		for (Object obj : this.bindersBean.getDomValueBinders().keySet().toArray()) {
			String key = (String) obj;
			
			if (!key.startsWith(parentEltId)) {
				continue;
				
			} else {
				
				switch (valueType) {
					case all:
						// TODO: anything??
						break;
					case attrsOnly:

						if (key.indexOf(XSAConstants.IDCONVENTION_ATTR_PREFIX) != parentEltId
								.length() + 1) {
							continue;
						}
						
						break;
					case pcdataOnly:

						if (key.indexOf(XSAConstants.IDCONVENTION_ATTR_PREFIX) != -1) {
							continue;
						}
						
						break;
					
					default:
						break;
				}
				
			}
			
			result = result && this.doSyncOneBinderWithDom(key, bApply);
			
		}
		return result;
	}
	
	/**
	 * 
	 * @param key
	 * @param bApply
	 * @return
	 */
	protected boolean doSyncOneBinderWithDom(String key, boolean bApply) {
		DomValueBinder binder = this.bindersBean.getDomValueBinders().get(key);
		
		try {
			if (binder.isValueChanged()) {
				binder.syncWithDom(bApply);
			}
			return true;
		} catch (DomBuildException e) {
			String msgStr = null;
			if (e instanceof DomMixedElementNotAllowedException) {
				
				msgStr = "The element &lt;"
						+ ((DomMixedElementNotAllowedException) e).getEltName()
						+ "&gt; is not a valid child of &lt;"
						+ ((DomMixedElementNotAllowedException) e).getSugtnParent()
								.getLocalName()
						+ "&gt;";
				
			} else if (e instanceof DomMixedContentNotAllowedException) {
				
				msgStr = "The element &lt;"
						+ ((DomMixedContentNotAllowedException) e).getNode()
								.getParentSugtnElement()
						+ "&gt; cannot have XML inside it";
				
			} else if (e instanceof DomMixedBadXmlException) {
				
				msgStr = "Bad XML: "
						+ ((DomMixedBadXmlException) e).getOriginalException().getMessage();
			} else if (e instanceof DomMixedTextNotAllowedException) {
				msgStr = "Annotator should not contain text: "
						+ ((DomMixedTextNotAllowedException) e).getParentEltName();
			} else {
				this.handleException(e);
			}
			
			if (msgStr != null) {
				FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, msgStr, "");
				
				FacesContext fCtx = FacesContext.getCurrentInstance();
				UIComponent badComp = FacesUtils.findComponentInRoot(key);
				fCtx.addMessage(badComp.getClientId(fCtx), msg);
				
			}
			return false;
		}
	}
	
	/**
	 * Just saves right now, TODO: track and check for changes since last save..
	 * 
	 * @throws DomBuildChangesException
	 * @throws SDXEException
	 */
	protected void gaurdUnsavedChanges() throws DomBuildChangesException, SDXEException {
		// This checks for uncommitted values only
// if (this.sdxeMediator.getDomTreeController().getChangesExist()) {
// throw new DomBuildChangesException(
// "Unsaved changes exist. These will be lost by proceeding. Please either Save, Rollback or Revert first.");
// }
		
		//
		// for now just always save when there is reason to doubt
		try {
			this.doSave();
		} catch (IOException e) {
			throw new SDXEException(e);
		} catch (JDOMException e) {
			throw new SDXEException(e);
		}
		
	}
	
	/**
	 * 
	 * @return
	 */
	public IDTBBean getJumpXMLTreeBBean() {
		return this.jumpXMLTreeBBean;
	}
	
	/**
	 * 
	 * @return
	 */
	public XSASessionBBean getSessionBBean() {
		return this.sessionBBean;
	}
	
	/**
	 * 
	 * @return
	 */
	public XSAIStorage getStorage() {
		return this.storage;
	}
	
	/**
	 * Bind this to a command in your UI to go back to the Areas view
	 * 
	 * @param ev
	 */
	public void goToAreas(ActionEvent ev) {
		try {
			
			this.doGoToAreas(true);
		} catch (Exception e) {
			this.handleException(e);
		}
	}
	
	/**
	 * Closes the JUMP dialogue
	 * 
	 * @param ev
	 */
	public void hideBroSelector(ActionEvent ev) {
		String idForElt = (String) ev.getComponent().getAttributes().get(
				PARAM_ID_FOR_ELT);
		UIComponent comp = ev.getComponent().getParent();
		while (!comp.getId().startsWith("PopUpJumpToBro")) {
			comp = comp.getParent();
		}
		PanelPopup popupComp = (PanelPopup) comp;
		popupComp.setRendered(false);
		popupComp.setVisible(false);
		
	}
	
	/**
	 * Call this to start editing the XML loaded in the DOMController, thus generating the UI and the binders. In WAMCP
	 * this is done by a Phase Listener that call this when a page that should have the forms is requested for the first
	 * time. This was the ugly solution that worked, after many Javascript solutions were tried and failed, and even
	 * some weird tricks that involves ICEfaces specifics.. I suggest you do the same, I tried hard!
	 * 
	 * @throws XSAException
	 */
	public void initEditSession() throws XSAException {
		
		ServletContext servletContext = (ServletContext) FacesContext
				.getCurrentInstance().getExternalContext().getContext();
		
		String schemaFilePath = servletContext
				.getRealPath(this.CONFIG_SCHEMA_FILE);
		try {
			this.sdxeMediator
					.open(this.storage.getWorkingFile().getCanonicalPath(),
							schemaFilePath);
			
			XSALocator siteLoc = this.xsaDoc.getSiteLocator();
			this.sdxeMediator.moveToSugtnPath(siteLoc.asString());
			
			this.uiGenDomListener.init();
			
			// Immitate the first event that is not always caught!
			this.uiGenDomListener
					.notifyDomMovedVertical(this.sdxeMediator
							.getCurrentSugtnElement());
			
		} catch (SDXEException e) {
			
			throw new XSAException(e);
		} catch (IOException e) {
			
			throw new XSAException(e);
		} catch (JDOMException e) {
			
			throw new XSAException(e);
		}
		
	}
	
	/**
	 * 
	 * @param parent
	 * @param diff
	 * @param locatorStr
	 * @param eltNode
	 * @param xsaInst
	 * @throws XSAException
	 * @throws UIGenerationException
	 * @throws SugtnException
	 * @throws JDOMException
	 */
	protected void moveContainerInDom(UIPanel parent, Integer diff,
			String locatorStr,
			SugtnElementNode eltNode,
			XSAInstance xsaInst)
			throws XSAException, UIGenerationException,
			SugtnException, JDOMException {
		
// No for side effects! this.doSyncBinderNDom(parent.getId(), ValueTypes.all, true);
		
		int targetIx;
		
		if (diff == null) {
			targetIx = -1;
		} else {
			targetIx = this.bindersBean.getIxBetBrosMap().get(parent.getId()) + diff;
		}
		
		this.removeContainerChildrenBindings(parent);
		
		this.bindersBean.getIxBetBrosMap().put(parent.getId(), targetIx);
		
		XSAUiGenFactoryAbstract uiFactory = this.createUiGenFactory();
		
		UIPanel grandParent = (UIPanel) parent.getParent();
		
		int ixInPanel = grandParent.getChildren().indexOf(parent);
		grandParent.getChildren().remove(parent);
		
		UIPanel parentNew = (UIPanel) uiFactory.createContainer(grandParent, eltNode, xsaInst,
				ixInPanel);
		
// Will be done inside
// grandParent.getChildren().add(ixInPanel, parentNew);
		
		XSAUiGenBuilder uiBuilder = new XSAUiGenBuilder(this.xsaDoc, uiFactory, this.sdxeMediator
				.getSugtnTreeController());
		
		try {
			this.doCommitToRealDom();
		} catch (SDXEException e) {
			this.handleException(e);
		}
		uiBuilder.generateUi(parentNew, (SugtnElementNode) eltNode,
				xsaInst);
		
	}
	
	/**
	 * 
	 * @param ev
	 */
	public void moveContainerJump(ActionEvent ev) {
		try {
			IDTUserObject userObj = (IDTUserObject) ev.getComponent().getAttributes().get(
					"IDT_PARAM_USEROBJECT");
			
			Object nodeObj = userObj.getJdtNode().getNode();
			Element nodeElt = null;
			
// YA 20101021: if the node is not of an element, go to the parent until you get the element
			while (!(nodeObj instanceof Element)) {
				DefaultMutableTreeNode parentTreeNode = (DefaultMutableTreeNode) (userObj
						.getWrapper().getParent());
				if (parentTreeNode == null) {
					return;
				}
				userObj = (IDTUserObject) parentTreeNode.getUserObject();
				if (userObj == null) {
					return;
				}
				nodeObj = userObj.getJdtNode().getNode();
			}
			
// END // YA 20101021:
			nodeElt = (Element) nodeObj;
			Element parentElt = nodeElt.getParentElement();
			if ((parentElt != null) && JUMP_ROOTELT_NAME.equals(parentElt.getName())) {
				
				String idForElt = this.sessionBBean.getJumpContainerEltId();
				
				String locatorStr = this.sessionBBean.getJumpLocatorString();
				
				XSAInstance xsaInst = this.xsaDoc
							.getInstanceByLocator(locatorStr);
				
				String ixesPathContsNField = this.bindersBean.getIxBetBrosPath(idForElt);
				
				String occXPath = xsaInst.getXPathStr(ixesPathContsNField, this.sdxeMediator
						.getDomTreeController());
				
				int occIx = XPathStrUtils.getLastIx(occXPath);
				
				int selIx = userObj.getWrapper().getParent().getIndex(userObj.getWrapper()) + 1;
				
// The whole container will be redrawn anyway this.doHideBroSelector(idForElt);
				
				this.doNextPrevInDom(ev, selIx - occIx, locatorStr);
			}
			
		} catch (Exception e) {
			this.handleException(e);
		}
		
	}
	
	public void moveInDomBaseShifter(ActionEvent ev) {
		try {
			Map<String, Object> attrs = ev.getComponent().getAttributes();
			String targetPart = (String) attrs.get(XSAConstants.PARAM_BASESHIFT_PART);
			String areaId = (String) attrs.get(XSAConstants.PARAM_BASESHIFT_AREAID);
			
			this.doBaseShift(EnumDoShiftModes.move, targetPart, -1, areaId);
			
		} catch (Exception e) {
			this.handleException(e);
		}
	}
	
	/**
	 * Bound to command generated for any area
	 * 
	 * @param ev
	 */
	public void moveIntoArea(ActionEvent ev) {
		
		try {
			// the area is still not shifted..
			this.bindersBean.clearShifters();
			
			// Clear first
			this.uiGenDomListener.clearUI();
			
			String areaId = (String) ev.getComponent().getAttributes().get(
					XSAConstants.PARAM_GOINTOAREA_AREAID);
			
			this.doGoIntoArea(areaId);
			
		} catch (Exception e) {
			this.handleException(e);
		}
		
	}
	
	/**
	 * Bound to the new command of the Base Shifter
	 * 
	 * @param ev
	 */
	public void newInDomBaseShifter(ActionEvent ev) {
		try {
			Map<String, Object> attrs = ev.getComponent().getAttributes();
			String targetPart = (String) attrs.get(XSAConstants.PARAM_BASESHIFT_PART);
			String areaId = (String) attrs.get(XSAConstants.PARAM_BASESHIFT_AREAID);
			
			int targetIx = (Integer) attrs.get(XSAConstants.PARAM_BASESHIFT_TARGET_IX);
			
			this.doBaseShift(EnumDoShiftModes.create, targetPart, targetIx, areaId);
// this.baseShiftExecutor.submit(new BaseShiftCallable(this, targetPart,
// EnumDoShiftModes.create, areaId));
		} catch (Exception e) {
			this.handleException(e);
		}
	}
	
	/**
	 * Bound to the new command of containers
	 * 
	 * @param ev
	 */
	public void newInDomContainer(ActionEvent ev) {
		
		Map<String, Object> compAttrs = ev.getComponent().getAttributes();
		
		String locatorStr = (String) compAttrs.get(PARAM_XSALOCATOR);
		String idForElt = (String) compAttrs.get(XSAUiActionListener.PARAM_ID_FOR_ELT);
		
		this.doSyncBinderNDom(idForElt, ValueTypes.all, true);
		
		try {
			XSASDXEDriver xsasdxeDriver = new XSASDXEDriver(this.sdxeMediator, this.xsaDoc);
			
			XSAInstance xsaInst = this.xsaDoc.getInstanceByLocator(locatorStr);
			SugtnElementNode sugtnElt = (SugtnElementNode) this.sdxeMediator
					.getSugtnTreeController()
					.getNodeAtSugtnPath(locatorStr);
			String ixBetBrosPath = this.bindersBean.getIxBetBrosPath(idForElt);
			String xPathAbs = xsaInst.getXPathStr(ixBetBrosPath, this.sdxeMediator
					.getDomTreeController());
			
			int targetIxBetBros = Integer.parseInt(XPathStrUtils.getLastStep(ixBetBrosPath)) + 1;
			xsasdxeDriver.newInDom(xsaInst, sugtnElt, xPathAbs, targetIxBetBros);
			// END insert between bros
			
			// Update UI
			UIComponent parent = ev.getComponent();
			String parentId = null;
			do {
				parent = parent.getParent();
				parentId = parent.getId();
			} while (!this.bindersBean.getIxBetBrosMap().containsKey(parentId));
			
			// diff is 1 because we will add next to the one we are currently at
			
			this.moveContainerInDom((UIPanel) parent, 1, locatorStr, sugtnElt,
					xsaInst);
			// END insert between bros
			
		} catch (Exception e) {
			this.handleException(e);
		}
		
	}
	
	/**
	 * Bound to add of Fields
	 * 
	 * @param ev
	 */
	public void newInDomField(ActionEvent ev) {
		Map<String, Object> compAttrs = ev.getComponent().getAttributes();
		
		String locatorStr = (String) compAttrs.get(PARAM_XSALOCATOR);
		
		String idForDecl = (String) compAttrs.get(XSAUiActionListener.PARAM_ID_FOR_ELT);
		
// No need: this.doSyncBinderNDom(idForElt, ValueTypes.all, true);
		
		try {
			XSASDXEDriver xsasdxeDriver = new XSASDXEDriver(this.sdxeMediator, this.xsaDoc);
			
			XSAInstance xsaInst = this.xsaDoc.getInstanceByLocator(locatorStr);
			
			SugtnDeclNode sugtnDecl = (SugtnDeclNode) this.sdxeMediator
					.getSugtnTreeController()
					.getNodeAtSugtnPath(locatorStr);
			String xPathAbs = xsaInst.getXPathStr(this.bindersBean
					.getIxBetBrosPath(idForDecl), this.sdxeMediator.getDomTreeController());
			
			SugtnTypeDeclaration sugtnType = null;
			int maxOccurs = 0;
			if (sugtnDecl instanceof SugtnElementNode) {
				SugtnElementNode sugtnElt = (SugtnElementNode) sugtnDecl;
				sugtnType = sugtnElt.getSimpleType();
				maxOccurs = sugtnElt.getMaxOccurs();
			} else if (sugtnDecl instanceof SugtnAttributeNode) {
				sugtnType = ((SugtnAttributeNode) sugtnDecl).getType();
				maxOccurs = 1;
			} else {
				throw new XSAException("Decl not Typed!!");
			}
			
			// YA20110216 Will not implement insert between bros for fields.. required only for containers!
			xsasdxeDriver.newInDom(xsaInst, sugtnDecl, xPathAbs, -1);
			
			// This is the last of its kind to be added to dom, so its ix is the count
			int ixBetBros = this.sdxeMediator.getDomTreeController()
					.getElementCountAtXPathRelToActive(xPathAbs);
			
			UIComponent parent = ev.getComponent();
			
			while (!idForDecl.equals(parent.getId())) {
				parent = parent.getParent();
			}
			
			parent = FacesUtils.findComponent(parent, idForDecl + "Occs");
			
			XSAUiGenFactoryAbstract uiFactory = this.createUiGenFactory();
			
			this.doCommitToRealDom();
			uiFactory.createFieldOccurence(idForDecl, ixBetBros, (UIPanel) parent, sugtnType,
					sugtnDecl, xsaInst, xsaInst.isAuthorityListGrows());
			
			// New button could be removed
			boolean removeNewButton = false;
			if (xsaInst.isRepeatable()) {
				int occurs = this.sdxeMediator.getDomTreeController()
						.getElementCountAtXPathRelToActive(xPathAbs);
// Max occurs is not that specific anymore: int maxOccurs = sugtnElt.getMaxOccurs();
				if ((maxOccurs != -1) && (occurs >= maxOccurs)) {
					removeNewButton = true;
				}
			} else {
				removeNewButton = true;
			}
			
			ev.getComponent().setRendered(!removeNewButton);
			
		} catch (Exception e) {
			this.handleException(e);
		}
		
	}
	
	/**
	 * Bound to Next of containers
	 * 
	 * @param ev
	 */
	public void nextInDom(ActionEvent ev) {
		try {
			String locatorStr = (String) ev.getComponent().getAttributes().get(PARAM_XSALOCATOR);
			
			this.doNextPrevInDom(ev, 1, locatorStr);
		} catch (Exception e) {
			this.handleException(e);
		}
	}
	
	/**
	 * Bound to the link that pops up the add annotator attribute. The values in the hiddenFields should be populated by
	 * Javascript before calling this.
	 * 
	 * @param ev
	 */
	public void prepareAnnotator(ActionEvent ev) {
		try {
			
			String clientId = JSFValueGetter.getString(this.bindersBean.getLastActiveIdInpt());
			
			// find the component by its client id
			// this will get it by its faces id? FacesContext.getCurrentInstance().getViewRoot().findComponent(targetId)
			String jsfId = clientId.substring(clientId.lastIndexOf(':') + 1);
			UIComponent targetComp = FacesUtils.findComponentInRoot(jsfId);
			
			if ((targetComp == null) || !(targetComp instanceof HtmlInputTextarea)) {
				throw new DomGeneralCorrectableException(
						"This field doesn't allow mixed content (including annotators)");
			}
			
			HtmlInputTextarea targetArea = (HtmlInputTextarea) targetComp;
			
			if (targetArea.isDisabled()) {
				throw new DomGeneralCorrectableException("This field is disabled, cannot edit it!");
			}
			
			Map<String, Object> compAttrs = ev.getComponent().getAttributes();
			
			String annotatorName = (String) compAttrs.get(PARAM_ANNOTATOR);
			
			String baseIdForAttrFields = jsfId + "ANNOTATOR" + annotatorName;
			SugtnElementNode sugtnElt;
			
			XSAAnnotator xsaInst = this.xsaDoc.getAnnotatorInstance(annotatorName);
			
			String locatorStr = JSFValueGetter.getString(this.bindersBean
					.getLastActiveLocatorInpt());
			locatorStr = locatorStr + "/" + annotatorName;
			
			try {
				sugtnElt = (SugtnElementNode) this.sdxeMediator
						.getSugtnTreeController()
						.getNodeAtSugtnPath(locatorStr);
			} catch (SugtnException e1) {
				throw new DomGeneralCorrectableException(annotatorName
						+ " is not allowed in this field according to the schema");
			}
			
			XSAUiGenFactoryAbstract uiFactory = this.createUiGenFactory();
			
			UIComponent container = this.uiGenDomListener.getToolbarParent();
			
			HashMap<String, Object> annotValsMap = new HashMap<String, Object>();
			
			this.bindersBean.setAnnotatorAttrsValueMap(annotValsMap);
			
			uiFactory.createAttrsPopup((UIPanel) container, sugtnElt,
					xsaInst, baseIdForAttrFields, -1, true);
			
			if (xsaInst.allowsText()) {
				String selStartStr = JSFValueGetter.getString(this.bindersBean
						.getSelectionStartInpt());
				int selStart = ((selStartStr == null) || selStartStr.isEmpty() ? 0 : Integer
						.parseInt(selStartStr));
				String selEndStr = JSFValueGetter.getString(this.bindersBean
						.getSelectionEndInpt());
				int selEnd = ((selEndStr == null) || selEndStr.isEmpty() ? 0 : Integer
						.parseInt(selEndStr));
				
// String origVal = JSFValueGetter.getString(targetArea);
				String origVal = (String) targetArea.getValue();
				
				String selected = origVal.substring(selStart, selEnd);
				
				annotValsMap.put("TEXT", selected);
			}
			
		} catch (Exception e) {
			this.handleException(e);
		}
	}
	
	/**
	 * Bound to prev of containers
	 * 
	 * @param ev
	 */
	public void prevInDom(ActionEvent ev) {
		try {
			String locatorStr = (String) ev.getComponent().getAttributes().get(PARAM_XSALOCATOR);
			
			this.doNextPrevInDom(ev, -1, locatorStr);
		} catch (Exception e) {
			this.handleException(e);
		}
		
	}
	
	/**
	 * 
	 * @param parent
	 * @throws XSAException
	 * @throws UIGenerationException
	 */
	protected void removeContainerChildrenBindings(UIComponent parent)
			throws XSAException,
			UIGenerationException {
		Iterator<UIComponent> allChildren = parent.getFacetsAndChildren();
		while (allChildren.hasNext()) {
			UIComponent child = allChildren.next();
			
			if (this.bindersBean.getIxBetBrosMap().containsKey(child.getId())) {
				this.bindersBean.getIxBetBrosMap().remove(child.getId());
			}
			
			if (child instanceof UIOutput) {
				// Only bounded values.. not labels.. etc
				if (this.bindersBean.getDomValueBinders().containsKey(child.getId())) {
					this.bindersBean.getDomValueBinders().remove(child.getId());
					
				}
			}
			
			if (child instanceof UIInput) {
				((UIInput) child).setSubmittedValue(null);
				((UIInput) child).setValue(null);
				((UIInput) child).setLocalValueSet(false);
				((UIInput) child).resetValue();
			}
			
			this.removeContainerChildrenBindings(child);
			
// Unsupported Operation allChildren.remove();
		}
		
		parent.getFacets().clear();
		parent.getChildren().clear();
	}
	
	/**
	 * Two Phase commit is no longer used
	 * 
	 * @param ev
	 */
	@Deprecated
	public void rollbackToRealDom(ActionEvent ev) {
		
		try {
			this.sdxeMediator.rollback();
		} catch (SDXEException e) {
			this.handleException(e);
		}
	}
	
	/**
	 * managed beans. The Required annotator fixes a stupid bug.
	 * 
	 * @param bindersBean
	 * 
	 */
	@Required
	public void setBindersBean(XSAUiBindersBean bindersBean) {
		this.bindersBean = bindersBean;
	}
	
	/**
	 * managed beans
	 * 
	 * @param jumpXMLTreeBBean
	 */
	public void setJumpXMLTreeBBean(IDTBBean jumpXMLTreeBBean) {
		this.jumpXMLTreeBBean = jumpXMLTreeBBean;
	}
	
	/**
	 * @param sdxeMediator
	 *            The managed bean
	 */
	public void setSdxeMediator(SDXEMediatorBean sdxeMediator) {
		this.sdxeMediator = sdxeMediator;
	}
	
	/**
	 * 
	 * @param sessionBBean
	 *            The managed bean
	 */
	public void setSessionBBean(XSASessionBBean sessionBBean) {
		this.sessionBBean = sessionBBean;
	}
	
	/**
	 * 
	 * @param storage
	 *            The managed bean
	 */
	public void setStorage(XSAIStorage storage) {
		this.storage = storage;
	}
	
	/**
	 * @param uiGenDomListener
	 *            The managed bean
	 */
	public void setUiGenDomListener(XSAUiGenDomListener uiGenDomListener) {
		this.uiGenDomListener = uiGenDomListener;
	}
	
	/**
	 * @param xsaDoc
	 *            The managed bean
	 */
	public void setXsaDoc(XSADocument xsaDoc) {
		this.xsaDoc = xsaDoc;
	}
	
	/**
	 * Bound to the JUMP link
	 * 
	 * @param ev
	 */
	public void showBroSelector(ActionEvent ev) {
		try {
			
			String idForElt = (String) ev.getComponent().getAttributes().get(
					PARAM_ID_FOR_ELT);
			
			String locatorStr = (String) ev.getComponent().getAttributes().get(PARAM_XSALOCATOR);
			
			XSAInstance xsaInst = this.xsaDoc.getInstanceByLocator(locatorStr);
			
			String ixesPathContsNField = this.bindersBean.getIxBetBrosPath(idForElt);
			
			String occXPath = xsaInst.getXPathStr(ixesPathContsNField, this.sdxeMediator
					.getDomTreeController());
			
			String brosXPStr = XPathStrUtils.removeLastIx(occXPath);
			
			Element rootElt = new Element(JUMP_ROOTELT_NAME);
			
			rootElt.addContent(this.sdxeMediator.getDomTreeController()
					.getElementTextListAtXPathRelToActive(brosXPStr));
			
			this.jumpXMLTreeBBean.buildModel(rootElt);
			
			this.sessionBBean.setJumpContainerEltId(idForElt);
			this.sessionBBean.setJumpLocatorString(locatorStr);
			
			String popupId = "PopUpJumpToBro-" + idForElt;
			PanelPopup popupComp = (PanelPopup) FacesUtils.findComponentInRoot(popupId);
			
			popupComp.setRendered(true);
			popupComp.setVisible(true);
		} catch (Exception e) {
			this.handleException(e);
		}
		
	}
	
	/**
	 * To show messages with a download link, use this. The link will be appended the end saying
	 * "click here to download"
	 * 
	 * @param msg
	 * @param targetPathAbs
	 * @throws IOException
	 */
	protected void showMessageWithDownloadLink(String msg, String targetPathAbs) throws IOException {
		ServletContext sltCtx = (ServletContext) FacesContext
				.getCurrentInstance()
				.getExternalContext().getContext();
		String filePathAbs = sltCtx.getRealPath("");
		
		String filePathRel = targetPathAbs.substring(filePathAbs.length());
		filePathRel = filePathRel.replace('\\', '/');
		String fileURL = sltCtx.getContextPath() + filePathRel;
		
		this.setHeaderMsg(msg + ": <a target='_blank' href='" + fileURL
				+ "'>"
				+ " Click to download! </a>", StyledMsgTypeEnum.SUCCESS);
		
	}
	
	/**
	 * Takes the values of fields that are under a certain part of the DOM, based on the IDs in the UI, and applied
	 * their values to the DOM. There are some attempts for enforcing mandatory fields here, but they are deprecated.
	 * 
	 * @param ev
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.sdxe.xsa.application.IXSAUiActionListener#syncBinderNDom(javax.faces.event.ActionEvent)
	 */
	public void syncBinderNDom(ActionEvent ev) {
		try {
			Map<String, Object> requestParams = ev.getComponent().getAttributes();
			
			ValueTypes valueType = (ValueTypes) requestParams.get(PARAM_VALUETYPE);
			boolean bApply = (Boolean) requestParams.get(PARAM_BAPPLY);
			
			String parentId = (String) requestParams
					.get(XSAConstants.PARAM_SYNC_PARENTINSTID);
			
			// first thing to do is to sync.. then we check
			// but the section id is not part of the ids for fields anymore
			// because of the no XML binding thing.. so we can't
// this.doSyncBinderNDom(parentId, valueType, bApply);
			
			XSAInstance parentInst = this.xsaDoc.getInstanceById(parentId);
			XSASDXEDriver xsaSdxeDriver = new XSASDXEDriver(this.sdxeMediator, this.xsaDoc);
			HashMap<String, HashMap<XSAInstance, List<String>>> presenceMap = null;
			try {
				presenceMap = xsaSdxeDriver
						.checkPresenceInInstance(parentInst, false, false);
				
			} finally {
// Regenerating the ui will cause regenerating the empty elements..
// // regen area's ui
// XSAInstance regenInst = parentInst;
// while (!XSAUiDivisions.area.equals(regenInst.getUiDivision())
// && !XSAUiDivisions.section.equals(regenInst.getUiDivision())) {
// regenInst = regenInst.getContainingInst();
// }
//
// String regenId = XSAUiGenFactoryAbstract.generateIdForInstance(regenInst);
//
// UIComponent regenComp = ev.getComponent().getParent();
// while (!regenId.equals(regenComp.getId())) {
// regenComp = regenComp.getParent();
// }
//
// FacesUtils.clearChildrenAndFacets(regenComp);
//
// XSAUiGenFactoryAbstract uiGenFactory = this.createUiGenFactory();
//
// XSAUiGenBuilder uiGenBuilder = new XSAUiGenBuilder(this.xsaDoc, uiGenFactory,
// this.sdxeMediator.getSugtnTreeController());
//
// uiGenBuilder.generateUi((UIPanel) regenComp, null, regenInst);
				
			}
			XSAUiGenFactoryAbstract uiGenFactory = this.createUiGenFactory();
			if (bApply && !ValueTypes.attrsOnly.equals(valueType)) {
				
				HashMap<XSAInstance, List<String>> missingInsts = presenceMap
						.get(XSASDXEDriver.MAPKEY_MISSINGLIST);
				
				boolean requiredMissing = false;
				for (XSAInstance missing : missingInsts.keySet()) {
					if (missing.isRequired() || missing.isFacsRequired()) {
						
						String missingId = uiGenFactory.generateIdForInstance(missing);
						
						UIComponent missingComp = FacesUtils.findComponentInRoot(missingId);
						String summary = missing.getMessage("en", "missing");
						
						summary = (summary != null ? summary : "Required");
						
						if (missing.isFacsRequired()) {
							summary += " with Facs";
						}
						
						String responsible = missing.getResponsible();
						if ((responsible != null) && !responsible.isEmpty()) {
							summary += "Responsible: " + responsible;
						}
						
						String contIxes = "";
						for (String ixesPath : missingInsts.get(missing)) {
							ixesPath = XPathStrUtils.removeLastStep(ixesPath);
							if (!ixesPath.isEmpty()) {
								contIxes += ixesPath + " & ";
							}
						}
						
						if (!contIxes.isEmpty()) {
							contIxes = "In containers: "
									+ contIxes.substring(0, contIxes.lastIndexOf(" & "));
						}
						
						FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
								summary,
								contIxes);
						
						FacesContext fCtx = FacesContext.getCurrentInstance();
						fCtx.addMessage(missingComp.getClientId(fCtx), msg);
						
						requiredMissing = true;
					}
					
				}
				
				String msg = null;
				if (!requiredMissing) {
					msg = "All values applied successfully";
					this.setHeaderMsg(msg, StyledMsgTypeEnum.SUCCESS);
				} else {
					msg = "Present values applied, but there are missing required values";
					this.setHeaderMsg(msg, StyledMsgTypeEnum.INSTRUCTIONS);
				}
				
			}
			
			HashMap<XSAInstance, List<String>> presentInsts = presenceMap
					.get(XSASDXEDriver.MAPKEY_PRESENTLIST);
			for (XSAInstance present : presentInsts.keySet()) {
				String eltId = uiGenFactory.generateIdForInstance(present);
				this.doSyncBinderNDom(eltId, valueType, bApply);
			}
			
		} catch (Exception e) {
			this.handleException(e);
		}
	}
	
	/**
	 * Apply for one field or component
	 * 
	 * @param ev
	 */
	public void syncBindersNDomOneElement(ActionEvent ev) {
		Map<String, Object> requestParams = ev.getComponent().getAttributes();
		
		ValueTypes valueType = (ValueTypes) requestParams.get(PARAM_VALUETYPE);
		boolean bApply = (Boolean) requestParams.get(PARAM_BAPPLY);
		String parentEltId = (String) requestParams.get(PARAM_PARENTID);
		
		this.doSyncBinderNDom(parentEltId, valueType, bApply);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.sdxe.xsa.application.IXSAUiActionListener#toggleAttrsPopupVisible(javax.faces.event.ActionEvent)
	 */
	/**
	 * Bound to Edit Attribute after it is lazy created
	 */
	public void toggleAttrsPopupVisible(ActionEvent ev) {
		String id = (String) ev.getComponent().getAttributes().get(
				XSAUiActionListener.PARAM_ID_FOR_ELT);
		Boolean oldVal = this.bindersBean.getAttrsPopupVisible().remove(id);
		boolean newVal = !oldVal;
		this.bindersBean.getAttrsPopupVisible().put(id, newVal);
		
	}
	
	/**
	 * Call upon closing, so that you can start a new init session without being returned to the current one.
	 */
	protected void uninitEditSession() {
		
		try {
			this.sdxeMediator.close();
			
			this.uiGenDomListener.uninit();
			
			this.storage.closeWorkingFile();
			
		} catch (Exception e) {
			this.handleException(e);
		}
		
	}
	
}
