//   Copyright 2013 Bibliotheca Alexandrina, Wellcome Trust Library, Wellcome Trust Library
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

package org.bibalex.wamcp.uigen;

import javax.faces.component.UISelectItem;

import org.bibalex.Messages;
import org.bibalex.sdxe.binding.DomValueBinder;
import org.bibalex.sdxe.binding.SDXEMediatorBean;
import org.bibalex.sdxe.exception.DomBuildChangesException;
import org.bibalex.sdxe.suggest.model.dom.SugtnAttributeNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnDeclNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnDeclQName;
import org.bibalex.sdxe.suggest.model.dom.SugtnElementNode;
import org.bibalex.sdxe.suggest.uigen.icefaces.SugtnIceCompsFactory;
import org.bibalex.sdxe.xsa.application.XSAConstants;
import org.bibalex.sdxe.xsa.application.XSAUiActionListener;
import org.bibalex.sdxe.xsa.exception.XSAException;
import org.bibalex.sdxe.xsa.model.XSADocument;
import org.bibalex.sdxe.xsa.model.XSAGroup;
import org.bibalex.sdxe.xsa.model.XSAInstance;
import org.bibalex.sdxe.xsa.uigen.icefaces.IXSASugtnCompsDecoratorFactory;
import org.bibalex.sdxe.xsa.uigen.icefaces.XSAUiBindersBean;
import org.bibalex.sdxe.xsa.uigen.icefaces.XSAUiGenFactoryAbstract;
import org.bibalex.uigen.icefaces.DoNothingCompsDecorator;
import org.bibalex.uigen.icefaces.IceCompsFactory;
import org.bibalex.uigen.icefaces.UIGenerationException;
import org.jdom.DataConversionException;
import org.jdom.Document;

import com.icesoft.faces.component.ext.HtmlCommandLink;
import com.icesoft.faces.component.ext.HtmlMessage;
import com.icesoft.faces.component.ext.HtmlSelectOneListbox;

public class WAMCPUiGenFactory extends XSAUiGenFactoryAbstract {
	
	private enum FacsRefRecepient {
		FieldOcc, Container
	}
	
	public WAMCPUiGenFactory(SDXEMediatorBean sdxeMediatorBean, XSAUiBindersBean bindersBean,
			IXSASugtnCompsDecoratorFactory compsDecoratorFactory, XSADocument xsaDoc) {
		super(sdxeMediatorBean, bindersBean, compsDecoratorFactory, xsaDoc);
		
	};
	
