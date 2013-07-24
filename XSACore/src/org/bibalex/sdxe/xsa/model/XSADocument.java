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

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bibalex.sdxe.controller.DomTreeController;
import org.bibalex.sdxe.xsa.exception.XSAException;
import org.bibalex.sdxe.xsa.exception.XSAXMLInvalidException;
import org.bibalex.util.XPathStrUtils;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * 
 * This class represents the root element of the XSA document. It shields the developer from the internals of how XSA is
 * written, by providing utility methods to get Instance objects. Actually the only way to acquire an instance object is
 * through calling a method of this class.
 * 
 * @author Younos.Naga
 * 
 */
public class XSADocument extends XSANode {
	private static final Logger LOG = Logger.getLogger("org.bibalex.xsa");
	
	public static final String XSA_NAMESPACE_URI = "http://www.bibalex.org/xsa";
	public static final String XPATH_FUNCS_NS_URI = "http://www.w3.org/2005/xpath-functions";
	
	private static final String XPPARAM_LOCATORSTR = "locatorStr";
	private static final String XPPARAM_UIDIV = "uiDiv";
	private static final String XPPARAM_UNDERREF = "UnderRef";
	private static final String XPPARAM_GROUPID = "GroupId";
	private static final String XPPARAM_INSTID = "InstId";
	
	/**
	 * The XML document in the XSA.xml file
	 */
	private Document xsa = null;
	
	/**
	 * Each XSA.xml document must have one and only one instance of UI site. This is the Locator of its Dataholder.
	 */
	private XSALocator siteLocator = null;
	
	/**
	 * XPaths that would be reused a lot of times
	 */
	private XPath locatedByLocatorXp;
	private XPath instanceByUiDivXp;
	private XPath instUnderByUnderRef;
	private XPath groupXp;
	private XPath instByIdXp;
	
	/**
	 * 
	 * 
	 * @return List of Instances that should and could be indexed to provide advanced search functionalities
	 * @throws JDOMException
	 * @throws XSAException
	 */
	public List<XSAInstance> getAdvancedSearchInsts() throws JDOMException, XSAException {
		List<XSAInstance> result = new LinkedList<XSAInstance>();
		
		XPath indexedXp = XPath.newInstance("//" + this.pfx + "instance[(" + this.pfx
				+ "indexed/@advancedSearch) and ((" + this.pfx + "ui/text()='"
				+ XSAUiDivisions.field + "') or ("
				+ this.pfx + "ui/text()='" + XSAUiDivisions.container + "' and @mixed='yes'))]");
		indexedXp.addNamespace(this.namespace);
		
		List objs = indexedXp.selectNodes(this.xsa);
		
		for (Object obj : objs) {
			Element elt = (Element) obj;
			result.add(XSAInstanceCache.getInstance().xsaInstanceForDomRep(elt));
		}
		
		return result;
	}
	
	/**
	 * 
	 * @param eltLocalName
	 * @return An Annotator object for the annotator element whose local name is sent
	 * @throws JDOMException
	 * @throws XSAException
	 */
	public XSAAnnotator getAnnotatorInstance(String eltLocalName) throws JDOMException,
			XSAException {
		
		XSAAnnotator result = null;
		
		XPath annotatorByName = XPath.newInstance("//" + this.pfx + "annotator[@eltName=$"
				+ XPPARAM_LOCATORSTR + "]");
		annotatorByName.addNamespace(this.namespace);
		
		annotatorByName.setVariable(XPPARAM_LOCATORSTR, eltLocalName);
		Object obj = annotatorByName.selectSingleNode(this.xsa);
		if (obj != null) {
			result = new XSAAnnotator((Element) obj);
		}
		
		return result;
		
	}
	
	/**
	 * 
	 * @return List of Annotator objects for all annotators declared in the XSA.xml
	 * @throws XSAException
	 * @throws JDOMException
	 */
	public List<XSAAnnotator> getAnnotators() throws XSAException, JDOMException {
		List<XSAAnnotator> result = new LinkedList<XSAAnnotator>();
		
		XPath allAnnotator = XPath.newInstance("//" + this.pfx + "annotator");
		allAnnotator.addNamespace(this.namespace);
		
		List<Object> objs = allAnnotator.selectNodes(this.xsa);
		
		for (Object obj : objs) {
			Element elt = (Element) obj;
			result.add(new XSAAnnotator(elt));
		}
		
		return result;
		
	}
	
