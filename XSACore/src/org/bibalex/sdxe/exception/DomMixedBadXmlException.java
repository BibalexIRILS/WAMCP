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
import org.jdom.JDOMException;

public class DomMixedBadXmlException extends DomBuildException {
	SugtnPCDataNode node;
	String nextPCData;
	JDOMException originalException;
	
	public DomMixedBadXmlException() {
		//  Auto-generated constructor stub
	}
	
	public DomMixedBadXmlException(String errorMessage) {
		super(errorMessage);
		//  Auto-generated constructor stub
	}
	
	public DomMixedBadXmlException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		//  Auto-generated constructor stub
	}
	
	/**
	 * @param domElt
	 * @param nextPCData
	 * @param originalException
	 */
	public DomMixedBadXmlException(SugtnPCDataNode node, String nextPCData,
			JDOMException originalException) {
		super(originalException);
		this.node = node;
		this.nextPCData = nextPCData;
		this.originalException = originalException;
	}
	
	public DomMixedBadXmlException(Throwable arg0) {
		super(arg0);
		//  Auto-generated constructor stub
	}
	
	/**
	 * @return the nextPCData
	 */
	public String getNextPCData() {
		return this.nextPCData;
	}
	
	/**
	 * @return the domElt
	 */
	public SugtnPCDataNode getNode() {
		return this.node;
	}
	
	/**
	 * @return the originalException
	 */
	public JDOMException getOriginalException() {
		return this.originalException;
	}
	
}
