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


/**
 * Class represents a Mixed Parsed Character Data (Mixed PCDATA) tree node
 * 
 */

@SuppressWarnings("serial")
public class SugtnMixedNode extends SugtnPCDataNode {
	
	/**
	 * Construct a new empty mixed PCData node
	 * 
	 * It also sets UserObject to "Mixed PCData".
	 * 
	 */
	public SugtnMixedNode() {
		super();
		this.setUserObject("Mixed PCData");
	}
	
	
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
		visitor.mixedNode(this);
		
	}
	
}