	/**
	 * 
	 * @return A list of all Instances that would contain data (Fields or Containers)
	 * @throws XSAException
	 */
	public List<XSAInstance> getDataContainingInstanceList() throws XSAException {
		LinkedList<XSAInstance> result = new LinkedList<XSAInstance>();
		
		try {
			XPath allInstancesXP = XPath.newInstance("//" + this.pfx
					+ "instance[normalize-space(child::"
					+ this.pfx + "ui/child::text()) = 'field' or normalize-space(child::"
					+ this.pfx
					+ "ui/child::text()) = 'container']");
			allInstancesXP.addNamespace(this.namespace);
			
			for (Object obj : allInstancesXP.selectNodes(this.xsa)) {
				result.add(XSAInstanceCache.getInstance().xsaInstanceForDomRep((Element) obj));
			}
			
		} catch (JDOMException e) {
			throw new XSAException(e);
		}
		
		return result;
	}
	
	/**
	 * 
	 * @return A list of Instance objects that should and could be indexed to provide faceting functionalities
	 * @throws JDOMException
	 * @throws XSAException
	 */
	public List<XSAInstance> getFacetInsts() throws JDOMException, XSAException {
		List<XSAInstance> result = new LinkedList<XSAInstance>();
		
		XPath indexedXp = XPath.newInstance("//" + this.pfx + "instance[(" + this.pfx
				+ "indexed/@facet='yes') and ((" + this.pfx + "ui/text()='"
				+ XSAUiDivisions.field + "') or ("
				+ this.pfx + "ui/text()='" + XSAUiDivisions.container + "' and @mixed='yes'))]");
		indexedXp.addNamespace(this.namespace);
		
		List objs = indexedXp.selectNodes(this.xsa);
		
		for (Object obj : objs) {
			Element elt = (Element) obj;
			result.add(XSAInstanceCache.getInstance().xsaInstanceForDomRep(elt));
		}
		
		return result;
	}
	
	/**
	 * 
	 * @param groupId
	 * @return The Group object for the group whose ID is sent
	 * @throws XSAException
	 * @throws JDOMException
	 */
	public XSAGroup getGroup(String groupId) throws XSAException, JDOMException {
		this.groupXp.setVariable(XPPARAM_GROUPID, groupId);
		
		Object xpRes = this.groupXp.selectSingleNode(this.xsa);
		
		return xpRes != null ? new XSAGroup((Element) xpRes) : null;
	}
	
	/**
	 * 
	 * @return List of Instance objects that should and could be indexed
	 * @throws JDOMException
	 * @throws XSAException
	 */
	public List<XSAInstance> getIndexedInsts() throws JDOMException, XSAException {
		List<XSAInstance> result = new LinkedList<XSAInstance>();
		
		XPath indexedXp = XPath.newInstance("//" + this.pfx + "instance[(" + this.pfx
				+ "indexed) and ((" + this.pfx + "ui/text()='" + XSAUiDivisions.field + "') or ("
				+ this.pfx + "ui/text()='" + XSAUiDivisions.container + "' and @mixed='yes'))]");
		indexedXp.addNamespace(this.namespace);
		
		List objs = indexedXp.selectNodes(this.xsa);
		
		for (Object obj : objs) {
			Element elt = (Element) obj;
			result.add(XSAInstanceCache.getInstance().xsaInstanceForDomRep(elt));
		}
		
		return result;
	}
	
	/**
	 * 
	 * @return The one Instance from which the Index Key is taken
	 * @throws JDOMException
	 * @throws XSAException
	 */
	public XSAInstance getIndexedKey() throws JDOMException, XSAException {
		XSAInstance result = null;
		
		XPath indexedXp = XPath.newInstance("//" + this.pfx + "instance[(" + this.pfx
				+ "indexed/@key='yes') and ((" + this.pfx + "ui/text()='" + XSAUiDivisions.field
				+ "') or (" + this.pfx + "ui/text()='" + XSAUiDivisions.container
				+ "' and @mixed='yes'))]");
		indexedXp.addNamespace(this.namespace);
		
		List objs = indexedXp.selectNodes(this.xsa);
		
		if (objs.size() != 1) {
			throw new XSAException("No indexed field that is a key or more than one");
		}
		
		Element elt = (Element) objs.get(0);
		result = XSAInstanceCache.getInstance().xsaInstanceForDomRep(elt);
		
		return result;
	}
	
