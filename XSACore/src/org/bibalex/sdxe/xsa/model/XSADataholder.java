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

import java.util.List;

import org.bibalex.sdxe.xsa.exception.XSAException;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

/**
 * Representation for the &lt;dataHolder&gt; element in the XSA.xml
 * 
 * @author Younos.Naga
 * 
 */
public class XSADataholder extends XSALocated {
	public static String XPPARAM_IX = "ix";
	
	public static String XSA_DATAHOLDER_ELTNAME = "dataHolder";
	
	private final int instanceCount;
	
	private final XPath instGetXp;
	
	/**
	 * Create a new DataHolder object using the given dom element & locator
	 * @param domElt the dom element that holds the XSA dataholder 
	 * @param locator the XSA locator element for this data holder
	 * @throws JDOMException
	 * @throws XSAException
	 */
	public XSADataholder(Element domElt, XSALocator locator) throws JDOMException, XSAException {
		super(domElt, locator);
		
		// TODO: do same in all constructors
		if (!XSA_DATAHOLDER_ELTNAME.equals(domElt.getName())) {
			throw new XSAException("Element invalid");
		}
		
		String instCntStr = "count(" + this.pfx + "instance)";
		XPath instCntXp = XPath.newInstance(instCntStr);
		instCntXp.addNamespace(this.namespace);
		
		this.instanceCount = instCntXp.numberValueOf(domElt).intValue();
		
		String instGetStr = this.pfx + "instance[$" + XPPARAM_IX + "]";
		this.instGetXp = XPath.newInstance(instGetStr);
		
	}
	
	/**
	 * Gets an instance from inside the dataHolder, depending on its index. This is deprecated.
	 * 
	 * @deprecated
	 * @param instanceIx
	 * @return
	 * @throws JDOMException
	 * @throws XSAException
	 */
	@Deprecated
	public XSAInstance getInstance(int instanceIx) throws JDOMException, XSAException {
		this.instGetXp.setVariable(XPPARAM_IX, instanceIx);
		
		List xpRes = this.instGetXp.selectNodes(this.domRep);
		if (xpRes.size() != 1) {
			throw new XSAException("Invalid instance index");
		}
		
		XSAInstance result = XSAInstanceCache.getInstance().xsaInstanceForDomRep(
				(Element) xpRes.get(0));
		
		return result;
		
	}
	
	/**
	 * 
	 * @return The number of instances defined for this dataHolder
	 */
	public int getInstanceCount() {
		return this.instanceCount;
	}
}
