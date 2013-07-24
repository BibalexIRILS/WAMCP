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

package org.bibalex.icefaces.jdomtree;

// Based on code from
// package com.arsitech.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Iterator;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.bibalex.jdom.treemodel.IJDTNode;
import org.bibalex.jdom.treemodel.JDTAttributeNode;
import org.bibalex.jdom.treemodel.JDTElementNode;
import org.bibalex.jdom.treemodel.JDTTextNode;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;

/**
 * Class to display an XML file in a JTree. Uses the JDOM (Java Document Object
 * Model) to back the XML.
 * 
 * @see http://java.sun.com/webservices/jaxp/dist/1.1/docs/tutorial/index.html
 */
public class IDTBBean implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5333902257595944391L;
	
	/** Temporary hack to get around the lack of a text node */
	private Element lastElement;;
	
	// tree default model, used as a value for the tree component
	private DefaultTreeModel model = null;
	
	private Element activeElement;
	
	public void buildModel(Document xmlDocument) throws IDTException {
		Document document = xmlDocument;
		
		this.buildModel(document.getRootElement());
		
	}
	
	public void buildModel(Element rootElt) {
		
		this.activeElement = rootElt;
		
		// Create the initial IJDTNode from the Factory method
		IJDTNode jdomRoot = this.createNode(rootElt);
		
		// Create the root node of the JTree and build it from the JDOM Document
		DefaultMutableTreeNode treeRoot =
				new DefaultMutableTreeNode();
		
		IDTUserObject rootUserObject = new IDTUserObject(treeRoot);
		rootUserObject.setText("ROOT");
		rootUserObject.setExpanded(true);
		
		this.model = new DefaultTreeModel(treeRoot);
		
		this.buildModelRecursive(jdomRoot, treeRoot);
		
	}
	
	public void buildModel(InputStream xmlInputStream) throws IDTException {
		try {
			
			SAXBuilder saxBuilder = new SAXBuilder();
			
			saxBuilder.setFeature("http://xml.org/sax/features/validation", false);
			saxBuilder.setFeature("http://xml.org/sax/features/namespaces", true);
			saxBuilder.setFeature("http://xml.org/sax/features/namespace-prefixes",
					true);
// Unsupported: saxBuilder.setFeature("http://xml.org/sax/features/xmlns-uris", false);
			
			Document document = saxBuilder.build(xmlInputStream);
			
			this.buildModel(document.getRootElement());
			
		} catch (JDOMException e) {
			throw new IDTException(e);
		} catch (IOException e) {
			throw new IDTException(e);
		}
	}
	
	private void buildModelRecursive(IJDTNode jdomNode, DefaultMutableTreeNode treeNode) {
		// If this is a whitespace node or unhandled node, ignore it
		if ((jdomNode == null) || (jdomNode.toString().trim().equals(""))) {
			return;
		}
		
		DefaultMutableTreeNode newTreeNode = new DefaultMutableTreeNode();
		IDTUserObject newUserObject = new IDTUserObject(newTreeNode);
		newUserObject.setJdtNode(jdomNode);
		
		// Walk over the children of the node
		Iterator i = jdomNode.iterator();
		while (i.hasNext()) {
			// Create JDOMNodes on the children and add to the tree
			IJDTNode newNode = this.createNode(i.next());
			this.buildModelRecursive(newNode, newTreeNode);
		}
		
		// After all the children have been added, connect to the tree
		treeNode.add(newTreeNode);
		
	}
	
	private IJDTNode createNode(Object node) {
		
		if (node instanceof Element) {
			this.lastElement = (Element) node;
			return new JDTElementNode((Element) node);
		} else if (node instanceof Attribute) {
			return new JDTAttributeNode((Attribute) node);
		} else if (node instanceof Text) {
			return new JDTTextNode(((Text) node).getTextTrim()).setParent(this.lastElement);
		} else {
			// All other nodes are not implemented
			return null;
			
		}
		
	}
	
	public IDTUserObject findUserObjForDomObj(Object obj) {
		return this.findUserObjForDomObjRecursive(obj,
				(DefaultMutableTreeNode) ((DefaultMutableTreeNode) this.model
						.getRoot()).getFirstChild());
		
	}
	
	private IDTUserObject findUserObjForDomObjRecursive(Object obj, DefaultMutableTreeNode node) {
		IDTUserObject userObj = (IDTUserObject) node.getUserObject();
		
		if (userObj.getJdtNode().getNode() == obj) {
			return userObj;
		}
		
		DefaultMutableTreeNode treeNode = userObj.getWrapper();
		
		for (int i = 0; i < treeNode.getChildCount(); ++i) {
			IDTUserObject childRes = this.findUserObjForDomObjRecursive(obj,
					(DefaultMutableTreeNode) treeNode.getChildAt(i));
			if (childRes != null) {
				return childRes;
			}
		}
		
		return null;
	}
	
	public Element getActiveElement() {
		return this.activeElement;
	}
	
	public Document getDocument() {
		return this.activeElement.getDocument();
		
	}
	
	public TreeModel getModel() {
		return this.model;
	}
	
}
