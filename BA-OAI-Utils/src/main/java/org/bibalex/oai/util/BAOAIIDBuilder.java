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

package org.bibalex.oai.util;

import java.util.StringTokenizer;

public class BAOAIIDBuilder {
	public static String getDomain(String oaiId) {
		StringTokenizer oaiIdTokens = new StringTokenizer(oaiId, ":", false);
		oaiIdTokens.nextToken(); // skip the oai scheme prefix
		return oaiIdTokens.nextToken();
	}
	
	public static String localIdFromOaiId(String oaiId) {
		String result = null;
		try {
			StringTokenizer tokenizer = new StringTokenizer(oaiId, ":");
			tokenizer.nextToken();
			tokenizer.nextToken();
			result = tokenizer.nextToken();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
// To avoid making unnecessary changes to the way images are identified, the OAI ID will not have any
// special way of constructing child parent relationships.. so it is not the role of this class to getParentId
// public static String parentIdFromOaiId(String oaiId) {
// String result = null;
//
// int lastDash = oaiId.lastIndexOf('-');
//
// if (lastDash == -1) {
//
// result = localIdFromOaiId(oaiId);
//
// } else {
//
// String parentOaiId = oaiId.substring(0, lastDash);
//
// result = localIdFromOaiId(parentOaiId);
// }
//
// return result;
// }
	
	protected final String domain;
	
	public BAOAIIDBuilder() {
		this.domain = "bibalex.org";
	}
	
	public BAOAIIDBuilder(String domain) {
		super();
		this.domain = domain;
	}
	
	/**
	 * Characters in the unreserved and reserved sets *must not* be escaped.
	 * reserved =";" | "/" | "?" | ":" | "@" | "&" | "=" | "+" | "$" | ","
	 * unreserved = alphanum | mark
	 * mark = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"
	 * 
	 * The rest must be escaped according to the encoding
	 * http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#SpecialCharacters
	 * 
	 * @param localId
	 * @return
	 */
	public String buildId(String localId) {
		
		String result;
		String localIdEscaped = localId;
		
		// PRESERVE ORDER OF REPLACES!!
		localIdEscaped = localIdEscaped.replaceAll(
				"[\\W&&[^\\;\\/\\?\\:\\@\\&\\=\\+\\$\\,\\-\\_\\.\\!\\~\\*\\'\\|\\%\\#\\x20]]", "_");
		localIdEscaped = localIdEscaped.replaceAll("%", "%25");
		localIdEscaped = localIdEscaped.replaceAll("#", "%23");
		localIdEscaped = localIdEscaped.replaceAll(" ", "%20");
		
		result = "oai:" + this.domain + ":" + localIdEscaped;
		return result;
	}
}
