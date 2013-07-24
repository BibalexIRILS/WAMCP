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

package org.bibalex.wamcp.dao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.userdetails.UserDetails;

public class WAMCPUser implements Cloneable, UserDetails {
	
	public static final String ROLE_AUTHENTICATED = "ROLE_AUTHENTICATED";
	
	private String username;
	private String password;
	private boolean enabled;
	
	private List<String /* WAMCPRoles */> roles;
	
	// State variable.. not part of value
	private boolean markedForDeletion = false;
	
	public WAMCPUser() {
		this.roles = new ArrayList<String /* WAMCPRoles */>();
		
		this.username = "";
		this.password = "";
		
		this.enabled = false;
		this.markedForDeletion = false;
		
	}
	
	@Override
	public Object clone() {
		WAMCPUser result = null;
		try {
			result = (WAMCPUser) super.clone();
		} catch (CloneNotSupportedException e) {
			// never happens
			e.printStackTrace();
		}
		result.roles = new ArrayList<String /* WAMCPRoles */>();
		result.roles.addAll(this.roles);
		return result;
	}
	
	@Override
	public boolean equals(Object arg0) {
		
		boolean result = super.equals(arg0);
		
		if (result && (arg0 instanceof WAMCPUser)) {
			WAMCPUser other = (WAMCPUser) arg0;
			
			result = result && this.username.equals(other.username);
			result = result && this.password.equals(other.password);
			result = result && (this.enabled == other.enabled);
			
			if (result) {
				
				for (int i = 0; i < this.roles.size(); ++i) {
					String /* WAMCPRoles */thisRole = this.roles.get(i);
					String /* WAMCPRoles */otherRole = other.roles.get(i);
					result = result && thisRole.equals(otherRole);
					
					if (!result) {
						break;
					}
				}
				
			}
		}
		return result;
	}
	
	@Override
	public GrantedAuthority[] getAuthorities() {
		GrantedAuthority[] result = new GrantedAuthorityImpl[this.roles.size()];
		for (int i = 0; i < this.roles.size(); ++i) {
			String /* WAMCPRoles */oneRole = this.roles.get(i);
			result[i] = new GrantedAuthorityImpl(oneRole.toString());
		}
		return result;
	}
	
	/**
	 * @return the enabled
	 */
	public boolean getEnabled() {
		return this.enabled;
	}
	
	public int getEnabledAsInt() {
		return this.enabled ? 1 : 0;
	}
	
	public Integer getEnabledAsInteger() {
		return new Integer(this.getEnabledAsInt());
	}
	
	/**
	 * @return the password
	 */
	@Override
	public String getPassword() {
		return this.password;
	}
	
	/**
	 * @return the roles
	 */
	public List<String /* WAMCPRoles */> getRoles() {
		return this.roles;
	}
	
	/**
	 * @return the username
	 */
	@Override
	public String getUsername() {
		return this.username;
	}
	
	@Override
	public int hashCode() {
		return this.username != null ? this.username.hashCode() : "BLANK".hashCode();
	}
	
	@Override
	public boolean isAccountNonExpired() {
		// TODONOT Auto-generated method stub
		return true;
	}
	
	@Override
	public boolean isAccountNonLocked() {
		// TODONOT Auto-generated method stub
		return true;
	}
	
	@Override
	public boolean isCredentialsNonExpired() {
		// TODONOT Auto-generated method stub
		return true;
	}
	
	@Override
	public boolean isEnabled() {
		
		return this.getEnabled();
	}
	
	/**
	 * @return the markedForDeletion
	 */
	public boolean isMarkedForDeletion() {
		return this.markedForDeletion;
	}
	
	/**
	 * @param enabled
	 *            the enabled to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	/**
	 * @param enabled
	 *            the enabled to set
	 */
	public void setEnabled(int enabled) {
		this.enabled = enabled == 1;
	}
	
	/**
	 * @param enabled
	 *            the enabled to set
	 */
	public void setEnabled(Integer enabled) {
		this.setEnabled(enabled.intValue());
	}
	
	/**
	 * @param markedForDeletion
	 *            the markedForDeletion to set
	 */
	public void setMarkedForDeletion(boolean markedForDeletion) {
		this.markedForDeletion = markedForDeletion;
	}
	
	/**
	 * @param password
	 *            the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	
	/**
	 * @param roles
	 *            the roles to set
	 */
	public void setRoles(List<String /* WAMCPRoles */> roles) {
		this.roles = roles;
	}
	
	/**
	 * @param username
	 *            the username to set
	 */
	public void setUsername(String username) {
		this.username = username.toLowerCase();
	}
}
