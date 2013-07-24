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

package org.bibalex.util;

import javax.faces.context.FacesContext;

import org.springframework.security.Authentication;
import org.springframework.security.context.HttpSessionContextIntegrationFilter;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;

public class SpringSecurityUtils {
	
	// For use with IceFaces
	public static void assureThreadLocalAuthSet() {
		SecurityContext sc = SecurityContextHolder.getContext();
		if (sc.getAuthentication() == null) {
			SecurityContext sc2 = (SecurityContext) FacesContext
					.getCurrentInstance()
					.getExternalContext()
					.getSessionMap()
					.get(
							HttpSessionContextIntegrationFilter.SPRING_SECURITY_CONTEXT_KEY);
			
			Authentication auth = sc2.getAuthentication();
			sc.setAuthentication(auth);
		}
		
	}
	
}
