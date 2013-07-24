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

package org.bibalex.sdxe.xsa.model;

import org.bibalex.sdxe.xsa.exception.XSAException;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;

/**
 * @author 
 * The XSA Annotator Object 
 */
public class XSAAnnotator extends XSAInstance {
	/**
	 * the element name of the Annotator object
	 */
	protected final String eltLocalName;
	
	protected XSAInstance orig;
	
	/**
	 * Constructor for the Annotator 
	 * @param domInstance the dom element that represents the annotator object in the xsa
	 * @throws JDOMException
	 * @throws XSAException
	 */
	protected XSAAnnotator(Element domInstance) throws JDOMException,
			XSAException {
		super(domInstance);
		this.eltLocalName = domInstance.getAttribute("eltName").getValue();
		
	}
	/**
	 * Check if the Annotator allow text input or not
	 * @return true if allow text in the annotator tag, false otherwise
	 */
	public boolean allowsText() {
		boolean result = true;
		Attribute textAttr = this.domRep.getAttribute("text");
		if (textAttr != null) {
			result = !"no".equalsIgnoreCase(textAttr.getValue());
		}
		return result;
	}
	
	@Override
	public XSAInstance getContainingInst() throws XSAException {
		// Annotators can't be contained
		return null;
	}
	
	/**
	 * get the annotator element name
	 * @return annotator element name
	 */
	public String getEltLocalName() {
		return this.eltLocalName;
	}
	
// @Override
// public String getLabel(String locale) throws JDOMException, XSAException {
//
// String result = super.getLabel(locale);
// if ((result == null) || result.isEmpty()) {
// result = "TEXT";
// }
// return result;
// }
	
	@Override
	public XSALocator getLocator() {
		
		return new XSALocator(this.eltLocalName);
	}
	
	@Override
	public String toString() {
		
		return this.eltLocalName;
	}
}
