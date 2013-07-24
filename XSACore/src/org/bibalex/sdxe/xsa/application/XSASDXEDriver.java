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

package org.bibalex.sdxe.xsa.application;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.log4j.Logger;
import org.bibalex.sdxe.binding.SDXEMediatorBean;
import org.bibalex.sdxe.controller.DomTreeController;
import org.bibalex.sdxe.exception.DomBuildChangesException;
import org.bibalex.sdxe.exception.DomBuildException;
import org.bibalex.sdxe.exception.SDXEException;
import org.bibalex.sdxe.suggest.model.SugtnTreeNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnAttributeNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnDeclNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnDeclQName;
import org.bibalex.sdxe.suggest.model.dom.SugtnDomAddableNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnElementNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnPCDataNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnTypeDeclaration;
import org.bibalex.sdxe.suggest.model.dom.SugtnTypeDeclaration.XSDEnumMember;
import org.bibalex.sdxe.xsa.exception.XSAException;
import org.bibalex.sdxe.xsa.exception.XSAValidationError;
import org.bibalex.sdxe.xsa.model.XSAAnnotator;
import org.bibalex.sdxe.xsa.model.XSADocument;
import org.bibalex.sdxe.xsa.model.XSAInstance;
import org.bibalex.sdxe.xsa.model.XSALocator;
import org.bibalex.sdxe.xsa.model.XSAUiDivisions;
import org.bibalex.util.XPathStrUtils;
import org.iso_relax.verifier.VerifierConfigurationException;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.transform.JDOMSource;
import org.jdom.xpath.XPath;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.icesoft.faces.context.effects.BlankEffect;
import com.sun.org.apache.bcel.internal.generic.BALOAD;

/**
 * Drives SDXE according to XSA. It makes using the XPath mode of DomTreeController much more convinient.
 * 
 * @author Younos.Naga
 * 
 */
public class XSASDXEDriver {
	/**
	 * For writing a log about the validation of the XML produced
	 * 
	 * @author Younos.Naga
	 * 
	 */
	public static class LoggingErrorHandler implements ErrorHandler {
		int errCount = 0;
		int warnCount = 0;
		BufferedWriter logWriter;
		
		public LoggingErrorHandler(String logFilePath) throws IOException {
			this.logWriter = new BufferedWriter(new FileWriter(logFilePath, false));
		}
		
		@Override
		public void error(SAXParseException ex) throws SAXException {
			++this.errCount;
			try {
				String msg = "" // Line and col are -1: "Line " + ex.getLineNumber() + " Col " + ex.getColumnNumber() +
						// " - "
						+ ex.getMessage();
				this.logWriter.append("Error: " + msg);
				this.logWriter.newLine();
			} catch (IOException e) {
				throw new SAXException(e);
			}
			
		}
		
		@Override
		public void fatalError(SAXParseException ex) throws SAXException {
			throw ex;
			
		}
		
		@Override
		protected void finalize() throws Throwable {
			this.logWriter.close();
			super.finalize();
		}
		
		@Override
		public void warning(SAXParseException ex) throws SAXException {
			++this.warnCount;
			try {
				String msg = "" // Line and col are -1: "Line " + ex.getLineNumber() + " Col " + ex.getColumnNumber() +
						// " - "
						+ ex.getMessage();
				this.logWriter.append("Warning: " + msg);
				this.logWriter.newLine();
			} catch (IOException e) {
				throw new SAXException(e);
			}
		}
	}
	
	/**
	 * For overriding types from the schema with types from the XSA.xml
	 * 
	 * 
	 * @author Younos.Naga
	 * 
	 */
	private static class XSASugtnBasicType extends SugtnTypeDeclaration {
		private String xsaTypename;
		
		private boolean xsaRequired;
		
		private Boolean xsaIsList;
		
		private final SugtnTypeDeclaration orig;
		
		private final LinkedList<XSDEnumMember> xsaenum;
		
		// TODO remove super because this is a delegation not an inheritance actually..
		// this need the extraction of the common interface between this and super
		public XSASugtnBasicType(SugtnTypeDeclaration orig) {
			super();
			
			if (orig == null) {
				orig = new SugtnTypeDeclaration();
			}
			
			this.orig = orig;
			this.xsaenum = new LinkedList<XSDEnumMember>();
		}
		
		@Override
		public String getBaseTypeName() {
			if (this.xsaTypename != null) {
				return this.xsaTypename;
			} else {
				return this.orig.getBaseTypeName();
			}
		}
		
		@Override
		public LinkedList<XSDEnumMember> getEnumeration() {
			if ((this.xsaenum.size() == 0) && this.orig.isEnumerated()) {
				return this.orig.getEnumeration();
			} else {
				return this.xsaenum;
			}
		}
		
		@Override
		public int getFractionDigits() {
			
			return this.orig.getFractionDigits();
		}
		
		@Override
		public Class getJavaClassForBaseType() {
			Class result = null;
			if (this.xsaTypename != null) {
				result = XSD_TYPE_JAVA_CLASS_MAP.get(this.xsaTypename);
			} else {
				result = this.orig.getJavaClassForBaseType();
			}
			
			return result;
			
		}
		
		@Override
		public int getLength() {
			
			return this.orig.getLength();
		}
		
		@Override
		public int getMaxLength() {
			
			return this.orig.getMaxLength();
		}
		
		@Override
		public double getMaxValue() {
			
			return this.orig.getMaxValue();
		}
		
		@Override
		public int getMinLength() {
			
			return this.orig.getMinLength();
		}
		
		@Override
		public double getMinValue() {
			
			return this.orig.getMinValue();
		}
		
		@Override
		public String getPattern() {
			
			return this.orig.getPattern();
		}
		
		@Override
		public SugtnDeclQName getQName() {
			
			return this.orig.getQName();
		}
		
		@Override
		public int getTotalDigits() {
			
			return this.orig.getTotalDigits();
		}
		
		@Override
		public boolean isEnumerated() {
			
			return this.orig.isEnumerated() || (this.xsaenum.size() > 0);
		}
		
		@Override
		public boolean isListType() {
			boolean result = this.orig.isListType();
			
			if (this.xsaIsList != null) {
				result = this.xsaIsList.booleanValue();
			}
			
			return result;
		}
		
