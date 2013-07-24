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

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.sql.DataSource;

import org.bibalex.util.FacesUtils;
import org.bibalex.wamcp.application.WAMCPSessionBackingBean;
import org.springframework.security.providers.dao.SaltSource;
import org.springframework.security.providers.encoding.PasswordEncoder;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

public class AccountMgmtBBean extends UsersDBAccessingBBean {
	
	private UIComponent compNewPwd;
	private UIComponent compOldPwd;
	
	private UIComponent compNewPwdConfirm;
	
	private String oldPwd;
	
	private String newPwd;
	private String newPwdConfirm;
	
	private WAMCPSessionBackingBean sessionBBean;
	
	public AccountMgmtBBean() {
		// Required for CGLIB
	}
	
	public AccountMgmtBBean(DataSource dataSource, PasswordEncoder passwordEncoder,
			SaltSource saltSource) {
		super(dataSource, passwordEncoder, saltSource);
		
	}
	
	@Transactional
	public void changePassword(ActionEvent ev) {
		// Check preconditions
		FacesContext fCtx = FacesContext.getCurrentInstance();
		if ((this.oldPwd == null) || this.oldPwd.isEmpty()) {
			FacesUtils.ManagerinvFacesUtils.addErrorMessage(this.compOldPwd.getClientId(fCtx),
					"Please enter your current password");
			return;
		} else if ((this.newPwd == null) || this.newPwd.isEmpty()) {
			FacesUtils.ManagerinvFacesUtils.addErrorMessage(this.compNewPwd.getClientId(fCtx),
					"Please enter the new password");
			return;
		} else if ((this.newPwdConfirm == null) || this.newPwdConfirm.isEmpty()) {
			FacesUtils.ManagerinvFacesUtils.addErrorMessage(this.compNewPwdConfirm
					.getClientId(fCtx),
					"Please confirm the new password");
			return;
		} else if (!this.newPwd.equals(this.newPwdConfirm)) {
			FacesUtils.ManagerinvFacesUtils.addErrorMessage(this.compNewPwdConfirm
					.getClientId(fCtx),
					"The new password and its confirmation are not equal");
			return;
		}
		
		UserDetails loggedInUser = this.sessionBBean.getLoggedInUser();
		String username = loggedInUser.getUsername();
		
		String dbPwd = (String) this.usersJdbc.queryForObject(SQL_SELECT_USER_PASSWORD,
				new Object[] { username }, String.class);
		String oldPwdHash = this.hashPassword(loggedInUser, this.oldPwd);
		if (!oldPwdHash.equals(dbPwd)) {
			FacesUtils.ManagerinvFacesUtils.addErrorMessage(this.compOldPwd.getClientId(fCtx),
					"Wrong password");
			return;
		}
		
		// Do change password
		
		this.usersJdbc.update(SQL_UPDATE_USER_PASSWORD,
				new Object[] { this.hashPassword(loggedInUser, this.newPwd),
				username });
		
		// Report success
		this.setHeaderMsg("Password changed successfully", StyledMsgTypeEnum.SUCCESS);
	}
	
	/**
	 * @return the compNewPwd
	 */
	public UIComponent getCompNewPwd() {
		return this.compNewPwd;
	}
	
	/**
	 * @return the compNewPwdConfirm
	 */
	public UIComponent getCompNewPwdConfirm() {
		return this.compNewPwdConfirm;
	}
	
	/**
	 * @return the compOldPwd
	 */
	public UIComponent getCompOldPwd() {
		return this.compOldPwd;
	}
	
	/**
	 * @return the newPwd
	 */
	public String getNewPwd() {
		return this.newPwd;
	}
	
	/**
	 * @return the newPwdConfirm
	 */
	public String getNewPwdConfirm() {
		return this.newPwdConfirm;
	}
	
	/**
	 * @return the oldPwd
	 */
	public String getOldPwd() {
		return this.oldPwd;
	}
	
	/**
	 * @param compNewPwd
	 *            the compNewPwd to set
	 */
	public void setCompNewPwd(UIComponent compNewPwd) {
		this.compNewPwd = compNewPwd;
	}
	
	/**
	 * @param compNewPwdConfirm
	 *            the compNewPwdConfirm to set
	 */
	public void setCompNewPwdConfirm(UIComponent compNewPwdConfirm) {
		this.compNewPwdConfirm = compNewPwdConfirm;
	}
	
	/**
	 * @param compOldPwd
	 *            the compOldPwd to set
	 */
	public void setCompOldPwd(UIComponent compOldPwd) {
		this.compOldPwd = compOldPwd;
	}
	
	/**
	 * @param newPwd
	 *            the newPwd to set
	 */
	public void setNewPwd(String newPwd) {
		this.newPwd = newPwd;
	}
	
	/**
	 * @param newPwdConfirm
	 *            the newPwdConfirm to set
	 */
	public void setNewPwdConfirm(String newPwdConfirm) {
		this.newPwdConfirm = newPwdConfirm;
	}
	
	/**
	 * @param oldPwd
	 *            the oldPwd to set
	 */
	public void setOldPwd(String oldPwd) {
		this.oldPwd = oldPwd;
	}
	
	/**
	 * @param sessionBBean
	 *            the sessionBBean to set
	 */
	public void setSessionBBean(WAMCPSessionBackingBean sessionBBean) {
		this.sessionBBean = sessionBBean;
	}
	
}
