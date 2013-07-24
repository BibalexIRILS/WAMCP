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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.vfs.FileType;
import org.bibalex.gallery.exception.BAGException;
import org.bibalex.gallery.model.BAGGalleryAbstract.EnumResolutions;
import org.bibalex.gallery.storage.BAGStorage;

public class BAGAlbum {
	
	private final BAGGalleryAbstract gallery;
	private final String name;
	private final LinkedList<BAGThumbnail> thumbnails;
	private final HashMap<Integer, String> iamgeNamesByIx;
	private final HashMap<String, Integer> imageIxesByName;
	private final int imagesCount;
	
	public BAGAlbum(BAGGalleryAbstract gallery, String name) throws BAGException {
		super();
		this.gallery = gallery;
		this.name = name;
		
		this.thumbnails = new LinkedList<BAGThumbnail>();
		this.iamgeNamesByIx = new HashMap<Integer, String>();
		this.imageIxesByName = new HashMap<String, Integer>();
		
		String thumbsUrl = this.gallery.getAlbumHttpAccUrlStr(this.name, EnumResolutions.thumb);
		int ix = 0;
		for (String filename : BAGStorage.listChildren(thumbsUrl, FileType.FILE)) {
			String imageName = FilenameUtils.removeExtension(filename);
			
			String urlStr = this.gallery.getThumbLocalUrl(this.name, imageName);
			
			BAGThumbnail thumb = new BAGThumbnail(imageName, imageName, urlStr);
			
			this.thumbnails.add(thumb);
			
			Integer ixObj = Integer.valueOf(ix);
			this.iamgeNamesByIx.put(ixObj, imageName);
			this.imageIxesByName.put(imageName, ixObj);
			ix++;
		}
		
		this.imagesCount = this.thumbnails.size();
	}
	
	public int getImagesCount() {
		
		return this.imagesCount;
	}
	
	public int getIx(BAGImage image) throws BAGException {
		Integer ixObj = this.imageIxesByName.get(image.getName());
		if (ixObj == null) {
			throw new BAGException("No such image");
		}
		
		return ixObj.intValue();
	}
	
	public String getName() {
		
		return this.name;
	}
	
	public List<BAGThumbnail> getThumbnails() {
		return this.thumbnails;
	}
	
	public BAGImage openImage(int ix, int width, int height) throws BAGException {
		String imageName = this.iamgeNamesByIx.get(Integer.valueOf(ix));
		if ((imageName == null) || imageName.isEmpty()) {
			throw new BAGException("Invlid image index");
		}
		return this.openImage(imageName, width, height);
	}
	
	public BAGImage openImage(String imageName, int width, int height) throws BAGException {
		return new BAGImage(this.gallery, this, imageName, width, height);
	}
}
