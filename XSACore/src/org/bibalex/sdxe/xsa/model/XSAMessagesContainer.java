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

package org.bibalex.sdxe.xsa.model;

import java.util.HashMap;

import org.bibalex.sdxe.xsa.exception.XSAException;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

/**
 * A class for any XSA.xml element that contains a &lt;messages&gt; element. It provides utility methods for extracting
 * the messages in a certain locale.
 * 
 * @author Younos.Naga
 * 
 */
public class XSAMessagesContainer extends XSANode {
	private XPath msgLocaleXP;
	private XPath msgNoLocaleXP;
	private HashMap<String, XSAMessages> messagesMap = null;
	
	private static final String XPPARAM_LOCALE = "msgsLocale";
	
	/**
	 * Create XSAMessageContainer for the given dom element
	 * @param domInstance the dom instance that the message container will be created for
	 * @throws XSAException
	 * @throws JDOMException
	 */
	public XSAMessagesContainer(Element domInstance) throws XSAException, JDOMException {
		super(domInstance);
		
		this.messagesMap = new HashMap<String, XSAMessages>();
		
		String msgLocaleStr = this.pfx + "messages[@locale=$" + XPPARAM_LOCALE + "]";
		this.msgLocaleXP = XPath.newInstance(msgLocaleStr);
		this.msgLocaleXP.addNamespace(this.namespace);
		
		String msgNoLocaleStr = this.pfx + "messages[not (@locale)]";
		this.msgNoLocaleXP = XPath.newInstance(msgNoLocaleStr);
		this.msgNoLocaleXP.addNamespace(this.namespace);
		
	}
	
	/**
	 * 
	 * @param locale
	 * @return The help message in the passed locale
	 * @throws JDOMException
	 * @throws XSAException
	 */
	public String getHelp(String locale) throws JDOMException, XSAException {
		return this.getMessage(locale, "help");
	}
	
	/**
	 * 
	 * @return The label message in the default (en) locale
	 * @throws XSAException
	 * @throws JDOMException
	 */
	public String getLabel() throws XSAException, JDOMException {
		return this.getLabel("en");
	}
	
	/**
	 * 
	 * @param locale
	 * @return The label message in the passed locale
	 * @throws JDOMException
	 * @throws XSAException
	 */
	public String getLabel(String locale) throws JDOMException, XSAException {
		return this.getMessage(locale, "label");
	}
	
	/**
	 * This allows extending the messages stored in XSA.xml. To store any message use any name as its element name.
	 * 
	 * @param locale
	 * @param msgName
	 * @return A message contained in an element whose name is passed, in a messages element of the locale passed
	 * @throws JDOMException
	 * @throws XSAException
	 */
	public String getMessage(String locale, String msgName) throws JDOMException, XSAException {
		XSAMessages localeMsgs = this.getMessages(locale);
		return (localeMsgs != null ? localeMsgs.getMessage(msgName) : null);
	}
	
	/**
	 * 
	 * @param locale
	 * @return The message element for the passed locale, or the default messages if the passed locale is not found
	 * @throws JDOMException
	 * @throws XSAException
	 */
	private XSAMessages getMessages(String locale) throws JDOMException, XSAException {
		if (!this.messagesMap.containsKey(locale)) {
			
			this.msgLocaleXP.setVariable(XPPARAM_LOCALE, locale);
			
			Object xpResult = this.msgLocaleXP.selectSingleNode(this.domRep);
			
			if (xpResult == null) {
				xpResult = this.msgNoLocaleXP.selectSingleNode(this.domRep);
			}
			
			if (xpResult != null) {
				this.messagesMap.put(locale, new XSAMessages((Element) xpResult));
			}
		}
		return this.messagesMap.get(locale);
	}
	
}
