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
import java.io.IOException;

import org.bibalex.gallery.exception.BAGException;
import org.bibalex.gallery.storage.BAGStorage;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class BAGGalleryWAlbumsCache extends BAGGalleryAbstract {
	
	private Document cachedMetaDataXml;
	
	protected BAGGalleryWAlbumsCache(String galleryRoot, String galleryDirect,
			String configUrlOrder,
			String cacheLocalPath, String contextRootPath, String metaGalleryRoot,
			String djatokaServerUrlStr) { // , int galleryHttpAccPort) {
		super(galleryRoot, galleryDirect, configUrlOrder, cacheLocalPath, contextRootPath,
				metaGalleryRoot,
				djatokaServerUrlStr); // , galleryHttpAccPort);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected Document getMetaDataXml() throws BAGException {
		if (this.cachedMetaDataXml == null) {
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
					
					this.cachedMetaDataXml = saxBuilder.build(metaGalleryIn);
					
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
		return this.cachedMetaDataXml;
	}
	
}
