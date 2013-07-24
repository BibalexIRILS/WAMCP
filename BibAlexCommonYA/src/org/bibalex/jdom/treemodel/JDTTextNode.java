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
import org.jdom.Element;
import org.jdom.xpath.XPath;

public class JDTTextNode implements IJDTNode {
	
	/** The decorated String */
	protected String decorated;
	
	/** The manually set parent of this string content */
	private Element parent = null;
	
	public JDTTextNode(String string) {
		this.decorated = string;
	}
	
	private String appendTextPart(String xPathString) {
		
		return xPathString + "/child::text()";
	}
	
	public Object getNode() {
		return this.decorated;
	}
	
	public String getNodeName() {
		return "";
	}
	
	public IJDTNode getParentNode() {
		if (this.parent == null) {
			throw new RuntimeException(
					"The parent of this String content has not been set!");
		}
		return new JDTElementNode(this.parent);
	}
	
	public String getQName() {
		// text nodes have no name
		return "";
	}
	
	@Override
	public XPath getXPath() throws JDTException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String getXPathString() {
		return this.appendTextPart(new XPathBuilder().buildXPathString(((Element) this
				.getParentNode()
				.getNode())));
	}
	
	public Iterator iterator() {
		return Collections.EMPTY_LIST.iterator();
	}
	
	public JDTTextNode setParent(Element parent) {
		this.parent = parent;
		return this;
	}
	
	@Override
	public String toString() {
		return this.decorated;
	}
	
}