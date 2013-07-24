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

import java.util.LinkedList;
import java.util.regex.Pattern;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.application.Application;
import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UIPanel;
import javax.faces.component.UIParameter;
import javax.faces.component.UISelectItem;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.faces.event.ActionEvent;
import javax.faces.event.MethodExpressionActionListener;
import javax.faces.model.SelectItem;

import org.bibalex.sdxe.suggest.model.dom.SugtnTypeDeclaration;

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
import com.icesoft.faces.component.panelcollapsible.PanelCollapsible;
import com.icesoft.faces.component.panelconfirmation.PanelConfirmation;
import com.icesoft.faces.component.panelpopup.PanelPopup;
import com.icesoft.faces.component.panelseries.UISeries;
import com.icesoft.faces.component.paneltooltip.PanelTooltip;
import com.icesoft.faces.component.selectinputdate.SelectInputDate;
import com.icesoft.faces.component.selectinputtext.SelectInputText;

/**
 * Creates Icefaces components
 * 
 * @author Younos.Naga
 * 
 */
public class IceCompsFactory {
	
	/**
	 * Match #{...}
	 * Like <code>UIComponentTag.isValueReference</code> we allow nested #{...} blocks, because this can still be a
	 * legitimate value reference:
	 * <p>
	 * <code>
	 * #{!empty bar ? '' : '#{foo}'}
	 * </code>
	 */
	private final static Pattern PATTERN_EXPRESSION = Pattern.compile("((#)\\{)(.*)(\\})");
	
	/**
	 * Checks where either this string is a valid JSF EL Expression or not
	 * @param value the expression to be tested
	 * @return true if this is a valid JSF EL Expression, false if not
	 */
	public static boolean isExpression(String value) {
		return PATTERN_EXPRESSION.matcher(value).matches();
	}
	
	/**
	 * 
	 * @param actionListener
	 * @return
	 * @throws UIGenerationException
	 */
	public static MethodExpressionActionListener methodExpressionActionListener(
			String actionListener)
			throws UIGenerationException {
		
		validateExpression(actionListener);
		
		IceCompsFactory THIS = new IceCompsFactory(new DoNothingCompsDecorator());
		
		MethodExpression methodExpression = THIS.exprFactory.createMethodExpression(THIS.elContext,
				actionListener,
				null, new Class[] { ActionEvent.class });
		MethodExpressionActionListener result = new MethodExpressionActionListener(
				methodExpression);
		
		return result;
	}
	
	/**
	 * This method is used to generate a confirmation panel
	 * 
	 * @param IceFaces component id
	 * @param title panel title
	 * @param message the confirmation message
	 * @param acceptLabel the accept button label
	 * @param cancelLabel the cancel button label
	 * @return PanelConfirmation
	 */
	@Deprecated
	public static PanelConfirmation panelConfirmation(String id, String title, String message,
			String acceptLabel, String cancelLabel) {
		
		IceCompsFactory THIS = new IceCompsFactory(new DoNothingCompsDecorator());
		
		PanelConfirmation result = (PanelConfirmation) THIS.application
				.createComponent(PanelConfirmation.COMPONENT_TYPE);
		
		THIS.assignId(result, id);
		// TODO defaults
		result.setTitle(title);
		result.setMessage(message);
		result.setAcceptLabel(acceptLabel);
		result.setCancelLabel(cancelLabel);
		
		result.setAutoCentre(true);
		
		return result;
	}
	
	/**
	 * This method is used for generating a UIParameter 
	 * 
	 * @param name parameter name
	 * @param value parameter value
	 * @return UIParameter object 
	 */
	@Deprecated
	public static UIParameter param(String name, String value) {
		IceCompsFactory THIS = new IceCompsFactory(new DoNothingCompsDecorator());
		
		UIParameter result = (UIParameter) THIS.application
				.createComponent(UIParameter.COMPONENT_TYPE);
		
		result.setName(name);
		result.setValue(value);
		
		return result;
		
	}
	
	/**
	 * This method is used for generating a SelectItem pair
	 * @param value the value of the item
	 * @return JSF UISelectItem component
	 * @throws UIGenerationException
	 */
	@Deprecated
	public static UISelectItem selectItem(Object value) throws UIGenerationException {
		return selectItem(value, null, null);
	}
	
