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

import org.bibalex.sdxe.suggest.model.dom.SugtnPCDataNode;

public class DomMixedContentNotAllowedException extends DomBuildException {
	SugtnPCDataNode node;
	
	public DomMixedContentNotAllowedException() {
		// TOAuto-generated constructor stub
	}
	
	public DomMixedContentNotAllowedException(String errorMessage) {
		super(errorMessage);
		// TOD Auto-generated constructor stub
	}
	
	public DomMixedContentNotAllowedException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TOD Auto-generated constructor stub
	}
	
	public DomMixedContentNotAllowedException(SugtnPCDataNode node) {
		this.node = node;
	}
	
	public DomMixedContentNotAllowedException(Throwable arg0) {
		super(arg0);
		// TO sAuto-generated constructor stub
	}
	
	/**
	 * @return the domElt
	 */
	public SugtnPCDataNode getNode() {
		return this.node;
	}
	
}
