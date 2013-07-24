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

package org.bibalex.sdxe.controller;

import javax.swing.event.TreeModelListener;

import org.bibalex.sdxe.suggest.model.SugtnTreeModel;
import org.bibalex.sdxe.suggest.model.SugtnTreeNode;

/**
 * Callbacks for events extensions for the Swing Tree Model (so that it can be registered as a listener for the
 * TreeModel)
 * Subclasses that uses certain view technologies must
 * provide such listener to stylize the tree as required
 * 
 * @author younos
 * 
 */
public interface ISugtnTreeStylingListener extends TreeModelListener {
	/**
	 * expand contract a domElt
	 * 
	 * @param domElt
	 * @param b
	 */
	public void setNodeExpanded(SugtnTreeNode node, boolean b);
	
	/**
	 * 
	 * @param model
	 */
	public void styleModelRoot(SugtnTreeModel model);
}