	/**
	 * This method is used for generating a SelectItem pair
	 * 
	 * @param value the value of the item
	 * @param label the label of the item 
	 * 
	 * @return JSF UISelectItem component
	 */
	@Deprecated
	public static UISelectItem selectItem(Object value, String label) throws UIGenerationException {
		return selectItem(value, label, null);
	}
	
	/**
	 * This method is used for generating a SelectItem pair
	 * 
	 * @param value the value of the item
	 * @param label the label of the item
	 * @param description describes the content of the item
	 * 
	 * @return JSF UISelectItem component
	 * 
	 * @throws UIGenerationException
	 * 
	 */
	@Deprecated
	public static UISelectItem selectItem(Object value, String label,
			String description) throws UIGenerationException {
		FacesContext context = FacesContext.getCurrentInstance();
		Application application = context.getApplication();
		
		UISelectItem selectItem = (UISelectItem) application
				.createComponent(UISelectItem.COMPONENT_TYPE);
		selectItem.setId(context.getViewRoot().createUniqueId());
		
		if (value == null) {
			// This requires JSF 1.2
			// JSF 1.1 doesn't allow 'null' as the item value, but JSF 1.2 requires
			// it for proper behaviour (see
			// https://javaserverfaces.dev.java.net/issues/show_bug.cgi?id=795)
			value = new SelectItem(null, "");
			selectItem.setValue(value);
		} else {
			selectItem.setItemValue(value);
		}
		
		if (label == null) {
			// If no label, make it the same as the value. For JSF-RI, this is needed for labels
			// next to UISelectMany checkboxes
			
			selectItem.setItemLabel(value.toString()); // label);
			
		} else {
			
			// Label may be a value reference (eg. into a bundle)
			
			if (isExpression(label)) {
				selectItem.setValueExpression("itemLabel", valueExpression(label, String.class));
			} else {
				// Label may be localized
				
				// TODONOT: support localization
				String localizedLabel = null;
				
				if (localizedLabel != null) {
					selectItem.setItemLabel(localizedLabel);
				} else {
					selectItem.setItemLabel(label);
				}
			}
		}
		
		if (description != null) {
			selectItem.setItemDescription(description);
		}
		
		return selectItem;
	}
	
	/**
	 * validates that this string is a valid JSF EL Expression
	 * @param elStr the JSF Expression Syntax to validate
	 * @throws UIGenerationException if this is not a vaild JSF EL Expression
	 */
	private static void validateExpression(String elStr) throws UIGenerationException {
		if (!isExpression(elStr)) {
			throw new UIGenerationException("Invalid EL Expression: " + elStr);
		}
	}
	
	/**
	 * This method is used for the creation of a JSF EL Expression 
	 * @param elStr the JSF EL Expression
	 * @param classType defines the class type of the returned value from the expression
	 * @return ValueExpression
	 * @throws UIGenerationException
	 */
	public static ValueExpression valueExpression(String elStr, Class classType)
			throws UIGenerationException {
		
		validateExpression(elStr);
		
		IceCompsFactory THIS = new IceCompsFactory(new DoNothingCompsDecorator());
		
		ValueExpression result = null;
		
		// JSF 1.2
		result = THIS.exprFactory.createValueExpression(
				THIS.elContext, elStr, classType);
		
		return result;
		
	}
	
	/**
	 * 
	 */
	private IIceCompsDecorator decorator = null;
	
	private FacesContext context = null;
	
	private Application application = null;
	
	private ELContext elContext = null;
	
	private ExpressionFactory exprFactory = null;
	
	/**
	 * This constructor is used to generate the IceFaces components with the given decorator 
	 * @param decorator this is an object that is an instance of the IIceCompsDecorator interface and it is used to decorate the generated IceFace comonents
	 */
	public IceCompsFactory(IIceCompsDecorator decorator) {
		this.context = FacesContext.getCurrentInstance();
		this.application = this.context.getApplication();
		this.elContext = this.context.getELContext();
		this.exprFactory = this.application.getExpressionFactory();
		this.decorator = decorator;
	}
	
