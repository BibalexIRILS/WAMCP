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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.bibalex.wamcp.dao.WAMCPUser;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.security.providers.dao.SaltSource;
import org.springframework.security.providers.encoding.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

public class UsersAdminBBean extends UsersDBAccessingBBean {
// public static class WAMCPRolesConverter implements Converter {
//		
// @Override
// public Object getAsObject(FacesContext ctx, UIComponent comp, String str) {
// return WAMCPRoles.valueOf(str);
// }
//		
// @Override
// public String getAsString(FacesContext ctx, UIComponent comp, Object obj) {
// return ((WAMCPRoles) obj).toString();
// }
//		
// }
	
// public static final WAMCPRolesConverter rolesConterver = new WAMCPRolesConverter();
	public final SelectItem[] rolesItems = populateRolesItems();
	
	private static String DEFAULT_NEW_USER_PASSWORD;// = "password";
	
	private static SelectItem[] populateRolesItems() {
		try {
			ServletContext servletContext = (ServletContext) FacesContext
					.getCurrentInstance().getExternalContext().getContext();
			String rolesFilePath = servletContext.getRealPath("WEB-INF/WAMCPRoles.conf");
			ArrayList<SelectItem> result = new ArrayList<SelectItem>();
			// Open the file that is the first
			// command line parameter
			FileInputStream fstream = new FileInputStream(rolesFilePath);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			try {
				
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String strLine;
				// Read File Line By Line
				while ((strLine = br.readLine()) != null) {
					if ((strLine != null) && !strLine.isEmpty()) {
						strLine = strLine.trim();
						result.add(new SelectItem(strLine, strLine));
					}
				}
				
			} finally {
				// Close the input stream
				in.close();
				
			}
			
			// Stupid work around
			SelectItem[] retVal = new SelectItem[result.size()];
			int i = 0;
			for (SelectItem roleSI : result) {
				retVal[i++] = roleSI;
			}
			
			return retVal;
		} catch (IOException e) {// Catch exception if any
			throw new RuntimeException("Exception while populate roles from configuration file.", e);
		}
	}
	
// private static SelectItem[] populateRolesItems() {
// WAMCPRoles[] rolesArr = WAMCPRoles.values();
// SelectItem[] result = new SelectItem[rolesArr.length];
// for (int i = 0; i < rolesArr.length; ++i) {
// WAMCPRoles role = rolesArr[i];
// result[i] = new SelectItem(role, role.toString());
// }
// return result;
// }
	
	private List<WAMCPUser> users = null;
	
	private List<WAMCPUser> usersOrig = null;
	
	private WAMCPUser userNew = null;
	
	private HashMap<String, ArrayList</* WAMCPRoles */String>> userRolesMap;
	
	public UsersAdminBBean() {
		super();
	//	DEFAULT_NEW_USER_PASSWORD = newPass;
		this.userNew = new WAMCPUser();
		setResetPassword();
	}
	
	public UsersAdminBBean(DataSource dataSource, PasswordEncoder passwordEncoder,
			SaltSource saltSource) {
		
		super(dataSource, passwordEncoder, saltSource);
	//	DEFAULT_NEW_USER_PASSWORD = newPass;
		this.userNew = new WAMCPUser();
		setResetPassword();
	}
	
	@Transactional
	public void addUser(ActionEvent ev) {
		if ((this.userNew.getUsername() != null) && !this.userNew.getUsername().isEmpty()) {
			String hashedPass = this.hashPassword(this.userNew, DEFAULT_NEW_USER_PASSWORD);
			this.usersJdbc.update(SQL_INSERT_USER, new Object[] { this.userNew.getUsername(),
					hashedPass, this.userNew.getEnabledAsInteger() });
			this.insertRoles(this.userNew);
			
			this.userNew = new WAMCPUser();
			this.populateUsers();
		} else {
			FacesContext.getCurrentInstance().addMessage(
					null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"User name cannot be empty", null));
			
		}
		
	}
	