	/**
	 * 
	 * @return List of Instances that appear in the Summary of results; that is, whose value must be stored in the index
	 * @throws JDOMException
	 * @throws XSAException
	 */
	public List<XSAInstance> getIndexedSummary() throws JDOMException, XSAException {
		List<XSAInstance> result = new LinkedList<XSAInstance>();
		
		XPath indexedXp = XPath.newInstance("//" + this.pfx + "instance[(" + this.pfx
				+ "indexed/@summary='yes') and ((" + this.pfx + "ui/text()='"
				+ XSAUiDivisions.field
				+ "') or (" + this.pfx + "ui/text()='" + XSAUiDivisions.container
				+ "' and @mixed='yes'))]");
		indexedXp.addNamespace(this.namespace);
		
		List objs = indexedXp.selectNodes(this.xsa);
		
		for (Object obj : objs) {
			Element elt = (Element) obj;
			result.add(XSAInstanceCache.getInstance().xsaInstanceForDomRep(elt));
		}
		
		return result;
	}
	
	/**
	 * 
	 * @param instId
	 * @return The Instance object whose Id is passed
	 * @throws XSAException
	 * @throws JDOMException
	 */
	public XSAInstance getInstanceById(String instId) throws XSAException, JDOMException {
		
		this.instByIdXp.setVariable(XPPARAM_INSTID, instId);
		
		Object xpRes = this.instByIdXp.selectSingleNode(this.xsa);
		
		return xpRes != null ? XSAInstanceCache.getInstance().xsaInstanceForDomRep((Element) xpRes)
				: null;
	}
	
	/**
	 * 
	 * @param locatorStr
	 * @return The Instance whose Locator string is passed. This uses a deprecated way of identifying instances.
	 * @throws XSAException
	 * @Deprecated
	 */
	@Deprecated
	public XSAInstance getInstanceByLocator(String locatorStr) throws XSAException {
		try {
			
			XSAInstance result = null;
			
			int instIx = XPathStrUtils.getLastIx(locatorStr);
			String dhLocator = null;
			if (instIx != 1) {
				dhLocator = XPathStrUtils.removeLastIx(locatorStr);
			} else {
				// this is a hack to put attributes in a separate section.. leave the ix
				dhLocator = locatorStr;
			}
			XSADataholder dh = (XSADataholder) this.getLocated(dhLocator);
			if (instIx < 1) {
				instIx = 1;
			}
			result = dh.getInstance(instIx);
			
			return result;
			
		} catch (JDOMException e) {
			throw new XSAException(e);
		}
	}
	
	/**
	 * 
	 * @param locatorStr
	 * @return A Located object.. that is a Data Holder.. no other Located objects were created. Ids are used instead.
	 *         Use of Located is deprecated.
	 * @throws XSAException
	 * @throws JDOMException
	 * @deprecated
	 */
	@Deprecated
	public XSALocated getLocated(String locatorStr) throws XSAException,
			JDOMException {
		this.guardUninit();
		
		XSALocated result = null;
		
		this.locatedByLocatorXp.setVariable(XPPARAM_LOCATORSTR, locatorStr);
		
		List nodes = this.locatedByLocatorXp.selectNodes(this.xsa);
		
		switch (nodes.size()) {
			case 0:
				result = null;
				break;
			
			case 1:
				Element rule = (Element) nodes.get(0);
				result = XSALocated.create(rule, new XSALocator(locatorStr));
				break;
			
			default:
				throw new XSAXMLInvalidException(
						"There are multiple elements with the same locator " + locatorStr);
		}
		
		return result;
	}
	
	/**
	 * 
	 * @return The Locator object for the one instance whose UI is site.
	 * @throws JDOMException
	 * @throws XSAException
	 */
	public XSALocator getSiteLocator() throws JDOMException, XSAException {
		this.guardUninit();
		XSALocator result = null;
		
		if (this.siteLocator != null) {
			result = this.siteLocator;
		} else {
			
			String siteFinderStr = "//" + this.pfx + "dataHolder[descendant::" + this.pfx
					+ "ui/text()='site']";
			
			XPath siteFinderXp = XPath.newInstance(siteFinderStr);
			siteFinderXp.addNamespace(this.namespace);
			
			List nodes = siteFinderXp.selectNodes(this.xsa);
			
			if (nodes.size() != 1) {
				throw new XSAXMLInvalidException(
						"There must be one and only one site in the XSA docutment");
			}
			
			Element site = (Element) nodes.get(0);
			
			String locatorStr = site.getAttributeValue("locator");
			
			result = new XSALocator(locatorStr);
			
			this.siteLocator = result;
		}
		
		return result;
	}
	
