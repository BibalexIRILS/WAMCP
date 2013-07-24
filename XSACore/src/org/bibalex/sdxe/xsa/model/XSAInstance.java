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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.WeakHashMap;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;
import org.bibalex.sdxe.controller.DomTreeController;
import org.bibalex.sdxe.exception.DomBuildChangesException;
import org.bibalex.sdxe.suggest.model.dom.SugtnDeclQName;
import org.bibalex.sdxe.xsa.application.XSAConstants;
import org.bibalex.sdxe.xsa.exception.XSAException;
import org.bibalex.sdxe.xsa.uigen.icefaces.XSAUiBindersBean;
import org.bibalex.util.KeyValuePair;
import org.bibalex.util.XPathStrUtils;
import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;

/**
 * The most important class of the XSA model package. Actually XSA is all about Instances. Besides extract data about
 * the instance from XSA, this class also provide methods for creating the XPath that would get occurrences of this
 * instance, either in the whole XML document, or under certain parents.
 * 
 * @author Younos.Naga
 * 
 */
public class XSAInstance extends XSAMessagesContainer {
	/**
	 * For XPath generation, to specify what kind of XPath is returned.
	 * 
	 * @author Younos.Naga
	 * 
	 */
	private enum XPathBrosMode {
		singleNode, allBrosInBaseShiftedArea, allBrosInWholeSite
	}
	
	/**
	 * 
	 * The access type that should be returned when requesting access control info
	 * 
	 * @author Younos.Naga
	 * 
	 */
	public enum XSAAccessTypes {
		Read, Write
	}
	
	private static final Logger LOG = Logger.getLogger("org.bibalex.xsa");
	public static final String LOCAL_NAME = "instance";
	
	public static final String MSG_LABEL = "label";
	
	// private static final String XPPARAM_ATTRNAME = "attrName";
// private static final String XPPARAM_ATTRNS = "attrNS";
	private static final String XPPARAM_LOCATORSTR = "locatorStr";
	private static final String XPPARAM_INSTIX = "instIx";
	
	private static final String XPPARAM_ID = "xmlID";
	
	/**
	 * A base shift pattern is a locator string with [?Base Part Name] in the middle. Each [?X] makes the part before it
	 * a base shift part, whose index is taken from an input whose label is X
	 * 
	 * @param pattern
	 * @return A list of label=pattern pairs for the parts in the pattern
	 */
	public static LinkedList<KeyValuePair<String, String>> breakBaseShiftPattern(String pattern) {
		
		LinkedList<KeyValuePair<String, String>> result = new LinkedList<KeyValuePair<String, String>>();
		while (pattern.indexOf('?') != -1) {
			int ix = pattern.indexOf('?');
			
			String part = pattern.substring(0, ix - 1);
			pattern = pattern.substring(ix + 1);
			
			ix = pattern.indexOf(']');
			
			String label = pattern.substring(0, ix);
			pattern = pattern.substring(ix + 1);
			
			result.add(new KeyValuePair<String, String>(part, label));
		}
		return result;
	}
	
	/**
	 * As a work around for having in XSA some WAMCP code that cannot be refactored outside, this boolean disables this
	 * code. This is allows others to use XSA without breaking it from WAMCP point of view.
	 */
	protected boolean allowWAMCPSpecific = false;
	
	private List<XSAInstance> instsUnderByIdRef = null;
	
	private XPath pcDataXp;
	
	private final XPath containingInstXp;
	private final XPath contInstByIdXP;
	private LinkedList<String> authorityList = null;
	
	private boolean authorityListGrows = false;
	private final boolean repeatable;
	
	private final int ixBetBros;
	private int ixInUi = -1;
	
	private final XSALocator locator;
	
	private final XSAUiDivisions uiDiv;
	
	private HashMap<SugtnDeclQName, String> attributeValues = null;
	
	private XSAInstance containingInst = null;
	
	private final String authListId;
	
	private Map<String, String> xpathesCache = null;
	
	private final HashMap<XSAAccessTypes, String> accessRoles;
	
	// This will complicate things and we can live without it
// public static String XSA_ATTR_INSTANCE_INDEX_DONT_PUBLISH_NS = "http://www.bibalex.org/xsa/nopulish";
	
	private String dataTypeName;
	
	private Boolean dataTypeIsList;
	
	private XSAUiBindersBean bindersBean;
	
	protected final boolean isInstForAttr;
	
	private String ixedLocatorStr = null;
	
	private int instsCountUnderByIdRef = -1;
	
	private String baseShiftPattern = null;
	
	/**
	 * A constuctor that should not be called, so that caching works. Use the factory method in XSAInstanceCache
	 * 
	 * @param domInstance
	 * @throws JDOMException
	 * @throws XSAException
	 */
	@Deprecated
	// TODO refactor so as to make this private and remove the dummy deprecated
	protected XSAInstance(Element domInstance) throws JDOMException, XSAException {
		super(domInstance);
		
		this.setSessionBeanReferences();
		
		this.accessRoles = new HashMap<XSAAccessTypes, String>();
		// This is a per session cache, so only one thread access it
// this.xpathesCache = Collections.synchronizedMap(new WeakHashMap<String, String>());
		this.xpathesCache = new WeakHashMap<String, String>();
		
		Attribute authListAtr = this.domRep.getAttribute("authorityList");
		if (authListAtr != null) {
			this.authListId = authListAtr.getValue();
		} else {
			this.authListId = null;
		}
		
		Attribute repeatAttr = domInstance.getAttribute("repeat");
		this.repeatable = (repeatAttr == null) || !("no".equalsIgnoreCase(repeatAttr.getValue()));
		
		String contInstById = "//" + this.pfx + "instance[@xml:id=$" + XPPARAM_ID + "]";
		this.contInstByIdXP = XPath.newInstance(contInstById);
		this.contInstByIdXP.addNamespace(this.namespace);
		
		String containingInstStr = "//" + this.pfx + "dataHolder[@locator=$" + XPPARAM_LOCATORSTR
				+ "]/" + this.pfx + "instance[$" + XPPARAM_INSTIX + "]";
		this.containingInstXp = XPath.newInstance(containingInstStr);
		this.containingInstXp.addNamespace(this.namespace);
		
		String dataHolderStr = "parent::" + this.pfx + "dataHolder";
		XPath dataHodlderXP = XPath.newInstance(dataHolderStr);
		dataHodlderXP.addNamespace(this.namespace);
		
		Element domDH = (Element) dataHodlderXP.selectSingleNode(this.domRep);
		
		boolean tempIsInstForAttr = false;
		if (domDH == null) {
			// This is in an annotator.. because we want annotators to be passed as xsaInsts
			// TODO: refactor this dirty hack by extracting the common interface
			this.ixBetBros = -1;
			this.uiDiv = null;
			this.locator = null;
		} else {
			Attribute locatorAttr = domDH.getAttribute(XSALocator.LOCAL_NAME);
			
			this.ixBetBros = domDH.getChildren(LOCAL_NAME, this.namespace).indexOf(this.domRep) + 1;
			
			if (locatorAttr != null) {
				String parentLocator = locatorAttr.getValue();
				
				String myLocatorStr = parentLocator;
				
				if (this.ixBetBros > 1) {
					myLocatorStr += "["
							+ this.ixBetBros + "]";
				}
				
				this.locator = new XSALocator(myLocatorStr);
				
				tempIsInstForAttr = XPathStrUtils.getLastStep(this.locator.asString()).startsWith(
						"@");
			} else {
				this.locator = null;
			}
			
			String uiDivStr = "normalize-space(" + this.pfx + "ui/text())";
			XPath uiDivXp = XPath.newInstance(uiDivStr);
			uiDivXp.addNamespace(this.namespace);
			
			String uiStr = uiDivXp.valueOf(this.domRep);
			this.uiDiv = XSAUiDivisions.valueOf(uiStr);
// Nice to have a default but how will we find field instances from XML??
// if (this.uiDiv == null) {
// this.uiDiv = XSAUiDivisions.field; // default
// }
			
		}
		this.isInstForAttr = tempIsInstForAttr;
	}
	
