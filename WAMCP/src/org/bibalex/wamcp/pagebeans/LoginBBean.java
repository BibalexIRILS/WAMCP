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

package org.bibalex.wamcp.pagebeans;

import java.net.URLEncoder;

import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.springframework.security.ui.AbstractProcessingFilter;

public class LoginBBean extends WAMCPRequestBBean {
	private String username;
	private String password;
	
// private AuthenticationManager springAuthMgr;
	
	/**
	 * @param springAuthMgr
	 */
	public LoginBBean() { // AuthenticationManager springAuthMgr) {
// this.springAuthMgr = springAuthMgr;
		super();
		Exception ex = (Exception) FacesContext
				.getCurrentInstance()
				.getExternalContext()
				.getSessionMap()
				.get(AbstractProcessingFilter.SPRING_SECURITY_LAST_EXCEPTION_KEY);
		
		if (ex != null) {
// Mysteriously stopped working after I changed the way the message is displayed.. no good reason, couldn't fix!
// this.setHeaderMsg(ex.getMessage(), StyledMsgTypeEnum.ERROR);
// So instead use the good old facesMessage
			if(ex.getMessage().equals("Bad credentials"))
			{//YM change message displayed to user
				//TODO YM use authentication Manager
				String message = "Failed to login to Bibalex. Make sure your user name and password are correct.";
				FacesContext.getCurrentInstance().addMessage(
						null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, 
							message, message));
			}
			else
			{
				
			FacesContext.getCurrentInstance().addMessage(
					null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, ex
							.getMessage(), ex.getMessage()));
			}
			FacesContext
					.getCurrentInstance()
					.getExternalContext()
					.getSessionMap()
					.put(AbstractProcessingFilter.SPRING_SECURITY_LAST_EXCEPTION_KEY, null);
		}
		
	}
	
	/**
	 * @return the password
	 */
	public String getPassword() {
		return this.password;
	}
	
// /**
// * @return the springAuthMgr
// */
// public AuthenticationManager getSpringAuthMgr() {
// return this.springAuthMgr;
// }
	
	/**
	 * @return the username
	 */
	public String getUsername() {
		return this.username;
	}
	
	public void login(ActionEvent ev) {
		try {
			ExternalContext context = FacesContext.getCurrentInstance()
					.getExternalContext();
			// FIXME the encoding of special chcarecters in the the password is wrong
			context.redirect("j_spring_security_check?j_username="
					+ URLEncoder.encode(this.getUsername().toLowerCase(), "UTF-8") + "&j_password="
					+ URLEncoder.encode(this.getPassword(), "UTF-8"));
			
		} catch (Exception e) {
			this.handleException(e);
		}
		
	}
	
// /**
// * @param springAuthMgr
// * the springAuthMgr to set
// */
// public void setSpringAuthMgr(AuthenticationManager springAuthMgr) {
// this.springAuthMgr = springAuthMgr;
// }
	
	/**
	 * @param password
	 *            the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	
	/**
	 * @param username
	 *            the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}
	
}
