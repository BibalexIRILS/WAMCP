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

import org.springframework.security.acls.Acl;
import org.springframework.security.acls.Permission;
import org.springframework.security.acls.objectidentity.ObjectIdentity;
import org.springframework.security.acls.sid.Sid;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface ISpringSecurityAclUtils {
	@Transactional(propagation = Propagation.REQUIRED)
	public void addPermission(IWFArtifact securedObject, Sid recipient,
			Permission permission, Class clazz);
	
	@Transactional(propagation = Propagation.REQUIRED)
	void addPermission(ObjectIdentity oid, Sid recipient, Permission permission);
	
	@Transactional(propagation = Propagation.REQUIRED)
	void deleteAcl(IWFArtifact securedObject, Class clazz);
	
	@Transactional(propagation = Propagation.REQUIRED)
	void deleteAcl(ObjectIdentity oid);
	
	@Transactional(propagation = Propagation.REQUIRED)
	void deletePermission(IWFArtifact securedObject, Sid recipient, Permission permission,
			Class clazz);
	
	@Transactional(propagation = Propagation.REQUIRED)
	void deletePermission(ObjectIdentity oid, Sid recipient, Permission permission);
	
	@Transactional(propagation = Propagation.REQUIRED)
	void deletePermissions(IWFArtifact securedObject, Sid recipient, Class clazz);
	
	@Transactional(propagation = Propagation.REQUIRED)
	void deletePermissions(ObjectIdentity oid, Sid recipient);
	
	@Transactional(propagation = Propagation.REQUIRED)
	Acl readPermissions(IWFArtifact securedObject, Sid recipient, Class clazz);
	
	@Transactional(propagation = Propagation.REQUIRED)
	Acl readPermissions(ObjectIdentity oid);
	
	@Transactional(propagation = Propagation.REQUIRED)
	Acl readPermissions(ObjectIdentity oid, Sid recipient);
}