	/**
	 * 
	 * @param id
	 * @param headerTextStr
	 * @param visibleELStr
	 * @return
	 */
	public PanelCollapsible accordion(String id, String headerTextStr, String visibleELStr) {
		
		PanelCollapsible result = (PanelCollapsible) this.application
				.createComponent(PanelCollapsible.COMPONENET_TYPE); // TODO: if icefaces fixed this typo.. we fix it too
		
		this.assignId(result, id);
		
		HtmlPanelGroup head = (HtmlPanelGroup) this.panel(HtmlPanelGroup.COMPONENT_TYPE, "header-"
				+ result.getId());
		HtmlPanelGroup body = (HtmlPanelGroup) this.panel(HtmlPanelGroup.COMPONENT_TYPE, "body-"
				+ result.getId());
		
		result.getFacets().put("header", head);
		result.getChildren().add(body);
		
		HtmlOutputText headerTxt = (HtmlOutputText) this.output(
				HtmlOutputText.COMPONENT_TYPE, null, headerTextStr);
		head.getChildren().add(headerTxt);
		
		if ((visibleELStr != null) && !visibleELStr.isEmpty()) {
			if (isExpression(visibleELStr)) {
				visibleELStr = visibleELStr.replace('#', '$');
				ValueBinding visibleEL = this.application.createValueBinding(
						visibleELStr);
				
				result.setValueBinding("expanded", visibleEL);
				
			} else {
				result.setExpanded(Boolean.parseBoolean(visibleELStr));
			}
		} else {
			result.setExpanded(false);
		}
		
		result = (PanelCollapsible) this.decorator.decorate(result);
		
		return result;
	}
	
	/**
	 * 
	 * @param comp
	 * @param id
	 */
	private void assignId(UIComponent comp, String id) {
		String actualId = this.generateId(id);
		comp.setId(actualId);
	}
	
	/**
	 * 
	 * @param COMPONENT_TYPE
	 * @param id
	 * @param value
	 * @param actionListener
	 * @return
	 * @throws UIGenerationException
	 */
	public UICommand command(String COMPONENT_TYPE, String id, String value,
			String actionListener)
			throws UIGenerationException {
		
		UICommand result = (UICommand) this.application.createComponent(COMPONENT_TYPE);
		
		this.assignId(result, id);
		
		result.setValue(value);
		
		if ((actionListener != null) && !actionListener.isEmpty()) {
			
			MethodExpressionActionListener mexAcListener = methodExpressionActionListener(actionListener);
			
			result.addActionListener(mexAcListener);
		}
		
		result = this.decorator.decorate(result);
		
		return result;
		
	}
	
	/**
	 * 
	 * @param id
	 * @return
	 */
	private String generateId(String id) {
		return (id != null) && !id.isEmpty()
				? id
				: this.context.getViewRoot().createUniqueId();
	}
	
	/**
	 * 
	 * @param id
	 * @param partialSubmit
	 * @return
	 */
	public HtmlInputText htmlInputText(String id, boolean partialSubmit) {
		
		// Input Field
		HtmlInputText result = (HtmlInputText) this.input(HtmlInputText.COMPONENT_TYPE,
				id);
		
		result.setPartialSubmit(partialSubmit);
		
		result = this.decorator.decorate(result);
		
		return result;
	}
	
	/**
	 * 
	 * @param id
	 * @param partialSubmit
	 * @return
	 */
	public HtmlInputTextarea htmlInputTextarea(String id,
			boolean partialSubmit) {
		
		HtmlInputTextarea result = (HtmlInputTextarea) this.input(
				HtmlInputTextarea.COMPONENT_TYPE, id);
		
		result.setPartialSubmit(partialSubmit);
		
		// XHTML requires the 'cols' and 'rows' attributes be set, even though
		// most people override them with CSS widths and heights. The default is
		// generally 20 columns by 2 rows
		
		result.setCols(20);
		result.setRows(2);
		
		result = this.decorator.decorate(result);
		
		return result;
	}
	
	/**
	 * 
	 * @param id
	 * @param idFor
	 * @return
	 */
	public HtmlMessage htmlMessage(String id, String idFor) {
		
		HtmlMessage result = (HtmlMessage) this.application
				.createComponent(HtmlMessage.COMPONENT_TYPE);
		
		this.assignId(result, id);
		result.setFor(idFor);
		
		result.setTitle("");
		result.setShowDetail(false);
		result.setShowSummary(true); // TODONOT configuration file
		result = this.decorator.decorate(result);
		return result;
	}
	
