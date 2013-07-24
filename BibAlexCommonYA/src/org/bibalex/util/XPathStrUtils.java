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

public class XPathStrUtils {
	public static int getLastIx(String xpStr) {
		String result = "-1";
		
		if ((xpStr.length() > 0) && (xpStr.lastIndexOf("]") == xpStr.length() - 1)) {
			
			result = xpStr.substring(xpStr.lastIndexOf("[") + 1, xpStr.lastIndexOf("]"));
			
		}
		
		try {
			return Integer.parseInt(result);
		} catch (NumberFormatException e) {
			if (result.equals("last()")) {
				return 0;
			} else {
				return -2;
			}
		}
		
	}
	
	public static String getLastStep(String xpStr) {
		int lastSlash = xpStr.lastIndexOf("/");
		String result = "";
		
		if (lastSlash != -1) {
			result = xpStr.substring(lastSlash + 1);
		}
		
		return result;
	}
	
	public static int getStepsCount(String xPathAbs) {
		if ((xPathAbs == null) || xPathAbs.isEmpty()) {
			return 0;
		}
		int result = 0;
		
		if (!xPathAbs.startsWith("/")) {
			xPathAbs = "/" + xPathAbs;
		}
		
		do {
			xPathAbs = removeLastStep(xPathAbs);
			++result;
		} while ((xPathAbs != null) && !xPathAbs.isEmpty());
		
		return result;
	}
	
	public static String removeLastIx(String xpStr) {
		String result = xpStr;
		
		if (getLastIx(result) >= 0) {
			
			result = result.substring(0, result.lastIndexOf("["));
			
		}
		
		return result;
	}
	
	public static String removeLastStep(String xpStr) {
		int lastSlash = xpStr.lastIndexOf("/");
		
		if (lastSlash != -1) {
			xpStr = xpStr.substring(0, lastSlash);
		}
		
		return xpStr;
	}
	
	public static String setLastIx(String xpStr, int ix) {
		String result = xpStr;
		
		if (xpStr.indexOf("/@") != -1) {
			result = removeLastStep(result);
		}
		
		result = removeLastIx(result);
		
		result = result + "[" + ix + "]";
		
		if (xpStr.indexOf("/@") != -1) {
			result = result + "/" + xpStr.substring(xpStr.lastIndexOf('@'));
		}
		
		return result;
		
	}
}
