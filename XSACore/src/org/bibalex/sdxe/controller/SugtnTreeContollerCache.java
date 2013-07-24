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

package org.bibalex.sdxe.controller;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.bibalex.sdxe.exception.SugtnException;
import org.bibalex.sdxe.suggest.SugtnTreeBuilderCache;
import org.bibalex.sdxe.xsa.application.XSAConstants;

/**
 * This is not really a cache any more, as it was decided to reduce caching to reduce concurrency issues, and lock
 * waits.
 * However this still contains the code to get hold of a sugtn controller
 * 
 * @author Younos.Naga
 * 
 */
public class SugtnTreeContollerCache {
	private static final Logger LOG = Logger.getLogger("org.bibalex.sdxe");
	
	/**
	 * Uses the container to get hold of its managed bean, through a JSF EL Expression. This bean was intended to be the
	 * cache of SugtnTreeControllers
	 * 
	 * @return The managed bean called "sugtnTreeContollerCache"
	 */
	public static SugtnTreeContollerCache getInstance() {
		
		FacesContext fCtx = FacesContext.getCurrentInstance();
		ELContext elCtx = fCtx.getELContext();
		ExpressionFactory exprFactory = fCtx.getApplication().getExpressionFactory();
		
		ValueExpression SugtnTreeContollerCacheEl = exprFactory.createValueExpression(elCtx,
				"#{"
						+ XSAConstants.BEANNAME_SugtnTreeContollerCache + "}",
				SugtnTreeContollerCache.class);
		
		SugtnTreeContollerCache result = (SugtnTreeContollerCache) SugtnTreeContollerCacheEl
				.getValue(elCtx);
		
		return result;
		
	}
	
// private Map<String, SugtnTreeController> sugtnTreeCache = null;
	
// public SugtnTreeContollerCache() {
// this.sugtnTreeCache = Collections
// .synchronizedMap(new WeakHashMap<String, SugtnTreeController>());
// }
	/**
	 * Uses the container to get hold of its managed bean, through a JSF EL Expression. This bean was intended to be the
	 * cache of the SugtnTreeBuilders.
	 * 
	 * @return A SugtnTreeController for the schema at the passed schema, with the element whose local name and
	 *         namespace are passed as root
	 */
	public SugtnTreeController sugtnTreeControllerForSchema(String schemaPath, String nameSpaceURI,
			String rootEltName) throws SugtnException {
		
		SugtnTreeController result = null;
		String key = schemaPath + nameSpaceURI + rootEltName;
		
// synchronized (this.sugtnTreeCache) {
		
// if (this.sugtnTreeCache.containsKey(key)) {
//
// result = this.sugtnTreeCache.get(key);
//
// } else {
		
		FacesContext fCtx = FacesContext.getCurrentInstance();
		ELContext elCtx = fCtx.getELContext();
		ExpressionFactory exprFactory = fCtx.getApplication().getExpressionFactory();
		
		ValueExpression SugtnTreeBuilderCacheEl = exprFactory.createValueExpression(
				elCtx,
				"#{"
						+ XSAConstants.BEANNAME_SugtnTreebuilderCache + "}",
				SugtnTreeBuilderCache.class);
		
		SugtnTreeBuilderCache builderCache = (SugtnTreeBuilderCache) SugtnTreeBuilderCacheEl
				.getValue(elCtx);
		
		result = new SugtnTreeController(builderCache);
		
		ServletContext servletContext = (ServletContext) FacesContext
				.getCurrentInstance().getExternalContext().getContext();
		
		String schemaFilePath = servletContext.getRealPath(schemaPath);
		result.initialize(schemaFilePath, nameSpaceURI, rootEltName);
		
// this.sugtnTreeCache.put(key, result);
//
// }
// }
		return result;
	}
	
}