	/**
	 * 
	 * @param id
	 * @param value
	 * @param forId
	 * @return
	 */
	public HtmlOutputLabel htmlOutputLabel(String id, String value, String forId) {
		
		HtmlOutputLabel result = (HtmlOutputLabel) this.output(
				HtmlOutputLabel.COMPONENT_TYPE, id, value);
		
		result.setFor(forId);
		
		result = this.decorator.decorate(result);
		return result;
	}
	
	/**
	 * 
	 * @param id
	 * @param partialSubmit
	 * @return
	 */
	public HtmlSelectBooleanCheckbox htmlSelectBooleanCheckbox(String id,
			boolean partialSubmit) {
		
		HtmlSelectBooleanCheckbox result = (HtmlSelectBooleanCheckbox) this.input(
				HtmlSelectBooleanCheckbox.COMPONENT_TYPE, id);
		
		this.assignId(result, id);
		result.setPartialSubmit(partialSubmit);
		
		result = this.decorator.decorate(result);
		return result;
	}
	
	/**
	 * 
	 * @param id
	 * @param partialSubmit
	 * @return
	 */
	public HtmlSelectManyCheckbox htmlSelectManyCheckbox(String id, boolean partialSubmit) {
		HtmlSelectManyCheckbox result = (HtmlSelectManyCheckbox) this.application
				.createComponent(HtmlSelectManyCheckbox.COMPONENT_TYPE);
		
		this.assignId(result, id);
		
		result.setLayout("pageDirection");
		
		result.setPartialSubmit(partialSubmit);
		
		result = this.decorator.decorate(result);
		return result;
	}
	
	/**
	 * 
	 * @param id
	 * @param partialSubmit
	 * @return
	 */
	public HtmlSelectManyListbox htmlSelectManyListbox(String id, boolean partialSubmit) {
		HtmlSelectManyListbox result = (HtmlSelectManyListbox) this.application
				.createComponent(HtmlSelectManyListbox.COMPONENT_TYPE);
		
		this.assignId(result, id);
		
		result.setPartialSubmit(partialSubmit);
		
		result = this.decorator.decorate(result);
		return result;
	}
	
	/**
	 * 
	 * @param id
	 * @param partialSubmit
	 * @return
	 */
	public HtmlSelectOneListbox htmlSelectOneListbox(String id, boolean partialSubmit) {
		
		HtmlSelectOneListbox result = (HtmlSelectOneListbox) this.application
				.createComponent(HtmlSelectOneListbox.COMPONENT_TYPE);
		
		this.assignId(result, id);
		
		result.setSize(1);
		
		result.setPartialSubmit(partialSubmit);
		
		result = this.decorator.decorate(result);
		return result;
	}
	
	/**
	 * 
	 * @param COMPONENT_TYPE
	 * @param id
	 * @return
	 */
	public UIInput input(String COMPONENT_TYPE, String id) {
		UIInput result = (UIInput) this.application.createComponent(COMPONENT_TYPE);
		
		this.assignId(result, id);
		
		result = this.decorator.decorate(result);
		return result;
	}
	
	/**
	 * 
	 * @param COMPONENT_TYPE
	 * @param id
	 * @param value
	 * @return
	 */
	public UIOutput output(String COMPONENT_TYPE, String id, String value) {
		UIOutput result = (UIOutput) this.application.createComponent(COMPONENT_TYPE);
		
		this.assignId(result, id);
		
		result.setValue(value);
		result = this.decorator.decorate(result);
		return result;
	}
	
	/**
	 * 
	 * @param COMPONENT_TYPE
	 * @param id
	 * @return
	 */
	public UIPanel panel(String COMPONENT_TYPE, String id) {
		UIPanel result = (UIPanel) this.application
				.createComponent(COMPONENT_TYPE);
		
		this.assignId(result, id);
		result = this.decorator.decorate(result);
		return result;
		
	}
	
	/**
	 * 
	 * @param COMPONENT_TYPE
	 * @param id
	 * @return
	 */
	public UIPanel panelFaceted(String COMPONENT_TYPE, String id) {
		UIPanel result = (UIPanel) this.panel(COMPONENT_TYPE, id);
		
		HtmlPanelGrid head = (HtmlPanelGrid) this.panel(HtmlPanelGrid.COMPONENT_TYPE, "header-"
				+ result.getId());
		HtmlPanelGrid body = (HtmlPanelGrid) this.panel(HtmlPanelGrid.COMPONENT_TYPE, "body-"
				+ result.getId());
		
		result.getFacets().put("header", head);
		result.getFacets().put("body", body);
		
		// ALREADY CALLED IN PANEL: result = this.decorator.decorate(result);
		
		return result;
		
	}
	
