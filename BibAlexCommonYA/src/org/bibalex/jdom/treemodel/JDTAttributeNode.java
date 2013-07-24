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

package org.bibalex.jdom.treemodel;

import java.util.Collections;
import java.util.Iterator;

import org.bibalex.jdom.XPathBuilder;
import org.jdom.Attribute;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

public class JDTAttributeNode implements IJDTNode {
	
	/** The decorated attribute */
	protected Attribute decorated;
	
	public JDTAttributeNode(Attribute attribute) {
		this.decorated = attribute;
	}
	
	public Object getNode() {
		return this.decorated;
	}
	
	public String getNodeName() {
		if (this.decorated != null) {
			return this.decorated.getName();
		}
		return "";
	}
	
	public IJDTNode getParentNode() {
		if (this.decorated.getParent() != null) {
			return new JDTElementNode(this.decorated.getParent());
		}
		return null;
	}
	
	public String getQName() {
		if (this.decorated.getNamespacePrefix().equals("")) {
			return this.decorated.getName();
		} else {
			return new StringBuffer(this.decorated.getNamespacePrefix())
					.append(":")
					.append(this.decorated.getName()).toString();
		}
	}
	
	@Override
	public XPath getXPath() throws JDTException {
		
		try {
			return new XPathBuilder().buildXPath(this.decorated);
		} catch (JDOMException e) {
			throw new JDTException(e);
		}
	}
	
	@Override
	public String getXPathString() {
		return new XPathBuilder().buildXPathString(this.decorated);
	}
	
	public Iterator iterator() {
		return Collections.EMPTY_LIST.iterator();
	}
	
	@Override
	public String toString() {
		return this.decorated.toString();
	}
	
}
