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

package org.bibalex.oai;

import java.net.URI;

public class DCTypes {
	/**
	 * Examples include books, letters, dissertations, poems, newspapers, articles, archives of mailing lists. Note that
	 * facsimiles or images of texts are still of the genre Text.
	 */
	public static URI TEXT = URI.create("http://purl.org/dc/dcmitype/Text");
	public static URI IMAGE = URI.create("http://purl.org/dc/dcmitype/Image");
	public static URI SERVICE = URI.create("http://purl.org/dc/dcmitype/Service");
	public static URI COLLECTION = URI.create("http://purl.org/dc/dcmitype/Collection");
	
}
