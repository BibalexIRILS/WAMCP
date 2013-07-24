//   Copyright 2013 Bibliotheca Alexandrina, Wellcome Trust Library, Wellcome Trust Library
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

package org.bibalex.wamcp.pagebeans;

import org.apache.log4j.Logger;
import org.bibalex.exception.BibalexException;
import org.bibalex.exception.Correctable;
import org.bibalex.sdxe.xsa.application.XSARequestBackingBean;
import org.springframework.security.AccessDeniedException;

public class WAMCPRequestBBean extends XSARequestBackingBean {
	
	private static final Logger LOG = Logger.getLogger("org.bibalex.wamcp");
	
	public WAMCPRequestBBean() {
		super();
	}
	
	@Override
	public void handleException(Exception e) throws RuntimeException {
		
		if (e instanceof RuntimeException) {
			if (e instanceof AccessDeniedException) {
				this.setHeaderMsg(
						"Access denied, contact the administrator to be granted access",
						StyledMsgTypeEnum.INSTRUCTIONS);
				LOG.info(e.getMessage());
			} else {
				LOG.fatal(e, e);
				throw (RuntimeException) e;
			}
		} else if (e instanceof BibalexException) {
			if (e.getClass().isAnnotationPresent(Correctable.class)) {
				this.reportUserError((BibalexException) e);
				LOG.info(e.getMessage());
			} else {
				LOG.warn(e.getMessage());
			}
		} else {
			LOG.error(e, e);
		}
		
	}
	
}
