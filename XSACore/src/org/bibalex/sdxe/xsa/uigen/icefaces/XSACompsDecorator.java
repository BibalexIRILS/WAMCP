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

import javax.faces.component.UICommand;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UIPanel;

import org.apache.log4j.Logger;
import org.bibalex.sdxe.xsa.model.XSAInstance;
import org.bibalex.sdxe.xsa.model.XSAInstance.XSAAccessTypes;
import org.bibalex.uigen.icefaces.DoNothingCompsDecorator;

import com.icesoft.faces.component.ext.HtmlCommandButton;
import com.icesoft.faces.component.ext.HtmlCommandLink;
import com.icesoft.faces.component.ext.HtmlInputText;
import com.icesoft.faces.component.ext.HtmlInputTextarea;
import com.icesoft.faces.component.ext.HtmlMessage;
import com.icesoft.faces.component.ext.HtmlOutputLabel;
import com.icesoft.faces.component.ext.HtmlPanelGrid;
import com.icesoft.faces.component.ext.HtmlPanelGroup;
import com.icesoft.faces.component.ext.HtmlSelectBooleanCheckbox;
import com.icesoft.faces.component.ext.HtmlSelectManyCheckbox;
import com.icesoft.faces.component.ext.HtmlSelectManyListbox;
import com.icesoft.faces.component.ext.HtmlSelectOneListbox;
import com.icesoft.faces.component.panelcollapsible.PanelCollapsible;
import com.icesoft.faces.component.panelpopup.PanelPopup;
import com.icesoft.faces.component.panelseries.UISeries;
import com.icesoft.faces.component.paneltabset.PanelTabSet;
import com.icesoft.faces.component.paneltooltip.PanelTooltip;
import com.icesoft.faces.component.selectinputdate.SelectInputDate;
import com.icesoft.faces.component.selectinputtext.SelectInputText;

/**
 * This implementation of the CompsDecorator interface does the following:
 * 1) Set access rights of the UI component according to those in XSA.
 * 2) Set onfocus Javascript needed for adding Annotators (mix-in elements) from the UI: 
 * 		this is done by setting a variable with the id of the active element except in case of that the active element is the annotator element. 
 */
public class XSACompsDecorator extends DoNothingCompsDecorator {
	private static Logger LOG = Logger.getLogger("org.bibalex.xsa");
	private XSAInstance xsaInst = null;
	
	/**
	 * @param xsaInst
	 */
	public XSACompsDecorator(XSAInstance xsaInst) {
		this.xsaInst = xsaInst;
	}
	
