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

package org.bibalex.wamcp.storage;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.bibalex.gallery.exception.BAGException;
import org.bibalex.gallery.icefaces.BAGGalleryBean;
import org.bibalex.gallery.model.BAGAlbum;
import org.bibalex.gallery.model.BAGGalleryAbstract;
import org.bibalex.gallery.model.BAGGalleryFactory;
import org.bibalex.gallery.model.BAGThumbnail;
import org.bibalex.wamcp.application.WAMCPSessionBackingBean;
import org.bibalex.workflow.storage.WFISVNClient;
import org.bibalex.workflow.storage.WFSVNException;
import org.bibalex.workflow.storage.WFSVNLockedException;
import org.jdom.Element;
import org.tmatesoft.svn.core.SVNException;

public class WAMCPGalleryBeanSVNStoredMetaGallery extends BAGGalleryBean implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7493399784364550359L;
	private WFISVNClient metaStroage;
	private WAMCPSessionBackingBean sessionBBean;
	
// public WAMCPGalleryBeanSVNStoredMetaGallery(String djatokaServerUrlStr,
// String cONFIGURLGALLERYROOT, String cONFIGMETAGALLERYROOT, String cONFIGPATHCACHEDIR) {
// super(djatokaServerUrlStr, cONFIGURLGALLERYROOT, cONFIGMETAGALLERYROOT, cONFIGPATHCACHEDIR);
// // TODONOT Auto-generated constructor stub
// }
	
	public WAMCPGalleryBeanSVNStoredMetaGallery(String djatokaServerUrlStr,
			String cONFIGURLGALLERYROOT, String cONFIGMETAGALLERYROOT,
			String cONFIGPATHCACHEDIR, /* int galleryHttpAccPort, */WFISVNClient metaStroage,
			WAMCPSessionBackingBean sessionBBean) {
		super(djatokaServerUrlStr, cONFIGURLGALLERYROOT, cONFIGMETAGALLERYROOT, cONFIGPATHCACHEDIR);
		// galleryHttpAccPort);
		
		this.metaStroage = metaStroage;
		this.sessionBBean = sessionBBean;
	}
	
	public void addAlbumToMetaGallery(String albumName, String albumCaption, String coverName,
			String metaFileName) throws BAGException {
		this.assureInit();
		
		this.updateMetaGallery();
		
		this.gallery.addAlbumToMetaGallery(albumName, albumCaption, coverName, metaFileName);
		
		this.commitMetaGallery(
				"Added album " + albumName);
	}
	
	@Override
	protected void assureInit() throws BAGException {
		if (this.gallery == null) {
			this.init();
		}
	}
	
	protected void commitMetaGallery(String commitMessage) throws BAGException {
		while (true) {
			try {
				this.metaStroage.commitFileFromUserDir(
						BAGGalleryAbstract.FILENAME_GALLERY_METADATA_XML,
						commitMessage);
				break;
			} catch (WFSVNLockedException e) {
				// will try again
				try {
					this.wait(5000);
				} catch (InterruptedException e1) {
					// die silently
				}
			} catch (WFSVNException e) {
				throw new BAGException(e);
			} catch (SVNException e) {
				throw new BAGException(e);
			} catch (IOException e) {
				throw new BAGException(e);
			}
		}
	}
	
	public void deleteAlbumForMetadataFile(String fileName) throws BAGException {
		this.assureInit();
		this.updateMetaGallery();
		String albumName = this.gallery.deleteAlbumForMetadataFile(fileName);
		this.commitMetaGallery("Deleted album from Meta Gallery: " + albumName);
	}
	
	@Override
	public List<BAGThumbnail> getAlbumCovers() throws BAGException {
		this.assureInit();
		this.updateMetaGallery();
		return this.gallery.getAlbumCovers();
		
	}
	
	@Override
	public List<String> getAllFolders() throws BAGException {
		this.assureInit();
		this.updateMetaGallery();
		return this.gallery.getAllFolders();
		
	}
	
	@Override
	public Element getMetaAlbumEltForMetadataFile(String metaAlbumFileName) throws BAGException {
		this.assureInit();
		this.updateMetaGallery();
		return this.gallery.getMetaAlbumEltForMetadataFile(metaAlbumFileName);
		
	}
	
	@Override
	public String getMetaAlbumStrForMetadataFile(String metaAlbumFileName) throws BAGException {
		this.assureInit();
		this.updateMetaGallery();
		return this.gallery.getMetaAlbumStrForMetadataFile(metaAlbumFileName);
		
	}
	
	@Override
	public HashMap<String, BAGThumbnail> getMetaDataToAlbumCoverMap() throws BAGException {
		this.assureInit();
		this.updateMetaGallery();
		return this.gallery.getMetaDataToAlbumCoverMap();
		
	}
	
	public String getMetaGalleryFileUrlStr() {
		
		return this.gallery.getMetaGalleryFileUrlStr();
	}
	
	public WFISVNClient getMetaStroage() {
		return this.metaStroage;
	}
	
	@Override
	public List<String> getNonAlbumedFolders() throws BAGException {
		this.assureInit();
		this.updateMetaGallery();
		return this.gallery.getNonAlbumedFolders();
		
	}
	
	public WAMCPSessionBackingBean getSessionBBean() {
		return this.sessionBBean;
	}
	
	@Override
	public void init() throws BAGException {
		try {
			
			if (!this.metaStroage.isInit()) {
				this.metaStroage.svnWCInit(this.sessionBBean.getUserName());
			}
			
			ServletContext servletContext = (ServletContext) FacesContext
					.getCurrentInstance().getExternalContext().getContext();
			// Image cache per user:
// String cacheLocalPath = URLPathStrUtils.appendParts(this.metaStroage.getUserDir()
// .getCanonicalPath(), this.CONFIG_PATH_CACHE_DIR);
//
// File cacheDir = new File(cacheLocalPath);
//
// if (!cacheDir.exists()) {
// cacheDir.mkdir();
// }
//
// this.gallery = BAGGalleryAbstract.getInstance(
// this.CONFIG_URL_GALLERY_ROOT,
// BAGGalleryAbstract.CONFIG_URL_ORDER_DEFAULT,
// cacheLocalPath,
// servletContext.getRealPath(""),
// this.metaStroage.getUserDir().getCanonicalPath(),
// this.djatokaServerUrlStr,
// new BAGGalleryFactory());
			
			// Aber warum?? We already prevent that two users (on the same VM) write the same cahce file at once
			String cacheLocalPath = servletContext.getRealPath(this.CONFIG_PATH_CACHE_DIR);
			
			this.gallery =
					new BAGGalleryFactory().construct(this.CONFIG_URL_GALLERY_ROOT,
							this.CONFIG_META_GALLERY_ROOT,
							BAGGalleryAbstract.CONFIG_URL_ORDER_DEFAULT,
							cacheLocalPath,
							servletContext.getRealPath(""),
							this.metaStroage.getUserDir().getCanonicalPath(),
							this.djatokaServerUrlStr);
			
// BAGGalleryAbstract.getInstance(
// this.CONFIG_URL_GALLERY_ROOT,
// this.CONFIG_META_GALLERY_ROOT,
// BAGGalleryAbstract.CONFIG_URL_ORDER_DEFAULT,
// cacheLocalPath,
// servletContext.getRealPath(""),
// this.metaStroage.getUserDir().getCanonicalPath(),
// this.djatokaServerUrlStr, /* this.galleryHttpAccPort, */
// new BAGGalleryFactory());
			
		} catch (IOException e) {
			throw new BAGException(e);
		} catch (SVNException e) {
			throw new BAGException(e);
		}
	}
	
	@Override
	public BAGAlbum openAlbum(String albumName) throws BAGException {
		this.assureInit();
		return this.gallery.openAlbum(albumName);
	}
	
	@Override
	public BAGAlbum openAlbumForMetadataFile(String metaAlbumFileName) throws BAGException {
		this.assureInit();
		this.updateMetaGallery();
		return this.gallery.openAlbumForMetadataFile(metaAlbumFileName);
	}
	
	public void setMetaStroage(WFISVNClient metaStroage) {
		this.metaStroage = metaStroage;
	}
	
	public void setSessionBBean(WAMCPSessionBackingBean sessionBBean) {
		this.sessionBBean = sessionBBean;
	}
	
	@SuppressWarnings("deprecation")
	protected void updateMetaGallery() throws BAGException {
		this.assureInit();
		try {
			
			this.metaStroage.updateFileInUserWorkingDir(
					BAGGalleryAbstract.FILENAME_GALLERY_METADATA_XML);
			
		} catch (SVNException e) {
			throw new BAGException(e);
		} catch (IOException e) {
			throw new BAGException(e);
		}
		
	}
	
}
