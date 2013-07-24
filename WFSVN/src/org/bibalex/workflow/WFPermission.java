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

import org.springframework.security.acls.domain.BasePermission;

public class WFPermission extends BasePermission {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2578680814148213173L;
	
	public final static WFPermission WFACL_UNTERMINATE = new WFPermission(
			1 << 5, 'u', "WFACL_UNTERMINATE");
	/**
	 * Registers the public static permissions defined on this class. This
	 * is
	 * mandatory so that the static methods will operate correctly. (copied
	 * from
	 * super class)
	 */
	static {
		registerPermissionsFor(WFPermission.class);
	}
	
	private final String voterProvoker;
	
	public WFPermission(int mask, char code, String voterProvoker) {
		super(mask, code);
		this.voterProvoker = voterProvoker;
	}
	
	public String getVoterProvoker() {
		return this.voterProvoker;
	}
	
}
