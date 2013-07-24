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

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.providers.dao.SaltSource;
import org.springframework.security.providers.encoding.PasswordEncoder;
import org.springframework.security.userdetails.UserDetails;

public abstract class UsersDBAccessingBBean extends WAMCPRequestBBean {
	
	protected final static String SQL_SELECT_USER_PASSWORD = " SELECT password FROM  users WHERE username = ? ";
	
	protected static final String SQL_DELETE_USER = "DELETE FROM users WHERE username = ?";
	
	protected static final String SQL_DELETE_AUTHORITIES = "DELETE FROM authorities WHERE username = ?";
	
	protected static final String SQL_SELECT_ALL_AUTHORITIES = "SELECT * FROM authorities";
	
	protected static final String SQL_SELECT_ALL_USERS = "SELECT * FROM users";
	
	protected static final String SQL_UPDATE_USER_ENABLED = " UPDATE users SET enabled = ?  WHERE username = ?";
	
	protected static final String SQL_UPDATE_USER_PASSWORD = " UPDATE users SET password = ?  WHERE username = ?";
	
	protected static final String SQL_INSERT_AUTHORITY = "INSERT INTO authorities VALUES(?,?)";
	
	protected static final String SQL_INSERT_USER = "INSERT INTO users(username, password, enabled) VALUES(?,?,?)";
	
	protected JdbcTemplate usersJdbc = null;
	protected PasswordEncoder passwordEncoder;
	
	protected SaltSource saltSource;
	
//	protected static String resetPassword ;
	
	public UsersDBAccessingBBean() {
		// required for CGLIB
	}
	
	public UsersDBAccessingBBean(DataSource dataSource, PasswordEncoder passwordEncoder,
			SaltSource saltSource) {
		this.usersJdbc = new JdbcTemplate(dataSource);
		
		this.saltSource = saltSource;
		this.passwordEncoder = passwordEncoder;
	}
	
	public PasswordEncoder getPasswordEncoder() {
		return this.passwordEncoder;
	}
	
	public SaltSource getSaltSource() {
		return this.saltSource;
	}
	
	public JdbcTemplate getUsersJdbc() {
		return this.usersJdbc;
	}
	
	protected String hashPassword(UserDetails userDetails, String password) {
		Object salt = this.saltSource.getSalt(userDetails);
		String result = this.passwordEncoder
				.encodePassword(password, salt);
		
		return result;
		
	}
	
	public void setDataSource(DataSource dataSource) {
		this.usersJdbc = new JdbcTemplate(dataSource);
	}
	
	public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}
	
	public void setSaltSource(SaltSource saltSource) {
		this.saltSource = saltSource;
	}
	
}
