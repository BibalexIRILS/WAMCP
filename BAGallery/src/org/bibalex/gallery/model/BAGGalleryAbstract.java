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

package org.bibalex.gallery.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.VFS;
import org.apache.log4j.Logger;
import org.bibalex.gallery.exception.BAGAlbumAlreadyExistsException;
import org.bibalex.gallery.exception.BAGException;
import org.bibalex.gallery.exception.BAGNotFoundException;
import org.bibalex.gallery.storage.BAGStorage;
import org.bibalex.util.URLPathStrUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

public abstract class BAGGalleryAbstract {
	public enum EnumResolutions {
		thumb, /* low, med, */high
	}
	
// Too much caching could kill you!! Concurrency Issues.. and Djatoka already has a tiles cache anyway
// protected static final Map<String, byte[]> imageBytesCache = Collections
// .synchronizedMap(new WeakHashMap<String, byte[]>());;
	
	public static final String CONFIG_URL_ORDER_RESALBUM = "ResAlbum";
	
	public static final String CONFIG_URL_ORDER_ALBUMRES = "AlbumRes";
	public static final String CONFIG_URL_ORDER_DEFAULT = CONFIG_URL_ORDER_RESALBUM;
	public static final String FILENAME_GALLERY_METADATA_XML = "metagallery.xml";
	
	private static final Logger LOG = Logger.getLogger("org.bibalex.gallery");
	
	// gallery Root => http access gallery Direct => direct access
	// TODO rename
	protected final String galleryRoot;
	protected final String galleryDirect;
	
	protected final String configUrlOrder;
	
	protected final String cacheLocalPath;
	
	protected final String contextRootPath;
	
	protected final String metaGalleryRoot;
	
	protected final String djatokaServerUrlStr;
	
// protected final int CONFIG_HTTPACC_PORT;
	
// Too much caching could kill you!! Concurrency Issues!
// protected static Map<String, BAGGalleryAbstract> instances = Collections
// .synchronizedMap(new WeakHashMap<String, BAGGalleryAbstract>());
//
// public static BAGGalleryAbstract getInstance(String galleryRoot, String galleryDirect,
// String configUrlOrder,
// String cacheLocalPath,
// String contextRootPath, String metaGalleryRoot, String djatokaServerUrlStr,
// /* int galleryHttpAccPort, */
// IBAGGalleryFactory factory) {
// // TODO Revisit and correct the problem of sharing the same gallery between users while the metagallery
// // is stored in the WC copy of each of them.. it uses the first
// return factory
// .construct(galleryRoot, galleryDirect, configUrlOrder,
// cacheLocalPath,
// contextRootPath, metaGalleryRoot,
// djatokaServerUrlStr/* , galleryHttpAccPort */);
// // synchronized (instances) {
// // String key = galleryRoot + galleryDirect + configUrlOrder + cacheLocalPath
// // + contextRootPath
// // + metaGalleryRoot + djatokaServerUrlStr;
// // if (!instances.containsKey(key)) {
// // instances
// // .put(key,
// // factory
// // .construct(galleryRoot, galleryDirect, configUrlOrder,
// // cacheLocalPath,
// // contextRootPath, metaGalleryRoot,
// // djatokaServerUrlStr/* , galleryHttpAccPort */));
// // }
// //
// // return instances.get(key);
// // }
// }
	
	private Document tempXml;
	
	protected BAGGalleryAbstract(String galleryRoot, String galleryDirect, String configUrlOrder,
			String cacheLocalPath,
			String contextRootPath, String metaGalleryRoot, String djatokaServerUrlStr
	/* int galleryHttpAccPort */) {
		super();
		
		this.galleryRoot = galleryRoot;
		this.galleryDirect = galleryDirect;
		this.configUrlOrder = configUrlOrder;
		this.cacheLocalPath = cacheLocalPath;
		this.contextRootPath = contextRootPath;
		this.metaGalleryRoot = metaGalleryRoot;
		this.djatokaServerUrlStr = djatokaServerUrlStr;
// this.CONFIG_HTTPACC_PORT = galleryHttpAccPort;
	}
	