// /**
// * @return the rolesconterver
// */
// public WAMCPRolesConverter getRolesConterver() {
// return rolesConterver;
// }
	
	/**
	 * @return the rolesitems
	 */
	public SelectItem[] getRolesItems() {
		return this.rolesItems;
	}
	
	/**
	 * @return the userNew
	 */
	public WAMCPUser getUserNew() {
		return this.userNew;
	}
	
	public List<WAMCPUser> getUsers() {
		// TODO: can this be called later after an event (addUser, Save,..etc) is processed..
		// because after the event we will call populate users anyway
		if (this.users == null) {
			this.populateUsers();
		}
		return this.users;
	}
	
	@Transactional
	private void insertRoles(WAMCPUser user) {
		// The AUTHENTICATED psuedoRole
		this.usersJdbc.update(SQL_INSERT_AUTHORITY, new Object[] { user.getUsername(),
				WAMCPUser.ROLE_AUTHENTICATED });
		
		// And the rest of the Roles
		for (/* WAMCPRoles */String role : user.getRoles()) {
			this.usersJdbc.update(SQL_INSERT_AUTHORITY, new Object[] { user.getUsername(),
					role.toString() });
		}
	}
	
	@Transactional
	public void persist() {
		for (int i = 0; i < this.users.size(); ++i) {
			WAMCPUser user = this.users.get(i);
			WAMCPUser userOrig = this.usersOrig.get(i);
			
			if (user.isMarkedForDeletion()) {
				this.usersJdbc.update(SQL_DELETE_USER, new Object[] { user.getUsername() });
				this.usersJdbc.update(SQL_DELETE_AUTHORITIES, new Object[] { user.getUsername() });
			} else if (!user.equals(userOrig)) {
				this.usersJdbc.update(SQL_UPDATE_USER_ENABLED,
						new Object[] { user.getEnabledAsInteger(),
						user.getUsername() });
				this.usersJdbc.update(SQL_DELETE_AUTHORITIES, new Object[] { user.getUsername() });
				this.insertRoles(user);
			}
		}
		
	}
	
	protected void populateUsers() {
		this.userRolesMap = new HashMap<String, ArrayList</* WAMCPRoles */String>>();
		try {
			ParameterizedRowMapper<ArrayList</* WAMCPRoles */String>> rolesMapper = new ParameterizedRowMapper<ArrayList</* WAMCPRoles */String>>() {
				
				@Override
				public ArrayList</* WAMCPRoles */String> mapRow(ResultSet rs, int rowNum)
						throws SQLException {
					ArrayList</* WAMCPRoles */String> result;
					
					String username = rs.getString("username");
					
					result = UsersAdminBBean.this.userRolesMap.get(username);
					if (result == null) {
						result = new ArrayList</* WAMCPRoles */String>();
						UsersAdminBBean.this.userRolesMap.put(username, result);
					}
					
					String authority = rs.getString("authority");
					if (WAMCPUser.ROLE_AUTHENTICATED.equals(authority)) {
						// skip
					} else {
						result.add(authority); // WAMCPRoles.valueOf(authority));
					}
					
					return result;
				}
			};
			this.usersJdbc.query(SQL_SELECT_ALL_AUTHORITIES, rolesMapper);
			
			ParameterizedRowMapper<WAMCPUser> userMapper = new ParameterizedRowMapper<WAMCPUser>() {
				
				// notice the return type with respect to Java 5 covariant return types
				public WAMCPUser mapRow(ResultSet rs, int rowNum) throws SQLException {
					WAMCPUser result = new WAMCPUser();
					String username = rs.getString("username");
					result.setUsername(username);
					result.setEnabled(rs.getInt("enabled"));
					ArrayList</* WAMCPRoles */String> userRoles = UsersAdminBBean.this.userRolesMap
							.get(username);
					if (userRoles != null) {
						result.getRoles().addAll(userRoles);
					}
					return result;
				}
			};
			
			this.users = this.usersJdbc.query(SQL_SELECT_ALL_USERS, userMapper);
			
			this.usersOrig = new ArrayList<WAMCPUser>();
			for (WAMCPUser user : this.users) {
				this.usersOrig.add((WAMCPUser) user.clone());
			}
		} finally {
			this.userRolesMap = null;
		}
	}
	
	public void refresh(ActionEvent ev) {
		this.populateUsers();
	}
	
	@Transactional
	public void resetPassword(ActionEvent ev) {
		String username = (String) ev.getComponent().getAttributes().get("PARAM_USERNAME");
		WAMCPUser userDetail = null;
		
		for (WAMCPUser userInList : this.users) {
			if (username.equals(userInList.getUsername())) {
				userDetail = userInList;
				break;
			}
		}
		
		if (userDetail != null) { // just a guard, because it sure will be
	//		System.out.println("resetPassword from pagebeans/UsersDBAccessingBBean.java: "+resetPassword);
			this.usersJdbc.update(SQL_UPDATE_USER_PASSWORD,
					new Object[] { this.hashPassword(userDetail, DEFAULT_NEW_USER_PASSWORD),
					username });
			
		}
	}
	
	public void save(ActionEvent ev) {
		this.persist();
		this.populateUsers();
	}
	
	private void setResetPassword() throws RuntimeException{
		try {
			ServletContext servletContext = (ServletContext) FacesContext
					.getCurrentInstance().getExternalContext().getContext();
			String passFilePath = servletContext.getRealPath("WEB-INF/Password.conf");
			
			// Open the file that is the first
			// command line parameter
			FileInputStream fstream = new FileInputStream(passFilePath);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			try {				
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String strLine;
				// Read File Line By Line
				while ((strLine = br.readLine()) != null) {
					if ((strLine != null) && !strLine.isEmpty()) {
						strLine = strLine.trim();
						DEFAULT_NEW_USER_PASSWORD = strLine;
					}
				}
			} finally {
				// Close the input stream
				in.close();
			}			
		} catch (IOException e) {// Catch exception if any
			System.out.println("Password.conf can't be openned.");
		}
		
	}
}