	/**
	 * Under and Below are confusing.. Below means directly or indirectly Under
	 * 
	 * @param subDivType
	 *            The type of subdivisions to return. Must be of a type that is lower than that of the parentInst
	 *            (except Containers which can contain Containers)
	 * @param parentInst
	 * @param goIntoContainers
	 *            If false, traversing will stop at container instances which are under the parentInst. If true, they
	 *            will be added to the result list, and instantces under them will also be added.
	 * @return List of instances directly or indirectly under the passed parentInst, whose UI is of the type passed
	 * @throws JDOMException
	 * @throws XSAException
	 */
	public List<XSAInstance> getSubDivisionsBelow(XSAUiDivisions subDivType,
			XSAInstance parentInst, boolean goIntoContainers)
			throws JDOMException, XSAException {
		List<XSAInstance> result = new LinkedList<XSAInstance>();
		
		List<XSAInstance> sections;
		List<XSAInstance> containers;
		switch (parentInst.getUiDivision()) {
			case area:
				sections = this.getSubDivisionsUnder(XSAUiDivisions.section, parentInst);
				if (subDivType.equals(XSAUiDivisions.section)) {
					result.addAll(sections);
					return result;
				} else {
					for (XSAInstance section : sections) {
						result.addAll(this.getSubDivisionsBelow(subDivType, section,
								goIntoContainers));
					}
				}
				// fall through as usual :) break;
			case section:
			case container:
				containers = this.getSubDivisionsUnder(XSAUiDivisions.container, parentInst);
				if (subDivType.equals(XSAUiDivisions.container)) {
					result.addAll(containers);
					return result;
				} else if (goIntoContainers) {
					for (XSAInstance container : containers) {
						result.addAll(this
								.getSubDivisionsBelow(subDivType, container, goIntoContainers));
					}
				}
				// keep the area falling through
				
// case container:
				
				if (subDivType.equals(XSAUiDivisions.field)) {
					result.addAll(this.getSubDivisionsUnder(subDivType, parentInst));
				} else {
					throw new IllegalArgumentException();
				}
				
				// stop falling before the exception
				break;
			default:
				// TODONE: NOTHING?? Yes
				// throw new UnsupportedOperationException("Not implemeneted");
		}
		
		return result;
	}
	
	/**
	 * Under and Below are confusing.. Under means directly Under; that is, referenced in the Under attribute
	 * 
	 * @param subDivisionType
	 *            The type of subdivisions to return. Must be of a type that is lower than that of the parentInst
	 *            (except Containers which can contain Containers)
	 * @param parentInst
	 * @return A list of Instances directly Under the passed parentInst
	 * @throws JDOMException
	 * @throws XSAException
	 */
	public List<XSAInstance> getSubDivisionsUnder(XSAUiDivisions subDivisionType,
			XSAInstance parentInst)
			throws JDOMException, XSAException {
		List<XSAInstance> result = null;
		switch (parentInst.getUiDivision()) {
			case site: // Because if an area or section is under a site it might not have a locator
			case area:
			case section:
			case container:
				result = this.getSubDivisionsUnderByUnderRef(subDivisionType, parentInst);
				break;
// Using the locator to get isntances under a parent proved totally confusing
// case container:
// result = this.getSubDivisionsUnderByLocator(subDivisionType, parentInst);
// break;
			case field:
			default:
				throw new IllegalArgumentException(
						"Getting subdivisions under a FIELD or an unknown type instance!!");
		}
		return result;
	}
	
	/**
	 * @deprecated
	 * @param subDivisionType
	 * @param parentInst
	 * @return
	 * @throws JDOMException
	 * @throws XSAException
	 */
	@Deprecated
	private List<XSAInstance> getSubDivisionsUnderByLocator(XSAUiDivisions subDivisionType,
			XSAInstance parentInst)
			throws JDOMException, XSAException {
		String underLocatorStr = parentInst.getLocator().asString();
		
		List<XSAInstance> result = new LinkedList<XSAInstance>();
		
		this.instanceByUiDivXp.setVariable(XPPARAM_LOCATORSTR, underLocatorStr);
		this.instanceByUiDivXp.setVariable(XPPARAM_UIDIV, subDivisionType.name());
		
		String parentId = parentInst.getId();
		
		List domInsts = this.instanceByUiDivXp.selectNodes(this.xsa);
		
		for (Object objInst : domInsts) {
			Element domInst = (Element) objInst;
			
			Attribute underAttr = domInst.getAttribute("under");
			String instUnder = null;
			if (underAttr != null) {
				instUnder = underAttr.getValue();
			}
			
			// If inst is under something it must this parent
			if ((instUnder != null)
					&& !instUnder.equals(parentId)) {
				
				continue;
				
			}
			
			// If parent is returned as a child of itself
			if (domInst.equals(parentInst.domRep)) {
				continue;
			}
			
			XSAInstance xsaInst = XSAInstanceCache.getInstance().xsaInstanceForDomRep(domInst);
			
			result.add(xsaInst);
		}
		
		return result;
	}
	