	/**
	 * 
	 */
	public void clearBaseShifterMaps() {
		this.getBindersBean().clearShifters();
		
	}
	
	/**
	 * Two instances are equal if the represent the same DOM element from XSA.xml
	 */
	@Override
	public boolean equals(Object arg0) {
		boolean result = false;
		
		if ((arg0 != null) && (arg0 instanceof XSAInstance)) {
			XSAInstance other = (XSAInstance) arg0;
			
			result = other.domRep.equals(this.domRep);
		} else {
			result = super.equals(arg0);
		}
		
		return result;
		
	}
	
	/**
	 * 
	 * @param accessType
	 * @return A comma separated ROLE_X,ROLE_Y that have the access right of the type required. All roles must be
	 *         prefixed by ROLE_
	 */
	public String getAccessRoles(XSAAccessTypes accessType) {
		String result = null;
		try {
			
			if (!this.accessRoles.containsKey(accessType)) {
				HashSet<String> roleSet;
				
				roleSet = this.getAccessRoleSet(accessType);
				result = "";
				for (String role : roleSet) {
					result += "," + role;
				}
				
				this.accessRoles.put(accessType, (result.isEmpty() ? null : result.substring(1)));
				
			}
			
			result = this.accessRoles.get(accessType);
			
		} catch (XSAException e) {
			e.printStackTrace();
			result = "EXCEPTION!!";
		}
		
		return result;
	}
	
	/**
	 * This method is used internally, so that the access rights from the (nearest) containing instances (that have
	 * access rights) are used, if access rights are not explicitly declared in an Instance.
	 * 
	 * @param accessType
	 * @return Broken down
	 * @throws XSAException
	 */
	protected HashSet<String> getAccessRoleSet(XSAAccessTypes accessType) throws XSAException {
		
		HashSet<String> result = new HashSet<String>();
		String eltName = null;
		if (XSAAccessTypes.Read.equals(accessType)) {
			eltName = "readAccess";
		} else {
			eltName = "writeAccess";
		}
		Element respElt = this.domRep.getChild(eltName, this.namespace);
		String selfReadAccess = respElt != null ? respElt.getTextNormalize() : null;
		if ((selfReadAccess != null) && !selfReadAccess.isEmpty()) {
			result.add(selfReadAccess);
		} else {
			XSAInstance contInst = this.getContainingInst();
			if (contInst != null) {
				result.addAll(contInst.getAccessRoleSet(accessType));
			}
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug(accessType.toString() + " Access Role Set for [" + this.toString() + "]:"
					+ result.toString());
		}
		
		return result;
	}
	
	/**
	 * 
	 * @return The content of the advancedSearch attrbite, if any
	 */
	public String getAdvancedSearchType() {
		Element indexedElt = this.domRep.getChild("indexed", this.namespace);
		String result = null;
		if (indexedElt != null) {
			result = indexedElt.getAttributeValue("advancedSearch");
		}
		return result;
	}
	
	/**
	 * 
	 * @return The set of designating attributes of this instance
	 */
	public Set<SugtnDeclQName> getAttributeNames() {
		if (this.attributeValues == null) {
			this.populateAttributeValues();
		}
		
		Set<SugtnDeclQName> result = this.attributeValues.keySet();
		
		return result;
		
	}
	
	/**
	 * 
	 * @param attrQName
	 * @return The value that the passed desingating attibute should have
	 * @throws JDOMException
	 */
	public String getAttributeValue(SugtnDeclQName attrQName) throws JDOMException {
		
		if (this.attributeValues == null) {
			this.populateAttributeValues();
		}
		
		return this.attributeValues.get(attrQName);
	}
	
	/**
	 * @return The authorityList limiting the values allowed for this instance, null if none is specified.
	 * @throws JDOMException
	 */
	public LinkedList<String> getAuthorityList() throws JDOMException {
		
		if (this.authorityList == null) {
			// This is a per session cache, so only one thread access it synchronized (this) {
			
			if ((this.authListId != null) && !this.authListId.isEmpty()) {
				this.authorityList = new LinkedList<String>();
				
				String authListStr = "//" + this.pfx + "authListDef[@xml:id='"
							+ this.authListId
							+ "']";
				
				XPath authListXP = XPath.newInstance(authListStr);
				authListXP.addNamespace(this.namespace);
// Added by default: authListXP.addNamespace(Namespace.XML_NAMESPACE);
				
				Element authListDom = (Element) authListXP.selectSingleNode(this.domRep);
				
				if (authListDom != null) {
					StringTokenizer listTokens = new StringTokenizer(
								authListDom.getTextNormalize(),
								"|", false);
					while (listTokens.hasMoreTokens()) {
						String listItem = listTokens.nextToken().trim();
						this.authorityList.add(listItem);
					}
					
					Attribute authListGrowsDom = authListDom.getAttribute("grows");
					this.authorityListGrows = (authListGrowsDom != null) &&
								authListGrowsDom.getBooleanValue();
					
				}
				
			}
			
		}
		
		return this.authorityList;
	}
	
	/**
	 * 
	 * @return The map containing the current indexes for all base shift parts
	 */
	public HashMap<String, Integer> getBaseShiftCurrs() {
		
		return this.getBindersBean().getbaseXPShiftedCurrs();
	}
	
