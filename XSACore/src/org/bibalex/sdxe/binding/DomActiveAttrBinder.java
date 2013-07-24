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

import org.apache.log4j.Logger;
import org.bibalex.sdxe.controller.DomTreeController;
import org.bibalex.sdxe.exception.DomBuildException;
import org.bibalex.sdxe.suggest.model.dom.SugtnAttributeNode;

/**
 * Bean that gets and set an attribute value for the currently active DOM element
 * @author younos
 * @deprecated
 */
@Deprecated
public class DomActiveAttrBinder extends DomValueBinder {
	private static final Logger LOG = Logger.getLogger("org.bibalex.sdxe");
	
	/**
	 * @param domTarget
	 * @param sugtnNodeForTarget
	 */
	public DomActiveAttrBinder(DomTreeController domTarget, SugtnAttributeNode sugtnFor) {
		super(domTarget, sugtnFor);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.sdxe.controller.DomValueBinder#valueFromDom()
	 */
	@Override
	protected void doValueFromDom() {
		try {
			
			this.value = this.domTarget
					.getAttributeForActive((SugtnAttributeNode) this.sugtnNodeForTarget);
			
		} catch (DomBuildException e) {
			
			this.value = "EXCEPTION! Check log!";
			
			LOG.error(e, e);
		}
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.sdxe.controller.DomValueBinder#valueToDom()
	 */
	@Override
	protected void doValueToDom() throws DomBuildException {
		
		this.domTarget.setAttributeForActive((SugtnAttributeNode) this.sugtnNodeForTarget,
				this.value);
		
// } catch (DomBuildException e) {
// LOG.error(e, e);
// }
		
	}
	
}