	public void addAlbumToMetaGallery(String albumName, String albumCaption, String coverName,
			String metaFileName) throws BAGException {
		try {
			Document metaDataXml = this.getMetaDataXml();
			
			XPath albumExistCheckXP;
			
			albumExistCheckXP = XPath.newInstance("//album[@name=$VARNAME]");
			albumExistCheckXP.setVariable("VARNAME", albumName);
			
			if (albumExistCheckXP.selectNodes(metaDataXml).size() > 0) {
				throw new BAGAlbumAlreadyExistsException("Album already exists in metadata file: "
						+ albumName);
			}
			
			Element galleryElt = metaDataXml.getRootElement();
			
			Element albumElt = new Element("album");
			albumElt.setAttribute("name", albumName);
			
			galleryElt.addContent(albumElt);
			
			Element coverElt = new Element("cover");
			coverElt.setText(coverName);
			
			albumElt.addContent(coverElt);
			
			Element captionElt = new Element("caption");
			captionElt.setText(albumCaption);
			
			albumElt.addContent(captionElt);
			
			Element metadataElt = new Element("metadata");
			metadataElt.setText(metaFileName);
			
			albumElt.addContent(metadataElt);
			
			// ////////////////
			this.persistMetaGallery(metaDataXml);
		} catch (JDOMException e) {
			throw new BAGException(e);
		}
		
	}
	
	protected ArrayList<BAGThumbnail> createAlbumCoversFromXML(Document metaDataXml)
			throws BAGException {
		try {
			ArrayList<BAGThumbnail> result = new ArrayList<BAGThumbnail>();
			
			XPath albumsXP = XPath.newInstance("//album");
			
			for (Object obj : albumsXP.selectNodes(metaDataXml)) {
				Element elt = (Element) obj;
				
				String name = elt.getAttributeValue("name");
				
				String title;
				
				Element optionalTitle = elt.getChild("caption");
				if (optionalTitle != null) {
					title = optionalTitle.getTextNormalize();
					if ((title == null) || title.isEmpty()) {
						title = name;
					}
				} else {
					title = name;
				}
				
				String coverImgName = elt.getChild("cover").getTextNormalize();
				
				String coverImgLocalUrl;
				try {
					coverImgLocalUrl = this.getThumbLocalUrl(name, coverImgName);
				} catch (BAGException e) {
					LOG.error(e, e);
					continue;
				}
				// this.getThumbLocalUrl(name, coverImgName);
				BAGThumbnail cover = new BAGThumbnail(name, title, coverImgLocalUrl);
				
				result.add(cover);
			}
			
			return result;
		} catch (JDOMException e) {
			throw new BAGException(e);
		}
	}
	
	protected HashMap<String, BAGThumbnail> createMetaDataToAlbumCoverMapFromXML(
			Document metaDataXml)
			throws BAGException {
		try {
			HashMap<String, BAGThumbnail> result = new HashMap<String, BAGThumbnail>();
			
			XPath albumsXP = XPath.newInstance("//album");
			
			for (Object obj : albumsXP.selectNodes(metaDataXml)) {
				Element elt = (Element) obj;
				
				String name = elt.getAttributeValue("name");
				
				String title;
				
				Element optionalTitle = elt.getChild("caption");
				if (optionalTitle != null) {
					title = optionalTitle.getTextNormalize();
					if ((title == null) || title.isEmpty()) {
						title = name;
					}
				} else {
					title = name;
				}
				
				String coverImgName = elt.getChild("cover").getTextNormalize();
				
				String coverImgLocalUrl;
				try {
					coverImgLocalUrl = this.getThumbLocalUrl(name, coverImgName);
				} catch (BAGException e) {
					LOG.error(e, e);
					continue;
				}
				// this.getThumbLocalUrl(name, coverImgName);
				BAGThumbnail cover = new BAGThumbnail(name, title, coverImgLocalUrl);
				
				String metaDataFileName = elt.getChild("metadata").getTextNormalize();
				
				result.put(metaDataFileName, cover);
			}
			
			return result;
		} catch (JDOMException e) {
			throw new BAGException(e);
		}
	}
	
	public String deleteAlbumForMetadataFile(String metaAlbumFileName) throws BAGException {
		
		Element albumElt = this.getAlbumEltForMetadataFile(metaAlbumFileName);
		
		String albumName = albumElt.getAttributeValue("name");
		
		Document metaGalleryXML = albumElt.getDocument();
		
		albumElt.detach();
		
		this.persistMetaGallery(metaGalleryXML);
		
		return albumName;
	}
	
