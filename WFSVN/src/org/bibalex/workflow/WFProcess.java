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

package org.bibalex.workflow;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.bibalex.util.FileUtils;
import org.bibalex.workflow.storage.WFSVNLockedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.objectidentity.ObjectIdentity;
import org.springframework.security.acls.objectidentity.ObjectIdentityImpl;

public class WFProcess implements Serializable {
	private static class MasksToStepsRowMappe implements ParameterizedRowMapper<String> {
		private final HashMap<String, String> map = new HashMap<String, String>();
		private final WFProcess parent;
		
		public MasksToStepsRowMappe(WFProcess parent) {
			this.parent = parent;
		}
		
		@Override
		public String mapRow(ResultSet rs, int rownum) throws SQLException {
			int mask = rs.getInt("MASK");
			String result = this.parent.getStepNameForMask(mask);
			String name = rs.getString("ARTIFACT_NAME");
			this.map.put(name, result);
			return result;
		}
		
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2351089256566833720L;
	
	private static final Logger LOG = Logger.getLogger("org.bibalex.workflow");
	private static final String SQL_SELECT_MASKS_BY_NAMES =
			" SELECT a.`ARTIFACT_NAME`, e.`MASK` FROM `WF_ARTIFACTS` a INNER JOIN (`ACL_ENTRY` e INNER JOIN `acl_object_identity` o ON e.ACL_OBJECT_IDENTITY = o.ID) ON a.OBJECT_ID_IDENTITY = o.OBJECT_ID_IDENTITY WHERE a.`ARTIFACT_NAME` in (#) ";
	
	private static final String SQL_SELECT_ACE_BY_OID =
			"SELECT `MASK` FROM `ACL_ENTRY` e INNER JOIN `acl_object_identity` o ON e.ACL_OBJECT_IDENTITY = o.ID WHERE o.OBJECT_ID_IDENTITY = ? ";
	
	private static final String SQL_SELECT_OID_FROM_ARTIFACTS =
			" SELECT `OBJECT_ID_IDENTITY` FROM `wf_artifacts` "
					+ " WHERE `ARTIFACT_NAME` = ? ";
	
	private static final String SQL_DELETE_FROM_ARTIFACTS =
			" DELETE FROM `wf_artifacts` "
					+ " WHERE `ARTIFACT_NAME` = ? ";
	
	private static final String SQL_INSERT_ARTIFACT = " INSERT INTO wf_artifacts (OBJECT_ID_IDENTITY,ARTIFACT_NAME) VALUES (NULL,?) ";
	
	private WFInitiationStep initiationStep;
	
	private WFTerminationStep terminationStep;
	
	private HashMap<String, WFAbstractStep> steps;
	
	protected final JdbcTemplate jdbcTempl;
	
	private boolean initialized = false;
	
	protected ISpringSecurityAclUtils aclSecurityUtil;
	
	public WFProcess(JdbcMutableAclService aclSvc, DataSource datasource,
			ISpringSecurityAclUtils aclSecurityUtil) {
		super();
		
		this.jdbcTempl = new JdbcTemplate(datasource);
		
		this.initialized = false;
		
		this.aclSecurityUtil = aclSecurityUtil;
	}
	
	protected void configStep(WFAbstractStep step) {
		step.setJdbcTempl(this.jdbcTempl);
		step.setWfProcess(this);
		step.setAclSecurityUtil(this.aclSecurityUtil);
	}
	
	public void deleteArtifact(String artifactName) {
		this.jdbcTempl.update(SQL_DELETE_FROM_ARTIFACTS,
				new Object[] { artifactName });
	}
	
	public ISpringSecurityAclUtils getAclSecurityUtil() {
		return this.aclSecurityUtil;
	}
	
	public long getArtifactOid(String artifactName) throws EmptyResultDataAccessException {
		
		long oidInDB = this.jdbcTempl.queryForLong(SQL_SELECT_OID_FROM_ARTIFACTS,
				new Object[] { artifactName });
		
		return oidInDB;
	}
	
	public WFInitiationStep getInitiationStep() {
		return this.initiationStep;
	}
	
	public JdbcTemplate getJdbcTempl() {
		return this.jdbcTempl;
	}
	
	public WFAbstractStep getStepForArtifact(IWFArtifact artifact) {
		WFAbstractStep result = null;
		
		ObjectIdentity oid = new ObjectIdentityImpl(artifact);
		
		Integer artiMask = (Integer) this.jdbcTempl.queryForObject(
				SQL_SELECT_ACE_BY_OID,
				new Object[] { oid.getIdentifier() }, Integer.class);
		
		result = this.getStepForMask(artiMask.intValue());
		
		return result;
	}
	
