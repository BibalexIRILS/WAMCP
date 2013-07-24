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

package org.bibalex.sdxe.xsa.application;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.bibalex.util.WebAppConstants;

/**
 * For being totally DRY, I wanted to use constants for bean names and even map keys. However, this didn't help because
 * I ended up copying and pasting the contant names. The problem is the lack of IDE support.
 * 
 * @author Younos.Naga
 * 
 */
public class XSAContextListener implements ServletContextListener {
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		
	}
	
	@Override
	public void contextInitialized(ServletContextEvent ev) {
		ServletContext context = ev.getServletContext();
		
		// For use by EL
		context.setAttribute(XSAConstants.class.getSimpleName(),
				WebAppConstants.getNameToValueMap(XSAConstants.class));
	}
	
}
