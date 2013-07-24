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

package org.bibalex.xsa.sample.xsaauth.pagebeans;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.bibalex.icefaces.jdomtree.IDTActionListener;
import org.bibalex.icefaces.jdomtree.IDTBBean;
import org.bibalex.icefaces.jdomtree.IDTUserObject;
import org.bibalex.jdom.XPathBuilder;
import org.bibalex.sdxe.exception.DomGeneralCorrectableException;
import org.bibalex.sdxe.suggest.model.dom.SugtnDeclQName;
import org.bibalex.sdxe.xsa.application.XSARequestBackingBean;
import org.bibalex.sdxe.xsa.application.XSAUiActionListener;
import org.bibalex.sdxe.xsa.exception.XSAException;
import org.bibalex.sdxe.xsa.model.XSAAnnotator;
import org.bibalex.sdxe.xsa.model.XSADocCache;
import org.bibalex.sdxe.xsa.model.XSADocument;
import org.bibalex.sdxe.xsa.model.XSAInstance;
import org.bibalex.sdxe.xsa.model.XSAUiDivisions;
import org.bibalex.util.JDOMUtils;
import org.bibalex.util.URLPathStrUtils;
import org.bibalex.util.XPathStrUtils;
import org.bibalex.xsa.sample.xsaauth.storage.SessionDirStorage;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

public class ImportBBean extends XSARequestBackingBean implements IDTActionListener {
	private String selectedXPath;
	
	private Document xmlDoc;
	
	private XSADocument xsaDoc;
	private IDTBBean iceDomTreeBBean;
	// All keys could be Element or Attribute so kept as Objects
	private HashMap<Object, LinkedList<XSAInstance>> presentInsts;
	private HashSet<Object> confusingObjs;
	
// TODO: If possible (doesn't seem so) automatically select the tree node when viewing its confusion
// private Tree iceTree;
//
// public Tree getIceTree() {
// return iceTree;
// }
//
// public void setIceTree(Tree iceTree) {
// this.iceTree = iceTree;
// }
	
	private HashSet<Object> preciseMatchObjs;
	// Must be mapped to String (locator) because it is bound to selectvalue values
	private HashMap<Object, String> resolutions;
	private HashMap<Object, String> presentXPStrs;
	private HashSet<Object> noMatchObjs;
	private List<XSAInstance> fieldInsts;
	private HashMap<Object, String> objValues;
	private final String CONFIG_SCHEMA_FILE;
	
	private SessionDirStorage storage;
	
	private XSAUiActionListener xsaUiActionListener;
	
	private String filename;
	
	public ImportBBean(String cONFIGSCHEMAFILE) {
		super();
		this.CONFIG_SCHEMA_FILE = cONFIGSCHEMAFILE;
	}
	
