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

package org.bibalex.sdxe.xsa.application;

import org.apache.log4j.Logger;
import org.bibalex.exception.BibalexException;
import org.bibalex.exception.Correctable;

/**
 * This bean provides a way for showing messages in a floating DIV that appears at the top of the screen, so that the
 * user sees it regardless of the current scroll top.
 * 
 * @author Younos.Naga
 * 
 */
public class XSARequestBackingBean {
	private static class StyledMessage {
		private String text = ""; // "This area displays important messages";
		
		private String style = "";
		
		/**
		 * @param messageText
		 *            the messageText to set
		 */
		private void setMsg(String message, StyledMsgTypeEnum msgType) {
			this.text = message;
			switch (msgType) {
				case ERROR:
					this.style = "iceMsgError"; // "color:OrangeRed;font-size:14px;";
					break;
				case SUCCESS:
					this.style = "iceMsgInfo"; // "color:LightGreen;font-size:14px;";
					break;
				default:
					break;
			}
		}
	}
	
	public enum StyledMsgTypeEnum {
		INSTRUCTIONS, SUCCESS, ERROR
	}
	
	private static final Logger LOG = Logger.getLogger("org.bibalex.xsa");
	private StyledMessage headerMsg = null;
	
	public XSARequestBackingBean() {
		this.headerMsg = new StyledMessage();
	}
	
	public StyledMessage getHeaderMsg() {
		return this.headerMsg;
	}
	
	public boolean getHeaderMsgIsEmpty() {
		return ((this.headerMsg.text == null) || this.headerMsg.text.isEmpty());
	}
	
	/**
	 * @return the messageStyle
	 */
	public String getHeaderMsgStyle() {
		return this.headerMsg.style;
	}
	
	/**
	 * @return the messageText
	 */
	public String getHeaderMsgText() {
		return this.headerMsg.text;
	}
	
	/**
	 * This method is used to handle any exception, it uses {@link #reportUserError(BibalexException)} 
	 * to preview the error to user 
	 * @param e
	 * @throws RuntimeException
	 */
	public void handleException(Exception e) throws RuntimeException {
		
		if (e instanceof RuntimeException) {
			LOG.fatal(e, e);
			throw (RuntimeException) e;
			
		} else if (e instanceof BibalexException) {
			if (e.getClass().isAnnotationPresent(Correctable.class)) {
				this.reportUserError((BibalexException) e);
				LOG.info(e.getMessage());
			} else {
				LOG.warn(e.getMessage());
			}
		} else {
			LOG.error(e, e);
		}
		
	}
	
	/**
	 * Previews the an error message if an exception happens
	 * @param e the exception happened
	 */
	protected void reportUserError(BibalexException e) {
		if (!e.getClass().isAnnotationPresent(Correctable.class)) {
			return;
		}
		// TODONOT better escape utils
		String msg = e.getMessage();
		msg = msg.replaceAll("<", "&lt;");
		msg = msg.replaceAll(">", "&gt;");
		
		this.setHeaderMsg(msg, StyledMsgTypeEnum.ERROR);
		
	}
	
	public void setHeaderMsg(String message, StyledMsgTypeEnum msgType) {
		this.headerMsg.setMsg(message, msgType);
	}
	
	public void setHeaderMsgIsEmpty(boolean b) {
		// Just to avoid exceptions caused by that JSF binding is two ways
	}
}
