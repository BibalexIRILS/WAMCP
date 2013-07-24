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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UIPanel;
import javax.faces.context.FacesContext;
import javax.faces.convert.IntegerConverter;

import org.apache.log4j.Logger;
import org.bibalex.Messages;
import org.bibalex.sdxe.binding.DomValueBinder;
import org.bibalex.sdxe.binding.SDXEMediatorBean;
import org.bibalex.sdxe.controller.DomTreeController;
import org.bibalex.sdxe.exception.SDXEException;
import org.bibalex.sdxe.suggest.model.SugtnTreeNode;
import org.bibalex.sdxe.suggest.model.dom.ISugtnTypedNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnAttributeNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnDeclNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnElementNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnMixedNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnPCDataNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnTypeDeclaration;
import org.bibalex.sdxe.suggest.uigen.icefaces.SugtnIceCompsFactory;
import org.bibalex.sdxe.xsa.application.XSAConstants;
import org.bibalex.sdxe.xsa.application.XSASDXEDriver;
import org.bibalex.sdxe.xsa.application.XSAUiActionListener;
import org.bibalex.sdxe.xsa.application.XSAUiActionListener.ValueTypes;
import org.bibalex.sdxe.xsa.binding.DomXPathRelMixedContainerBinder;
import org.bibalex.sdxe.xsa.binding.DomXPathRelToActiveBinder;
import org.bibalex.sdxe.xsa.exception.XSAException;
import org.bibalex.sdxe.xsa.model.XSAAnnotator;
import org.bibalex.sdxe.xsa.model.XSADocument;
import org.bibalex.sdxe.xsa.model.XSAGroup;
import org.bibalex.sdxe.xsa.model.XSAInstance;
import org.bibalex.sdxe.xsa.model.XSAMessagesContainer;
import org.bibalex.sdxe.xsa.model.XSAUiDivisions;
import org.bibalex.uigen.icefaces.DoNothingCompsDecorator;
import org.bibalex.uigen.icefaces.IceCompsFactory;
import org.bibalex.uigen.icefaces.UIGenerationException;
import org.bibalex.util.FacesUtils;
import org.bibalex.util.KeyValuePair;
import org.bibalex.util.XPathStrUtils;
import org.jdom.DataConversionException;
import org.jdom.JDOMException;

import com.icesoft.faces.component.ext.HtmlCommandLink;
import com.icesoft.faces.component.ext.HtmlInputText;
import com.icesoft.faces.component.ext.HtmlInputTextarea;
import com.icesoft.faces.component.ext.HtmlMessage;
import com.icesoft.faces.component.ext.HtmlOutputLabel;
import com.icesoft.faces.component.ext.HtmlOutputText;
import com.icesoft.faces.component.ext.HtmlPanelGrid;
import com.icesoft.faces.component.ext.HtmlPanelGroup;
import com.icesoft.faces.component.ext.HtmlSelectBooleanCheckbox;
import com.icesoft.faces.component.ext.HtmlSelectManyCheckbox;
import com.icesoft.faces.component.ext.HtmlSelectManyListbox;
import com.icesoft.faces.component.ext.HtmlSelectOneListbox;
import com.icesoft.faces.component.panelpopup.PanelPopup;
import com.icesoft.faces.component.paneltabset.PanelTab;
import com.icesoft.faces.component.paneltabset.PanelTabSet;
import com.icesoft.faces.component.paneltooltip.PanelTooltip;
import com.icesoft.faces.component.selectinputtext.SelectInputText;
import com.sun.facelets.Facelet;
import com.sun.facelets.FaceletFactory;
import com.sun.facelets.compiler.SAXCompiler;
import com.sun.facelets.compiler.UIInstructionsCrucial;
import com.sun.facelets.impl.DefaultFaceletFactory;
import com.sun.facelets.impl.DefaultResourceResolver;
import com.sun.facelets.impl.ResourceResolver;

/**
 * An implementation of the UI components factory, using Iceface and facelets, this class is abstract
 * to help in overriding the current decoration of the components.
 */
public abstract class XSAUiGenFactoryAbstract implements IXSAUiGenFactory {
	/**
	 * Copied from the Facelets code, to make it possible to dynamically apply facelets.
	 * This code has no modifications, but is private in the Facelet implementation.
	 */
	public static class FaceletViewHandlerEmulator {
		public final static long DEFAULT_REFRESH_PERIOD = 2;
		public final static String PARAM_REFRESH_PERIO = "facelets.REFRESH_PERIOD";
		public final static String PARAM_RESOURCE_RESOLVER = "facelets.RESOURCE_RESOLVER";
		
		public static FaceletFactory createFaceletFactory(SAXCompiler c) {
			
			// refresh period
			long refreshPeriod = DEFAULT_REFRESH_PERIOD;
			FacesContext ctx = FacesContext.getCurrentInstance();
			String userPeriod = ctx.getExternalContext().getInitParameter(
					PARAM_REFRESH_PERIO);
			
			if ((userPeriod != null) && (userPeriod.length() > 0)) {
				refreshPeriod = Long.parseLong(userPeriod);
			}
			
			// resource resolver
			ResourceResolver resolver = new DefaultResourceResolver();
			String resolverName = ctx.getExternalContext().getInitParameter(
					PARAM_RESOURCE_RESOLVER);
			if ((resolverName != null) && (resolverName.length() > 0)) {
				try {
					resolver = (ResourceResolver) Class.forName(resolverName, true,
							Thread.currentThread().getContextClassLoader())
							.newInstance();
				} catch (Exception e) {
					throw new FacesException("Error Initializing ResourceResolver["
							+ resolverName + "]", e);
				}
			}
			
			// Resource.getResourceUrl(ctx,"/")
			return new DefaultFaceletFactory(c, resolver, refreshPeriod);
		}
	}
	
	public static final String ID_DELIMITER_BTNS = "___";
	
	/**
	 * generates an id for an element's attribute
	 */
	public static String generateIdForEltAttribute(String eltId, String attributeName) {
		return eltId + XSAConstants.IDCONVENTION_XPSTEP_DELIMITER
				+ XSAConstants.IDCONVENTION_ATTR_PREFIX + attributeName + "0";
	}
	
	/**
	 * Generates an id that is similar to the locator, with _ instead of / and some other characters 
	 * replaced because there are some special characters that can't be used as a part of the id.
	 * @param locator the path of the element in the XSA
	 * @return a String that can be used as a JSF element ID
	 */
	public static String generateIdForLocatorStr(String locator) {
		String result = locator;
		result = result.replace('/', XSAConstants.IDCONVENTION_XPSTEP_DELIMITER);
		result = result.replace('@', XSAConstants.IDCONVENTION_ATTR_PREFIX);
		result = result.replace('[', XSAConstants.IDCONVENTION_XPPRED_OPEN);
		result = result.replace(']', XSAConstants.IDCONVENTION_XPPRED_CLOSE);
		result = result.replace(':', XSAConstants.IDCONVENTION_NS_PREFIX);
		return result;
	}
	
	protected IXSASugtnCompsDecoratorFactory compsDecoratorFactory = null;
	
	protected static final Logger LOG = Logger.getLogger("org.bibalex.xsa");
	
	protected DomTreeController domTarget = null;
	protected XSAUiBindersBean bindersBean = null;
	
	protected SDXEMediatorBean sdxeMediatorBean;
	
	protected XSADocument xsaDoc;
	
	
	public XSAUiGenFactoryAbstract(
			SDXEMediatorBean sdxeMediatorBean,
			XSAUiBindersBean bindersBean,
			IXSASugtnCompsDecoratorFactory compsDecoratorFactory,
			XSADocument xsaDoc) {
		this.sdxeMediatorBean = sdxeMediatorBean;
		this.domTarget = sdxeMediatorBean.getDomTreeController();
		this.bindersBean = bindersBean;
		this.compsDecoratorFactory = compsDecoratorFactory;
		this.xsaDoc = xsaDoc;
		
	}
	