	public void doImport(ActionEvent ev) {
		try {
			for (Object obj : this.resolutions.keySet().toArray()) {
				
				String locatorStr = this.resolutions.get(obj);
				XSAInstance inst = null;
				
				if (locatorStr != null) {
					inst = this.xsaDoc.getInstanceByLocator(this.resolutions.get(obj));
				}
				
				if (inst == null) {
					throw new DomGeneralCorrectableException(
							"You must resolve all confusions and no matches");
				}
				if (obj instanceof Element) {
					Element elt = (Element) obj;
					do {
						for (SugtnDeclQName attrQName : inst.getAttributeNames()) {
							
							String attrNsUri = attrQName.getTargetNamespaceURI();
							Namespace attrNs = null;
							if (Namespace.NO_NAMESPACE.getURI().equals(attrNsUri)) {
								attrNs = Namespace.NO_NAMESPACE;
							} else if (Namespace.XML_NAMESPACE.getURI().equals(attrNsUri)) {
								attrNs = Namespace.XML_NAMESPACE;
								
							} else {
								attrNs = Namespace
										.getNamespace(attrNsUri);
							}
							
							elt.setAttribute(attrQName.getLocalName(), inst
									.getAttributeValue(attrQName), attrNs);
						}
						inst = inst.getContainingInst();
						
						if (!XSAUiDivisions.container.equals(inst.getUiDivision())) {
							break;
						}
						
						int contInstXPathSteps = XPathStrUtils.getStepsCount(inst
								.getLocatorString());
						
						do {
							elt = elt.getParentElement();
						} while (XPathStrUtils.getStepsCount(new XPathBuilder()
								.buildXPathString(elt)) != contInstXPathSteps);
						
					} while (true);
				}
			}
			
			XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat().setEncoding("UTF-8"));
			outputter.output(this.xmlDoc, new FileOutputStream(URLPathStrUtils.appendParts(
					this.storage.getSessionDir().getCanonicalPath(), "Imported.XML")));
			
			this.init(this.iceDomTreeBBean);
			
			if (!this.confusingObjs.isEmpty() || !this.noMatchObjs.isEmpty()) {
				throw new DomGeneralCorrectableException(
						"The import is still not precise.. cannot accept this file");
			}
			
			this.storage.newFromDocument(this.filename, this.xmlDoc);
			
			this.xsaUiActionListener.initEditSession();
			
			FacesContext.getCurrentInstance().getExternalContext().redirect(
					"EditForm.iface");
			
		} catch (Exception e) {
			this.handleException(e);
		}
		
	}
	
	public Object[] getConfusingObjsArray() {
		return this.confusingObjs.toArray();
	}
	
	public List<XSAInstance> getFieldInsts() {
		return this.fieldInsts;
	}
	
	public String getFilename() {
		return this.filename;
	}
	
	public Object[] getNoMatchObjsArray() {
		return this.noMatchObjs.toArray();
	}
	
	public HashMap<Object, String> getObjValues() {
		return this.objValues;
	}
	
	public Object[] getPreciseMatchObjsArray() {
		return this.preciseMatchObjs.toArray();
	}
	
	public HashMap<Object, LinkedList<XSAInstance>> getPresentInsts() {
		return this.presentInsts;
	}
	
	public HashMap<Object, String> getPresentXPStrs() {
		return this.presentXPStrs;
	}
	
	public HashMap<Object, String> getResolutions() {
		return this.resolutions;
	}
	
	public String getSelectedXPath() {
		return this.selectedXPath;
	}
	
	public SessionDirStorage getStorage() {
		return this.storage;
	}
	
	public void getValueAtXPath(ActionEvent ev) {
		
	}
	
	public XSAUiActionListener getXsaUiActionListener() {
		return this.xsaUiActionListener;
	}
	
	public void init(IDTBBean idTreeBBean) {
		try {
			this.iceDomTreeBBean = idTreeBBean;
			this.xmlDoc = idTreeBBean.getDocument();
			
			this.xsaDoc = XSADocCache.getInstance().xsaDocForSchema(
					this.CONFIG_SCHEMA_FILE);
			
			this.presentInsts = new HashMap<Object, LinkedList<XSAInstance>>();
			this.confusingObjs = new HashSet<Object>();
			this.noMatchObjs = new HashSet<Object>();
			this.preciseMatchObjs = new HashSet<Object>();
			this.resolutions = new HashMap<Object, String>();
			this.presentXPStrs = new HashMap<Object, String>();
			this.objValues = new HashMap<Object, String>();
			this.fieldInsts = new ArrayList<XSAInstance>();
			
			Namespace ns = this.iceDomTreeBBean.getActiveElement().getNamespace();
			
			String pfx = XPathBuilder.getPrefixForNamespace(ns, true);
			pfx = pfx.substring(0, pfx.lastIndexOf(':'));
			
			Namespace nsXp = Namespace.getNamespace(pfx, ns.getURI());
			
			for (XSAInstance xsaInst : this.xsaDoc.getDataContainingInstanceList()) {
				
				if (XSAUiDivisions.field.equals(xsaInst.getUiDivision())) {
					this.fieldInsts.add(xsaInst);
				}
// if ("Normalised (AR)".equals(xsaInst.getLabel())) {
// LOG.debug("Normalized arabic");
// }
// String instXPStr = xsaInst.getXPathToAllOccsInAllConts();
				String instXPStr = xsaInst.getXPathToAllOccsInAllBaseShifts();
				
				instXPStr = XPathBuilder.addNamespacePrefix(instXPStr, nsXp);
				
				XPath instXP = XPath.newInstance(instXPStr);
				instXP.addNamespace(nsXp);
				
				for (Object obj : instXP.selectNodes(this.xmlDoc)) {
					
					LinkedList<XSAInstance> presenceList = this.presentInsts.get(obj);
					
					if (presenceList == null) {
						presenceList = new LinkedList<XSAInstance>();
						this.presentInsts.put(obj, presenceList);
					}
					
					presenceList.add(xsaInst);
					
					if (presenceList.size() > 1) {
						this.confusingObjs.add(obj);
						this.resolutions.put(obj, null);
						
						IDTUserObject userObj = this.iceDomTreeBBean.findUserObjForDomObj(obj);
						userObj.setLeafIcon("tree-leaf-confusing.png");
						userObj.setBranchContractedIcon("tree-leaf-confusing.png");
						userObj.setBranchExpandedIcon("tree-leaf-confusing.png");
						
					}
					
					if (obj instanceof Element) {
						this.presentXPStrs.put(obj, new XPathBuilder()
								.buildXPathString((Element) obj));
						this.objValues.put(obj, JDOMUtils.getElementContentAsString(
								((Element) obj), Format
										.getCompactFormat()));
					} else if (obj instanceof Attribute) {
						this.presentXPStrs.put(obj, new XPathBuilder()
								.buildXPathString((Attribute) obj));
						this.objValues.put(obj, ((Attribute) obj).getValue());
					}
				}
				
			}
			
			for (Object key : this.presentInsts.keySet().toArray()) {
				if (!this.confusingObjs.contains(key)) {
					this.preciseMatchObjs.add(key);
					
					IDTUserObject userObj = this.iceDomTreeBBean.findUserObjForDomObj(key);
					userObj.setLeafIcon("tree-leaf-precise.png");
					userObj.setBranchContractedIcon("tree-leaf-precise.png");
					userObj.setBranchExpandedIcon("tree-leaf-precise.png");
				}
				
			}
			
			// No Match
			XPath textHoldingEltsXP = XPath
					.newInstance("//*[not(normalize-space(child::text())='')]");
			// It doesn't need this.. no prefixes: textHoldingEltsXP.addNamespace(nsXp);
			
			for (Object obj : textHoldingEltsXP.selectNodes(this.xmlDoc)) {
				if (this.preciseMatchObjs.contains(obj) || this.confusingObjs.contains(obj)) {
					continue;
				}
				
				if (obj instanceof Element) {
					// annotators
					Element noMatchElt = (Element) obj;
					boolean isAnnotator = false;
					for (XSAAnnotator annot : this.xsaDoc.getAnnotators()) {
						if (annot.getEltLocalName().equals(noMatchElt.getName())) {
							isAnnotator = true;
							break;
						}
					}
					if (isAnnotator) {
						continue;
					}
					
					this.presentXPStrs.put(obj, new XPathBuilder()
							.buildXPathString((Element) obj));
					this.objValues.put(obj, ((Element) obj).getValue());
				} else if (obj instanceof Attribute) {
					this.presentXPStrs.put(obj, new XPathBuilder()
							.buildXPathString((Attribute) obj));
					this.objValues.put(obj, ((Attribute) obj).getValue());
				}
				
				this.noMatchObjs.add(obj);
				this.resolutions.put(obj, null);
				
			}
			
		} catch (XSAException e) {
			this.handleException(e);
		} catch (JDOMException e) {
			this.handleException(e);
		} catch (IOException e) {
			this.handleException(e);
		}
	}
	
	@Override
	public void nodeClicked(ActionEvent ev) {
		IDTUserObject userObj = (IDTUserObject) ev.getComponent().getAttributes().get(
				"IDT_PARAM_USEROBJECT");
		this.selectedXPath = this.presentXPStrs.get(userObj.getJdtNode().getNode());
		if ((this.selectedXPath == null) || this.selectedXPath.isEmpty()) {
			this.selectedXPath = userObj.getJdtNode().getXPathString();
		}
	}
	
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	public void setSelectedXPath(String selectedXPath) {
		this.selectedXPath = selectedXPath;
	}
	
	public void setStorage(SessionDirStorage storage) {
		this.storage = storage;
	}
	
	public void setXsaUiActionListener(XSAUiActionListener xsaUiActionListener) {
		this.xsaUiActionListener = xsaUiActionListener;
	}
	
}
