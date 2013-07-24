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

public class BAGThumbnail {
	private final String name;
	
	private final String caption;
	
	private final String imageUrlStr;
	
	public BAGThumbnail(String name, String caption, String imageUrlStr) {
		super();
		this.caption = caption;
		this.name = name;
		this.imageUrlStr = imageUrlStr;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof BAGThumbnail) {
			return this.name.equals(((BAGThumbnail) other).name);
		} else {
			return super.equals(other);
		}
	}
	
	public String getCaption() {
		return this.caption;
	}
	
	public String getImageUrlStr() {
		return this.imageUrlStr;
	}
	
	public String getName() {
		return this.name;
	}
	
	@Override
	public int hashCode() {
		return this.name.hashCode();
	}
	
	@Override
	public String toString() {
		return this.name;
	}
	
}
