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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.bibalex.sdxe.xsa.application.XSAConstants;
import org.jdom.JDOMException;

/**
 * A cache of XSA.xml document models
 * 
 * @author Younos.Naga
 * 
 */
public class XSADocCache {
	private static final Logger LOG = Logger.getLogger("org.bibalex.xsa");
	
	/**
	 * Gets an Instance of this Cache object. The word instance is overloaded.
	 * 
	 * @return
	 */
	public static XSADocCache getInstance() {
		
		FacesContext fCtx = FacesContext.getCurrentInstance();
		ELContext elCtx = fCtx.getELContext();
		ExpressionFactory exprFactory = fCtx.getApplication().getExpressionFactory();
		
		ValueExpression XSADocCacheEl = exprFactory.createValueExpression(elCtx,
				"#{"
						+ XSAConstants.BEANNAME_XSADOCCache + "}",
				XSADocCache.class);
		
		XSADocCache result = (XSADocCache) (XSADocCacheEl
				.getValue(elCtx));
		
		return result;
		
	}
	
	private Map<String, XSADocument> cache = null;
	/**
	 * Constructor to create new empty XSADocCashe
	 */
	public XSADocCache() {
		this.cache = Collections
				.synchronizedMap(new WeakHashMap<String, XSADocument>());
		
	}
	
	/**
	 * For any schema file schema1.xsd the XSA.xml file should be called schema1.xsd.xsa.xml and placed in the same
	 * folder.
	 * 
	 * @param schemaPath
	 * @return The XSA.xml document object for the schema file passed.
	 */
	public XSADocument xsaDocForSchema(String schemaPath) {
		
		XSADocument result = null;
		synchronized (this.cache) {
			
			if (this.cache.containsKey(schemaPath)) {
				
				result = this.cache.get(schemaPath);
				
			} else {
				
				try {
					
					result = new XSADocument();
					
					ServletContext servletContext = (ServletContext) FacesContext
							.getCurrentInstance().getExternalContext().getContext();
					
					String xsaFilePath = servletContext.getRealPath(schemaPath
							+ ".xsa.xml");
					result.init(new File(xsaFilePath));
					
					this.cache.put(schemaPath, result);
					
				} catch (IOException e) {
					LOG.fatal(e, e);
				} catch (JDOMException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
		return result;
	}
}
