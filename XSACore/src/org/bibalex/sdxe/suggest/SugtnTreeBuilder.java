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

package org.bibalex.sdxe.suggest;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.bibalex.sdxe.controller.ISugtnTreeStylingListener;
import org.bibalex.sdxe.controller.SugtnTreeController;
import org.bibalex.sdxe.exception.SugtnException;
import org.bibalex.sdxe.suggest.model.SugtnTreeModel;
import org.bibalex.sdxe.suggest.model.SugtnTreeNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnAttributeNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnElementNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnMixedNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnPCDataNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnTypeDeclaration;
import org.xml.sax.SAXException;

import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.XSType;

/**
 * A tree builder works on an XSOM of a schema to transform it into a tree.
 * 
 * @author younos
 * 
 */

public class SugtnTreeBuilder {
	private static final Logger LOG = Logger.getLogger("org.bibalex.sdxe");
	
	
	/**
	 * Schema object model
	 */
	private XSSchemaSet schemas = null;
	
	
	/**
	 * The sugtnController which controls the tree model and all its nodes
	 */
	private SugtnTreeController sugtnController;
	
	/**
	 * Constructs a new tree builder using the schema set passed as an argument.
	 * 
	 * The builder works on a schema object model, so it needs to be passed in the constructor.
	 * 
	 * @param xss
	 * 			XSOM Schema set
	 * 
	 * @throws SugtnException
	 * @throws SAXException
	 * @throws IOException
	 * 
	 **/
	public SugtnTreeBuilder(XSSchemaSet xss) throws SugtnException, SAXException, IOException {
		this.schemas = xss;
		
	}
	
	/**
	 * XSOM's isMixed returns the value of mixed="true|false" of the complex type. 
	 * 
	 * However, an element is allowed to
	 * hold mixed content if its type, or any if its super types is mixed enabled. This method checks for this.
	 * 
	 * @param eltType
	 * 			Complex type of an element
	 * @return true if complex type node or one of its super types is mixed, false if otherwise.
	 * 
	 **/
	private boolean isMixedThorough(XSComplexType eltType) {
		boolean result = eltType.isMixed();
		XSType bType = eltType.getBaseType();
		if ((result == false) && (bType.isComplexType())
				&& (!bType.getName().equals(SugtnTypeDeclaration.XSD_BASE_TYPE_NAME))) {
			// do a recursive call, if the super type is complex, but not the "base type"
			return this.isMixedThorough(bType.asComplexType());
		} else {
			return result;
		}
	}
	
