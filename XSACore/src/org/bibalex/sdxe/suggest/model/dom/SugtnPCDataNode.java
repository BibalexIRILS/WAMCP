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

/**
 * 
 */
package org.bibalex.sdxe.suggest.model.dom;

import org.bibalex.sdxe.suggest.model.ISugtnTreeNodeVisitor;

import com.sun.xml.xsom.XSSimpleType;

/**
 * Class represents a Parsed Character Data (PCDATA) tree node
 * 
 */
@SuppressWarnings("serial")
public class SugtnPCDataNode extends SugtnDomAddableNode implements ISugtnTypedNode {
	
	private SugtnTypeDeclaration type = null;

	/**
	 * Construct a new empty PCData node
	 * 
	 * It also sets UserObject to "PCdata".
	 * 
	 */
	public SugtnPCDataNode() {
		super();
		this.setUserObject("PCData");
	}
	
	/**
	 * Construct a new PCData node with the same declaration type as XSOM's simple type passed as arguments.
	 *  
	 * It also sets UserObject to the local  name of the qualified name of node's type.
	 * 
	 * @param xsSimpleType
	 * 			XSOM's simple type to set PCData node's declaration type to.
	 *  
	 */
	public SugtnPCDataNode(XSSimpleType xsSimpleType) {
		this();
		
		this.type = new SugtnTypeDeclaration(xsSimpleType);
		this.type.setRequired(true);
		
		this.setUserObject(this.type.getQName().getLocalName());
	}
	
	/**
	 * Gets type declaration of the node
	 * 
	 * @return Type declaration of the node
	 */
	public SugtnTypeDeclaration getType() {
		return this.type;
	}
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.bibalex.sdxe.suggest.SugtnTreeNode#invite( org.bibalex.sdxe.
	 * suggest.ISugtnTreeNodeVisitor)
	 */
	/**
	 * Invite method of the visitor design pattern.
	 * 
	 *  @param visitor
	 *  		Tree node visitor interface.
	 *  
	 */
	@Override
	public void invite(ISugtnTreeNodeVisitor visitor) {
		visitor.pcdataNode(this);
		
	}
	
}
