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
package org.bibalex.sdxe.binding;

import org.bibalex.sdxe.controller.DomTreeController;
import org.bibalex.sdxe.exception.DomBuildException;
import org.bibalex.sdxe.suggest.model.dom.SugtnPCDataNode;

/**
 * Bean that gets and sets the text of the active dom node.
 * 
 * @author younos
 * @deprecated
 */
@Deprecated
public class DomActiveTextBinder extends DomValueBinder {
// private static final Logger LOG = Logger.getLogger("org.bibalex.xsa");
	
	/**
	 * @param domTarget
	 * @param sugtnNodeForTarget
	 */
	public DomActiveTextBinder(DomTreeController domTarget, SugtnPCDataNode sugtnFor) {
		super(domTarget, sugtnFor);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.sdxe.controller.DomValueBinder#valueFromDom()
	 */
	@Override
	protected void doValueFromDom() {
		this.value = this.domTarget.getTextOfActive();
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.sdxe.controller.DomValueBinder#valueToDom()
	 */
	@Override
	protected void doValueToDom() throws DomBuildException {
		this.domTarget.setTextOfActive((SugtnPCDataNode) this.sugtnNodeForTarget, this.value);
// } catch (DomBuildException e) {
// LOG.error(e, e);
// }
		
	}
	
}
