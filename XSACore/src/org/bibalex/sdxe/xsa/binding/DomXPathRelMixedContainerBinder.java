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

package org.bibalex.sdxe.xsa.binding;

import org.bibalex.sdxe.controller.DomTreeController;
import org.bibalex.sdxe.exception.DomBuildException;
import org.bibalex.sdxe.suggest.model.dom.SugtnElementNode;

/**
 * For binding containers that can contain mixed content
 * 
 * @author Younos.Naga
 * 
 */
public class DomXPathRelMixedContainerBinder extends DomXPathRelToActiveBinder {
	
	public DomXPathRelMixedContainerBinder(String xPathStr, DomTreeController domTarget,
			SugtnElementNode sugtnFor, String xsaLocatorStr) {
		
		super(xPathStr, domTarget, sugtnFor, xsaLocatorStr);
		
	}
	
	@Override
	protected void doValueFromDom() {
		
		super.doValueFromDom();
		int gtIx = this.value.indexOf('<');
		if (gtIx < 0) {
			gtIx = this.value.length();
		}
		this.value = this.value.substring(0, gtIx);
	}
	
	@Override
	protected void doValueToDom() throws DomBuildException {
		// Why am I doing this replace all?
		String tempValue = this.value.replaceAll("<", "&lt;");
		
		super.doValueFromDom();
		
		int gtIx = this.value.indexOf('<');
		if (gtIx < 0) {
			gtIx = this.value.length();
		}
		
		this.value = tempValue + this.value.substring(gtIx);
		
		super.doValueToDom();
		
		this.doValueFromDom();
	}
}
