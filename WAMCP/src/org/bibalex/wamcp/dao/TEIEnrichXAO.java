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

package org.bibalex.wamcp.dao;

import org.bibalex.sdxe.controller.DomTreeController;
import org.bibalex.sdxe.xsa.exception.XSAEnforceException;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;

public class TEIEnrichXAO {
	public static String facsGraphicNameForFacsRef(String facsRef, Document teiXml)
			throws JDOMException, XSAEnforceException {
		if ((facsRef == null) || facsRef.isEmpty()) {
			return null;
		}
		
		if (!facsRef.startsWith("#")) {
			throw new XSAEnforceException("A reference to an XML Id must be prefixed by '#'");
		}
		
		String facsId = facsRef.substring(1);
		
		Element root = teiXml.getRootElement();
		
		Namespace nsXp = DomTreeController.acquireNamespace(root
				.getNamespaceURI(), root, true);
		String pfx = nsXp.getPrefix();
		if ((pfx != null) && !pfx.isEmpty()) {
			pfx += ":";
		}
		
		String facsNameForIdStr = "//" + pfx + "facsimile/" + pfx + "surface[@xml:id=$VARID]/"
				+ pfx + "graphic/@url";
		
		XPath facsNameForIdXP = XPath.newInstance(facsNameForIdStr);
		facsNameForIdXP.addNamespace(nsXp);
		
		facsNameForIdXP.setVariable("VARID", facsId);
		
		Object facsNameObj = facsNameForIdXP.selectSingleNode(teiXml);
		
		if (facsNameObj == null) {
			throw new XSAEnforceException("A reference for an image that doesn't exist: "
					+ facsId);
		}
		
		String facsName = ((Attribute) facsNameObj).getValue();
		
		return facsName;
	}
}