	/**
	 * @see getSubDivisionsUnder(XSAUiDivisions subDivisionType, XSAInstance parentInst) This one just limits the
	 *      returned ones to a certain UI.
	 * @param subDivisionType
	 *            subDivisionType The type of subdivisions to return. Must be of a type that is lower than that of the
	 *            parentInst (except Containers which can contain Containers)
	 * @param parentInst
	 * @return List of instances directly under the passed parentInst, whose UI is of the type passed
	 * @throws JDOMException
	 * @throws XSAException
	 */
	private List<XSAInstance> getSubDivisionsUnderByUnderRef(XSAUiDivisions subDivisionType,
			XSAInstance parentInst) throws JDOMException, XSAException {
		// TODO make sure that locators can be under each other if they exist
		List<XSAInstance> result = new LinkedList<XSAInstance>();
		
		String parentId = parentInst.getId();
		
		this.instUnderByUnderRef.setVariable(XPPARAM_UNDERREF, parentId);
		this.instUnderByUnderRef.setVariable(XPPARAM_UIDIV, subDivisionType.name());
		
		List domInsts = this.instUnderByUnderRef.selectNodes(this.xsa);
		
		for (Object objInst : domInsts) {
			
			Element domInst = (Element) objInst;
			
			// If parent is returned as a child of itself
			if (domInst.equals(parentInst.domRep)) {
				continue;
			}
			
			XSAInstance xsaInst = XSAInstanceCache.getInstance().xsaInstanceForDomRep(domInst);
			
			result.add(xsaInst);
		}
		
		return result;
	}
	
	/**
	 * Makes sure the Object is initialized
	 * 
	 * @throws XSAException
	 */
	private void guardUninit() throws XSAException {
		if (this.xsa == null) {
			throw new XSAException("Uninitialized");
		}
		
	}
	
	/**
	 * Initializes the object from an XSA.xml file.
	 * 
	 * @param xsaFile
	 * @throws JDOMException
	 * @throws IOException
	 */
	public void init(File xsaFile) throws JDOMException, IOException {
		SAXBuilder saxBuilder = new SAXBuilder();
		
		// TODO: set validation to true
		saxBuilder.setFeature("http://xml.org/sax/features/validation", false); // true);
		
		saxBuilder.setFeature("http://xml.org/sax/features/namespaces", true);
		saxBuilder.setFeature("http://xml.org/sax/features/namespace-prefixes",
				true);
// Unsupported: saxBuilder.setFeature("http://xml.org/sax/features/xmlns-uris", false);
		
		this.xsa = saxBuilder.build(xsaFile);
		
		this.namespace = DomTreeController.acquireNamespace(XSA_NAMESPACE_URI, this.xsa
				.getRootElement(), true);
		
		this.pfx = this.namespace.getPrefix();
		if ((this.pfx != null) && !this.pfx.isEmpty()) {
			this.pfx = this.pfx + ":";
		}
		
		String byLocatorStr = "//*[@locator=$" + XPPARAM_LOCATORSTR + "]";
		this.locatedByLocatorXp = XPath.newInstance(byLocatorStr);
		this.locatedByLocatorXp.addNamespace(this.namespace);
		
		String instanceByUiDivStr = "//" + this.pfx + "instance[starts-with(parent::" + this.pfx
				+ "dataHolder/@locator,$" + XPPARAM_LOCATORSTR + ") and child::" + this.pfx
				+ "ui=$" + XPPARAM_UIDIV + "]";
		this.instanceByUiDivXp = XPath.newInstance(instanceByUiDivStr);
		this.instanceByUiDivXp.addNamespace(this.namespace);
		
		String underByUnderRefStr = "//" + this.pfx + "instance[@under=$" + XPPARAM_UNDERREF
				+ " and child::" + this.pfx
				+ "ui=$" + XPPARAM_UIDIV + "]";
		this.instUnderByUnderRef = XPath.newInstance(underByUnderRefStr);
		this.instUnderByUnderRef.addNamespace(this.namespace);
		
		String groupStr = "//" + this.pfx + "groupDef[@xml:id=$" + XPPARAM_GROUPID + "]";
		
		this.groupXp = XPath.newInstance(groupStr);
		this.groupXp.addNamespace(this.namespace);
		
		String instByIdStr = "//" + this.pfx + "instance[@xml:id=$" + XPPARAM_INSTID + "]";
		this.instByIdXp = XPath.newInstance(instByIdStr);
		this.instByIdXp.addNamespace(this.namespace);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("XSA file " + xsaFile.getAbsolutePath() + " loaded");
		}
		
	}
	
}
