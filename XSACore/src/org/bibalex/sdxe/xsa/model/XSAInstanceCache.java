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

import java.util.Map;
import java.util.WeakHashMap;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;
import org.bibalex.sdxe.xsa.application.XSAConstants;
import org.bibalex.sdxe.xsa.exception.XSAException;
import org.jdom.Element;
import org.jdom.JDOMException;

/**
 * 
 * A cache for XSA Instance objects that ONE USER can access; i.e. must be a Session Bean. Some Instance methods depend
 * on the current Base Shift values of the page the user is currently viewing, so they cannot be shared among users.
 * 
 * Instances are cached keyed by the DOM element of the Instance they represent
 * 
 * @author Younos.Naga
 * 
 */
public class XSAInstanceCache {
	// TODO Refactor this and XSADocCache to be DRY.. but how to inherit when there are static methods that return
// generic types??
	private static final Logger LOG = Logger.getLogger("org.bibalex.xsa");
	
	/**
	 * 
	 * @return an instance of the cache object. Sorry for overloading the word Instance
	 * 
	 */
	public static XSAInstanceCache getInstance() {
		
		FacesContext fCtx = FacesContext.getCurrentInstance();
		if (fCtx == null) { // when unit testing
			return new XSAInstanceCache();
		}
		ELContext elCtx = fCtx.getELContext();
		ExpressionFactory exprFactory = fCtx.getApplication().getExpressionFactory();
		
		ValueExpression XSADocCacheEl = exprFactory.createValueExpression(elCtx,
				"#{"
						+ XSAConstants.BEANNAME_XSAINSTANCECache + "}",
				XSAInstanceCache.class);
		
		XSAInstanceCache result = (XSAInstanceCache) (XSADocCacheEl
				.getValue(elCtx));
		
		return result;
		
	}
	
	// This is a per session cache, so only one thread access it, so ignore:
	// TODONOT if there would ever be 2 instances of XSA running on the same JRE,
	// will they ever have the same Element key for two different XSAInsts?
	
	private Map<Element, XSAInstance> cache = null;
	protected boolean allowWAMCPSpecific = false;
	
	/**
	 * 
	 */
	public XSAInstanceCache() {
		// This is a per session cache, so only one thread access it
// this.cache = Collections
// .synchronizedMap(new WeakHashMap<Element, XSAInstance>());
		this.cache = new WeakHashMap<Element, XSAInstance>();
		
	}
	
	public boolean isAllowWAMCPSpecific() {
		return this.allowWAMCPSpecific;
	}
	
	public void setAllowWAMCPSpecific(boolean allowWAMCPSpecific) {
		this.allowWAMCPSpecific = allowWAMCPSpecific;
	}
	
	/**
	 * The only correct way of creating an Instance object is through this factory method.
	 * 
	 * @param domInstance
	 * @return
	 * @throws XSAException
	 * @throws JDOMException
	 */
	public XSAInstance xsaInstanceForDomRep(Element domInstance) throws XSAException, JDOMException {
		
		XSAInstance result = null;
		
		// Maybe this would degrade performance drastically
		// so we will just depend on the collections synchornization of the map
		
// synchronized (this.cache) {
		if (this.cache.containsKey(domInstance)) {
			result = this.cache.get(domInstance);
		} else {
			result = new XSAInstance(domInstance);
			
			result.setAllowWAMCPSpecific(this.allowWAMCPSpecific);
			
			this.cache.put(domInstance, result);
		}
// }
		
		return result;
		
	}
}
