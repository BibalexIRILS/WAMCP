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

package org.bibalex.sdxe.suggest.model;

import org.bibalex.sdxe.suggest.model.dom.SugtnAttributeNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnElementNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnMixedNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnPCDataNode;

/**
 * 
 * Use this to traverse the schema tree
 * @author Younos.Naga
 * 
 * Interface that is used as visitor interface in visitor design pattern
 * 
 */
public interface ISugtnTreeNodeVisitor {
	/**
	 * Visit method for attribute node data model 
	 * @param node 
	 * 			attribute node
	 */
	public void attributeNode(SugtnAttributeNode node);
	
	/**
	 * Visit method for element node data model 
	 * @param node 
	 * 			element node
	 */
	public void elementNode(SugtnElementNode node);
	
	/**
	 * Visit method for mixed PCData node data model 
	 * @param node 
	 * 			mixed PCData node
	 */
	public void mixedNode(SugtnMixedNode node);
	
	/**
	 * Visit method for PCData node data model 
	 * @param node 
	 * 			PCData node
	 */
	public void pcdataNode(SugtnPCDataNode node);
}
