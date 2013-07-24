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

import org.bibalex.sdxe.suggest.model.SugtnTreeNode;
import org.bibalex.sdxe.suggest.model.SugtnUtil;

import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSDeclaration;

/**
 * Class represents a superclass for {@link SugtnElementNode} and {@link SugtnAttributeNode}
 *
 */
@SuppressWarnings("serial")
public abstract class SugtnDeclNode extends SugtnDomAddableNode {
	/**
	 * Declaration qualified name of the node which is local name and namespace URI 
	 */
	protected SugtnDeclQName sugtnDeclQName = null;
	
	/**
	 * Default value of the node
	 */
	protected String defaultValue = null;
	
	/**
	 * Fixed value of the node
	 */
	protected String fixedValue = null;
	
	
	/**
	 * Annotation or comment of the node.
	 * XSOM considers annotation as W3C node which represents XML nodes from xsd.xml file. 
	 * We extract the data from W3C node and then add it to documentation.
	 * 
	 * @see SugtnUtil#documentationFromAnnotation(XSAnnotation)
	 */
	protected String documenation = null;
	
	/**
	 * Constructor of {@link SugtnDeclNode}.
	 * 
	 * It extracts class fields (qualified name and documentation) from node's schema declaration.
	 * 
	 * @param decl
	 * 			Schema declaration of the node.
	 * 
	 * @see SugtnUtil#documentationFromAnnotation(XSAnnotation)
	 */
	protected SugtnDeclNode(XSDeclaration decl) {
		super();
		this.sugtnDeclQName = new SugtnDeclQName();
		
		XSAnnotation annotation = decl.getAnnotation();
		
		this.documenation = (annotation != null ?
				SugtnUtil.documentationFromAnnotation(annotation)
				: "");
		
//		if (annotation != null) {
//			org.w3c.dom.Element annotElm = (org.w3c.dom.Element) annotation.getAnnotation();
//			org.w3c.dom.NodeList docList = annotElm.getElementsByTagName(ANNOTATION_TAG_NAME_DOC);
//			
//			StringBuilder docStr = new StringBuilder();
//			
//			for (int i = 0; i < docList.getLength(); ++i) {
//				org.w3c.dom.Element docElm = (org.w3c.dom.Element) docList.item(i);
//				docElm.normalize();
//				docStr.append(docElm.getTextContent().trim() + " ");
//			}
//			
//			StringTokenizer internalTrimmer = new StringTokenizer(docStr.toString(), " ", false);
//			docStr = new StringBuilder();
//			
//			while (internalTrimmer.hasMoreTokens()) {
//				docStr.append(internalTrimmer.nextToken())
//						.append(" ");
//			}
//			
//			this.documenation = docStr.toString();
//			
//		} else {
//			this.documenation = "";
//		}
		
	}
	
	/**
	 * Gets default value of the node
	 * 
	 * @return the defaultValue of the node
	 */
	public String getDefaultValue() {
		return this.defaultValue;
	}
	
	/**
	 * Gets documentation of the node
	 * 
	 * @return the documentation of the node
	 */
	public String getDocumenation() {
		return this.documenation;
	}
	
	/**
	 * Gets fixed value of the node
	 * 
	 * @return the fixedValue of the node
	 */
	public String getFixedValue() {
		return this.fixedValue;
	}
	
	/**
	 * Gets local name of the node
	 * 
	 * @return the localName of the node
	 */
	public String getLocalName() {
		return this.sugtnDeclQName.localName;
	}
	
	/**
	 * Gets qualified name of the node
	 * 
	 * @return the sugtnDeclQName of the node
	 */
	public SugtnDeclQName getqName() {
		return this.sugtnDeclQName;
	}
	
	/**
	 * Gets TargetNamespaceURI of the node
	 * 
	 * @return the targetNamespaceURI of the node
	 */
	public String getTargetNamespaceURI() {
		return this.sugtnDeclQName.targetNamespaceURI;
	}
	
	/**
	 * Compares node's QName to QName of the other object passed as an argument.
	 * 
	 *  It returns true if and only if both have the same value and argument is not null.
	 * 
	 * @param other
	 * 			Declaration node
	 * @return true if QName of the node equals QName of the other node, false if otherwise.
	 */
	public boolean isQNamesEqual(SugtnDeclNode other) {
		if (other == null) {
			return false;
		} else {
			return this.sugtnDeclQName.equals(other.getqName());
		}
	}
	
}
