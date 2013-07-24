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

package org.bibalex.util;

import java.io.File;

public class URLPathStrUtils {
	public static String appendParts(String base, String... parts) {
		char sep = '/';
		if (base.charAt(1) == ':') {
			// Windows files
			sep = '\\';
		}
		int lastSlashIx = base.lastIndexOf(sep);
		if (lastSlashIx == base.length() - 1) {
			base = base.substring(0, lastSlashIx);
		}
		
		String result = base;
		
		for (String part : parts) {
			
			result += sep + part;
		}
		
		return result;
	}
	
	public static String assurePathSeparatorAtEnd(String path) {
		String result = path;
		
		if (result.lastIndexOf(File.separatorChar) != result.length() - 1) {
			result += File.separatorChar;
		}
		
		return result;
	}
	
}
