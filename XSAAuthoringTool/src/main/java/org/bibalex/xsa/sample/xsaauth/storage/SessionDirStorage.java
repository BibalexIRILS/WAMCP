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

package org.bibalex.xsa.sample.xsaauth.storage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.bibalex.sdxe.xsa.storage.XSAIStorage;
import org.bibalex.util.FileUtils;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class SessionDirStorage implements XSAIStorage {
	protected File workingFile;
	protected File sessionDir;
	
	@Override
	public void closeWorkingFile() {
		this.workingFile = null;
	}
	
	@Override
	protected void finalize() throws Throwable {
		if ((this.sessionDir != null) && this.sessionDir.exists()) {
			this.sessionDir.delete();
		}
		super.finalize();
	}
	
	public File getSessionDir() {
		this.init();
		return this.sessionDir;
	}
	
	@Override
	public File getWorkingFile() {
		return this.workingFile;
	}
	
	protected void init() {
		if (this.sessionDir == null) {
			HttpSession session = (HttpSession) FacesContext.getCurrentInstance()
					.getExternalContext().getSession(false);
			
			ServletContext servletContext = (ServletContext) FacesContext
					.getCurrentInstance().getExternalContext().getContext();
			
			String dirPath = servletContext
					.getRealPath(
					session.getId());
			
			this.sessionDir = new File(dirPath);
			
			this.sessionDir.mkdir();
			
		}
	}
	
	public void newFromDocument(String newFilename, Document xmlDocument) throws IOException {
		this.init();
		this.workingFile = new File(this.sessionDir.getCanonicalPath() + File.separator
				+ newFilename);
		
		FileWriter fileWriter = new FileWriter(this.workingFile);
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat()
				.setEncoding("UTF-8"));
		outputter.output(xmlDocument, fileWriter);
	}
	
	public void newFromTemplate(String newFilename, String templateFilePathRel)
			throws IOException {
		
		this.init();
		this.workingFile = new File(this.sessionDir.getCanonicalPath() + File.separator
				+ newFilename);
		
		ServletContext servletContext = (ServletContext) FacesContext
				.getCurrentInstance().getExternalContext().getContext();
		
		String templateFilePath = servletContext
				.getRealPath(templateFilePathRel);
		
		File templateFile = new File(templateFilePath);
		
		FileUtils.copyFile(templateFile, this.workingFile);
	}
}
