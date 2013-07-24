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

package org.bibalex.uigen.icefaces;

import javax.faces.component.UICommand;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UIPanel;

import com.icesoft.faces.component.ext.HtmlCommandLink;
import com.icesoft.faces.component.ext.HtmlInputText;
import com.icesoft.faces.component.ext.HtmlInputTextarea;
import com.icesoft.faces.component.ext.HtmlMessage;
import com.icesoft.faces.component.ext.HtmlOutputLabel;
import com.icesoft.faces.component.ext.HtmlSelectBooleanCheckbox;
import com.icesoft.faces.component.ext.HtmlSelectManyCheckbox;
import com.icesoft.faces.component.ext.HtmlSelectManyListbox;
import com.icesoft.faces.component.ext.HtmlSelectOneListbox;
import com.icesoft.faces.component.panelcollapsible.PanelCollapsible;
import com.icesoft.faces.component.panelpopup.PanelPopup;
import com.icesoft.faces.component.panelseries.UISeries;
import com.icesoft.faces.component.paneltooltip.PanelTooltip;
import com.icesoft.faces.component.selectinputdate.SelectInputDate;
import com.icesoft.faces.component.selectinputtext.SelectInputText;

/**
 * An implmentation of CompDecoratator
 * 
 * @author Younos.Naga
 * 
 */
public class DoNothingCompsDecorator implements IIceCompsDecorator {
	
	@Override
	public HtmlInputText decorate(HtmlInputText comp) {
		// Auto-generated method stub
		return comp;
	}
	
	@Override
	public HtmlInputTextarea decorate(HtmlInputTextarea comp) {
		// Auto-generated method stub
		return comp;
	}
	
	@Override
	public HtmlMessage decorate(HtmlMessage comp) {
		// Auto-generated method stub
		return comp;
	}
	
	@Override
	public HtmlOutputLabel decorate(HtmlOutputLabel comp) {
		// Auto-generated method stub
		return comp;
	}
	
	@Override
	public HtmlSelectBooleanCheckbox decorate(HtmlSelectBooleanCheckbox comp) {
		// Auto-generated method stub
		return comp;
	}
	
	@Override
	public HtmlSelectManyCheckbox decorate(HtmlSelectManyCheckbox comp) {
		// Auto-generated method stub
		return comp;
	}
	
	@Override
	public HtmlSelectManyListbox decorate(HtmlSelectManyListbox comp) {
		// Auto-generated method stub
		return comp;
	}
	
	@Override
	public HtmlSelectOneListbox decorate(HtmlSelectOneListbox comp) {
		// Auto-generated method stub
		return comp;
	}
	
	@Override
	public PanelCollapsible decorate(PanelCollapsible comp) {
		// Auto-generated method stub
		return comp;
	}
	
	@Override
	public PanelPopup decorate(PanelPopup comp) {
		// Auto-generated method stub
		return comp;
	}
	
	@Override
	public PanelTooltip decorate(PanelTooltip comp) {
		// Auto-generated method stub
		return comp;
	}
	
	@Override
	public SelectInputText decorate(SelectInputText comp) {
		// Auto-generated method stub
		return comp;
	}
	
	@Override
	public UICommand decorate(UICommand comp) {
		// Auto-generated method stub
		if (comp instanceof HtmlCommandLink) {
			((HtmlCommandLink) comp).setTabindex("-1");
		}
		return comp;
	}
	
	@Override
	public UIInput decorate(UIInput comp) {
		// Auto-generated method stub
		return comp;
	}
	
	@Override
	public UIOutput decorate(UIOutput comp) {
		// Auto-generated method stub
		return comp;
	}
	
	@Override
	public UIPanel decorate(UIPanel comp) {
		// Auto-generated method stub
		return comp;
	}
	
	@Override
	public UISeries decorate(UISeries comp) {
		// Auto-generated method stub
		return comp;
	}
	
	@Override
	public SelectInputDate decoreate(SelectInputDate comp) {
		// Auto-generated method stub
		return comp;
	}
	
}