		@Override
		public boolean isRequired() {
			
			return this.xsaRequired;
		}
		
		@Override
		public boolean isUnionType() {
			
			return this.orig.isUnionType();
		}
		
	}
	
	private static final Logger LOG = Logger.getLogger("org.bibalex.XSASDXEDriver");
	
	/**
	 * Overrides the type passed according to type in XSA.xml
	 * 
	 * @param origType
	 * @param xsaInst
	 * @return Overridden type
	 * @throws XSAException
	 */
	public static SugtnTypeDeclaration overrideSugtnType(SugtnTypeDeclaration origType,
			XSAInstance xsaInst)
			throws XSAException {
		try {
			XSASugtnBasicType result = new XSASugtnBasicType(origType);
			
			LinkedList<String> authList;
			
			authList = xsaInst.getAuthorityList();
			
			if (authList != null) {
				
				if (xsaInst.isAuthorityListGrows()) {
					// TODO????
				} else {
					
					result.getEnumeration().clear();
					
					for (String listItem : authList) {
						
						String listItemValue = null;
						String listItemDocum = null;
						int ixEq = listItem.indexOf('=');
						
						if (ixEq != -1) {
							listItemDocum = listItem.substring(0, ixEq - 1).trim();
							listItemValue = listItem.substring(ixEq + 1).trim();
						} else {
							listItemValue = listItem;
							listItemDocum = listItem;
						}
						int quoteIX = listItemValue.indexOf('"');
						if (quoteIX == 0) {
							listItemValue = listItemValue.substring(1);
							listItemValue = listItemValue.substring(0, listItemValue.length() - 1);
						}
						
						XSDEnumMember enumMember = new XSDEnumMember();
						
						enumMember.value = listItemValue;
						
						enumMember.documentation = listItemDocum;
						
						result.getEnumeration().add(enumMember);
					}
					
				}
			}
			
			String typename = xsaInst.getDataTypeName();
			if ((typename != null) && !typename.isEmpty()) {
				result.xsaTypename = typename;
			}
			
			Boolean islist = xsaInst.getDataTypeIsList();
			if (islist != null) {
				result.xsaIsList = islist;
			}
			
			result.xsaRequired = xsaInst.isRequired();
			
			return result;
		} catch (JDOMException e) {
			throw new XSAException(e);
		}
	}
	
	/**
	 * Validates the document against a schema and writes to the log file.
	 * 
	 * @param orig
	 * @param xsdFilePath
	 *            Any schema type; XSD, rng or rnc
	 * @param schemaExt
	 * @param logFilePath
	 * @return 0 if there is nothing wrong, 1 if there are warnings, 2 if there are errors
	 * @throws IOException
	 * @throws XSAValidationError
	 * @throws SAXException
	 * @throws JDOMException
	 * @throws VerifierConfigurationException
	 */
	public static int validate(Document orig, String xsdFilePath, String schemaExt,
			final String logFilePath)
			throws IOException, XSAValidationError, SAXException, JDOMException,
			VerifierConfigurationException {
		
		String uriConst = null;
		
		if ("rng".equals(schemaExt) || "rnc".equals(schemaExt)) {
			uriConst = XMLConstants.RELAXNG_NS_URI;
		} else if ("xsd".equals(schemaExt)) {
			uriConst = XMLConstants.W3C_XML_SCHEMA_NS_URI;
		} else {
			// DTD maybe!
			throw new IllegalArgumentException();
		}
		
		String schemaFilePath = xsdFilePath;
		
		schemaFilePath = schemaFilePath.substring(0, schemaFilePath.lastIndexOf('.')) + "."
				+ schemaExt;
		
		// From
		// http://stackoverflow.com/questions/1541253/how-to-validate-an-xml-document-using-a-relax-ng-schema-and-jaxp
		// and http://www.ibm.com/developerworks/xml/library/x-javaxmlvalidapi.html
		
		if ("rng".equals(schemaExt)) {
// MSV Fails to read enrich-wamcp complicated schema
// // Sun Multi Schema VBalidator (MSV)
// System.setProperty(SchemaFactory.class.getName() + ":"
// + XMLConstants.RELAXNG_NS_URI,
// org.iso_relax.verifier.jaxp.validation.RELAXNGSchemaFactoryImpl.class
// .getCanonicalName());
// JING
			System
					.setProperty(SchemaFactory.class.getName() + ":"
							+ XMLConstants.RELAXNG_NS_URI,
							com.thaiopensource.relaxng.jaxp.XMLSyntaxSchemaFactory.class
									.getCanonicalName());
		} else if ("rnc".equals(schemaExt)) {
// MSV supports only XMl format not compact format
// // Sun Multi Schema VBalidator (MSV)
// System.setProperty(SchemaFactory.class.getName() + ":"
// + XMLConstants.RELAXNG_NS_URI,
// org.iso_relax.verifier.jaxp.validation.RELAXNGSchemaFactoryImpl.class
// .getCanonicalName());
			
			// JING
			System.setProperty(SchemaFactory.class.getName() + ":"
					+ XMLConstants.RELAXNG_NS_URI,
					com.thaiopensource.relaxng.jaxp.CompactSyntaxSchemaFactory.class
							.getCanonicalName());
		}
		
		SchemaFactory factory = SchemaFactory.newInstance(uriConst);
		
		// Compile the schema.
		Schema schema = factory.newSchema(new URL(schemaFilePath));
		
		// Get a validator from the schema.
		Validator validator = schema.newValidator();
		LoggingErrorHandler errorHandler = new LoggingErrorHandler(logFilePath);
		validator.setErrorHandler(errorHandler);
		// Check the document
		try {
			validator.validate(new JDOMSource(orig));
			errorHandler.logWriter.flush();
			
			if (errorHandler.errCount > 0) {
				return 2;
			} else if (errorHandler.warnCount > 0) {
				return 1;
			} else {
				return 0;
			}
		} catch (SAXParseException ex) {
			throw new XSAValidationError(ex);
		}
		
	}
	
	private SDXEMediatorBean sdxeMediator = null;
	
	private final XSADocument xsaDoc;
	
	private final DomTreeController domTarget;
	public static String MAPKEY_MISSINGLIST = "missing";
	
