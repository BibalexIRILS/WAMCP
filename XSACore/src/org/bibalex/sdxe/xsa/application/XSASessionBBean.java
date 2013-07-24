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

package org.bibalex.sdxe.xsa.application;

import java.io.Serializable;

/**
 * Session bean for the jump floating DIV. TODO: Should we move this into xsaBindersBean?
 * 
 * @author Younos.Naga
 * 
 */
public class XSASessionBBean implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5106111306353939446L;
	
	private String jumpContainerEltId;
	
	private String jumpLocatorString;
	
	public String getJumpContainerEltId() {
		return this.jumpContainerEltId;
	}
	
	public String getJumpLocatorString() {
		return this.jumpLocatorString;
	}
	
	public void setJumpContainerEltId(String jumpContainerEltId) {
		this.jumpContainerEltId = jumpContainerEltId;
	}
	
	public void setJumpLocatorString(String jumpLocatorString) {
		this.jumpLocatorString = jumpLocatorString;
	}
}
