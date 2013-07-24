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

package org.bibalex.sdxe.xsa.uigen.icefaces;

import java.util.Vector;

import javax.faces.component.UICommand;
import javax.faces.component.UIOutput;
import javax.faces.component.UIPanel;

import org.bibalex.sdxe.suggest.model.dom.ISugtnTypedNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnElementNode;
import org.bibalex.sdxe.xsa.exception.XSAException;
import org.bibalex.sdxe.xsa.model.XSAGroup;
import org.bibalex.sdxe.xsa.model.XSAInstance;
import org.bibalex.uigen.icefaces.UIGenerationException;

import com.icesoft.faces.component.paneltabset.PanelTabSet;

/**
 * Interface that should be used to separate the UI generated from the UI stack technology. 
 * 
 */
//this needs a little refactoring to remove dependency on Icefaces and JSF altogether
public interface IXSAUiGenFactory {
	
	/**
	 * An area is represented by a link to go into it
	 */
	public abstract UICommand createArea(UIPanel container,
			XSAInstance areaInst)
			throws UIGenerationException, XSAException;
	
	/**
	 * Creates a container and adds it to its parent if it has one. 
	 * @param container the parent container
	 * @param eltNode the node represents the container
	 * @param xsaInstm 
	 * @param ixInUI the index of the container in the UI
	 * @return
	 * @throws XSAException
	 * @throws UIGenerationException
	 */
	public abstract UIPanel createContainer(UIPanel container, SugtnElementNode eltNode,
			XSAInstance xsaInstm, int ixInUI)
			throws XSAException, UIGenerationException;
	
	/**
	 * Create a set of all field occurrences
	 * @param container the container which contains the field
	 * @param sugtnTyped 
	 * @param xsaInst
	 */
	public abstract Vector<UIOutput> createField(UIPanel container, ISugtnTypedNode sugtnTyped,
			XSAInstance xsaInst)
			throws XSAException;
	
	/**
	 * Creates a section
	 * @param container the section's container
	 * @param secInst the instance corresponding to this section
	 * @return
	 * @throws XSAException
	 * @throws UIGenerationException
	 */
	public abstract UIPanel createSection(PanelTabSet container,
				XSAInstance secInst) throws XSAException, UIGenerationException;
	
	public abstract UIPanel createSubSec(UIPanel container, XSAGroup subSecInst)
			throws XSAException;
	
}
