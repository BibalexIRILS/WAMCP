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

package org.bibalex.wamcp.exception;

import org.bibalex.exception.BibalexException;

public class WAMCPException extends BibalexException {
	
	public WAMCPException() {
		
	}
	
	public WAMCPException(String arg0) {
		super(arg0);
		
	}
	
	public WAMCPException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		
	}
	
	public WAMCPException(Throwable arg0) {
		super(arg0);
		
	}
	
}
