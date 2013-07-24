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

import java.io.IOException;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.bibalex.sdxe.exception.DomBuildException;
import org.bibalex.sdxe.exception.SDXEException;
import org.bibalex.sdxe.xsa.application.XSAConstants;
import org.bibalex.sdxe.xsa.application.XSAUiActionListener;
import org.bibalex.sdxe.xsa.uigen.icefaces.IXSASugtnCompsDecoratorFactory;
import org.bibalex.sdxe.xsa.uigen.icefaces.XSAUiGenFactoryAbstract;
import org.bibalex.sdxe.xsa.uigen.icefaces.XSAUiGenFactoryDefault;
import org.jdom.JDOMException;

public class XSAAuthUiActionListener extends XSAUiActionListener {
	
	public XSAAuthUiActionListener(String cONFIGSCHEMAFILE,
			String cONFIGROOTELTNAME, String cONFIGNAMESPACE) {
		super(cONFIGSCHEMAFILE, cONFIGROOTELTNAME, cONFIGNAMESPACE);
		
	}
	
	public void close(ActionEvent ev) {
		this.uninitEditSession();
		try {
			FacesContext.getCurrentInstance().getExternalContext().redirect("index.jsp");
		} catch (IOException e) {
			this.handleException(e);
		}
	}
	
	@Override
	protected IXSASugtnCompsDecoratorFactory createCompsDecorator() {
		return new XSAAuthCompsDecoratorFactory();
	}
	
	@Override
	protected XSAUiGenFactoryAbstract createUiGenFactory() {
		return new XSAUiGenFactoryDefault(
				this.sdxeMediator,
				this.bindersBean,
				this.createCompsDecorator(),
				this.xsaDoc);
		
	}
	
	public void save(ActionEvent ev) {
		try {
			this.doSave();
			
			String saveMsg = (String) ev.getComponent().getAttributes().get(
					XSAConstants.PARAM_SAVE_MSG);
			
			this.showMessageWithDownloadLink(saveMsg, this.storage.getWorkingFile()
					.getCanonicalPath());
			
		} catch (DomBuildException e) {
			this.handleException(e);
		} catch (IOException e) {
			this.handleException(e);
		} catch (SDXEException e) {
			this.handleException(e);
		} catch (JDOMException e) {
			this.handleException(e);
		}
		
	}
	
}
