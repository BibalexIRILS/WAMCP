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

package org.bibalex.sdxe.xsa.model;

import org.bibalex.sdxe.suggest.model.dom.SugtnDeclQName;

/**
 * 
 * This class binds an element declaration from a certain location in the XSD to some use of it. Uses were indexed, so
 * the first use of locator is A/B/C[1] while the second is A/B/C[2]. This was all deprecated and ids are used instead.
 * 
 * @deprecated
 * @author Younos.Naga
 * 
 */
@Deprecated
public class XSALocator {
	public static final SugtnDeclQName XSA_SUGTN_LOCATOR_TYPE_QNAME = new SugtnDeclQName(
			XSADocument.XSA_NAMESPACE_URI, "TLocatorString");
	
	public static final String LOCAL_NAME = "locator";
	
	private String locator;
	
	public XSALocator(String locator) {
		this.locator = locator;
	}
	
	public String asString() {
		return this.locator;
	}
	
	@Override
	public String toString() {
		
		return this.asString();
	}
}
