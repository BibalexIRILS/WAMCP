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

import org.springframework.security.acls.NotFoundException;
import org.springframework.security.acls.objectidentity.ObjectIdentity;
import org.springframework.security.acls.objectidentity.ObjectIdentityImpl;
import org.springframework.security.acls.sid.GrantedAuthoritySid;
import org.springframework.security.acls.sid.Sid;

public class WFInitiationStep extends WFAbstractStep {
	
	private Class<? extends WFCreationProtectionArtifact> creationProtectionClass;
	
	protected void addCreationProtectionAcl() {
		ObjectIdentity cid = this.createCreationProtectionOid();
		Sid sid = new GrantedAuthoritySid(this.stepRole);
		
		try {
			// Try to read the acl and make sure that is has only 1 entry
			if (this.aclSecurityUtil.readPermissions(cid, sid).getEntries().length != 1) {
				this.removeCreationProtectionAcl();
				this.addCreationProtectionAcl();
			}
		} catch (NotFoundException e) {
			// If reading the acl fails.. it is not there yet so creat it
			this.aclSecurityUtil.addPermission(cid, sid, this.stepPermission);
		}
		
	}
	
	public ObjectIdentity createCreationProtectionOid() {
		return new ObjectIdentityImpl(
				this.creationProtectionClass/*.getCanonicalName()*/,
				WFCreationProtectionArtifact.WFACL_ARTIFACT_CREATION_FIXED_OID);
	}
	
	@Override
	public void enter(IWFArtifact artifact) {
		throw new UnsupportedOperationException();
	}
	
	public Class<? extends WFCreationProtectionArtifact> getCreationProtectionClass() {
		return this.creationProtectionClass;
	}
	
	protected void removeCreationProtectionAcl() {
		ObjectIdentity cid = this.createCreationProtectionOid();
		this.aclSecurityUtil.deleteAcl(cid);
		
	}
	
	public void setCreationProtectionClass(
			Class<? extends WFCreationProtectionArtifact> creationProtectionClass) {
		this.creationProtectionClass = creationProtectionClass;
	}
	
}