	protected void createFacsRefControls(XSAInstance xsaInst, SugtnDeclNode declNode,
			int ixBetBros, String id, IceCompsFactory compsFactory, FacsRefRecepient eltUi)
			throws UIGenerationException,
			XSAException {
		if (!(declNode instanceof SugtnElementNode)) {
			return;
		}
		String idsPfx = null;
		switch (eltUi) {
			case Container:
				idsPfx = "Cnt";
				break;
			case FieldOcc:
				idsPfx = "FOc";
				break;
			
		}
		IceCompsFactory donothingCompsFactory = new IceCompsFactory(
				new DoNothingCompsDecorator());
		
		SugtnAttributeNode facsAttr = ((SugtnElementNode) declNode)
				.getChildAttribute(new SugtnDeclQName("", XSAConstants.WAMCP_FACS_ATTR_NAME)); //$NON-NLS-1$
		
		// IMPORTANT: Keep the UNBOUNDED at the end of the ID
		String idInpFacs = generateIdForEltAttribute(id, XSAConstants.WAMCP_FACS_ATTR_NAME)
				+ "UNBOUNDED"; //$NON-NLS-1$
		
		HtmlSelectOneListbox inpFacs = donothingCompsFactory.htmlSelectOneListbox(idInpFacs,
				SugtnIceCompsFactory.USE_PARTIAL_SUBMIT);
		
// YA 20100729 --> requirement enforcement is now manual (in XSASDXEDriver)
// // This didn't work
// inpFacs.setRequired(xsaInst.isFacsRequired());
// // if(!inpFacs.isRequired()){
// // UISelectItem blankOption = IceCompsFactory.selectItem(null);
// // inpFacs.getChildren().add(blankOption);
// // }
// inpFacs.setRequiredMessage("This field must have a facsimile referred");
		
		inpFacs.setStyleClass("xsaFacsSelectOne" + idsPfx); //$NON-NLS-1$
		
		inpFacs.setTabindex("-1"); //$NON-NLS-1$
		
		this.bindersBean.addDynCompInHolder(XSAConstants.ID_InpFacs + idsPfx, inpFacs);
// This is of no use without getting the required validator (above) to work
		HtmlMessage msgFacs = donothingCompsFactory.htmlMessage(null, idInpFacs);
		this.bindersBean.addDynCompInHolder(XSAConstants.ID_MsgFacs + idsPfx, msgFacs);
		
		HtmlCommandLink cmdSetFacs = (HtmlCommandLink) compsFactory
				.command(
						HtmlCommandLink.COMPONENT_TYPE,
						null,
						Messages.getString("en", "WAMCPUiGenFactory.AssocImg" + idsPfx), "#{" + XSAConstants.BEANNAME_ACTION_LISTENER //$NON-NLS-1$ //$NON-NLS-2$
								+ ".refToShownFacs}"); //$NON-NLS-1$
		cmdSetFacs.getAttributes().put(XSAUiActionListener.PARAM_ID_FOR_ELT,
				idInpFacs);
		cmdSetFacs.setImmediate(true);// so that validator doesn't kill us
		this.bindersBean.addDynComp(XSAConstants.ID_CmdSetFacs + idsPfx, cmdSetFacs);
		
		HtmlCommandLink cmdShowFacs = (HtmlCommandLink) donothingCompsFactory
				.command(
						HtmlCommandLink.COMPONENT_TYPE,
						null,
						Messages.getString("en", "WAMCPUiGenFactory.ViewImg" + idsPfx), "#{" + XSAConstants.BEANNAME_ACTION_LISTENER //$NON-NLS-1$ //$NON-NLS-2$
								+ ".showFacs}"); //$NON-NLS-1$
		cmdShowFacs.getAttributes().put(XSAUiActionListener.PARAM_ID_FOR_ELT,
				idInpFacs);
		cmdShowFacs.setImmediate(true);// so that validator doesn't kill us
		this.bindersBean.addDynComp(XSAConstants.ID_CmdShowfacs + idsPfx, cmdShowFacs);
		
		HtmlCommandLink cmdClearFacs = (HtmlCommandLink) compsFactory.command(
				HtmlCommandLink.COMPONENT_TYPE,
				null, Messages.getString("en", "WAMCPUiGenFactory.ClearAssoc" + idsPfx), "#{" //$NON-NLS-1$ //$NON-NLS-2$
						+ XSAConstants.BEANNAME_ACTION_LISTENER
						+ ".clearFacs}"); //$NON-NLS-1$
		cmdClearFacs.getAttributes().put(XSAUiActionListener.PARAM_ID_FOR_ELT,
				idInpFacs);
		cmdClearFacs.setImmediate(true);// so that validator doesn't kill us
		this.bindersBean.addDynComp(XSAConstants.ID_CmdClearfacs + idsPfx, cmdClearFacs);
		
		DomValueBinder facsBinder = this
				.bindValue(inpFacs, facsAttr, xsaInst, ixBetBros, false, false);
		
		inpFacs.setValueBinding("value", null); // This component doesn't show the value, //$NON-NLS-1$
		// but rather is a selector to select which image to show or delete
		
		Document teiXml;
		try {
			teiXml = this.domTarget.getDomTotal();
		} catch (DomBuildChangesException e1) {
			throw new XSAException(e1);
		}
		
		for (String facsRef : facsBinder.getValueList()) {
			
			facsRef = facsRef.trim();
			
			if (facsRef.isEmpty()) {
				continue;
			}
			
// YA 20101028 The names are too long for the control
// String facsGraphic = null;
// try {
// facsGraphic = TEIEnrichXAO.facsGraphicNameForFacsRef(facsRef, teiXml);
// } catch (JDOMException e) {
// LOG.error(e, e);
// } catch (XSAEnforceException e) {
// LOG.error(e, e);
// }
//
// UISelectItem selectItem = IceCompsFactory.selectItem(facsRef,
// facsGraphic);
			
			UISelectItem selectItem = IceCompsFactory.selectItem(facsRef,
					facsRef);
// End YA 20101028
			
			inpFacs.getChildren().add(selectItem);
			
		}
	}
	
	@Override
	protected void decorateArea(XSAInstance xsaInst, String id, IceCompsFactory compsFactory)
			throws UIGenerationException, XSAException {
		// // Nothing
		
	}
	
	@Override
	protected void decorateContainer(XSAInstance xsaInst, SugtnDeclNode declNode,
			int ixBetBros, String id, IceCompsFactory compsFactory) throws UIGenerationException,
			XSAException {
		
		try {
			if (xsaInst.isNomixed()
					&& xsaInst.isFacsRequired()) {
				this.createFacsRefControls(xsaInst, declNode, ixBetBros, id, compsFactory,
						FacsRefRecepient.Container);
			}
		} catch (DataConversionException e) {
			throw new XSAException(e);
		}
	}
	
	@Override
	protected void decorateField(XSAInstance xsaInst, SugtnDeclNode declNode, String id,
			IceCompsFactory compsFactory) throws UIGenerationException, XSAException {
		// // Nothing
		
	}
	
	@Override
	protected void decorateFieldOcc(XSAInstance xsaInst, SugtnDeclNode declNode,
			int ixBetBros, String id, IceCompsFactory compsFactory) throws UIGenerationException,
			XSAException {
		if (xsaInst.isFacsOptional()) {
			// TODONE move this WAMCP specific code to a place similar to the decorator
			// but with access to the XSAInstance
			this.createFacsRefControls(xsaInst, declNode, ixBetBros, id, compsFactory,
					FacsRefRecepient.FieldOcc);
		}
	}
	
	@Override
	protected void decorateGroup(XSAGroup subSecInst, String id, IceCompsFactory compsFactory)
			throws XSAException {
		// // Nothing
		
	}
	
	@Override
	protected void decorateSection(XSAInstance xsaInst, String id, IceCompsFactory compsFactory)
			throws UIGenerationException, XSAException {
		// // Nothing
		
	}
	
}
