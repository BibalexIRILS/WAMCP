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
public class DomMaxOccursException extends DomEnforceException {
	
	private int maxOccurs;
	protected String element;
	
	public DomMaxOccursException(String element, int maxOccurs, String context) {
		super(context + " can have only up to " + maxOccurs + " of " + element);
		
		this.context = context;
		this.element = element;
		this.maxOccurs = maxOccurs;
	}
	
	/**
	 * @return the maxOccurs
	 */
	public int getMaxOccurs() {
		return this.maxOccurs;
	}
	
}
