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

package org.bibalex.sdxe.suggest.model.dom;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.bibalex.sdxe.suggest.model.SugtnUtil;

import com.sun.xml.xsom.XSFacet;
import com.sun.xml.xsom.XSRestrictionSimpleType;
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.XSVariety;

/**
 * Class to transform schema.xsd file into java objects.
 *	
 * It simply checks for each node in schema.xsd and constructs an appropriate Java object for it.
 *  
 */
public class SugtnTypeDeclaration {
	
	/**
	 * Class represents xs:enumeration node where its schema looks this:
	 * < xs:enumeration value="......." >
	 * 		< xs:annotation >
	 * 			< xs:documentation >......< /xs:documentation >
     *      < /xs:annotation >
     * < /xs:enumeration >
	 */
	public static class XSDEnumMember {
		/**
		 * "value" attribute of enumeration node.
		 */
		public String value;
		/**
		 * "documentation" node of the annotation node under enumeration node.
		 */
		public String documentation;
	}

	/**
	 * Any type.
	 * For Java class mapping, see  {@link SugtnTypeDeclaration#XSD_TYPE_JAVA_CLASS_MAP XSD_TYPE_JAVA_CLASS_MAP} .
	 *  
	 */
	public static final String XSD_BASE_TYPE_NAME = "anyType";
	
	/**
	 * Any Simple type.
	 * For Java class mapping, see  {@link SugtnTypeDeclaration#XSD_TYPE_JAVA_CLASS_MAP XSD_TYPE_JAVA_CLASS_MAP} .
	 *  
	 */
	public static final String XSD_BASE_SIMPLE_TYPE_NAME = "anySimpleType";
	
	/**
	 * Map XSD memberTypes and itemType attributes to Java class.
	 *  Mapping is done as followed:
	 *  <ul>
	 *  	<li> XSD_BASE_TYPE_NAME --> java.lang.String.class
	 *  	<li> XSD_BASE_SIMPLE_TYPE_NAME --> java.lang.String.class
	 *  	<li> null --> java.lang.String.class
	 *		<li> xs:base64Binary --> byte[].class
	 *		<li> xs:boolean --> boolean.class
	 *		<li> xs:byte --> byte.class
	 *		<li> xs:date --> javax.xml.datatype.XMLGregorianCalendar.class
	 *		<li> xs:dateTime --> javax.xml.datatype.XMLGregorianCalendar.class
	 *		<li> xs:decimal --> java.math.BigDecimal.class
	 *		<li> xs:double --> double.class
	 *		<li> xs:duration --> javax.xml.datatype.Duration.class
	 *		<li> xs:float --> float.class
	 *		<li> xs:gYearMonth --> javax.xml.datatype.XMLGregorianCalendar.class
	 *		<li> xs:gYear --> javax.xml.datatype.XMLGregorianCalendar.class
	 *		<li> xs:gMonthDay --> javax.xml.datatype.XMLGregorianCalendar.class
	 *		<li> xs:gMonth --> javax.xml.datatype.XMLGregorianCalendar.class
	 *		<li> xs:gDay --> javax.xml.datatype.XMLGregorianCalendar.class
	 *		<li> xs:hexBinary --> byte[].class
	 *		<li> xs:int --> int.class
	 *		<li> xs:integer --> java.math.BigInteger.class
	 *		<li> xs:long --> long.class
	 *		<li> xs:NOTATION --> javax.xml.namespace.QName.class
	 *		<li> xs:QName --> javax.xml.namespace.QName.class
	 *		<li> xs:short --> short.class
	 *		<li> xs:string --> java.lang.String.class
	 *		<li> xs:time --> javax.xml.datatype.XMLGregorianCalendar.class
	 *		<li> xs:unsignedByte --> short.class
	 *		<li> xs:unsignedInt --> long.class
	 *		<li> xs:unsignedShort --> int.class
	 *		<li> xs:anyURI --> java.net.URI.class
	 *		<li> xs:IDREF --> String.class
	 *		<li> xs:IDREFS --> String[].class
	 *  </ul>
	 */
	protected static final HashMap<String, Class> XSD_TYPE_JAVA_CLASS_MAP = new HashMap<String, Class>();
	