	/**
	 * Used for generating unique IDs per base shift. Even though any two controls bound to two occurrences at two
	 * different base shift would never coexist on the same screen, when one control is generated by an ID that is
	 * already used by a control that will be destroyed, this seems to "confuse" ICEFaces (or any other UI fmwk)
	 * 
	 * @param domTarget
	 * @return The locator string with indices added after each base shift part
	 * @throws XSAException
	 */
	public String getBaseShiftedLocatorStr(DomTreeController domTarget) throws XSAException {
		
		String result = this.getIxedLocatorStr();
		
		String pattern;
		
		try {
			pattern = this.getBaseShiftPattern();
		} catch (JDOMException e) {
			throw new XSAException(e);
		}
		
		String baseShifted = "";
		if ((pattern != null) && !pattern.isEmpty()) {
			baseShifted = this.getBindersBean().getBaseXPShiftedOfCurrArea();
			if ((baseShifted == null) || baseShifted.isEmpty()) {
// throw new XSASessionBeanException(
				LOG.warn("In a base shifted area, but base shifters maps are empty. Will populate shifters maps in the session bean");
				this.populateBaseShifterMaps(domTarget);
				return this.getBaseShiftedLocatorStr(domTarget);
				
			}
			
			StringTokenizer baseTokens = new StringTokenizer(baseShifted, "/", false);
			StringTokenizer absTokens = new StringTokenizer(result, "/",
					false);
			StringBuilder resultBuilder = new StringBuilder();
			
			while (absTokens.hasMoreTokens()) {
				
				if (baseTokens.hasMoreTokens()) {
					resultBuilder.append("/" + baseTokens.nextToken());
					absTokens.nextToken();
				} else {
					resultBuilder.append("/" + absTokens.nextToken());
				}
				
			}
			
			result = resultBuilder.toString();
		}
		
		return result;
		
	}
	
	/**
	 * 
	 * @param part
	 * @return The current index at which the passed part is shifted
	 */
	public int getBaseShiftPartCurr(String part) {
		return this.getBindersBean().getbaseXPShiftedCurrs().get(part);
	}
	
	/**
	 * 
	 * @param part
	 * @return The maximum index to which the passed part can be shifted
	 */
	public int getBaseShiftPartMax(String part) {
		return this.getBindersBean().getBaseXPShiftedMaxes().get(part);
	}
	
	/**
	 * 
	 * @return A list of fragments of the base shift pattern, each would cause a base shift
	 */
	public LinkedList<String> getBaseShiftParts() {
		LinkedList<String> result = this.getBindersBean().getBaseXPShiftPatternParts();
		
// Too much hussle to make sure that the global variables is for this area, and this method is called very few times,
// and just after populateBaseShiftMaps
// Iterator<String> resultIter = result.iterator();
// while(resultIter.hasNext()){
// String part = resultIter.next();
// if()
// }
		return result;
	}
	
	/**
	 * 
	 * @return The complete base shift pattern that this instance should obey; areas can contain instances corresponding
	 *         to elements that are not under the area's base shifter
	 * @throws XSAException
	 * @throws JDOMException
	 */
	public String getBaseShiftPattern() throws XSAException, JDOMException {
		
		String result = this.baseShiftPattern;
		
		if (result != null) {
			return result;
		}
		result = this.getBaseShiftPatternInternal();
		
		if ((result != null) && !result.isEmpty()) {
			// if this is an XML bounded instnace
			if (this.locator != null) {
				// make sure that the pattern is for use by this instance
				String myBase = this.locator.asString();
				LinkedList<KeyValuePair<String, String>> brokenPattern = breakBaseShiftPattern(result);
				for (KeyValuePair<String, String> partKV : brokenPattern) {
					String part = partKV.getKey();
					if (myBase.startsWith(part)) {
						myBase = myBase.substring(part.length());
					} else {
						// A part of the pattern is not in our locator.. its not for us
						result = "";
						break;
					}
				}
			}
		}
		this.baseShiftPattern = result;
		return result;
	}
	
	/**
	 * 
	 * @return The base shift pattern specified at the containing area
	 * @throws XSAException
	 * @throws JDOMException
	 */
	private String getBaseShiftPatternInternal() throws XSAException, JDOMException {
		String result = "";
		Element baseShift = this.domRep.getChild("baseShift", this.namespace);
		
		if (baseShift != null) {
			if (!XSAUiDivisions.area.equals(this.uiDiv)) {
				throw new XSAException("A base shift should only be defined in an area (for now)");
			}
			Attribute patternAttr = baseShift.getAttribute("pattern");
			if (patternAttr != null) {
				result = patternAttr.getValue();
			}
			
		} else {
			
			// Nothing in self and we have to use container's
			XSAInstance contInst = this.getContainingInst();
			if (contInst != null) {// not the site
				if (XSAUiDivisions.site.equals(contInst.getUiDivision())) {
					result = "";
				} else {
					result = contInst.getBaseShiftPatternInternal();
				}
			}
		}
		return result;
	}
	
	/**
	 * 
	 * @param domTarget
	 * @return The prefix of the XPath that would do the base shifting, if it is necessary
	 * @throws XSAException
	 */
	public String getBaseXPShifted(DomTreeController domTarget) throws XSAException {
		String pattern;
		
		try {
			pattern = this.getBaseShiftPattern();
		} catch (JDOMException e) {
			throw new XSAException(e);
		}
		
		String baseShifted = "";
		if ((pattern != null) && !pattern.isEmpty()) {
			baseShifted = this.getBindersBean().getBaseXPShiftedOfCurrArea();
			if ((baseShifted == null) || baseShifted.isEmpty()) {
// throw new XSASessionBeanException(
				LOG.warn("In a base shifted area, but base shifters maps are empty. Will populate shifters maps in the session bean");
				this.populateBaseShifterMaps(domTarget);
				return this.getBaseXPShifted(domTarget);
			}
		} else {
			baseShifted = this.getLocator().asString();
		}
		
		return baseShifted;
	}
	
	/**
	 * 
	 * @return The binders bean linking this instance to the Session
	 */
	public XSAUiBindersBean getBindersBean() {
		if (this.bindersBean == null) {
			this.setSessionBeanReferences();
		}
		return this.bindersBean;
	}
	
