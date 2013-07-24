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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bibalex.jdom.XPathBuilder;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

public class JDTElementNode implements IJDTNode {
	
	/** the decorated Element */
	protected Element decorated;
	
	public JDTElementNode(Element element) {
		this.decorated = element;
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
		if (this.decorated.getParentElement() != null) {
			return new JDTElementNode(this.decorated.getParentElement());
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
	
	public String getQualifier() {
		return new XPathBuilder().buildElementQualifier(this.decorated, false);
		
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
		List list = this.decorated.getAttributes();
		ArrayList content = new ArrayList(list);
		
		// put the element's content in the list in order
		Iterator i = this.decorated.getContent().iterator();
		while (i.hasNext()) {
			content.add(i.next());
		}
		return content.iterator();
	}
	
	@Override
	public String toString() {
		return this.decorated.toString();
	}
}