	/**
	 * It creates part of the tree model of an element.
	 * 
	 * Whenever an element is expanded, use this method to create the tree of its children. The modelListener is an
	 * interface of callbacks so that the view can react upon model changes. The min/maxOfModelGroup are the minOccurs
	 * and maxOccurs declared in the model group declaration, so that it would be used to determine the min/max occurs
	 * of the this element in this location. However, the rules for determining the min/max occurs are very complicated,
	 * and it is better to use XSA's required="true|false" attribute instead of minOccurs, and repeatable="true|false"
	 * attribute instead of maxOccurs.
	 * If node is of complex type, visit each node under it (attribute , PCData and particles nodes) and add nodes based on their types
	 * If node is of simple type, it is added directly to the tree (PCData node)
	 * 
	 * @param nameSpaceURI
	 *            Namespace of a schema level element
	 * @param parentEltName
	 *            Local name of a schema level element
	 * @param modelListener
	 *            Callbacks interface so that the view can act upon model changes
	 * @param maxOfModelGroup
	 *            Neglect and use XSA's repeatable
	 * @param minOfModelGroup
	 *            Neglect and use XSA's required
	 * @return The tree model of the children that can come under the parentElement in XML in this location
	 * @throws SugtnException
	 * 
	 */
	private SugtnTreeModel makeSuggestionTree(String nameSpaceURI, String parentEltName,
			ISugtnTreeStylingListener modelListener, int minOfModelGroup, int maxOfModelGroup)
			throws SugtnException {
		XSElementDecl parentElt;
		SugtnTreeModel result;
		SugtnTreeNode node;
		SugtnElementNode root;
		
		// use the qualified name to get the element declaration
		parentElt = this.schemas.getElementDecl(nameSpaceURI, parentEltName);
		if (parentElt == null) {
			// Maybe the name is wrong, or it is not a schema level element
			// TODO find a way (maybe using SCD) to get locally declared elements
			throw new SugtnException(
					"The element must be a direct child of Schema; i.e., global not local");
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("Sugtn TreeModel for element <" + parentEltName + "> will be built");
		}
		
		// The root of the expanded tree model of an element is the node for the element itself
		root = new SugtnElementNode(parentElt, minOfModelGroup, maxOfModelGroup,
				this.sugtnController);
		
		result = new SugtnTreeModel(root);
		result.addTreeModelListener(modelListener);
		
		// The element is either of complex type, or simple type that contains nothing but PCData
		if (parentElt.getType().isComplexType()) {
			
			XSComplexType eltType = parentElt.getType().asComplexType();
			
			// An element of complex type can contain attribute nodes
			for (XSAttributeUse attrib : eltType.getAttributeUses()) {
				
				node = new SugtnAttributeNode(attrib);
				
				result.insertNodeInto(node, root, root.getChildCount());
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("Element attributes added to Sugtn Tree");
			}
			
			// If a complex type is allowed to contain PCData it is said to be of mixed content.
			// Check for mixed
			if (this.isMixedThorough(eltType)) {
				node = new SugtnMixedNode();
				
				result.insertNodeInto(node, root, root.getChildCount());
				
				// The Mixed node is a PCData node, and since this node is frequently sought, we set a reference to it
				root.setPcDataNode((SugtnPCDataNode) node);
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("Element is Mixed enabled");
				}
			}
			
			XSParticle eltParticle = eltType.getContentType().asParticle();
			
			if (eltParticle != null) {
				SugtnBuildVisitor sugtnBuildVisitor = new SugtnBuildVisitor(
						parentElt, result,
						this.sugtnController); // , eltParticle.getMinOccurs(), eltParticle.getMaxOccurs());
				eltParticle.getTerm().visit(sugtnBuildVisitor);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Element child elements added to Sugtn Tree");
				}
			} else {
				
				XSSimpleType simpleContentType = eltType.getContentType().asSimpleType();
				
				if (simpleContentType != null) {
					
					node = new SugtnPCDataNode(simpleContentType);
					result.insertNodeInto(node, root, root.getChildCount());
					root.setPcDataNode((SugtnPCDataNode) node);
					
					if (LOG.isDebugEnabled()) {
						LOG
								.debug("Element is of simple content (no child elements, only attributes)");
					}
				} // else this must be a contentType of class Empty.. nothing to do!
			}
			
		} else {
			
			node = new SugtnPCDataNode(parentElt.getType().asSimpleType());
			result.insertNodeInto(node, root, root.getChildCount());
			root.setPcDataNode((SugtnPCDataNode) node);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Element is a simple element (no child elements or attributes)");
			}
		}
		
		if (modelListener != null) {
			modelListener.styleModelRoot(result);
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("Sugtn Tree created for the element: <" + parentEltName + ">");
		}
		
		return result;
	}
	
	/**
	 * This is the entry point of building the tree model of the schema.
	 * 
	 * It calls {@link #makeSuggestionTree(String, String, ISugtnTreeStylingListener, int, int) makeSuggestionTree()} 
	 * which creates part of the tree model of an element.
	 * 
	 * @param nameSpaceURI
	 *            Namespace of a schema level element
	 * @param elementName
	 *            Local name of a schema level element
	 * @param modelListener
	 *            Callbacks interface so that the view can act upon model changes
	 * @param maxOfModelGroup
	 *            Neglect and use XSA's repeatable
	 * @param minOfModelGroup
	 *            Neglect and use XSA's required
	 * @return The tree model of the children that can come under the parentElement in XML in this location
	 * 
	 * @throws SugtnException
	 * 
	 */
	public SugtnTreeModel modelForElement(String nameSpaceURI, String elementName,
			ISugtnTreeStylingListener modelListener, int minOfModelGroup, int maxOfModelGroup)
			throws SugtnException {
		
		SugtnTreeModel result = null;
		String stylerName = (modelListener != null ? modelListener.getClass().toString()
				: "NO_STYLER");
		String key = nameSpaceURI + "/" + elementName + "/"
				+ stylerName;
		
// While caching is desireable, the bottle necks it creates, because of synchronization, makes too much of it not
// desirable. We cache only what needs disk IO
// synchronized (this.sugtnTreeModelCache) {
// if (this.sugtnTreeModelCache.containsKey(key)) {
// result = this.sugtnTreeModelCache.get(key);
//
// if (LOG.isDebugEnabled()) {
// LOG.debug("Model for element retrieved from cache with key (" + key + ")");
// }
// } else {
// Make suggestiontree is thread safe because it never writes to instance variables
		result = this.makeSuggestionTree(nameSpaceURI, elementName, modelListener,
				minOfModelGroup, maxOfModelGroup);
		
// No need of this cache
// this.sugtnTreeModelCache.put(key, result);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("Model for element created  with key (" + key
					+ ")"); // No cache: "and put into cache"
		}
//
// }
// }
		
// When the tree model itself is cached, the result must be a clone of the suggestion tree model for the element that is
// stored in the cache as a prototype
// // Result up to now would be shared by users of the webapp
// // this is ok if we are guaranteed that the webapp will
// // only read it, which is impossible. So we return a clone
// return (SugtnTreeModel) result.clone();
		return result;
	}
	
	/**
	 * Sets tree controller
	 * 
	 * @param sugtnController
	 *            The sugtnController which controls the tree model and all its nodes
	 */
	public void setSugtnController(SugtnTreeController sugtnController) {
		this.sugtnController = sugtnController;
	}
}