	/**
	 * 
	 * @return The instance which this instance is under; using the "under" attribute, or the locator string (which is a
	 *         deprecated way)
	 * @throws XSAException
	 */
	public XSAInstance getContainingInst() throws XSAException {
		if (this.containingInst != null) {
			return this.containingInst;
		}
		// No need for this, we just write to containinginst once: synchronized (this) {
		
		try {
			XSAInstance result = null;
			Element xpRes = null;
			
			Attribute underAttr = this.domRep.getAttribute("under");
			String underStr = null;
			if (underAttr != null) {
				underStr = underAttr.getValue();
			}
			
			if ((underStr != null) && !underStr.isEmpty()) {
				
				this.contInstByIdXP.setVariable(XPPARAM_ID, underStr);
				xpRes = (Element) this.contInstByIdXP.selectSingleNode(this.domRep);
				// TODONOT make sure that locators can be under each other if they exist
			} else {
				
				String locatorStr = this.locator.asString();
				
				do {
					String locatStrNew = XPathStrUtils.removeLastStep(locatorStr);
					if (locatorStr.equals(locatStrNew)) {
						break;
					} else {
						locatorStr = locatStrNew;
					}
					
					int lastIx = XPathStrUtils.getLastIx(locatorStr);
					if (lastIx == -1) {
						lastIx = 1;
					}
					this.containingInstXp.setVariable(XPPARAM_INSTIX, lastIx);
					this.containingInstXp.setVariable(XPPARAM_LOCATORSTR, XPathStrUtils
							.removeLastIx(locatorStr));
					
					xpRes = (Element) this.containingInstXp.selectSingleNode(this.domRep);
					
					if (xpRes == null) {
// int lastSlash = locatorStr.lastIndexOf("/");
// if (lastSlash > 0) {
// locatorStr = locatorStr.substring(0, lastSlash);
// } else {
// break; //No containing instance
// }
					}
					
				} while (xpRes == null);
			}
			if (xpRes != null) {
				result = XSAInstanceCache.getInstance().xsaInstanceForDomRep(xpRes);
				// new XSAInstance(xpRes);
			}
			
			this.containingInst = result;
			return result;
			
		} catch (JDOMException e) {
			throw new XSAException(e);
		}
		
	}
	
	/**
	 * 
	 * @return True of there is a data type explicitly declared with a list="true" attribute
	 * @throws JDOMException
	 */
	public Boolean getDataTypeIsList() throws JDOMException {
		if (this.dataTypeIsList == null) {
			this.initDataType();
		}
		return this.dataTypeIsList;
	}
	
	/**
	 * 
	 * @return Gets the name of the data type explicitly declared if any
	 * @throws JDOMException
	 */
	public String getDataTypeName() throws JDOMException {
		if (this.dataTypeName == null) {
			this.initDataType();
		}
		return this.dataTypeName;
	}
	
	/**
	 * 
	 * @return The value of the &lt;defaultVal&gt; element
	 * @throws JDOMException
	 */
	public String getDefaultValue() throws JDOMException {
		String result = null;
		
		Element pcdataElt = this.domRep.getChild("defaultVal", this.namespace);
		if (pcdataElt != null) {
			result = pcdataElt.getTextNormalize();
		}
		
		return result;
		
	}
	
	/**
	 * 
	 * @return Content of the facet attribute of the indexed element. This should be the type of the field used for
	 *         faceting
	 */
	public String getExtraFacetType() {
		Element indexedElt = this.domRep.getChild("indexed", this.namespace);
		String result = null;
		if (indexedElt != null) {
			result = indexedElt.getAttributeValue("facet");
		}
		return result;
	}
	
	/**
	 * 
	 * @return The UI Group in which this instance is
	 */
	public String getGroup() {
		Attribute groupAttr = this.domRep.getAttribute("group");
		return groupAttr != null ? groupAttr.getValue() : null;
	}
	
	/**
	 * 
	 * @return The id of the instance
	 */
	public String getId() {
		Attribute idAttr = this.domRep.getAttribute("id", Namespace.XML_NAMESPACE);
		return idAttr != null ? idAttr.getValue() : null;
	}
	
	/**
	 * 
	 * @return The number of instances which are directly under this instance
	 * @throws JDOMException
	 * @throws XSAException
	 */
	public int getInstsCountUnderByIdRef() throws JDOMException, XSAException {
		if ((this.getId() == null) || this.getId().isEmpty()) {
			throw new UnsupportedOperationException();
		}
		
		if (this.instsCountUnderByIdRef == -1) {
			// No need for this, we write to insts ocne: synchronized (this) {
			XPath underMeXP = XPath.newInstance("count(//" + this.pfx
					+ "instance[@under=$MY_ID])");
			underMeXP.addNamespace(this.namespace);
			underMeXP.setVariable("MY_ID", this.getId());
			this.instsCountUnderByIdRef = underMeXP.numberValueOf(this.domRep).intValue();
			// }
		}
		
		return this.instsCountUnderByIdRef;
	}
	
	/**
	 * 
	 * @return A list of instances directly under this
	 * @throws JDOMException
	 * @throws XSAException
	 */
	public List<XSAInstance> getInstsUnderByIdRef() throws JDOMException, XSAException {
		if ((this.getId() == null) || this.getId().isEmpty()) {
			throw new UnsupportedOperationException();
		}
		
		if (this.instsUnderByIdRef == null) {
// synchronized (this) {
			
			this.instsUnderByIdRef = new LinkedList<XSAInstance>();
			
			XPath underMeXP = XPath.newInstance("//" + this.pfx + "instance[@under=$MY_ID]");
			underMeXP.addNamespace(this.namespace);
			underMeXP.setVariable("MY_ID", this.getId());
			
			for (Object obj : underMeXP.selectNodes(this.domRep)) {
				this.instsUnderByIdRef.add(XSAInstanceCache.getInstance().xsaInstanceForDomRep(
							(Element) obj));
			}
// }
		}
		
		return this.instsUnderByIdRef;
	}
	
	/**
	 * @return The index in which this instance appears between instances of the same element
	 */
	public int getIxBetBros() {
		return this.ixBetBros;
	}
	
	/**
	 * 
	 * @return The locator string with the Ix between bros appended (for id generation)
	 * @throws XSAException
	 */
	public String getIxedLocatorStr() throws XSAException {
		if (this.ixedLocatorStr != null) {
			return this.ixedLocatorStr;
		}
		if (this.locator == null) {
			return "";
		}
		
		String myIxedLocStr = this.locator.asString();
		String parentIxedLocStr = "";
		
		XSAInstance contgInst = this.getContainingInst();
		if (contgInst != null) {
			parentIxedLocStr = contgInst.getIxedLocatorStr();
		}
		
		if (parentIxedLocStr.isEmpty()) {
			return myIxedLocStr;
		}
		
		String postfix = "";
		for (int i = XPathStrUtils.getStepsCount(myIxedLocStr)
				- XPathStrUtils.getStepsCount(parentIxedLocStr); i > 0; --i) {
			postfix = XPathStrUtils.getLastStep(myIxedLocStr) + "/" + postfix;
			myIxedLocStr = XPathStrUtils.removeLastStep(myIxedLocStr);
		}
		String result = parentIxedLocStr + "/" + postfix;
		result = result.substring(0, result.length() - 1);
		
		this.ixedLocatorStr = result;
		return result;
	}
	