	@Override
	public HtmlInputText decorate(HtmlInputText comp) {
		// Repeated code because there is no base class for icefaces comps :(
		comp.setRenderedOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Read));
		comp.setEnabledOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Write));
		comp.setOnfocus((comp.getOnfocus() != null ? comp.getOnfocus() + ";" : "")
				+ " setLastActive(this.id, '" + this.xsaInst.getLocator().asString() + "');");
		return comp;
	}
	
	@Override
	public HtmlInputTextarea decorate(HtmlInputTextarea comp) {
		// Repeated code because there is no base class for icefaces comps :(
		comp.setRenderedOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Read));
		comp.setEnabledOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Write));
		comp.setOnfocus((comp.getOnfocus() != null ? comp.getOnfocus() + ";" : "")
				+ " setLastActive(this.id, '" + this.xsaInst.getLocator().asString() + "');");
		return comp;
	}
	
	@Override
	public HtmlMessage decorate(HtmlMessage comp) {
		// Repeated code because there is no base class for icefaces comps :(
		comp.setRenderedOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Read));
		return comp;
	}
	
	@Override
	public HtmlOutputLabel decorate(HtmlOutputLabel comp) {
		// Repeated code because there is no base class for icefaces comps :(
		comp.setRenderedOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Read));
		return comp;
	}
	
	@Override
	public HtmlSelectBooleanCheckbox decorate(HtmlSelectBooleanCheckbox comp) {
		// Repeated code because there is no base class for icefaces comps :(
		comp.setRenderedOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Read));
		comp.setEnabledOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Write));
		comp.setOnfocus((comp.getOnfocus() != null ? comp.getOnfocus() + ";" : "")
				+ " setLastActive(this.id, '" + this.xsaInst.getLocator().asString() + "');");
		return comp;
		
	}
	
	@Override
	public HtmlSelectManyCheckbox decorate(HtmlSelectManyCheckbox comp) {
		// Repeated code because there is no base class for icefaces comps :(
		comp.setRenderedOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Read));
		comp.setEnabledOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Write));
		comp.setOnfocus((comp.getOnfocus() != null ? comp.getOnfocus() + ";" : "")
				+ " setLastActive(this.id, '" + this.xsaInst.getLocator().asString() + "');");
		return comp;
	}
	
	@Override
	public HtmlSelectManyListbox decorate(HtmlSelectManyListbox comp) {
		// Repeated code because there is no base class for icefaces comps :(
		comp.setRenderedOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Read));
		comp.setEnabledOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Write));
		comp.setOnfocus((comp.getOnfocus() != null ? comp.getOnfocus() + ";" : "")
				+ " setLastActive(this.id, '" + this.xsaInst.getLocator().asString() + "');");
		return comp;
	}
	
	@Override
	public HtmlSelectOneListbox decorate(HtmlSelectOneListbox comp) {
		// Repeated code because there is no base class for icefaces comps :(
		comp.setRenderedOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Read));
		comp.setEnabledOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Write));
		comp.setOnfocus((comp.getOnfocus() != null ? comp.getOnfocus() + ";" : "")
				+ " setLastActive(this.id, '" + this.xsaInst.getLocator().asString() + "');");
		return comp;
	}
	
	@Override
	public PanelCollapsible decorate(PanelCollapsible comp) {
		// Repeated code because there is no base class for icefaces comps :(
		comp.setRenderedOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Read));
		comp.setEnabledOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Write));
		return comp;
	}
	
	@Override
	public PanelPopup decorate(PanelPopup comp) {
		// Repeated code because there is no base class for icefaces comps :(
		comp.setRenderedOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Read));
		
		return comp;
	}
	
	@Override
	public PanelTooltip decorate(PanelTooltip comp) {
		// Repeated code because there is no base class for icefaces comps :(
		comp.setRenderedOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Read));
		return comp;
	}
	
	@Override
	public SelectInputText decorate(SelectInputText comp) {
		// Repeated code because there is no base class for icefaces comps :(
		comp.setRenderedOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Read));
		comp.setEnabledOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Write));
		comp.setOnfocus((comp.getOnfocus() != null ? comp.getOnfocus() + ";" : "")
				+ " setLastActive(this.id, '" + this.xsaInst.getLocator().asString() + "');");
		return comp;
	}
	
	@Override
	public UICommand decorate(UICommand comp) {
		comp = super.decorate(comp);
		if (comp instanceof HtmlCommandLink) {
			HtmlCommandLink link = (HtmlCommandLink) comp;
			// Repeated code because there is no base class for icefaces comps :(
			link.setRenderedOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Read));
			link.setEnabledOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Write));
		} else if (comp instanceof HtmlCommandButton) {
			HtmlCommandButton btn = (HtmlCommandButton) comp;
			// Repeated code because there is no base class for icefaces comps :(
			btn.setRenderedOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Read));
			btn.setEnabledOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Write));
		} else {
			throw new UnsupportedOperationException(
					"To enforce authorization, only IceFaces commands should be used");
		}
		
		return comp;
	}
	
	@Override
	public UIInput decorate(UIInput comp) {
// LOG.warn("An input with NO ACCESS RIGHTS enforcement created: " + comp.getId());
		return super.decorate(comp);
	}
	
	@Override
	public UIOutput decorate(UIOutput comp) {
// LOG.warn("An output with NO ACCESS RIGHTS enforcement created: " + comp.getId());
		return super.decorate(comp);
	}
	
	@Override
	public UIPanel decorate(UIPanel comp) {
		if (comp instanceof HtmlPanelGroup) {
			((HtmlPanelGroup) comp).setRenderedOnUserRole(this.xsaInst
					.getAccessRoles(XSAAccessTypes.Read));
		} else if (comp instanceof HtmlPanelGrid) {
			((HtmlPanelGrid) comp).setRenderedOnUserRole(this.xsaInst
					.getAccessRoles(XSAAccessTypes.Read));
		}
		return comp;
	}
	
	@Override
	public UISeries decorate(UISeries comp) {
		if (comp instanceof PanelTabSet) {
			PanelTabSet tabset = (PanelTabSet) comp;
			// Repeated code because there is no base class for icefaces comps :(
			tabset.setRenderedOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Read));
		}
		return comp;
	}
	
	@Override
	public SelectInputDate decoreate(SelectInputDate comp) {
		// THIS MAYBE NOT CALLED EVER.. BECAUSE COMPILER SEES THE InputData as an InputText
		// Repeated code because there is no base class for icefaces comps :(
		comp.setRenderedOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Read));
		comp.setEnabledOnUserRole(this.xsaInst.getAccessRoles(XSAAccessTypes.Write));
		return comp;
	}
	
}
