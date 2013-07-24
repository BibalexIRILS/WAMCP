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

package org.bibalex.wamcp.application;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.component.UISelectOne;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.VFS;
import org.apache.log4j.Logger;
import org.bibalex.Messages;
import org.bibalex.gallery.exception.BAGException;
import org.bibalex.gallery.icefaces.BAGAlbumBBean;
import org.bibalex.gallery.model.BAGAlbum;
import org.bibalex.gallery.storage.BAGStorage;
import org.bibalex.jsf.JSFValueGetter;
import org.bibalex.oai.util.BAOAIIDBuilder;
import org.bibalex.sdxe.binding.DomValueBinder;
import org.bibalex.sdxe.controller.DomTreeController;
import org.bibalex.sdxe.exception.DomBuildException;
import org.bibalex.sdxe.exception.SDXEException;
import org.bibalex.sdxe.xsa.application.XSAConstants;
import org.bibalex.sdxe.xsa.application.XSAMissingRequiredValExcpetion;
import org.bibalex.sdxe.xsa.application.XSASDXEDriver;
import org.bibalex.sdxe.xsa.application.XSAUiActionListener;
import org.bibalex.sdxe.xsa.exception.XSAException;
import org.bibalex.sdxe.xsa.model.XSAInstance;
import org.bibalex.sdxe.xsa.model.XSALocator;
import org.bibalex.sdxe.xsa.storage.XSAIStorage;
import org.bibalex.sdxe.xsa.uigen.icefaces.IXSASugtnCompsDecoratorFactory;
import org.bibalex.sdxe.xsa.uigen.icefaces.XSAUiGenFactoryAbstract;
import org.bibalex.uigen.icefaces.DoNothingCompsDecorator;
import org.bibalex.uigen.icefaces.IceCompsFactory;
import org.bibalex.uigen.icefaces.UIGenerationException;
import org.bibalex.util.FacesUtils;
import org.bibalex.util.FileUtils;
import org.bibalex.util.URLPathStrUtils;
import org.bibalex.wamcp.dao.TEIEnrichXAO;
import org.bibalex.wamcp.exception.WAMCPException;
import org.bibalex.wamcp.exception.WAMCPGeneralCorrectableException;
import org.bibalex.wamcp.pagebeans.SearchBBean;
import org.bibalex.wamcp.storage.WAMCPGalleryBeanSVNStoredMetaGallery;
import org.bibalex.wamcp.storage.WAMCPIndexedStorage;
import org.bibalex.wamcp.storage.WAMCPOldArabicStorage;
import org.bibalex.wamcp.storage.WAMCPStorage;
import org.bibalex.wamcp.uigen.WAMCPUiGenFactory;
import org.bibalex.workflow.storage.WFSVNArtifactLoadingException;
import org.bibalex.workflow.storage.WFSVNException;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;

import com.icesoft.faces.component.ext.HtmlPanelGrid;
import com.icesoft.faces.component.panelpopup.PanelPopup;
import com.icesoft.faces.context.effects.JavascriptContext;
import com.sun.facelets.FaceletException;

public class WAMCPUiActionListener extends XSAUiActionListener {

	public static abstract class AbstractConfirmListener {
		protected UIComponent popupContainer;
		protected WAMCPSessionBackingBean sbb;

		public AbstractConfirmListener(UIComponent popupContainer,
				WAMCPSessionBackingBean sbb) {
			this.popupContainer = popupContainer;
			this.sbb = sbb;
		}

		public void accept(ActionEvent ev) {
			try {
				this.doAccept(ev);
			} finally {
				this.cleanUp();
			}

		}

		public void cancel(ActionEvent ev) {
			try {
				this.doCancel(ev);
			} finally {
				this.cleanUp();
			}

		}

		protected void cleanUp() {
			PanelPopup popup = (PanelPopup) this.popupContainer
					.findComponent(ID_CONFIRM_PANEL);
			popup.setVisible(false);
			popup.setRendered(false);
			popup.getChildren().clear();
			popup.getFacets().clear();
			this.popupContainer.getChildren().remove(popup);
			// no inclosing instance of WAMCPUiActionListener is accessible here
			// WAMCPUiActionListener.this.wamcpSessionBBean.setConfirmListener(null);
			this.sbb.setConfirmListener(null);
		}

		protected abstract void doAccept(ActionEvent ev);

		protected abstract void doCancel(ActionEvent ev);
	}

	private abstract class WorkflowConfirmListener extends
			AbstractConfirmListener {
		protected final String outcome;

		public WorkflowConfirmListener(UIComponent parent,
				WAMCPSessionBackingBean wamcpSessionBBean, String outcome2) {
			super(parent, wamcpSessionBBean);
			this.outcome = outcome2;
		}
	}

	private static final Logger LOG = Logger.getLogger("org.bibalex.wamcp");

	private static final String ID_CONFIRM_PANEL = "ID_CONFIRM_PANEL";

	private WAMCPStorage wamcpStorage;

	private final WAMCPSessionBackingBean wamcpSessionBBean;

	private SearchBBean searchBBean = null;

	private final BAGAlbumBBean albumBBean;

	private WAMCPGalleryBeanSVNStoredMetaGallery galleryBean;

	private String schemaLocations;

	protected String validateAgainst = "xsd";

	private String xsdFilePath;

	public WAMCPUiActionListener(String cONFIGSCHEMAFILE,
			String cONFIGROOTELTNAME, String cONFIGNAMESPACE,
			WAMCPSessionBackingBean sessionBBean, BAGAlbumBBean albumBBean) {
		super(cONFIGSCHEMAFILE, cONFIGROOTELTNAME, cONFIGNAMESPACE);

		ServletContext servletContext = (ServletContext) FacesContext
				.getCurrentInstance().getExternalContext().getContext();

		this.xsdFilePath = servletContext.getRealPath(cONFIGSCHEMAFILE);
		this.xsdFilePath = "file:///" + this.xsdFilePath;

		this.schemaLocations = cONFIGNAMESPACE + " " + this.xsdFilePath;

		this.sessionBBean = sessionBBean;

		this.wamcpSessionBBean = sessionBBean;

		// Tell the longer life bean About yourself
		this.wamcpSessionBBean.setUiActionListener(this);

		this.albumBBean = albumBBean;
		// Tell the longer lived bean about yourself
		albumBBean.setControllingReqBBean(this);

	}

	@SuppressWarnings("deprecation")
	private void addRevisionToXML(String changeKind) { // , long svnRev) {
		try {
			// this is part of the save procedure so no need to call commit..
			// it's already done
			Document domTotal = this.sdxeMediator.getDomTreeController()
					.getDomTotal();

			// TODO use DomTreeController for addition with insured validity
			// SugtnDeclNode persNameSugtn =
			// this.sdxeMediator.getSugtnTreeController().getNodeAtSugtnPath("/TEI/teiHeader/revisionDesc/change/persName");
			// DomTreeController domTreeController =
			// this.sdxeMediator.getDomTreeController();

			// TODONOT!
			// //In case of failure in saving we should not record this failed
			// attempt
			// Document result = domTotal.clone();

			Element root = domTotal.getRootElement();
			Namespace ns = DomTreeController.acquireNamespace(
					root.getNamespaceURI(), root, true);
			String pfx = ns.getPrefix();
			if ((pfx != null) && !pfx.isEmpty()) {
				pfx += ":";
			}

			//
			XPath revDescXp = XPath.newInstance("/" + pfx + "TEI/" + pfx
					+ "teiHeader/" + pfx + "revisionDesc");
			revDescXp.addNamespace(ns);

			Object revDescObj = revDescXp.selectSingleNode(domTotal);
			if (revDescObj != null) {
				Element revDescElt = (Element) revDescObj;

				Element changeElt = new Element("change", root.getNamespace());
				// The SVN revision will be known only after we save:
				// changeElt.setText("SVN revision " + revNum);
				changeElt.setText(changeKind);
				String when = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
						.format(new Date());

				// Without this hack adding the revisionDesc element causes the
				// XML to be invalid
				// because the @when attribute type is wrong in the schemas
				int plusIx = when.indexOf('+');
				if (plusIx != -1) {
					when = when.substring(0, plusIx + 3) + ":"
							+ when.substring(plusIx + 3);
				}
				changeElt.setAttribute("when", when);

				Element persNameElt = new Element("persName",
						root.getNamespace());
				persNameElt.setText(this.wamcpSessionBBean.getUserName());

				changeElt.addContent(persNameElt);
				revDescElt.addContent(changeElt);

			} else {
				throw new DomBuildException(
						"Template is corrupted: No revisionDesc");
			}

			// return result;
		} catch (Exception e) {
			this.handleException(e);
		}
	}

