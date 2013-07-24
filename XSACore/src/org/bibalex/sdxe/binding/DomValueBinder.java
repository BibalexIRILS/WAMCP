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

package org.bibalex.sdxe.binding;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.bibalex.sdxe.controller.DomTreeController;
import org.bibalex.sdxe.exception.DomBuildException;
import org.bibalex.sdxe.suggest.model.dom.SugtnDomAddableNode;

/**
 * Abstract bean that binds a DOM controller to the UI.
 * 
 * @author Younos.Naga
 * 
 */
@SuppressWarnings("serial")
public abstract class DomValueBinder implements Serializable {
	/**
	 * The value of the field
	 */
	protected String value;
	/**
	 * value to indicate whether the value was changed or not
	 */
	protected boolean valueChanged = false;
	/**
	 * value to indicate if this binding was initialized
	 */
	protected boolean initialized = false;
	
	protected DomTreeController domTarget = null;	
	protected SugtnDomAddableNode sugtnNodeForTarget;	
	protected String valueListDelimiter = " ";
	
	/**
	 * @param domTarget
	 * @param sugtnNodeForTarget
	 */
	public DomValueBinder(DomTreeController domTarget, SugtnDomAddableNode sugtnFor) {
		this.domTarget = domTarget;
		this.sugtnNodeForTarget = sugtnFor;
// must lazy init: this.valueFromDom();
		
		this.valueChanged = false;
	}
	
	protected abstract void doValueFromDom();
	
	protected abstract void doValueToDom() throws DomBuildException;
	
	/**
	 * @return the sugtnNodeForTarget
	 */
	public SugtnDomAddableNode getSugtnNodeForTarget() {
		return this.sugtnNodeForTarget;
	}
	
	/**
	 * @return the value
	 */
	public String getValue() {
		if (!this.initialized) {
			this.valueFromDom();
			this.initialized = true;
		}
		return this.value;
	}
	
	public Boolean getValueChanged() {
		
		return Boolean.valueOf(this.valueChanged);
	}
	
	public List<String> getValueList() {
		// returns empty strings this.getValue().split(this.valueListDelimiter)
		
		StringTokenizer tokens = new StringTokenizer(this.getValue(), this.valueListDelimiter,
				false);
// if (tokens.countTokens() == 0) {
// return null;
// }
		List<String> result = new ArrayList<String>();
		
		while (tokens.hasMoreTokens()) {
			result.add(tokens.nextToken());
		}
		return result;
	}
	
	public String getValueListDelimiter() {
		return this.valueListDelimiter;
	}
	
	/**
	 * @return the valueChanged
	 */
	public boolean isValueChanged() {
		return this.valueChanged;
	}
	
	/**
	 * @param value
	 *            the value to set
	 */
	public final void setValue(String value) {
		if (((this.value == null) && (value == null))
				|| ((this.value != null) && this.value.equals(value))) {
			
			return; // don't cause an unnecessary change
			
		}
		
		this.value = value;
		
		this.valueChanged = true;
	}
	
	public final void setValueList(List<String> valueListNew) {
		String valueNew = null;
		
		if (valueListNew.size() == 0) {
			valueNew = "";
		} else {
			StringBuffer sb = new StringBuffer();
			sb.append(valueListNew.get(0));
			for (int i = 1; i < valueListNew.size(); i++) {
				sb.append(this.valueListDelimiter);
				sb.append(valueListNew.get(i));
			}
			valueNew = sb.toString();
		}
		
		this.setValue(valueNew);
	}
	
	public void setValueListDelimiter(String valueListDelimiter) {
		this.valueListDelimiter = valueListDelimiter;
	}
	
	public void syncWithDom(boolean bApply) throws DomBuildException {
		if (bApply) {
			this.valueToDom();
		} else {
			this.valueFromDom();
		}
		
	}
	
	public final void valueFromDom() {
		this.valueChanged = false;
		this.doValueFromDom();
	}
	
	public final void valueToDom() throws DomBuildException {
		if (this.valueChanged) {
			
			this.valueChanged = false;
			this.doValueToDom();
			
		} // else //don't cause an unnecessary commit
	}
	
}
