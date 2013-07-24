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
import java.io.InputStream;

import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

//import net.sf.saxon.s9api.*;

import org.bibalex.oai.util.BAOAIIDBuilder;
import org.bibalex.util.URLPathStrUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

//import net.sf.saxon.s9api.Processor;
//import net.sf.saxon.s9api.XsltCompiler;
//import net.sf.saxon.s9api.XsltExecutable;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.bibalex.gallery.exception.BAGAlbumAlreadyExistsException;
import org.bibalex.gallery.exception.BAGException;
import org.bibalex.gallery.exception.BAGNotFoundException;
import org.bibalex.gallery.model.BAGAlbum;
import org.bibalex.gallery.model.BAGGalleryAbstract;
import org.bibalex.gallery.model.BAGThumbnail;
import org.bibalex.gallery.storage.BAGStorage;

import org.bibalex.sdxe.controller.DomTreeController;
import org.bibalex.sdxe.xsa.storage.XSAIStorage;
import org.bibalex.util.FileUtils;
import org.bibalex.util.SpringSecurityUtils;
import org.bibalex.util.URLPathStrUtils;
import org.bibalex.wamcp.application.WAMCPSessionBackingBean;
import org.bibalex.wamcp.exception.WAMCPException;
import org.bibalex.wamcp.exception.WAMCPGeneralCorrectableException;
import org.bibalex.workflow.IWFArtifact;
import org.bibalex.workflow.WFAbstractStep;
import org.bibalex.workflow.storage.WFISVNClient;
import org.bibalex.workflow.storage.WFSVNArtifact;
import org.bibalex.workflow.storage.WFSVNArtifactLoadingException;
import org.bibalex.workflow.storage.WFSVNClient.WFSVNEntry;
import org.bibalex.workflow.storage.WFSVNException;
import org.bibalex.workflow.storage.WFSVNLockedException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.MarcXmlReader;
import org.marc4j.marc.Record;
import org.springframework.dao.DataIntegrityViolationException;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;

import edu.emory.mathcs.backport.java.util.Collections;

public class WAMCPStorage implements XSAIStorage {

	public static String filenameForShelfMark(String shelfMark) {
		String fileName = FileUtils.makeSafeFileName(shelfMark);
		if ("xml".equalsIgnoreCase(FileUtils.getExtension(fileName))) {
			fileName = FileUtils.removeExtension(fileName);
		}
		return fileName;
	}

	protected WFISVNClient svnClient;
	protected WAMCPSessionBackingBean sessionBBean = null;

	protected WFSVNArtifact artifact;

	protected WAMCPGalleryBeanSVNStoredMetaGallery galleryBean;

	private Collection<WFSVNEntry> cachedEntries;

	private boolean readOnlyMode = false;

	protected void assureInit() throws SVNException {

		SpringSecurityUtils.assureThreadLocalAuthSet();

		if (!this.svnClient.isInit()) {
			this.svnClient.svnWCInit(this.sessionBBean.getUserName());
		}
	}

	public void cacheEntries() throws SVNException {
		this.cachedEntries = this.getEntries();
	}

	public boolean close(boolean forceUnlock) throws SVNException,
			WFSVNArtifactLoadingException {
		this.guardArtifactLoaded();
		this.assureInit();
		return this.svnClient.svnClose(this.artifact, forceUnlock);
	}

	@Override
	public void closeWorkingFile() {
		this.artifact = null;

	}

	public SVNCommitInfo commit(String commitMessage) throws SVNException,
			WFSVNException {
		this.guardArtifactLoaded();
		this.assureInit();
		return this.svnClient.svnCommit(this.artifact, commitMessage);
	}

