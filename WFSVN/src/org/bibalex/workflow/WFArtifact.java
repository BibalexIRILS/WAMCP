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

public class WFArtifact implements IWFArtifact {
	private final long id;
	private final String artifactName;
	
	protected WFArtifact(long id, String artifactName) {
		super();
		this.id = id;
		this.artifactName = artifactName;
	}
	
	@Override
	public String getArtifactName() {
		return this.artifactName;
	}
	
	@Override
	public long getId() {
		return this.id;
	}
	
}
