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
 * Representation for the &lt;messages&gt; element
 * 
 * @author Younos.Naga
 * 
 */
public class XSAMessages extends XSANode {
	/**
	 * 
	 * @param domRep
	 * @throws XSAException
	 */
	public XSAMessages(Element domRep) throws XSAException {
		super(domRep);
	}
	
	/**
	 * 
	 * @param msgName
	 * @return
	 * @throws JDOMException
	 */
	public String getMessage(String msgName) throws JDOMException {
		String result = this.domRep.getChildText(msgName, this.namespace);
		return result;
	}
	
}
