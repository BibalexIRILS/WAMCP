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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class FileUtils {
	public static void copyFile(File sourceFile, File destFile) throws IOException {
		if (!destFile.exists()) {
			destFile.createNewFile();
		}
		
		FileChannel source = null;
		FileChannel destination = null;
		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		} finally {
			if (source != null) {
				source.close();
			}
			if (destination != null) {
				destination.close();
			}
		}
	}
	
	// Deletes all files and subdirectories under dir.
	// Returns true if all deletions were successful.
	// If a deletion fails, the method stops attempting to delete and returns false.
	public static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (String element : children) {
				boolean success = deleteDir(new File(dir, element));
				if (!success) {
					return false;
				}
			}
		}
		
		// The directory is now empty so delete it
		return dir.delete();
	}
	
	public static String getExtension(String fileName) {
		String result = "";
		int lastDotIx = fileName.lastIndexOf('.');
		if (lastDotIx > 0) {
			result = fileName.substring(lastDotIx + 1);
		}
		return result;
	}
	
	public static String makeSafeFileName(String fileName) {
		
		String invalidChars = " \\/:*?\"<>|";
		
		char[] chars = fileName.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if ((invalidChars.indexOf(chars[i]) >= 0) // OS-invalid
					|| (chars[i] < '\u0020') // ctrls
					|| ((chars[i] > '\u007e') && (chars[i] < '\u00a0')) // ctrls
			) {
				chars[i] = '_';
			}
		}
		
		return new String(chars);
	}
	
	public static String removeExtension(String fileName) {
		
		String result = "";
		int lastDotIx = fileName.lastIndexOf('.');
		if (lastDotIx > 0) {
			result = fileName.substring(0, lastDotIx);
		} else {
			result = fileName;
		}
		return result;
		
	}
}