	/**
	 * 
	 * @return True if this instance should contribute in the summaries of the search results
	 */
	public boolean getIxFieldIsSummary() {
		Element eltIxed = this.domRep.getChild("indexed", this.namespace);
		Attribute summaryAttr = eltIxed.getAttribute("summary");
		
		boolean result = false;
		if (summaryAttr != null) {
			result = "yes".equalsIgnoreCase(summaryAttr.getValue());
		}
		
		return result;
	}
	
	/**
	 * 
	 * @return The content of the &lt;indexed&gt; element
	 */
	public String getIxFieldName() {
		Element eltIxed = this.domRep.getChild("indexed", this.namespace);
		
		return eltIxed.getTextNormalize();
	}
	
	/**
	 * @return The index in which this instance appears in the UI, between instances of the same dataHolder, and
	 *         instances under the same containing instance, and instances of the same group
	 * @throws JDOMException
	 */
	public int getIxInUi() throws JDOMException {
		if (this.ixInUi == -1) {
			// No need for this, we write once: synchronized (this) {
			
			String myGroup = this.getGroup();
			
			List uiSiblings = null;
			if ((myGroup != null) && !myGroup.isEmpty()) {
				String xiInUiStr = "//" + this.pfx + "instance[@group=$MY_GROUP]";
				
				XPath ixInUiXp = XPath.newInstance(xiInUiStr);
				ixInUiXp.addNamespace(this.namespace);
				
				ixInUiXp.setVariable("MY_GROUP", myGroup);
				
				uiSiblings = ixInUiXp.selectNodes(this.domRep);
			} else {
				String xiInUiStr = "parent::" + this.pfx + "dataHolder/" + this.pfx
						+ "instance[not (@under)] | //" + this.pfx
						+ "instance[@under=$MY_UNDER]";
				
				String myUnder = this.domRep.getAttributeValue("under");
				
				XPath ixInUiXp = XPath.newInstance(xiInUiStr);
				ixInUiXp.addNamespace(this.namespace);
				
				ixInUiXp.setVariable("MY_UNDER", myUnder);
				
				uiSiblings = ixInUiXp.selectNodes(this.domRep);
			}
			
			HashSet<String> groupsEncountered = new HashSet<String>();
			
			int result = 0;
			for (Object instObj : uiSiblings) {
				Element instElt = (Element) instObj;
				Attribute grpAttr = instElt.getAttribute("group");
				String grpId = grpAttr != null ? grpAttr.getValue() : null;
				
				int ziadah = 0;
				
				if ((grpId != null) && !grpId.isEmpty() && !grpId.equals(myGroup)) {
					
					if (!groupsEncountered.contains(grpId)) {
						ziadah = 1;
						groupsEncountered.add(grpId);
					}
					
				} else {
					ziadah = 1;
				}
				
				result += ziadah;
				
				if (this.domRep.equals(instObj)) {
					break;
				}
			}
			this.ixInUi = result;
			// }
		}
		return this.ixInUi;
	}
	
	/**
	 * 
	 * @return The locator of this instance's dataHolder
	 */
	public XSALocator getLocator() {
		return this.locator;
	}
	
	/**
	 * 
	 * @return The locator of this instance's dataHolder as String
	 */
	public String getLocatorString() {
		return this.locator.asString();
	}
	
	/**
	 * 
	 * @return The content of &lt;responsible&gt; element. This is not used.
	 */
	public String getResponsible() {
		Element respElt = this.domRep.getChild("responsible", this.namespace);
		return respElt != null ? respElt.getTextNormalize() : null;
	}
	
	/**
	 * 
	 * @return The solrType attribute of the &lt;indexed&gt; element. This should be the type of the solr field to use
	 *         for indexing.
	 */
	public String getSolrType() {
		Element indexedElt = this.domRep.getChild("indexed", this.namespace);
		String result = null;
		if (indexedElt != null) {
			result = indexedElt.getAttributeValue("solrType");
		}
		return result;
	}
	
	/**
	 * 
	 * @return The UI division that should represent this instance
	 * @throws JDOMException
	 */
	public XSAUiDivisions getUiDivision() throws JDOMException {
		return this.uiDiv;
	}
	
	/**
	 * @see getXPathStrInternal(XPathBrosMode brosMode, String ixesPathContsNField,
	 *      String baseShifted, DomTreeController domTarget)
	 * 
	 *      To avoid regenerating an XPath several times, the XPathes of occurrences are cached
	 * 
	 * @param ixesPathContsNField
	 *            The indexes that will be added to each path step starting from the first container in the instances
	 *            hierarchy
	 * @param domTarget
	 *            The DOM on which the instance will be
	 * @return An XPath string of one occurrence
	 * @throws XSAException
	 */
	public String getXPathStr(String ixesPathContsNField, DomTreeController domTarget)
			throws XSAException {
		
		String result = null;
		
		String baseShifted = this.getBaseXPShifted(domTarget);
		
		String key = ixesPathContsNField + baseShifted;
		if (this.xpathesCache.containsKey(key)) {
			
			result = this.xpathesCache.get(key);
			
		} else {
			
			result = this.getXPathStrInternal(XPathBrosMode.singleNode, ixesPathContsNField,
					baseShifted, domTarget);
			
			this.xpathesCache.put(key, result);
			
		}
		return result;
	}
	