	/**
	 * Adds buttons to apply changes or revert them (Apply and Revert buttons) to the Area or
	 * Section.
	 * 
	 * @param hldrIdStr
	 */
	// One of the things that took too much time but was never used, according to the 80/20 rule.
	protected UIPanel addApplyRevert(String hldrIdStr,
			String idOfParentElt,
			ValueTypes valueType,
			XSAInstance parentInst,
			boolean oneElement)
			throws UIGenerationException, XSAException {
		IceCompsFactory compsFactory = new IceCompsFactory(this.compsDecoratorFactory
				.createDecorator(parentInst));
		HtmlPanelGroup pnlAppRev = (HtmlPanelGroup) compsFactory.panel(
				HtmlPanelGroup.COMPONENT_TYPE, null);
		
// //Use styling to send to bottom of container
// container.setStyle("position:relative; ");
// pnlAppRev.setStyle("position:absolute; bottom:0;");
		
		HashMap<String, Object> attrsShared = new HashMap<String, Object>();
		
		String actionExprStr = null;
		if (oneElement) {
			attrsShared.put(XSAUiActionListener.PARAM_PARENTID, idOfParentElt);
			actionExprStr = "#{" + XSAConstants.BEANNAME_ACTION_LISTENER
					+ ".syncBindersNDomOneElement}";
			
		} else {
			attrsShared.put(XSAConstants.PARAM_SYNC_PARENTINSTID,
					parentInst.getId());
			actionExprStr = "#{" + XSAConstants.BEANNAME_ACTION_LISTENER + "."
					+ XSAUiActionListener.METHOD_SYNCWDOM + "}";
		}
		
		attrsShared.put(XSAUiActionListener.PARAM_VALUETYPE, valueType);
		
		// ///// APPLUY ////////
		
		HtmlCommandLink btnApply = (HtmlCommandLink) compsFactory.command(
				HtmlCommandLink.COMPONENT_TYPE,
				null, // "btnApply-" + idOfParentElt,
				Messages.getString("en", "XSAUiGenFactory.Verify"),
				actionExprStr);
		
		btnApply.getAttributes().putAll(attrsShared);
		btnApply.getAttributes().put(XSAUiActionListener.PARAM_BAPPLY, Boolean.TRUE);
		
		this.bindersBean.addDynCompInHolder(XSAConstants.ID_arBtnApply, btnApply);
		
		// ////// REVERT //////////
		
		// No Revert anymore because there is no 2 phase commit
// HtmlCommandLink btnRevert = (HtmlCommandLink) compsFactory.command(
// HtmlCommandLink.COMPONENT_TYPE,
// null, // "btnRevert-" + idOfParentElt,
// "Reset",
// actionExprStr);
//
// btnRevert.getAttributes().putAll(attrsShared);
// btnRevert.getAttributes().put(XSAUiActionListener.PARAM_BAPPLY, Boolean.FALSE);
//
// // container.getChildren().add(btnRevert);
// this.bindersBean.addDynCompInHolder(XSAConstants.ID_arBtnRevert, btnRevert);
// END No Revert anymore
		
		this.applyFacelet(pnlAppRev, "/templates/xsa/applyRevert.xhtml");
		
		this.bindersBean.addDynComp(hldrIdStr, pnlAppRev);
		
		return pnlAppRev;
	}
	
	/**
	 * Adds the base shifting UI to an area
	 * @param xsaInst this is the instance that points to the area
	 */
	public void addBaseShifters(XSAInstance xsaInst) throws UIGenerationException, XSAException {
		
		try {
			String pattern = xsaInst.getBaseShiftPattern();
			
			LinkedList<KeyValuePair<String, String>> brokenPattern = XSAInstance
					.breakBaseShiftPattern(pattern);
			
			// FIXMENOT: This changes session (global) variables.. a side effect
			xsaInst.populateBaseShifterMaps(this.domTarget);
			
			for (KeyValuePair<String, String> partKV : brokenPattern) {
				String part = partKV.getKey();
				
				// ///////////////////////////
				
				// Generate UI
				// clear from last iteration
				this.bindersBean.getDynComps().clear();
				
				// /////// label
				IceCompsFactory compsFactory = new IceCompsFactory(new DoNothingCompsDecorator());
				
				HtmlOutputText label = (HtmlOutputText) compsFactory.output(
						HtmlOutputText.COMPONENT_TYPE, null, partKV.getValue());
				
				this.bindersBean.addDynComp(XSAConstants.ID_lblBaseshift, label);
				
				// ////// current
				Integer currIx = this.bindersBean.getbaseXPShiftedCurrs().get(part);
				HtmlOutputText currTxt = (HtmlOutputText) compsFactory.output(
						HtmlOutputText.COMPONENT_TYPE, null, ""
								+ currIx);
				
				this.bindersBean.addDynComp(XSAConstants.ID_currentBaseshift, currTxt);
				
				// / max
				Integer maxIx = this.bindersBean.getBaseXPShiftedMaxes().get(part);
				HtmlOutputText maxTxt = (HtmlOutputText) compsFactory.output(
						HtmlOutputText.COMPONENT_TYPE, null, ""
								+ maxIx);
				
				this.bindersBean.addDynComp(XSAConstants.ID_maxBaseshift, maxTxt);
				
				// target
				
				HtmlInputText targetTxt = compsFactory.htmlInputText(null, true);
				targetTxt.setConverter(new IntegerConverter());
				ValueExpression vb = IceCompsFactory.valueExpression("#{"
						+ XSAConstants.BEANNAME_BINDERS_BEAN +
						".baseXPShiftedTargets['" + part + "']}", Integer.class);
				targetTxt.setValueExpression("value", vb);
				
				this.bindersBean.addDynComp(XSAConstants.ID_targetBaseshift, targetTxt);
				
				// //////// go
				
				HtmlCommandLink go = (HtmlCommandLink) compsFactory.command(
						HtmlCommandLink.COMPONENT_TYPE, null, "", "#{"
								+ XSAConstants.BEANNAME_ACTION_LISTENER + ".moveInDomBaseShifter}");
				go.getAttributes().put(XSAConstants.PARAM_BASESHIFT_AREAID, xsaInst.getId());
				go.getAttributes().put(XSAConstants.PARAM_BASESHIFT_PART, part);
				
				this.bindersBean.addDynComp(XSAConstants.ID_moveBaseshift, go);
				
				// //////// create
				
				HtmlCommandLink create = (HtmlCommandLink) compsFactory.command(
						HtmlCommandLink.COMPONENT_TYPE, null, "", "#{"
								+ XSAConstants.BEANNAME_ACTION_LISTENER + ".newInDomBaseShifter}");
				create.getAttributes().put(XSAConstants.PARAM_BASESHIFT_AREAID, xsaInst.getId());
				create.getAttributes().put(XSAConstants.PARAM_BASESHIFT_PART, part);
				create.getAttributes().put(XSAConstants.PARAM_BASESHIFT_TARGET_IX, maxIx + 1);
				
				this.bindersBean.addDynComp(XSAConstants.ID_createBaseshift, create);
				
				// //////// delete
				
				HtmlCommandLink deleteLnk = (HtmlCommandLink) compsFactory.command(
						HtmlCommandLink.COMPONENT_TYPE, null, "", "#{"
								+ XSAConstants.BEANNAME_ACTION_LISTENER
								+ ".deleteFromDomBaseShifter}");
				deleteLnk.getAttributes().put(XSAConstants.PARAM_BASESHIFT_AREAID, xsaInst.getId());
				deleteLnk.getAttributes().put(XSAConstants.PARAM_BASESHIFT_PART, part);
				deleteLnk.getAttributes().put(XSAConstants.PARAM_BASESHIFT_TARGET_IX, currIx);
				
				this.bindersBean.addDynComp(XSAConstants.ID_deleteBaseshift, deleteLnk);
				
				// ////////
				
				HtmlPanelGroup panel = (HtmlPanelGroup) compsFactory.panel(
						HtmlPanelGroup.COMPONENT_TYPE, null);
				try {
					this.applyFacelet(panel, "/templates/xsa/baseShifter.xhtml");
				} finally {
					this.bindersBean.getDynComps().clear();
				}
				
				this.bindersBean.getBaseXPShiftedPanels().add(panel);
				
			}
			
		} catch (JDOMException e) {
			throw new XSAException(e);
			
		}
		
	}
	
	/**
	 * This method is used to add a label to a given container
	 * @param container The UIPanel that the label will be added to
	 * @param field The UIComponent that belongs to the given container and the created label will be inserted before it 
	 * @param xsaInst contains the message to be displayed
	 * @param sugtnNode the node which represents the field that contains the message
	 * @param compsFactory this is used to create the icefaces label object
	 * @return
	 * @throws XSAException
	 */
	@Deprecated
	protected HtmlOutputLabel addLabel(UIPanel container, /* UIOutput */UIComponent field,
			XSAMessagesContainer xsaInst,
			SugtnDeclNode sugtnNode,
			IceCompsFactory compsFactory) throws XSAException {
		
		String id = field.getId();
		
		String value = this.getDisplayValue(xsaInst, sugtnNode);
		
		HtmlOutputLabel label = compsFactory.htmlOutputLabel("lbl-" + id, value, id);
		
		List<UIComponent> siblings = container.getChildren();
		
		siblings.add(siblings.indexOf(field), label);
		
		return label;
	}
	
