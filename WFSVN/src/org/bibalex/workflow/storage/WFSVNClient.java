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
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.bibalex.util.FileUtils;
import org.bibalex.util.URLPathStrUtils;
import org.bibalex.workflow.IWFArtifact;
import org.bibalex.workflow.WFAbstractStep;
import org.bibalex.workflow.WFProcess;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.acls.AccessControlEntry;
import org.springframework.security.acls.Acl;
import org.springframework.security.acls.objectidentity.ObjectIdentityImpl;
import org.springframework.security.acls.sid.GrantedAuthoritySid;
import org.springframework.security.acls.sid.PrincipalSid;
import org.springframework.security.acls.sid.Sid;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class WFSVNClient implements WFISVNClient, Serializable {
	public static class WFSVNEntry {
		private String name;
		private Date lastModified;
		private String lockHolder;
		private boolean isDeleteRequested;
		private long revision;
		private String commitMsg;
		
		public String getCommitMsg() {
			return this.commitMsg;
		}
		
		public boolean getIsDeleteRequested() {
			return this.isDeleteRequested;
		}
		
		public Date getLastModified() {
			return this.lastModified;
		}
		
		public String getLockHolder() {
			return this.lockHolder;
		}
		
		public String getName() {
			return this.name;
		}
		
		public long getRevision() {
			return this.revision;
		}
		
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3897736042422346558L;
	
	// circular reference leads to not creating the proxy
	// private WFISVNClient proxiedThis;
	
	private static final Logger LOG = Logger.getLogger("org.bibalex.workflow");
	
	public String URL_SVN_ROOT; // = "http://svn.bibalex.org/WellcomeTrust/WAMCPStorage/";
	
	private static final String WORK_SPACE = File.separator + "Workspace";
	
	private File userDir = null;
	
	private SVNClientManager svnClientManager;
	private ISVNAuthenticationManager authManager;
	
	// TODO store repo as "weak" reference
	private SVNRepository repo;
	
	private WFProcess wfProcess;
	
	private File xmlDir;
	
	private String svnUsername;
	
	private String svnPassword;
	
	private String appUserName = null;
	
	public WFSVNClient() throws SVNException {
		/*
		 * For using over http:// and https://
		 */
		DAVRepositoryFactory.setup();
		/*
		 * For using over svn:// and svn+xxx://
		 */
		SVNRepositoryFactoryImpl.setup();
		
		/*
		 * For using over file:///
		 */
		FSRepositoryFactory.setup();
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.bibalex.workflow.storage.WFISVNClient#add(org.bibalex.workflow.storage.SVNFileCreationProtectionArtifact
	 * , org.bibalex.workflow.storage.WFSVNArtifact)
	 */
	@Override
	public synchronized void add(SVNFileCreationProtectionArtifact creationProtectionArtifact,
			WFSVNArtifact artifact) throws WFSVNException, IOException {
		if (!this.isInit()) {
			throw new WFSVNException("Must initialize working copy first!");
		}
		String repoUrlStr = this.composeDirSvnUrlStr(artifact);
		
		this.wfProcess.lockUrlStr(repoUrlStr, this.appUserName);
		
		try {
			String filename = artifact.getArtifactName();
			artifact.workingDir = new File(this.xmlDir.getCanonicalPath()
					+ File.separator + filename);
			if (!artifact.workingDir.exists()) {
				artifact.workingDir.mkdir();
			} else {
				throw new WFSVNException(filename
						+ " already exists locally");
			}
			
			String newFilePath = artifact.workingDir.getCanonicalPath()
					+ File.separator + filename
					+ ".xml";
			artifact.workingFile = new File(newFilePath);
			if (!artifact.workingFile.exists()) {
				artifact.workingFile.createNewFile();
			} else {
				// Cleanup upon failure
				artifact.workingDir.delete();
				
				throw new WFSVNException(filename
						+ " already exists locally");
			}
			
			// ////////////////// Add to repo (schedule for add) /////////////
			// / Will throw exception if already exists -> guard unique id
			
			try {
				this.svnClientManager.getWCClient().doAdd(
						artifact.workingDir, // path - working copy path
						false, // force - if true, this method does not throw exceptions on already-versioned items
						false, // mkdir - if true, create a directory also at path
						false, // climbUnversionedParents - not used; make use of makeParents instead
						SVNDepth.INFINITY, // depth - tree depth
						false, // includeIgnored - if true, does not apply ignore patterns to paths being added
						true); // makeParents - if true, climb upper and schedule also all unversioned paths in the way
				
			} catch (SVNException e) {
				// Cleanup upon failure
				artifact.workingFile.delete();
				artifact.workingDir.delete();
				
				throw new WFSVNException(artifact.getArtifactName()
						+ " already exists in storage", e);
				
			}
			try {
				this.svnClientManager.getWCClient().doLock(new File[] { artifact.workingDir },
						false,
						this.appUserName);
			} catch (SVNException e) {
				// no problem that we couldn't unlock it.. just log this:
				LOG.warn("SVN locking exception: " + e.getMessage());
			}
		} catch (IOException e) {
			this.wfProcess.unlockUrlStr(repoUrlStr, this.appUserName);
			throw e;
		}
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.workflow.storage.WFISVNClient#commitFileFromUserDir(java.lang.String, java.lang.String)
	 */
	@Override
	@Deprecated
	public SVNCommitInfo commitFileFromUserDir(String localFileName, String commitMessage)
			throws SVNException, IOException, WFSVNException {
		
		SVNCommitInfo result = null;
		String repoUrlStr = URLPathStrUtils.appendParts(this.URL_SVN_ROOT, localFileName);
		
		this.wfProcess.lockUrlStr(repoUrlStr, this.appUserName);
		
		try {
			long revNum = this.svnClientManager.getUpdateClient().doUpdate(
					this.userDir, // path - working copy path
					SVNRevision.HEAD, // revision - revision to update to
					SVNDepth.FILES, // depth - tree depth to update
					true, // allowUnversionedObstructions - flag that allows tollerating unversioned items during update
					false); // depthIsSticky - flag that controls whether the requested depth should be written to the
			// working cop
			
			LOG.info("SVN: " + this.userDir.getCanonicalPath() + " updated to revision " + revNum);
			
			String localFilePath = URLPathStrUtils.appendParts(this.userDir.getCanonicalPath(),
					localFileName);
			
			File localFile = new File(localFilePath);
			
			result = this.svnClientManager.getCommitClient()
					.doCommit(
							new File[] { localFile }, // paths - paths to commit
							true, // keepLocks - whether to unlock or not files in the repository
							commitMessage, // commitMessage - commit log message
							null, // revisionProperties - custom revision properties
							null, // changelists - changelist names array
							false, // keepChangelist - whether to remove changelists or not
							false, // force - true to force a non-recursive commit; if depth is SVNDepth.INFINITY the
// force flag
// is
// ignored
							SVNDepth.FILES); // depth - tree depth to process
			
			SVNErrorMessage errorMessage = result.getErrorMessage();
			if (errorMessage != null) {
				if (errorMessage.isWarning()) {
					LOG.warn(errorMessage.getFullMessage());
				} else {
					LOG.error(errorMessage.getFullMessage());
// throw new WFSVNException(errorMessage.getFullMessage());
				}
			} else {
				LOG.info("SVN: " + localFilePath + " is now at revision "
						+ result.getNewRevision());
			}
		} finally {
			this.wfProcess.unlockUrlStr(repoUrlStr, this.appUserName);
		}
		return result;
		
	}
	
	protected String composeDirSvnUrlStr(WFSVNArtifact artifact) {
		String fileDir = artifact.getArtifactName();
		
		return URLPathStrUtils.appendParts(this.URL_SVN_ROOT,
				"XML", fileDir);
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.workflow.storage.WFISVNClient#delete(org.bibalex.workflow.storage.WFSVNArtifact,
	 * java.lang.String)
	 */
	@Override
	public SVNCommitInfo delete(WFSVNArtifact artifact, String commitMessage)
			throws WFSVNException, SVNException, IOException {
		// FIXME remove this when Spring Security method interception gets back to its senses
		// The stupid Spring Security is just a piece of s**t.. I will do its work.. grrrr!!
		ObjectIdentityImpl oid = new ObjectIdentityImpl(artifact);
		Acl perms = this.wfProcess.getAclSecurityUtil().readPermissions(
				oid,
				new GrantedAuthoritySid("ROLE_ADMIN"));
		
		boolean granted = false;
		for (AccessControlEntry ace : perms.getEntries()) {
			if (WFSVNClientPermission.WFACL_DELETE.equals(ace
					.getPermission())) {
				granted = ace.isGranting();
			}
		}
		
		if (!granted) {
			throw new WFSVNPermissionException("Deletion not requested");
		}
		
		// Now I will do my work!
		
		this.openWrite(artifact);
		SVNCommitInfo result = null;
		try {
			this.svnClientManager.getWCClient().doDelete(artifact.workingDir, true, true, false);
			
			result = this.svnClientManager.getCommitClient().doCommit(
					new File[] { artifact.workingDir }, // paths - paths to commit
					true, // keepLocks - whether to unlock or not files in the repository
					commitMessage, // commitMessage - commit log message
					null, // revisionProperties - custom revision properties
					null, // changelists - changelist names array
					false, // keepChangelist - whether to remove changelists or not
					false, // force - true to force a non-recursive commit; if depth is SVNDepth.INFINITY the force flag
					// is
					// ignored
					SVNDepth.INFINITY); // depth - tree depth to process
			
			SVNErrorMessage errorMessage = result.getErrorMessage();
			if (errorMessage != null) {
				if (errorMessage.isWarning()) {
					LOG.warn(errorMessage.getFullMessage());
				} else {
					LOG.error(errorMessage.getFullMessage());
// throw new WFSVNException(errorMessage.getFullMessage());
				}
			} else {
				// TODO: Move with deleted artifacts
				// But this needs that we create a table for deleted artifacts first
// WFStepDeleted.singleton.enter(this.wfProcess.getAclSecurityUtil(), artifact);
// So instead we jsut delete
				this.wfProcess.getAclSecurityUtil().deleteAcl(oid);
				
				LOG.warn("SVN: " + artifact.getWorkingDir() + " deleted at revision "
						+ result.getNewRevision());
			}
			
		} finally {
			// Delete locks on the now non existent file
			String repoUrlStr = this.composeDirSvnUrlStr(artifact);
			// TODO? Should I move this to the beginning so that the file being deleted
			// must be locked by the deleter
			this.wfProcess.unlockUrlStr(repoUrlStr, this.appUserName);
			try {
				this.svnClientManager.getWCClient()
						.doUnlock(new File[] { artifact.workingDir }, false);
			} catch (SVNException e) {
				// no problem that we couldn't unlock it.. just log this:
				LOG.warn("SVN locking exception: " + e.getMessage());
			}
			
		}
		
		return result;
	}
	
	@Override
	protected void finalize() throws Throwable {
		this.svnClientManager.dispose();
		super.finalize();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.workflow.storage.WFISVNClient#getArtifactsDeleted()
	 */
	@Override
	public List<IWFArtifact> getArtifactsDeleted() {
		
		return WFStepDeleted.singleton.getArtifactsInStep(this.wfProcess.getJdbcTempl());
	}
	
	@Override
	public String getFileSvnUrlStr(WFSVNArtifact artifact) throws SVNException {
		String result = null;
		
		SVNStatus status = this.svnClientManager.getStatusClient().doStatus(artifact.workingFile,
				false);
		
		result = status.getURL().toDecodedString();
		
		return result;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.workflow.storage.WFISVNClient#getUserDir()
	 */
	@Override
	public File getUserDir() {
		return this.userDir;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.workflow.storage.WFISVNClient#getWfProcess()
	 */
	@Override
	public WFProcess getWfProcess() {
		return this.wfProcess;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.workflow.storage.WFISVNClient#isInit()
	 */
	@Override
	public boolean isInit() {
		return this.userDir != null;
	}
	
	private void lazyInitRepo() throws SVNException {
		// For use with getEntries
		
		if (this.repo != null) {
			return;
		}
		
		SVNURL url = SVNURL.parseURIEncoded(URLPathStrUtils.appendParts(this.URL_SVN_ROOT, "XML",
				""));
		
		this.repo = SVNRepositoryFactory.create(url);
		
		this.repo.setAuthenticationManager(this.authManager);
		
	}
	
	@Override
	public boolean manuallyCheckPermission(String artifactName, String wfStage, Authentication auth) {
		boolean result = false;
		WFAbstractStep step = this.wfProcess.getStepForName(wfStage);
		
		WFSVNArtifact target = WFSVNArtifact.factory(this.wfProcess, artifactName);
		
		int mask = step.getStepPermission().getMask();
		
		Acl acl = this.wfProcess.getAclSecurityUtil().readPermissions(
				new ObjectIdentityImpl(target));
		
		for (AccessControlEntry ace : acl.getEntries()) {
			if (ace.getPermission().getMask() == mask) {
				Sid sid = ace.getSid();
				if (sid instanceof PrincipalSid) {
					
					result = ((PrincipalSid) sid).getPrincipal().equals(
							auth.getPrincipal().toString())
							&& ace.isGranting();
					
				} else if (sid instanceof GrantedAuthoritySid) {
					
					for (GrantedAuthority grantedAuth : auth.getAuthorities()) {
						
						String role = grantedAuth.getAuthority();
						if (role.equals(((GrantedAuthoritySid) sid).getGrantedAuthority())) {
							result = ace.isGranting();
						}
						
					}
				}
				
			}
		}
		
		return result;
	}
	
// THIS DIDN'T WORK!
// /*
// * (non-Javadoc)
// *
// * @see org.bibalex.workflow.storage.WFISVNClient#permitDelete(java.lang.String)
// */
// public void permitDelete(String fileName) throws WFSVNException {
// if (!this.wfProcess.isInitialized()) {
// throw new WFSVNException("Must initialize workflow first!");
// }
// WFSVNArtifact delTarget = WFSVNArtifact.factory(this.wfProcess, fileName);
// this.wfProcess.getAclSecurityUtil().addPermission(new ObjectIdentityImpl(delTarget),
// new GrantedAuthoritySid(WFSVNClientPermission.WFACL_ROLE_DELETER),
// WFSVNClientPermission.WFACL_DELETE);
// }
	
	@Override
	public void openRead(WFSVNArtifact artifact) throws SVNException, IOException, WFSVNException {
		String fileDir = artifact.getArtifactName();
		String fileName = artifact.getArtifactName() + ".xml";
		
		artifact.workingDir = new File(this.xmlDir.getCanonicalPath()
				+ File.separator + fileDir);
// this.workingDir.mkdir();
		
		SVNUpdateClient updateClient = this.svnClientManager.getUpdateClient();
		/*
		 * sets externals to be ignored during the checkout
		 */
		updateClient.setIgnoreExternals(true);
		/*
		 * returns the number of the revision at which the working copy is
		 */
		long revNo;
		
		revNo = updateClient.doUpdate(
				artifact.workingDir, // path - working copy path
				SVNRevision.HEAD, // revision - revision to update to
				SVNDepth.INFINITY, // depth - tree depth to update
				true, // allowUnversionedObstructions - flag that allows tollerating unversioned items during update
				false);
		// depthIsSticky - flag that controls whether the requested depth should be written to the
		// working cop
		
		LOG.info("SVN: " + artifact.workingDir + " now contains revision " + revNo);
		
		artifact.workingFile = new File(artifact.workingDir.getCanonicalPath()
				+ File.separator
				+ fileName);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.workflow.storage.WFISVNClient#open(org.bibalex.workflow.storage.WFSVNArtifact)
	 */
	@Override
	public void openWrite(WFSVNArtifact artifact) throws SVNException, IOException, WFSVNException {
		if (!this.isInit()) {
			throw new WFSVNException("Must initialize working copy first!");
		}
		String repoUrlStr = this.composeDirSvnUrlStr(artifact);
		
		this.wfProcess.lockUrlStr(repoUrlStr, this.appUserName);
		
		try {
			this.openRead(artifact);
			try {
				this.svnClientManager.getWCClient().doLock(new File[] { artifact.workingDir },
						false,
						this.appUserName);
			} catch (SVNException e) {
				// no problem that we couldn't unlock it.. just log this:
				LOG.warn("SVN locking exception: " + e.getMessage());
			}
		} catch (SVNException e) {
			this.wfProcess.unlockUrlStr(repoUrlStr, this.appUserName);
			throw e;
		} catch (IOException e) {
			this.wfProcess.unlockUrlStr(repoUrlStr, this.appUserName);
			throw e;
		}
	}
	
	@Override
	public void requestDelete(String fileName) throws WFSVNException {
		if (!this.wfProcess.isInitialized()) {
			throw new WFSVNException("Must initialize workflow first!");
		}
		WFSVNArtifact delTarget = WFSVNArtifact.factory(this.wfProcess, fileName);
		this.wfProcess.getAclSecurityUtil().addPermission(
				new ObjectIdentityImpl(delTarget),
				new GrantedAuthoritySid("ROLE_ADMIN"),
				WFSVNClientPermission.WFACL_DELETE);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.workflow.storage.WFISVNClient#setSvnPassword(java.lang.String)
	 */
	@Override
	public void setSvnPassword(String svnPassword) {
		this.svnPassword = svnPassword;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.workflow.storage.WFISVNClient#setSvnUsername(java.lang.String)
	 */
	@Override
	public void setSvnUsername(String svnUsername) {
		this.svnUsername = svnUsername;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.workflow.storage.WFISVNClient#setURL_SVN_ROOT(java.lang.String)
	 */
	@Override
	public void setURL_SVN_ROOT(String uRLSVNROOT) {
		this.URL_SVN_ROOT = uRLSVNROOT;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.workflow.storage.WFISVNClient#setWfProcess(org.bibalex.workflow.WFProcess)
	 */
	@Override
	public void setWfProcess(WFProcess wfProcess) {
		this.wfProcess = wfProcess;
	}
	
	public void stealLock(String fileName) throws WFSVNException {
		
		// TODO find out how to steal the lock without checking out
		// then implement this
		
// String fileUrlStr = URLPathStrUtils.appendParts(this.URL_SVN_ROOT,
// "XML", fileName);
// try {
// this.svnClientManager.getWCClient()
// .doUnlock(new File[] { artifact.workingDir }, false);
// } catch (SVNException e) {
// // no problem that we couldn't unlock it.. just log this:
// LOG.warn("SVN locking exception: " + e.getMessage());
// }
// String repoUrlStr = this.getArtifactRepoUrlStr(artifact);
// this.wfProcess.unlockUrlStr(repoUrlStr);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.workflow.storage.WFISVNClient#svnAdd(java.lang.String)
	 */
	@Override
	public WFSVNArtifact svnAdd(String artifactName) throws SVNException,
			WFSVNException, IOException {
		
		WFSVNArtifact result = WFSVNArtifact.factory(this.wfProcess, artifactName);
		try {
			SVNFileCreationProtectionArtifact creationProtectionArtifact = new SVNFileCreationProtectionArtifact();
			
			// FIXME: This skips the security check.. refactor to use the proxy.add
			this.add(creationProtectionArtifact,
					result);
			
			this.wfProcess.getInitiationStep().complete(result, "PROCEED");
		} catch (Exception e) {
			if (!(e instanceof DataIntegrityViolationException)) {
				this.wfProcess.deleteArtifact(artifactName);
			}
			throw new WFSVNException(e);
		}
		return result;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.workflow.storage.WFISVNClient#svnClose(org.bibalex.workflow.storage.WFSVNArtifact)
	 */
	@Override
	public boolean svnClose(WFSVNArtifact artifact, boolean forceUnlock) throws SVNException,
			WFSVNArtifactLoadingException {
		
		boolean result = false;
		
		SVNStatus status = this.svnClientManager.getStatusClient().doStatus(
				artifact.workingFile,
				false,
				false);
		
		// YA 20100920 Delete local when:
		// Either force unlock or no local modifications
		// Was !equals(SVNStatusType.STATUS_MODIFIED), but this is not true in many cases;
		// when it is changed by an operation, conflicted, ..etc
		// equals(STATUS_NORMAL) is not very otta as well, but at least it is safe
		if (forceUnlock || status.getContentsStatus().equals(SVNStatusType.STATUS_NORMAL)) {
			try {
				this.svnClientManager.getWCClient()
						.doUnlock(new File[] { artifact.workingDir }, false);
			} catch (SVNException e) {
				// no problem that we couldn't unlock it.. just log this:
				LOG.warn("SVN locking exception: " + e.getMessage());
			}
			String repoUrlStr = this.composeDirSvnUrlStr(artifact);
			
			// delete the lock only if the app user is the locker
			this.wfProcess.unlockUrlStr(repoUrlStr, this.appUserName);
			
			result = FileUtils.deleteDir(artifact.workingDir);
		}
		
		return result;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.workflow.storage.WFISVNClient#svnCommit(org.bibalex.workflow.storage.WFSVNArtifact,
	 * java.lang.String)
	 */
	@Override
	public SVNCommitInfo svnCommit(WFSVNArtifact artifact, String commitMessage)
			throws SVNException, WFSVNException {
		
// SVNStatus status = this.svnClientManager.getStatusClient().doStatus(artifact.workingFile,
// false);
//
// // TODO use Diff instead of status because the auto-addition/deletion feature makes
// // only opening a file enough to make it modified. Unfortunately Diff that is supplied
// // with SVNKit is good only for generating text reports.. useless!
// if (!SVNStatusType.STATUS_MODIFIED.equals(status.getContentsStatus())) {
// // no local modifications.. no need to commit
// LOG.warn("The case of committing non-modified files was not tested before.. if an exception happens report it!");
// return null;
// }
		
		long revNum = this.svnClientManager.getUpdateClient().doUpdate(
				artifact.workingDir, // path - working copy path
				SVNRevision.HEAD, // revision - revision to update to
				SVNDepth.INFINITY, // depth - tree depth to update
				true, // allowUnversionedObstructions - flag that allows tollerating unversioned items during update
				false); // depthIsSticky - flag that controls whether the requested depth should be written to the
		// working cop
		
		LOG.info("SVN: " + artifact.workingDir + " updated to revision " + revNum);
		
		SVNCommitInfo result = this.svnClientManager.getCommitClient()
				.doCommit(
						new File[] { artifact.workingDir }, // paths - paths to commit
						true, // keepLocks - whether to unlock or not files in the repository
						commitMessage, // commitMessage - commit log message
						null, // revisionProperties - custom revision properties
						null, // changelists - changelist names array
						false, // keepChangelist - whether to remove changelists or not
						false, // force - true to force a non-recursive commit; if depth is SVNDepth.INFINITY the force
// flag is
// ignored
						SVNDepth.INFINITY); // depth - tree depth to process
		
		SVNErrorMessage errorMessage = result.getErrorMessage();
		if ((errorMessage != null) && !errorMessage.isWarning()) {
			throw new WFSVNException(errorMessage.getFullMessage());
		}
		
		LOG.info("SVN: " + artifact.workingDir + " is now at revision "
				+ result.getNewRevision());
		
		return result;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.workflow.storage.WFISVNClient#svnDelete(java.lang.String, java.lang.String)
	 */
	@Override
	public SVNCommitInfo svnDelete(String filename, String commitMessage) throws WFSVNException,
			SVNException, IOException {
		if (!this.wfProcess.isInitialized()) {
			throw new WFSVNException("Must initialize workflow first!");
		}
		WFSVNArtifact artifact = WFSVNArtifact.factory(this.wfProcess, filename);
		// FIXME: This skips the security check.. refactor to use the proxy.delete
		SVNCommitInfo result = this.delete(artifact, commitMessage);
		return result;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.workflow.storage.WFISVNClient#svnGetEntries()
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Collection<WFSVNEntry> svnGetEntries() throws SVNException {
		this.lazyInitRepo();
		
		Collection<WFSVNEntry> result = new LinkedList<WFSVNEntry>();
		HashMap<String, WFSVNEntry> resultMap = new HashMap<String, WFSVNEntry>();
		
		List<String> delReqArtifs = new ArrayList<String>();
		
		ParameterizedRowMapper<String> artifactMapper = new ParameterizedRowMapper<String>() {
			
			@Override
			public String mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				String name = rs.getString("ARTIFACT_NAME");
				return name;
			}
		};
		
		delReqArtifs = this.wfProcess.getJdbcTempl().query(
				WFAbstractStep.SQL_SELECT_ARTIFS_BY_MASK,
				new Object[] { WFSVNClientPermission.WFACL_DELETE.getMask() },
				artifactMapper);
		
		Collection entries = this.repo.getDir("", -1, null, (Collection) null);
		Iterator iterator = entries.iterator();
		while (iterator.hasNext()) {
			
			SVNDirEntry entry = (SVNDirEntry) iterator.next();
			
			WFSVNEntry resEntry = new WFSVNEntry();
			resEntry.name = entry.getName();
			resEntry.lastModified = entry.getDate();
			resEntry.revision = entry.getRevision();
			resEntry.commitMsg = entry.getCommitMessage();
			resEntry.isDeleteRequested = delReqArtifs.contains(resEntry.name);
			
			resultMap.put(resEntry.name, resEntry);
			result.add(resEntry);
		}
		
		SqlRowSet locksRS = this.wfProcess.getJdbcTempl().queryForRowSet(
				"SELECT FILE_URLSTR, HOLDER FROM WAMCPSTORAGE_LOCKS");
		
		while (locksRS.next()) {
			String fileUrlStr = locksRS.getString("FILE_URLSTR");
			String filename = fileUrlStr.substring(fileUrlStr.lastIndexOf('/') + 1);
			WFSVNEntry lockedEntry = resultMap.get(filename);
			String lockHolder = locksRS.getString("HOLDER");
			if (lockedEntry != null) {
				lockedEntry.lockHolder = lockHolder;
			} else {
				// stale lock.. delete
				this.wfProcess.unlockUrlStr(fileUrlStr, lockHolder);
			}
		}
		
		return result;
	}
	
	@Override
	public WFSVNArtifact svnOpenRead(String filename) throws SVNException, IOException,
			WFSVNException {
		WFSVNArtifact result = WFSVNArtifact.factory(this.wfProcess, filename);
		
		// FIXME: "this" skips the security check.. refactor to use the proxy.openRead
		this.openRead(result);
		
		return result;
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.workflow.storage.WFISVNClient#svnOpen(java.lang.String)
	 */
	@Override
	public WFSVNArtifact svnOpenWrite(String filename) throws SVNException, IOException,
			WFSVNException {
		WFSVNArtifact result = WFSVNArtifact.factory(this.wfProcess, filename);
		
		// FIXME: "this" skips the security check.. refactor to use the proxy.openWrite
		this.openWrite(result);
		
		return result;
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.workflow.storage.WFISVNClient#svnRevert(org.bibalex.workflow.storage.WFSVNArtifact)
	 */
	@Override
	public void svnRevert(WFSVNArtifact artifact) throws SVNException,
			WFSVNArtifactLoadingException {
		
		this.svnClientManager.getWCClient().doRevert(
				new File[] { artifact.workingDir },
				SVNDepth.INFINITY, null);
		LOG.info("SVN: " + artifact.workingDir + " reverted to Base Copy");
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.workflow.storage.WFISVNClient#svnWCInit(java.lang.String)
	 */
	@Override
	public void svnWCInit(String appUserName) throws SVNException {
		ServletContext servletContext = (ServletContext) FacesContext
				.getCurrentInstance().getExternalContext().getContext();
		
		if (!this.wfProcess.isInitialized()) {
			this.wfProcess.init();
		}
		
		this.appUserName = appUserName;
		
		String dirName = appUserName.replaceAll("[\\p{Punct}\\p{Blank}]", "");
		
		String userDirPath = servletContext.getRealPath(WORK_SPACE
				+ File.separator + dirName);
		
		this.userDir = new File(userDirPath);
		if (!this.userDir.exists()) {
			this.userDir.mkdir();
		}
		
		this.authManager = SVNWCUtil.createDefaultAuthenticationManager(
				this.userDir,
				this.svnUsername, this.svnPassword);
		/*
		 * Creates a default run-time configuration options driver. Default
		 * options
		 * created in this way use the Subversion run-time configuration area
		 * (for
		 * instance, on a Windows platform it can be found in the
		 * '%APPDATA%\Subversion'
		 * directory).
		 * 
		 * readonly = true - not to save any configuration changes that can be
		 * done
		 * during the program run to a config file (config settings will only
		 * be read to initialize; to enable changes the readonly flag should be
		 * set
		 * to false).
		 * 
		 * SVNWCUtil is a utility class that creates a default options driver.
		 */
		ISVNOptions options = SVNWCUtil
				.createDefaultOptions(this.userDir, true);
		
		/*
		 * Creates an instance of SVNClientManager providing a default auth
		 * manager and an options driver
		 */
		this.svnClientManager = SVNClientManager.newInstance(options,
				this.authManager);
		
		// see if user dir is already a working copy
		try {
			
			this.svnClientManager.getWCClient().doInfo(this.userDir,
					SVNRevision.WORKING);
			
		} catch (SVNException e) {
			// make user dir a working copy
			SVNURL url = SVNURL.parseURIEncoded(this.URL_SVN_ROOT);
			
			long revNum = this.svnClientManager.getUpdateClient().doCheckout(
					url, // SVNURL url,
					this.userDir, // File dstPath,
					SVNRevision.HEAD, // SVNRevision pegRevision,
					SVNRevision.HEAD, // SVNRevision revision,
					SVNDepth.IMMEDIATES, // SVNDepth depth,
					false); // boolean allowUnversionedObstructions)
			
			LOG
					.info("SVN: Created working copy in user workspace at revision: "
							+ revNum);
		}
		
		this.xmlDir = new File(URLPathStrUtils.appendParts(userDirPath, "XML"));
		
		if (!this.xmlDir.exists()) {
			this.xmlDir.mkdir();
		}
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.workflow.storage.WFISVNClient#uninit()
	 */
	@Override
	public void uninit() {
		this.svnClientManager = null;
		this.authManager = null;
		this.userDir = null;
		this.xmlDir = null;
	}
	
	@Override
	public void unrequestDelete(String fileName) throws WFSVNException {
		
		if (!this.wfProcess.isInitialized()) {
			throw new WFSVNException("Must initialize workflow first!");
		}
		WFSVNArtifact delTarget = WFSVNArtifact.factory(this.wfProcess, fileName);
		this.wfProcess.getAclSecurityUtil().deletePermission(
				new ObjectIdentityImpl(delTarget),
				new GrantedAuthoritySid("ROLE_ADMIN"),
				WFSVNClientPermission.WFACL_DELETE);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bibalex.workflow.storage.WFISVNClient#updateFileInUserWorkingDir(java.lang.String)
	 */
	@Override
	@Deprecated
	public void updateFileInUserWorkingDir(String fileName)
			throws SVNException, IOException {
		
		SVNURL fileSvnUrl = SVNURL.parseURIEncoded(this.URL_SVN_ROOT);
		fileSvnUrl.appendPath(fileName, true);
		
		String fileLocalPathAbs = URLPathStrUtils.appendParts(this.userDir.getCanonicalPath(),
				fileName);
		
		File fileLocal = new File(fileLocalPathAbs);
		
		SVNUpdateClient updateClient = this.svnClientManager.getUpdateClient();
		/*
		 * sets externals to be ignored during the checkout
		 */
		updateClient.setIgnoreExternals(true);
		
		/*
		 * returns the number of the revision at which the working copy is
		 */
		long revNo = updateClient.doUpdate(
				fileLocal, // path - working copy path
				SVNRevision.HEAD, // revision - revision to update to
				SVNDepth.FILES, // depth - tree depth to update
				true, // allowUnversionedObstructions - flag that allows tollerating unversioned items during update
				false); // depthIsSticky - flag that controls whether the requested depth should be written to the
		// working cop
		
		LOG.info("SVN: " + this.userDir.getCanonicalPath() + " now contains revision " + revNo
				+ " of " + fileName);
	}
}
