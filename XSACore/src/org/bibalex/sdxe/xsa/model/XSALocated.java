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
import org.jdom.Element;
import org.jdom.JDOMException;

/**
 * 
 * This is class for whatever has a Locator that relates the XSA description to something in the XSD. This is currently
 * only the dataHolder elements, because annotators cannot be bound to a certain element declaration as they are used in
 * many places in the XML.
 * 
 * @author Younos.Naga
 * 
 */
public abstract class XSALocated extends XSANode {
	
	/**
	 * 
	 * This factory method creates the correct type of located object.. that is currently certainly a dataHolder
	 * 
	 * @param rule
	 * @param locator
	 * @return
	 * @throws XSAException
	 * @throws JDOMException
	 */
	public static XSALocated create(Element rule, XSALocator locator) throws XSAException,
			JDOMException {
		
		XSALocated result = null;
		
		if (XSADataholder.XSA_DATAHOLDER_ELTNAME.equals(rule.getName())) {
			result = new XSADataholder(rule, locator);
		} // else if () TODONOT : Annotator
		
		return result;
		
	}
	
	/**
	 *
	 * 
	 */
	private final XSALocator locator;
	
	/**
	 * 
	 * This extracts the locator from the DOM element
	 * 
	 * @param domElt
	 * @param locator
	 * @throws XSAException
	 */
	protected XSALocated(Element domElt, XSALocator locator) throws XSAException {
		super(domElt);
		this.locator = locator;
	}
	
	/**
	 * @return the locator
	 */
	public XSALocator getLocator() {
		return this.locator;
	}
}