	public static String MAPKEY_PRESENTLIST = "present";
	
	/**
	 * @param sdxeMediator
	 * @param xsaDoc
	 */
	public XSASDXEDriver(SDXEMediatorBean sdxeMediator, XSADocument xsaDoc) {
		this.sdxeMediator = sdxeMediator;
		this.xsaDoc = xsaDoc;
		this.domTarget = sdxeMediator.getDomTreeController();
	}
	
	/**
	 * Checks for mandatory instances under a non base-shifted area
	 * 
	 * @param areaInst
	 * @param parentInst
	 * @param throwOnMissing
	 * @param isRemoveEmptyOccs
	 * @param result
	 * @throws SDXEException
	 * @throws JDOMException
	 */
	private void checkForPresenceInArea(XSAInstance areaInst, XSAInstance parentInst,
			boolean throwOnMissing, boolean isRemoveEmptyOccs,
			HashMap<String, HashMap<XSAInstance, List<String>>> result) throws SDXEException,
			JDOMException {
		if (LOG.isTraceEnabled()) {
			LOG.trace("Going into " + areaInst.toString() + " from "
					+ parentInst.toString() + ". Base Shift Currs: "
					+ areaInst.getBaseShiftCurrs());
		}
		
		HashMap<String, HashMap<XSAInstance, List<String>>> areaMap = this
				.checkPresenceInInstance(areaInst,
						throwOnMissing, isRemoveEmptyOccs);
		
		result.get(MAPKEY_MISSINGLIST).putAll(areaMap.get(MAPKEY_MISSINGLIST));
		result.get(MAPKEY_PRESENTLIST).putAll(areaMap.get(MAPKEY_PRESENTLIST));
	}
	
	/**
	 * Checks for mandatory under a base shifted area
	 * 
	 * @param areaInst
	 * @param baseShiftParts
	 * @param currIx
	 * @param parentInst
	 * @param throwOnMissing
	 * @param isRemoveEmptyOccs
	 * @param result
	 * @throws SDXEException
	 * @throws JDOMException
	 */
	private void checkForPresenceInBaseShifter(XSAInstance areaInst,
			List<String> baseShiftParts, int currIx, XSAInstance parentInst,
			boolean throwOnMissing, boolean isRemoveEmptyOccs,
			HashMap<String, HashMap<XSAInstance, List<String>>> result) throws SDXEException,
			JDOMException {
		
		if (currIx < baseShiftParts.size()) {
			String part = baseShiftParts.get(currIx);
			for (int i = 1; i <= areaInst.getBaseShiftPartMax(part); ++i) {
				areaInst.setBaseShiftPartCurr(part, i);
				this.checkForPresenceInBaseShifter(areaInst, baseShiftParts, currIx + 1,
						parentInst, throwOnMissing, isRemoveEmptyOccs, result);
			}
		} else {
			this.checkForPresenceInArea(areaInst, parentInst, throwOnMissing, isRemoveEmptyOccs,
					result);
		}
	}
	
