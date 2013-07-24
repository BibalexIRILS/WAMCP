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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.component.UIPanel;

import org.bibalex.sdxe.binding.DomValueBinder;
import org.bibalex.sdxe.xsa.application.XSAConstants;
import org.bibalex.sdxe.xsa.uigen.icefaces.XSAUiGenFactoryAbstract.FaceletViewHandlerEmulator;
import org.bibalex.uigen.icefaces.DoNothingCompsDecorator;
import org.bibalex.uigen.icefaces.IceCompsFactory;

import com.icesoft.faces.component.ext.HtmlInputHidden;
import com.icesoft.faces.component.ext.HtmlPanelGroup;
import com.sun.facelets.Facelet;
import com.sun.facelets.FaceletException;
import com.sun.facelets.FaceletFactory;
import com.sun.facelets.compiler.SAXCompiler;

/**
 * This should be a managed bean in the session scope whose name is "xsaBindersBean". The bean contains all the objects
 * that are bounded to the UI.
 */
public class XSAUiBindersBean implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4750737745178714150L;
	
	/**
	 * Facelet templates are cached because disk I/O for reading them, with each apply, would degrade performance
	 * greatly
	 */
	private final HashMap<String, Facelet> faceletsChace = new HashMap<String, Facelet>();
	
	/**
	 * Maps ids to booleans that show or hide the popup for editing attributes
	 */
	private HashMap<String, Boolean> attrsPopupVisible = null;
	
	/**
	 * Maps each id to the binder bean that binds it to the DOM
	 */
	private HashMap<String, DomValueBinder> domValueBinders = null;
	
	/**
	 * Maps each id with the index of a fields occurrenct, or the currently viewed container occurrence
	 */
	private HashMap<String, Integer> ixBetBrosMap = null;
	
	/**
	 * The map that contains all dynamically generated containers
	 */
	private HashMap<String, UIComponent> dynComps = null;
	
	/**
	 * Bounded with the pop up for editing attribute
	 */
	private Collection<String> attrsPopupIds = null;
	private HashMap<String, UIPanel> attrsPopupFields = null;
	private HashMap<String, UIPanel> attrsPopupLabels = null;
	
	/**
	 * Used for adding Annotators in a field content
	 */
	private HtmlInputHidden lastActiveIdInpt = null;
	private HtmlInputHidden lastActiveLocatorInpt = null;
	private HtmlInputHidden selectionStartInpt = null;
	private HtmlInputHidden selectionEndInpt = null;
	private HashMap<String, Object> annotatorAttrsValueMap = null;
	
	// TODO replace this mess of maps by only a list of panels and a list of "BaseShiftPart.class"
	private final List<UIPanel> baseXPShiftedPanels;
	
	private final HashMap<String, Integer> baseXPShiftedCurrs;
	
	private final HashMap<String, Integer> baseXPShiftedMaxes;
	
	private final HashMap<String, Integer> baseXPShiftedTargets;
	
	private final LinkedList<String> baseXPShiftPatternParts;
	
	public XSAUiBindersBean() {
		this.baseXPShiftPatternParts = new LinkedList<String>();
		this.baseXPShiftedTargets = new HashMap<String, Integer>();
		this.baseXPShiftedMaxes = new HashMap<String, Integer>();
		this.baseXPShiftedCurrs = new HashMap<String, Integer>();
		this.baseXPShiftedPanels = new LinkedList<UIPanel>();
		this.attrsPopupVisible = new HashMap<String, Boolean>();
		this.domValueBinders = new HashMap<String, DomValueBinder>();
		this.ixBetBrosMap = new HashMap<String, Integer>();
		this.dynComps = new HashMap<String, UIComponent>();
		this.attrsPopupIds = new ArrayList<String>();
		this.attrsPopupFields = new HashMap<String, UIPanel>();
		this.attrsPopupLabels = new HashMap<String, UIPanel>();
		
	}
	
	public void addDynComp(String hldrId, UIComponent comp) {
		this.dynComps.put(hldrId, comp);
		
	}
	
	/**
	 * An input Component (or any other component) can be wrapped in a panel group, when adding it in the dynamic
	 * components map. This way the binding can be safely done via a panel group tag.
	 */
	public void addDynCompInHolder(String hldrId, UIComponent comp) {
		IceCompsFactory compsFactory = new IceCompsFactory(new DoNothingCompsDecorator());
		HtmlPanelGroup compHolder = (HtmlPanelGroup) compsFactory.panel(
				HtmlPanelGroup.COMPONENT_TYPE, null);
		compHolder.getChildren().add(comp);
		this.dynComps.put(hldrId, compHolder);
		
	}
	
	/**
	 * When the binders are not needed anymore, they should be cleared.
	 */
	public void clearBinders() {
		// The base shift binders are cleared in UiActionListern.intoArea this.baseXPShiftedPanels.clear();
		
		this.attrsPopupVisible.clear();
		this.domValueBinders.clear();
		this.ixBetBrosMap.clear();
	}
	
	/**
	 * When shifter maps are not needed anymore, they should be cleared.
	 */
	public void clearShifters() {
		this.baseXPShiftPatternParts.clear();
// this is with ClearUi this.baseXPShiftedPanels.clear();
		this.baseXPShiftedCurrs.clear();
		this.baseXPShiftedMaxes.clear();
		this.baseXPShiftedTargets.clear();
		
	}
	
	public HashMap<String, Object> getAnnotatorAttrsValueMap() {
		return this.annotatorAttrsValueMap;
	}
	
	/**
	 * @return the attrsPopupFields
	 */
	public HashMap<String, UIPanel> getAttrsPopupFields() {
		return this.attrsPopupFields;
	}
	
	/**
	 * @return the attrsPopupIds
	 */
	public Collection<String> getAttrsPopupIds() {
		return this.attrsPopupIds;
	}
	
	/**
	 * @return the attrsPopupLabels
	 */
	public HashMap<String, UIPanel> getAttrsPopupLabels() {
		return this.attrsPopupLabels;
	}
	
	/**
	 * @return the attrsPopupVisible
	 */
	public HashMap<String, Boolean> getAttrsPopupVisible() {
		return this.attrsPopupVisible;
	}
	
	public HashMap<String, Integer> getbaseXPShiftedCurrs() {
		return this.baseXPShiftedCurrs;
	}
	
	public HashMap<String, Integer> getBaseXPShiftedMaxes() {
		return this.baseXPShiftedMaxes;
	}
	
	/**
	 * Gets shifted base of XPath for the current area
	 */
	public String getBaseXPShiftedOfCurrArea() {
		String result = "";
		for (String part : this.baseXPShiftPatternParts) {
			result += part + "[" + this.baseXPShiftedCurrs.get(part) + "]";
		}
		return result;
	}
	
	public List<UIPanel> getBaseXPShiftedPanels() {
		return this.baseXPShiftedPanels;
	}
	
	public HashMap<String, Integer> getBaseXPShiftedTargets() {
		
		return this.baseXPShiftedTargets;
	}
	
	public LinkedList<String> getBaseXPShiftPatternParts() {
		return this.baseXPShiftPatternParts;
	}
	
	/**
	 * @return the domValueBinders
	 */
	public HashMap<String, DomValueBinder> getDomValueBinders() {
		return this.domValueBinders;
	}
	
	/**
	 * @return the dynComps
	 */
	public HashMap<String, UIComponent> getDynComps() {
		return this.dynComps;
	}
	
	/**
	 * Gets a Facelet from the cache, and reads it from the file passed
	 */
	public Facelet getFacelet(String relPath) throws FaceletException, FacesException, ELException,
			IOException {
		if (!this.faceletsChace.containsKey(relPath)) {
			
			FaceletFactory faceletFactory = FaceletViewHandlerEmulator
					.createFaceletFactory(new SAXCompiler());
			
			Facelet newFacelet = faceletFactory.getFacelet(relPath);
			
			this.faceletsChace.put(relPath, newFacelet);
		}
		
		return this.faceletsChace.get(relPath);
		
	}
	
	/**
	 * @return the ixBetBrosMap
	 */
	public HashMap<String, Integer> getIxBetBrosMap() {
		return this.ixBetBrosMap;
	}
	
	/**
	 * Gets a string of slash delimited number from the component id, each representing the index that would be added to the corresponding
	 * XPath step, starting from the first Container in the instances hierarchy
	 */
	public String getIxBetBrosPath(String id) {
		String result = "";
		if ((id != null) && !id.isEmpty()) {
			
			String parentsIxes = this.getIxBetBrosPath(id.substring(0, id
					.lastIndexOf(XSAConstants.IDCONVENTION_XPSTEP_DELIMITER)));
			
			if (this.ixBetBrosMap.containsKey(id)) {
				
				result = parentsIxes + "/" + this.ixBetBrosMap.get(id);
				
			} else {
				if (id.endsWith("UNBOUNDED")) {
					// this is not a part of the id that the BINDERBEAN should worry about
					result = parentsIxes;
				} else if (id
						.substring(
								id.lastIndexOf(XSAConstants.IDCONVENTION_XPSTEP_DELIMITER) + 1)
						.startsWith("" + XSAConstants.IDCONVENTION_ATTR_PREFIX)) {
					// Attribute; doesn't have an index, 0 will tell get XPStr not to append an ix
					result = parentsIxes + "/0";
				} else {
					if ((parentsIxes != null) && !parentsIxes.isEmpty()) {
						
						// Element child of container and parent of other ui but not in ui itself
						result = parentsIxes + "/1";
						
					} else {
						// not a child of any container and not a field itself, so we don't bother
					}
				}
			}
			
		}
		
		return result;
	}
	
	
	/**
	 * This method is used in adding annotators to the active element.
	 * @return the last element had the focus.
	 */
	public HtmlInputHidden getLastActiveIdInpt() {
		return this.lastActiveIdInpt;
	}
	
	public HtmlInputHidden getLastActiveLocatorInpt() {
		return this.lastActiveLocatorInpt;
	}
	
	public HtmlInputHidden getSelectionEndInpt() {
		return this.selectionEndInpt;
	}
	
	public HtmlInputHidden getSelectionStartInpt() {
		return this.selectionStartInpt;
	}
	
	public void setAnnotatorAttrsValueMap(HashMap<String, Object> annotatorAttrsValueMap) {
		this.annotatorAttrsValueMap = annotatorAttrsValueMap;
	}
	
	/**
	 * @param attrsPopupFields
	 *            the attrsPopupFields to set
	 */
	protected void setAttrsPopupFields(HashMap<String, UIPanel> attrsPopupFields) {
		this.attrsPopupFields = attrsPopupFields;
	}
	
	/**
	 * @param attrsPopupIds
	 *            the attrsPopupIds to set
	 */
	protected void setAttrsPopupIds(List<String> attrsPopupIds) {
		this.attrsPopupIds = attrsPopupIds;
	}
	
	/**
	 * @param attrsPopupLabels
	 *            the attrsPopupLabels to set
	 */
	protected void setAttrsPopupLabels(HashMap<String, UIPanel> attrsPopupLabels) {
		this.attrsPopupLabels = attrsPopupLabels;
	}
	
	public void setLastActiveIdInpt(HtmlInputHidden lastActiveIdInpt) {
		this.lastActiveIdInpt = lastActiveIdInpt;
	}
	
	public void setLastActiveLocatorInpt(HtmlInputHidden lastActiveLocatorInpt) {
		this.lastActiveLocatorInpt = lastActiveLocatorInpt;
	}
	
	public void setSelectionEndInpt(HtmlInputHidden selectionEndInpt) {
		this.selectionEndInpt = selectionEndInpt;
	}
	
	public void setSelectionStartInpt(HtmlInputHidden selectionStartInpt) {
		this.selectionStartInpt = selectionStartInpt;
	}
	
}
