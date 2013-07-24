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

package org.bibalex.xsa.sample.xsaauth.pagebeans;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.EventObject;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.bibalex.icefaces.jdomtree.IDTBBean;
import org.bibalex.icefaces.jdomtree.IDTException;
import org.bibalex.sdxe.xsa.application.XSARequestBackingBean;
import org.bibalex.sdxe.xsa.application.XSAUiActionListener;
import org.bibalex.xsa.sample.xsaauth.storage.SessionDirStorage;

import com.icesoft.faces.component.inputfile.FileInfo;
import com.icesoft.faces.component.inputfile.InputFile;

public class CreateOpenBBean extends XSARequestBackingBean {
	private String filename = "xsa.xsd.xsa.xml";
	private IDTBBean importXMLTreeBBean = null;
	private ImportBBean importBBean = null;
	
	private FileInfo currentFile;
	private int fileProgress;
	
	public final String CONFIG_BLANK_XML;
	
	private SessionDirStorage storage;
	
	private XSAUiActionListener xsaUiActionListener;
	
	public CreateOpenBBean(String cONFIGBLANKXML) {
		super();
		this.CONFIG_BLANK_XML = cONFIGBLANKXML;
	}
	
	public FileInfo getCurrentFile() {
		return this.currentFile;
	}
	
	/**
	 * @return the filename
	 */
	public String getFilename() {
		return this.filename;
	}
	
	public int getFileProgress() {
		return this.fileProgress;
	}
	
	public ImportBBean getImportBBean() {
		return this.importBBean;
	}
	
	public IDTBBean getImportXMLTreeBBean() {
		return this.importXMLTreeBBean;
	}
	
	public SessionDirStorage getStorage() {
		return this.storage;
	}
	
	public XSAUiActionListener getXsaUiActionListener() {
		return this.xsaUiActionListener;
	}
	
	public void newBlank(ActionEvent ev) {
		try {
			this.storage.newFromTemplate(this.filename, this.CONFIG_BLANK_XML);
			
			this.xsaUiActionListener.initEditSession();
			
			FacesContext.getCurrentInstance().getExternalContext().redirect(
					"EditForm.iface");
		} catch (Exception e) {
			this.handleException(e);
		}
	}
	
	/**
	 * <p>
	 * This method is bound to the inputFile component and is executed multiple times during the file upload process.
	 * Every call allows the user to finds out what percentage of the file has been uploaded. This progress information
	 * can then be used with a progressBar component for user feedback on the file upload progress.
	 * </p>
	 * 
	 * @param event
	 *            holds a InputFile object in its source which can be probed
	 *            for the file upload percentage complete.
	 */
	
	public void progressListener(EventObject event) {
		InputFile ifile = (InputFile) event.getSource();
		this.fileProgress = ifile.getFileInfo().getPercent();
	}
	
	/**
	 * @param filename
	 *            the filename to set
	 */
	public void setFilename(String shelfMark) {
		this.filename = shelfMark;
	}
	
	public void setImportBBean(ImportBBean importBBean) {
		this.importBBean = importBBean;
	}
	
	public void setImportXMLTreeBBean(IDTBBean importXMLTreeBBean) {
		this.importXMLTreeBBean = importXMLTreeBBean;
	}
	
	public void setStorage(SessionDirStorage storage) {
		this.storage = storage;
	}
	
	public void setXsaUiActionListener(XSAUiActionListener xsaUiActionListener) {
		this.xsaUiActionListener = xsaUiActionListener;
	}
	
	public void uploadActionListener(ActionEvent actionEvent) {
		InputFile inputFile = (InputFile) actionEvent.getSource();
		this.currentFile = inputFile.getFileInfo();
		// file has been saved
		if (this.currentFile.isSaved()) {
			try {
				FileInputStream importInputStrem = new FileInputStream(this.currentFile.getFile());
				
				this.importXMLTreeBBean.buildModel(importInputStrem);
				
				this.importBBean.setFilename(this.currentFile.getFileName());
				
				this.importBBean.init(this.importXMLTreeBBean);
				
				FacesContext.getCurrentInstance().getExternalContext().redirect(
						"Import.iface");
			} catch (IOException e) {
				this.handleException(e);
			} catch (IDTException e) {
				this.handleException(e);
			}
		}
		
		// TODO: The default error message already appears and is better than these,should we remove these?
		// upload failed, generate custom messages
		if (this.currentFile.isFailed()) {
			if (this.currentFile.getStatus() == FileInfo.INVALID) {
				this.setHeaderMsg("Invlid file, cannot upload!", StyledMsgTypeEnum.ERROR);
			}
			if (this.currentFile.getStatus() == FileInfo.SIZE_LIMIT_EXCEEDED) {
				this.setHeaderMsg("The file size exceeds the upload size limit",
						StyledMsgTypeEnum.INSTRUCTIONS);
			}
			if (this.currentFile.getStatus() == FileInfo.INVALID_CONTENT_TYPE) {
				this.setHeaderMsg("Invlid content type", StyledMsgTypeEnum.ERROR);
			}
			if (this.currentFile.getStatus() == FileInfo.INVALID_NAME_PATTERN) {
				this.setHeaderMsg("Please select a .xml file", StyledMsgTypeEnum.INSTRUCTIONS);
			}
		}
	}
	
}
