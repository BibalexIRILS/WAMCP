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
 * To decorate generated components, these methods are called. 
 * 
 * @author Younos.Naga
 * 
 */
public interface IIceCompsDecorator {
	
	public HtmlInputText decorate(HtmlInputText comp);
	
	public HtmlInputTextarea decorate(HtmlInputTextarea comp);
	
	public HtmlMessage decorate(HtmlMessage comp);
	
	public HtmlOutputLabel decorate(HtmlOutputLabel comp);
	
	public HtmlSelectBooleanCheckbox decorate(HtmlSelectBooleanCheckbox comp);
	
	public HtmlSelectManyCheckbox decorate(HtmlSelectManyCheckbox comp);
	
	public HtmlSelectManyListbox decorate(HtmlSelectManyListbox comp);
	
	public HtmlSelectOneListbox decorate(HtmlSelectOneListbox comp);
	
	public PanelCollapsible decorate(PanelCollapsible comp);
	
	public PanelPopup decorate(PanelPopup comp);
	
	public PanelTooltip decorate(PanelTooltip comp);
	
	public SelectInputText decorate(SelectInputText comp);
	
	public UICommand decorate(UICommand comp);
	
	public UIInput decorate(UIInput comp);
	
	public UIOutput decorate(UIOutput comp);
	
	public UIPanel decorate(UIPanel comp);
	
	public UISeries decorate(UISeries comp);
	
	public SelectInputDate decoreate(SelectInputDate comp);
	
}