	public void deleteAlbumFromMetaGallery(String albumName) throws BAGException {
		try {
			Document metaDataXml = this.getMetaDataXml();
			
			XPath albumExistCheckXP;
			
			albumExistCheckXP = XPath.newInstance("//album[@name=$VARNAME]");
			albumExistCheckXP.setVariable("VARNAME", albumName);
			
			for (Object obj : albumExistCheckXP.selectNodes(metaDataXml)) {
				((Element) obj).detach();
			}
			// ////////////////
			this.persistMetaGallery(metaDataXml);
		} catch (JDOMException e) {
			throw new BAGException(e);
		}
	}
	
	public BAGThumbnail getAlbumCoverFromLoadedTempXml(String albumName) throws BAGException {
		if (this.tempXml == null) {
			throw new BAGException("Must load temp xml before callig getAlbumCover");
		}
		try {
			ArrayList<BAGThumbnail> result = new ArrayList<BAGThumbnail>();
			
			XPath albumXP = XPath.newInstance("//album[@name=$ALBUMNAME]");
			albumXP.setVariable("ALBUMNAME", albumName);
			Object obj = albumXP.selectSingleNode(this.tempXml);
			if (obj == null) {
				return null;
				// throw new BAGException("Album not found");
			}
			Element elt = (Element) obj;
			
			String title;
			
			Element optionalTitle = elt.getChild("caption");
			if (optionalTitle != null) {
				title = optionalTitle.getTextNormalize();
				if ((title == null) || title.isEmpty()) {
					title = albumName;
				}
			} else {
				title = albumName;
			}
			
			String coverImgName = elt.getChild("cover").getTextNormalize();
			
			BAGThumbnail cover = new BAGThumbnail(albumName, title, coverImgName);
			
			return cover;
		} catch (JDOMException e) {
			throw new BAGException(e);
		}
	}
	
	public List<BAGThumbnail> getAlbumCovers() throws BAGException {
		Document metaDataXml = this.getMetaDataXml();
		return this.createAlbumCoversFromXML(metaDataXml);
	}
	
	protected String getAlbumDirectAccURLStr(String albumName, EnumResolutions res) {
		String result;
		
		result = URLPathStrUtils.appendParts(this.galleryDirect, this.orderUrlParts(
				res.toString(),
				albumName));
		
		return result;
		
	}
	
	private Element getAlbumEltForMetadataFile(String metaAlbumFileName) throws BAGException {
		try {
			Document metagallery = this.getMetaDataXml();
			
			XPath albumForMetadataFileXP;
			
			albumForMetadataFileXP = XPath.newInstance("//album[child::metadata = $VARMETAFN]");
			
			albumForMetadataFileXP.setVariable("VARMETAFN", metaAlbumFileName);
			
			List objs = albumForMetadataFileXP.selectNodes(metagallery);
			
			if (objs.size() == 0) {
				throw new BAGNotFoundException("Querying for the album of metadata file: "
						+ metaAlbumFileName + " shourld return one result but returned: "
						+ objs.size());
			} else if (objs.size() > 1) {
				throw new BAGException("Querying for the album of metadata file: "
						+ metaAlbumFileName + " shourld return one result but returned: "
						+ objs.size());
			}
			
			return ((Element) objs.get(0));
		} catch (JDOMException e) {
			throw new BAGException(e);
		}
	}
	
	protected String getAlbumHttpAccUrlStr(String albumName, EnumResolutions res) {
		String resHttpBase;
		
		resHttpBase = URLPathStrUtils.appendParts(this.galleryRoot, this.orderUrlParts(
				res.toString(),
				albumName));
		
		return resHttpBase;
	}
	
	protected ArrayList<String> getAlbumNamesFromXML(Document metaDataXml)
			throws BAGException {
		// TODO refactor so that the logic shared with createAlbumCo.. is DRY
		try {
			ArrayList<String> result = new ArrayList<String>();
			
			XPath albumsXP = XPath.newInstance("//album");
			
			for (Object obj : albumsXP.selectNodes(metaDataXml)) {
				Element elt = (Element) obj;
				
				String name = elt.getAttributeValue("name");
				
				String title;
				
				result.add(name);
			}
			
			return result;
		} catch (JDOMException e) {
			throw new BAGException(e);
		}
	}
	
