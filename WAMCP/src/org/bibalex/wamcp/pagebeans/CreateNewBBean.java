//   Copyright 2013 Bibliotheca Alexandrina, Wellcome Trust Library, Wellcome Trust Library
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

package org.bibalex.wamcp.pagebeans;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.EventObject;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.bibalex.gallery.model.BAGAlbum;
import org.bibalex.icefaces.jdomtree.IDTBBean;
import org.bibalex.icefaces.jdomtree.IDTException;
import org.bibalex.wamcp.storage.WAMCPGalleryBeanSVNStoredMetaGallery;
import org.bibalex.wamcp.storage.WAMCPStorage;

import com.icesoft.faces.component.inputfile.FileInfo;
import com.icesoft.faces.component.inputfile.InputFile;

public class CreateNewBBean extends WAMCPRequestBBean implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1731697073131258396L;
	private String shelfmark = null;
	private BAGAlbum album = null;
	private WAMCPGalleryBeanSVNStoredMetaGallery galleryBean = null;
	private String coverName = null;
	private String albumCaption = null;
	private IDTBBean importXMLTreeBBean = null;
	private ImportBBean importBBean = null;
	private WAMCPStorage storage = null;
	
	private FileInfo currentFile;
	private int fileProgress;
	
	public final String CONFIG_BLANK_XML;// = "/XML/msDescBlank.xml";
	
	public CreateNewBBean(String cONFIGBLANKXML) {
		super();
		this.CONFIG_BLANK_XML = cONFIGBLANKXML;
	}
	
	public void albumDetailsDone(ActionEvent ev) {
		
		this.shelfmark = this.album.getName().replace('_', ' ');
		
		try {
			FacesContext.getCurrentInstance().getExternalContext().redirect(
					"New_XMLSource.iface");
		} catch (IOException e) {
			this.handleException(e);
		}
	}
	
	public void chooseAlbumCover(ActionEvent ev) {
		
		try {
			this.coverName = (String) ev.getComponent().getAttributes().get(
					"PARAM_SELECTALBUM_NAME");
			
		} catch (Exception e) {
			this.handleException(e);
		}
		
	}
	
	public BAGAlbum getAlbum() {
		return this.album;
	}
	
	public String getAlbumCaption() {
		return this.albumCaption;
	}
	
	public String getCoverName() {
		return this.coverName;
	}
	
	public FileInfo getCurrentFile() {
		return this.currentFile;
	}
	
	public int getFileProgress() {
		return this.fileProgress;
	}
	
	public WAMCPGalleryBeanSVNStoredMetaGallery getGalleryBean() {
		return this.galleryBean;
	}
	
	public ImportBBean getImportBBean() {
		return this.importBBean;
	}
	
	public IDTBBean getImportXMLTreeBBean() {
		return this.importXMLTreeBBean;
	}
	
	/**
	 * @return the shelfmark
	 */
	public String getShelfMark() {
		return this.shelfmark;
	}
	
	public WAMCPStorage getStorage() {
		return this.storage;
	}
	
	public void newBlank(ActionEvent ev) {
		try {
			this.storage.createFromTemplate(this.CONFIG_BLANK_XML, this.shelfmark, this.album
					.getName(),
					this.albumCaption, this.coverName);
			
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
	
	public void selectAlbum(ActionEvent ev) {
		
		try {
			String albumName = (String) ev.getComponent().getAttributes().get(
					"PARAM_SELECTALBUM_NAME");
			
			this.album = this.galleryBean.openAlbum(albumName);
			
			this.coverName = this.album.getThumbnails().get(0).getName();
			
			FacesContext.getCurrentInstance().getExternalContext().redirect(
					"New_AlbumDetails.iface");
		} catch (Exception e) {
			this.handleException(e);
		}
		
	}
	
	public void setAlbumCaption(String albumCaption) {
		this.albumCaption = albumCaption;
	}
	
	public void setGalleryBean(WAMCPGalleryBeanSVNStoredMetaGallery galleryBean) {
		this.galleryBean = galleryBean;
	}
	
	public void setImportBBean(ImportBBean importBBean) {
		this.importBBean = importBBean;
	}
	
	public void setImportXMLTreeBBean(IDTBBean importXMLTreeBBean) {
		this.importXMLTreeBBean = importXMLTreeBBean;
	}
	
	/**
	 * @param shelfmark
	 *            the shelfmark to set
	 */
	public void setShelfMark(String shelfMark) {
		this.shelfmark = shelfMark;
	}
	
	public void setStorage(WAMCPStorage storage) {
		this.storage = storage;
	}
	
	public void uploadActionListener(ActionEvent actionEvent) {
		InputFile inputFile = (InputFile) actionEvent.getSource();
		this.currentFile = inputFile.getFileInfo();
		// file has been saved
		if (this.currentFile.isSaved()) {
			try {
				FileInputStream importInputStrem = new FileInputStream(this.currentFile.getFile());
				
				this.importXMLTreeBBean.buildModel(importInputStrem);
				
				this.importBBean.setAlbumCaption(this.albumCaption);
				this.importBBean.setAlbumCoverName(this.coverName);
				this.importBBean.setAlbumName(this.album.getName());
				this.importBBean.setShelfmark(this.shelfmark);
				
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
