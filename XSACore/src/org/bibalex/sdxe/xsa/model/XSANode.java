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

import org.bibalex.sdxe.controller.DomTreeController;
import org.bibalex.sdxe.xsa.exception.XSAException;
import org.jdom.Element;
import org.jdom.Namespace;

/**
 * 
 * If a class is created to represent any XML element in the XSA.xml document, it extends this class
 * 
 * There are utility methods, specifically to handle namespace.
 * 
 * @author Younos.Naga
 * 
 */
public class XSANode {
	/**
	 * The DOM representation of the XSA node; that is, the original XML element
	 */
	protected Element domRep;
	
	/**
	 * The namespace that should be used in XPaths manipulating the domRep
	 */
	protected Namespace namespace;
	
	/**
	 * The prefix that should be used in XPaths manipulating the domRep
	 */
	protected String pfx;
	
	/**
	 * Needed by the Faces beans container
	 */
	public XSANode() {
		// Needed by Beans container
	}
	
	/**
	 * Constructor that sets the namespace and prefix according to what they should be for XPath
	 * 
	 * @param domRep
	 * @throws XSAException
	 */
	public XSANode(Element domRep) throws XSAException {
		
		if (!XSADocument.XSA_NAMESPACE_URI.equals(domRep.getNamespaceURI())) {
			throw new XSAException("Element invalid");
		}
		
		this.domRep = domRep;
		
		this.setNamespace(DomTreeController.acquireNamespace(XSADocument.XSA_NAMESPACE_URI,
				this.domRep,
				true));
	}
	
	/**
	 * 
	 * @return
	 */
	public Element getDomRep() {
		return this.domRep;
	}
	
	/**
	 * @return the namespace
	 */
	public Namespace getNamespace() {
		return this.namespace;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getPfx() {
		return this.pfx;
	}
	
	/**
	 * 
	 * @param domRep
	 */
	public void setDomRep(Element domRep) {
		this.domRep = domRep;
	}
	
	/**
	 * Sets the namespace, and the prefix according to the prefix stored in the namespace
	 * 
	 * @param namespace
	 *            the namespace to set
	 */
	public void setNamespace(Namespace namespace) {
		this.namespace = namespace;
		
		this.pfx = this.namespace.getPrefix();
		
		if ((this.pfx != null) && !this.pfx.isEmpty()) {
			this.pfx = this.pfx + ":";
		}
		
	}
	
	/**
	 * 
	 * @param pfx
	 */
	public void setPfx(String pfx) {
		this.pfx = pfx;
	}
	
}