	public long createFromDocument(Document xmlDoc, String shelfmark,
			String albumName, String albumCaption, String albumCoverName)
			throws JDOMException, WAMCPException, IOException, WFSVNException,
			SVNException, BAGException {
		this.guardNoArtifactLoaded();

		this.assureInit();

		String filenameNoExt = WAMCPStorage.filenameForShelfMark(shelfmark);

		boolean valid = (filenameNoExt != null)
				&& !filenameNoExt.trim().isEmpty();
		if (!valid) {
			throw new WAMCPGeneralCorrectableException("Invalid File Name");
		}

		Element root = xmlDoc.getRootElement();

		Namespace nsElt = root.getNamespace();
		Namespace nsXp = DomTreeController.acquireNamespace(
				root.getNamespaceURI(), root, true);
		String pfx = nsXp.getPrefix();
		if ((pfx != null) && !pfx.isEmpty()) {
			pfx += ":";
		}

		// ///////////////
		// set the id and shelfmark in the XML

		String xmlid = filenameNoExt.replaceAll("[\\p{Punct}\\p{Blank}]", "");
		xmlid = "m" + xmlid;

		// msDesc
		XPath msDescXp = XPath.newInstance("/" + pfx + "TEI/" + pfx + "text/"
				+ pfx + "body/" + pfx + "div/" + pfx + "msDesc");
		msDescXp.addNamespace(nsXp);

		Object msDescObj = msDescXp.selectSingleNode(xmlDoc);

		if (msDescObj == null) {
			throw new WAMCPGeneralCorrectableException(
					"XML has no '/TEI/text/body/div/msDesc'");

		}

		Element msDescElt = (Element) msDescObj;

		msDescElt.setAttribute("id", xmlid, Namespace.XML_NAMESPACE);

		// ////////////////////

		XPath shelfmarkXP = XPath.newInstance("/" + pfx + "TEI/" + pfx
				+ "text/" + pfx + "body/" + pfx + "div/" + pfx + "msDesc/"
				+ pfx + "msIdentifier/" + pfx + "idno");
		shelfmarkXP.addNamespace(nsXp);

		Element shelfmarkElt;
		Object obj = shelfmarkXP.selectSingleNode(xmlDoc);

		if (obj == null) {

			Element msIdElt = msDescElt.getChild("msIdentifier", nsElt);

			// TODO use DomTreeController for addition with insured validity

			if (msIdElt == null) {
				msIdElt = new Element("msIdentifier", nsElt);
				msDescElt.getChildren().add(0, msIdElt);
			}

			shelfmarkElt = new Element("idno", nsElt);
			obj = shelfmarkElt;

			msIdElt.addContent(shelfmarkElt);

		} else {
			shelfmarkElt = (Element) obj;
		}

		shelfmarkElt.setText(shelfmark); // filenameNoExt);

		// //////////////////////////

		try {
			// TODO use DomTreeController for addition that is surely valid

			// / YA20100811 Facs ref for valid doc
			// TODO; be DRY between this and
			// WAMCPUiActionListener.refToShownFacs --> delete code
			// refToShownFacs
			BAGAlbum album = this.galleryBean.openAlbum(albumName);

			String facsParentStr = "//" + pfx + "facsimile";
			XPath facsParentXP = XPath.newInstance(facsParentStr);
			facsParentXP.addNamespace(nsXp);

			Object facsParentObj = facsParentXP.selectSingleNode(xmlDoc);

			if (facsParentObj == null) {
				facsParentObj = new Element("facsimile", nsElt);
				List rootChildren = root.getChildren();
				if ((rootChildren.size() == 0)
						|| !"teiHeader".equals(((Element) rootChildren.get(0))
								.getName())) {
					rootChildren.add(0, new Element("teiHeader", nsElt));
				}
				rootChildren.add(1, (Element) facsParentObj); // 1 is after the
																// teiHeader

			} else {
				// Discard old values in the facs to avoid double ids for the
				// same thumb
				((Element) facsParentObj).getContent().clear();
			}

			Element facsParentElt = (Element) facsParentObj;

			for (BAGThumbnail thumb : album.getThumbnails()) {

				String imageName = thumb.getName();

				Element surfaceElt = new Element("surface", nsElt);
				facsParentElt.addContent(surfaceElt);

				String facsId = "i"
						+ imageName.substring(imageName.lastIndexOf('_') + 1);
				// we don't store the extension any more: facsId =
				// facsId.substring(0, facsId.lastIndexOf('.'));

				surfaceElt.setAttribute("id", facsId, Namespace.XML_NAMESPACE);

				Element graphicElt = new Element("graphic", nsElt);
				surfaceElt.addContent(graphicElt);

				graphicElt.setAttribute("url", imageName);
			}

			// / END YA20100811 Facs ref for valid doc

			// / Set this new MsDesc to be the MetaData file for the album
			// IMPORTANT: Keep this as the las action and its failsafe delete
			// right after
			this.galleryBean.addAlbumToMetaGallery(albumName, albumCaption,
					albumCoverName, filenameNoExt);

			// Do the actual addition.. the Blank or imported MsDesc XML
			this.artifact = this.svnClient.svnAdd(filenameNoExt);

		} catch (Exception e) {
			this.artifact = null;

			if (!(e instanceof BAGAlbumAlreadyExistsException)) {
				this.galleryBean.deleteAlbumForMetadataFile(filenameNoExt);
			}

			// How could this happen??
			if (e instanceof WFSVNLockedException) {
				throw (WFSVNLockedException) e;
			}

			throw new WAMCPException(e);
		}

		// //////////////////////////////////////////

		FileWriter writer = new FileWriter(this.artifact.getWorkingFile());

		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat()
				.setEncoding("UTF-8"));
		outputter.output(xmlDoc, writer);