	static {
		// Object is not useful at all and it will be a string in XML after all
// XSD_TYPE_JAVA_CLASS_MAP.put(XSD_BASE_TYPE_NAME, java.lang.Object.class);
// XSD_TYPE_JAVA_CLASS_MAP.put(XSD_BASE_SIMPLE_TYPE_NAME, java.lang.Object.class);
// XSD_TYPE_JAVA_CLASS_MAP.put(null, java.lang.Object.class);
		XSD_TYPE_JAVA_CLASS_MAP.put(XSD_BASE_TYPE_NAME, java.lang.String.class);
		XSD_TYPE_JAVA_CLASS_MAP.put(XSD_BASE_SIMPLE_TYPE_NAME, java.lang.String.class);
		XSD_TYPE_JAVA_CLASS_MAP.put(null, java.lang.String.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("base64Binary", byte[].class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("boolean", boolean.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("byte", byte.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("date", javax.xml.datatype.XMLGregorianCalendar.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("dateTime", javax.xml.datatype.XMLGregorianCalendar.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("decimal", java.math.BigDecimal.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("double", double.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("duration", javax.xml.datatype.Duration.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("float", float.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("gYearMonth", javax.xml.datatype.XMLGregorianCalendar.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("gYear", javax.xml.datatype.XMLGregorianCalendar.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("gMonthDay", javax.xml.datatype.XMLGregorianCalendar.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("gMonth", javax.xml.datatype.XMLGregorianCalendar.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("gDay", javax.xml.datatype.XMLGregorianCalendar.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("hexBinary", byte[].class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("int", int.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("integer", java.math.BigInteger.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("long", long.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("NOTATION", javax.xml.namespace.QName.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("QName", javax.xml.namespace.QName.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("short", short.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("string", java.lang.String.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("time", javax.xml.datatype.XMLGregorianCalendar.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("unsignedByte", short.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("unsignedInt", long.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("unsignedShort", int.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("anyURI", java.net.URI.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("IDREF", String.class);
		
		XSD_TYPE_JAVA_CLASS_MAP.put("IDREFS", String[].class);
		
	}
	
	/**
	 * Base XSD type.
	 */
	protected XSSimpleType baseXsdType = null;
	
	/**
	 * Qualified name of the type. It follows the convention: "localname" @ targetNamespaceURI eg.: "decemal@ http://www.w3.org/2001/XMLSchema". 
	 */
	protected SugtnDeclQName qName = null;
	
	/**
	 * "use" attribute. True if use="required". False if otherwise.
	 */
	protected boolean required = false;
	
	/**
	 * The variety of this simple type. It is either {@link XSVariety#ATOMIC atomic}, {@link XSVariety#UNION union} or {@link XSVariety#LIST list}.  
	 */
	protected XSVariety variety = null;
	/**
	 * List of enumerations.
	 */
	protected LinkedList<XSDEnumMember> enumeration = null;
	/**
	 * Value of xs:maxValue tag.
	 */
	protected double maxValue = Double.MIN_VALUE;
	/**
	 * Value of xs:minValue tag.
	 */
	protected double minValue = Double.MAX_VALUE;
	/**
	 * Value of xs:length tag.
	 */
	protected int length = -1;
	/**
	 * Value of xs:maxLength tag.
	 */
	protected int maxLength = -1;
	/**
	 * Value of xs:minLength tag.
	 */
	protected int minLength = -1;
	/**
	 * Value of xs:pattern tag.
	 */
	protected String pattern = null;
	/**
	 * Value of xs:totalDigits tag.
	 */
	protected int totalDigits = -1;
	/**
	 * Value of xs:fractionDigits tag.
	 */
	protected int fractionDigits = -1;

	// somehow this is not visible in subclass!! protected
	/**
	 * Constructs a new empty type declaration.
	 */
	public SugtnTypeDeclaration() {
		this.enumeration = new LinkedList<XSDEnumMember>();
		
	}
	
	/**
	 * Constructs a new type declaration with XSOM's simple type passed as argument.
	 * @param xsSimpleType
	 * 			XSOM's simple type
	 */
	public SugtnTypeDeclaration(XSSimpleType xsSimpleType) {
		
		String typeName = xsSimpleType.getName();
		
		this.enumeration = new LinkedList<XSDEnumMember>();
		
		if (typeName == null) { // Due to a bug in XSOM
			typeName = XSD_BASE_SIMPLE_TYPE_NAME;
		}
		
		this.qName = new SugtnDeclQName(xsSimpleType.getTargetNamespace(), typeName);
		
		this.variety = xsSimpleType.getVariety();
		
		XSSimpleType tempType = xsSimpleType;
		
		while (!XSD_TYPE_JAVA_CLASS_MAP.containsKey(tempType.getName())) {
			
			if (!XSD_BASE_SIMPLE_TYPE_NAME.equals(tempType.getBaseType().getName())) {
				
				tempType = tempType.getBaseType().asSimpleType();
				
				if (tempType == null) {
					throw new NullPointerException(
							"This will be thrown if proceeded.. reason is that base type is not Simple type so asSimpleType returns null");
				}
				
			} else {
				
				tempType = null;
				break;
				
			}
			
		}
		
		this.baseXsdType = tempType;
		
		XSRestrictionSimpleType restriction = xsSimpleType.asRestriction();
		
		if (restriction != null) {
			
			Iterator<? extends XSFacet> i = restriction.getDeclaredFacets().iterator();
			
			while (i.hasNext()) {
				
				XSFacet facet = i.next();
				
				if (facet.getName().equals(XSFacet.FACET_ENUMERATION)) {
					
					XSDEnumMember member = new XSDEnumMember();
					
					member.value = facet.getValue().value;
					member.documentation = SugtnUtil.documentationFromAnnotation(facet
							.getAnnotation());
					
					if ((member.documentation == null) || member.documentation.isEmpty()) {
						member.documentation = member.value;
					}
					
					this.enumeration.add(member);
// this.enumeration.add(facet.getValue().value);
					
				}
				
				if (facet.getName().equals(XSFacet.FACET_MAXINCLUSIVE)) {
					
					this.maxValue = Double.parseDouble(facet.getValue().value);
					
				}
				
				if (facet.getName().equals(XSFacet.FACET_MININCLUSIVE)) {
					
					this.minValue = Double.parseDouble(facet.getValue().value);
					
				}
				
				if (facet.getName().equals(XSFacet.FACET_MAXEXCLUSIVE)) {
					
					this.maxValue = Double.parseDouble(facet.getValue().value) - 1;
					
				}
				
				if (facet.getName().equals(XSFacet.FACET_MINEXCLUSIVE)) {
					
					this.minValue = Double.parseDouble(facet.getValue().value) + 1;
					
				}
				
				if (facet.getName().equals(XSFacet.FACET_LENGTH)) {
					
					this.length = Integer.parseInt(facet.getValue().value);
					
				}
				
				if (facet.getName().equals(XSFacet.FACET_MAXLENGTH)) {
					
					this.maxLength = Integer.parseInt(facet.getValue().value);
					
				}
				
				if (facet.getName().equals(XSFacet.FACET_MINLENGTH)) {
					
					this.minLength = Integer.parseInt(facet.getValue().value);
					
				}
				
				if (facet.getName().equals(XSFacet.FACET_PATTERN)) {
					
					this.pattern = facet.getValue().value;
					
				}
				
				if (facet.getName().equals(XSFacet.FACET_TOTALDIGITS)) {
					
					this.totalDigits = Integer.parseInt(facet.getValue().value);
					
				}
				
				if (facet.getName().equals(XSFacet.FACET_FRACTIONDIGITS)) {
					
					this.fractionDigits = Integer.parseInt(facet.getValue().value);
					
				}
				
			}
		}
	}
	
	/**
	 * Gets base type name.
	 * 
	 * @return Base type name.
	 */
	public String getBaseTypeName() {
		String result;
		if (this.baseXsdType != null) {
			result = this.baseXsdType.getName();
		} else {
			if (this.qName != null) {
				result = XSD_BASE_SIMPLE_TYPE_NAME;
			} else {
				result = XSD_BASE_TYPE_NAME;
			}
		}
		return result;
	}
	
	/**
	 * Gets list of enumerations.
	 * 
	 * @return List of enumerations.
	 */
	public LinkedList<XSDEnumMember> getEnumeration() {
		return this.enumeration;
	}
	
	/**
	 * Gets Value of xs:fractionDigits tag.
	 * 
	 * @return The value of xs:fractionDigits tag.
	 */
	public int getFractionDigits() {
		return this.fractionDigits;
	}
	
	/**
	 * Gets Java class mapped to base type name.
	 * 
	 * @return Java class mapped to base type name.
	 */
	public Class getJavaClassForBaseType() {
		
		Object result = XSD_TYPE_JAVA_CLASS_MAP.get(this.getBaseTypeName());
		if (result != null) {
			return (Class) result;
		} else {
			return null;
		}
	}
	
	/**
	 * Gets Value of xs:length tag.
	 * @return Value of xs:length tag.
	 */
	public int getLength() {
		return this.length;
	}
	
	/**
	 * Gets value of xs:maxLength tag.
	 * @return Value of xs:maxLength tag.
	 */
	public int getMaxLength() {
		return this.maxLength;
	}
	
	/**
	 * Gets value of xs:maxValue tag.
	 * @return Value of xs:maxValue tag.
	 */
	public double getMaxValue() {
		return this.maxValue;
	}
	
	/**
	 * Gets value of xs:minLength tag.
	 * @return Value of xs:minLength tag.
	 */
	public int getMinLength() {
		return this.minLength;
	}
	
	/**
	 * Gets value of xs:minValue tag.
	 * @return Value of xs:minValue tag.
	 */
	public double getMinValue() {
		return this.minValue;
	}
	
	/**
	 * Gets value of xs:pattern tag.
	 * @return Value of xs:pattern tag.
	 */
	public String getPattern() {
		return this.pattern;
	}
	
	/**
	 * Gets qualified name of the type.
	 * @return Qualified name of the type.
	 */
	public SugtnDeclQName getQName() {
		return this.qName;
	}
	
	/**
	 * Gets value of xs:totalDigits tag.
	 * @return Value of xs:totalDigits tag.
	 */
	public int getTotalDigits() {
		return this.totalDigits;
	}
	
	/**
	 * Test if Type has enumeration or not
	 * 
	 * @return True if size of this.enumeration > 0 
	 * 		   False if otherwise.
	 */
	public boolean isEnumerated() {
		
		return this.enumeration.size() > 0;
	}
	
	/**
	 * Test if  the variety of this simple type is {@link XSVariety#LIST list} or not.
	 *  
	 * @return True if variety of this type equals {@link XSVariety#LIST XSVariety.LIST}.
	 * 		   False if otherwise.
	 * 
	 * @see XSVariety#LIST
	 */
	public boolean isListType() {
// return (this.baseXsdType != null ? this.baseXsdType.isList() : false);
		return this.variety == XSVariety.LIST;
	}
	
	/**
	 * Test if required is set to true of false.
	 * 
	 * @return True if this.required equals true.
	 * 		   False if otherwise.
	 * 
	 */
	public boolean isRequired() {
		return this.required;
	}
	
	/**
	 * Test if  the variety of this simple type is {@link XSVariety#UNION union} or not.
	 *  
	 * @return True if variety of this type equals {@link XSVariety#UNION XSVariety.UNION}.
	 * 		   False if otherwise.
	 * 
	 * @see XSVariety#UNION
	 */
	public boolean isUnionType() {
// return (this.baseXsdType != null ? this.baseXsdType.isUnion() : false);
		return this.variety == XSVariety.UNION;
	}
	
	/**
	 * Sets required field.
	 * 
	 * @param required
	 *            required to set
	 */
	void setRequired(boolean required) {
		this.required = required;
	}
}