	/**
	 * 
	 * @param contInst
	 * @param ixesString
	 * @param throwOnMissing
	 * @param isRemoveEmptyOccs
	 * @return
	 * @throws SDXEException
	 * @throws JDOMException
	 */
	private HashMap<String, HashMap<XSAInstance, List<String>>> checkPresenceInContainer(
			XSAInstance contInst,
			String ixesString,/* HashSet<String> enforced, */boolean throwOnMissing,
			boolean isRemoveEmptyOccs)
			throws SDXEException, JDOMException {
		
		HashMap<String, HashMap<XSAInstance, List<String>>> result = new HashMap<String, HashMap<XSAInstance, List<String>>>(
				2,
				(float) 1.0);
		result.put(MAPKEY_MISSINGLIST, new HashMap<XSAInstance, List<String>>());
		result.put(MAPKEY_PRESENTLIST, new HashMap<XSAInstance, List<String>>());
		
		String contXpAbs = contInst.getXPathStr(ixesString, this.domTarget);
		boolean botherLang = false;
		boolean bAllEmpty = true;
		List<XSAInstance> lstXSAInst = null;
// if (enforced.contains(contXpAbs)) {
// return result;
// }
		
		if (LOG.isTraceEnabled()) {
			LOG.trace("Checking presence in container " + contInst.toString());
		}
		
		int contOccurs = this.domTarget
				.getElementCountAtXPathRelToActive(contXpAbs);
		
		// iterate from high to low to prevent change of Ix in case of deletion
		for (int contIx = contOccurs; contIx > 0; --contIx) {
			boolean keepThisContainerOcc = false;
			
			for (XSAInstance subContInst : this.xsaDoc.getSubDivisionsUnder( // Below(
					XSAUiDivisions.container, contInst/* , false */)) {
				
				String subContIxesStr = XPathStrUtils.removeLastStep(ixesString) + "/" + contIx;
				int stepDiff = XPathStrUtils.getStepsCount(subContInst.getLocatorString())
						- XPathStrUtils.getStepsCount(contInst.getLocatorString());
				for (int j = 0; j < stepDiff - 1; ++j) {
					subContIxesStr += "/1";
				}
				
				subContIxesStr += "/0";
				
				if (LOG.isTraceEnabled()) {
					LOG.trace("Going into " + subContInst.toString() + subContIxesStr + " from "
							+ contInst.toString());
				}
				HashMap<String, HashMap<XSAInstance, List<String>>> contMap = this
						.checkPresenceInContainer(
								subContInst, subContIxesStr, /* enforced, */throwOnMissing,
								isRemoveEmptyOccs);
				
				result.get(MAPKEY_MISSINGLIST).putAll(contMap.get(MAPKEY_MISSINGLIST));
				result.get(MAPKEY_PRESENTLIST).putAll(contMap.get(MAPKEY_PRESENTLIST));
				
			}
			LinkedList<String> lstAuth = null;
			lstXSAInst = this.xsaDoc.getSubDivisionsUnder(XSAUiDivisions.field,
					contInst);
			bAllEmpty = true;
			for (XSAInstance fieldInst : lstXSAInst) {
				try{
					lstAuth =	fieldInst.getAuthorityList();
				}catch (JDOMException e) 
				{//YM Do Nothing :)
				}
				
// if (enforced.contains(fieldInst.toString() + ixesString)) {
// continue;
// }
//YM16062011  Debug REmoving Empty Elements "Unlock" 				
//				
//				XSALocator locator = fieldInst.getLocator();
//				if(locator != null)
//				{
//					if("/TEI/text/body/div/msDesc/msPart/msContents/msItem/title/@xml:lang".equals(locator.asString()))
//						LOG.trace("Start Debug 31");
//					if("/TEI/text/body/div/msDesc/msPart/msContents/msItem/textLang/@mainLang".equals(locator.asString()))
//						LOG.trace("Start Debug 13");
//				}
				
				if(fieldInst.getLabel().equals("Language") && contInst.getLabel().equals("Other title")) 
				{
					botherLang= true;
				}
				String fieldIxesStr = XPathStrUtils.removeLastStep(ixesString) + "/" + contIx;
				int stepDiff = XPathStrUtils.getStepsCount(fieldInst.getLocatorString())
						- XPathStrUtils.getStepsCount(contInst.getLocatorString());
				for (int j = 0; j < stepDiff - 1; ++j) {
					fieldIxesStr += "/1";
				}
				fieldIxesStr += "/0";
				
				String fieldXpAbs = fieldInst.getXPathStr(fieldIxesStr, this.domTarget);
				
				int fieldOccurs = this.domTarget
						.getElementCountAtXPathRelToActive(fieldXpAbs);
				
				// YA 20100718 Delete the empty fields we added
				if (isRemoveEmptyOccs && (fieldOccurs > 0)) {
					
					fieldOccurs = this.removeEmptyOccs(fieldInst, fieldXpAbs, fieldOccurs);
					
					// The container contains something.. an element or attribute
					keepThisContainerOcc = keepThisContainerOcc || (fieldOccurs > 0);
					
				}
				// END YA 20100728
				if(fieldOccurs > 0 )
					bAllEmpty = false;
				if ((fieldInst.isRequired() && (fieldOccurs == 0))
						|| (fieldInst.isFacsRequired()
						&& this.checkPresenceOfFacs(fieldXpAbs, fieldOccurs))) {
					//YM 27072011 Modification for Flaps
					boolean flag =false;
					if(lstAuth != null ){
						
						for(int i=0; i < lstAuth.size(); i++)
						{//case of empty values in Authority list
							if(lstAuth.get(i).substring(lstAuth.get(i).indexOf('=')+1).trim().equals("\"\""))
							{//
								flag=true;
								break;
							}
								
						}
					}
					if(flag)
					{
						break;
					}
					else if(botherLang)
					{//Y.M 03082011 special handling for Other language until we modify the containers required fileds
						//language is empty wait until check on container Occurrence 						
						break;
					}
					else if (throwOnMissing) {
						throw new XSAMissingRequiredValExcpetion(fieldInst, fieldIxesStr);
					}					
					this.getIxesListForInstance(result, MAPKEY_MISSINGLIST, fieldInst).add(
							fieldIxesStr);
					
				} else {
					
					this.getIxesListForInstance(result, MAPKEY_PRESENTLIST, fieldInst).add(
							fieldIxesStr);
					
				}
				
// enforced.add(fieldInst.toString() + ixesString);
				
			}
			
			// YA 20100718 Delete the empty fields we added
			if (isRemoveEmptyOccs && (contOccurs > 0) && !keepThisContainerOcc) { // The contOccurs check is only to
				// make Debug easier
				
				String xpAbs = XPathStrUtils.setLastIx(contXpAbs, contIx);
				
				SugtnElementNode sugtnNodeForContainer = (SugtnElementNode) this.sdxeMediator
						.getSugtnTreeController()
						.getNodeAtSugtnPath(contInst.getLocatorString());
				
				String value = this.domTarget.getValueAtXPathRelToActive(xpAbs,
						sugtnNodeForContainer);
				
				if ((value == null) || value.trim().isEmpty()) {
					
					if (LOG.isDebugEnabled()) {
						LOG.debug("Removing emtpy occurence at: " + xpAbs);
					}
					
					this.deleteFromDom(xpAbs, sugtnNodeForContainer, contInst, false);
					
					// reduce the occurs
					--contOccurs;
				} else {
					LOG.trace("Occurence at: " + xpAbs + " has the value: " + value);
				}
				
			}
			// END YA 20100728 Delete
			
		}
		
		// YA 20100718 To identify the empty elements this has to be done after all children
		if(contOccurs >0 && botherLang && bAllEmpty )
		{//Y.M 03082011 special handling for Other language until we modify the containers required fileds
			//language is required if container contains value for the other title
			throw new XSAMissingRequiredValExcpetion(lstXSAInst.get(0), ixesString);
		}
		
		if ((contOccurs == 0) && contInst.isRequired()) {
			
			if (throwOnMissing) {
				throw new XSAMissingRequiredValExcpetion(contInst, ixesString);
			}
			this.getIxesListForInstance(result, MAPKEY_MISSINGLIST, contInst).add(ixesString);
			
		} else {
			
			this.getIxesListForInstance(result, MAPKEY_PRESENTLIST, contInst).add(ixesString);
			
		}
		// END YA 20100718 To identify the empty elements this has to be done after all children
		
// enforced.add(contXpAbs);
		return result;
	}
	
