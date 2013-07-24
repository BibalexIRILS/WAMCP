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

package org.bibalex.workflow.storage;

import java.util.List;

import org.bibalex.workflow.ISpringSecurityAclUtils;
import org.bibalex.workflow.IWFArtifact;
import org.bibalex.workflow.WFPermission;
import org.bibalex.workflow.WFStep;
import org.springframework.jdbc.core.JdbcTemplate;

public class WFStepDeleted extends WFStep {
	public static WFStepDeleted singleton = new WFStepDeleted();
	
	private WFStepDeleted() {
		// Auto-generated constructor stub
		this.stepPermission = WFSVNClientPermission.WFACL_UNDELETE;
		this.stepRole = WFSVNClientPermission.WFACL_ROLE_UNDELETER;
	}
	
	@Override
	public void complete(IWFArtifact artifact, String outcome) {
		throw new UnsupportedOperationException();
	}
	
	public void enter(ISpringSecurityAclUtils aclSecurityUtil, IWFArtifact artifact) {
		this.aclSecurityUtil = aclSecurityUtil;
		super.enter(artifact);
	}
	
	@Override
	protected void enter(IWFArtifact artifact) {
		throw new UnsupportedOperationException();
		
	}
	
	@Override
	public List<IWFArtifact> getArtifactsInStep() {
		throw new UnsupportedOperationException();
	}
	
	public List<IWFArtifact> getArtifactsInStep(JdbcTemplate jdbcTempl) {
		this.jdbcTempl = jdbcTempl;
		return super.getArtifactsInStep();
	}
	
	@Override
	public void setStepPermission(WFPermission stepPermission) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setStepRole(String stepRole) {
		throw new UnsupportedOperationException();
	}
	
}
