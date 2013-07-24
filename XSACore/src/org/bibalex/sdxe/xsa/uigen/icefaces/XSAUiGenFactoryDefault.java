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

import org.bibalex.sdxe.binding.SDXEMediatorBean;
import org.bibalex.sdxe.suggest.model.dom.SugtnDeclNode;
import org.bibalex.sdxe.xsa.exception.XSAException;
import org.bibalex.sdxe.xsa.model.XSADocument;
import org.bibalex.sdxe.xsa.model.XSAGroup;
import org.bibalex.sdxe.xsa.model.XSAInstance;
import org.bibalex.uigen.icefaces.IceCompsFactory;
import org.bibalex.uigen.icefaces.UIGenerationException;

/**
 * Adds nothing to the UIGenFactoryAbstract
 */
public class XSAUiGenFactoryDefault extends XSAUiGenFactoryAbstract {
	
	public XSAUiGenFactoryDefault(SDXEMediatorBean sdxeMediatorBean, XSAUiBindersBean bindersBean,
			IXSASugtnCompsDecoratorFactory compsDecoratorFactory, XSADocument xsaDoc) {
		super(sdxeMediatorBean, bindersBean, compsDecoratorFactory, xsaDoc);
		
	}
	
	@Override
	protected void decorateArea(XSAInstance xsaInst, String id, IceCompsFactory compsFactory)
			throws UIGenerationException, XSAException {
		// Nothing
		
	}
	
	@Override
	protected void decorateContainer(XSAInstance xsaInst, SugtnDeclNode declNode, int ixBetBros,
			String id, IceCompsFactory compsFactory) throws UIGenerationException, XSAException {
		// Nothing
		
	}
	
	@Override
	protected void decorateField(XSAInstance xsaInst, SugtnDeclNode declNode, String id,
			IceCompsFactory compsFactory) throws UIGenerationException, XSAException {
		// Nothing
		
	}
	
	@Override
	protected void decorateFieldOcc(XSAInstance xsaInst, SugtnDeclNode declNode, int ixBetBros,
			String id, IceCompsFactory compsFactory) throws UIGenerationException, XSAException {
		// Nothing
		
	}
	
	@Override
	protected void decorateGroup(XSAGroup subSecInst, String id, IceCompsFactory compsFactory)
			throws XSAException {
		// Nothing
		
	}
	
	@Override
	protected void decorateSection(XSAInstance xsaInst, String id, IceCompsFactory compsFactory)
			throws UIGenerationException, XSAException {
		// Nothing
		
	}
	
}
