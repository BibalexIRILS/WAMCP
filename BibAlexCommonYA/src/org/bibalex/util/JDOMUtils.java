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

package org.bibalex.util;

import java.io.IOException;
import java.io.StringWriter;

import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class JDOMUtils {
	
	public static String getElementContentAsString(Element elt, Format format) throws IOException {
		String result = null;
		
		// There are no children.. just text if any, so return it!
		if (elt.getChildren().isEmpty()) {
			return elt.getTextTrim();
		}
		
		StringWriter pcDataWriter = new StringWriter();
		XMLOutputter outputter = new XMLOutputter(format.setEncoding("UTF-8"));
		
		outputter.output(elt, pcDataWriter);
		pcDataWriter.flush();
		pcDataWriter.close();
		result = pcDataWriter.toString();
		result = result.trim();
		
		result = result.substring(result.indexOf('>') + 1);
		result = result.substring(0, result.lastIndexOf('<'));
		
		return result;
	}
	
	public static Namespace getElementNamespace(Element elt) {
		String nsUri = elt.getNamespaceURI();
		
		if ((nsUri == null) || nsUri.isEmpty()) {
			return Namespace.NO_NAMESPACE;
		} else if (Namespace.XML_NAMESPACE.getURI().equals(nsUri)) {
			return Namespace.XML_NAMESPACE;
		} else {
			return Namespace.getNamespace(nsUri);
		}
		
	}
	
	public static String getNodeValue(Object obj, boolean escaped) throws IOException {
		String result = null;
		if (obj instanceof Element) {
			result = getElementContentAsString((Element) obj, Format.getCompactFormat());
			if (escaped) {
				result = result.replaceAll("<", "&lt;");
				result = result.replaceAll(">", "&gt;");
			}
		} else if (obj instanceof Attribute) {
			result = ((Attribute) obj).getValue();
		}
		return result;
	}
}
