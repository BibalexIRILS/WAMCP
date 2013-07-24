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

package org.bibalex.wamcp.storage;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.StringTokenizer;

import org.apache.solr.common.SolrInputDocument;
import org.bibalex.sdxe.exception.DomBuildException;
import org.bibalex.sdxe.exception.SDXEException;
import org.bibalex.sdxe.exception.SugtnException;
import org.bibalex.sdxe.xsa.model.XSALocator;
import org.bibalex.wamcp.application.WAMCPSessionBackingBean;
import org.bibalex.workflow.storage.WFISVNClient;
import org.bibalex.workflow.storage.WFSVNArtifact;
import org.bibalex.workflow.storage.WFSVNArtifactLoadingException;
import org.bibalex.workflow.storage.WFSVNException;
import org.jdom.JDOMException;
import org.tmatesoft.svn.core.SVNException;

public class WAMCPOldArabicStorage extends WAMCPIndexedStorage {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1890023510617343858L;
	protected final WAMCPIndexedStorage orig;
	
	public WAMCPOldArabicStorage(WAMCPIndexedStorage orig) throws SugtnException,
			WFSVNException, URISyntaxException {
		super(orig.CONFIG_URL_BAGAPI_BASE, orig.CONFIG_SCHEMA_FILE, orig.CONFIG_ROOTELTNAME,
				orig.CONFIG_NAMESPACE, orig.sdxeMediator, orig.CONFIG_SOLR_SERVER, orig.CONFIG_PUBLISH_SOLR_SERVER);
		this.orig = orig;
	}
	
	@Override
	protected void addFieldValueToDoc(SolrInputDocument doc, String fieldName, Object value,
			String fieldType) {
		String strValue = value.toString();
		
		if ((strValue == null) || strValue.isEmpty() || "FILL IN".equals(strValue)) {
			return;
		}
		
		StringBuilder replacedValBuilder = new StringBuilder();
		StringTokenizer tokenz = new StringTokenizer(strValue);
		while (tokenz.hasMoreTokens()) {
			String token = tokenz.nextToken();
			// String replacedToken = this.replaceOldChars(token);
			// if (token.equals(replacedToken)) {
			replacedValBuilder.append(token + " ");
			/*
			 * } else {
			 * replacedValBuilder.append("<choice><orig> ").append(token).append(" </orig>")
			 * .append("<reg> ").append(replacedToken).append(" </reg></choice> ");
			 * }
			 */
		}
		String processedValue = replacedValBuilder.toString();
		processedValue = processedValue.trim();
		super.addFieldValueToDoc(doc, fieldName, processedValue, fieldType);
	}
	
	@Override
	public boolean close(boolean forceUnlock, boolean unloadTempXml) throws SVNException,
			WFSVNArtifactLoadingException {
		return this.orig.close(forceUnlock, unloadTempXml);
	}
	
	@Override
	public void closeWorkingFile() {
		this.orig.closeWorkingFile();
	}
	
	@Override
	public WFSVNArtifact getArtifact() {
		
		return this.orig.getArtifact();
	}
	
	@Override
	public WAMCPGalleryBeanSVNStoredMetaGallery getGalleryBean() {
		
		return this.orig.getGalleryBean();
	}
	
	@Override
	public WAMCPSessionBackingBean getSessionBBean() {
		
		return this.orig.getSessionBBean();
	}
	
	@Override
	public WFISVNClient getSvnClient() {
		
		return this.orig.getSvnClient();
	}
	
	@Override
	public File getWorkingFile() {
		
		return this.orig.getWorkingFile();
	}
	
	@Override
	public void openRead(String fileNameNoExt) throws SVNException, IOException, WFSVNException {
		
		this.orig.openRead(fileNameNoExt);
	}
	
	protected void reindex(String filename, long revision, boolean commit) {
		
		try {
			LOG.debug("Rebuilding doc for: " + filename);
			
			// to avoid repetetively loading temp XML
			this.openRead(filename);
			
			this.sdxeMediator
					.open(this.getWorkingFile().getCanonicalPath(),
							this.schemaFilePath);
			
			XSALocator siteLoc = this.xsaDoc.getSiteLocator();
			this.sdxeMediator.moveToSugtnPath(siteLoc.asString());
			
			this.updateIndex(revision, commit);
			
		} catch (SVNException e) {
			LOG.error(e, e);
		} catch (SDXEException e) {
			LOG.error(e, e);
		} catch (JDOMException e) {
			LOG.error(e, e);
		} catch (Exception e) {
			LOG.error(e, e);
			
		} finally {
			
			try {
				
				this.close(true, false);
				
				this.sdxeMediator.close();
				
				this.closeWorkingFile();
				
			} catch (SVNException e) {
				LOG.error(e, e);
			} catch (DomBuildException e) {
				LOG.error(e, e);
			} catch (Exception e) {
				LOG.error(e, e);
			}
			
		}
	}
	
	public void reindexUponRelease(long revNum) {
		try {
			this.updateIndex(revNum, true);
		} catch (WFSVNException e) {
			LOG.error(e, e);
		}
		
	}
	
//	public String replaceOldChars(String token) {
//		String result = token.replace('\u06A1', 'Ù�'); // Feh no dots U+06A1
//		result = result.replace('\u06A2', 'Ù�'); // Feh dot below only U+06A2
//		result = result.replace('\u06A3', 'Ù�'); // Feh dots below and above U+06A3
//		result = result.replace('\u066F', 'Ù‚'); // Qaf no dots U+066F
//		result = result.replace('\u06BA', 'Ù†'); // Nun no dots U+06BA
//		result = result.replace('\u066E', 'Ø¨'); // Beh no dots U+066E
//		result = result.replace('\u067F', 'Øª'); // Teh no dots U+067F
//		result = result.replace('\u067D', 'Ø«'); // Theh no dots U+067D
//		result = result.replace('\u0679', 'ÙŠ'); // Yeh no dots U+0679
//		result = result.replace('\u067A', 'ÙŠ'); // Yeh dots above and below U+067A
//		result = result.replace('\u0678', 'ÙŠ'); // Yeh and hamza U+0678
//		result = result.replace('\u06B0', 'Ø­'); // Hah 2 dots below and above U+06B0
//		
//		return result;
//		
//	}

}
