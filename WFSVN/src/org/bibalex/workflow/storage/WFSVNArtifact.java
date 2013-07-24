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

import java.io.File;

import org.bibalex.workflow.WFArtifact;
import org.bibalex.workflow.WFProcess;
import org.springframework.dao.EmptyResultDataAccessException;

public final class WFSVNArtifact extends WFArtifact {
	
	public static WFSVNArtifact factory(WFProcess proc, String artifactName) {
		WFSVNArtifact result = null;
		long oid = -1;
		try {
			
			oid = proc.getArtifactOid(artifactName);
			result = new WFSVNArtifact(oid, artifactName);
			
		} catch (EmptyResultDataAccessException e) {
			// Not found.. create it!
			proc.putArtifactWithName(artifactName);
			result = factory(proc, artifactName);
		}
		return result;
	}
	
	File workingFile;
	
	File workingDir;
	
	protected WFSVNArtifact(long id, String artifactName) {
		super(id, artifactName);
		
	}
	
	public File getWorkingDir() {
		return this.workingDir;
	}
	
	public File getWorkingFile() {
		return this.workingFile;
	}
	
}
