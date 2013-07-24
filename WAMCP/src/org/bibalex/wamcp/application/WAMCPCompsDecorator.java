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

package org.bibalex.wamcp.application;

import javax.faces.component.UIInput;

import org.bibalex.sdxe.xsa.model.XSAInstance;
import org.bibalex.sdxe.xsa.uigen.icefaces.XSACompsDecorator;

import com.icesoft.faces.component.ext.HtmlInputText;
import com.icesoft.faces.component.ext.HtmlInputTextarea;
import com.icesoft.faces.component.ext.HtmlSelectBooleanCheckbox;
import com.icesoft.faces.component.ext.HtmlSelectManyCheckbox;
import com.icesoft.faces.component.ext.HtmlSelectManyListbox;
import com.icesoft.faces.component.ext.HtmlSelectOneListbox;
import com.icesoft.faces.component.selectinputdate.SelectInputDate;
import com.icesoft.faces.component.selectinputtext.SelectInputText;

public class WAMCPCompsDecorator extends XSACompsDecorator {
	
	public WAMCPCompsDecorator(XSAInstance xsaInst) {
		super(xsaInst);
		
	}
	
	@Override
	public HtmlInputText decorate(HtmlInputText comp) {
		HtmlInputText result = (HtmlInputText) super.decorate(comp);
		result.setOnfocus((result.getOnfocus() != null ? result.getOnfocus() + ";" : "")
				+ " attachInputToVKB(this); "); // YA20101227: + " PopupVirtualKeyboard.attachInput(this); ");
		result.setStyleClass("xsaTextField");
		return result;
	}
	
	@Override
	public HtmlInputTextarea decorate(HtmlInputTextarea comp) {
		HtmlInputTextarea result = (HtmlInputTextarea) super.decorate(comp);
		result.setOnfocus((result.getOnfocus() != null ? result.getOnfocus() + ";" : "")
				+ " attachInputToVKB(this); "); // YA20101227: + " PopupVirtualKeyboard.attachInput(this); ");
		result.setStyleClass("xsaTextArea");
		return result;
	}
	
	@Override
	public HtmlSelectBooleanCheckbox decorate(HtmlSelectBooleanCheckbox comp) {
		HtmlSelectBooleanCheckbox result = super.decorate(comp);
		result.setOnfocus((result.getOnfocus() != null ? result.getOnfocus() + ";" : "")
				+ " attachInputToVKB(this); "); // YA20101227: + " PopupVirtualKeyboard.attachInput(this); ");
		result.setStyleClass("xsaTextField");
		return result;
	}
	
	@Override
	public HtmlSelectManyCheckbox decorate(HtmlSelectManyCheckbox comp) {
		HtmlSelectManyCheckbox result = super.decorate(comp);
		result.setOnfocus((result.getOnfocus() != null ? result.getOnfocus() + ";" : "")
				+ " attachInputToVKB(this); "); // YA20101227: + " PopupVirtualKeyboard.attachInput(this); ");
		result.setStyleClass("xsaTextArea");
		return result;
	}
	
	@Override
	public HtmlSelectManyListbox decorate(HtmlSelectManyListbox comp) {
		HtmlSelectManyListbox result = super.decorate(comp);
		result.setOnfocus((result.getOnfocus() != null ? result.getOnfocus() + ";" : "")
				+ " attachInputToVKB(this); "); // YA20101227: + " PopupVirtualKeyboard.attachInput(this); ");
		result.setStyleClass("xsaTextArea");
		return result;
	}
	
	@Override
	public HtmlSelectOneListbox decorate(HtmlSelectOneListbox comp) {
		HtmlSelectOneListbox result = super.decorate(comp);
		result.setOnfocus((result.getOnfocus() != null ? result.getOnfocus() + ";" : "")
				+ " attachInputToVKB(this); "); // YA20101227: + " PopupVirtualKeyboard.attachInput(this); ");
		result.setStyleClass("xsaTextField");
		return result;
	}
	
	@Override
	public SelectInputText decorate(SelectInputText comp) {
		SelectInputText result = super.decorate(comp);
		result.setOnfocus((result.getOnfocus() != null ? result.getOnfocus() + ";" : "")
				+ " attachInputToVKB(this); "); // YA20101227: + " PopupVirtualKeyboard.attachInput(this); ");
		result.setStyleClass("xsaTextField");
		return result;
	}
	
	@Override
	public UIInput decorate(UIInput comp) {
		UIInput result = super.decorate(comp);
		String style = (String) result.getAttributes().get("styleClass");
		style = style == null ? "" : " " + style;
		result.getAttributes().put("styleClass", "xsaTextField" + style);
		return result;
	}
	
	@Override
	public SelectInputDate decoreate(SelectInputDate comp) {
		SelectInputDate result = (SelectInputDate) super.decorate(comp);
		result.setOnfocus((result.getOnfocus() != null ? result.getOnfocus() + ";" : "")
				+ " attachInputToVKB(this); "); // YA20101227: + " PopupVirtualKeyboard.attachInput(this); ");
		result.setStyleClass("xsaTextField");
		return result;
	}
	
}
