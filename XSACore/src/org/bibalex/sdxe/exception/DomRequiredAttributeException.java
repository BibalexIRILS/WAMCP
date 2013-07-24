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
package org.bibalex.sdxe.exception;

import org.bibalex.exception.Correctable;

/**
 * @author younos
 * 
 */
@SuppressWarnings("serial")
@Correctable
public class DomRequiredAttributeException extends DomEnforceException {
	
	private String elementtName;
	private String attributeName;
	
	/**
	 * @param errorMessage
	 * @param elementtName
	 * @param attributeName
	 */
	public DomRequiredAttributeException(String elementtName,
			String attributeName, String context) {
		super("Element <"
				+ elementtName + "> must have the attibute @"
				+ attributeName);
		this.context = context;
		this.elementtName = elementtName;
		this.attributeName = attributeName;
	}
	
	/**
	 * @return the attributeName
	 */
	public String getAttributeName() {
		return this.attributeName;
	}
	
	/**
	 * @return the elementtName
	 */
	public String getElementtName() {
		return this.elementtName;
	}
	
}
