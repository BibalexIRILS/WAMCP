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
public class DomChoiceException extends DomEnforceException {
	
	private String mutex1;
	private String mutex2;
	
	public DomChoiceException(String mutex1, String mutex2, String context) {
		super(context + " contains mutually execlusive children: " + mutex1
				+ " and " + mutex2);
		this.context = context;
		this.mutex1 = mutex1;
		this.mutex2 = mutex2;
	}
	
	/**
	 * @return the mutex1
	 */
	public String getMutex1() {
		return this.mutex1;
	}
	
	/**
	 * @return the mutex2
	 */
	public String getMutex2() {
		return this.mutex2;
	}
}
