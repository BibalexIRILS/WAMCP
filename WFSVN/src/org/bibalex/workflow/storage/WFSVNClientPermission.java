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

import org.bibalex.workflow.WFPermission;

public class WFSVNClientPermission extends WFPermission {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1457931046593830315L;
	
	public static final WFSVNClientPermission WFACL_CREATE = new WFSVNClientPermission(1 << 6, 'c',
			"WFACL_CREATE"); // 64
	
	public static final WFSVNClientPermission WFACL_EDIT = new WFSVNClientPermission(1 << 7, 'e',
			"WFACL_EDIT"); // 128
	
	public static final WFSVNClientPermission WFACL_REVIEW1 = new WFSVNClientPermission(1 << 8,
			's',
			"WFACL_REVIEW1"); // 256
	
	public static final WFSVNClientPermission WFACL_REVIEW2 = new WFSVNClientPermission(1 << 11,
			't',
			"WFACL_REVIEW2"); // 2048
	
	public static final String WFACL_ROLE_DELETER = "ROLE_DELETER";
	public static final WFSVNClientPermission WFACL_DELETE = new WFSVNClientPermission(1 << 9, 'd',
			"WFACL_DELETE"); // 512
	
	public static final String WFACL_ROLE_UNDELETER = "ROLE_UNDELETER";
	public static final WFSVNClientPermission WFACL_UNDELETE = new WFSVNClientPermission(1 << 10,
			'U',
			"WFACL_UNDELETE"); // 1024
	
	/**
	 * Registers the public static permissions defined on this class. This is
	 * mandatory so that the static methods will operate correctly. (copied from
	 * super class)
	 */
	static {
		registerPermissionsFor(WFSVNClientPermission.class);
	}
	
	public WFSVNClientPermission(int mask, char code, String voterProvoker) {
		super(mask, code, voterProvoker);
		// Auto-generated constructor stub
	}
	
}