	public WFAbstractStep getStepForMask(int mask) {
		return this.steps.get(this.getStepNameForMask(mask));
	}
	
	public WFAbstractStep getStepForName(String name) {
		return this.steps.get(name);
	}
	
	public String getStepName(WFAbstractStep step) {
		String result = null;
		
		for (String key : this.steps.keySet()) {
			if (this.steps.get(key).equals(step)) {
				result = key;
				break;
			}
		}
		
		return result;
	}
	
	public String getStepNameForArtifact(IWFArtifact artifact) {
		WFAbstractStep step = this.getStepForArtifact(artifact);
		return this.getStepName(step);
	}
	
	public String getStepNameForMask(int mask) {
		String result = null;
		
		for (String key : this.steps.keySet()) {
			if (this.steps.get(key).getStepPermission().getMask() == mask) {
				result = key;
				break;
			}
		}
		
		return result;
	}
	
	public HashMap<String, String> getStepNamesForArtifactNames(List<String> names) {
		if (names.size() == 0) {
			return new HashMap<String, String>();
		}
// A single '?' cannot be replaced by a list.. you will need as many ? as list elements
// String inNames = "";
// for (String name : names) {
// inNames += name + ",";
// }
// inNames = inNames.substring(0, inNames.lastIndexOf(','));
		String qMarks = "?";
		for (int i = 1; i < names.size(); ++i) {
			qMarks += ",?";
		}
		
		String query = SQL_SELECT_MASKS_BY_NAMES.replaceAll("#", qMarks);
		
		MasksToStepsRowMappe rowMapper = new MasksToStepsRowMappe(this);
		
		this.jdbcTempl.query(query,
				names.toArray(),
						// new Object[] { inNames },
				rowMapper);
		
		return rowMapper.map;
	}
	
	protected HashMap<String, WFAbstractStep> getSteps() {
		return this.steps;
	}
	
	public WFTerminationStep getTerminationStep() {
		return this.terminationStep;
	}
	
	public void init() {
		if (!this.initialized) {
			
			this.initiationStep.addCreationProtectionAcl();
			
			this.initialized = true;
			
		}
	}
	
	public boolean isInitialized() {
		return this.initialized;
	}
	
// TX cannot be used because the bean is already under control of Spring Security
// Let's just relax the requirement for running in a separate transaction
// @Transactional(propagation = Propagation.REQUIRES_NEW)
	public void lockUrlStr(String releasedMetaGallUrlStr,
			String userName) throws DataIntegrityViolationException, WFSVNLockedException {
		try {
			this.jdbcTempl.update("INSERT INTO WAMCPSTORAGE_LOCKS VALUES(?,?)",
					new Object[] {
							releasedMetaGallUrlStr, userName });
		} catch (DataIntegrityViolationException e) {
			String holder = (String) this.jdbcTempl.queryForObject(
					"SELECT holder FROM wamcpstorage_locks WHERE FILE_URLSTR = ?",
					new Object[] { releasedMetaGallUrlStr }, String.class);
			
			if (!userName.equals(holder)) {
				throw new WFSVNLockedException(FileUtils.removeExtension(releasedMetaGallUrlStr
						.substring(releasedMetaGallUrlStr.lastIndexOf('/') + 1))
						+ " is locked by "
						+ holder);
			}
		}
		
	}
	
	public void putArtifactWithName(String artifactName) throws DataIntegrityViolationException {
		
		this.jdbcTempl.update(SQL_INSERT_ARTIFACT, new Object[] { artifactName });
	}
	
	public void setSteps(HashMap<String, WFAbstractStep> steps) {
		this.steps = steps;
		
		this.initiationStep = (WFInitiationStep) this.steps.get("INIT");
		this.terminationStep = (WFTerminationStep) this.steps.get("DONE");
		
		for (WFAbstractStep wfStep : this.steps.values()) {
			this.configStep(wfStep);
		}
	}
	
	public void uninit() {
		this.initiationStep.removeCreationProtectionAcl();
	}
	
// TX cannot be used because the bean is already under control of Spring Security
// Let's just relax the requirement for running in a separate transaction
// @Transactional(propagation = Propagation.REQUIRES_NEW)
	public void unlockUrlStr(String releasedMetaGallUrlStr,
			String userName) {
		this.jdbcTempl.update(
				"DELETE FROM WAMCPSTORAGE_LOCKS WHERE FILE_URLSTR = ? AND HOLDER = ?",
				new Object[] { releasedMetaGallUrlStr, userName });
	}
}
