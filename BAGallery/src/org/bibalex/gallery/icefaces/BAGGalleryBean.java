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

package org.bibalex.gallery.icefaces;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.bibalex.gallery.exception.BAGException;
import org.bibalex.gallery.model.BAGAlbum;
import org.bibalex.gallery.model.BAGGalleryAbstract;
import org.bibalex.gallery.model.BAGGalleryFactory;
import org.bibalex.gallery.model.BAGThumbnail;
import org.jdom.Element;

public class BAGGalleryBean implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7412321696593824191L;
	protected BAGGalleryAbstract gallery;
	protected final String CONFIG_PATH_CACHE_DIR;
	protected String djatokaServerUrlStr;
	protected final String CONFIG_URL_GALLERY_ROOT;
	protected final String CONFIG_META_GALLERY_ROOT;
	
// protected final int galleryHttpAccPort;
	
	public BAGGalleryBean(String djatokaServerUrlStr, String cONFIGURLGALLERYROOT,
			String cONFIGMETAGALLERYROOT,
			String cONFIGPATHCACHEDIR/* , int galleryHttpAccPort */) {
		super();
		this.djatokaServerUrlStr = djatokaServerUrlStr;
		this.CONFIG_PATH_CACHE_DIR = cONFIGPATHCACHEDIR;
		this.CONFIG_URL_GALLERY_ROOT = cONFIGURLGALLERYROOT;
		this.CONFIG_META_GALLERY_ROOT = cONFIGMETAGALLERYROOT;
		// this.galleryHttpAccPort = galleryHttpAccPort;
	}
	
	protected void assureInit() throws BAGException {
		if (this.gallery == null) {
			this.init();
		}
	}
	
	public BAGThumbnail getAlbumCoverFromLoadedTempXml(String albumName) throws BAGException {
		this.assureInit();
		return this.gallery.getAlbumCoverFromLoadedTempXml(albumName);
	}
	
	public List<BAGThumbnail> getAlbumCovers() throws BAGException {
		this.assureInit();
		
		return this.gallery.getAlbumCovers();
		
	}
	
	public List<String> getAllFolders() throws BAGException {
		this.assureInit();
		
		return this.gallery.getAllFolders();
		
	}
	
	public String getGalleryRootUrlStr() {
		return this.CONFIG_URL_GALLERY_ROOT;
	}
	
	public Element getMetaAlbumEltForMetadataFile(String metaAlbumFileName) throws BAGException {
		this.assureInit();
		
		return this.gallery.getMetaAlbumEltForMetadataFile(metaAlbumFileName);
		
	}
	
	public String getMetaAlbumStrForMetadataFile(String metaAlbumFileName) throws BAGException {
		this.assureInit();
		
		return this.gallery.getMetaAlbumStrForMetadataFile(metaAlbumFileName);
		
	}
	
	public HashMap<String, BAGThumbnail> getMetaDataToAlbumCoverMap() throws BAGException {
		this.assureInit();
		
		return this.gallery.getMetaDataToAlbumCoverMap();
	}
	
	public List<String> getNonAlbumedFolders() throws BAGException {
		this.assureInit();
		
		return this.gallery.getNonAlbumedFolders();
		
	}
	
	public void init() throws BAGException {
		
		ServletContext servletContext = (ServletContext) FacesContext
				.getCurrentInstance().getExternalContext().getContext();
		
		String cacheLocalPath = servletContext.getRealPath(this.CONFIG_PATH_CACHE_DIR);
		
		this.gallery =
				new BAGGalleryFactory().construct(
						this.CONFIG_URL_GALLERY_ROOT,
						this.CONFIG_META_GALLERY_ROOT,
						BAGGalleryAbstract.CONFIG_URL_ORDER_DEFAULT,
						cacheLocalPath,
						servletContext.getRealPath(""),
						this.CONFIG_META_GALLERY_ROOT,
						this.djatokaServerUrlStr);
// BAGGalleryAbstract.getInstance(
// this.CONFIG_URL_GALLERY_ROOT,
// this.CONFIG_META_GALLERY_ROOT,
// BAGGalleryAbstract.CONFIG_URL_ORDER_DEFAULT,
// cacheLocalPath,
// servletContext.getRealPath(""),
// this.CONFIG_META_GALLERY_ROOT,
// this.djatokaServerUrlStr, /* this.galleryHttpAccPort, */
// new BAGGalleryFactory());
		
	}
	
	public void loadTempXml() throws BAGException {
		this.assureInit();
		this.gallery.loadTempXml();
		
	}
	
	public BAGAlbum openAlbum(String albumName) throws BAGException {
		this.assureInit();
		return this.gallery.openAlbum(albumName);
	}
	
	public BAGAlbum openAlbumForMetadataFile(String metaAlbumFileName) throws BAGException {
		this.assureInit();
		return this.gallery.openAlbumForMetadataFile(metaAlbumFileName);
	}
	
	public void unloadTempXml() throws BAGException {
		this.assureInit();
		this.gallery.unloadTempXml();
		
	}
	
}
