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

package org.bibalex.sdxe.suggest.uigen.icefaces;

import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.faces.application.Application;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIPanel;
import javax.faces.component.UISelectItems;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.model.SelectItem;
import javax.faces.validator.DoubleRangeValidator;
import javax.faces.validator.LengthValidator;

import org.apache.log4j.Logger;
import org.bibalex.sdxe.suggest.model.dom.SugtnDeclNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnElementNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnMixedNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnPCDataNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnTypeDeclaration;
import org.bibalex.sdxe.suggest.model.dom.SugtnTypeDeclaration.XSDEnumMember;
import org.bibalex.uigen.icefaces.IceCompsFactory;
import org.bibalex.uigen.icefaces.UIGenerationException;
import org.jdom.JDOMException;

/**
 * Utility class for dynamically creating Icefaces components, validators and converters.
 * 
 * @author Younos.Naga
 * 
 */
public class SugtnIceCompsFactory {
	private static final Logger LOG = Logger.getLogger("org.bibalex.sdxe");
	// TODO: move to configuration
	public static boolean USE_PARTIAL_SUBMIT = true;
	private static String dateFormat = "yyyy/MM/dd G";
	
	public static UIInput createInput(UIPanel container, String id,
			SugtnTypeDeclaration sugtnType, IceCompsFactory compsFactory)
			throws UIGenerationException {
		return createInput(container, id, sugtnType, compsFactory, false);
	}
	
	/**
	 * Creates component according to type
	 * 
	 * @throws UIGenerationException
	 * 
	 * @throws JDOMException
	 */
	
