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

package org.bibalex.xsa.sample.xsaauth.xsa;

import org.bibalex.sdxe.exception.DomBuildException;
import org.bibalex.sdxe.xsa.uigen.icefaces.XSAUiGenDomListener;
import org.bibalex.sdxe.xsa.uigen.icefaces.XSAUiGenFactoryAbstract;
import org.bibalex.sdxe.xsa.uigen.icefaces.XSAUiGenFactoryDefault;
import org.jdom.Document;

public class XSAAuthUiGenDomListener extends XSAUiGenDomListener {
	
	public XSAAuthUiGenDomListener(String schemaFilePath) {
		super(schemaFilePath);
		
	}
	
	@Override
	protected XSAUiGenFactoryAbstract createUiGenFactory() {
		
		return new XSAUiGenFactoryDefault(
				this.sdxeMediator,
				this.bindersBean,
				new XSAAuthCompsDecoratorFactory(),
				this.xsaDoc);
	}
	
	@Override
	public void notifyBeforeSave(Document domTotalClone) throws DomBuildException {
		// TODONOTHING Auto-generated method stub
		
	}
	
}