	/**
	 * Call this to check for mandatory instances under the passed instance.
	 * 
	 * @param parentInst
	 *            The instance from which checking begins
	 * @param throwOnMissing
	 *            Throws exception when a required instance is a missing
	 * @param isRemoveEmptyOccs
	 *            Deletes XML nodes of occurrences with no content, and doesn't get fooled by presence of white space or
	 *            designating attributes
	 * @return Two maps in a map (keyed array). One for instances that are present (regardless mandatory or not), and
	 *         the other for instances that are mandatory but missing.
	 * @throws JDOMException
	 * @throws SDXEException
	 */
	public HashMap<String, HashMap<XSAInstance, List<String>>> checkPresenceInInstance(
			XSAInstance parentInst,
			boolean throwOnMissing, boolean isRemoveEmptyOccs)
			throws JDOMException,
			SDXEException {
		
		if (LOG.isTraceEnabled()) {
			LOG.trace("Checking presence in instance " + parentInst.toString());
		}
		
		
// HashSet<String> enforced = new HashSet<String>();
		
		HashMap<String, HashMap<XSAInstance, List<String>>> result =
				new HashMap<String, HashMap<XSAInstance, List<String>>>(2, (float) 1.0);
		result.put(MAPKEY_MISSINGLIST, new HashMap<XSAInstance, List<String>>());
		result.put(MAPKEY_PRESENTLIST, new HashMap<XSAInstance, List<String>>());
		// TODONOT guard that instance is area instance
		
		if (XSAUiDivisions.site.equals(parentInst.getUiDivision())) {
			// We are checking the whole site, so check all areas
			if (LOG.isDebugEnabled()) {
				LOG.debug("Checking presence in instance called with the site isntance. Stack Trace follows: ");
				
				StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
				int maxIx = 10;
				int minIx = 0;
				if (maxIx > stackTrace.length - 1) {
					maxIx = stackTrace.length - 1;
				}
				for (int i = minIx; i <= maxIx; ++i) {
					LOG.debug(stackTrace[i].toString());
				}
				
			}
			boolean siteHasAreas = false;
			for (XSAInstance areaInst : this.xsaDoc.getSubDivisionsUnder(XSAUiDivisions.area,
					parentInst)) {
				siteHasAreas = true;
				if (!areaInst.isBaseShifted()) {
					
					this.checkForPresenceInArea(areaInst, parentInst, throwOnMissing,
							isRemoveEmptyOccs, result);
				} else {
					
					// FIXMENOT: This changes session (global) variables.. a side effect
					LOG.warn("Forcing Clearing Base Shifters Maps.. such call must be followed by doGoToAreas");
					areaInst.clearBaseShifterMaps();
			//YM-YN 1606 2011 the following line was added to re-populate base shifter 	after clearing it, as a re-initialization step for base shifters	
					areaInst.populateBaseShifterMaps(domTarget);
					
					this.checkForPresenceInBaseShifter(areaInst, areaInst.getBaseShiftParts(), 0,
							parentInst, throwOnMissing, isRemoveEmptyOccs, result);
					
				}
				
			}
			// TODONE Isn't this enough?? can't we return now?? Wouldn't all the instances checked by the code below
// would have been called with recursive calls? At most it may be necessary to go on if there are no areas!
			if (siteHasAreas) {
				return result;
			}
		}
		
		// For areas or sites with no areas or even containers, we first check for containers, directly or inderctly
// below
		for (XSAInstance contInst : this.xsaDoc.getSubDivisionsBelow(XSAUiDivisions.container,
				parentInst, false)) {
			if (LOG.isTraceEnabled()) {
				LOG.trace("Going into " + contInst.toString() + ":0 from " + parentInst.toString());
			}
			HashMap<String, HashMap<XSAInstance, List<String>>> contMap = this
					.checkPresenceInContainer(contInst,
							"/0", throwOnMissing, isRemoveEmptyOccs);
			
			result.get(MAPKEY_MISSINGLIST).putAll(contMap.get(MAPKEY_MISSINGLIST));
			result.get(MAPKEY_PRESENTLIST).putAll(contMap.get(MAPKEY_PRESENTLIST));
		}
		
		// Then we check for fields directly or inderctly under the instance, but not in containers (alerady checked)
		for (XSAInstance fieldInst : this.xsaDoc.getSubDivisionsBelow(XSAUiDivisions.field,
				parentInst, false)) {
			
// if (enforced.contains(fieldInst.toString() + "/0")) {
// continue;
// }
			if (XSAUiDivisions.container.equals(fieldInst.getContainingInst())) {
				// This is instead of keeping a set of enforced..
				// if it is a field in a container it has been enforced earlier
				if (LOG.isTraceEnabled()) {
					LOG.trace("Skipping " + fieldInst.toString());
				}
				continue;
			}
			if (LOG.isTraceEnabled()) {
				LOG.trace("Checking " + fieldInst.toString() + " from " + parentInst.toString());
			}
			
			String fieldXpAbs = fieldInst.getXPathStr("/0", this.domTarget);
			
			int fieldOccurs = this.domTarget
					.getElementCountAtXPathRelToActive(fieldXpAbs);
			
			// YA 20100718 Delete the empty fields we added
			if (isRemoveEmptyOccs) {
				fieldOccurs = this.removeEmptyOccs(fieldInst, fieldXpAbs, fieldOccurs);
				// END YA 20100728
			}
			
			if ((fieldInst.isRequired() && (fieldOccurs == 0))
					|| (fieldInst.isFacsRequired()
					&& this.checkPresenceOfFacs(fieldXpAbs, fieldOccurs))) {
				if (throwOnMissing) {
					throw new XSAMissingRequiredValExcpetion(fieldInst, "/0");
				}
				
				this.getIxesListForInstance(result, MAPKEY_MISSINGLIST, fieldInst).add("/0");
				
			} else {
				
				this.getIxesListForInstance(result, MAPKEY_PRESENTLIST, fieldInst).add("/0");
				
			}
			
// enforced.add(fieldInst.toString() + "/0");
		}
		
		return result;
		
	}
	
	/**
	 * WAMCP Specific
	 * 
	 * @param fieldXpAbs
	 * @param fieldOccurs
	 * @return true if the facs is not (required and missing)
	 * @throws SDXEException
	 * @throws JDOMException
	 */
	private boolean checkPresenceOfFacs(String fieldXpAbs, int fieldOccurs) throws SDXEException,
			JDOMException {
		String facsXpAbs = fieldXpAbs + "[@" + XSAConstants.WAMCP_FACS_ATTR_NAME + "]";
		int facsOccurs = this.domTarget
				.getElementCountAtXPathRelToActive(facsXpAbs);
		
		return facsOccurs == fieldOccurs;
	}
	