	/**
	*/
	protected boolean anyBaseShifterhasNoInstances() {
		for (Integer max : this.bindersBean.getBaseXPShiftedMaxes().values()) {
			
			if ((max == null) || (max == 0)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Applies a facelet without a particular context, in this case it uses the current faces context
	 * as the default context and it applies the facelet to it using 
	 * {@link #applyFacelet(UIPanel, String, FacesContext) this.applyFacelet}
	 */
	public void applyFacelet(UIPanel target, String relPath) throws XSAException {
		this.applyFacelet(target, relPath, FacesContext.getCurrentInstance());
	}
	
	/**
	 * Applies a facelet within the passed context
	 * @param target The UIComponent to populate in a compositional fashion. In most cases a Facelet will be base a UIViewRoot. 
	 * @param relPath 
	 * @param ctx The current FacesContext (Should be the same as FacesContext.getInstance())
	 */
	public void applyFacelet(UIPanel target, String relPath, FacesContext ctx)
			throws XSAException {
		try {
			// Somehow the dynComps map is passed correctly only the first time
			// so I moved it to the bindingBean
// ELContext elCtx = ctx.getELContext();
// ExpressionFactory exprFactory = ctx.getApplication().getExpressionFactory();
// VariableMapper varMapper = ctx.getELContext().getVariableMapper();
// if (varMapper == null) {
// varMapper = new DefaultVariableMapper();
// ((ELContextImpl) ctx.getELContext()).setVariableMapper(varMapper);
// }
//
// ctx.getExternalContext().getRequestMap().put("dynComps", dynComps);
// ValueExpression dynCompsExpr = exprFactory.createValueExpression(elCtx, "#{dynComps}",
// dynComps.getClass());
// varMapper.setVariable("dynComps", dynCompsExpr);
			
			Facelet facelet = this.bindersBean.getFacelet(relPath);
			
			facelet.apply(ctx, target);
			
			UIInstructionsCrucial.makeUIInstructionsCrucial(target.getChildren());
			
		} catch (IOException e) {
			throw new XSAException(e);
		}
	}
	
	/**
	 * Creates a Value binding and the corresponding bean, with the correct XPath string. This method also creates the
	 * JS to set the default value onfocus.
	 */
	protected DomValueBinder bindValue(UIOutput field, SugtnDeclNode sugtnNode,
			XSAInstance xsaInst,
			int occurenceIx, boolean isList, boolean isForMixedContainer)
			throws XSAException, UIGenerationException {
		if (xsaInst instanceof XSAAnnotator) {
			throw new XSAException("bindVlaue called for annotator " + xsaInst);
		}
		
		String id = field.getId();
		
		String bindingExpr = null;
		if (isList) {
			bindingExpr = "#{" + XSAConstants.BEANNAME_BINDERS_BEAN + ".domValueBinders['" + id
					+ "'].valueList}";
			field.setValueExpression("value", IceCompsFactory
					.valueExpression(bindingExpr, List.class));
			
		} else {
			bindingExpr = "#{" + XSAConstants.BEANNAME_BINDERS_BEAN + ".domValueBinders['" + id
					+ "'].value}";
			field.setValueExpression("value", IceCompsFactory
					.valueExpression(bindingExpr, String.class));
			
		}
		
// YA 20100627.. 1) Binding with a xpath that is not exactly for one value is meaningless
// and 2) attributes cannot have an ix path component and will use this of their element
// automatically in getIxBetBrosPath if none is added for them
		
		// 2)
		if (sugtnNode instanceof SugtnElementNode) {
			
			// 1)
			if (occurenceIx <= 0) {
				throw new XSAException(
						"occurenceIx must be positive. -1 means binding to a value that dosn't exist, 0 means binding for all values that exist");
			}
			
			this.bindersBean.getIxBetBrosMap().put(id, occurenceIx);
		}
		
		String xPathAbs = xsaInst
				.getXPathStr(this.bindersBean.getIxBetBrosPath(id), this.domTarget);
		
// YA 20100808 MixedCont
		
		DomXPathRelToActiveBinder binder = null;
		
		if (isForMixedContainer) {
			binder = new DomXPathRelMixedContainerBinder(
					xPathAbs,
					this.domTarget,
					(SugtnElementNode) sugtnNode,
					xsaInst.getLocator().asString());
		} else {
			binder = new DomXPathRelToActiveBinder(
					xPathAbs,
					this.domTarget,
					sugtnNode,
					xsaInst.getLocator().asString());
		}
		// end YA20100808
		
		this.bindersBean.getDomValueBinders().put(id, binder);
		
		// Set value according to XSA and XSD
		String valueDefault = null;
		try {
			if (xsaInst.isInstForAttr()) {
				valueDefault = xsaInst.getDefaultValue();
			} else {
				if (sugtnNode instanceof SugtnAttributeNode) {
					
					valueDefault = xsaInst.getAttributeValue(sugtnNode.getqName());
					
				} else if (sugtnNode instanceof SugtnElementNode) {
					
					valueDefault = xsaInst.getDefaultValue();
					
				} else {
					throw new XSAException("Unknown domElt type");
				}
			}
		} catch (JDOMException e) {
			throw new XSAException(e);
		}
		
		if ((valueDefault == null) || valueDefault.isEmpty()) {
			
			// We process elements that are explicitly
			// stated in XSA but in the case of attributes
			// we have to be careful not to fill the XML
			// with attributes that nobody asked for
			
			if ((sugtnNode instanceof SugtnElementNode)
					|| ((sugtnNode instanceof SugtnAttributeNode)
					&& xsaInst.isInstForAttr())) {
				
				// Ok we can get it from the XSD!!
				
				valueDefault = sugtnNode.getFixedValue();
				
				if ((valueDefault == null) || valueDefault.isEmpty()) {
					valueDefault = sugtnNode.getDefaultValue();
				}
			}
		}
		
		if ((valueDefault != null) && !valueDefault.isEmpty()) {
			String valueInDom = binder.getValue();
			if (!valueDefault.equals(valueInDom)) {
				if ((valueInDom == null) || valueInDom.equals("")) {
					
					// YA 20110221 FILL IN.. the bad idea that was used for only 2 days and caused too much trouble
					// The idea was to fill in mandatory fields with a FILL IN as a reminder.. which complicated things
					// and confused catalogers.
					if ("FILL IN".equals(valueDefault)) {
						if (field instanceof HtmlInputText) {
							HtmlInputText comp = (HtmlInputText) field;
							comp.setValue(valueDefault);
							
						} else if (field instanceof HtmlInputTextarea) {
							HtmlInputTextarea comp = (HtmlInputTextarea) field;
							comp.setValue(valueDefault);
							
						} else if (field instanceof HtmlSelectBooleanCheckbox) {
							HtmlSelectBooleanCheckbox comp = (HtmlSelectBooleanCheckbox) field;
							comp.setValue(valueDefault);
							
						} else if (field instanceof HtmlSelectManyCheckbox) {
							HtmlSelectManyCheckbox comp = (HtmlSelectManyCheckbox) field;
							comp.setValue(valueDefault);
							
						} else if (field instanceof HtmlSelectManyListbox) {
							HtmlSelectManyListbox comp = (HtmlSelectManyListbox) field;
							comp.setValue(valueDefault);
							
						} else if (field instanceof HtmlSelectOneListbox) {
							HtmlSelectOneListbox comp = (HtmlSelectOneListbox) field;
							comp.setValue(valueDefault);
							
						} else if (field instanceof SelectInputText) {
							SelectInputText comp = (SelectInputText) field;
							comp.setValue(valueDefault);
							
						}
					}
					// END YA 20110221
					
					// There was no value in Dom.. set to XSA value
					
					// YA 20101010 Don't add the default value until the field is edited
// binder.setValue(valueDefault);
// TODO test on IE7, and consider adding if(value == '' || value == null)
// even though this is added for field that are newly added to the DOM
					if (field instanceof HtmlInputText) {
						HtmlInputText comp = (HtmlInputText) field;
						comp.setOnfocus(comp.getOnfocus() + "; if(!this.value) this.value = '"
									+ valueDefault + "'; ");
						
					} else if (field instanceof HtmlInputTextarea) {
						HtmlInputTextarea comp = (HtmlInputTextarea) field;
						comp.setOnfocus(comp.getOnfocus() + "; if(!this.value) this.value = '"
									+ valueDefault + "'; ");
						
					} else if (field instanceof HtmlSelectBooleanCheckbox) {
						HtmlSelectBooleanCheckbox comp = (HtmlSelectBooleanCheckbox) field;
						comp.setOnfocus(comp.getOnfocus() + "; if(!this.value) this.value = '"
									+ valueDefault + "'; ");
						
					} else if (field instanceof HtmlSelectManyCheckbox) {
						HtmlSelectManyCheckbox comp = (HtmlSelectManyCheckbox) field;
						comp.setOnfocus(comp.getOnfocus() + "; if(!this.value) this.value = '"
									+ valueDefault + "'; ");
						
					} else if (field instanceof HtmlSelectManyListbox) {
						HtmlSelectManyListbox comp = (HtmlSelectManyListbox) field;
						comp.setOnfocus(comp.getOnfocus() + "; if(!this.value) this.value = '"
									+ valueDefault + "'; ");
						
					} else if (field instanceof HtmlSelectOneListbox) {
						HtmlSelectOneListbox comp = (HtmlSelectOneListbox) field;
						comp.setOnfocus(comp.getOnfocus() + "; if(!this.value) this.value = '"
									+ valueDefault + "'; ");
						
					} else if (field instanceof SelectInputText) {
						SelectInputText comp = (SelectInputText) field;
						comp.setOnfocus(comp.getOnfocus() + "; if(!this.value) this.value = '"
									+ valueDefault + "'; ");
						
					}
					
					// End YA 20101010 Lazy add
				} else {
				}
			} // else nothing because the values are equal
		}
		
		return binder;
		
	}
	
	@Override
	public UICommand createArea(UIPanel container,
				XSAInstance areaInst)
			throws UIGenerationException, XSAException {
		// becuse sometimes somehow DynComps comes with stuff inside!!
		this.bindersBean.getDynComps().clear();
		
		IceCompsFactory compsFactory = new IceCompsFactory(this.compsDecoratorFactory
				.createDecorator(areaInst));
		String id = this.generateIdForInstance(/* eltNode, */areaInst);
		
		HtmlPanelGroup resultPanel = (HtmlPanelGroup) compsFactory.panel(
				HtmlPanelGroup.COMPONENT_TYPE, "pnl-" + id);
		
		String dispVal = this.getDisplayValue(areaInst, null);
		
		String actionEL = "#{" + XSAConstants.BEANNAME_ACTION_LISTENER + ".moveIntoArea}";
		
		HtmlCommandLink result = (HtmlCommandLink) compsFactory.command(
				HtmlCommandLink.COMPONENT_TYPE, id, dispVal,
				actionEL);
		
		// YA 20100921 Ignore the write access for area, and use only the read access
		// because what is an Area that you cannot enter good for?
		result.setEnabledOnUserRole(result.getRenderedOnUserRole());
		
		result.getAttributes().put(XSAConstants.PARAM_GOINTOAREA_AREAID,
				areaInst.getId());
		this.bindersBean.addDynCompInHolder(XSAConstants.ID_areaBtnOpen, result);
		
		String helpString = this.getHelpString(areaInst, null);
		
		HtmlOutputText helpTxt = (HtmlOutputText) compsFactory.output(
				HtmlOutputText.COMPONENT_TYPE, "txtHlp-" + id, helpString);
		
		this.bindersBean.addDynCompInHolder(XSAConstants.ID_areaTxtHelp, helpTxt);
		
		this.decorateArea(areaInst, id, compsFactory);
		
		try {
			this.applyFacelet(resultPanel, "/templates/xsa/area.xhtml");
		} finally {
			this.bindersBean.getDynComps().clear();
		}
		container.getChildren().add(resultPanel);
		
		return result;
		
	}
	
	/**
	 * Creates a popup for editing the attributes of the XML element. The same popup is used to assist in adding the
	 * annotators, because the attributes of an annotator must be edited upon its addition; afterwards, the annotator is
	 * merely text
	 */
	public HtmlCommandLink createAttrsPopup(
			UIPanel container,
			SugtnElementNode eltNode, XSAInstance xsaInst,
			String idForEltOccurence, int nonAnnotatorEltIxBetBros, boolean forAnnotator)
			throws XSAException, UIGenerationException {
		
		String title = eltNode.getLocalName() + " "
				+ Messages.getString("en", "XSAUiGenFactory.Attributes");
		
		String visibleEL = null;
		IceCompsFactory compsFactory = null;
		
		if (!forAnnotator) {
			compsFactory = new IceCompsFactory(this.compsDecoratorFactory
					.createDecorator(xsaInst));
			
			this.bindersBean.getAttrsPopupVisible().put(idForEltOccurence, Boolean.TRUE);// Boolean.FALSE);
			
			visibleEL = "#{" + XSAConstants.BEANNAME_BINDERS_BEAN + ".attrsPopupVisible['"
					+ idForEltOccurence
					+ "']}";
		} else {
			compsFactory = new IceCompsFactory(new DoNothingCompsDecorator());
			visibleEL = "#{true}";
		}
		
		PanelPopup popPanel = compsFactory.panelPopup(
				"popAttrs-" + idForEltOccurence, title, true, visibleEL);
		
		// This is because PanelPop needs its body to be a table i.e. PanelGrid
		HtmlPanelGrid panelPopBody = (HtmlPanelGrid) popPanel.getBody();
		HtmlPanelGroup panelBody = (HtmlPanelGroup) compsFactory.panel(
				HtmlPanelGroup.COMPONENT_TYPE, null);
		panelPopBody.getChildren().add(panelBody);
		
		HtmlPanelGrid panelHead = (HtmlPanelGrid) popPanel.getHeader();
		
		// YA 20101111 Annotated value in popup
		ArrayList<String> attrsPopupIds = new ArrayList<String>();
		HashMap<String, UIPanel> attrsPopupFields = new HashMap<String, UIPanel>();
		HashMap<String, UIPanel> attrsPopupLabels = new HashMap<String, UIPanel>();
		
		XSASDXEDriver xsaDriver = new XSASDXEDriver(this.sdxeMediatorBean, this.xsaDoc);
		
		if (forAnnotator) {
			String id = generateIdForEltAttribute(idForEltOccurence, "TEXT");
			
			attrsPopupIds.add(id);
			
			// // Create container ////
			HtmlPanelGroup attrPanel = (HtmlPanelGroup) compsFactory.panel(
					HtmlPanelGroup.COMPONENT_TYPE, "pnl-" + id);
			attrsPopupFields.put(id, attrPanel);
			
			if (((XSAAnnotator) xsaInst).allowsText()) {
				// // CREATE ////
				
				SugtnTypeDeclaration sugtnType = new SugtnTypeDeclaration();
				
				UIInput attrField = (UIInput) SugtnIceCompsFactory.createInput(attrPanel,
						id, sugtnType,
						compsFactory);
				
				String bindingExpr = "#{" + XSAConstants.ID_ANNOTATORVALUESMAP_FOREL + "['TEXT']}";
				
				attrField.setValueExpression("value", IceCompsFactory
						.valueExpression(bindingExpr, String.class));
				
				// Set value according to XSA and XSD
				String valueDefault = "";
				try {
					valueDefault = xsaInst.getDefaultValue();
				} catch (JDOMException e) {
					throw new XSAException(e);
				}
				
				this.bindersBean.getAnnotatorAttrsValueMap().put("TEXT", valueDefault);
				
			} else {
				attrPanel.getChildren().add(
						new IceCompsFactory(new DoNothingCompsDecorator()).output(
								HtmlOutputText.COMPONENT_TYPE, id,
								"This annotator cannot contain text"));
			}
			
			// attrField.setStyleClass("xsaTextField");
			
			// // Decorate ////
			HtmlPanelGroup desc = this.createDescrPanel(xsaInst, eltNode);
			attrsPopupLabels.put(id, desc);
			
		}
		// END YA 20101111
		
		SugtnTreeNode child = (SugtnTreeNode) eltNode.getFirstChild();
		
		// // CREATE ALL ///
		while (child != null) {
			
			SugtnAttributeNode attrNode = null;
			if (child instanceof SugtnAttributeNode) {
				attrNode = (SugtnAttributeNode) child;
			} else {
				break; // we are done
			}
			
			String id = generateIdForEltAttribute(idForEltOccurence, attrNode.getLocalName());
			
			attrsPopupIds.add(id);
			
			// // Create container ////
			HtmlPanelGroup attrPanel = (HtmlPanelGroup) compsFactory.panel(
					HtmlPanelGroup.COMPONENT_TYPE, "pnl-" + id);
			
			SugtnTypeDeclaration sugtnType = xsaDriver.overrideSugtnTypeForChildAttribute(
					attrNode,
					xsaInst);
			
			// // CREATE ////
			
			UIInput attrField = (UIInput) SugtnIceCompsFactory.createInput(attrPanel,
					id, sugtnType,
					compsFactory);
			attrsPopupFields.put(id, attrPanel);
			
			// /// BIND /////
			if (!forAnnotator) {
				this.bindValue(attrField, attrNode, xsaInst, nonAnnotatorEltIxBetBros, sugtnType
						.isListType(), false);
			} else {
				
				String bindingExpr = "#{" + XSAConstants.ID_ANNOTATORVALUESMAP_FOREL + "['"
						+ attrNode.getqName().toString()
						+ "']}";
				
				Class clazz = sugtnType.getJavaClassForBaseType();
				if (!String[].class.equals(clazz)) {
					clazz = String.class;
				} // TODO support other data classes
				
				attrField.setValueExpression("value", IceCompsFactory
						.valueExpression(bindingExpr, clazz));
				
				// Set value according to XSA and XSD
				String valueDefault = "";
				try {
					valueDefault = xsaInst.getAttributeValue(attrNode.getqName());
				} catch (JDOMException e) {
					throw new XSAException(e);
				}
				
				if (String[].class.equals(clazz)) {
					this.bindersBean.getAnnotatorAttrsValueMap().put(
							attrNode.getqName().toString(),
							new String[] { valueDefault });
					
					// TODO WAMCP specific code: refactor
					if (attrField instanceof HtmlSelectManyListbox) {
						((HtmlSelectManyListbox) attrField).setStyleClass("xsaFacsSelectMany");
					}
					
				} else {
					this.bindersBean.getAnnotatorAttrsValueMap().put(
							attrNode.getqName().toString(),
							valueDefault);
				}
				
				// attrField.setStyleClass("xsaTextField");
			}
			
			// // Decorate ////
			HtmlPanelGroup desc = this.createDescrPanel(null, attrNode);
			attrsPopupLabels.put(id, desc);
			
			child = (SugtnTreeNode) eltNode.getChildAfter(child);
		}
		
		// / DECORATE ALL ///
		
		if (!forAnnotator) {
			// apply revert
			this.addApplyRevert(XSAConstants.ID_pnlAppRevPopAttrs, idForEltOccurence,
					ValueTypes.attrsOnly, xsaInst, true);// panelBody
			
			// close button
			IceCompsFactory nothingCompsFactory = new IceCompsFactory(new DoNothingCompsDecorator());
			HtmlCommandLink btnClose = (HtmlCommandLink) nothingCompsFactory.command(
					HtmlCommandLink.COMPONENT_TYPE, "btnClose-pnl-" + idForEltOccurence, "Close",
					"#{" + XSAConstants.BEANNAME_ACTION_LISTENER + ".toggleAttrsPopupVisible}");
			btnClose.getAttributes().put(XSAUiActionListener.PARAM_ID_FOR_ELT, idForEltOccurence);
			this.bindersBean.addDynCompInHolder(XSAConstants.ID_attrPopupClose, btnClose);
		} else {
			IceCompsFactory nothingCompsFactory = new IceCompsFactory(new DoNothingCompsDecorator());
			HtmlCommandLink btnOk = (HtmlCommandLink) nothingCompsFactory.command(
					HtmlCommandLink.COMPONENT_TYPE, "btnOk-pnl-" + idForEltOccurence, "Ok",
					"#{" + XSAConstants.BEANNAME_ACTION_LISTENER + ".annotate}");
			btnOk.getAttributes().put(XSAUiActionListener.PARAM_ANNOTATOR,
					((XSAAnnotator) xsaInst).getEltLocalName());
			btnOk.getAttributes().put(XSAUiActionListener.PARAM_ANNOTAT_CANCEL,
					"OK");
			this.bindersBean.addDynCompInHolder(XSAConstants.ID_attrPopupOk, btnOk);
			
			HtmlCommandLink btnCancel = (HtmlCommandLink) nothingCompsFactory.command(
					HtmlCommandLink.COMPONENT_TYPE, "btnCancel-pnl-" + idForEltOccurence, "Cancel",
					"#{" + XSAConstants.BEANNAME_ACTION_LISTENER + ".annotate}");
			btnCancel.getAttributes().put(XSAUiActionListener.PARAM_ANNOTATOR,
					((XSAAnnotator) xsaInst).getEltLocalName());
			btnCancel.getAttributes().put(XSAUiActionListener.PARAM_ANNOTAT_CANCEL,
					"CANCEL");
			
			btnCancel.setImmediate(true);
			
			this.bindersBean.addDynCompInHolder(XSAConstants.ID_attrPopupCancel, btnCancel);
			
		}
		
		this.bindersBean.setAttrsPopupFields(attrsPopupFields);
		this.bindersBean.setAttrsPopupIds(attrsPopupIds);
		this.bindersBean.setAttrsPopupLabels(attrsPopupLabels);
		
		this.applyFacelet(panelBody, "/templates/xsa/attrsPopup.xhtml");
		
		container.getChildren().add(popPanel);
		
		if (!forAnnotator) {
			// open link
			HtmlCommandLink result = (HtmlCommandLink) compsFactory.command(
					HtmlCommandLink.COMPONENT_TYPE, "display-popAttrs-"
							+ idForEltOccurence,
					Messages.getString("en", "XSAUiGenFactory.EditAttrs"),
					"#{" + XSAConstants.BEANNAME_ACTION_LISTENER + ".toggleAttrsPopupVisible}");
			
			result.getAttributes().put(XSAUiActionListener.PARAM_ID_FOR_ELT, idForEltOccurence);
			
			result.setImmediate(true);
			
			return result;
		} else {
			return null;
		}
	}
	
	@Override
	public UIPanel createContainer(UIPanel container, SugtnElementNode eltNode,
			XSAInstance xsaInst, int ixInUI)
			throws XSAException {
		
		// becuse sometimes somehow DynComps comes with stuff inside!!
		this.bindersBean.getDynComps().clear();
		
		try {
			IceCompsFactory authCompsFactory = new IceCompsFactory(this.compsDecoratorFactory
					.createDecorator(xsaInst));
			IceCompsFactory nothingCompsFactory = new IceCompsFactory(new DoNothingCompsDecorator());
			
			String idBase = this.generateIdForInstance(/* eltNode, */xsaInst);
			HtmlMessage msg = authCompsFactory.htmlMessage(null, idBase);
			this.bindersBean.addDynCompInHolder(XSAConstants.ID_contMsg, msg);
			
			HtmlPanelGroup contDesc = this.createDescrPanel(xsaInst, eltNode);
			this.bindersBean.addDynCompInHolder(XSAConstants.ID_contDesc, contDesc);
			
			// ///////
			// Decorate!!
			
			int ixBetBros = -1;
			
			if (this.bindersBean.getIxBetBrosMap().containsKey(idBase)) {
				ixBetBros = this.bindersBean.getIxBetBrosMap().get(idBase);
			} else {
				// YA 20100627 1 --> -1 and leave ixBetBros -1 not 1 to
				// try to get the last one in XML
				this.bindersBean.getIxBetBrosMap().put(idBase, -1);
			}
			String ixesPath = this.bindersBean.getIxBetBrosPath(idBase);
			String xpStr = xsaInst.getXPathStr(ixesPath, this.domTarget);
			
			int occurs;
			occurs = this.domTarget.getElementCountAtXPathRelToActive(xpStr);
			
			// YA 20100728 Always create one
			if (occurs == 0) {
				XSASDXEDriver xsasdxeDriver = new XSASDXEDriver(this.sdxeMediatorBean, this.xsaDoc);
				
				// YA20110216 -1 because this is the first of these bros
				xsasdxeDriver.newInDom(xsaInst, eltNode, xpStr, -1);
				
				occurs = 1;
			}
			// END YA 20100728
			
			int maxOccurs = eltNode.getMaxOccurs();
			
			if (occurs > 0) {
				
				// if the ix was last.. now we know the real last
				if (ixBetBros == -1) {
					this.bindersBean.getIxBetBrosMap().put(idBase, occurs);
					ixBetBros = occurs;
				}
				
				String ixBetBrosString = ixBetBros + " / " + occurs;
				HtmlOutputText outIx = (HtmlOutputText) nothingCompsFactory.output(
						HtmlOutputText.COMPONENT_TYPE, "outIx-" + idBase, ixBetBrosString);
				
				this.bindersBean.addDynCompInHolder(XSAConstants.ID_contIx, outIx);
				
				HtmlCommandLink editAttrsLink = this.lazyAttrsPopup(idBase, xsaInst.getLocator()
						.asString(), ixBetBros, authCompsFactory);
				this.bindersBean
						.addDynComp(XSAConstants.ID_contEditAttrsLink, editAttrsLink);
				
				if (maxOccurs != 1) {
					// browsing buttons
					
					HtmlCommandLink btnPrev = (HtmlCommandLink) nothingCompsFactory.command(
							HtmlCommandLink.COMPONENT_TYPE, "btnPrev-" + idBase,
							"",
							"#{" + XSAConstants.BEANNAME_ACTION_LISTENER + ".prevInDom}");
					
					btnPrev.getAttributes().put(XSAUiActionListener.PARAM_XSALOCATOR,
							xsaInst.getLocator().asString());
					
					btnPrev.setDisabled((ixBetBros == 1) || (occurs == 1));
					
					this.bindersBean.addDynComp(XSAConstants.ID_contPrev, btnPrev);
					
					HtmlCommandLink btnNext = (HtmlCommandLink) nothingCompsFactory.command(
							HtmlCommandLink.COMPONENT_TYPE, "btnNext-" + idBase,
							"",
							"#{" + XSAConstants.BEANNAME_ACTION_LISTENER + ".nextInDom}");
					
					btnNext.getAttributes().put(XSAUiActionListener.PARAM_XSALOCATOR,
							xsaInst.getLocator().asString());
					
					btnNext.setDisabled((ixBetBros == -1) || (ixBetBros == occurs));
					
					this.bindersBean.addDynComp(XSAConstants.ID_contNext, btnNext);
					
					HtmlCommandLink btnJump = (HtmlCommandLink) nothingCompsFactory.command(
							HtmlCommandLink.COMPONENT_TYPE, "btnJump-" + idBase,
							"",
							"#{" + XSAConstants.BEANNAME_ACTION_LISTENER + ".showBroSelector}");
					
					btnJump.getAttributes().put(XSAUiActionListener.PARAM_ID_FOR_ELT,
							idBase);
					btnJump.getAttributes().put(XSAUiActionListener.PARAM_XSALOCATOR,
							xsaInst.getLocator().asString());
					
					btnJump.setDisabled(occurs < 2);
					
					this.bindersBean.addDynComp(XSAConstants.ID_contJump, btnJump);
					
				}
				
				// Delete button
				HtmlCommandLink btnDel = (HtmlCommandLink) authCompsFactory
						.command(
								HtmlCommandLink.COMPONENT_TYPE,
								"btnDel-" + idBase,
								Messages.getString("en", "XSAUiGenFactory.Delete"),
								"#{" + XSAConstants.BEANNAME_ACTION_LISTENER
										+ ".deleteFromDomContainer}");
				
				btnDel.getAttributes().put(XSAUiActionListener.PARAM_XSALOCATOR,
						xsaInst.getLocator().toString());
				btnDel.getAttributes().put(XSAUiActionListener.PARAM_ID_FOR_ELT, idBase);
				
				btnDel.setImmediate(true);
				
				this.bindersBean.addDynComp(XSAConstants.ID_contDel, btnDel);
				
			}
			
			if ((occurs == 0) || xsaInst.isRepeatable()) {
				if ((maxOccurs == -1) || (occurs < maxOccurs)) {
					// new button
					HtmlCommandLink btnNew = (HtmlCommandLink) authCompsFactory.command(
							HtmlCommandLink.COMPONENT_TYPE, "btnNew-" + idBase,
							Messages.getString("en", "XSAUiGenFactory.Add"),
							"#{" + XSAConstants.BEANNAME_ACTION_LISTENER + ".newInDomContainer}");
					btnNew.getAttributes().put(XSAUiActionListener.PARAM_XSALOCATOR,
							xsaInst.getLocator().asString());
					btnNew.getAttributes().put(XSAUiActionListener.PARAM_ID_FOR_ELT,
							idBase);
					
					this.disableBtnNewInCont(xsaInst, ixesPath, btnNew);
					
					this.bindersBean.addDynComp(XSAConstants.ID_contNew, btnNew);
					
				}
			}
			
			HtmlPanelGroup result = (HtmlPanelGroup) authCompsFactory.panel(
					HtmlPanelGroup.COMPONENT_TYPE, null);
			this.bindersBean.addDynCompInHolder(XSAConstants.ID_contFields, result);
			
			// containers which show their element's text() node
			if (!xsaInst.isNomixed() && (ixBetBros > 0)) {
				
				// TODO read xsa:instance mixedContLabel and create a field header
				
				String id = idBase + "TEXT" + ixBetBros;
				
				HtmlMessage msgtxt = authCompsFactory.htmlMessage(null, id);
				
				this.bindersBean.addDynCompInHolder(XSAConstants.ID_occMsg, msgtxt);
				
				UIInput fieldOccurence = null;
				
				SugtnTypeDeclaration sugtnType = eltNode.getSimpleType();
				sugtnType = XSASDXEDriver.overrideSugtnType(sugtnType, xsaInst);
				
				// YA 20100808 MixedCont.. cannot be large
				fieldOccurence = SugtnIceCompsFactory.createInput(result, id,
						sugtnType, authCompsFactory, false); // large);
				// END YA 20100808
				
// YA 20100729 --> requirement enforcement is now done only at workflow transitions
// if (xsaInst.isRequired()) {
// fieldOccurence.addValidator(new NotEmptyValidator());
// fieldOccurence.setRequired(true);
// fieldOccurence
// .setRequiredMessage("Either delete the container or fill its text");
// }
				
				int ixInUi = ixBetBros;
				if (ixInUi >= result.getChildCount()) {
					ixInUi = result.getChildCount() - 1;
				}
				this.bindersBean.addDynCompInHolder(XSAConstants.ID_occIpHldrId, fieldOccurence);
				
				// Bind
				
				this.bindValue(fieldOccurence, eltNode, xsaInst, ixBetBros, sugtnType.isListType(),
										// YA 20100808 MixedCont.. cannot be large
						true);
				// END YA 20100808
				
				this.decorateFieldOcc(xsaInst, eltNode, ixBetBros, id, authCompsFactory);
				
				// ////////////////////////////
				// // REVERT
				// ///////////////////////////
				
				String actionExprStr = "#{" + XSAConstants.BEANNAME_ACTION_LISTENER
						+ ".syncBindersNDomOneElement}";
				// + XSAUiActionListener.METHOD_SYNCWDOM + "}";
				HtmlCommandLink btnRevert = (HtmlCommandLink) authCompsFactory.command(
						HtmlCommandLink.COMPONENT_TYPE,
						"btnRevert-" + id,
						Messages.getString("en", "XSAUiGenFactory.Reset"),
						actionExprStr);
				
				btnRevert.getAttributes().put(XSAUiActionListener.PARAM_PARENTID, id);
				btnRevert.getAttributes().put(XSAUiActionListener.PARAM_VALUETYPE,
						ValueTypes.pcdataOnly);
				btnRevert.getAttributes().put(XSAUiActionListener.PARAM_BAPPLY, Boolean.FALSE);
				
				btnRevert.setImmediate(true);
				
				this.bindersBean.addDynComp(XSAConstants.ID_occBtnRevert, btnRevert);
				
				HtmlPanelGroup pnlFieldOcc = (HtmlPanelGroup) authCompsFactory.panel(
						HtmlPanelGroup.COMPONENT_TYPE, "pnl-" + id);
				
				this.applyFacelet(pnlFieldOcc, "/templates/xsa/fieldOcc.xhtml");
				
				result.getChildren().add(ixInUi, pnlFieldOcc);
				
			}
			
			this.decorateContainer(xsaInst, eltNode, ixBetBros, idBase, authCompsFactory);
			
			HtmlPanelGroup contHolder = (HtmlPanelGroup) authCompsFactory.panel(
					HtmlPanelGroup.COMPONENT_TYPE, idBase);
			
			FacesContext fCtx = FacesContext.getCurrentInstance();
			FacesUtils.addCtxVariable(fCtx, "CONTAINER_ELT_ID", idBase);
			
			try {
				this.applyFacelet(contHolder, "/templates/xsa/container.xhtml", fCtx);
			} finally {
				this.bindersBean.getDynComps().clear();
			}
			
			if (ixInUI == -1) {
				container.getChildren().add(contHolder);
			} else {
				container.getChildren().add(ixInUI, contHolder);
			}
			
			return result;
		} catch (JDOMException e) {
			throw new XSAException(e);
		} catch (SDXEException e) {
			throw new XSAException(e);
		} catch (UIGenerationException e) {
			throw new XSAException(e);
		}
		
	}
	
	/**
	 * This deliberately doesn't comply to authentication so that
	 * the user knows it should be there but he is not allowed to see it
	 * 
	 * @param xsaInst
	 * @param declNode
	 * @return
	 * @throws XSAException
	 */
	
	protected HtmlPanelGroup createDescrPanel(XSAMessagesContainer xsaInst, SugtnDeclNode declNode)
			throws XSAException {
		IceCompsFactory compsFactory = new IceCompsFactory(new DoNothingCompsDecorator());
		HtmlPanelGroup result = (HtmlPanelGroup) compsFactory.panel(
				HtmlPanelGroup.COMPONENT_TYPE, null);
		
		HtmlPanelGroup namePanel = (HtmlPanelGroup) compsFactory.panel(
				HtmlPanelGroup.COMPONENT_TYPE, null);
		String nameStr = this.getDisplayValue(xsaInst, declNode);
		HtmlOutputText nameTxt = (HtmlOutputText) compsFactory.output(
				HtmlOutputText.COMPONENT_TYPE, null,
				nameStr);
		namePanel.getChildren().add(nameTxt);
		
		result.getChildren().add(namePanel);
		
		PanelTooltip tooltipPanel = compsFactory.panelTooltip(null, namePanel);
		String helpStr = this.getHelpString(xsaInst, declNode);
		HtmlOutputText helpTxt = (HtmlOutputText) compsFactory.output(
				HtmlOutputText.COMPONENT_TYPE, null,
				helpStr);
		tooltipPanel.getBody().getChildren().add(helpTxt);
		
		tooltipPanel.setStyleClass("xsaToolTip");
		
		result.getChildren().add(tooltipPanel);
		
		return result;
	}
	
	@Override
	public Vector<UIOutput> createField(UIPanel container, ISugtnTypedNode sugtnTyped, // SugtnPCDataNode pcdataNode,
			XSAInstance xsaInst)
			throws XSAException {
		// becuse sometimes somehow DynComps comes with stuff inside!!
		this.bindersBean.getDynComps().clear();
		
		try {
			IceCompsFactory compsFactory = new IceCompsFactory(this.compsDecoratorFactory
					.createDecorator(xsaInst));
			SugtnElementNode eltNode = ((SugtnTreeNode) sugtnTyped).getParentSugtnElement();
			SugtnDeclNode declNode = null;
			if (sugtnTyped instanceof SugtnAttributeNode) {
				declNode = (SugtnAttributeNode) sugtnTyped;
			} else {
				declNode = eltNode;
			}
			
			String id = this.generateIdForInstance(/* eltNode, */xsaInst);
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("Creating Field with id: " + id);
			}
			
			HtmlMessage msg = compsFactory.htmlMessage(null, id);
			this.bindersBean.addDynCompInHolder(XSAConstants.ID_fldMsg, msg);
			
// String idBase = this.getIdBaseFromId(id, declNode);
			
			// Create
			SugtnTypeDeclaration sugtnType = sugtnTyped.getType();
			
			// YA 20100627
			// Having an ixes path component for a field not a field occurence is meaningless
			// we just keep a -2 to prevent the empty ixesPath exception while creating the
			// xpath for the whole field.. we never use the last ix after that
			this.bindersBean.getIxBetBrosMap().put(id, -2);
			String ixesPath = this.bindersBean.getIxBetBrosPath(id);
			String xpStr = xsaInst.getXPathStr(ixesPath, this.domTarget);
			
// END YA 20100627
			
			Vector<UIOutput> result = new Vector<UIOutput>();
			
// YA DelCont: "pnl-" + idBase);
			
			int occurs = this.domTarget.getElementCountAtXPathRelToActive(xpStr);
			
			// YA 20100728 Always create one
			if (occurs == 0) {
				XSASDXEDriver xsasdxeDriver = new XSASDXEDriver(this.sdxeMediatorBean, this.xsaDoc);
				
				// YA20110216 -1 because this is the first of these bros
				xsasdxeDriver.newInDom(xsaInst, declNode, xpStr, -1);
				
				occurs = 1;
			}
			// END YA 20100728
			
			// YA 20100627
			// Having an ixes path component for a field not a field occurence is meaningless
// And then it caused a bug .. that was certain.. why do I keep doing stupid things like this?
// //Could be inside createFieldOcc but placed here to improve performance
// sugtnType = XSASDXEDriver.overrideSugtnType(sugtnType, xsaInst);
			
			HtmlPanelGroup occsPanel = (HtmlPanelGroup) compsFactory.panel(
					HtmlPanelGroup.COMPONENT_TYPE,
					id + "Occs");
			
			for (int i = 1; i <= occurs; ++i) {
				result.add(this.createFieldOccurence(id, i, occsPanel, sugtnType, declNode,// eltNode,
						xsaInst, xsaInst.isAuthorityListGrows()));
			}
			
			this.bindersBean.addDynCompInHolder(XSAConstants.ID_fldOccsPnl, occsPanel);
			
			if ((occurs == 0) || xsaInst.isRepeatable()) {
				
				int maxOccurs = Integer.MIN_VALUE;
				if (sugtnTyped instanceof SugtnAttributeNode) {
					maxOccurs = 1;
				} else {
					maxOccurs = eltNode.getMaxOccurs();
				}
				
				if ((maxOccurs == -1) || (occurs < maxOccurs)) {
					// new button
					HtmlCommandLink btnNew = (HtmlCommandLink) compsFactory.command(
							HtmlCommandLink.COMPONENT_TYPE, "btnNew-" + id, "Add",
							"#{" + XSAConstants.BEANNAME_ACTION_LISTENER + ".newInDomField}");
					btnNew.getAttributes().put(XSAUiActionListener.PARAM_XSALOCATOR,
							xsaInst.getLocator().asString());
					btnNew.getAttributes().put(XSAUiActionListener.PARAM_ID_FOR_ELT,
							id); // Base);
					
					// TODONE: do not create fields for containers with no instance, but
					// create them for areas and any other non-repeatable ui
					this.disableBtnNewInCont(xsaInst, ixesPath, btnNew);
					
					this.bindersBean.addDynComp(XSAConstants.ID_fldBtnNew, btnNew);
					
				}
			}
			
			HtmlPanelGroup fieldDescr = this.createDescrPanel(xsaInst, declNode);// eltNode);
			
			this.bindersBean.addDynCompInHolder(XSAConstants.ID_fldDescr, fieldDescr);
			
			this.decorateField(xsaInst, declNode, id, compsFactory);
			
			HtmlPanelGroup resultPanel = (HtmlPanelGroup) compsFactory.panel(
					HtmlPanelGroup.COMPONENT_TYPE,
					id);
			
			try {
				this.applyFacelet(resultPanel, "/templates/xsa/field.xhtml");
			} finally {
				this.bindersBean.getDynComps().clear();
			}
			
			// YA20110223 Mandatory/Required Class
			if (xsaInst.isRequired()) {
				resultPanel.setStyleClass("xsaFieldRequiredBG");
				// END YA20110223
			}
			
			container.getChildren().add(resultPanel);
			
			return result;
		} catch (UIGenerationException e) {
			throw new XSAException(e);
		} catch (JDOMException e) {
			throw new XSAException(e);
		} catch (SDXEException e) {
			throw new XSAException(e);
		}
		
	}
	
	
	/**
	 * This method is used to create one field occurrence.
	 * @param idBase the base id of all the occurrences of the field
	 * @param ixBetBros the index of this field occurrence
	 * @param container field container
	 * @param sugtnType
	 * @param declNode
	 * @param xsaInst
	 * @param isAutocompleteField
	 * @return
	 * @throws UIGenerationException
	 * @throws XSAException
	 */
	public UIInput createFieldOccurence(String idBase, int ixBetBros, UIPanel container,
			SugtnTypeDeclaration sugtnType, SugtnDeclNode declNode, XSAInstance xsaInst,
			boolean isAutocompleteField)
			throws UIGenerationException, XSAException {
		// becuse sometimes somehow DynComps comes with stuff inside!!
		this.bindersBean.getDynComps().clear();
		
		IceCompsFactory compsFactory = new IceCompsFactory(this.compsDecoratorFactory
				.createDecorator(xsaInst));
		String id = idBase + ixBetBros; // "_" + i;
		
		HtmlMessage msg = compsFactory.htmlMessage(null, id);
		
		this.bindersBean.addDynCompInHolder(XSAConstants.ID_occMsg, msg);
		
		UIInput fieldOccurence = null;
		
		sugtnType = XSASDXEDriver.overrideSugtnType(sugtnType, xsaInst);
		
		if (isAutocompleteField) {
// // Create the autocomplete field allowing for the preliminary
// // null assist list
// LinkedList<String> assistList;
// try {
// assistList = xsaInst.getAuthorityList();
// } catch (JDOMException e) {
// throw new XSAException(e);
// }
//
// // Auto complete is totally differnt from all other components
// fieldOccurence = compsFactory.selectInputText(container, id,
// sugtnType, assistList);
			
			// TODO AutoComplete - providing matches and storage of new items
			throw new UnsupportedOperationException("Still TODO");
			
		} else {
			boolean large = false;
			if (declNode instanceof SugtnElementNode) {
				SugtnPCDataNode pcDataNode = ((SugtnElementNode) declNode).getPcDataNode();
				if (pcDataNode != null) {
					large = pcDataNode instanceof SugtnMixedNode;
				}
			}
			try {
				large = large && !xsaInst.isNomixed();
			} catch (DataConversionException e) {
				throw new XSAException(e);
			}
			fieldOccurence = SugtnIceCompsFactory.createInput(container, id,
					sugtnType, compsFactory, large);
// YA 20100728 Always create one.. so you can't enforce its requirement... will delete empty
// fieldOccurence.addValidator(new NotEmptyValidator());
// fieldOccurence.setRequired(true);
// fieldOccurence.setRequiredMessage("Either delete the field or fill it");
// END YA 20100728
		}
		
		int ixInUi = ixBetBros;
		if (ixInUi >= container.getChildCount()) {
			ixInUi = container.getChildCount() - 1;
		}
		this.bindersBean.addDynCompInHolder(XSAConstants.ID_occIpHldrId, fieldOccurence);
		
		// Bind
		
		this.bindValue(fieldOccurence, declNode/* eltNode */, xsaInst, ixBetBros, sugtnType
				.isListType(), false);
		
		this.decorateFieldOcc(xsaInst, declNode, ixBetBros, id, compsFactory);
		
		// Decorate
		HtmlPanelGroup pnlPopAttrs = (HtmlPanelGroup) compsFactory.panel(
				HtmlPanelGroup.COMPONENT_TYPE, null);
		
		HtmlCommandLink doPopAttrs = null;
		if (declNode instanceof SugtnElementNode) {
			doPopAttrs = this.lazyAttrsPopup(id, xsaInst.getLocator().asString(), ixBetBros,
					compsFactory);
		}
		
		this.bindersBean.addDynComp(XSAConstants.ID_occPopAttrLink, doPopAttrs);
		this.bindersBean.addDynComp(XSAConstants.ID_occPopAttrHldrId, pnlPopAttrs);
// ////////////////////////////
// // REVERT
// ///////////////////////////
		
		String actionExprStr = "#{" + XSAConstants.BEANNAME_ACTION_LISTENER
				+ ".syncBindersNDomOneElement}";
		HtmlCommandLink btnRevert = (HtmlCommandLink) compsFactory.command(
				HtmlCommandLink.COMPONENT_TYPE,
				"btnRevert-" + id,
				"Reset",
				actionExprStr);
		
		btnRevert.getAttributes().put(XSAUiActionListener.PARAM_PARENTID, id);
		btnRevert.getAttributes().put(XSAUiActionListener.PARAM_VALUETYPE, ValueTypes.pcdataOnly);
		btnRevert.getAttributes().put(XSAUiActionListener.PARAM_BAPPLY, Boolean.FALSE);
		
		btnRevert.setImmediate(true);
		
		String valChangedExprStr = null;
		
		valChangedExprStr = "#{!" + XSAConstants.BEANNAME_BINDERS_BEAN + ".domValueBinders['" + id
				+ "'].valueChanged}";
		btnRevert.setValueExpression("disabled", IceCompsFactory
				.valueExpression(valChangedExprStr, Boolean.class));
		
		this.bindersBean.addDynComp(XSAConstants.ID_occBtnRevert, btnRevert);
		
		// ////////////////////////////
		
		// TODONE delete button
		HtmlCommandLink btnDel = (HtmlCommandLink) compsFactory.command(
				HtmlCommandLink.COMPONENT_TYPE,
				"btnDel-" + id, Messages.getString("en", "XSAUiGenFactory.Delete"), "#{"
						+ XSAConstants.BEANNAME_ACTION_LISTENER
						+ ".deleteFromDomField}");
		
		btnDel.getAttributes().put(XSAUiActionListener.PARAM_XSALOCATOR,
				xsaInst.getLocator().toString());
		btnDel.getAttributes().put(XSAUiActionListener.PARAM_ID_FOR_ELT, idBase);
		btnDel.getAttributes().put(XSAUiActionListener.PARAM_ID_FOR_OCC, id);
		
		// by pass the validation of the values if we just want to delete the field
		btnDel.setImmediate(true);
		
		this.bindersBean.addDynComp(XSAConstants.ID_occBtnDelHldrId, btnDel);
		
		HtmlPanelGroup pnlFieldOcc = (HtmlPanelGroup) compsFactory.panel(
				HtmlPanelGroup.COMPONENT_TYPE, "pnl-" + id);
		try {
			this.applyFacelet(pnlFieldOcc, "/templates/xsa/fieldOcc.xhtml");
		} finally {
			this.bindersBean.getDynComps().clear();
		}
		// must be after applying the facelet because applying clears children
// pnlFieldOcc.getChildren().add(pnlPopAttrs);
		
		container.getChildren().add(ixInUi, pnlFieldOcc);
		
		return fieldOccurence;
		
	}
	
	public UIPanel createSection(PanelTabSet container,
			XSAInstance secInst) throws XSAException, UIGenerationException {
		// becuse sometimes somehow DynComps comes with stuff inside!!
		this.bindersBean.getDynComps().clear();
		
		this.addApplyRevert(XSAConstants.ID_pnlAppRevSection,
				this.generateIdForInstance(secInst),
				ValueTypes.all, secInst, false);
		
		IceCompsFactory compsFactory = new IceCompsFactory(this.compsDecoratorFactory
				.createDecorator(secInst));
		
		String id = this.generateIdForInstance(/* eltNode, */secInst);
		
		PanelTab resultCont = (PanelTab) compsFactory.panel(PanelTab.COMPONENT_TYPE, id);
		
		String label = this.getDisplayValue(secInst, null);
		resultCont.setLabel(label);
		// TODO find a way to display help for the section
		
		this.decorateSection(secInst, id, compsFactory);
		
		HtmlPanelGroup result = (HtmlPanelGroup) compsFactory.panel(
				HtmlPanelGroup.COMPONENT_TYPE, null);
		this.bindersBean.addDynComp(XSAConstants.ID_secContents, result);
		try {
			this.applyFacelet(resultCont, "templates/xsa/section.xhtml");
		} finally {
			this.bindersBean.getDynComps().clear();
		}
		
		container.getChildren().add(resultCont);
		
		return result;
	}
	
	/**
	 * This deliberately doesn't comply to authentication so that
	 * the user knows it should be there but he is not allowed to see it
	 * 
	 */
	// TODO rename to createGroup ?
	@Override
	public UIPanel createSubSec(UIPanel container, XSAGroup subSecInst) // , SugtnDeclNode sugtnNode)
			throws XSAException {
		// becuse sometimes somehow DynComps comes with stuff inside!!
		this.bindersBean.getDynComps().clear();
		
		IceCompsFactory compsFactory = new IceCompsFactory(new DoNothingCompsDecorator());
		String groupId = subSecInst.getId();
		String pnlId = XSAConstants.IDCONVENTION_GRP_PREFIX + groupId;
		
		HtmlPanelGroup subsecHolder = (HtmlPanelGroup) compsFactory.panel(
				HtmlPanelGroup.COMPONENT_TYPE, pnlId);
		
		HtmlPanelGroup result = (HtmlPanelGroup) compsFactory.panel(
				HtmlPanelGroup.COMPONENT_TYPE, pnlId + "-body"); // head.getId() + "Body");
		
		if (subSecInst.isDecorate()) {
			
			HtmlPanelGroup head = (HtmlPanelGroup) compsFactory.panel(
					HtmlPanelGroup.COMPONENT_TYPE,
					pnlId + "-head"); // Collapse");
			
			HtmlOutputText title = (HtmlOutputText) compsFactory.output(
					HtmlOutputText.COMPONENT_TYPE,
					null, this.getDisplayValue(subSecInst, null));
			
			FacesContext fCtx = FacesContext.getCurrentInstance();
			
			this.bindersBean.addDynComp(XSAConstants.ID_subsecHead, head);
			this.bindersBean.addDynCompInHolder(XSAConstants.ID_subsecBody, result);
			this.bindersBean.addDynComp(XSAConstants.ID_subsecTitle, title);
			
			try {
				
				this.decorateGroup(subSecInst, pnlId, compsFactory);
				
				this.applyFacelet(subsecHolder, "/templates/xsa/subsec.xhtml", fCtx);
			} finally {
				this.bindersBean.getDynComps().clear();
			}
			
		} else {
			subsecHolder.getChildren().add(result);
		}
		
		container.getChildren().add(subsecHolder);
		
		return result;
	}
	
	protected abstract void decorateArea(XSAInstance xsaInst, String id,
			IceCompsFactory compsFactory) throws UIGenerationException,
			XSAException;
	
	protected abstract void decorateContainer(XSAInstance xsaInst, SugtnDeclNode declNode,
			int ixBetBros, String id, IceCompsFactory compsFactory) throws UIGenerationException,
			XSAException;
	
	protected abstract void decorateField(XSAInstance xsaInst, SugtnDeclNode declNode,
			String id, IceCompsFactory compsFactory) throws UIGenerationException,
			XSAException;
	
	protected abstract void decorateFieldOcc(XSAInstance xsaInst, SugtnDeclNode declNode,
			int ixBetBros, String id, IceCompsFactory compsFactory) throws UIGenerationException,
			XSAException;
	
	protected abstract void decorateGroup(XSAGroup subSecInst, String id,
			IceCompsFactory compsFactory) throws XSAException;
	
	protected abstract void decorateSection(XSAInstance xsaInst, String id,
			IceCompsFactory compsFactory) throws UIGenerationException,
			XSAException;
	
	protected void disableBtnNewInCont(XSAInstance myInst, String ixesPath,
			HtmlCommandLink btnNew) throws SDXEException, JDOMException {
		if (this.anyBaseShifterhasNoInstances()) {
			btnNew.setDisabled(true);
			return;
		}
		XSAInstance contInstance = myInst.getContainingInst();
		if (XSAUiDivisions.container.equals(contInstance.getUiDivision())) {
			
			String contIxesPath = ixesPath;
			
			int mySteps = XPathStrUtils.getStepsCount(myInst.getLocator()
					.asString());
			int contgSteps = XPathStrUtils.getStepsCount(contInstance.getLocator()
					.asString());
			
			while (mySteps != contgSteps) {
				--mySteps;
				contIxesPath = XPathStrUtils.removeLastStep(contIxesPath);
			}
			
			String contXPath = contInstance.getXPathStr(contIxesPath, this.domTarget);
			
			int containerOccurs = this.domTarget
					.getElementCountAtXPathRelToActive(contXPath);
			
			btnNew.setDisabled(containerOccurs == 0);
		}
	}
	
	/**
	 * For UI divisons that don't have corresponding XML, if there's an ID for an instance in XSA, it is used.
	 * For UI division that have corresponding XML, an ID is generated from the locator of the XML node declaration
	 */
	public String generateIdForInstance(XSAInstance instForActive) {
		String result = null;
		try {
			switch (instForActive.getUiDivision()) {
				case area:
				case section:
					result = instForActive.getId();
					break;
				case container:
				case field:
				case site:
					try {
						
						result = generateIdForLocatorStr(instForActive
									.getBaseShiftedLocatorStr(this.domTarget));
						
					} catch (XSAException e) {
						result = "EXCEPTION";
					}
					
					break;
				default:
					result = "UNKNOWN_UI_DIV";
			}
		} catch (JDOMException e) {
			result = "EXCEPTION:" + e.getMessage();
		}
		
		return result;
	}
	
	/**
	 * returns the label of the given instance and adds an * to it if this is required.
	 */
	protected String getDisplayValue(XSAMessagesContainer xsaInst, SugtnDeclNode sugtnNode)
			throws XSAException {
		
		String value = null;
		if (xsaInst != null) {
			try {
				value = xsaInst.getLabel("en");
			} catch (JDOMException e) {
				throw new XSAException(e);
			}
		}
		
		if ((value == null) || value.isEmpty()) {
			if (sugtnNode == null) {
				value = "No SugtnNode provided (probably because this is an Area or Section) and no Lable in XSA";
			} else {
				value = sugtnNode.getLocalName();
			}
		}
		
		boolean required = false;
		
		if ((xsaInst != null) && (xsaInst instanceof XSAInstance)) {
			try {
				required = ((XSAInstance) xsaInst).isRequired();
			} catch (DataConversionException e) {
				throw new XSAException(e);
			}
		} else {
			
			if (sugtnNode instanceof ISugtnTypedNode) {
				SugtnTypeDeclaration sugtnType = ((ISugtnTypedNode) sugtnNode).getType();
				
// This would go into the first condition
// if (xsaInst != null) {
// sugtnType = XSASDXEDriver.overrideSugtnType(sugtnType, (XSAInstance) xsaInst);
// }
				
				required = sugtnType.isRequired();
			}
		}
		
		if (required) {
			value = value + " *";
		}
		
		return value;
	}
	
	/**
	 * This method is used to get the hepl message for the given instance 
	 * @param xsaInst
	 * @param declNode
	 * @return
	 * @throws XSAException
	 */
	protected String getHelpString(XSAMessagesContainer xsaInst, SugtnDeclNode declNode)
			throws XSAException {
		String helpString = null;
		if (xsaInst != null) {
			try {
				
				helpString = xsaInst.getHelp("en");
			} catch (JDOMException e) {
				throw new XSAException(e);
			}
		}
		
		if ((helpString == null) || helpString.isEmpty()) {
			if (declNode == null) {
				helpString = "No SugtnNode provided (probably because this is an area or section) and no help provided in XSA";
			} else {
				helpString = declNode.getDocumenation();
				if ((helpString == null) || helpString.isEmpty()) {
					helpString = "No help available";
				}
			}
		}
		
		return helpString;
	}
	
	protected HtmlCommandLink lazyAttrsPopup(String idForEltOccurence, String locatorStr, int
			eltOccurenceIx, IceCompsFactory compsFactory)
			throws UIGenerationException {
		HtmlCommandLink result = (HtmlCommandLink) compsFactory.command(
				HtmlCommandLink.COMPONENT_TYPE, "lazy-display-popAttrs-"
						+ idForEltOccurence,
				Messages.getString("en", "XSAUiGenFactory.EditAttrs"),
				"#{" + XSAConstants.BEANNAME_ACTION_LISTENER + ".createAttrsPopup}");
		
		result.getAttributes().put(XSAUiActionListener.PARAM_ID_FOR_ELT, idForEltOccurence);
		result.getAttributes().put(XSAUiActionListener.PARAM_XSALOCATOR, locatorStr);
		result.getAttributes().put(XSAUiActionListener.PARAM_OCCIX, "" + eltOccurenceIx);
		
		result.setImmediate(true);
		
		return result;
	}
	
}
