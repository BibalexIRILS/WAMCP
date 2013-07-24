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
import org.jdom.Namespace;

/**
 * Representation for the &lt;grou&gt; XSA.xml element
 * 
 * @author Younos.Naga
 * 
 */
public class XSAGroup extends XSAMessagesContainer {
	
	/**
	 * @param domInstance
	 * @throws XSAException
	 * @throws JDOMException
	 */
	public XSAGroup(Element domInstance) throws XSAException, JDOMException {
		super(domInstance);
	}
	
	/**
	 * 
	 * @return the group id
	 */
	public String getId() {
		return this.domRep.getAttributeValue("id", Namespace.XML_NAMESPACE);
	}
	
	/**
	 * 
	 * @return the index in UI as explicitly identified in the mandatory ixInUi attribute
	 */
	public int getIxInUi() {
		return Integer.parseInt(this.domRep.getAttributeValue("ixInUi"));
	}
	
	/**
	 * 
	 * @return true unless an attribute decorated is set to "no"
	 */
	public boolean isDecorate() {
		return !"no".equalsIgnoreCase(this.domRep.getAttributeValue("decorate"));
	}
}