	/**
	 * Deletes absolutely empty nodes from XML; those with no text or attributes. That is, if an instance is empty but
	 * has designating attributes, it will not be removed. This is to clean the XML after removing empty element by the
	 * checkForPresence function. Annotators are execluded, becaus many of them are empty.
	 * 
	 * @throws XSAException
	 * @throws JDOMException
	 * @throws DomBuildException
	 */
	public void deleteEmptyElements() throws XSAException, JDOMException, DomBuildException {
		List<String> execlusionList = new LinkedList<String>();
		for (XSAAnnotator annotator : this.xsaDoc.getAnnotators()) {
			Element execluded = new Element(annotator.getEltLocalName(),
					this.domTarget.getDomTotal().getRootElement()
							.getNamespace("").getURI());
			// TODO Support namespace: annotator.getEltNamespace());
			execlusionList.add(execluded.toString());
		}
		
		this.domTarget.deleteEmptyElements(execlusionList);
		
	}
	
	/**
	 * Deletes an occurrence
	 * 
	 * @param xPathAbs
	 * @param declToDelete
	 * @param xsaInst
	 * @param isUserRequest
	 * @throws SDXEException
	 * @throws JDOMException
	 */
	public void deleteFromDom(String xPathAbs, SugtnDeclNode declToDelete, XSAInstance xsaInst,
			boolean isUserRequest)
			throws SDXEException, JDOMException {
		
		XSAInstance xsaInstCont = xsaInst.getContainingInst();
		
		if (xsaInstCont == null) {
			LOG.warn("Site instance " + xsaInst + " being deleted!! REFUSED!");
			return;
		}
		
		if (LOG.isTraceEnabled()) {
			LOG.trace("Deleting from instance " + xsaInst + " to containing instance "
						+ xsaInstCont);
		}
		
		Element domParent = null;
		
		// Delete the target node
		if (declToDelete instanceof SugtnElementNode) {
			// If this is not a manual order
			if (!isUserRequest) {
				// first make sure that it doesn't hold attributes that we care about
				LinkedList<Element> domElts = this.domTarget
						.getElementCloneListAtXPathRelToActive(xPathAbs);
				
				if ((domElts != null) && (domElts.size() > 0)) {
					if (domElts.size() > 1) {
						LOG.warn("More than one element returned while one was expected");
					}
					
					for (Element occElt : domElts) {
						for (Object occAttrObj : occElt.getAttributes()) {
							
							Attribute occAttr = (Attribute) occAttrObj;
							SugtnDeclQName occAttrQName = new SugtnDeclQName(
									occAttr.getNamespaceURI(),
									occAttr.getName());
							if (xsaInst.getAttributeNames().contains(occAttrQName)) {
								continue; // designating attribute.. doesn't hold data
							}
							
							if ((occAttr.getValue() != null)
									&& !occAttr.getValue().trim().isEmpty()) {
								// TODONOT add "FILL IN".eqauls(getValue.trim())
								return; // non designating attribute that really contains data.. we cannot delete!
							}
						}
						
					}
					
				}
			}
			domParent = this.domTarget.deleteEltAtXPathRelToActive(xPathAbs,
					(SugtnElementNode) declToDelete);
			
		} else if (declToDelete instanceof SugtnAttributeNode) {
			
			String xPathElt;
			if (XPathStrUtils.getLastStep(xPathAbs).startsWith("@")) {
				xPathElt = XPathStrUtils.removeLastStep(xPathAbs);
			} else {
				xPathElt = xPathAbs;
			}
			
			domParent = this.domTarget.setValueAtXPathRelToActive(
					xPathElt, null, (SugtnAttributeNode) declToDelete);
			
		} else {
			
			throw new IllegalArgumentException("Never happnes?");
			
		}
		
		if (domParent == null) {
			// nothing more to delete.. maybe nothing was there to delete at all
			return;
		}
		
		XSALocator locatorNew = xsaInstCont.getLocator();
		if (locatorNew == null) {
			// we have reached the area or section, nothing more to delete
			// delete domParent and all its parents until the first one that has children
			// It shouldn't contain attrbiutes but better safe than sorry!
			while (domParent.getChildren().isEmpty() && domParent.getAttributes().isEmpty()) {
				Element domEltToDelete = domParent;
				domParent = domParent.getParentElement();
				domEltToDelete.detach();
			}
			return;
		}
		
		// The xsaInstance that was deleted for sure has a locator
		int steps = XPathStrUtils.getStepsCount(xsaInst.getLocatorString())
				- XPathStrUtils.getStepsCount(locatorNew.toString());
		
		for (int i = 0; i < steps - 1; ++i) { // we already deleted 1 step
		
			// check if the intermediate (non-instance occ) element
			// holds other elements..
			// It shouldn't contain attrbiutes but better safe than sorry!
			if (!domParent.getChildren().isEmpty() || !domParent.getAttributes().isEmpty()) {
				// it does.. so we have to leave it alone
				return;
			} else {
				// it will be deleted.. move it up
				domParent = domParent.getParentElement();
			}
			
			xPathAbs = XPathStrUtils.removeLastStep(xPathAbs);
			declToDelete = declToDelete.getParentSugtnElement();
			
			domParent = this.domTarget.deleteEltAtXPathRelToActive(xPathAbs,
					(SugtnElementNode) declToDelete);
			
		}
	}
	
	/**
	 * 
	 * @param map
	 * @param listId
	 * @param inst
	 * @return
	 */
	private List<String> getIxesListForInstance(
			HashMap<String, HashMap<XSAInstance, List<String>>> map, String listId, XSAInstance inst) {
		
		List<String> result = map.get(listId).get(inst);
		
		if (result == null) {
			result = new LinkedList<String>();
			map.get(listId).put(inst, result);
		}
		
		return result;
	}
	
