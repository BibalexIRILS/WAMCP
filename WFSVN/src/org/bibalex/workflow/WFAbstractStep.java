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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.security.acls.objectidentity.ObjectIdentity;
import org.springframework.security.acls.objectidentity.ObjectIdentityImpl;
import org.springframework.security.acls.sid.GrantedAuthoritySid;

public abstract class WFAbstractStep {
	private static final Logger LOG = Logger.getLogger("org.bibalex.workflow.Step");
	public static final String SQL_SELECT_ARTIFS_BY_MASK =
			"SELECT a.OBJECT_ID_IDENTITY, a.ARTIFACT_NAME  FROM (WF_ARTIFACTS a INNER JOIN `acl_object_identity` o ON a.OBJECT_ID_IDENTITY = o.OBJECT_ID_IDENTITY ) INNER JOIN ACL_ENTRY e  ON e.ACL_OBJECT_IDENTITY = o.ID    WHERE e.MASK = ?";
	
	protected WFPermission stepPermission;
	
	protected String stepRole;
	
	protected HashMap<String, String> nextStepMap;
	
	protected JdbcTemplate jdbcTempl;
	
	protected WFProcess wfProcess;
	
	protected ISpringSecurityAclUtils aclSecurityUtil;
	
	// Doesn't work with CGILib proxy of Spring AOP which needs the default constructor
// ... if using a constructor with args proves necessary we can switch JDK dynamic proxies
// public WFAbstractStep(JdbcMutableAclService aclSvc, DataSource datasource) {
// super();
//
// this.aclSvc = aclSvc;
// this.jdbcTempl = new JdbcTemplate(datasource);
// }
	
	public void complete(IWFArtifact artifact, String outcome) {
		
		// This will be done inside the enter (to assure that it is done)
		// this.aclSecurityUtil.deleteAcl(oid);\
		if (LOG.isDebugEnabled()) {
			LOG.debug("Artifact " + artifact.getArtifactName() + " is completing step "
					+ /* this.wfProcess.getStepName(this) + */"[Name Protected] requiring role "
					+ this.stepRole + " with permission " + this.stepPermission.getMask()
					+ " with outcome " + outcome);
		}
		// This is useless because we are in a proxy.. and this should be from 0 to 10 anyway!
// if (LOG.isTraceEnabled()) {
// StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
// int maxIx = stackTrace.length - 1;
// int minIx = maxIx - 10;
// if (minIx < 0) {
// minIx = 0;
// }
// for (int i = maxIx; i >= minIx; --i) {
// LOG.trace(stackTrace[i].toString());
// }
// }
		HashMap<String, WFAbstractStep> steps = this.wfProcess.getSteps();
		String out =  this.nextStepMap.get(outcome);
		
		WFAbstractStep nextStep = steps.get( out );
		nextStep.enter(artifact);
		
	}
	
	protected void enter(IWFArtifact artifact) {
		
		ObjectIdentity oid = new ObjectIdentityImpl(artifact);
		
		this.aclSecurityUtil.deleteAcl(oid);
		this.aclSecurityUtil.addPermission(oid, new GrantedAuthoritySid(
				this.stepRole), this.stepPermission);
		if (LOG.isDebugEnabled()) {
			LOG.debug("Artifact " + artifact.getArtifactName() + " with oid "
					+ (Long) oid.getIdentifier()
					+ " has entered step "
					+ /* this.wfProcess.getStepName(this) + */"[Name Protected] requiring role "
					+ this.stepRole + " with permission " + this.stepPermission.getMask());
		}
		
	}
	
	public List<IWFArtifact> getArtifactsInStep() {
		List<IWFArtifact> result = new ArrayList<IWFArtifact>();
		
		ParameterizedRowMapper<IWFArtifact> artifactMapper = new ParameterizedRowMapper<IWFArtifact>() {
			
			@Override
			public IWFArtifact mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				long id = rs.getLong("OBJECT_ID_IDENTITY");
				String name = rs.getString("ARTIFACT_NAME");
				IWFArtifact result = new WFArtifact(id, name);
				return result;
			}
		};
		
		// TODO or do we need to multiply by 10 shift times?
		result = this.jdbcTempl.query(SQL_SELECT_ARTIFS_BY_MASK,
				new Object[] { this.stepPermission.getMask() },
				artifactMapper);
		
		return result;
	}
	
	public HashMap<String, String> getNextStepMap() {
		return this.nextStepMap;
	}
	
	public WFPermission getStepPermission() {
		return this.stepPermission;
	}
	
	public String getStepRole() {
		return this.stepRole;
	}
	
	public WFProcess getWfProcess() {
		return this.wfProcess;
	}
	
	public void setAclSecurityUtil(ISpringSecurityAclUtils aclSecurityUtil) {
		this.aclSecurityUtil = aclSecurityUtil;
	}
	
	protected void setJdbcTempl(JdbcTemplate jdbcTempl) {
		this.jdbcTempl = jdbcTempl;
	}
	
	public void setNextStepMap(HashMap<String, String> nextStepMap) {
		this.nextStepMap = nextStepMap;
	}
	
	public void setStepPermission(WFPermission stepPermission) {
		this.stepPermission = stepPermission;
	}
	
	public void setStepRole(String stepRole) {
		this.stepRole = stepRole;
	}
	
	@Required
	protected void setWfProcess(WFProcess wfProcess) {
		this.wfProcess = wfProcess;
	}
}
