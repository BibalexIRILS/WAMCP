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

import org.bibalex.util.SpringSecurityUtils;
import org.springframework.security.acls.AccessControlEntry;
import org.springframework.security.acls.Acl;
import org.springframework.security.acls.AlreadyExistsException;
import org.springframework.security.acls.MutableAcl;
import org.springframework.security.acls.MutableAclService;
import org.springframework.security.acls.Permission;
import org.springframework.security.acls.objectidentity.ObjectIdentity;
import org.springframework.security.acls.objectidentity.ObjectIdentityImpl;
import org.springframework.security.acls.sid.Sid;

public class SpringSecurityAclUtils implements ISpringSecurityAclUtils {
	private MutableAclService mutableAclService;
	
	@Override
	public void addPermission(IWFArtifact securedObject, Sid recipient, Permission permission,
			Class clazz) {
		ObjectIdentity oid = new ObjectIdentityImpl(clazz/* .getCanonicalName() */, securedObject
				.getId());
		this.addPermission(oid, recipient, permission);
	}
	
	@Override
	public void addPermission(ObjectIdentity oid, Sid recipient, Permission permission) {
		SpringSecurityUtils.assureThreadLocalAuthSet();
		
		MutableAcl acl;
		
		try {
			acl = this.mutableAclService.createAcl(oid);
		} catch (AlreadyExistsException e) {
			acl = (MutableAcl) this.mutableAclService.readAclById(oid);
		}
		
		acl.insertAce(acl.getEntries().length, permission, recipient, true);
		this.mutableAclService.updateAcl(acl);
		
	}
	
	@Override
	public void deleteAcl(IWFArtifact securedObject, Class clazz) {
		SpringSecurityUtils.assureThreadLocalAuthSet();
		
		ObjectIdentity oid = new ObjectIdentityImpl(clazz/* .getCanonicalName() */, securedObject
				.getId());
		
		this.deleteAcl(oid);
	}
	
	@Override
	public void deleteAcl(ObjectIdentity oid) {
		SpringSecurityUtils.assureThreadLocalAuthSet();
		
		this.mutableAclService.deleteAcl(oid, false);
		
	}
	
	@Override
	public void deletePermission(IWFArtifact securedObject, Sid recipient, Permission permission,
			Class clazz) {
		SpringSecurityUtils.assureThreadLocalAuthSet();
		
		ObjectIdentity oid = new ObjectIdentityImpl(clazz/* .getCanonicalName() */, securedObject
				.getId());
		
		this.deletePermission(oid, recipient, permission);
	}
	
	@Override
	public void deletePermission(ObjectIdentity oid, Sid recipient, Permission permission) {
		SpringSecurityUtils.assureThreadLocalAuthSet();
		
		MutableAcl acl = (MutableAcl) this.mutableAclService.readAclById(oid);
		
		// Remove all permissions associated with this particular recipient (string equality used to keep things simple)
		AccessControlEntry[] entries = acl.getEntries();
		
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].getSid().equals(recipient)) {
				if ((permission == null)
						|| entries[i].getPermission().equals(permission)) {
					acl.deleteAce(i);
				}
			}
		}
		
		this.mutableAclService.updateAcl(acl);
		
	}
	
	@Override
	public void deletePermissions(IWFArtifact securedObject, Sid recipient,
			Class clazz) {
		this.deletePermission(securedObject, recipient, null, clazz);
	}
	
	@Override
	public void deletePermissions(ObjectIdentity oid, Sid recipient) {
		this.deletePermission(oid, recipient, null);
	}
	
	@Override
	public Acl readPermissions(IWFArtifact securedObject, Sid recipient,
			Class clazz) {
		ObjectIdentity oid = new ObjectIdentityImpl(clazz/* .getCanonicalName() */, securedObject
				.getId());
		return this.readPermissions(oid, recipient);
	}
	
	@Override
	public Acl readPermissions(ObjectIdentity oid) {
		SpringSecurityUtils.assureThreadLocalAuthSet();
		
		MutableAcl acl;
		
		return this.mutableAclService.readAclById(oid);
	}
	
	@Override
	public Acl readPermissions(ObjectIdentity oid, Sid recipient) {
		SpringSecurityUtils.assureThreadLocalAuthSet();
		
		MutableAcl acl;
		
		return this.mutableAclService.readAclById(oid, new Sid[] { recipient });
	}
	
	public void setMutableAclService(MutableAclService mutableAclService) {
		this.mutableAclService = mutableAclService;
	}
	
}
