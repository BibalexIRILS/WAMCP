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

@SuppressWarnings("serial")
@Correctable
public class DomMinOccursException extends DomEnforceException {
	
	private int minOccurs;
	
	private String missing;
	
	public DomMinOccursException(String missing, int minOccurs, String context) {
		super(context + " must have at least " + minOccurs + " of " + missing);
		
		this.context = context;
		this.missing = missing;
		this.minOccurs = minOccurs;
	}
	
	/**
	 * @return the minOccurs
	 */
	public int getMinOccurs() {
		return this.minOccurs;
	}
	
	/**
	 * @return the missing
	 */
	public String getMissing() {
		return this.missing;
	}
}