		this.galleryBean.loadTempXml();

		// and commit
		SVNCommitInfo commInfo = this.commit("Adding new Ms Description: "
				+ xmlid + " - " + "Logged in as: "
				+ this.sessionBBean.getUserName());

		// TODONE the values do not go into the solrindex because the
		// DomController is not yet
		// initialized.. refactor the code in construct solr doc from dom to
		// make it use the
		// document and the prefix passed as a parameter, and override this
		// method in WAMCPIndexedStorage
		// to call it using xmlDoc and Pfx (will have to do it here!)

		long result = commInfo != null ? commInfo.getNewRevision() : -1;

		return result;

	}

	public long createFromTemplate(String templateFilePathRel,
			String shelfmark, String albumName, String albumCaption,
			String albumCoverName) throws IOException, SVNException,
			WFSVNException, JDOMException, WAMCPException, BAGException {

		ServletContext servletContext = (ServletContext) FacesContext
				.getCurrentInstance().getExternalContext().getContext();

		String templateFilePath = servletContext
				.getRealPath(templateFilePathRel);
		File templateFile = new File(templateFilePath);

		SAXBuilder saxBuilder = new SAXBuilder();

		saxBuilder.setFeature("http://xml.org/sax/features/validation", false);
		saxBuilder.setFeature("http://xml.org/sax/features/namespaces", true);
		saxBuilder.setFeature("http://xml.org/sax/features/namespace-prefixes",
				true);
		// Unsupported:
		// saxBuilder.setFeature("http://xml.org/sax/features/xmlns-uris",
		// false);

		Document templateDoc = saxBuilder.build(templateFile);
		return this.createFromDocument(templateDoc, shelfmark, albumName,
				albumCaption, albumCoverName);
	}

	public SVNCommitInfo delete(String fileName, String commitMessage)
			throws WFSVNException, SVNException, IOException {
		this.assureInit();

		try {
			SVNCommitInfo result = this.svnClient.svnDelete(fileName,
					commitMessage);
			try {
				this.galleryBean.deleteAlbumForMetadataFile(fileName);
			} catch (BAGNotFoundException e) {
				// it's ok.. we just wanted to delete it anyway!
			}
			return result;
		} catch (BAGException e) {
			throw new WFSVNException(e);
		}

	}

	public WFSVNArtifact getArtifact() {
		return this.artifact;
	}

	public List<String> getArtifactsDeleted() throws SVNException {
		this.assureInit();
		ArrayList<String> result = new ArrayList<String>();

		for (IWFArtifact artif : this.svnClient.getArtifactsDeleted()) {
			result.add(artif.getArtifactName());
		}

		return result;
	}

	public String getArtifactShelfMark() {
		return this.artifact.getArtifactName().replace('_', ' ');
	}

	public List<String> getArtifactsInStep(String stepName) throws SVNException {
		this.assureInit();
		ArrayList<String> result = new ArrayList<String>();

		for (IWFArtifact artif : this.svnClient.getWfProcess()
				.getStepForName(stepName).getArtifactsInStep()) {
			result.add(artif.getArtifactName());
		}

		Collections.sort(result);

		return result;
	}

	public Collection<WFSVNEntry> getCachedEntries() {
		return this.cachedEntries;
	}

	public Collection<WFSVNEntry> getEntries() throws SVNException {
		this.assureInit();
		return this.svnClient.svnGetEntries();
	}

	public WAMCPGalleryBeanSVNStoredMetaGallery getGalleryBean() {
		return this.galleryBean;
	}

	public boolean getIsWorking() {
		return this.isWorking();
	}

	public String getLoadedFileNameNoExt() throws WFSVNArtifactLoadingException {
		this.guardArtifactLoaded();
		return this.artifact.getArtifactName();
	}

	/**
	 * @return the sessionBBean
	 */
	public WAMCPSessionBackingBean getSessionBBean() {
		return this.sessionBBean;
	}

	public WFISVNClient getSvnClient() {
		return this.svnClient;
	}

	public File getUserDir() throws SVNException {
		this.assureInit();
		return this.svnClient.getUserDir();
	}

	/**
	 * @return the workingFile
	 */
	@Override
	public File getWorkingFile() {
		try {
			this.guardArtifactLoaded();
		} catch (WFSVNArtifactLoadingException e) {
			return null;
		}
		return this.artifact.getWorkingFile();
	}

	public String getWorkingFileSvnUrlStr() throws SVNException,
			WFSVNArtifactLoadingException {
		this.assureInit();
		this.guardArtifactLoaded();

		return this.svnClient.getFileSvnUrlStr(this.artifact);

	}

	protected void guardArtifactLoaded() throws WFSVNArtifactLoadingException {
		if (!this.isWorking()) {
			throw new WFSVNArtifactLoadingException(
					"An artifact must be loaded before issuing this command");
		}
	}

	protected void guardNoArtifactLoaded() throws WFSVNArtifactLoadingException {
		if (this.isWorking()) {
			throw new WFSVNArtifactLoadingException(
					"An artifact is already loaded by this client instance");
		}
	}

	public boolean isReadOnlyMode() {
		return this.readOnlyMode;
	}

	public boolean isWorking() {
		return this.artifact != null;
	}

	public boolean manuallyCheckPermission(String artifactName, String wfStage)
			throws WFSVNArtifactLoadingException, SVNException {

		this.assureInit();

		return this.svnClient.manuallyCheckPermission(artifactName, wfStage,
				this.sessionBBean.getAuth());
	}

	public void moveArtifactInWorkflow(String outcome)
			throws WFSVNArtifactLoadingException, SVNException {

		this.guardArtifactLoaded();
		this.assureInit();
		WFAbstractStep step = this.svnClient.getWfProcess().getStepForArtifact(
				this.artifact);
		step.complete(this.artifact, outcome);

	}

	public void openRead(String fileNameNoExt) throws SVNException,
			IOException, WFSVNException {
		this.guardNoArtifactLoaded();
		this.assureInit();
		this.artifact = this.svnClient.svnOpenRead(fileNameNoExt);
		this.readOnlyMode = true;
	}

	// public void permitDelete(String fileName) throws SVNException,
	// WFSVNException {
	// this.assureInit();
	// this.svnClient.permitDelete(fileName);
	//
	// }

	public void openWrite(String fileNameNoExt) throws SVNException,
			IOException, WFSVNException {
		this.guardNoArtifactLoaded();
		this.assureInit();
		this.artifact = this.svnClient.svnOpenWrite(fileNameNoExt);
		this.readOnlyMode = false;
	}

	public/* synchronized */void release(Document metadataXml, Element metaAlbum)
			throws BAGException, WAMCPException, WFSVNArtifactLoadingException,
			WFSVNLockedException {
		this.guardArtifactLoaded();

		// JdbcTemplate locksDBTempl =
		// this.svnClient.getWfProcess().getJdbcTempl();

		String releasedMetaGallUrlStr = URLPathStrUtils.appendParts(
				this.galleryBean.getGalleryRootUrlStr(),
				BAGGalleryAbstract.FILENAME_GALLERY_METADATA_XML);

		try {
			XMLOutputter outputter = new XMLOutputter(Format.getCompactFormat()
					.setEncoding("UTF-8"));

			this.svnClient.getWfProcess().lockUrlStr(releasedMetaGallUrlStr,
					this.sessionBBean.getUserName());

			try {
				SAXBuilder saxBuilder = new SAXBuilder();

				saxBuilder.setFeature("http://xml.org/sax/features/validation",
						false);
				saxBuilder.setFeature("http://xml.org/sax/features/namespaces",
						true);
				saxBuilder.setFeature(
						"http://xml.org/sax/features/namespace-prefixes", true);
				// Unsupported:
				// saxBuilder.setFeature("http://xml.org/sax/features/xmlns-uris",
				// false);

				Document metaGalleryXml;

				ByteArrayOutputStream remoteFileOS = new ByteArrayOutputStream();
				ByteArrayInputStream metaGalleryIn = null;
				try {
					BAGStorage.readRemoteFile(releasedMetaGallUrlStr,
							remoteFileOS);

					metaGalleryIn = new ByteArrayInputStream(
							remoteFileOS.toByteArray());

					metaGalleryXml = saxBuilder.build(metaGalleryIn);

				} finally {
					try {
						remoteFileOS.close();
						metaGalleryIn.close();
					} catch (IOException e) {
						throw new BAGException(e);
					}
				}
				XPath oldMetaAlbumXP = XPath.newInstance("//album[@name='"
						+ metaAlbum.getAttributeValue("name") + "']");

				Element metaGalleryRoot = metaGalleryXml.getRootElement();
				for (Object oldMetaAlbum : oldMetaAlbumXP
						.selectNodes(metaGalleryRoot)) {
					((Element) oldMetaAlbum).detach();
				}

				metaGalleryRoot.addContent(metaAlbum);

				File metaGalleryTempFile = File.createTempFile(FileUtils
						.makeSafeFileName(this.sessionBBean.getUserName()),
						"metagallery");
				FileOutputStream metaGalleryOut = new FileOutputStream(
						metaGalleryTempFile);

				outputter.output(metaGalleryXml, metaGalleryOut);

				metaGalleryOut.flush();
				metaGalleryOut.close();

				String releasedXMLUrlStr = URLPathStrUtils.appendParts(
						this.galleryBean.getGalleryRootUrlStr(), "XML",
						filenameForShelfMark(this.artifact.getArtifactName())
								+ ".tei.xml");

				File msDescTempFile = File.createTempFile(FileUtils
						.makeSafeFileName(this.sessionBBean.getUserName()),
						"msDescReleased");
				FileOutputStream msDescOut = new FileOutputStream(
						msDescTempFile);

				outputter.output(metadataXml, msDescOut);

				msDescOut.flush();
				msDescOut.close();

				// BAGStorage.copyFile(msDescTempFile.toURI().toString(),
				// releasedXMLUrlStr);
				// BAGStorage.copyFile(metaGalleryTempFile.toURI().toString(),
				// releasedMetaGallUrlStr);
				// TODO be DRY

				if (!BAGStorage
						.putFile(releasedXMLUrlStr, msDescTempFile, null)) {
					throw new WAMCPGeneralCorrectableException(
							"Could not upload tei file to release destination");
				}

				if (!BAGStorage.putFile(releasedMetaGallUrlStr,
						metaGalleryTempFile, null)) {
					throw new WAMCPGeneralCorrectableException(
							"Could not upload meta gallery file to release destination");
				}

				generateMARCFile(msDescTempFile,this.artifact.getArtifactName(),this.galleryBean.getGalleryRootUrlStr());
				
				// generate html file for the manuscript on release
				// both versions are generated (full and DIV)
				// also generate html for metadata print
				// also generate pdf for images of manuscript
				// Yasmine20110508 {
				// Yasmine20110621{
				
				HttpServletRequest request = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
                String uri = request.getRequestURL().toString();
                String[] splited = uri.split("/");
                // host = http://172.16.0.17:80/
//              String host = splited[0]+"//"+splited[2]+"/";
                String host = splited[0]+"//"+request.getLocalAddr()+":"+request.getLocalPort()+"/";
              
				int statusCode =0;
				BAOAIIDBuilder oaiIdBuilder = new BAOAIIDBuilder();
				String oaiId = oaiIdBuilder.buildId(this.artifact.getArtifactName());
				String url = host+"BAG-API/rest/desc/"+oaiId+"/transform?type=html";
				statusCode = myGetHttp(url);
				System.out.println(url +" ==> status code: "+statusCode);
				
				String urlDivOpt = host+"BAG-API/rest/desc/"+oaiId+"/transform?divOpt=true&type=html";
				statusCode = myGetHttp(urlDivOpt);
				System.out.println(urlDivOpt +" ==> status code: "+statusCode);
				
				String urlMetadataHtml = host+"BAG-API/rest/desc/"+oaiId+"/transform?type=meta";
				statusCode = myGetHttp(urlMetadataHtml);
				System.out.println(urlMetadataHtml +" ==> status code: "+statusCode);
				
					
				
//				String rootUrlStr = this.galleryBean.getGalleryRootUrlStr();
//				String tempUserName = this.sessionBBean.getUserName();
//				System.out.println("rootUrlStr: "+rootUrlStr);
//				System.out.println("tempUserName: "+tempUserName);
//				transformXMLtoHTML(rootUrlStr, tempUserName,
//						filenameForShelfMark(this.artifact.getArtifactName()),
//						false);
//				transformXMLtoHTML(rootUrlStr, tempUserName,
//						filenameForShelfMark(this.artifact.getArtifactName()),
//						true);
				// } Yasmine20110621
				// } Yasmine20110508
			} finally {
				this.svnClient.getWfProcess()
						.unlockUrlStr(releasedMetaGallUrlStr,
								this.sessionBBean.getUserName());
			}
		} catch (DataIntegrityViolationException e) {
			throw new WAMCPGeneralCorrectableException(
					"Another user is releasing at the moment, try again in a few seconds");
		} catch (JDOMException e) {
			throw new WAMCPException(e);
		} catch (IOException e) {
			throw new WAMCPException(e);
		}

	}

	public static void generateMARCFile(File msDescTempFile,String artifactName,String GalleryRootURL) throws IOException, WAMCPGeneralCorrectableException, BAGException {
		// TODO Auto-generated method stub
		// TODO Michael Release MARC xml here here
		File outMarcXML = File.createTempFile(FileUtils
				.makeSafeFileName(artifactName),
				"MARCXML");

		//String XSLTFullPath = "D:\\Projects\\JavaProjects\\wellcomeTrust\\TEI to MARC21 mapping\\Tomlinson, June Marc21 - WAMCP\\SYQ134caca8f_Test xslt Feb 20120216\\Test xslt Feb 20120216\\tei2marcxml.xsl";

		File xsltFile;// = new File(XSLTFullPath);
		
		//String root = System.getProperty("user.dir");
		String filepath = ((ServletContext) FacesContext
				.getCurrentInstance().getExternalContext().getContext())
				.getRealPath("/Schema/tei2marcxml_Customized.xsl"); // in case of Windows: "\\path \\to\\yourfile.txt
		//String abspath = root+filepath;



		// using above path read your file into byte []
		xsltFile = new File(filepath);

		try {
			FileOutputStream outStream = new FileOutputStream(
					outMarcXML);
			XSLTtransform.transformXmlDocument(msDescTempFile,
					xsltFile, outStream);
		} catch (TransformerException e) {
			e.printStackTrace();
			throw new WAMCPGeneralCorrectableException(
					"Could not transform TEI to MARC XML");
		}
		
		
		//Store MARC XML on the server
		String releasedMarcXMLUrlStr = URLPathStrUtils.appendParts(
				GalleryRootURL, "MARCXML",
				filenameForShelfMark(artifactName).replace(".tei", "")
						+ ".xml");
		if (!BAGStorage.putFile(releasedMarcXMLUrlStr, outMarcXML, null)) {
			throw new WAMCPGeneralCorrectableException(
					"Could not upload mrc file to release destination");
		}
		
		
		
		File outMrc;
		try {
			outMrc = File.createTempFile(FileUtils
					.makeSafeFileName(artifactName),
					"mrc");

			InputStream input = new FileInputStream(outMarcXML);
			MarcXmlReader reader = new MarcXmlReader(input);

			MarcWriter writer = new MarcStreamWriter(
					new FileOutputStream(outMrc),"UTF-8");
			while (reader.hasNext()) {
				Record record = reader.next();
				writer.write(record);
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new WAMCPGeneralCorrectableException(
					"Could not transform MARC XML to mrc file");
		}
		

		String releasedMarcUrlStr = URLPathStrUtils.appendParts(
				GalleryRootURL, "MARC",
				filenameForShelfMark(artifactName).replace(".tei", "")
						+ ".mrc");
		if (!BAGStorage.putFile(releasedMarcUrlStr, outMrc, null)) {
			throw new WAMCPGeneralCorrectableException(
					"Could not upload mrc file to release destination");
		}
	}

	public void requestDelete(String fileName) throws SVNException,
			WFSVNException {
		this.assureInit();
		this.svnClient.requestDelete(fileName);

	}

	public void revert() throws SVNException, WFSVNArtifactLoadingException {
		this.guardArtifactLoaded();
		this.assureInit();
		this.svnClient.svnRevert(this.artifact);
	}

	public void setGalleryBean(WAMCPGalleryBeanSVNStoredMetaGallery galleryBean) {
		this.galleryBean = galleryBean;
	}

	/**
	 * @param sessionBBean
	 *            the sessionBBean to set
	 */
	public void setSessionBBean(WAMCPSessionBackingBean sessionBBean) {
		this.sessionBBean = sessionBBean;
	}

	public void setSvnClient(WFISVNClient svnClient) {
		this.svnClient = svnClient;
	}

	public void uninit() {
		this.svnClient.uninit();

	}

	public void unrequestDelete(String fileName) throws SVNException,
			WFSVNException {
		this.assureInit();
		this.svnClient.unrequestDelete(fileName);

	}

	// Yasmine20110508

//	public static void transformXMLtoHTML(String rootURL, String tempUserName,
//			String oaiId, Boolean divOpt) {
//		Processor proc = new Processor(false);
//		XsltCompiler comp = proc.newXsltCompiler();
//		XsltExecutable exp;
//		try {
//			// System.out.println(source.getStringValue());
//			// String localId = BAOAIIDBuilder.localIdFromOaiId(oaiId);
//			String fileName = "";
//			String templateName = "recentTemplate";
//			if (divOpt) {
//				fileName = "div-" + oaiId;
//				templateName = "div-" + templateName;
//			} else {
//				fileName = oaiId;
//			}
//
//			String releasedXMLUrlStr = URLPathStrUtils.appendParts(rootURL,
//					"XML", oaiId + ".tei.xml");
//			String templateUrlStr = URLPathStrUtils.appendParts(rootURL, "XSL",
//					templateName + ".xsl");
//
//			ByteArrayOutputStream remoteXMLFileOS = new ByteArrayOutputStream();
//			ByteArrayInputStream xmlFileIn = null;
//			ByteArrayOutputStream remoteTemplateFileOS = new ByteArrayOutputStream();
//			ByteArrayInputStream templateFileIn = null;
//
//			BAGStorage.readRemoteFile(releasedXMLUrlStr, remoteXMLFileOS);
//			xmlFileIn = new ByteArrayInputStream(remoteXMLFileOS.toByteArray());
//			BAGStorage.readRemoteFile(templateUrlStr, remoteTemplateFileOS);
//			templateFileIn = new ByteArrayInputStream(
//					remoteTemplateFileOS.toByteArray());
//
//			exp = comp.compile(new StreamSource(templateFileIn));
//
//			XdmNode source = proc.newDocumentBuilder().build(
//					new StreamSource(xmlFileIn));
//
//			File tempGeneratedHTML = null;
//			tempGeneratedHTML = File.createTempFile(
//					FileUtils.makeSafeFileName(tempUserName), fileName
//							+ ".html");
//
//			Serializer out = proc.newSerializer(tempGeneratedHTML);
//			out.setOutputProperty(Serializer.Property.METHOD, "html");
//			out.setOutputProperty(Serializer.Property.INDENT, "yes");
//			XsltTransformer trans = exp.load();
//			trans.setInitialContextNode(source);
//			trans.setDestination(out);
//			trans.transform();
//
//			String releasedHTMLUrlStr = URLPathStrUtils.appendParts(rootURL,
//					"HTML", fileName + ".html");
//
//			BAGStorage.putFile(releasedHTMLUrlStr, tempGeneratedHTML, null);
//
//			remoteXMLFileOS.close();
//			xmlFileIn.close();
//			remoteTemplateFileOS.close();
//			templateFileIn.close();
//		} catch (SaxonApiException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (BAGException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}

	/**
	 * Sends an HTTP GET request to a url
	 * 
	 * @param endpoint
	 *            - The URL of the server. (Example:
	 *            " http://www.yahoo.com/search")
	 * @param requestParameters
	 *            - all the request parameters (Example:
	 *            "param1=val1&param2=val2"). Note: This method will add the
	 *            question mark (?) to the request - DO NOT add it yourself
	 * @return - The response from the end point
	 */
//	public static String sendGetRequest(String endpoint,
//			String requestParameters) {
//		String result = null;
//		if (endpoint.startsWith("http://")) {
//			// Send a GET request to the servlet
//			try {
//				// Construct data
//				StringBuffer data = new StringBuffer();
//
//				// Send data
//				String urlStr = endpoint;
//				if (requestParameters != null && requestParameters.length() > 0) {
//					urlStr += "?" + requestParameters;
//				}
//				URL url = new URL(urlStr);
//				URLConnection conn = url.openConnection();
//
//				// Get the response
//				BufferedReader rd = new BufferedReader(new InputStreamReader(
//						conn.getInputStream()));
//
//				StringBuffer sb = new StringBuffer();
//				String line;
//				while ((line = rd.readLine()) != null) {
//					sb.append(line);
//				}
//				rd.close();
//				result = sb.toString();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//		return result;
//	}

//	public static int myGetHttp(String urlStr) {
//		int result = 0;
//		int statusCode = 0;
//		// Send a GET request to the servlet
//		try {
//			URL url = new URL(urlStr);
//			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//			conn.setRequestMethod("GET");
//
//			conn.connect();
//			statusCode = conn.getResponseCode();
//
//			if (statusCode == HttpURLConnection.HTTP_ACCEPTED) {
//				InputStream in = conn.getInputStream();
//				BufferedReader reader = new BufferedReader(
//						new InputStreamReader(in));
//				String text = reader.readLine();
//				System.out.println(text);
//			}
//			conn.disconnect();
//			result = statusCode;
//		} catch (IOException ex) {
//			ex.printStackTrace();
//			System.out.println("made it here");
//			result = statusCode;
//		}
//
//		return result;
//	}

	
	
	public static int myGetHttp(String urlStr) {
		int result = 0;
		int statusCode = 0;
		// Send a GET request to the servlet
		
		
			DefaultHttpClient httpClient = new DefaultHttpClient();
			
			HttpGet httpGet = new HttpGet(urlStr);
			
			HttpEntity entity = null;
			try {
				HttpResponse httpResp = httpClient.execute(httpGet);
				
				if (200 != httpResp.getStatusLine().getStatusCode()) {
					result = httpResp.getStatusLine().getStatusCode();
				}
				else{
					result = 200;
				}
			} catch (IOException ex) {
				
				// In case of an IOException the connection will be released
				// back to the connection manager automatically
				ex.printStackTrace();
				
			} catch (RuntimeException ex) {
				
				// In case of an unexpected exception you may want to abort
				// the HTTP request in order to shut down the underlying
				// connection and release it back to the connection manager.
				httpGet.abort();
				throw ex;
				
			} finally {
				try {
					if (entity != null) {
						entity.consumeContent();
					}
				} catch (IOException e) {
					// Nothing
					e.printStackTrace();
				}
			}
		
		return result;
	
	}
	
	
	
//	public static int sendHttpClientGet(String url) {
//		int result = 0;
//		// Create an instance of HttpClient.
//		HttpClient client = new HttpClient();
//
//		// Create a method instance.
//		GetMethod method = new GetMethod(url);
//
//		// Provide custom retry handler is necessary
//		method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
//				new DefaultHttpMethodRetryHandler(3, false));
//
//		try {
//			// Execute the method.
//			int statusCode = client.executeMethod(method);
//
//			if (statusCode != HttpStatus.SC_OK) {
//				// System.err.println("Method failed: " +
//				// method.getStatusLine());
//				result = statusCode;
//			} else {
//				// Read the response body.
//				byte[] responseBody = method.getResponseBody();
//				// Deal with the response.
//				// Use caution: ensure correct character encoding and is not
//				// binary data
//				System.out.println(new String(responseBody));
//				result = HttpStatus.SC_OK;
//			}
//		} catch (HttpException e) {
//			System.err.println("Fatal protocol violation: " + e.getMessage());
//			e.printStackTrace();
//		} catch (IOException e) {
//			System.err.println("Fatal transport error: " + e.getMessage());
//			e.printStackTrace();
//		} finally {
//			// Release the connection.
//			method.releaseConnection();
//		}
//		return result;
//	}
}
