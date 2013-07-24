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

import org.bibalex.exception.Correctable;
import org.bibalex.sdxe.xsa.exception.XSAEnforceException;
import org.bibalex.sdxe.xsa.exception.XSAException;
import org.bibalex.sdxe.xsa.model.XSAInstance;
import org.bibalex.util.XPathStrUtils;
import org.jdom.JDOMException;

/**
 * This exception is used when a required field is missing. 
 * 
 * @author Younos.Naga
 * 
 */
@Correctable
public class XSAMissingRequiredValExcpetion extends XSAEnforceException {
	
	public XSAMissingRequiredValExcpetion(XSAInstance inst, String ixesPath) throws XSAException,
			JDOMException {
		super(
				"Some required values are missing. The first missing value is: "
						+ inst.getLabel("en") + " at the containers: "
						+ XPathStrUtils.removeLastStep(ixesPath));
	}
	
}
