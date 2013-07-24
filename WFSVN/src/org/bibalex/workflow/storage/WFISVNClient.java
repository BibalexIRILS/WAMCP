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
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.bibalex.workflow.IWFArtifact;
import org.bibalex.workflow.WFProcess;
import org.bibalex.workflow.storage.WFSVNClient.WFSVNEntry;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.Authentication;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;

public interface WFISVNClient {
	public abstract void add(SVNFileCreationProtectionArtifact creationProtectionArtifact,
			WFSVNArtifact artifact) throws WFSVNException, IOException;
	
	/**
	 * @return
	 * @throws SVNException
	 * @throws IOException
	 * @throws WFSVNException
	 * @deprecated this could break the workflow if used in a wrong way.. so use it only when really necessary
	 */
	@Deprecated
	public abstract SVNCommitInfo commitFileFromUserDir(String localFileName, String commitMessage)
			throws SVNException, IOException, WFSVNException;
	
	// @Secured("ROLE_ADMIN,DELETE")
	public abstract SVNCommitInfo delete(WFSVNArtifact artifact, String commitMessage)
			throws WFSVNException, SVNException, IOException;
	
	public abstract List<IWFArtifact> getArtifactsDeleted();
	
	public abstract String getFileSvnUrlStr(WFSVNArtifact artifact) throws SVNException;
	
	public abstract File getUserDir();
	
	public abstract WFProcess getWfProcess();
	
	public abstract boolean isInit();
	
	public abstract boolean manuallyCheckPermission(String artifactName, String wfStage,
			Authentication auth);
	
	public abstract void openRead(WFSVNArtifact artifact) throws SVNException, IOException,
			WFSVNException,
			DataIntegrityViolationException;
	
	public abstract void openWrite(WFSVNArtifact artifact) throws SVNException, IOException,
			WFSVNException,
			DataIntegrityViolationException;
	
	// DIDN'T WORK!
// public abstract void permitDelete(String fileName) throws WFSVNException;
// @Secured("ROLE_DELETER")
	public abstract void requestDelete(String fileName) throws WFSVNException;
	
	public abstract void setSvnPassword(String svnPassword);
	
	public abstract void setSvnUsername(String svnUsername);
	
	public abstract void setURL_SVN_ROOT(String uRLSVNROOT);
	
	public abstract void setWfProcess(WFProcess wfProcess);
	
	public abstract WFSVNArtifact svnAdd(String artifactName) throws SVNException,
			WFSVNException, IOException;
	
	public abstract boolean svnClose(WFSVNArtifact artifact, boolean forceUnlock)
			throws SVNException,
			WFSVNArtifactLoadingException;
	
	public abstract SVNCommitInfo svnCommit(WFSVNArtifact artifact, String commitMessage)
			throws SVNException, WFSVNException;
	
	public abstract SVNCommitInfo svnDelete(String filename, String commitMessage)
			throws WFSVNException,
			SVNException, IOException;
	
	public abstract Collection<WFSVNEntry> svnGetEntries() throws SVNException;
	
	public abstract WFSVNArtifact svnOpenRead(String filename) throws SVNException, IOException,
			WFSVNException;
	
	public abstract WFSVNArtifact svnOpenWrite(String filename) throws SVNException, IOException,
			WFSVNException;
	
	public abstract void svnRevert(WFSVNArtifact artifact) throws SVNException,
			WFSVNArtifactLoadingException;
	
	public abstract void svnWCInit(String appUserName) throws SVNException;
	
	public abstract void uninit();
	
	public abstract void unrequestDelete(String fileName) throws WFSVNException;
	
	/**
	 * @deprecated this could break the workflow if used in a wrong way.. so use it only when really necessary
	 * @param fileSvnUrlStrRel
	 * @param localFileName
	 * @throws SVNException
	 * @throws IOException
	 */
	@Deprecated
	public abstract void updateFileInUserWorkingDir(String fileName)
			throws SVNException, IOException;
	
}