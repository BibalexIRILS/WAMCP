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

package org.bibalex.sdxe.suggest.model;

import java.util.StringTokenizer;

import com.sun.xml.xsom.XSAnnotation;

public class SugtnUtil {
	public static final String ANNOTATION_TAG_NAME_PARENT = "xs:annotation";
	public static final String ANNOTATION_TAG_NAME_DOC = "xs:documentation";
	public static final String ANNOTATION_TAG_NAME_APP = "xs:appinfo";
	
	/**
	 * Annotation or comment of the node.
	 * XSOM considers annotation as W3C node which represents XML nodes from xsd.xml file. 
	 * We extract the data from W3C node and then add it to documentation.
	 *  
	 * @param annotation
	 * 			XSOM annotation that has the documentation
	 * @return
	 * 		documentation of the node. It returns null if annotation is null.
	 */
	public static String documentationFromAnnotation(XSAnnotation annotation) {
		String result = null;
		
		if (annotation == null) {
			return result;
		}
		
		org.w3c.dom.Element annotElm = (org.w3c.dom.Element) annotation.getAnnotation();
		org.w3c.dom.NodeList docList = annotElm.getElementsByTagName(ANNOTATION_TAG_NAME_DOC);
		
		StringBuilder docStr = new StringBuilder();
		
		for (int i = 0; i < docList.getLength(); ++i) {
			org.w3c.dom.Element docElm = (org.w3c.dom.Element) docList.item(i);
			docElm.normalize();
			docStr.append(((org.w3c.dom.Node) docElm).getTextContent().trim() + " ");
		}
		
		StringTokenizer internalTrimmer = new StringTokenizer(docStr.toString(), " ", false);
		docStr = new StringBuilder();
		
		while (internalTrimmer.hasMoreTokens()) {
			docStr.append(internalTrimmer.nextToken())
					.append(" ");
		}
		
		result = docStr.toString();
		
		return result;
	}
}
