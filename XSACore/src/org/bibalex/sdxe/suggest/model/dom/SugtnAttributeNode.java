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

import org.bibalex.sdxe.suggest.model.ISugtnTreeNodeVisitor;

import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSAttributeUse;

/**
 * Class represents attribute node in XML file
 * 
 */
@SuppressWarnings("serial")
public class SugtnAttributeNode extends SugtnDeclNode implements ISugtnTypedNode {
	
	/**
	 * Type declaration of the attribute
	 */
	private SugtnTypeDeclaration type = null;
	
//	private boolean isRequired = false;
	
	/**
	 * Constructor of {@link SugtnAttributeNode}.
	 * 
	 * It extracts attribute's information from attribute's declaration from schema.
	 * Attribute's information is:
	 * <ul>
	 * <li> Declaration qualified name of the attribute
	 * <li> Type of attribute's declaration
	 * <li> Attribute label. It is the concatenation of the following parts:
	 * <ul>
	 * <li>	@
	 * <li> local name of the declaration qualified name of the attribute
	 * <li> * if attribute is required
	 * <li>  - Default="defaultValue"  if attribute has a default value
	 * <li>  - Fixed to "fixedValue"   if attribute has a fixed value
	 * <li> : local name of qualified name of attribute's type
	 * <ul>
	 * </ul>
	 * 
	 * @param attribUse
	 * 			XSOM attribute
	 * 
	 * @see SugtnDeclQName
	 * @see SugtnTypeDeclaration
	 * 
	 */
	public SugtnAttributeNode(XSAttributeUse attribUse) {
		super(attribUse.getDecl());
		XSAttributeDecl decl = attribUse.getDecl();
		
		this.sugtnDeclQName.localName = decl.getName();
		this.sugtnDeclQName.targetNamespaceURI = decl.getTargetNamespace();
		
		this.type = new SugtnTypeDeclaration(decl.getType());
		this.type.setRequired(attribUse.isRequired());
		
//		this.isRequired = attribUse.isRequired();
		
		//Create label
		
		StringBuilder label = new StringBuilder("@" + this.sugtnDeclQName.localName);
		
		if (this.isRequired()) {
			label.append("*");
		}
		
		if (attribUse.getDefaultValue() != null) {
			this.defaultValue = attribUse.getDefaultValue().toString();
			
			label.append(" - Default=\"").append(this.defaultValue)
					.append("\"");
		}
		
		if (attribUse.getFixedValue() != null) {
			this.fixedValue = attribUse.getFixedValue().toString();
			
			label.append(" - Fixed to \"").append(this.fixedValue).append("\"");
		}
		
		label.append(": ").append(this.type.getQName().getLocalName());
		
		this.setUserObject(label.toString());
	}
	
	/**
	 * Gets Type declaration
	 *   
	 * @return Type declaration of the attribute
	 *
	 */
	public SugtnTypeDeclaration getType() {
		return this.type;
	}
	
	/**
	 * Invite method of the visitor design pattern
	 * 
	 *  @param visitor
	 *  		Tree node visitor interface
	 *  
	 */
	@Override
	public void invite(ISugtnTreeNodeVisitor visitor) {
		visitor.attributeNode(this);
		
	}
	
	/**
	 * Test if this attribute is required or not
	 * 
	 * @return true if attribute is required, false if otherwise.
	 */
	public boolean isRequired() {
//		return this.isRequired;
		return this.type.isRequired();
	}
	
}
