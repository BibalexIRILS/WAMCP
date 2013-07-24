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

@SuppressWarnings("serial")
public class DomEnforceException extends DomBuildException {
	
	protected String context;
	
	protected DomEnforceException(String errorMessage) {
		super(errorMessage);
	}
	
	public DomEnforceException(String errorMessage, String context) {
		super(errorMessage + " while enforcing element: (" + context + ")");
		this.context = context;
	}
	
	public DomEnforceException(String errorMessage, String context, Throwable orig) {
		super(errorMessage + " while enforcing element: (" + context + ")", orig);
		this.context = context;
	}
	
	/**
	 * @param arg0
	 */
	public DomEnforceException(Throwable arg0) {
		super(arg0);
	}
	
}
