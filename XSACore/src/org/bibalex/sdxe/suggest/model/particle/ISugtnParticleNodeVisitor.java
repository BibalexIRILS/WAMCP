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

package org.bibalex.sdxe.suggest.model.particle;

import org.bibalex.sdxe.suggest.model.ISugtnTreeNodeVisitor;

/**
 * Interface that is used as visitor interface in visitor design pattern.
 * 
 * @author Younos.Naga
 * 
 */
public interface ISugtnParticleNodeVisitor extends
		ISugtnTreeNodeVisitor {
	
	/**
	 * Visit method for all particle node.
	 *  
	 * @param node all particle node.
	 * 
	 */
	public void allNode(SugtnAllNode node);
	
	/**
	 * Visit method for choice particle node.
	 *  
	 * @param node choice particle node.
	 * 
	 */
	public void choiceNode(SugtnChoiceNode node);
	
	/**
	 * Visit method for sequence particle node.
	 *  
	 * @param node sequence particle node.
	 * 
	 */
	public void sequenceNode(SugtnSequenceNode node);
	
}
