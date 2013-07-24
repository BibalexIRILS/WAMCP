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

package org.bibalex.sdxe.exception;

import org.bibalex.exception.Correctable;
import org.bibalex.sdxe.suggest.model.dom.SugtnElementNode;

@Correctable
public class DomMixedElementNotAllowedException extends DomBuildException {
	
	String eltName;
	
	SugtnElementNode sugtnParent;
	
	public DomMixedElementNotAllowedException() {
		
	}
	
	public DomMixedElementNotAllowedException(String errorMessage) {
		super(errorMessage);
		
	}
	
	public DomMixedElementNotAllowedException(String eltName, SugtnElementNode sugtnParent) {
		this.eltName = eltName;
		this.sugtnParent = sugtnParent;
		
	}
	
	public DomMixedElementNotAllowedException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		
	}
	
	public DomMixedElementNotAllowedException(Throwable arg0) {
		super(arg0);
		
	}
	
	/**
	 * @return the eltName
	 */
	public String getEltName() {
		return this.eltName;
	}
	
	/**
	 * @return the sugtnParent
	 */
	public SugtnElementNode getSugtnParent() {
		return this.sugtnParent;
	}
	
}