	/**
	 * XPath to a specific occurrence is generated by generating XPaths of all containing instances, starting from the
	 * area or site downwards. The XPath of each instance has the values of all its designating attributes, and not
	 * those of other instances of the same dataHolder. The XPath up to now would be evaluate to all occurrences of an
	 * instance. To get a specific occurrence at any step, the index of this occurrence between its brothers (siblings
	 * of the same element and same designating attributes) is appended. To get an XPath to all occurrences at any step
	 * the index is not appended. Another thing that makes the XPath general or specific is the Area Base Shifters. If
	 * the base is shifted the XPath would evaluate to only the occurrences under this base shift. If the base is not
	 * shifted, the XPath would evaluate to all occurrences in the whole document.
	 * 
	 * 
	 * @param brosMode
	 *            Specifies how specific the XPath should be
	 * @param ixesPathContsNField
	 *            The indexes that will be added to each path step starting from the first container in the instances
	 *            hierarchy, if the mode is specific enough
	 * @param baseShifted
	 *            The base part of the XPath, representing the Area. Either shifted or not, depending on how specific
	 *            the XPath should be.
	 * @param domTarget
	 * @return The XPath string
	 * @throws XSAException
	 */
	private String getXPathStrInternal(XPathBrosMode brosMode, String ixesPathContsNField,
			String baseShifted, DomTreeController domTarget)
			throws XSAException {
		String result = null;
		
		switch (this.uiDiv) {
			case area:
			case section:
				throw new XSAException(
						"Cannot get XPath for a UIDivision that has no XML counterpart!");
				
			case container:
			case field:
				int myIx = -3;
				
				XSAInstance contgInst = this.getContainingInst();
				String resultBase = null;
				try {
					if (contgInst.getUiDivision().equals(XSAUiDivisions.container)) {
						
						switch (brosMode) {
							case allBrosInWholeSite:
								resultBase = contgInst.getXPathToAllOccsInAllBaseShifts();
								break;
							
							case allBrosInBaseShiftedArea:
								resultBase = contgInst.getXPathToAllOccsInAllConts(domTarget);
								break;
							
							case singleNode:

								if ((ixesPathContsNField != null) && !ixesPathContsNField.isEmpty()) {
									myIx = Integer.parseInt(XPathStrUtils
											.getLastStep(ixesPathContsNField));
								} else {
									throw new XSAException(
											"In a container but ixesPathContsNField is empty!");
								}
								
								int mySteps = XPathStrUtils.getStepsCount(this.locator.asString());
								int contgSteps = XPathStrUtils.getStepsCount(contgInst.locator
										.asString());
								if (mySteps == contgSteps) {
// Bad idea
// // A field in a container or area that holds the text node of a certain occerence of its
// // element
// // We use a double index predicate which is wrong XPath but the best hack (using /text()
// // would need a lot of coding)
// return contgInst.getXPathStr(ixesPathContsNField) + "["
// + XPathStrUtils.getLastStep(ixesPathContsNField) + "]";
									LOG
											.error("Having a field under a container of the same locator is not supported, remove: "
													+ this);
								} else if (mySteps > contgSteps) {
									while (mySteps > contgSteps) {
										--mySteps;
										ixesPathContsNField = XPathStrUtils
												.removeLastStep(ixesPathContsNField);
									}
									
									resultBase = contgInst
											.getXPathStr(ixesPathContsNField, domTarget);
								} else {
									throw new XSAException("Will never happen!");
								}
								break;
						}
						
					} else {
						// Fields directly under the area/section
						if (XPathBrosMode.singleNode.equals(brosMode)) {
							if ((ixesPathContsNField != null) && !ixesPathContsNField.isEmpty()) {
								myIx = Integer.parseInt(XPathStrUtils
										.getLastStep(ixesPathContsNField));
								
								ixesPathContsNField = XPathStrUtils
										.removeLastStep(ixesPathContsNField);
								
								if (!ixesPathContsNField.isEmpty()) {
									// After removing the ix that this call has consumed, there are still more ixes
									
									boolean bThrow = false;
									
									// but this could be the ix for the whole field that is added as a hack
									if (Integer.parseInt(XPathStrUtils
											.getLastStep(ixesPathContsNField)) == -2) {
										ixesPathContsNField = XPathStrUtils
												.removeLastStep(ixesPathContsNField);
										bThrow = (!ixesPathContsNField.isEmpty());
									} else {
										bThrow = true;
									}
									if (bThrow) {
										throw new XSAException(
												"No more containers but the ixesPath still contains: "
														+ ixesPathContsNField);
									}
								}
							} else {
								throw new XSAException(
										"In a field but ixesPathContsNField is empty!");
							}
							
						} else {
							// myIx will not be used
						}
						resultBase = baseShifted;
						
					}
					StringTokenizer baseTokens = new StringTokenizer(resultBase, "/", false);
					StringTokenizer absTokens = new StringTokenizer(this.locator.asString(), "/",
							false);
					StringBuilder resultBuilder = new StringBuilder();
					
					while (absTokens.hasMoreTokens()) {
						
						if (baseTokens.hasMoreTokens()) {
							resultBuilder.append("/" + baseTokens.nextToken());
							absTokens.nextToken();
						} else {
							resultBuilder.append("/" + absTokens.nextToken());
						}
						
					}
					
					result = XPathStrUtils.removeLastIx(resultBuilder.toString());
					
					StringBuilder predicate = new StringBuilder();
					if (!this.isInstForAttr) {
						for (Object instObj : this.domRep.getParentElement().getChildren(
								this.domRep.getName(), this.domRep.getNamespace())) {
							Element instElt = (Element) instObj;
							
							XSAInstance myCont = this.getContainingInst();
							XSAInstance otherCont = XSAInstanceCache.getInstance()
									.xsaInstanceForDomRep(instElt)
									.getContainingInst();
							
							if ((myCont != null) && (otherCont != null)) {
								if (!myCont.equals(otherCont)) {
									// These are two instances under different instances
									// so the containing instances should already produce
									// XPather that differentiate the XML elements
									continue;
								}
							}
							
							if (instElt != this.domRep) {
								predicate.append("not(");
							} else {
								predicate.append("   (");
							}
							
							// TODO use the functions getAttributeNames and getAttributeValue to be DRY
							for (Object attrObj : instElt.getChildren("attribute", this.namespace)) {
								Element attrElt = (Element) attrObj;
								
								String attrName = attrElt.getAttributeValue("name");
								String attrNs = attrElt.getAttributeValue("nsuri");
								String attrVal = attrElt.getAttributeValue("value");
								
								if ((attrNs != null) && !attrNs.isEmpty()) {
									
									Namespace ns = DomTreeController
											.acquireNamespaceNoParent(attrNs);
									String pfx = ns.getPrefix();
									// Attribs don't have deafault namespace
									if ((pfx == null) || pfx.isEmpty()) {
										pfx = "DefNs";
										ns = Namespace.getNamespace(ns.getURI(), pfx);
									}
									
									attrName = pfx + ":" + attrName;
								}
								if ((attrVal != null) && !attrVal.isEmpty()) {
									predicate.append('@' + attrName + "='" + attrVal + "'");
									predicate.append(" and ");
								}
							}
							int delStart;
							if (instElt != this.domRep) {
								delStart = predicate.lastIndexOf("not(");
							} else {
								delStart = predicate.lastIndexOf("   (");
							}
							if (delStart == predicate.length() - 4) {
								// only not( was appended
								
								predicate.delete(delStart, predicate.length());
							} else {
								
								delStart = predicate.lastIndexOf(" and ");
								if (delStart == predicate.length() - 5) {
									// a freshly added and
									predicate
												.delete(delStart,
														predicate.length());
									predicate.append(") and ");
								} else {
									// this.domRep = xsaElt and "this" doesn't have any attribute values set
								}
							}
							
						}
						if (predicate.length() > 0) {
							
							result += "[" + predicate.substring(0, predicate.lastIndexOf(" and "))
									+ "]";
							
						}
// This causes the instance to be "lost" when a child attribute is added to it to hold data
// else {
// result += "[not(@*)]";
// }
						
					}
					
					if (XPathBrosMode.singleNode.equals(brosMode)) {
						String position = null;
						if (myIx == -1) {
							position = "[last()]";
						} else if (myIx > 0) {
							position = "[" + myIx + "]";
						} else if (myIx == 0) {
							position = ""; // all bros in this container
						} else if (myIx == -2) {
							// an invalid number that will be detected as a number to be removed
							// but indicates that it should not be used
							position = "[0]";
						} else {
							throw new XSAException("The poisions path passed was wrong!");
						}
						
						result += position;
					}
					// Fields that do not repeat don't have to
// else {
// throw new
// XSAException("Instances that would be bound to data fields or containers must be identifiable by attributes");
// }
					
				} catch (JDOMException e) {
					throw new XSAException(e);
				}
				
		}
		return result;
	}
	
