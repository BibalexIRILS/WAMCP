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

package org.bibalex.wamcp.application;

import java.io.Serializable;

import javax.faces.context.FacesContext;

import org.bibalex.sdxe.xsa.application.XSASessionBBean;
import org.bibalex.sdxe.xsa.application.XSAUiActionListener;
import org.bibalex.wamcp.application.WAMCPUiActionListener.AbstractConfirmListener;
import org.bibalex.workflow.WFProcess;
import org.springframework.security.Authentication;
import org.springframework.security.context.HttpSessionContextIntegrationFilter;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.userdetails.UserDetails;

public class WAMCPSessionBackingBean extends XSASessionBBean implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5361048244696519177L;
	
	private XSAUiActionListener uiActionListener = null;
	
	private WFProcess wfProcess = null;
	
	private boolean inEditSession = false;
	
	private AbstractConfirmListener confirmListener = null;
	
	public WAMCPSessionBackingBean() {
		
	}
	
	public Authentication getAuth() {
		// This thread local variable is not correctly maintained.. and returns
		// null auth
// SecurityContext sc = SecurityContextHolder.getContext();
		SecurityContext sc = (SecurityContext) FacesContext
				.getCurrentInstance()
				.getExternalContext()
				.getSessionMap()
				.get(
						HttpSessionContextIntegrationFilter.SPRING_SECURITY_CONTEXT_KEY);
		Authentication auth = sc.getAuthentication();
		return auth;
		
	}
	
	/**
	 * @return the confirmListener
	 */
	public AbstractConfirmListener getConfirmListener() {
		return this.confirmListener;
	}
	
	public UserDetails getLoggedInUser() {
		Authentication auth = this.getAuth();
		if (auth.getPrincipal() instanceof UserDetails) {
			return (UserDetails) auth.getPrincipal();
		} else {
			return null;
		}
		
	}
	
	public String getUserName() {
		Authentication auth = this.getAuth();
		if (auth.getPrincipal() instanceof UserDetails) {
			return ((UserDetails) auth.getPrincipal()).getUsername();
		} else {
			return auth.getPrincipal().toString();
		}
		
	}
	
	public WFProcess getWfProcess() {
		return this.wfProcess;
	}
	
	/**
	 * @return the inEditSession
	 */
	public boolean isInEditSession() {
		return this.inEditSession;
	}
	
	/**
	 * @param confirmListener
	 *            the confirmListener to set
	 */
	public void setConfirmListener(AbstractConfirmListener confirmListener) {
		this.confirmListener = confirmListener;
	}
	
	/**
	 * @param inEditSession
	 *            the inEditSession to set
	 */
	public void setInEditSession(boolean inEditSession) {
		this.inEditSession = inEditSession;
	}
	
	/**
	 * @param uiActionListener
	 *            the uiActionListener to set
	 */
	public void setUiActionListener(XSAUiActionListener uiActionListener) {
		this.uiActionListener = uiActionListener;
	}
	
	public void setWfProcess(WFProcess wfProcess) {
		this.wfProcess = wfProcess;
	}
}
