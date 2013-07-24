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

package org.bibalex.jdom;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.bibalex.util.KeyValuePair;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;

public class XPathBuilder {
	public static final String DEFAULT_NS_XPATH_PREFIX = "dns";
	
	public static String addNamespacePrefix(String origXPStr, Namespace ns) {
		String pfx = getPrefixForNamespace(ns, true);
		
		return addNamespacePrefix(origXPStr, pfx);
		
	}
	
	public static String addNamespacePrefix(String origXPStr, String pfx) {
		String result = null;
		StringTokenizer tokens = new StringTokenizer(origXPStr, "/", false);
		
		StringBuilder xpStringBuilder = new StringBuilder();
		if (origXPStr.startsWith("//")) {
			xpStringBuilder.append("//");
		} else if (origXPStr.startsWith("/")) {
			xpStringBuilder.append("/");
		}
		
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken();
			
			if (token.startsWith("@")) {
				xpStringBuilder.append(token).append("/");
			} else {
				xpStringBuilder.append(pfx).append(token).append("/");
			}
		}
		
		if (xpStringBuilder.length() > 0) {
			result = xpStringBuilder.substring(0, xpStringBuilder.length() - 1);
		} else {
			result = ""; // never null!
		}
		
		return result;
	}
	
	public static String getPrefixForNamespace(Namespace ns, boolean useDefaultNSXPathPrefix) {
		
		String result = ns.getPrefix();
		
		if ((result != null) && !result.isEmpty()) {
			
			result += ":";
			
		} else {
			
			if (useDefaultNSXPathPrefix && !Namespace.NO_NAMESPACE.equals(ns)) {
				result = DEFAULT_NS_XPATH_PREFIX + ":";
			}
			
		}
		
		return result;
	}
	
	private final List<KeyValuePair<String, String>> pfxUriPairs = new LinkedList<KeyValuePair<String, String>>();
	
	private XPath addNamespaces(XPath result) {
		for (KeyValuePair<String, String> pfxUri : this.pfxUriPairs) {
			Namespace ns = Namespace.getNamespace(pfxUri.key, pfxUri.value);
			result.addNamespace(ns);
		}
		return result;
	}
	
	public String buildElementQualifier(Element elt, boolean useDefaultNSXPathPrefix) {
		String eltPfx = this.getPrefixForNamespaceAndAppendToList(elt.getNamespace(),
				useDefaultNSXPathPrefix);
		
		String result = eltPfx + elt.getName();
		
		String predicate = "";
		
		Attribute idAttr = elt.getAttribute("id", Namespace.XML_NAMESPACE);
		if (idAttr != null) {
			predicate = "@xml:id='" + idAttr.getValue() + "'";
			predicate = "[" + predicate + "]";
			result += predicate;
		} else {
			
			for (Object obj : elt.getAttributes()) {
				
				Attribute attr = (Attribute) obj;
				
				if ("http://www.w3.org/2001/XMLSchema-instance".equals(attr.getNamespaceURI())) {
					continue;
				}
				
				String attrPfx = this.getPrefixForNamespaceAndAppendToList(attr.getNamespace(),
						useDefaultNSXPathPrefix);
				
				predicate += "@" + attrPfx + attr.getName() + "='" + attr.getValue() + "' and ";
			}
			
			if (!predicate.isEmpty()) {
				predicate = predicate.substring(0, predicate.lastIndexOf(" and "));
				predicate = "[" + predicate + "]";
				result += predicate;
			}
			
		}
		
		return result;
	}
	
	public XPath buildXPath(Attribute target) throws JDOMException {
		String xpStr = this.buildXPathString(target);
		XPath result = XPath.newInstance(xpStr);
		
		this.addNamespaces(result);
		
		return result;
	}
	
	public XPath buildXPath(Element target) throws JDOMException {
		String xpStr = this.buildXPathString(target);
		XPath result = XPath.newInstance(xpStr);
		
		this.addNamespaces(result);
		
		return result;
	}
	
	public String buildXPathString(Attribute target) {
		String result = this.buildXPathString(target.getParent());
		
		String attrPfx = this.getPrefixForNamespaceAndAppendToList(target.getNamespace(), true);
		
		result += "/@" + attrPfx + target.getName();
		
		return result;
	}
	
	public String buildXPathString(Element target) {
		String result = "";
		
		Element parent = target.getParentElement();
		Element child = target;
		
		while (child != null) {
			
			String childXPathName = this.buildElementQualifier(child, true);
			
			int ix = parent != null ? parent.getChildren(child.getName(), child.getNamespace())
					.indexOf(child) + 1 : 1;
			
			String temp = "/" + childXPathName + "[" + ix + "]";
			
			result = temp + result;
			
			child = parent;
			if (parent != null) {
				parent = parent.getParentElement();
			}
			
		}
		
		return result;
	}
	
	public String getPrefixForNamespaceAndAppendToList(Namespace ns,
			boolean useDefaultNSXPathPrefix) {
		String result = getPrefixForNamespace(ns, useDefaultNSXPathPrefix);
		
		KeyValuePair<String, String> pfxUri = new KeyValuePair<String, String>(result, ns.getURI());
		if (this.pfxUriPairs.indexOf(pfxUri) == -1) {
			this.pfxUriPairs.add(pfxUri);
		}
		
		return result;
	}
}
