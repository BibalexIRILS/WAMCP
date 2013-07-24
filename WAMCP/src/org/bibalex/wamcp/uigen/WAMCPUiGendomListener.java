//   Copyright 2013 Bibliotheca Alexandrina, Wellcome Trust Library, Wellcome Trust Library
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

package org.bibalex.wamcp.uigen;

import java.io.Serializable;

import org.bibalex.sdxe.controller.DomTreeController;
import org.bibalex.sdxe.exception.DomBuildException;
import org.bibalex.sdxe.exception.DomGeneralCorrectableException;
import org.bibalex.sdxe.xsa.uigen.icefaces.XSAUiGenDomListener;
import org.bibalex.sdxe.xsa.uigen.icefaces.XSAUiGenFactoryAbstract;
import org.bibalex.wamcp.application.WAMCPCompsDecoratorFactory;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;

public class WAMCPUiGendomListener extends XSAUiGenDomListener implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 570297631752437116L;
	
	public WAMCPUiGendomListener(String schemaFilePath) {
		super(schemaFilePath);
		
	}
	
	@Override
	protected XSAUiGenFactoryAbstract createUiGenFactory() {
		return new WAMCPUiGenFactory(
				this.sdxeMediator,
				this.bindersBean,
				new WAMCPCompsDecoratorFactory(),
				this.xsaDoc);
	}
	
	@Override
	public void notifyBeforeSave(Document domTotalClone)
			throws DomBuildException {
		// TODO This code doesn't belong here..
		// refactor when the Itnterface is refactored
		try {
			
			// TODO enforce: /TEI/text/body/div/msDesc/physDesc/objectDesc/supportDesc/support/material exists if
			// ancestor::supportDesc/@material='chart' only
			Element root = domTotalClone.getRootElement();
			Namespace nsXp = DomTreeController.acquireNamespace(root
					.getNamespaceURI(), root, true);
			String pfx = nsXp.getPrefix();
			if ((pfx != null) && !pfx.isEmpty()) {
				pfx += ":";
			}
			
			// msDesc
			XPath msDescXp = XPath.newInstance("/" + pfx + "TEI/" + pfx
					+ "text/" + pfx + "body/"
					+ pfx
					+ "div/" + pfx + "msDesc");
			msDescXp.addNamespace(nsXp);
			
			Object msDescObj = msDescXp.selectSingleNode(domTotalClone);
			
			boolean failure = false;
			if (msDescObj != null) {
				Element msDescElt = (Element) msDescObj;
				
				XPath shelfmarkXp = XPath.newInstance("/" + pfx + "TEI/" + pfx
						+ "text/" + pfx
						+ "body/" + pfx + "div/" + pfx + "msDesc/" + pfx
						+ "msIdentifier/" + pfx
						+ "idno");
				shelfmarkXp.addNamespace(nsXp);
				
				Object shelfmarkObj = shelfmarkXp
						.selectSingleNode(domTotalClone);
				
				if (shelfmarkObj != null) {
					Element shelfmarkElt = (Element) shelfmarkObj;
					String sheflmarkStr = shelfmarkElt.getTextNormalize();
					if ((sheflmarkStr == null) || sheflmarkStr.isEmpty()) {
						failure = true;
					} else {
						sheflmarkStr = sheflmarkStr.replaceAll(
								"[\\p{Punct}\\p{Blank}]", "");
						String id = "m" + sheflmarkStr;
						msDescElt.setAttribute("id", id,
								Namespace.XML_NAMESPACE);
					}
				} else {
					failure = true;
				}
				
			} else {
				throw new DomBuildException(
						"Template is corrupted because the file has no msDesc");
			}
			
			if (failure) {
				throw new DomGeneralCorrectableException(
						"The mandatory attribute @id of the msDesc is always taken from the Shelfmark; please provide a Shelfmark before saving!");
			}
			
			// msPart
			Namespace nsElts = domTotalClone.getRootElement().getNamespace();
			XPath msPartXP = XPath.newInstance("//" + pfx + "TEI/" + pfx
					+ "text/" + pfx
					+ "body/" + pfx + "div/" + pfx + "msDesc/" + pfx + "msPart");
			msPartXP.addNamespace(nsXp);
			int i = 1;
			for (Object msPartObj : msPartXP.selectNodes(domTotalClone)) {
				Element msPartElt = (Element) msPartObj;
				
				// TODO use DomTreeController to run the validators
				
				Element altIdElt = msPartElt.getChild("altIdentifier", nsElts);
				if (altIdElt == null) {
					altIdElt = new Element("altIdentifier", nsElts);
					msPartElt.getChildren().add(0, altIdElt); // altIdentifier must be the first element
				}
				
				altIdElt.setAttribute("type", "internal");
				
				Element idNoElt = altIdElt.getChild("idno", nsElts);
				if (idNoElt == null) {
					idNoElt = new Element("idno", nsElts);
					altIdElt.addContent(idNoElt);
				}
				
				idNoElt.setText("" + i++);
				
// // object desc type=codex
//
// XPath objDescXP = XPath.newInstance(pfx + "physDesc/" + pfx + "objectDesc");
// objDescXP.addNamespace(ns);
// Object objDescObj = objDescXP.selectSingleNode(msPartElt);
// if (objDescObj != null) {
// Element objDescElt = (Element) objDescObj;
// objDescElt.setAttribute("form", "codex");
// }
			}
			
			// ///////////////////////////////////////////////////////////////
			// Clean up of some mistakes
			
			XPath debugAttrXP = XPath.newInstance("//*[@DEBUG]");
			for (Object obj : debugAttrXP.selectNodes(domTotalClone)) {
				((Element) obj).removeAttribute("DEBUG");
			}
			
			// /////
			
			XPath noAuthorXP = XPath
					.newInstance("//*[contains(lower-case(normalize-space(text())), 'no author')]");
			for (Object obj : noAuthorXP.selectNodes(domTotalClone)) {
				if (obj instanceof Element) {
					((Element) obj).setText("Anonymous");
				} else if (obj instanceof Attribute) {
					// This cannot happen.. I know but I'm just being stupid!
					((Attribute) obj).setValue("Anonymous");
				}
			}
			
			// objectDesc form=codex
			XPath objectDescXP = XPath
					.newInstance("//" + pfx + "objectDesc");
			objectDescXP.addNamespace(nsXp);
			for (Object obj : objectDescXP.selectNodes(domTotalClone)) {
				if (obj instanceof Element) {
					((Element) obj).setAttribute("form", "codex");
				} else if (obj instanceof Attribute) {
					// This cannot happen.. I know but I'm just being stupid!
					
				}
			}
			
			// ///////////////////////////////////////////////////////////////////////////////
			
		} catch (JDOMException e) {
			throw new DomBuildException(e);
		}
		
	}
	
}