	public List<String> getAllFolders() throws BAGException {
		String albumsFolderUrlStr = this.galleryRoot;
		
		if (CONFIG_URL_ORDER_RESALBUM.equals(this.configUrlOrder)) {
			albumsFolderUrlStr = URLPathStrUtils.appendParts(albumsFolderUrlStr,
					EnumResolutions.thumb.toString());
		} else {
			throw new UnsupportedOperationException("TODO");
		}
		
		return BAGStorage.listChildren(albumsFolderUrlStr, FileType.FOLDER);
		
// public List<BAGThumbnail> getAllFolders() throws BAGException {
//
// ArrayList<BAGThumbnail> result = new ArrayList<BAGThumbnail>();
// for (String albumName : BAGStorage.listChildren(albumsFolderUrlStr, FileType.FOLDER)) {
// String albumThumbsUrlStr = this.getResolutionBaseURLStr(albumName,
// EnumResolutions.thumb);
// String firstThumbUrlStr = BAGStorage.listChildren(albumThumbsUrlStr, FileType.FILE).get(0);
//
// String firstThumbLocalUrl = this.getThumbLocalUrl(albumName, FilenameUtils
// .removeExtension(firstThumbUrlStr));
//
// result.add(new BAGThumbnail(albumName, albumName, firstThumbLocalUrl));
// }
// return result;
		
	}
	
	public String getDjatokaServerUrlStr() {
		return this.djatokaServerUrlStr;
	}
	
	protected String getImageDirectAccUrlStr(String albumName, String imageName, EnumResolutions res) {
		
		String resDirectpBase = this.getAlbumDirectAccURLStr(albumName, res);
		
		return this.getImageUrlStr(resDirectpBase, imageName, res);
		
	}
	
	protected String getImageHttpAccUrlStr(String albumName, String imageName, EnumResolutions res)
			throws BAGException {
		
		String resHttpBase = this.getAlbumHttpAccUrlStr(albumName, res);
		
		return this.getImageUrlStr(resHttpBase, imageName, res);
		
	}
	
	private String getImageUrlStr(String albumBaseUrlStr, String imageName, EnumResolutions res) {
		String ext = null;
		switch (res) {
			case high:
				ext = ".jp2";
				break;
			case thumb:
				ext = ".jpg";
				break;
			
		}
		
		String imgUrlStr = URLPathStrUtils
				.appendParts(albumBaseUrlStr,
						imageName + ext);
		
		return imgUrlStr;
	}
	
// public byte[] getImageBytes(String imageUrlStr) throws BAGException {
// synchronized (imageBytesCache) {
// if (!imageBytesCache.containsKey(imageUrlStr)) {
// ByteArrayOutputStream cacheBytesOut = new ByteArrayOutputStream();
// try {
// BAGStorage.readRemoteFile(imageUrlStr, cacheBytesOut);
// imageBytesCache.put(imageUrlStr, cacheBytesOut.toByteArray());
// } finally {
// try {
// cacheBytesOut.close();
// } catch (IOException e) {
// throw new BAGException(e);
// }
// }
// }
//
// return imageBytesCache.get(imageUrlStr);
// }
// }
	
	public Element getMetaAlbumEltForMetadataFile(String metaAlbumFileName) throws BAGException {
		return (Element) this.getAlbumEltForMetadataFile(metaAlbumFileName).clone();
	}
	
	public String getMetaAlbumStrForMetadataFile(String metaAlbumFileName) throws BAGException {
		try {
			Element albumElt = this.getAlbumEltForMetadataFile(metaAlbumFileName);
			
			XMLOutputter outputter = new XMLOutputter(Format.getCompactFormat()
					.setEncoding("UTF-8"));
			
			StringWriter resultWriter = new StringWriter();
			
			outputter.output(albumElt, resultWriter);
			
			resultWriter.flush();
			resultWriter.close();
			
			String result = resultWriter.toString();
			
			return result;
		} catch (IOException e) {
			throw new BAGException(e);
		}
		
	}
	
	public HashMap<String, BAGThumbnail> getMetaDataToAlbumCoverMap() throws BAGException {
		Document metaDataXml = this.getMetaDataXml();
		return this.createMetaDataToAlbumCoverMapFromXML(metaDataXml);
	}
	
	protected abstract Document getMetaDataXml() throws BAGException;
	
	public String getMetaGalleryFileUrlStr() {
		return URLPathStrUtils.appendParts(this.metaGalleryRoot, FILENAME_GALLERY_METADATA_XML);
	}
	