	public static UIInput createInput(UIPanel container, String id,
			SugtnTypeDeclaration sugtnType, IceCompsFactory compsFactory,
			boolean large) throws UIGenerationException {
		
		UIInput result = null;
		
		if (sugtnType == null) {
			result = createTextInput(id, compsFactory, large);
		} else {
			FacesContext context = FacesContext.getCurrentInstance();
			Application application = context.getApplication();
			
			Class<?> clazz = sugtnType.getJavaClassForBaseType();
			
			Converter converter = application.createConverter(clazz);
			
			if (sugtnType.isEnumerated()) {
				// Lookup
				
				// TODONOT move to IceCompsFactory (NOT coz it's too complicated to refactor)
				UISelectItems uiSelectItems = (UISelectItems) FacesContext.getCurrentInstance()
						.getApplication().createComponent(UISelectItems.COMPONENT_TYPE);
				uiSelectItems.setId(id + "SelectItems");
				ArrayList<SelectItem> selectItemList = new ArrayList<SelectItem>();
				uiSelectItems.setValue(selectItemList);
				
				if (sugtnType.isListType()) {
					// UISelectMany
					if (sugtnType.getEnumeration().size() <= 3) {
						// This maybe more usable but it doesn't support scroll bars
						result = compsFactory.htmlSelectManyCheckbox(id, USE_PARTIAL_SUBMIT);
					} else {
						result = compsFactory.htmlSelectManyListbox(id,
								USE_PARTIAL_SUBMIT);
					}
					
				} else {
					
					// UISelectOne
					result = compsFactory.htmlSelectOneListbox(id,
							USE_PARTIAL_SUBMIT);
// YA 20101010 Always add the empty choice.. to adapt with auto add/delete if (!sugtnType.isRequired()) {
					selectItemList.add(new SelectItem(null, ""));
// YA 20101010 }
					
				}
				
				result.getChildren().add(uiSelectItems);
				
				for (XSDEnumMember enumMember : sugtnType.getEnumeration()) {
					
					Object convValue = converter == null ?
							enumMember.value :
							converter.getAsObject(context, result, enumMember.value);
					
					selectItemList.add(new SelectItem(convValue, enumMember.documentation));
					
				}
				
			} else if (boolean.class.equals(clazz)) {
				
				result = compsFactory.htmlSelectBooleanCheckbox(id,
						USE_PARTIAL_SUBMIT);
				
// TODO Date doesn't work properly
// } else if (Date.class.equals(clazz)
// || javax.xml.datatype.XMLGregorianCalendar.class.equals(clazz)) {
//
// result = compsFactory.selectInputDate(id, dateFormat, USE_PARTIAL_SUBMIT);
// Add the converter?
				
			} else { // TODONOT rest of supported clazzes
			
				// TODO Test this code
				if ((converter == null) &&
						(String[].class.equals(clazz))) {
					
					LOG
							.warn("Running code that was never tested in SugtnIceCompsFactory.java after if ((converter == null) && (String[].class.equals(clazz)))");
					
					converter = new Converter() {
						
						@Override
						public Object getAsObject(FacesContext arg0, UIComponent arg1,
								String str) {
							StringTokenizer tokens = new StringTokenizer(str, " ", false);
							String[] result = new String[tokens.countTokens()];
							int i = 0;
							while (tokens.hasMoreTokens()) {
								result[i++] = tokens.nextToken();
							}
							return result;
						}
						
						@Override
						public String getAsString(FacesContext arg0, UIComponent arg1,
								Object obj) {
							
							if (obj == null) {
								return null;
							}
							
							String result = "";
							
							for (String str : (String[]) obj) {
								result += str + " ";
							}
							
							if (result.length() > 0) {
								result = result.substring(0, result.length() - 1);
							}
							
							return result;
						}
					};
				}
				
				result = createTextInput(id, compsFactory, large);
				
			}
// YA 20100729 --> requirement enforcement is now manual:
// result.setRequired(sugtnType.isRequired());
			
			if (converter != null) {
				result.setConverter(converter);
			}
			
			// validation
			if (result instanceof EditableValueHolder) {
				
				// ////// LENGTH ////////
				LengthValidator lenValidator = null;
				
				if (sugtnType.getMaxLength() != -1) {
					lenValidator = new LengthValidator();
					lenValidator.setMaximum(sugtnType.getMaxLength());
				}
				
				if (sugtnType.getMinLength() != -1) {
					if (lenValidator == null) {
						lenValidator = new LengthValidator();
					}
					lenValidator.setMinimum(sugtnType.getMinLength());
				}
				
				if (lenValidator != null) {
					result.addValidator(lenValidator);
				}
				
				// // Range ////
				DoubleRangeValidator rangeValidator = null;
				if (sugtnType.getMinValue() != Double.MAX_VALUE) {
					rangeValidator = new DoubleRangeValidator();
					rangeValidator.setMinimum(sugtnType.getMinValue());
				}
				
				if (sugtnType.getMaxValue() != Double.MIN_VALUE) {
					if (rangeValidator == null) {
						rangeValidator = new DoubleRangeValidator();
					}
					rangeValidator.setMaximum(sugtnType.getMaxValue());
				}
				
				if (rangeValidator != null) {
					result.addValidator(rangeValidator);
				}
				
// This will make life hard when craeteing attrs popup.. add it for explicit fields only
// result.addValidator(new NotEmptyValidator());
// TODONE add validators
				
			}
			
		}
		
		// ////////////
		
		if (container != null) {
			container.getChildren().add(result);
		}
		
		return result;
	}
	
	public static UIInput createInput(UIPanel container, String id,
			SugtnTypeDeclaration sugtnType, SugtnDeclNode decl, IceCompsFactory compsFactory)
			throws UIGenerationException {
		boolean large = false;
		if (decl instanceof SugtnElementNode) {
			SugtnPCDataNode pcDataNode = ((SugtnElementNode) decl).getPcDataNode();
			if (pcDataNode != null) {
				large = pcDataNode instanceof SugtnMixedNode;
			}
		}
		return createInput(container, id, sugtnType, compsFactory, large);
	}
	
	private static UIInput createTextInput(String id, IceCompsFactory compsFactory,
			boolean large) {
		UIInput component;
		
		if (large) {
			component = compsFactory.htmlInputTextarea(id,
					USE_PARTIAL_SUBMIT);
		} else {
			component = compsFactory.htmlInputText(id,
					USE_PARTIAL_SUBMIT);
			
		}
		return component;
	}
	
	public static String getDateFormat() {
		return dateFormat;
	}
	
	public static void setDateFormat(String dateFormat) {
		SugtnIceCompsFactory.dateFormat = dateFormat;
	}
}