	public void browseFaceted(ActionEvent ev) {

		try {

			this.searchBBean.setCriteria("");

			this.searchBBean.doSearch();

			this.searchBBean.filterRes(
					WAMCPIndexedStorage.WAMCP_INDEX_lastModifiedByFieldNAME,
					WAMCPIndexedStorage.WAMCP_INDEX_lastModifiedByFieldNAME
							+ ":" + this.wamcpSessionBBean.getUserName());

			// // Give Solr Time to return
			// Thread.sleep(500);

			FacesContext.getCurrentInstance().getExternalContext()
					.redirect("BrowseFaceted.iface");

		} catch (Exception e) {
			this.handleException(e);
		}
	}

	public void clearFacs(ActionEvent ev) {
		try {
			Map<String, Object> attrs = ev.getComponent().getAttributes();

			String id = (String) attrs.get(PARAM_ID_FOR_ELT);

			DomValueBinder binder = this.bindersBean.getDomValueBinders().get(
					id);

			UISelectOne facsSelect = (UISelectOne) FacesUtils
					.findComponentInRoot(id);

			Object selObj = JSFValueGetter.getObject(facsSelect);

			if (selObj == null) {
				return;
			}
			String facsRef = (String) selObj;

			if ((facsRef == null) || facsRef.isEmpty()) {
				return;
			}

			List<String> valueList = binder.getValueList();

			int selIx = valueList.indexOf(facsRef);

			valueList.remove(selIx);

			binder.setValueList(valueList);

			facsSelect.getChildren().remove(selIx);

			// We have to do it here.. or implement some new way to do it with
			// the other bean of the field
			binder.syncWithDom(true);

		} catch (DomBuildException e) {
			this.handleException(e);
		}
	}

	/**
	 * YM 19062011 This method is binded to the "Home" link in ReadOnly view It
	 * closes the opened artifact without saving changes
	 * 
	 * @param ev
	 */
	public void closeReadOnlyMode(ActionEvent ev) {
		try {
			if (this.wamcpStorage.close(false))
				this.doClosePart2();
		} catch (Exception e) {
			this.handleException(e);
		}
	}

	public void close(ActionEvent ev) {
		try {
			this.gaurdUnsavedChanges();
			if (this.wamcpStorage.close(false)) {
				this.doClosePart2();
			} else {

				if (this.wamcpStorage.isReadOnlyMode()) {
					this.doClosePart2();
				}

				this.confirm(
						ev.getComponent().getParent(),
						"Changes Exist",
						"The manuscript is now locked for you which will prevent othes from opening it. What would you like to do?",
						"Keep lock and close", "Stay to save & unlock",
						new AbstractConfirmListener(ev.getComponent()
								.getParent(), this.wamcpSessionBBean) {

							@Override
							protected void doAccept(ActionEvent ev) {
								WAMCPUiActionListener.this.doClosePart2();

							}

							@Override
							protected void doCancel(ActionEvent ev) {
								// Nothing

							}
						});
			}
		} catch (Exception e) {
			this.handleException(e);
		}
	}

	public void confirm(UIComponent container, String title, String message,
			String acceptLabel, String cancelLabel,
			AbstractConfirmListener confirmListener)
			throws UIGenerationException, FaceletException, FacesException,
			ELException, IOException {

		FacesContext ctx = FacesContext.getCurrentInstance();
		IceCompsFactory compsFactory = new IceCompsFactory(
				new DoNothingCompsDecorator());
		PanelPopup popup = compsFactory.panelPopup(ID_CONFIRM_PANEL, title,
				true, "#{true}");
		HtmlPanelGrid target = (HtmlPanelGrid) popup.getBody();

		FacesUtils.addCtxVariable(ctx, "CXTVAR_CONFIRM_MSG", message);
		FacesUtils.addCtxVariable(ctx, "CXTVAR_CONFIRM_ACCEPT", acceptLabel);
		FacesUtils.addCtxVariable(ctx, "CXTVAR_CONFIRM_CANCEL", cancelLabel);

		try {
			this.createUiGenFactory().applyFacelet(target,
					"/templates/wamcp/confirmation.xhtml", ctx);
		} catch (XSAException e) {
			this.handleException(e);
		}

		this.wamcpSessionBBean.setConfirmListener(confirmListener);
		container.getChildren().add(popup);
	}

	@Override
	protected IXSASugtnCompsDecoratorFactory createCompsDecorator() {
		return new WAMCPCompsDecoratorFactory();
	}

	@Override
	protected XSAUiGenFactoryAbstract createUiGenFactory() {
		return new WAMCPUiGenFactory(this.sdxeMediator, this.bindersBean,
				this.createCompsDecorator(), this.xsaDoc);
	}

	public void delete(ActionEvent ev) {
		try {
			String fileName = (String) ev.getComponent().getAttributes()
					.get("PARAM_MSDESC_FILENAME");
			this.confirm(
					ev.getComponent().getParent(),
					"Are you sure?",
					"This will permenantly delete the Manuscript Metadata from SVN, do you want to continue?",
					"Yes", "No", new WorkflowConfirmListener(ev.getComponent()
							.getParent(), this.wamcpSessionBBean, fileName) {

						@Override
						protected void doAccept(ActionEvent ev) {
							try {
								SVNCommitInfo commitInfo = WAMCPUiActionListener.this.wamcpStorage
										.delete(this.outcome,
												"Logged in as: "
														+ WAMCPUiActionListener.this.wamcpSessionBBean
																.getUserName());

								// this.galleryBean.deleteAlbumForMetadataFile(fileName);
								WAMCPUiActionListener.this.setHeaderMsg(
										"Deleted successfully",
										StyledMsgTypeEnum.SUCCESS);
							} catch (Exception e) {
								WAMCPUiActionListener.this.handleException(e);
							}

						}

						@Override
						protected void doCancel(ActionEvent ev) {
							// TODO Auto-generated method stub

						}
					});
		} catch (Exception e) {
			this.handleException(e);
		}

	}