	public List<String> getNonAlbumedFolders() throws BAGException {
// public List<BAGThumbnail> getNonAlbumedFolders() throws BAGException {
//
// List<BAGThumbnail> result = this.getAllFolders();
//
// List<BAGThumbnail> albums = this.getAlbumCovers();
		
		List<String> result = this.getAllFolders();
		
		Document metaDataXml = this.getMetaDataXml();
		List<String> albums = this.getAlbumNamesFromXML(metaDataXml);
		
		result.removeAll(albums);
		
// Iterator<String> candidatesIter = result.iterator();
// while (candidatesIter.hasNext()) {
//
// // BAGThumbnail candidate = candidatesIter.next();
// String candidate = candidatesIter.next();
// if (albums.indexOf(candidate) != -1) {
// candidatesIter.remove();
// }
//
// }
		
		return result;
	}
	
	public String getThumbLocalUrl(String albumName, String imageName) throws BAGException {
		try {
			String thumbImgUrlStr = this.getImageHttpAccUrlStr(albumName, imageName,
					EnumResolutions.thumb);
			
			String cachedURLAbs = BAGStorage.cacheFileLocally(
					URLPathStrUtils.appendParts(this.cacheLocalPath, "thumb", albumName),
					thumbImgUrlStr)
					.toString();
			
			String ctxRootURL = VFS.getManager().resolveFile(this.contextRootPath).getURL()
					.toString();
			
			String result = cachedURLAbs.substring(ctxRootURL.length());
			
			return result;
			
		} catch (FileSystemException e) {
			throw new BAGException(e);
		}
	}
	
	public void loadTempXml() throws BAGException {
		if (this.tempXml != null) {
			throw new BAGException("Already loaded");
		}
		SAXBuilder saxBuilder = new SAXBuilder();
		
		saxBuilder.setFeature("http://xml.org/sax/features/validation", false);
		saxBuilder.setFeature("http://xml.org/sax/features/namespaces", true);
		saxBuilder.setFeature("http://xml.org/sax/features/namespace-prefixes",
					true);
// Unsupported: saxBuilder.setFeature("http://xml.org/sax/features/xmlns-uris", false);
		
		String metaDataUrl = this.getMetaGalleryFileUrlStr();
// URLPathStrUtils.appendParts(this.metaGalleryRoot, FILENAME_GALLERY_METADATA_XML);
		
		try {
			ByteArrayOutputStream remoteFileOS = new ByteArrayOutputStream();
			ByteArrayInputStream metaGalleryIn = null;
			try {
				BAGStorage.readRemoteFile(metaDataUrl, remoteFileOS);
				
				metaGalleryIn = new ByteArrayInputStream(remoteFileOS
							.toByteArray());
				
				this.tempXml = saxBuilder.build(metaGalleryIn);
				
			} finally {
				try {
					remoteFileOS.close();
					metaGalleryIn.close();
				} catch (IOException e) {
					throw new BAGException(e);
				}
			}
			
		} catch (JDOMException e) {
			throw new BAGException(e);
		} catch (IOException e) {
			throw new BAGException(e);
		}
		
	}
	
	public BAGAlbum openAlbum(String name) throws BAGException {
		
		return new BAGAlbum(this, name);
		
	}
	
	public BAGAlbum openAlbumForMetadataFile(String metaAlbumFileName) throws BAGException {
		
		String albumName = this.getAlbumEltForMetadataFile(metaAlbumFileName).getAttributeValue(
				"name");
		
		return this.openAlbum(albumName);
		
	}
	
	protected String[] orderUrlParts(String resolution, String albumName) {
		if (CONFIG_URL_ORDER_RESALBUM.equals(this.configUrlOrder)) {
			return new String[] { resolution, albumName };
		} else if (CONFIG_URL_ORDER_ALBUMRES.equals(this.configUrlOrder)) {
			return new String[] { albumName, resolution };
		} else {
			throw new IllegalArgumentException(this.configUrlOrder); // config not argument
		}
	}
	
	protected void persistMetaGallery(Document metaDataXml) throws BAGException {
		try {
			String filePath = this.getMetaGalleryFileUrlStr();
			
			FileWriter writer = new FileWriter(filePath);
			
			XMLOutputter outputter = new XMLOutputter(Format.getCompactFormat()
					.setEncoding("UTF-8"));
			outputter.output(metaDataXml, writer);
		} catch (IOException e) {
			throw new BAGException(e);
		}
	}
	
	public void unloadTempXml() {
		this.tempXml = null;
	}
}