	/**
	 * @see getXPathStrInternal(XPathBrosMode brosMode, String ixesPathContsNField,
	 *      String baseShifted, DomTreeController domTarget)
	 * 
	 *      To avoid regenerating an XPath several times, the XPathes of occurrences are cached
	 * 
	 * @return An XPath to all occurrences in all the document
	 * @throws XSAException
	 */
	public String getXPathToAllOccsInAllBaseShifts() throws XSAException {
		String result = null;
		
		String baseUnshifted = this.getLocatorString();
		
		if (this.xpathesCache.containsKey(baseUnshifted)) {
			
			result = this.xpathesCache.get(baseUnshifted);
			
		} else {
			
			result = this
					.getXPathStrInternal(XPathBrosMode.allBrosInWholeSite, null, baseUnshifted,
							null); // not used in this case
			
			this.xpathesCache.put(baseUnshifted, result);
		}
		
		return result;
	}
	
	/**
	 * @see getXPathStrInternal(XPathBrosMode brosMode, String ixesPathContsNField,
	 *      String baseShifted, DomTreeController domTarget)
	 * 
	 *      To avoid regenerating an XPath several times, the XPathes of occurrences are cached
	 * @param domTarget
	 * @return An XPath to all occurrences in all containers, but under one area base shift
	 * @throws XSAException
	 */
	public String getXPathToAllOccsInAllConts(DomTreeController domTarget) throws XSAException {
		String result = null;
		
		String baseShifted = this.getBaseXPShifted(domTarget);
		
		if (this.xpathesCache.containsKey(baseShifted)) {
			
			result = this.xpathesCache.get(baseShifted);
			
		} else {
			
			result = this.getXPathStrInternal(XPathBrosMode.allBrosInBaseShiftedArea, null,
					baseShifted, domTarget);
			
			this.xpathesCache.put(baseShifted, result);
		}
		
		return result;
	}
	
	/**
	 * The hashCode of the dom element in XSA.xml which this instance represents
	 */
	@Override
	public int hashCode() {
		
		return this.domRep.hashCode();
	}
	
	/**
	 * 
	 * @throws JDOMException
	 */
	private void initDataType() throws JDOMException {
		// This is a per session cache, so only one thread access itsynchronized (this) {
		
		String dataTypeStr = "parent::" + this.pfx + "dataHolder/" + this.pfx + "dataType";
		XPath dataTypeXP = XPath.newInstance(dataTypeStr);
		dataTypeXP.addNamespace(this.namespace);
		Element dataTypeElt = (Element) dataTypeXP.selectSingleNode(this.domRep);
		if (dataTypeElt != null) {
			this.dataTypeName = dataTypeElt.getTextNormalize();
			Attribute listAttr = dataTypeElt.getAttribute("list");
			if (listAttr != null) {
				this.dataTypeIsList = listAttr.getBooleanValue();
			}
		}
		if (this.dataTypeName == null) {
			this.dataTypeName = "";
		}
		if (this.dataTypeIsList == null) {
			this.dataTypeIsList = Boolean.FALSE;
		}
// }
	}
	
	/**
	 * This is a workaround for having some WAMCP specific code that is hard to refactor outside
	 * 
	 * @return True if the {@link XSAInstanceCache} property allowWAMCPSpecific is set to true.
	 */
	public boolean isAllowWAMCPSpecific() {
		return this.allowWAMCPSpecific;
	}
	
	/**
	 * To provide autocomplete functionalities based on the history of entered data, instances with authority lists that
	 * grow were planned. This was not implemented though.
	 * 
	 * @return the authorityListGrows
	 */
	public boolean isAuthorityListGrows() {
		return this.authorityListGrows;
	}
	
	/**
	 * 
	 * @return True if in an Area whose base shift pattern is a prefix in the locator of this instance
	 * @throws XSAException
	 */
	public boolean isBaseShifted() throws XSAException {
		String pattern;
		
		try {
			pattern = this.getBaseShiftPattern();
		} catch (JDOMException e) {
			throw new XSAException(e);
		}
		
		return ((pattern != null) && !pattern.isEmpty());
	}
	
	/**
	 * WAMCP specific
	 * 
	 * @return True if the attrubite facs is not set to "no", and the WAMCP specifics are allowed
	 */
	public boolean isFacsOptional() {
		// / FIXME Remove this WAMCP specific code from general distros of XSA
		return this.allowWAMCPSpecific && !this.isFacsProhibited();
	}
	
	/**
	 * WAMCP sepcific
	 * 
	 * @return True if the attrubite facs is set to "no", and the WAMCP specifics are allowed
	 */
	public boolean isFacsProhibited() {
		// FIXME Remove this WAMCP specific code from general distros of XSA
		boolean result = false;
		
		Attribute noFacsAttr = this.domRep.getAttribute(XSAConstants.WAMCP_FACS_ATTR_NAME);
		
		if (noFacsAttr != null) {
			result = "no".equalsIgnoreCase(noFacsAttr.getValue());
		}
		return this.allowWAMCPSpecific && result;
	}
	
	/**
	 * WAMCP sepcific
	 * 
	 * @return True if the attrubite facs is set to "yes", and the WAMCP specifics are allowed
	 */
	public boolean isFacsRequired() {
		// FIXME Remove this WAMCP specific code from general distros of XSA
		boolean result = false;
		
		Attribute noFacsAttr = this.domRep.getAttribute(XSAConstants.WAMCP_FACS_ATTR_NAME);
		
		if (noFacsAttr != null) {
			result = "yes".equalsIgnoreCase(noFacsAttr.getValue());
		}
		return this.allowWAMCPSpecific && result;
	}
	
	/**
	 * 
	 * @return True if the dataholder is an attribute
	 */
	public boolean isInstForAttr() {
		return this.isInstForAttr;
	}
	
	/**
	 * 
	 * @return True if mixed attribute is set to "no" or the dataHolder is an attribute
	 * @throws DataConversionException
	 */
	public boolean isNomixed() throws DataConversionException {
		
		boolean result = false;
		Attribute nomixedAttr = this.domRep.getAttribute("mixed");
		if (nomixedAttr != null) {
			result = "no".equalsIgnoreCase(nomixedAttr.getValue());
		}
		return result;
		
	}
	