	/**
	 * 
	 * @param id
	 * @param title
	 * @param isModal
	 * @param visibleELStr
	 * @return
	 * @throws UIGenerationException
	 */
	public PanelPopup panelPopup(String id, String title, boolean isModal,
			String visibleELStr)
			throws UIGenerationException {
		PanelPopup result = (PanelPopup) this.panelFaceted(PanelPopup.COMPONENT_TYPE, id);
		HtmlPanelGrid head = (HtmlPanelGrid) result.getHeader();
		HtmlPanelGrid body = (HtmlPanelGrid) result.getBody();
		
		visibleELStr = visibleELStr.replace('#', '$');
		ValueBinding visibleEL = this.application.createValueBinding(
				visibleELStr);
		
		result.setValueBinding("visible", visibleEL);
		result.setValueBinding("rendered", visibleEL);
		head.setValueBinding("rendered", visibleEL);
		body.setValueBinding("rendered", visibleEL);
		
		// TODO move to value Expressions once supported by IcePanelPopup
// ValueExpression visibleEL = valueExpression(visibleELStr);
// ValueExpression renderedEL = valueExpression(visibleELStr);
// result.setValueExpression("visible", visibleEL);
// result.setValueExpression("rendered", renderedEL);
		
		// TITLE
		result.setTitle(title);
		
		HtmlOutputText outTitle = (HtmlOutputText) this.output(
				HtmlOutputText.COMPONENT_TYPE,
				null, title);
		
		head.getChildren().add(outTitle);
		
		result.setModal(isModal);
		
		if (!isModal) {
			result.setDraggable("true");
		} else {
			result.setAutoCentre(true);
		}
		result = this.decorator.decorate(result);
		return result;
		
	}
	
	/**
	 * 
	 * @param id
	 * @param owner
	 * @return
	 */
	public PanelTooltip panelTooltip(String id, HtmlPanelGroup owner) {
		PanelTooltip result = (PanelTooltip) this.panel(PanelTooltip.COMPONENT_TYPE, id);
		
		HtmlPanelGrid body = (HtmlPanelGrid) this.panel(HtmlPanelGrid.COMPONENT_TYPE, "body-"
				+ result.getId());
		
		result.getFacets().put("body", body);
		
		result.setHideOn("mouseout");
		result.setStyle("background-color: #FFFFCC; opacity:.9;");
		
		owner.getChildren().add(result);
		owner.setPanelTooltip(result.getId());
		
		result = this.decorator.decorate(result);
		
		return result;
	}
	
	/**
	 * 
	 * @param id
	 * @param format
	 * @param partialSubmit
	 * @return
	 */
	public SelectInputDate selectInputDate(String id, String format, boolean partialSubmit) {
		SelectInputDate result = (SelectInputDate) this.application
				.createComponent(SelectInputDate.COMPONENT_TYPE);
		result.setId(id);
		result.setRenderAsPopup(true);
		result.setPopupDateFormat(format);
		
		result.setPartialSubmit(partialSubmit);
		result = (SelectInputDate) this.decorator.decorate(result);
		
		return result;
		
	}
	
	/**
	 * 
	 * @param container
	 * @param id
	 * @param sugtnType
	 * @param authList
	 * @return
	 * @throws UIGenerationException
	 */
	public SelectInputText selectInputText(UIPanel container, String id,
			SugtnTypeDeclaration sugtnType, LinkedList<String> authList)
			throws UIGenerationException {
		SelectInputText result = null;
		
		result = (SelectInputText) this.input(SelectInputText.COMPONENT_TYPE, id);
		
		for (String listItem : authList) {
			UISelectItem selItem = selectItem(listItem);
			result.getChildren().add(selItem);
		}
		
		result = (SelectInputText) this.decorator.decorate(result);
		return result;
	}
	
	/**
	 * 
	 * @param COMPONENT_TYPE
	 * @param id
	 * @return
	 */
	public UISeries series(String COMPONENT_TYPE, String id) {
		UISeries result = (UISeries) this.application
				.createComponent(COMPONENT_TYPE);
		
		this.assignId(result, id);
		
		result = this.decorator.decorate(result);
		return result;
	}
	
}