	@Override
	public void deleteFromDomBaseShifter(ActionEvent ev) {
		class DeleteBaseEltConfirmListener extends AbstractConfirmListener {
			private final String targetPart;
			private final String areaId;
			private final int targetIx;

			public DeleteBaseEltConfirmListener(UIComponent parent,
					WAMCPSessionBackingBean wamcpSessionBBean,
					String targetPart, int targetIx, String areaId) {
				super(parent, wamcpSessionBBean);
				this.targetPart = targetPart;
				this.areaId = areaId;
				this.targetIx = targetIx;
			}

			@Override
			protected void doAccept(ActionEvent ev) {
				try {
					WAMCPUiActionListener.this.doBaseShift(
							EnumDoShiftModes.delete, this.targetPart,
							this.targetIx, this.areaId);
				} catch (Exception e) {
					WAMCPUiActionListener.this.handleException(e);
				}

			}

			@Override
			protected void doCancel(ActionEvent ev) {
				// Nothing

			}
		}
		try {
			Map<String, Object> attrs = ev.getComponent().getAttributes();
			String targetPart = (String) attrs
					.get(XSAConstants.PARAM_BASESHIFT_PART);
			String areaId = (String) attrs
					.get(XSAConstants.PARAM_BASESHIFT_AREAID);

			int targetIx = (Integer) attrs
					.get(XSAConstants.PARAM_BASESHIFT_TARGET_IX);

			this.confirm(
					ev.getComponent().getParent(),
					"Deleting a base element",
					"The contents entered in a whole area will be deleted! Are you sure that you want to delete a base element and all of its children?",
					"Yes.. I delete a whole area", "No.. a whole area is big",
					new DeleteBaseEltConfirmListener(ev.getComponent()
							.getParent(), this.wamcpSessionBBean, targetPart,
							targetIx, areaId));
		} catch (Exception e) {
			this.handleException(e);
		}

	}

	protected void doClosePart2() {
		try {
			this.uninitEditSession();
			// redirect
			FacesContext.getCurrentInstance().getExternalContext()
					.redirect("Welcome.iface");

		} catch (Exception e) {
			this.handleException(e);
		}
	}

	@Override
	protected boolean doSave() throws IOException, SDXEException, JDOMException {
		if (!this.doCommitToRealDom()) {
			this.setHeaderMsg(
					"Unacceptable mixed values exist, revise all tabs",
					StyledMsgTypeEnum.ERROR);
			return false; // To show the messages
		}

		String filePathAbs = this.storage.getWorkingFile().getCanonicalPath();
		this.sdxeMediator.saveRealDom(filePathAbs);

		return true;
	}

	protected long doSaveToStorage(boolean throwOnMissing, boolean removeEmpty)
			throws JDOMException, SDXEException, SVNException, IOException,
			WFSVNException {

		// YA20110330 Patch for DEBUG attrbiute unintentionally left
		if (removeEmpty) {
			XPath debugAttrXP = XPath.newInstance("//*[@DEBUG]");
			for (Object obj : debugAttrXP.selectNodes(this.sdxeMediator
					.getDomTreeController().getDomTotal())) {
				((Element) obj).removeAttribute("DEBUG");
			}
		}

		// The deletion of empty fields is removed from this class' doSave
		// if (!this.doSave()) {
		// return;
		// }

		// if (!this.doCommitToRealDom()) {
		// this.setHeaderMsg(
		// "Unacceptable mixed values exist, revise all tabs",
		// StyledMsgTypeEnum.ERROR);
		// return; // To show the messages
		// }
		if (!this.runCheckPresence(throwOnMissing, removeEmpty)) {
			this.setHeaderMsg(
					"Unacceptable mixed values exist, revise all tabs",
					StyledMsgTypeEnum.ERROR);
			return -1; // To show the messages
		}

		this.addRevisionToXML(org.bibalex.Messages.getString("en",
				"WAMCPUiActionListener.saveToStorage.revisionDescMsg")
				+ this.wamcpStorage.getWorkingFileSvnUrlStr());

		// if (removeEmpty || checkRequired) {
		// XSAInstance instForDomCurr =
		// this.uiGenDomListener.getInstForDomCurrent();
		// XSASDXEDriver xsaSdxeDriver = new XSASDXEDriver(this.sdxeMediator,
		// this.xsaDoc);
		//
		// xsaSdxeDriver.checkPresenceInInstance(instForDomCurr, checkRequired,
		// removeEmpty);
		// }

		String filePathAbs = this.storage.getWorkingFile().getCanonicalPath();
		this.sdxeMediator.saveRealDom(filePathAbs);

		// // Because the empty elements are gone so we have to regenerate UI
		// this.doGoToAreas();

		// Diff to see if there are actually any changes

		SVNCommitInfo comi = this.wamcpStorage.commit("Logged in as: "
				+ this.wamcpSessionBBean.getUserName());
		long result = -1;
		if (comi != null) {
			result = comi.getNewRevision();
		}

		// The locking will prevent remote changes
		// String fileName = this.wamcpStorage.getLoadedFileNameNoExt();
		//
		// this.uninitEditSession();
		// this.wamcpStorage.open(fileName);
		// this.initEditSession();
		return result;
	}