	/**
	 * Repeatability is contextual; i.e. a non-repeatable attribute cannot be repeated under one occurrence of its
	 * containing isntance.
	 * 
	 * @return False if the repeatable attribute is "no"
	 */
	public boolean isRepeatable() {
		return this.repeatable;
	}
	
	/**
	 * Requirement is contextual; i.e. each instance is required in the context of its containing attribute only.
	 * 
	 * @return True if the required attribute of the responsible element is set to "true"
	 * @throws DataConversionException
	 */
	public boolean isRequired() throws DataConversionException {
		boolean result = false;
		Element resp = this.domRep.getChild("responsible", this.namespace);
		if (resp != null) {
			Attribute req = resp.getAttribute("required");
			result = (req != null) && req.getBooleanValue();
		}
		return result;
	}
	
	/**
	 * A singleton instance is one who can appear only once in the document under all circustances
	 * 
	 * @return True if all instances in the hierarchy are not repeatable, and the area is not base shifted
	 * @throws XSAException
	 * @throws JDOMException
	 */
	public boolean isSingleton() throws XSAException, JDOMException {
		String baseShiftPattern = this.getBaseShiftPattern();
		
		boolean result = !this.isRepeatable()
				&& ((baseShiftPattern == null) || baseShiftPattern.isEmpty());
		
		XSAInstance contInst = this.getContainingInst();
		
		if (contInst != null) {
			result = result && contInst.isSingleton();
		}
		
		return result;
	}
	
	/**
	 * 
	 */
	private void populateAttributeValues() {
// // This is a per session cache, so only one thread access itsynchronized (this) {
		
		this.attributeValues = new HashMap<SugtnDeclQName, String>();
		
		for (Object obj : this.domRep.getChildren("attribute", this.namespace)) {
			Element attrElt = (Element) obj;
			
			String attrName = attrElt.getAttributeValue("name");
			String attrNs = attrElt.getAttributeValue("nsuri");
			if (attrNs == null) {
				attrNs = "";
			}
			String attrVal = attrElt.getAttributeValue("value");
			
			SugtnDeclQName attrQName = new SugtnDeclQName(attrNs, attrName);
			
			this.attributeValues.put(attrQName, attrVal);
		}
// }
	}
	
	/**
	 * Call this whenever the base shift current and max maps are empty
	 * 
	 * @param domTarget
	 * @throws XSAException
	 */
	public void populateBaseShifterMaps(DomTreeController domTarget) throws XSAException {
		
		try {
			Element rootElt = domTarget.getDomTotal().getRootElement();
			
			// Will not be empty when called from uigen
// if (!(this.getBindersBean().getBaseXPShiftedMaxes().isEmpty() && this.bindersBean
// .getbaseXPShiftedCurrs().isEmpty())) {
// throw new XSAException(
// "Proceeding will change already set session variable; this might be an undesired side effect. If it is desired call clearShifters before calling this!");
// }
			
			String pattern = this.getBaseShiftPattern();
			
			LinkedList<KeyValuePair<String, String>> brokenPattern = XSAInstance
					.breakBaseShiftPattern(pattern);
			String prefix = "";
			for (KeyValuePair<String, String> partKV : brokenPattern) {
				String part = partKV.getKey();
				
				Integer currIx = this.getBindersBean().getbaseXPShiftedCurrs().get(part);
				
				// ///////////////
				
				prefix += part;
				String partCountStr = "count(" + domTarget.addNsPfxToXPath(prefix) + ")";
				
				XPath partCountXP = XPath.newInstance(partCountStr);
				partCountXP.addNamespace(domTarget.getNamespaceForXPath());
				
				int count = partCountXP.numberValueOf(rootElt).intValue();
				
				this.getBindersBean().getBaseXPShiftedMaxes().put(part, count);
				
				if (currIx == null) {
					if (count > 0) {
						currIx = 1;
					} else {
						currIx = 0;
					}
					this.getBindersBean().getbaseXPShiftedCurrs().put(part, currIx);
					this.getBindersBean().getBaseXPShiftPatternParts().add(part);
				}
				
				this.getBindersBean().getBaseXPShiftedTargets().put(part, currIx);
				
				prefix += "[" + currIx + "]";
			}
		} catch (JDOMException e) {
			throw new XSAException(e);
		} catch (DomBuildChangesException e) {
			throw new XSAException(e);
		}
	}
	
	/**
	 * 
	 * @param allowWAMCPSpecific
	 */
	public void setAllowWAMCPSpecific(boolean allowWAMCPSpecific) {
		this.allowWAMCPSpecific = allowWAMCPSpecific;
	}
	
	/**
	 * Shift a certain fragment of the base shift pattern
	 * 
	 * @param part
	 *            The fragment to shift
	 * @param newCurr
	 *            The index
	 */
	public void setBaseShiftPartCurr(String part, int newCurr) {
		this.getBindersBean().getbaseXPShiftedCurrs().put(part, newCurr);
	}
	
	/**
	 * If the instance is base shifted it needs access to session variables.. this resolves the references to managed
	 * beans, when needed. This uses JSF EL and should be overridden if XSA is used in a context other than JSF.
	 * Unfortunately using the Spring abstract methods did not succeed. (Spring will most probably be included anyway)
	 */
	protected void setSessionBeanReferences() {
// No scope registered for scope "session" caused by:
// XmlBeanFactory bf = new XmlBeanFactory(
// new ClassPathResource("/applicationContext-beans.xml"));
// this.bindersBean = (XSAUiBindersBean) bf.getBean(XSAConstants.BEANNAME_BINDERS_BEAN);
		
		FacesContext fCtx = FacesContext.getCurrentInstance();
		LOG.debug("Setting instance: " + this + " references to session beans");
		if (fCtx != null) {
			ELContext elCtx = fCtx.getELContext();
			ExpressionFactory exprFactory = fCtx.getApplication().getExpressionFactory();
			
			ValueExpression bbeanEl = exprFactory.createValueExpression(elCtx,
					"#{" + XSAConstants.BEANNAME_BINDERS_BEAN + "}",
					XSAUiBindersBean.class);
			
			this.bindersBean = (XSAUiBindersBean) bbeanEl.getValue(elCtx);
			
		} else {
			
			LOG.warn("Could not get hold of a faces context, are you in a test environment or is it a race condition");
			this.bindersBean = null; // should not use function that depend on it when unit testing
			
		}
		
	}
	
	/**
	 * 
	 */
	@Override
	public String toString() {
		
		return this.locator != null ? this.locator.toString() : this.getId();
	}
}