	/**
	 * Creates an occurrence of an instance, creating any containing XML nodes and instances that are not already there.
	 * 
	 * @param xsaInst
	 *            The instance
	 * @param sugtnDecl
	 *            The decl of the locator of the inst above.. stupid!
	 * @param xPathAbs
	 *            The XPath extracted from the inst above, with the ixesBetBrosPath in the session bean.. also stupid!
	 * @param targetIxBetBros
	 *            Set this to -1 to add at the tail of the list, or to a number less than the list size to add in the
	 *            middle.
	 * @throws JDOMException
	 * @throws SDXEException
	 */
	public void newInDom(XSAInstance xsaInst, SugtnDeclNode sugtnDecl, String xPathAbs,
			int targetIxBetBros)
			throws JDOMException,
			SDXEException {
		
		String xPathNewElt = xPathAbs; // Because XPathAbs is manipulated a lot here
		
		// int steps =
		Element newEltsRoot = this.domTarget.addSugtnAtXPathRelToActive(
				XPathStrUtils.removeLastStep(xPathAbs),
				sugtnDecl);
		
		// YA20100729 Adding an attribute to an existing element Bug Fix
		if (newEltsRoot == null) {
			return;
		}
		// End YA
		
// Sometimes we overshoot.. using steps is better: Element newEltsRootParent = newEltsRoot.getParentElement();
		
		SugtnElementNode sugtnParent = null; // sugtnElt;
		if (sugtnDecl instanceof SugtnElementNode) {
			
			sugtnParent = (SugtnElementNode) sugtnDecl;
			
		} else if (sugtnDecl instanceof SugtnAttributeNode) {
			
			sugtnParent = sugtnDecl.getParentSugtnElement();
			
		}
		
		Element newElt = newEltsRoot;
		
		// count elements with child elements added
		int steps = 1;
		while (newElt.getChildren().size() > 0) { // yes, getchildren returns num of elements only
			assert newElt.getChildren().size() == 1 : "Adding an element lead to a branching tree!!!";
			newElt = (Element) newElt.getChildren().get(0);
			++steps;
		}
		
		while (steps > 0) {
			
			if (LOG.isTraceEnabled()) {
				
				LOG.trace("Adding attributes from Instance: " + xsaInst + " to element "
							+ newElt);
				
				LOG.trace("XPath Absolute is: " + xPathAbs);
				
			}
			
			HashSet<SugtnDeclQName> attribsSet = new HashSet<SugtnDeclQName>();
			attribsSet.addAll(xsaInst.getAttributeNames());
			
// // Add the in memory attribute of XSA Instance Index Between Bros
// SugtnDeclQName attrIxBetBrosQName = new SugtnDeclQName("",
// XSAInstance.XSA_ATTR_INSTANCE_INDEX_DONT_PUBLISH);
// String attrIxBetBrosVal = xsaInst.getAttributeValue(attrIxBetBrosQName);
// newElt.setAttribute(attrIxBetBrosQName.getLocalName(), attrIxBetBrosVal,
// Namespace.NO_NAMESPACE);
// attribsSet.remove(attrIxBetBrosQName);
			
			SugtnPCDataNode pcDataNode = null;
			// Loop for the possible attibutes and pcdata to set them if they are set in XSA
			for (int j = 0; !attribsSet.isEmpty() && (j < sugtnParent.getChildCount()); ++j) {
				SugtnTreeNode sugtnChild = (SugtnTreeNode) sugtnParent.getChildAt(j);
				
				if (sugtnChild instanceof SugtnAttributeNode) {
					SugtnDeclQName attribQName = ((SugtnAttributeNode) sugtnChild).getqName();
					
					if (attribsSet.contains(attribQName)) {
						
						String attribVal = xsaInst.getAttributeValue(attribQName);
						
						// This won't work beacause the XPath will not return the element since
						// we are still setting its arributes
// this.domTarget.setValueAtXPathRelToActive(
// xPathAbs, attribVal, (SugtnAttributeNode) sugtnChild);
						
						Namespace attribNs = DomTreeController
								.acquireNamespaceNoParent((SugtnAttributeNode) sugtnChild);
						
						// If value is not specified in XSA, get from XSD
						if ((attribVal == null) || attribVal.isEmpty()) {
							
							String valueInXSD = null;
							
							SugtnDeclNode declNode = (SugtnDeclNode) sugtnChild;
							
							valueInXSD = declNode.getFixedValue();
							if ((valueInXSD == null) || valueInXSD.isEmpty()) {
								valueInXSD = declNode.getDefaultValue();
								
							}
							
							attribVal = valueInXSD;
						}
						
						if ((attribVal != null) && !attribVal.isEmpty()) {
							newElt.setAttribute(attribQName.getLocalName(), attribVal, attribNs);
						}
						
						attribsSet.remove(attribQName);
					}
					
				} else if (sugtnChild instanceof SugtnPCDataNode) {
					
					pcDataNode = (SugtnPCDataNode) sugtnChild;
// Setting the default value in XML is not good now that we automatically add fields . It is set in the field bind
// method in XSAUiGenFactory
// String pcDataVal = xsaInst.getPCDataValue();
//
// // If value is not specified in XSA, get from XSD
// if ((pcDataVal == null) || pcDataVal.isEmpty()) {
//
// String valueInXSD = null;
//
// SugtnDeclNode declNode = (SugtnDeclNode) sugtnChild;
//
// valueInXSD = declNode.getFixedValue();
// if ((valueInXSD == null) || valueInXSD.isEmpty()) {
// valueInXSD = declNode.getDefaultValue();
//
// }
//
// pcDataVal = valueInXSD;
// }
//
// if ((pcDataVal != null) && !pcDataVal.isEmpty()) {
// this.domTarget.setValueAtXPathRelToActive(
// xPathAbs, pcDataVal, (SugtnPCDataNode) sugtnChild);
// }
				} else {
					break;
				}
			}
			
// YA 20101010 Being DRY between XSAUiGenFactory.bindValue and XSASDXEDriver.newInDom
// // We can add a default value to either a PCData-holding element or an attribute
// if ((pcDataNode != null) || xsaInst.isInstForAttr()) {
// // Set value according to XSA and XSD
// String valueDefault = null;
// try {
// valueDefault = xsaInst.getDefaultValue();
// } catch (JDOMException e) {
// throw new XSAException(e);
// }
//
// if ((valueDefault == null) || valueDefault.isEmpty()) {
//
// // this is an element or an attribute explicitly stated in XSA
//
// // So we can get its default from the XSD!!
//
// valueDefault = sugtnDecl.getFixedValue();
//
// if ((valueDefault == null) || valueDefault.isEmpty()) {
// valueDefault = sugtnDecl.getDefaultValue();
// }
// }
//
// if ((valueDefault != null) && !valueDefault.isEmpty()) {
// if (sugtnDecl instanceof SugtnAttributeNode) {
//
// newElt.setAttribute(sugtnDecl.getLocalName(), valueDefault,
// DomTreeController
// .acquireNamespaceNoParent((SugtnAttributeNode) sugtnDecl));
//
// } else if (pcDataNode != null) {
//
// this.domTarget.setValueAtXPathRelToActive(
// xPathAbs, valueDefault, pcDataNode);
//
// } else {
// LOG
// .error("Default value provided but the element cannot hold text for element: "
// + xPathAbs);
// }
// }
// }
// END YA 20101010 Being DRY
			
			if (!attribsSet.isEmpty()) {
				throw new XSAException("XSA instance with locator " + xsaInst.getLocator()
						+ " defines attribute(s) " + attribsSet
						+ " that are not a valid child for the instance element");
			}
			
			// Move up to the containing instance
			XSAInstance xsaInstCont = xsaInst.getContainingInst();
			
			if (xsaInstCont != null) {
				if (LOG.isTraceEnabled()) {
					
					LOG.trace("Moving from instance " + xsaInst + " to containing instance "
								+ xsaInstCont);
					
				}
				xsaInst = xsaInstCont;
				
			} else {
				LOG.warn("Site instance " + xsaInst
						+ " encountered.. loop should have been broken earlier!!");
				break;
			}
			
			XSALocator locatorNew = xsaInst.getLocator();
			if (locatorNew == null) {
				// we have reached the area or section, nothing more to add
				break;
			}
			int numXPathSteps = XPathStrUtils.getStepsCount(
					locatorNew.asString());
			
			assert XPathStrUtils.getStepsCount(xPathAbs) != numXPathSteps : "This loop will be endless!";
			
			while ((steps > 0) && (XPathStrUtils.getStepsCount(xPathAbs) != numXPathSteps)) {
				
				xPathAbs = XPathStrUtils.removeLastStep(xPathAbs);
				
				--steps;
				newElt = newElt.getParentElement();
			}
			
			if (steps > 0) {
				// Get the sugtnElement bby the locator string
				sugtnParent = (SugtnElementNode) this.sdxeMediator.getSugtnTreeController()
						.getNodeAtSugtnPath(xsaInst.getLocator().asString());
			} // else save this unneccessary movememnt of sugtn Tree
			
		}
		
		// YA20110216 move the newly added element to the indicated location between bros
		if ((sugtnDecl instanceof SugtnElementNode) && (targetIxBetBros > 0)) {
			this.domTarget.moveLastEltToIxBetBros(xPathNewElt, targetIxBetBros);
		}
		// END moving
		
		// return result;
	}
	