	public void generatePortletHTMLFiles(ActionEvent ev) {

		String dirUrlStr = URLPathStrUtils.appendParts(
				this.galleryBean.getGalleryRootUrlStr(), "XML");
		
		HttpServletRequest request = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String uri = request.getRequestURL().toString();
        String[] splited = uri.split("/");
        // host = http://172.16.0.17:80/
//        String host = splited[0]+"//"+splited[2]+"/";
        String host = splited[0]+"//"+request.getLocalAddr()+":"+request.getLocalPort()+"/";
        
		try {
			List<String> childrenFilesStr = BAGStorage.listChildren(dirUrlStr,
					FileType.FILE);
			int statusCode =0;
			
			for (String childStr : childrenFilesStr) {
				System.out.println("childStr: "+childStr);
				int extIndx = childStr.indexOf('.');
				String localOaiId = childStr.substring(0, extIndx);
				
				BAOAIIDBuilder oaiIdBuilder = new BAOAIIDBuilder();
				String oaiId = oaiIdBuilder.buildId(localOaiId);
				System.out.println("oaiId: "+oaiId);
				String url = host+"BAG-API/rest/desc/"+oaiId+"/transform?type=html";
				statusCode = WAMCPStorage.myGetHttp(url);
				System.out.println(url +" ==> status code: "+statusCode);
				
//				System.out.println(oaiId+" {Html}: "+statusCode);
				String urlDivOpt = host+"BAG-API/rest/desc/"+oaiId+"/transform?divOpt=true&type=html";
				statusCode = WAMCPStorage.myGetHttp(urlDivOpt);
//				System.out.println(oaiId+" {Html, div}: "+statusCode);
				System.out.println(urlDivOpt +" ==> status code: "+statusCode);

				
//				WAMCPStorage.transformXMLtoHTML(rootUrlStr, tempUserName,
//						localOaiId, true);
//				WAMCPStorage.transformXMLtoHTML(rootUrlStr, tempUserName,
//						localOaiId, false);

			}

		} catch (BAGException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void generatePrintableMetadataHTMLFiles(ActionEvent ev) {

		String dirUrlStr = URLPathStrUtils.appendParts(
				this.galleryBean.getGalleryRootUrlStr(), "XML");
		
		HttpServletRequest request = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String uri = request.getRequestURL().toString();
        String[] splited = uri.split("/");
        // host = http://172.16.0.17:80/
//        String host = splited[0]+"//"+splited[2]+"/";
        String host = splited[0]+"//"+request.getLocalAddr()+":"+request.getLocalPort()+"/";

		try {
			List<String> childrenFilesStr = BAGStorage.listChildren(dirUrlStr,
					FileType.FILE);
			int statusCode =0;
			
			for (String childStr : childrenFilesStr) {
				System.out.println("childStr: "+childStr);
				int extIndx = childStr.indexOf('.');
				String localOaiId = childStr.substring(0, extIndx);
				
				BAOAIIDBuilder oaiIdBuilder = new BAOAIIDBuilder();
				String oaiId = oaiIdBuilder.buildId(localOaiId);
				System.out.println("oaiId: "+oaiId);
								
				String urlMetadataHtml = host+"BAG-API/rest/desc/"+oaiId+"/transform?type=meta";
				statusCode = WAMCPStorage.myGetHttp(urlMetadataHtml);
				System.out.println(urlMetadataHtml +" ==> status code: "+statusCode);
				
			}

		} catch (BAGException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void generateMARCFiles(ActionEvent ev) {

		String dirUrlStr = URLPathStrUtils.appendParts(
				this.galleryBean.getGalleryRootUrlStr(), "XML");
		
		HttpServletRequest request = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String uri = request.getRequestURL().toString();
        String[] splited = uri.split("/");
        // host = http://172.16.0.17:80/
//        String host = splited[0]+"//"+splited[2]+"/";
        String host = splited[0]+"//"+request.getLocalAddr()+":"+request.getLocalPort()+"/";

		try {
			List<String> childrenFilesStr = BAGStorage.listChildren(dirUrlStr,
					FileType.FILE);
			int statusCode =0;
			String XMLFilePath;
			for (String childStr : childrenFilesStr) {
				System.out.println("childStr: "+childStr);
				XMLFilePath = URLPathStrUtils.appendParts(dirUrlStr,childStr);
				
				//File XMLfile = new File(XMLFilePath);
			//	FileObject imageFO;
				
			//	imageFO = VFS.getManager().resolveFile(XMLFilePath);
				
				File XMLfile=null;
				try {
					XMLfile = File.createTempFile(FileUtils
							.makeSafeFileName(childStr),
					"msDescReleased");
				
				
				BAGStorage.readRemoteFile(XMLFilePath,new FileOutputStream(XMLfile));
				} catch (IOException e1) {
					this.handleException(e1);
				}
				
				
			//	URL url = new URL(XMLFilePath);
			//	InputStream is = url.openStream();
				
				try {
					WAMCPStorage.generateMARCFile(XMLfile, childStr, this.galleryBean.getGalleryRootUrlStr());
				} catch (WAMCPGeneralCorrectableException e) {
					// TODO Auto-generated catch block
					this.handleException(e);
				} catch (IOException e) {
					this.handleException(e);
				}
				
//				int extIndx = childStr.indexOf('.');
//				String localOaiId = childStr.substring(0, extIndx);
//				
//				BAOAIIDBuilder oaiIdBuilder = new BAOAIIDBuilder();
//				String oaiId = oaiIdBuilder.buildId(localOaiId);
//				System.out.println("oaiId: "+oaiId);
//								
//				String urlMetadataHtml = host+"BAG-API/rest/desc/"+oaiId+"/transform?type=meta";
//				statusCode = WAMCPStorage.myGetHttp(urlMetadataHtml);
//				System.out.println(urlMetadataHtml +" ==> status code: "+statusCode);
				
			}

		} catch (BAGException e) {
			this.handleException(e);
		}
	}
	
	
	public void generateImagePdfFiles(ActionEvent ev) {

		String dirUrlStr = URLPathStrUtils.appendParts(
				this.galleryBean.getGalleryRootUrlStr(), "XML");
//		System.out.println("dirUrlStr: "+dirUrlStr);
		
		HttpServletRequest request = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String uri = request.getRequestURL().toString();
//        System.out.println("uri: "+uri);
        String[] splited = uri.split("/");
        // host = http://172.16.0.17:80/
//      String host = splited[0]+"//"+splited[2]+"/";
      String host = splited[0]+"//"+request.getLocalAddr()+":"+request.getLocalPort()+"/";
      
//        System.out.println("Host: "+host);
        
		try {
			List<String> childrenFilesStr = BAGStorage.listChildren(dirUrlStr,
					FileType.FILE);
//			String rootUrlStr = this.galleryBean.getGalleryRootUrlStr();
//			String tempUserName = this.wamcpSessionBBean.getUserName();
			int statusCode =0;
			
			for (String childStr : childrenFilesStr) {
				System.out.println("childStr: "+childStr);
				int extIndx = childStr.indexOf('.');
				String localOaiId = childStr.substring(0, extIndx);
				
				BAOAIIDBuilder oaiIdBuilder = new BAOAIIDBuilder();
				String oaiId = oaiIdBuilder.buildId(localOaiId);
//				System.out.println("oaiId: "+oaiId);
				
				String urlImagesPdf = host+"BAG-API/rest/desc/"+oaiId+"/transform?type=img";
				statusCode = WAMCPStorage.myGetHttp(urlImagesPdf);
				System.out.println(urlImagesPdf +" ==> status code: "+statusCode);
//				WAMCPStorage.transformXMLtoHTML(rootUrlStr, tempUserName,
//						localOaiId, true);
//				WAMCPStorage.transformXMLtoHTML(rootUrlStr, tempUserName,
//						localOaiId, false);

			}

		} catch (BAGException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public BAGAlbumBBean getAlbumBBean() {
		return this.albumBBean;
	}

	public List<String> getArtifactsInDeleted() {
		try {
			return this.wamcpStorage.getArtifactsDeleted();
		} catch (SVNException e) {
			this.handleException(e);
			return null;
		}

	}

	public List<String> getArtifactsInDone() {
		try {
			return this.wamcpStorage.getArtifactsInStep("DONE");
		} catch (SVNException e) {
			this.handleException(e);
			return null;
		}

	}

	public List<String> getArtifactsInEdit() {
		try {
			return this.wamcpStorage.getArtifactsInStep("EDIT");
		} catch (SVNException e) {
			this.handleException(e);
			return null;
		}

	}

	public List<String> getArtifactsInReview1() {
		try {
			return this.wamcpStorage.getArtifactsInStep("REVIEW1");
		} catch (SVNException e) {
			this.handleException(e);
			return null;
		}

	}

	public List<String> getArtifactsInReview2() {
		try {
			return this.wamcpStorage.getArtifactsInStep("REVIEW2");
		} catch (SVNException e) {
			this.handleException(e);
			return null;
		}

	}

	public WAMCPGalleryBeanSVNStoredMetaGallery getGalleryBean() {
		return this.galleryBean;
	}

	public String getSchemaLocations() {
		return this.schemaLocations;
	}

	public SearchBBean getSearchBBean() {
		return this.searchBBean;
	}

	public String getValidateAgainst() {
		return this.validateAgainst;
	}

	public WAMCPStorage getWamcpStorage() {
		return this.wamcpStorage;
	}

	public String getXsdFilePath() {
		return this.xsdFilePath;
	}

	public void goHome(ActionEvent ev) {
		try {

			FacesContext.getCurrentInstance().getExternalContext()
					.redirect("Welcome.iface");

		} catch (IOException e) {
			this.handleException(e);
		}
	}

	@Override
	public void initEditSession() throws XSAException {
		if (this.wamcpSessionBBean.isInEditSession()) {
			LOG.warn("Last edit session not properly closed. Will close now!");
			this.uninitEditSession();
		}

		ServletContext servletContext = (ServletContext) FacesContext
				.getCurrentInstance().getExternalContext().getContext();

		String schemaFilePath = servletContext
				.getRealPath(this.CONFIG_SCHEMA_FILE);
		try {
			this.sdxeMediator.open(this.wamcpStorage.getWorkingFile()
					.getCanonicalPath(), schemaFilePath);

			XSALocator siteLoc = this.xsaDoc.getSiteLocator();
			this.sdxeMediator.moveToSugtnPath(siteLoc.asString());

			this.uiGenDomListener.init();

			// Immitate the first event that is not always caught!
			this.uiGenDomListener.notifyDomMovedVertical(this.sdxeMediator
					.getCurrentSugtnElement());

			String filename = WAMCPStorage
					.filenameForShelfMark(this.wamcpStorage.getArtifact()
							.getArtifactName());
			BAGAlbum albumForMsDesc = this.galleryBean
					.openAlbumForMetadataFile(filename);

			this.albumBBean.setAlbum(albumForMsDesc);

		} catch (SDXEException e) {

			throw new XSAException(e);
		} catch (IOException e) {

			throw new XSAException(e);
		} catch (JDOMException e) {

			throw new XSAException(e);
		} catch (BAGException e) {

			throw new XSAException(e);
		}
	}

	public void logout(ActionEvent ev) {
		try {
			// TODONOT clear all managed beans.. destroy the session for real
			// EVEN THIS NOT HERE this.storage.uninit();
			// / WILL BE DONE WHEN SESSION IS INVALIDATED
			FacesContext.getCurrentInstance().getExternalContext()
					.redirect("j_spring_security_logout");

			((HttpSession) FacesContext.getCurrentInstance()
					.getExternalContext().getSession(false)).invalidate();

		} catch (Exception e) {
			this.handleException(e);
		}
	}

	public void moveWorkflow(ActionEvent ev) {

		String outcome = (String) ev.getComponent().getAttributes()
				.get("WF_OUTCOME");
		String checkReqStr = (String) ev.getComponent().getAttributes()
				.get("CHECK_REQ");

		boolean checkReq = Boolean.parseBoolean(checkReqStr);

		try {

			// YA 20110217 Save workflow stage in Solr
			// // YA20100810 Unlock
			// // TODO: Maybe this is not a step where editing and saving is
			// allowed... so we can't
			// this.saveToStorage(ev);
			// // this.gaurdUnsavedChanges();
			// // END YA20100810 Unlock
			// END YA 20110217 Save workflow stage in Solr

			if (checkReq) {
				this.runCheckPresence(true, false);
			}

			this.confirm(
					ev.getComponent().getParent(),
					"Work flow transition confirmation",
					"You are about to do a work flow transition, which might make the Manuscript inaccessible to you anymore. Are you sure?",
					"Yes.. I'm done working on it",
					"No.. I clicked by mistake", new WorkflowConfirmListener(ev
							.getComponent().getParent(),
							this.wamcpSessionBBean, outcome) {
						@Override
						protected void doAccept(ActionEvent ev) {
							WAMCPUiActionListener.this.moveWorkflowPart2(ev,
									this.outcome);
						}

						@Override
						protected void doCancel(ActionEvent ev) {
							// Nothing

						}

					});

		} catch (XSAMissingRequiredValExcpetion e) {
			try {
				this.confirm(
						ev.getComponent().getParent(),
						"Missing required values",
						"Some required values are missing, are you sure you want to do the work flow transition?",
						"Yes", "No", new WorkflowConfirmListener(ev
								.getComponent().getParent(),
								this.wamcpSessionBBean, outcome) {

							@Override
							protected void doAccept(ActionEvent ev) {
								WAMCPUiActionListener.this.moveWorkflowPart2(
										ev, this.outcome);

							}

							@Override
							protected void doCancel(ActionEvent ev) {
								// Nothing

							}
						});
			} catch (Exception e1) {
				this.handleException(e1);
			}

		} catch (Exception e0) {
			this.handleException(e0);
		}
	}

	private void moveWorkflowPart2(ActionEvent ev, String outcome) {

		try {
			WAMCPUiActionListener.this.wamcpStorage
					.moveArtifactInWorkflow(outcome);

			// YA 20110217 Save workflow stage in Solr
			// this.runCheckPresence(false, true);
			this.doSaveToStorage(false, true);
			// END YA 20110217 Save workflow stage in Solr

			this.wamcpStorage.close(true);

			this.doClosePart2();

		} catch (Exception e) {
			this.handleException(e);
		}
	}
	public void downloadXML(ActionEvent ev){
		try {
			if (ev != null) {
				String msg = "Metadata XML :";
				this.showMessageWithDownloadLink(msg, this.wamcpStorage
						.getWorkingFile().getCanonicalPath());		
				HttpServletRequest request = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
				request.getRequestURL().toString();
			}
		} catch (Exception e) {
			// NO: this.sdxeMediator.getDomTreeController().setDomTotal(backup);
			this.handleException(e);
		}		
	}
	public void openFileReadOnly(ActionEvent ev) {
		String fileName = (String) ev.getComponent().getAttributes()
				.get("PARAM_MSDESC_FILENAME");
		fileName = WAMCPStorage.filenameForShelfMark(fileName);

		String page = "";
		try {

			// YM 20110614 Open Read only

			page = "ViewForm.iface";
			// Open Read Only
			this.wamcpStorage.openRead(fileName);

			FacesContext.getCurrentInstance().getExternalContext()
					.redirect(page);

		} catch (WFSVNArtifactLoadingException e) {
			try {
				if (fileName.equals(this.wamcpStorage.getLoadedFileNameNoExt())) {
					try {
						FacesContext.getCurrentInstance().getExternalContext()
								.redirect(page);
					} catch (Exception e1) {
						WAMCPUiActionListener.this.handleException(e1);
					}
				} else {
					this.confirm(
							ev.getComponent().getParent(),
							"Found live edit session",
							"Found a session that is not properly closed, working on file: "
									+ this.wamcpStorage.getWorkingFile()
											.getName(),
							"Close it without saving",
							"Return to this session",
							new WorkflowConfirmListener(ev.getComponent()
									.getParent(), this.wamcpSessionBBean, page) {

								@Override
								protected void doAccept(ActionEvent ev) {
									try {
										WAMCPUiActionListener.this.wamcpStorage
												.close(false);
										WAMCPUiActionListener.this.sdxeMediator
												.rollback();
										WAMCPUiActionListener.this
									  			.uninitEditSession();
									} catch (Exception e1) {
										WAMCPUiActionListener.this
												.handleException(e1);
									}
								}

								@Override
								protected void doCancel(ActionEvent ev) {
									try {
										FacesContext.getCurrentInstance()
												.getExternalContext()
												.redirect(this.outcome);
									} catch (Exception e1) {
										WAMCPUiActionListener.this
												.handleException(e1);
									}
								}
							});
				}
			} catch (Exception e2) {
				this.handleException(e2);
			}

		} catch (Exception e) {
			this.handleException(e);
		}
	}

	public void openFile(ActionEvent ev) {
		String fileName = (String) ev.getComponent().getAttributes()
				.get("PARAM_MSDESC_FILENAME");
		fileName = WAMCPStorage.filenameForShelfMark(fileName);

		String page = "";
		try {
			String step = (String) ev.getComponent().getAttributes()
					.get("PARAM_OPEN_STEP");

			boolean readOnly = false;
			// FIXME remove this when the Spring security method interception
			// gets to its senses
			// TODO create a step for the released MsDescs
			if (this.wamcpStorage.manuallyCheckPermission(fileName, step)) {

				if ("EDIT".equals(step)) {
					page = "EditForm.iface";
				} else if ("REVIEW1".equals(step)) {
					page = "Review1.iface";
				} else if ("REVIEW2".equals(step)) {
					page = "Review2.iface";
				} else if ("DONE".equals(step)) {
					page = "Released.iface";
					readOnly = true;
				} else {
					throw new WAMCPException(
							"No or unkown workflow step provided!");
				}

			} else {
				page = "ViewForm.iface";
				// YA 20100919 Open Read only if the permissions granted is not
				// enough for write access
				readOnly = true;
				// throw new WAMCPGeneralCorrectableException("Access Denied");
				// END YA 20100919
			}

			if (readOnly) {
				this.wamcpStorage.openRead(fileName);
			} else {
				this.wamcpStorage.openWrite(fileName);
			}
			FacesContext.getCurrentInstance().getExternalContext()
					.redirect(page);

		} catch (WFSVNArtifactLoadingException e) {
			try {
				if (fileName.equals(this.wamcpStorage.getLoadedFileNameNoExt())) {
					try {
						FacesContext.getCurrentInstance().getExternalContext()
								.redirect(page);
					} catch (Exception e1) {
						WAMCPUiActionListener.this.handleException(e1);
					}
				} else {
					this.confirm(
							ev.getComponent().getParent(),
							"Found live edit session",
							"Found a session that is not properly closed, working on file: "
									+ this.wamcpStorage.getWorkingFile()
											.getName(),
							"Close it without saving",
							"Return to this session",
							new WorkflowConfirmListener(ev.getComponent()
									.getParent(), this.wamcpSessionBBean, page) {

								@Override
								protected void doAccept(ActionEvent ev) {
									try {
										WAMCPUiActionListener.this.wamcpStorage
												.close(false);
										WAMCPUiActionListener.this.sdxeMediator
												.rollback();
										WAMCPUiActionListener.this
												.uninitEditSession();
									} catch (Exception e1) {
										WAMCPUiActionListener.this
												.handleException(e1);
									}
								}

								@Override
								protected void doCancel(ActionEvent ev) {
									try {
										FacesContext.getCurrentInstance()
												.getExternalContext()
												.redirect(this.outcome);
									} catch (Exception e1) {
										WAMCPUiActionListener.this
												.handleException(e1);
									}
								}
							});
				}
			} catch (Exception e2) {
				this.handleException(e2);
			}

		} catch (Exception e) {
			this.handleException(e);
		}
	}

	@SuppressWarnings("deprecation")
	public void refToShownFacs(ActionEvent ev) {
		try {
			Map<String, Object> attrs = ev.getComponent().getAttributes();

			String id = (String) attrs.get(PARAM_ID_FOR_ELT);

			String imageName = this.albumBBean.getImage().getName();

			DomTreeController domCont = this.sdxeMediator
					.getDomTreeController();

			this.doCommitToRealDom();
			Document domTot = domCont.getDomTotal();
			Element root = domTot.getRootElement();

			Namespace nsXp = DomTreeController.acquireNamespace(
					root.getNamespaceURI(), root, true);
			String pfx = nsXp.getPrefix();
			if ((pfx != null) && !pfx.isEmpty()) {
				pfx += ":";
			}

			String facsIdForNameStr = "//" + pfx + "facsimile/" + pfx
					+ "surface/" + pfx + "graphic[@url=$VARURL]/parent::" + pfx
					+ "surface/@xml:id";

			XPath facsIdForNameXP = XPath.newInstance(facsIdForNameStr);
			facsIdForNameXP.addNamespace(nsXp);

			facsIdForNameXP.setVariable("VARURL", imageName);

			Object facsIdObj = facsIdForNameXP.selectSingleNode(domTot);
			String facsId = "";
			if (facsIdObj == null) {

				// / Be DRY between this and WAMCPStorage.createFromDoc

				String facsParentStr = "//" + pfx + "facsimile";
				XPath facsParentXP = XPath.newInstance(facsParentStr);
				facsParentXP.addNamespace(nsXp);

				Object facsParentObj = facsParentXP.selectSingleNode(domTot);

				if (facsParentObj != null) {
					Element facsParentElt = (Element) facsParentObj;

					Namespace nsElt = root.getNamespace();

					Element surfaceElt = new Element("surface", nsElt);
					facsParentElt.addContent(surfaceElt);

					facsId = "i"
							+ imageName
									.substring(imageName.lastIndexOf('_') + 1);
					// we don't store the extension any more: facsId =
					// facsId.substring(0, facsId.lastIndexOf('.'));

					surfaceElt.setAttribute("id", facsId,
							Namespace.XML_NAMESPACE);

					Element graphicElt = new Element("graphic", nsElt);
					surfaceElt.addContent(graphicElt);

					graphicElt.setAttribute("url", imageName);
				} else {
					throw new DomBuildException(
							"Template is corrupted: No facsimile");
				}
			} else {
				facsId = ((Attribute) facsIdObj).getValue();
			}

			String facsRef = "#" + facsId;

			UISelectOne facsSelect = (UISelectOne) FacesUtils
					.findComponentInRoot(id);

			DomValueBinder binder = this.bindersBean.getDomValueBinders().get(
					id);

			List<String> valueList = binder.getValueList();

			String clientId = facsSelect.getClientId(FacesContext
					.getCurrentInstance());

			if (valueList.indexOf(facsRef) == -1) {
				valueList.add(facsRef);
				binder.setValueList(valueList);

				UISelectItem selectItem = IceCompsFactory.selectItem(facsRef,
						facsRef); // imageName);

				facsSelect.getChildren().add(selectItem);

				// The component gets re-rendered only when it recieves its
				// first child
				if (facsSelect.getChildCount() == 1) {
					// We should select the new association.. but the component
					// doesn't get rerendered
					facsSelect.setValue(facsRef);
				}

				FacesUtils.ManagerinvFacesUtils
						.addInfoMessage(clientId, Messages.getString("en",
								"WAMCPUiGenFactory.AssocAdded"));
			} else {

				FacesUtils.ManagerinvFacesUtils.addErrorMessage(clientId,
						Messages.getString("en",
								"WAMCPUiGenFactory.AlreadyAssoced"));

			}

			// We have to do it here.. or implement some new way to do it with
			// the other bean of the field
			binder.syncWithDom(true);

		} catch (Exception e) {
			this.handleException(e);
		}
	}

	@SuppressWarnings("deprecation")
	public void releaseMsDesc(ActionEvent ev) {
		try {
			// YA20100810 Unlock

			// this.runCheckPresence(true, true);
			long revNum = this.doSaveToStorage(true, true);
			// this.gaurdUnsavedChanges();
			// END YA20100810 Unlock

			// this.checkForRequiredVals(true);

			DomTreeController domController = this.sdxeMediator
					.getDomTreeController();

			// YA 20110209 - Remove empty elements from all of the document..
			// not under any instance!
			XSASDXEDriver xsaSdxeDriver = new XSASDXEDriver(this.sdxeMediator,
					this.xsaDoc);
			xsaSdxeDriver.deleteEmptyElements();
			// END YA 20110209

			Document toRelease = (Document) domController.getDomTotal().clone();

			XPath presenceIndicatorsXP = XPath
					.newInstance("//*[normalize-space(text())='WAMCPPresenceIndicator']");
			for (Object obj : presenceIndicatorsXP.selectNodes(toRelease)) {
				if (obj instanceof Element) {
					((Element) obj).setText("");
				} else if (obj instanceof Attribute) {
					// This cannot happen.. I know but I'm just being stupid!
					((Attribute) obj).setValue("");
				}
			}

			// TODO: run an XSL that cleans any mistakes up and assures validity
			// ///////////////////////////////////////////////////////////////
			// Clean up of some mistakes

			Namespace xpNs = toRelease.getRootElement().getNamespace();

			String pfx = xpNs.getPrefix();

			if ((pfx == null) || pfx.isEmpty()) {
				if ((xpNs.getURI() != null) && !xpNs.getURI().isEmpty()) {
					pfx = "dns:";
					xpNs = Namespace.getNamespace("dns", xpNs.getURI());
				} else {
					pfx = "";
				}
			} else {
				pfx = pfx + ":";
			}
			XPath titleNormalizedLangXP = XPath.newInstance("/" + pfx + "TEI/"
					+ pfx + "text/" + pfx + "body/" + pfx + "div/" + pfx
					+ "msDesc/" + pfx + "msPart/" + pfx + "msContents/" + pfx
					+ "msItem/" + pfx + "title[@type='normalised']");
			titleNormalizedLangXP.addNamespace(xpNs);

			for (Object obj : titleNormalizedLangXP.selectNodes(toRelease)) {

				((Element) obj).setAttribute("lang", "ara-Latn",
						Namespace.XML_NAMESPACE);

			}

			// YM 20110614 Adding required attribute @type='line-height' under
			// the element
			// /TEI/text/body/div/msDesc/physDesc/objectDesc/layoutDesc/layout/dimensions
			//

			XPath Mistaradimensions = XPath.newInstance("/" + pfx + "TEI/"
					+ pfx + "text/" + pfx + "body/" + pfx + "div/" + pfx
					+ "msDesc/" + pfx + "physDesc/" + pfx + "objectDesc/" + pfx
					+ "layoutDesc/" + pfx + "layout[@n='mistara']/" + pfx
					+ "dimensions");
			Mistaradimensions.addNamespace(xpNs);
			for (Object obj : Mistaradimensions.selectNodes(toRelease)) {

				((Element) obj).setAttribute("type", "line-height");
				((Element) obj).setAttribute("unit", "mm");

			}

			// ///////////////////////////////////////////////////////////////////////////////

			// ///////////////////////////////////////////////////////////////////////////////

			String logFilePath = URLPathStrUtils.appendParts(this.wamcpStorage
					.getUserDir().getCanonicalPath(),
					this.wamcpStorage.getLoadedFileNameNoExt()
							+ "_validation.log");

			// Guard unsaved changes already covers the case that there are
			// uncommmited changes
			switch (XSASDXEDriver.validate(toRelease, this.xsdFilePath,
					this.validateAgainst, logFilePath)) {
			case 0: // success
				this.showMessageWithDownloadLink("XML valid, log saved: ",
						logFilePath);
				break;
			case 1: // warnings exist
				this.showMessageWithDownloadLink(
						"XML valid with warnings, log saved: ", logFilePath);
				break;
			case 2: // errors exist
				this.showMessageWithDownloadLink("XML invalid, log saved: ",
						logFilePath);
				return;

			}
			//TODO: Michael release TEI XML file here so we need to construct and release also the MARC21 XML File 
			
			Element metaAlbumElt = this.galleryBean
					.getMetaAlbumEltForMetadataFile(this.wamcpStorage
							.getArtifact().getArtifactName());

			this.wamcpStorage.release(toRelease, metaAlbumElt);

			this.wamcpStorage.moveArtifactInWorkflow("ACCEPT");

			// To store the new workflow stage (released) in the index
			// And tokenize all dotless words for better search for dotless
			// variants
			new WAMCPOldArabicStorage((WAMCPIndexedStorage) this.wamcpStorage)
					.reindexUponRelease(revNum);
			// this.wamcpStorage.commit(
			// "This revisoin was made availble in front end! Logged in as: "
			// + this.wamcpSessionBBean.getUserName());

			this.wamcpStorage.close(true);

			this.doClosePart2();

		} catch (Exception e) {
			this.handleException(e);
		}
	}

	// public void permitDelete(ActionEvent ev) {
	// String fileName = (String) ev.getComponent().getAttributes().get(
	// "PARAM_MSDESC_FILENAME");
	// try {
	// this.wamcpStorage.permitDelete(fileName);
	// } catch (Exception e) {
	// this.handleException(e);
	// }
	// }
	public void requestDelete(ActionEvent ev) {
		String fileName = (String) ev.getComponent().getAttributes()
				.get("PARAM_MSDESC_FILENAME");
		try {
			this.wamcpStorage.requestDelete(fileName);

			this.setHeaderMsg(
					"Delete requested, an Admin should now perform it",
					StyledMsgTypeEnum.SUCCESS);
		} catch (Exception e) {
			this.handleException(e);
		}
	}

	public void revertToBase(ActionEvent ev) {
		try {
			this.confirm(
					ev.getComponent().getParent(),
					"Are you sure?",
					"Any changes in your work space copy will be lost even if they are saved, do you want to continue?",
					"Yes.. I need this", "No.. maybe I need to Rollback",
					new AbstractConfirmListener(ev.getComponent().getParent(),
							this.wamcpSessionBBean) {

						@Override
						protected void doAccept(ActionEvent ev) {
							try {
								String shelfMark = WAMCPUiActionListener.this.wamcpStorage
										.getLoadedFileNameNoExt();

								WAMCPUiActionListener.this.wamcpStorage
										.revert();

								WAMCPUiActionListener.this.uninitEditSession();

								WAMCPUiActionListener.this.wamcpStorage
										.openWrite(shelfMark);

								WAMCPUiActionListener.this.initEditSession();

								WAMCPUiActionListener.this.doGoToAreas(true);
							} catch (Exception e1) {
								WAMCPUiActionListener.this.handleException(e1);
							}
						}

						@Override
						protected void doCancel(ActionEvent ev) {
							// Nothing

						}
					});
		} catch (Exception e) {
			this.handleException(e);
		}
	}

	public void rollbackUnsaved(ActionEvent ev) {
		try {

			this.confirm(
					ev.getComponent().getParent(),
					"Are you sure?",
					"Unsaved changes will be lost, would you like to continue?",
					"Yes.. undo all unsaved changes",
					"No.. maybe I need to use Reset",
					new AbstractConfirmListener(ev.getComponent().getParent(),
							this.wamcpSessionBBean) {

						@Override
						protected void doAccept(ActionEvent ev) {
							try {
								// This rollsback uncommitted changes only
								// WAMCPUiActionListener.this.sdxeMediator
								// .rollback();
								// WAMCPUiActionListener.this
								// .doGoToAreas();

								// This reverts to saved
								String shelfMark = WAMCPUiActionListener.this.wamcpStorage
										.getLoadedFileNameNoExt();

								WAMCPUiActionListener.this.uninitEditSession();

								WAMCPUiActionListener.this.wamcpStorage
										.openWrite(shelfMark);

								WAMCPUiActionListener.this.initEditSession();

								WAMCPUiActionListener.this.doGoToAreas(true);
							} catch (Exception e1) {
								WAMCPUiActionListener.this.handleException(e1);
							}
						}

						@Override
						protected void doCancel(ActionEvent ev) {
							// Nothing
						}
					});

		} catch (Exception e) {
			this.handleException(e);
		}

	}

	private boolean runCheckPresence(boolean throwOnMissing, boolean removeEmpty)
			throws SDXEException, JDOMException {
		if (!this.doCommitToRealDom()) {
			this.setHeaderMsg(
					"Unacceptable mixed values exist, revise all tabs",
					StyledMsgTypeEnum.ERROR);
			return false; // To show the messages
		}
		XSAInstance instForDomCurr = this.uiGenDomListener
				.getInstForDomCurrent();
		XSASDXEDriver xsaSdxeDriver = new XSASDXEDriver(this.sdxeMediator,
				this.xsaDoc);
		try {
			xsaSdxeDriver.checkPresenceInInstance(instForDomCurr,
					throwOnMissing, removeEmpty);

		} finally {
			if (removeEmpty) {
				// Because the empty elements are gone so we have to regenerate
				// UI
				this.doGoToAreas(true);
			}
			// If we do this it will regenerate the empty elements
			// // regen area's ui
			// String currContainerId =
			// XSAUiGenFactoryAbstract.generateIdForInstance(instForDomCurr);
			// UIComponent currContainerComp =
			// FacesUtils.findComponentInRoot(currContainerId);
			//
			// this.uiGenDomListener.clearUI();
			// XSAUiGenFactoryAbstract uiGenFactory = this.createUiGenFactory();
			//
			// XSAUiGenBuilder uiGenBuilder = new XSAUiGenBuilder(this.xsaDoc,
			// uiGenFactory,
			// this.sdxeMediator.getSugtnTreeController());
			//
			// try {
			// uiGenBuilder.generateUi((UIPanel) currContainerComp,
			// this.uiGenDomListener
			// .getSugtnForDomCurrent(), instForDomCurr);
			// } catch (UIGenerationException e) {
			// this.handleException(e);
			// }

		}
		return true;
	}

	public void saveToStorage(ActionEvent ev) {
		try {

			// this.runCheckPresence(false, true);
			this.doSaveToStorage(false, true);

			if (ev != null) {
				String msg = (String) ev.getComponent().getAttributes()
						.get(XSAConstants.PARAM_SAVE_MSG);
				this.showMessageWithDownloadLink(msg, this.wamcpStorage
						.getWorkingFile().getCanonicalPath());
			}
		} catch (Exception e) {
			// NO: this.sdxeMediator.getDomTreeController().setDomTotal(backup);
			this.handleException(e);
		}
	}

	public void saveToWorkspace(ActionEvent ev) {

		try {
			if (!this.doSave()) {
				return;
			}

			String msg = (String) ev.getComponent().getAttributes()
					.get(XSAConstants.PARAM_SAVE_MSG);
			this.showMessageWithDownloadLink(msg, this.wamcpStorage
					.getWorkingFile().getCanonicalPath());
		} catch (Exception e) {
			this.handleException(e);
		}
	}

	public void search(ActionEvent ev) {

		String criteria = this.searchBBean.getCriteria();

		try {
			this.searchBBean.doSearch();

			FacesContext.getCurrentInstance().getExternalContext()
					.redirect("SearchRes.iface");

		} catch (Exception e) {
			this.handleException(e);
		}
	}

	public void searchFilterRes(ActionEvent ev) {
		Map<String, Object> attrs = ev.getComponent().getAttributes();
		String fieldName = (String) attrs.get("PARAM_FILTER_FIELDNAME");
		String fieldVal = (String) attrs.get("PARAM_FILTER_FIELDVALUE");

		try {
			// this.wamcpSessionBBean.getSearchBBean()
			this.searchBBean.filterRes(fieldName, fieldVal);

			// TODO add filter tag with X to unfilter
		} catch (Exception e) {
			this.handleException(e);
		}
	}

	public void searchUnfilterRes(ActionEvent ev) {
		Map<String, Object> attrs = ev.getComponent().getAttributes();
		String fieldName = (String) attrs.get("PARAM_FILTER_FIELDNAME");
		String fieldVal = (String) attrs.get("PARAM_FILTER_FIELDVALUE");

		try {
			// this.wamcpSessionBBean.getSearchBBean()
			this.searchBBean.unfilterRes(fieldName, fieldVal);

			// TODO add filter tag with X to unfilter
		} catch (Exception e) {
			this.handleException(e);
		}
	}

	public void setGalleryBean(WAMCPGalleryBeanSVNStoredMetaGallery galleryBean) {
		this.galleryBean = galleryBean;
	}

	@Override
	public void setHeaderMsg(String message, StyledMsgTypeEnum msgType) {

		super.setHeaderMsg(message, msgType);

		JavascriptContext
				.addJavascriptCall(
						FacesContext.getCurrentInstance(),
						"document.getElementById('pnlMessagesParent_UNIQUE_ID').style.display='inline';");
	}

	public void setSchemaLocations(String schemaLocations) {
		this.schemaLocations = schemaLocations;
	}

	public void setSearchBBean(SearchBBean searchBBean) {
		this.searchBBean = searchBBean;
	}

	public void setStorage(WAMCPIndexedStorage storage) {
		this.wamcpStorage = storage;
		super.setStorage(storage);
	}

	@Override
	public void setStorage(XSAIStorage storage) {
		this.wamcpStorage = (WAMCPStorage) storage;
		super.setStorage(storage);
	}

	public void setValidateAgainst(String validateAgainst) {
		this.validateAgainst = validateAgainst;
	}

	public void setXsdFilePath(String xsdFilePath) {
		this.xsdFilePath = xsdFilePath;
	}

	@SuppressWarnings("deprecation")
	public void showFacs(ActionEvent ev) {
		try {
			Map<String, Object> attrs = ev.getComponent().getAttributes();

			String idElt = (String) attrs.get(PARAM_ID_FOR_ELT);

			UISelectOne facsSelect = (UISelectOne) FacesUtils
					.findComponentInRoot(idElt);

			String facsRef = JSFValueGetter.getString(facsSelect);

			DomTreeController domCont = this.sdxeMediator
					.getDomTreeController();

			this.doCommitToRealDom();
			Document domTot = domCont.getDomTotal();

			String facsName = TEIEnrichXAO.facsGraphicNameForFacsRef(facsRef,
					domTot);
			if ((facsName == null) || facsName.isEmpty()) {
				// This is obvious.. the message will be confusing!
				// String clientId =
				// facsSelect.getClientId(FacesContext.getCurrentInstance());
				// FacesUtils.ManagerinvFacesUtils.addInfoMessage(clientId,
				// Messages.getString("en",
				// "WAMCPUiGenFactory.NoFacsRefSelected"));
			} else {
				this.albumBBean.doSelectImageName(facsName);
			}
		} catch (Exception e) {
			this.handleException(e);
		}
	}

	@SuppressWarnings("deprecation")
	public void testValidation(ActionEvent ev) {
		try {
			String logFilePath = URLPathStrUtils.appendParts(this.wamcpStorage
					.getUserDir().getCanonicalPath(),
					this.wamcpStorage.getLoadedFileNameNoExt()
							+ "_validation.log");

			switch (XSASDXEDriver.validate(this.sdxeMediator
					.getDomTreeController().getDomTotal(), this.xsdFilePath,
					this.validateAgainst, logFilePath)) {
			case 0: // success
				this.showMessageWithDownloadLink(Messages.getString("en",
						"WAMCPUiActionListener.XMLValidationOk"), logFilePath);
				break;
			case 1: // warnings exist
				this.showMessageWithDownloadLink(Messages.getString("en",
						"WAMCPUiActionListener.XMLValidationWarn"), logFilePath);
				break;
			case 2: // errors exist
				this.showMessageWithDownloadLink(Messages.getString("en",
						"WAMCPUiActionListener.XMLValidationError"),
						logFilePath);
				return;
			}
		} catch (Exception e) {
			this.handleException(e);
		}

	}

	@Override
	protected void uninitEditSession() {

		try {
			this.sdxeMediator.close();

			this.uiGenDomListener.uninit();

			this.wamcpStorage.closeWorkingFile();

			this.albumBBean.closeAlbum();
		} catch (Exception e) {
			this.handleException(e);
		}

	}

	public void unrequestDelete(ActionEvent ev) {
		String fileName = (String) ev.getComponent().getAttributes()
				.get("PARAM_MSDESC_FILENAME");
		try {
			this.wamcpStorage.unrequestDelete(fileName);

			this.setHeaderMsg("Delete request cancelled",
					StyledMsgTypeEnum.SUCCESS);
		} catch (Exception e) {
			this.handleException(e);
		}
	}

}
