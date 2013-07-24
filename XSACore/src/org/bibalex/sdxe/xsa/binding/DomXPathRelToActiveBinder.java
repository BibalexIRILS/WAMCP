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

package org.bibalex.sdxe.xsa.binding;

import org.apache.log4j.Logger;
import org.bibalex.sdxe.binding.DomValueBinder;
import org.bibalex.sdxe.controller.DomTreeController;
import org.bibalex.sdxe.exception.DomBindingException;
import org.bibalex.sdxe.exception.DomBuildException;
import org.bibalex.sdxe.exception.SDXEException;
import org.bibalex.sdxe.suggest.model.dom.SugtnDomAddableNode;
import org.bibalex.util.XPathStrUtils;
import org.jdom.JDOMException;

/**
 * For binding fields according to an XPath of a dom node. The XPath used to bind the field can be shown by setting the
 * level of logger "org.bibalex.sdxe.xsa.binding" below DEBUG.
 * 
 * @author Younos.Naga
 * 
 */
public class DomXPathRelToActiveBinder extends DomValueBinder {
	private static final Logger LOG = Logger.getLogger("org.bibalex.sdxe.xsa.binding");
	
	protected String xPathStr = null;
	
	protected String locatorStr = null;
	
	public DomXPathRelToActiveBinder(String xPathStr, DomTreeController domTarget,
			SugtnDomAddableNode sugtnFor, String xsaLocatorStr) {
		super(domTarget, sugtnFor);
		
		if (!xPathStr.startsWith("/")) {
			xPathStr = "/" + xPathStr;
		}
		
		// make sure that the XPath is always that of an element
		if (XPathStrUtils.getLastStep(xPathStr).startsWith("@")) {
			xPathStr = XPathStrUtils.removeLastStep(xPathStr);
		}
		
		this.xPathStr = xPathStr;
		
		this.locatorStr = xsaLocatorStr;
	}
	
	/**
	 * This method is used for setting the value attribute which is previewed on UI with the content of 
	 * the node, it uses the method {@link org.bibalex.sdxe.controller.DomTreeController #getValueAtXPathRelToActive(String, SugtnDomAddableNode)} 
	 * to get this value.
	 * 
	 */
	@Override
	protected void doValueFromDom() {
		try {
			// TODONOT setIx must be called to get new value
			
			this.value = this.domTarget.getValueAtXPathRelToActive(this.xPathStr, // this.appendIxToXPath(),
					this.sugtnNodeForTarget);
			
			if (LOG.isDebugEnabled()) {
				this.value = this.value + " {" + this.xPathStr + "}";
			}
			// DEBUG this.value += " FROM " + this.xPathStr;
		} catch (DomBuildException e) {
			this.value = "EXCEPTION!";
			LOG.error(e, e);
		} catch (JDOMException e) {
			this.value = "EXCEPTION!";
			LOG.error(e, e);
		} catch (SDXEException e) {
			this.value = "EXCEPTION!";
			LOG.error(e, e);
		}
		
	}
	
	/**
	 * This method is used for writing the value attribute to the node it uses the method 
	 * {@link org.bibalex.sdxe.controller.DomTreeController #setValueAtXPathRelToActive(String, String, SugtnDomAddableNode)} 
	 * to write this value.
	 * 
	 */
	@Override
	protected void doValueToDom() throws DomBuildException {
		try {
			String val = this.value;
			if (val.endsWith(" {" + this.xPathStr + "}")) {
				int valEndIx = val.length() - (" {" + this.xPathStr + "}").length() - 1;
				if (valEndIx > 0) {
					val = val.substring(0, valEndIx);
				} else {
					val = "";
				}
			}
			this.domTarget.setValueAtXPathRelToActive(this.xPathStr, // this.appendIxToXPath(),
					val,
					this.sugtnNodeForTarget);
			
		} catch (JDOMException e) {
			LOG.error(e, e);
		} catch (DomBindingException e) {
			LOG.error(e, e);
		}
		
	}
	
	/**
	 * @return the locatorStr
	 */
	public String getLocatorStr() {
		return this.locatorStr;
	}
	
	/**
	 * @return the xPathStr
	 */
	public String getxPathStr() {
		return this.xPathStr;
	}
	
}