	/**
	 * 
	 * @param attrNode
	 * @param xsaInst
	 * @return
	 * @throws XSAException
	 */
	public SugtnTypeDeclaration overrideSugtnTypeForChildAttribute(
			SugtnAttributeNode attrNode, XSAInstance xsaInst) throws XSAException {
		try {
			XSASugtnBasicType result = new XSASugtnBasicType(attrNode.getType());
			
			// TODO WAMCP specific code: refactor
			if ("facs".equals(attrNode.getLocalName())) {
				
				result.xsaTypename = "IDREFS";
				result.xsaIsList = true;
				
				result.xsaenum.clear();
				
				Element root = this.domTarget.getDomTotal().getRootElement();
				
				Namespace nsXp = DomTreeController.acquireNamespace(root
						.getNamespaceURI(), root, true);
				String pfx = nsXp.getPrefix();
				if ((pfx != null) && !pfx.isEmpty()) {
					pfx += ":";
				}
				
				String surfsStr = "//" + pfx + "facsimile/" + pfx + "surface";
				XPath surfsXP = XPath.newInstance(surfsStr);
				surfsXP.addNamespace(nsXp);
				
				for (Object surfObj : surfsXP.selectNodes(root)) {
					Element surfElt = (Element) surfObj;
					
					String ref = surfElt.getAttributeValue("id", Namespace.XML_NAMESPACE);
					if ((ref == null) || ref.isEmpty()) {
						continue;
					}
					
					ref = "#" + ref;
					
					Element graphElt = surfElt.getChild("graphic", root.getNamespace());
					if (graphElt == null) {
						continue;
					}
					
					String name = graphElt.getAttributeValue("url");
					
					if ((name == null) || name.isEmpty()) {
						continue;
					}
					
					XSDEnumMember enumMem = new XSDEnumMember();
					enumMem.documentation = name;
					enumMem.value = ref;
					
					result.xsaenum.add(enumMem);
				}
				
			}
			return result;
		} catch (JDOMException e) {
			throw new XSAException(e);
		} catch (DomBuildChangesException e) {
			throw new XSAException(e);
		}
	}
	
	/**
	 * 
	 * YA 20100718 Delete the empty fields we added
	 * 
	 * @param xsaInst
	 * @param xpAbs
	 * @param origOccurs
	 * @return
	 * @throws SDXEException
	 * @throws JDOMException
	 */
	
	private int removeEmptyOccs(XSAInstance xsaInst, String xpAbs, int origOccurs)
			throws SDXEException, JDOMException {
		
		SugtnDomAddableNode sugtnNodeForTarget = this.sdxeMediator.getSugtnTreeController()
				.getNodeAtSugtnPath(xsaInst.getLocatorString());
		
		int result = origOccurs;
		
		// iterate from high to low to retain ix after deletion
		for (int ix = origOccurs; ix > 0; --ix) {
			
			if (sugtnNodeForTarget instanceof SugtnElementNode) {
				xpAbs = XPathStrUtils.setLastIx(xpAbs, ix);
			} else if (sugtnNodeForTarget instanceof SugtnAttributeNode) {
				xpAbs = XPathStrUtils.removeLastStep(xpAbs);
			}
			
			String value = this.domTarget.getValueAtXPathRelToActive(xpAbs,
					sugtnNodeForTarget);
			
			boolean remove;
			
			if (value == null) {
				remove = true;
			} else {
				String valueTrim = value.trim();
				remove = valueTrim.isEmpty() || "FILL IN".equals(valueTrim);
			}
			
			if (remove) {
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("Removing emtpy occurence at: " + xpAbs);
				}
				
				// Delete the empty node
				this.deleteFromDom(xpAbs, (SugtnDeclNode) sugtnNodeForTarget, xsaInst, false);
				
				// reduce the occurs
				--result;
				
			}
		}
		
		return result;
	}
}
