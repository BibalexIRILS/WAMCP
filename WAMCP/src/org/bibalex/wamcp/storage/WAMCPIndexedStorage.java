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

﻿package org.bibalex.wamcp.storage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.ServletContext;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.bibalex.gallery.exception.BAGException;
import org.bibalex.gallery.model.BAGThumbnail;
import org.bibalex.sdxe.binding.SDXEMediatorBean;
import org.bibalex.sdxe.controller.SDXEMediator;
import org.bibalex.sdxe.controller.SugtnTreeContollerCache;
import org.bibalex.sdxe.controller.SugtnTreeController;
import org.bibalex.sdxe.exception.DomBuildChangesException;
import org.bibalex.sdxe.exception.DomBuildException;
import org.bibalex.sdxe.exception.SDXEException;
import org.bibalex.sdxe.exception.SugtnException;
import org.bibalex.sdxe.suggest.model.dom.ISugtnTypedNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnDeclNode;
import org.bibalex.sdxe.suggest.model.dom.SugtnTypeDeclaration;
import org.bibalex.sdxe.xsa.application.XSASDXEDriver;
import org.bibalex.sdxe.xsa.exception.XSAException;
import org.bibalex.sdxe.xsa.model.XSADocCache;
import org.bibalex.sdxe.xsa.model.XSADocument;
import org.bibalex.sdxe.xsa.model.XSAInstance;
import org.bibalex.sdxe.xsa.model.XSALocator;
import org.bibalex.util.URLPathStrUtils;
import org.bibalex.wamcp.exception.WAMCPException;
import org.bibalex.workflow.storage.WFSVNArtifactLoadingException;
import org.bibalex.workflow.storage.WFSVNClient.WFSVNEntry;
import org.bibalex.workflow.storage.WFSVNException;
import org.bibalex.workflow.storage.WFSVNLockedException;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;

public class WAMCPIndexedStorage extends WAMCPStorage implements Serializable {
	public static class IndexEntry {
		public String name;
		public long revision;
// We get it from DB, so let's keep it that way: public String wfStageName;
		public String lastModifiedBy;
		public Date lastModifiedDate;
		public HashMap<String, String> summaryFieldsValues = new HashMap<String, String>();
		public String wfStep;
		
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7291992014509294840L;
	
	protected static Logger LOG = Logger.getLogger("org.bibalex.wamcp.ixedStorage");
	protected final String CONFIG_SOLR_SERVER;
	protected final String CONFIG_PUBLISH_SOLR_SERVER;
	
// protected static WeakHashMap<String, SolrServer> solrServerMap = new WeakHashMap<String, SolrServer>();
	
	public static String IXED_STORAGE_CONCATED_VALUE_DELIMETIR = " | ";
	
	public static final HashMap<String, Integer> COMMONERA_CALENDARS_OFFSET = new HashMap<String, Integer>(
			13);
	
	static {
		
		// no need for new Integer(0), because of the newly introduced Java Boxing
		
		COMMONERA_CALENDARS_OFFSET.put(null, 0);
		
		COMMONERA_CALENDARS_OFFSET.put("Gregorian", 0);
		COMMONERA_CALENDARS_OFFSET.put("Julian", 0);
		COMMONERA_CALENDARS_OFFSET.put("AM", -5510);
		COMMONERA_CALENDARS_OFFSET.put("Hijri-shamsi", 1911);
		COMMONERA_CALENDARS_OFFSET.put("Hijri-qamari", 623);
		COMMONERA_CALENDARS_OFFSET.put("Coptic-EoM", 285);
		COMMONERA_CALENDARS_OFFSET.put("Alexandrian", -313);
		COMMONERA_CALENDARS_OFFSET.put("Iranian-Yazdigird", 633);
		COMMONERA_CALENDARS_OFFSET.put("Iranian-Jalali", 1079);
		COMMONERA_CALENDARS_OFFSET.put("Spanish", -38);
		COMMONERA_CALENDARS_OFFSET.put("Ilahi", 1584);
		COMMONERA_CALENDARS_OFFSET.put("Hindu", -58);
		COMMONERA_CALENDARS_OFFSET.put("unknown", 0);
		COMMONERA_CALENDARS_OFFSET.put("other", 0);
	}
	
	protected static final HashMap<String, String> XSD_TYPE_SOLR_TYPE_PFX_MAP = new HashMap<String, String>();
	
	static {
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put(SugtnTypeDeclaration.XSD_BASE_TYPE_NAME,
				"text");
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put(SugtnTypeDeclaration.XSD_BASE_SIMPLE_TYPE_NAME,
				"text");
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put(null, "text");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("base64Binary", "binary");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("boolean", "boolean");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("byte", "binary");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("date", "tdate");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("dateTime", "tdate");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("decimal", "tlong");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("double", "tdouble");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("duration", "tdate");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("float", "tfloat");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("gYearMonth",
				"tdate");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("gYear", "tdate");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP
				.put("gMonthDay", "tdate");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("gMonth", "tdate");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("gDay", "tdate");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("hexBinary", "binary");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("int", "tint");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("integer", "tint");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("long", "tlong");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("NOTATION", "string");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("QName", "string");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("short", "tint");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("string", "textExact");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("time", "tdate");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("unsignedByte", "tint");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("unsignedInt", "tint");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("unsignedShort", "tint");
		
		XSD_TYPE_SOLR_TYPE_PFX_MAP.put("anyURI", "string");
		
	}
	public static final String WAMCP_INDEX_IDFIELDNAME = "id";
	public static final String WAMCP_INDEX_lastModifiedByFieldNAME = "Last.Modified.By";
	public static final String WAMCP_INDEX_FIELDNAME_revision = "revision";
	public static final String WAMCP_INDEX_FIELDNAME_timestamp = "timestamp";
	
	public static final String WAMCP_INDEX_FIELDNAME_WFSTEP = "Workflow.Stage";
	
	public static final String[] IXED_STORAGE_CATCHALLFIELDNAMES = new String[] { "AllTextEN",
			"AllTextAR", "AllExactEN", "AllExactAR" };
	
	protected static final String WAMCP_INDEX_FIELDNAME_COVERCATION = "Cover.Caption";
	
	protected static final String WAMCP_INDEX_FIELDNAME_COVERIMAGE = "Cover.Image";
	static CommonsHttpSolrServer solrServer = null;
	static CommonsHttpSolrServer publishSolrServer = null;
	
	public static SolrServer getSolrServer(String url) throws WFSVNException {
		
// if (!solrServerMap.containsKey(url)) {
		
		/*
		 * CommonsHttpSolrServer is thread-safe and if you are using the following constructor,
		 * you *MUST* re-use the same instance for all requests. If instances are created on
		 * the fly, it can cause a connection leak. The recommended practice is to keep a
		 * static instance of CommonsHttpSolrServer per solr server url and share it for all requests.
		 * See https://issues.apache.org/jira/browse/SOLR-861 for more details
		 */

		if (solrServer == null) {
			try {
				solrServer = new CommonsHttpSolrServer(url);
			} catch (MalformedURLException e) {
				throw new WFSVNException(e);
			}
		}
		return solrServer;
		
		// Should we need any configurations, here they are:
// solrServer.setParser(new XMLResponseParser());
//
// solrServer.setSoTimeout(1000); // socket read timeout
// solrServer.setConnectionTimeout(100);
// solrServer.setDefaultMaxConnectionsPerHost(100);
// solrServer.setMaxTotalConnections(100);
// solrServer.setFollowRedirects(false); // defaults to false
// // allowCompression defaults to false.
// // solrServer side must support gzip or deflate for this to have any effect.
// solrServer.setAllowCompression(true);
// solrServer.setMaxRetries(1); // defaults to 0. > 1 not recommended.
//
// solrServerMap.put(url, solrServer);
// }
//
// return solrServerMap.get(url);
	}
	
	protected final XSADocument xsaDoc;
	
	protected final SDXEMediatorBean sdxeMediator;
	
	protected final HashMap<XSAInstance, String> ixedFieldsNames;
	protected final HashMap<String, String> ixedFieldsTypes;
	
	protected final HashMap<XSAInstance, String> advSearchFieldsNames;
	protected final HashMap<String, String> advSearchFieldsTypes;
	
	// These fields are not exported in the schema.properties
	// because they are only auxilary fields for a special purpose
	protected final HashMap<XSAInstance, String> extraFacetFieldsNames;
	protected final HashMap<String, String> extraFacetFieldsTypes;
	
	protected final SugtnTreeController sugtnTreeController;
	
	protected URI cfgBagApiUri;
	
	protected String quickMessage = null;
	
// // Some constants. + - ! ( ) { } [ ] ^ " ~ * ? : \
// protected static final String LUCENE_ESCAPE_CHARS = "[\\\\\\+\\-\\!\\(\\)\\:\\^\\[\\]\\{\\}\\~\\*\\?\\\"]";
//
// protected static final Pattern LUCENE_PATTERN = Pattern.compile(LUCENE_ESCAPE_CHARS);
// protected static final String REPLACEMENT_STRING = "\\\\$0";
	
	protected String schemaFilePath;
	
	protected String mssToReindex;
	
	protected String CONFIG_URL_BAGAPI_BASE;
	
	protected String CONFIG_SCHEMA_FILE;
	
	protected String CONFIG_ROOTELTNAME;
	
	protected String CONFIG_NAMESPACE;
	
	protected static Pattern arabicUnicodeLetters = Pattern.compile(".*[\u0600-\u06FF]+.*");
	
	public WAMCPIndexedStorage(String CONFIG_URL_BAGAPI_BASE, String CONFIG_SCHEMA_FILE,
			String CONFIG_ROOTELTNAME, String CONFIG_NAMESPACE, SDXEMediatorBean sdxeMediatorBean,
			String CONFIG_SOLR_SERVER, String CONFIG_PUBLISH_SOLR_SERVER)
			throws SugtnException,
			WFSVNException, URISyntaxException {
		
		this.cfgBagApiUri = new URI(CONFIG_URL_BAGAPI_BASE);
		
		this.CONFIG_URL_BAGAPI_BASE = CONFIG_URL_BAGAPI_BASE;
		this.CONFIG_SCHEMA_FILE = CONFIG_SCHEMA_FILE;
		this.CONFIG_ROOTELTNAME = CONFIG_ROOTELTNAME;
		this.CONFIG_NAMESPACE = CONFIG_NAMESPACE;
		this.sdxeMediator = sdxeMediatorBean;
		this.CONFIG_SOLR_SERVER = CONFIG_SOLR_SERVER;
		this.CONFIG_PUBLISH_SOLR_SERVER = CONFIG_PUBLISH_SOLR_SERVER;
		
		this.xsaDoc = XSADocCache.getInstance().xsaDocForSchema(
				CONFIG_SCHEMA_FILE);
		this.sugtnTreeController = SugtnTreeContollerCache.getInstance()
				.sugtnTreeControllerForSchema(
						CONFIG_SCHEMA_FILE, CONFIG_NAMESPACE, CONFIG_ROOTELTNAME);
		this.ixedFieldsNames = new HashMap<XSAInstance, String>();
		this.ixedFieldsTypes = new HashMap<String, String>();
		this.advSearchFieldsNames = new HashMap<XSAInstance, String>();
		this.advSearchFieldsTypes = new HashMap<String, String>();
		
		this.extraFacetFieldsNames = new HashMap<XSAInstance, String>();
		this.extraFacetFieldsTypes = new HashMap<String, String>();
		
		try {
			List<XSAInstance> indexedInsts = this.xsaDoc.getIndexedInsts();
			
			for (XSAInstance xsaInst : indexedInsts) {
				
				String fieldName = xsaInst.getIxFieldName();
				String extraFacetFName = "";
				
				String extraFacetFType = xsaInst.getExtraFacetType();
				if ((extraFacetFType != null) && !extraFacetFType.isEmpty()) {
					extraFacetFName = fieldName + "_" + extraFacetFType;
				}
				
				if (xsaInst.getIxFieldIsSummary()) {
					fieldName += "SSS";
				}
				
				// Add the prefix that will map this to the correct dynamic field
				// SDXEMediator could be still uninitialized so we can't use its tree
				SugtnDeclNode declNode = this.sugtnTreeController
						.getNodeAtSugtnPath(
						xsaInst.getLocator().asString());
				
				SugtnTypeDeclaration origType = null;
				if (declNode instanceof ISugtnTypedNode) {
					origType = ((ISugtnTypedNode) declNode).getType();
				}
				
				SugtnTypeDeclaration sugtnType = XSASDXEDriver.overrideSugtnType(origType, xsaInst);
				
				String solrFieldType = xsaInst.getSolrType();
				
				if ((solrFieldType == null) || solrFieldType.isEmpty()) {
					
					if (sugtnType.isEnumerated()) {
						solrFieldType = XSD_TYPE_SOLR_TYPE_PFX_MAP.get("QName");
					} else {
						solrFieldType = XSD_TYPE_SOLR_TYPE_PFX_MAP.get(sugtnType.getBaseTypeName());
					}
					
				}
				
				fieldName += "_" + solrFieldType;
				
// if (!xsaInst.isSingleton()
// || sugtnType.isListType() ||
// ((declNode instanceof SugtnElementNode)
// && ((SugtnElementNode) declNode).isMultivalued())) {
// fieldName += "_M";
// extraFacetFName += "_M";
// }
				
				this.ixedFieldsNames.put(xsaInst, fieldName);
				this.ixedFieldsTypes.put(fieldName, solrFieldType);
				
				if ((extraFacetFType != null) && !extraFacetFType.isEmpty()) {
					this.extraFacetFieldsNames.put(xsaInst, extraFacetFName);
					this.extraFacetFieldsTypes.put(extraFacetFName, extraFacetFType);
				}
			}
			
			// FIXME make the advances search fields dynamic
			List<XSAInstance> advancedInsts = this.xsaDoc.getAdvancedSearchInsts();
			
			for (XSAInstance advInst : advancedInsts) {
				String fieldName = advInst.getIxFieldName();
				
				this.advSearchFieldsNames.put(advInst, fieldName);
				this.advSearchFieldsTypes.put(fieldName, advInst.getAdvancedSearchType());
			}
			
		} catch (JDOMException e) {
			throw new WFSVNException(e);
		} catch (SugtnException e) {
			throw new WFSVNException(e);
		} catch (XSAException e) {
			throw new WFSVNException(e);
		}
		
		this.schemaFilePath = ((ServletContext) FacesContext
				.getCurrentInstance().getExternalContext().getContext())
				.getRealPath("/Schema/enrich-wamcp.xsd"); // FIXME: Hard coded value!
	}
	
	protected void addFieldValueToDoc(SolrInputDocument doc, String fieldName, Object value,
			String fieldType) {
		String strValue = value.toString();
		
		if ((strValue == null) || strValue.isEmpty() || "FILL IN".equals(strValue)) {
			return;
		}
		
// Not needed while adding.. only when querying
// // Escape Lucene special chars: + - ! ( ) { } [ ] ^ " ~ * ? : \ (not && || )
//
// strValue = LUCENE_PATTERN.matcher(strValue).replaceAll(REPLACEMENT_STRING);
		
		// Guard against values that would cause Solr Internal Server Error
		if ((fieldType == null) || fieldType.isEmpty()) {
			fieldType = this.getSolrTypeForField(fieldName);
		}
		try {
			if (fieldType.startsWith("text")) {
				if (fieldName.indexOf('_') > -1) {
					// the field type of dynamic text fields depends on the language of text
					if (!(fieldName.endsWith("EN") || fieldName.endsWith("AR"))) {
						if (arabicUnicodeLetters.matcher(strValue).matches()) { // contains Arabic
							this.addFieldValueToDoc(doc, fieldName + "AR", strValue, fieldType);
						} else {
							this.addFieldValueToDoc(doc, fieldName + "EN", strValue, fieldType);
						}
						return;
					}
				} else {
					// nothing to do.. but avoid going into the else of numerics
				}
			} else if ("string".equals(fieldType)
					|| "binary".equals(fieldType)) {
				// nothing to do.. but avoid going into the else of numerics
			} else if ("boolean".equals(fieldType)) {
				
				Boolean.parseBoolean(strValue);
				
			} else if ("tdate".equals(fieldType)) {
				
				new SimpleDateFormat().parse(strValue);
				
			} else {
				// numeric tyoes... make sure only chars
				String notAllowed = "[^0-9+-\\.]";
				// TODO use locale
				strValue = strValue.replaceAll(notAllowed, "");
				
				if ("tlong".equals(fieldType)) {
					
					Long.parseLong(strValue);
					
				} else if ("tdouble".equals(fieldType)) {
					
					Double.parseDouble(strValue);
					
				} else if ("tfloat".equals(fieldType)) {
					
					Float.parseFloat(strValue);
					
				} else if ("tint".equals(fieldType)) {
					
					Integer.parseInt(strValue);
					
				}
			}
		} catch (Exception e) {
			
			LOG.warn("Ingoring value (" + strValue
					+ ") that seems to be not acceptable for the field (" + fieldName + ")");
			
			return;
		}
		
		doc.addField(fieldName, strValue);
	}
	
	void assertResponseOk(HttpResponse httpResp) throws Exception {
		int statusCode = httpResp.getStatusLine().getStatusCode();
		
		if (200 != statusCode) {
			throw new Exception("the html file of the doc. cann't be retrieved");
		}
	}
	
	@Override
	public boolean close(boolean forceUnlock) throws SVNException, WFSVNArtifactLoadingException {
		// Is this really necessary?? Does anything require that?
		
		return this.close(forceUnlock, true);
	}
	
	public boolean close(boolean forceUnlock, boolean unloadTempXML) throws SVNException,
			WFSVNArtifactLoadingException {
		
		boolean result = super.close(forceUnlock);
		
		try {
			// Not sure if this is necessary or just code left overs
			if (unloadTempXML) {
				this.galleryBean.unloadTempXml();
			}
		} catch (BAGException e) {
			LOG.error(e, e);
		}
		
		return result;
	}
	
	@Override
	public SVNCommitInfo commit(String commitMessage) throws SVNException, WFSVNException {
		
		SVNCommitInfo result = super.commit(commitMessage);
		
		long revNum;
		
		if (result == null) {
			revNum = -1;
		} else {
			revNum = result.getNewRevision();
		}
		
		this.updateIndex(revNum, true);
		
		// Will we store stage in index? "EDIT"); // TODONOT?? get the stage name in which the commit is allowed
		// dynamically
		
		return result;
	}
	
	/**
	 * this method is used to make some processing on text so as to improve the searching results
	 * @return
	 * @throws XSAException
	 * @throws DomBuildChangesException
	 * @throws WFSVNException
	 * @throws SugtnException
	 * @throws JDOMException
	 */
	@SuppressWarnings("deprecation")
	protected SolrInputDocument constructDocFromXSA() throws XSAException,
			DomBuildChangesException,
			WFSVNException, SugtnException, JDOMException {
// if (!this.initialized) {
// this.init();
// }
		SolrInputDocument result = new SolrInputDocument();
		
		boolean iniatlizedSdxe = false;
		
		if (!this.sdxeMediator.getDomTreeController().getIsInitialized()) {
			try {
				this.sdxeMediator.open(this.getWorkingFile().getCanonicalPath(),
						this.schemaFilePath);
				XSALocator siteLoc = this.xsaDoc.getSiteLocator();
				this.sdxeMediator.moveToSugtnPath(siteLoc.asString());
			} catch (SDXEException e) {
				throw new WFSVNException(e);
			} catch (IOException e) {
				throw new WFSVNException(e);
			}
			
			iniatlizedSdxe = true;
		}
		
		try {
			List<XSAInstance> indexedInsts = this.xsaDoc.getIndexedInsts();
			
			for (XSAInstance xsaInst : indexedInsts) {
				
				String fieldName = this.ixedFieldsNames.get(xsaInst);
				String solrFieldType = this.ixedFieldsTypes.get(fieldName);
				
				String advFieldName = this.advSearchFieldsNames.get(xsaInst);
				// FIXME uncomment this, and index advanced search according to its type not the normal search type!
// String advFieldType = (advFieldName != null ? this.advSearchFieldsTypes
// .get(advFieldName) : null);
				
				String extraFacetFName = this.extraFacetFieldsNames.get(xsaInst);
				String extraFacetFType = (extraFacetFName != null ? this.extraFacetFieldsTypes
						.get(extraFacetFName) : null);
				
				// Now find the nodes that must be indexed
				// YA 20110221 Index all occurences in all base shifts
// String allBrosStr = xsaInst.getXPathToAllOccsInAllConts();
				String allBrosStr = xsaInst.getXPathToAllOccsInAllBaseShifts();
				// END YA 20110221
				
				XPath allBrosXp = this.sdxeMediator.getDomTreeController()
						.createXPathWithNS(allBrosStr);
				
				// this is part of the commit procedure so there can't be unsaved changes
				List allBros = allBrosXp.selectNodes(this.sdxeMediator.getDomTreeController()
						.getDomTotal());
				
				for (Object bro : allBros) {
					
					String value = null;
					
					if (bro instanceof Element) {
// try {
						value = ((Element) bro).getValue();
// Why would we index XML tags?
// JDOMUtils.getElementContentAsString((Element) bro, Format
// .getCompactFormat());
// value = value.replaceAll("<", "&lt;");
// value = value.replaceAll(">", "&gt;");
// } catch (IOException e) {
// throw new XSAException(e);
// }
						
						// And there is this Folio reference in many values.. let's strip it out
						// And those stupid white spaces.. nuke 'em
						value = value.replaceAll("[Ff]ol\\.\\ ?\\d+[ab]\\.?\\ ?\\d*", "").trim()
								.replaceAll("\\s+", " ");
						
					} else if (bro instanceof Attribute) {
						value = ((Attribute) bro).getValue();
					} else {
						throw new WFSVNException("Bad object type returned from: " + allBrosStr);
					}
					
					if ((extraFacetFType != null) && !extraFacetFType.isEmpty()
							&& !extraFacetFType.equals(solrFieldType)) {
						if ("string".equals(extraFacetFType)) {
							String facetVal = value.length() == 0 ? value : ArabicNormalizer
									.normalize(value);
// facetVal = "\""+facetVal+"";
							if ("Tahmid".equals(xsaInst.getIxFieldName())) {
								facetVal = facetVal.replaceAll("[ًٌٍَُِّْ]", "");
								if (facetVal.startsWith("الحمد")) {
									facetVal = facetVal.replaceFirst("الحمد", "").trim();
								}
								
								if (facetVal.startsWith("لله")) {
									facetVal = facetVal.replaceFirst("لله", "").trim();
								}
// All diacretics are now removed
// // sometimes a shaddah is explicitly inserted
// if (facetVal.startsWith("للّه")) {
// facetVal = facetVal.replaceFirst("للّه", "").trim();
// }
								
								// other times they want to avoid the automatic shaddah
								if (facetVal.startsWith("للـه")) {
									facetVal = facetVal.replaceFirst("للـه", "").trim(); // to avoid the Shaddah they
// add a legator
								}
								if (facetVal.startsWith("لـله")) {
									facetVal = facetVal.replaceFirst("لـله", "").trim(); // to avoid the Shaddah they
// add a legator
								}
								if (facetVal.startsWith("لـلـه")) {
									facetVal = facetVal.replaceFirst("لـلـه", "").trim(); // to avoid the Shaddah they
// add a legator
								}
								
								if (facetVal.startsWith("الذى")) {
									facetVal = facetVal.replaceFirst("الذى", "").trim();
								}
								if (facetVal.startsWith("الذي")) {
									facetVal = facetVal.replaceFirst("الذي", "").trim();
								}
								
							}
							StringTokenizer tahmidTokenz = new StringTokenizer(facetVal, " ",
									false);
							StringBuilder facetValBuilder = new StringBuilder();
							int i = 0;
							while (tahmidTokenz.hasMoreTokens() && (i < 20)) { // get max 25 words of the string
								String token = tahmidTokenz.nextToken();
								
								facetValBuilder.append(token + " ");
								
								if (token.length() >= 3) {
									++i;
								}
								
							}
							
							facetVal = facetValBuilder.toString();
// else if ("Provenance.Owners.Readers".equals(xsaInst.getIxFieldName())) {
							
// if (facetVal.startsWith("Note of ownership:")) {
// facetVal = facetVal.replaceFirst("Note of ownership\\:", "")
// .trim();
// }
// }
							this.addFieldValueToDoc(result, extraFacetFName, facetVal,
									extraFacetFType);
						} // TODO rest of types for extra facet
					}
					
					// The case of the date
					if (xsaInst.getIxFieldName().endsWith("Year") && (value != null)
							&& !value.isEmpty()) {
						value = value.trim();
						int lastSpace = value.lastIndexOf(' ');
						if (lastSpace != -1) {
							value = value.substring(lastSpace + 1); // the last part
						}
						try {
							long normalizedValue = Long.valueOf(value);
							
							String calendar = xsaInst.getDomRep().getAttributeValue("calendar");
							
							normalizedValue += COMMONERA_CALENDARS_OFFSET.get(calendar);
							
							value = "" + normalizedValue;
						} catch (NumberFormatException e) {
							LOG.debug("Skipping the indexing of year from the value: " + value);
							continue;
						}
					}
					
					if ("boolean".equals(solrFieldType)) {
						if ((value != null)
								&& !value.isEmpty()
								&& !("none".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value)
										|| "false".equalsIgnoreCase(value) || "0"
										.equalsIgnoreCase(value))) {
							value = "true";
						} else {
							value = "false";
						}
					}
					
					// The case of the shelfmark for sorting quasi-numerically
					if (fieldName.startsWith("Shelfmark")) {
						String sortableShelfmark = value.replace(' ', '_');
						int lastUnderscore = sortableShelfmark.lastIndexOf('_');
						if (lastUnderscore > -1) {
							sortableShelfmark = sortableShelfmark.substring(0, lastUnderscore);
						}
						
						String actualShelfmark = value.substring(lastUnderscore + 1);
						char lastChar = actualShelfmark.charAt(actualShelfmark.length() - 1);
						boolean endWithALetter = Character.isLetter(lastChar);
						if (endWithALetter) {
							actualShelfmark = actualShelfmark
									.substring(0, actualShelfmark.length() - 1);
						}
						while (actualShelfmark.length() < 4) {
							actualShelfmark = '0' + actualShelfmark;
						}
						if (endWithALetter) {
							actualShelfmark += lastChar;
						}
						sortableShelfmark += '_' + actualShelfmark;
						
						this.addFieldValueToDoc(result, "Sortable.Shelfmark", sortableShelfmark,
								"string");
					}
					// all fields are now multivalues.. very few were not
// if (fieldName.endsWith("_M") && !solrFieldType.startsWith("text")) {
					if ((!xsaInst.isSingleton()
							// || sugtnType.isListType() ||
// ((declNode instanceof SugtnElementNode)
// && ((SugtnElementNode) declNode).isMultivalued())
							)
							&& !solrFieldType.startsWith("text")) {
						// Field is multivalued but its content is not text, i.e. will not
						// be analyzed or even tokenized on whitespace or anything..
						// TOKENIZE IT HERE
						
						StringTokenizer tokens = new StringTokenizer(value, " ", false);
						
						while (tokens.hasMoreTokens()) {
							String token = tokens.nextToken();
							
							this.addFieldValueToDoc(result, fieldName, token, null);
							if (advFieldName != null) {
								this.addFieldValueToDoc(result, advFieldName, token,
										this.advSearchFieldsTypes.get(advFieldName));
							}
						}
						
					} else {
						this.addFieldValueToDoc(result, fieldName, value, null);
						if (advFieldName != null) {
							this.addFieldValueToDoc(result, advFieldName, value,
									this.advSearchFieldsTypes.get(advFieldName));
						}
						
					}
				}
			}
		} finally {
			if (iniatlizedSdxe) {
				
				try {
					
					this.sdxeMediator.close();
					
				} catch (DomBuildException e) {
					throw new WFSVNException(e);
				}
				
			}
		}
		return result;
		
	}
	
	@Override
	public SVNCommitInfo delete(String fileName, String commitMessage) throws WFSVNException,
			SVNException, IOException {
		
		SVNCommitInfo result = super.delete(fileName, commitMessage);
		
		// remove the document from index
		SolrServer server = this.getDefaultSolrServer();
		try {
			server.deleteById(fileName);
			server.commit();
		} catch (SolrServerException e) {
			throw new WFSVNException(e);
		}
		
		return result;
		
	}
	
	public String fullHTML(String oaiId) {
		
		String xmlFolderUrlStr = "HTML";
// String localId = BAOAIIDBuilder.localIdFromOaiId(oaiId);
		
		String fileName = oaiId;
		
		String fileUrlStr = URLPathStrUtils
		.appendParts(this.getGalleryBean().getGalleryRootUrlStr());
		
		fileUrlStr = URLPathStrUtils.appendParts(fileUrlStr,
				xmlFolderUrlStr);
		
		fileUrlStr = URLPathStrUtils.appendParts(fileUrlStr,
				fileName + ".html");
		
		if (LOG.isEnabledFor(Priority.WARN)) {
			LOG.warn("fullHTML: " + fileUrlStr);
		}
// System.out.println("**************************** fullHTML: "
// + fileUrlStr + " **********************************");
		return this.streamUrl(fileUrlStr);
	}
	
	public Collection<String> getAllFieldNames() throws WFSVNException {
// if (!this.initialized) {
// this.init();
// }
		Collection<String> result = new ArrayList<String>(this.ixedFieldsNames.size()
				+ this.advSearchFieldsNames.size() + IXED_STORAGE_CATCHALLFIELDNAMES.length);
		
		result.addAll(this.ixedFieldsNames.values());
		result.addAll(this.advSearchFieldsNames.values());
		
		for (String catchAllField : IXED_STORAGE_CATCHALLFIELDNAMES) {
			result.add(catchAllField);
		}
		
		return result;
	}
	
	public SolrServer getDefaultSolrServer() throws WFSVNException {
		return getSolrServer(this.CONFIG_SOLR_SERVER);
		
	}
	
	public SolrServer getPublishSolrServer() throws WFSVNException {
		try {
			publishSolrServer = new CommonsHttpSolrServer(this.CONFIG_PUBLISH_SOLR_SERVER);
		} catch (MalformedURLException e) {
			throw new WFSVNException(e);
		}
		return publishSolrServer;
		
	}
	
	public String getMssToReindex() {
		return this.mssToReindex;
	}
	
	public String getQuickMessage() {
		return this.quickMessage;
	}
	
	public Collection<String> getRegularFieldNames() throws WFSVNException {
		return this.ixedFieldsNames.values();
	}
	
	public SDXEMediator getSdxeMediator() {
		return this.sdxeMediator;
	}
	
	public String getSolrTypeForField(String fieldName) {
// if (!this.initialized) {
// this.init();
// }
		String result = this.ixedFieldsTypes.get(fieldName);
		
		if (result == null) {
			result = this.advSearchFieldsTypes.get(fieldName);
		}
		
		if (result == null) { // must be one of IXED_STORAGE_CATCHALLFIELDNAMES
			result = "text";
			if (fieldName.contains("Exact")) {
				result += "Exact";
			}
			result += fieldName.substring(fieldName.length() - 2);
			
		}
		return result;
		
	}
	
// @Override
// public void openRead(String fileNameNoExt) throws SVNException, IOException, WFSVNException {
//
// super.openRead(fileNameNoExt);
//
// try {
// this.galleryBean.loadTempXml();
// } catch (BAGException e) {
// throw new WFSVNException(e);
//
// }
// }
	
	@Override
	public void moveArtifactInWorkflow(String outcome) throws WFSVNArtifactLoadingException,
			SVNException {
		super.moveArtifactInWorkflow(outcome);
		
		// Updating the index on each worflow move is a big overhead, and nothing have changed
		// except the workflow step.. so WE WONT STORE IT IN THE INDEX
		// updateIndex(-1,newStep);
	}
	
	@Override
	public void openWrite(String fileNameNoExt) throws SVNException, IOException, WFSVNException {
		
		super.openWrite(fileNameNoExt);
		
		try {
			this.galleryBean.loadTempXml();
		} catch (BAGException e) {
			throw new WFSVNException(e);
			
		}
		
	}
	
	// Searching for old arabic
// protected void reindex(String filename, long revision, boolean commit) {
//
// try {
// LOG.debug("Rebuilding doc for: " + filename);
//
// // to avoid repetetively loading temp XML
// super.openRead(filename);
//
// this.sdxeMediator
// .open(this.getWorkingFile().getCanonicalPath(),
// this.schemaFilePath);
//
// XSALocator siteLoc = this.xsaDoc.getSiteLocator();
// this.sdxeMediator.moveToSugtnPath(siteLoc.asString());
//
// this.updateIndex(revision, commit);
//
// } catch (SVNException e) {
// LOG.error(e, e);
// } catch (SDXEException e) {
// LOG.error(e, e);
// } catch (JDOMException e) {
// LOG.error(e, e);
// } catch (Exception e) {
// LOG.error(e, e);
//
// } finally {
//
// try {
//
// this.close(true, true);
//
// this.sdxeMediator.close();
//
// this.closeWorkingFile();
//
// } catch (SVNException e) {
// LOG.error(e, e);
// } catch (DomBuildException e) {
// LOG.error(e, e);
// } catch (Exception e) {
// LOG.error(e, e);
// }
//
// }
// }
	
	public List<IndexEntry> parseSolrResp(QueryResponse rsp) throws WFSVNException {
// if (!this.initialized) {
// this.init();
// }
		SolrDocumentList docs = rsp.getResults();
		
		List<IndexEntry> result = new LinkedList<IndexEntry>();
		
		Iterator iter = docs.iterator();
		
		while (iter.hasNext()) {
			SolrDocument doc = (SolrDocument) iter.next();
			
			IndexEntry entry = new IndexEntry();
			
			entry.name = (String) doc.getFieldValue(WAMCP_INDEX_IDFIELDNAME); // id is the uniqueKey field
			doc.removeFields(WAMCP_INDEX_IDFIELDNAME);
			
			entry.revision = ((Long) doc.getFieldValue(WAMCP_INDEX_FIELDNAME_revision)).longValue();
			doc.removeFields(WAMCP_INDEX_FIELDNAME_revision);
			
			entry.lastModifiedDate = (Date) doc.getFieldValue(WAMCP_INDEX_FIELDNAME_timestamp);
			doc.removeFields(WAMCP_INDEX_FIELDNAME_timestamp);
			
			entry.lastModifiedBy = (String) doc.getFieldValue(WAMCP_INDEX_lastModifiedByFieldNAME);
			doc.removeFields(WAMCP_INDEX_lastModifiedByFieldNAME);
			
			entry.wfStep = (String) doc.getFieldValue(WAMCP_INDEX_FIELDNAME_WFSTEP);
			doc.removeFields(WAMCP_INDEX_FIELDNAME_WFSTEP);
			
			for (String fldname : doc.getFieldNames()) {
				String concatedValue = "";
				
				int ix = fldname.indexOf('_');
				
				String fldKey;
				if (ix != -1) {
					fldKey = fldname.substring(0, ix);
				} else {
					fldKey = fldname;
// throw new WFSVNException("Malformed field name: " + fldname);
				}
				
// String fldType = this.ixedFieldsTypes.get(fldname);
				
				if (fldKey.endsWith("SSS")) {
					// summary field
					fldKey = fldKey.substring(0, fldKey.length() - 3);
				} else {
					// not a summary field
					continue;
				}
				
				fldKey = fldKey.replace('.', ' ');
				
				for (Object obj : doc.getFieldValues(fldname)) {
					
					concatedValue += IXED_STORAGE_CONCATED_VALUE_DELIMETIR + obj.toString();
					
					// TODONOT highlighting
					// if (rsp.getHighlighting().get(entry.name) != null) {
					// List<String> highlightSnippets = rsp.getHighlighting().get(entry.name).get(
					// fldname);
					// }
				}
				
				concatedValue = concatedValue.substring(IXED_STORAGE_CONCATED_VALUE_DELIMETIR
						.length());
				
				entry.summaryFieldsValues.put(fldKey, concatedValue);
				
			}
			
			result.add(entry);
		}
		return result;
	}
	
	public void rebuildIndex(ActionEvent ev) {
		
		try {
			
			this.assureInit();
			
			LOG.info("Index rebuild process started at: "
					+ new SimpleDateFormat().format(new Date()));
			
			SolrServer server = this.getDefaultSolrServer();
			
			// clear index
			server.deleteByQuery("*:*");
			server.commit();
			LOG.debug("Deleted docs from backend SOLR server by query *:*");
			
			
			SolrServer publishServer = this.getPublishSolrServer();
			
			// clear index
			publishServer.deleteByQuery("*:*");
			publishServer.commit();
			LOG.debug("Deleted docs from publish SOLR server by query *:*");
			
			try {
				
				this.getGalleryBean().loadTempXml();
				
				WAMCPOldArabicStorage oldArabicIndexer = new WAMCPOldArabicStorage(this);
				Collection<WFSVNEntry> svnEntries = this.svnClient.svnGetEntries();
				
				for (WFSVNEntry entry : svnEntries) {
					
					oldArabicIndexer.reindex(entry.getName(), entry.getRevision(), false);
// this.getDefaultSolrServer().commit(); // to be deleted
// The internal server error is not caused by heavy load
// try {
// Thread.sleep(100);
// } catch (InterruptedException e) {
// // continue what you are doing!
// }
				}
				
				this.getDefaultSolrServer().commit();
				this.getPublishSolrServer().commit();
			} finally {
				this.getGalleryBean().unloadTempXml();
			}
			LOG.info("Index rebuild process successfully terminated at: "
					+ new SimpleDateFormat().format(new Date()));
		} catch (SVNException e) {
			LOG.fatal(e, e);
		} catch (WFSVNException e) {
			LOG.fatal(e, e);
		} catch (SolrServerException e) {
			LOG.fatal(e, e);
		} catch (IOException e) {
			LOG.fatal(e, e);
		} catch (BAGException e) {
			LOG.fatal(e, e);
		} catch (SugtnException e) {
			LOG.fatal(e, e);
		} catch (URISyntaxException e) {
			LOG.fatal(e, e);
		}
	}
	
	public void reindexManuscript(ActionEvent ev) {
		try {
			this.galleryBean.loadTempXml();
			
			new WAMCPOldArabicStorage(this).reindex(this.mssToReindex, -1, true);
		} catch (BAGException e) {
			LOG.error(e, e);
		} catch (SugtnException e) {
			LOG.error(e, e);
		} catch (WFSVNException e) {
			LOG.error(e, e);
		} catch (URISyntaxException e) {
			LOG.error(e, e);
		} finally {
			try {
				this.galleryBean.unloadTempXml();
			} catch (BAGException e) {
				LOG.error(e, e);
			}
		}
	}
	
	@Override
	public void release(Document metadataXml, Element metaAlbum) throws BAGException,
			WAMCPException, WFSVNArtifactLoadingException, WFSVNLockedException {
		
		super.release(metadataXml, metaAlbum);
		
		OAIFilesBuilder.releaseOAIFiles(metadataXml, this.cfgBagApiUri,
				WAMCPStorage.filenameForShelfMark(this.artifact.getArtifactName()), this.xsaDoc,
				this.galleryBean.getGalleryRootUrlStr());
	}
	
	public void setMssToReindex(String mssToReindex) {
		this.mssToReindex = mssToReindex;
	}
	
	public void setQuickMessage(String quickMessage) {
		this.quickMessage = quickMessage;
	}
	
	private String streamUrl(String urlStr) {
		BasicHttpParams httpParams;
		httpParams = new BasicHttpParams();
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		HttpConnectionParams.setConnectionTimeout(httpParams, 2000);
		ThreadSafeClientConnManager httpCM = new ThreadSafeClientConnManager(httpParams,
				schemeRegistry);
		DefaultHttpClient httpClient = new DefaultHttpClient(httpCM, httpParams);
		
		HttpGet httpGet = new HttpGet(urlStr);
		
		HttpResponse httpResp = null;
		try {
			
			httpResp = httpClient.execute(httpGet);
			
			this.assertResponseOk(httpResp);
			InputStream stream = httpResp.getEntity().getContent();
			StringBuffer out = new StringBuffer();
			byte[] b = new byte[4096];
			for (int n; (n = stream.read(b)) != -1;) {
				out.append(new String(b, 0, n, "UTF-8"));
			}
			String tmp = out.toString();
			tmp = tmp.substring(tmp.indexOf("?>") + 2);
			return tmp;
			
		} catch (ClientProtocolException e) {
			httpGet.abort();
		} catch (IOException e) {
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
	
	protected void updateIndex(long revision, boolean commit)
				// We get this from DB: String wfStepName)
			throws WFSVNException {
		
		// update the index
		SolrInputDocument doc = null;
		try {
			doc = this.constructDocFromXSA();
		} catch (DomBuildChangesException e) {
			throw new WFSVNException(e);
		} catch (XSAException e) {
			throw new WFSVNException(e);
		} catch (SugtnException e) {
			throw new WFSVNException(e);
		} catch (JDOMException e) {
			throw new WFSVNException(e);
		}
		
		// ////// Non XSA Specified fields /////////////
		
		// id field
		doc.addField(WAMCP_INDEX_IDFIELDNAME, this.getArtifact().getArtifactName());
		
		// html version
		String htmlString = this.fullHTML(this.getArtifact().getArtifactName());
		doc.addField("ARHtml", htmlString);
		doc.addField("ENHtml", htmlString);
		// doc.addField("ARExactHtml", htmlString);
		// doc.addField("ENExactHtml", htmlString);
		
		// last modified by
		doc.addField(WAMCP_INDEX_lastModifiedByFieldNAME, this.getSessionBBean().getUserName());
		
		// last modified date
		// automatically added in the timestamp field
		
		// revision
		doc.addField(WAMCP_INDEX_FIELDNAME_revision, revision);
		
		// Workflow step
		String currentWFStage = this.getSvnClient().getWfProcess()
				.getStepNameForArtifact(this.getArtifact());
		doc.addField(WAMCP_INDEX_FIELDNAME_WFSTEP, currentWFStage);
		
		// Representative cover image
		BAGThumbnail cover = null;
		try {
			cover = this.getGalleryBean().getAlbumCoverFromLoadedTempXml(this.getArtifact()
					.getArtifactName());
		} catch (BAGException e1) {
			LOG.error(e1, e1);
			try {
				this.getGalleryBean().loadTempXml();
			} catch (BAGException e) {
				LOG.error(e, e);
			}
			try {
				cover = this.getGalleryBean().getAlbumCoverFromLoadedTempXml(this.getArtifact()
						.getArtifactName());
			} catch (BAGException e) {
				LOG.error(e, e);
			}
		}
		
		String caption;
		String image;
		if (cover == null) {
			LOG.warn("Cannot get album cover data");
			caption = "";
			image = "";
		} else {
			caption = cover.getCaption();
			image = cover.getImageUrlStr();
		}
		doc.addField(WAMCP_INDEX_FIELDNAME_COVERCATION, caption);
		doc.addField(WAMCP_INDEX_FIELDNAME_COVERIMAGE, image);
		
		// /////////////////////
		
		// do the post requet to Solr
		SolrServer server = this.getDefaultSolrServer();
		
		try {
			server.add(doc);
			if (commit) {
				server.commit();
			}
		} catch (SolrServerException e) {
			LOG.error("Solr server error when adding doc: " + doc);
			throw new WFSVNException(e);
		} catch (SolrException e) {
			LOG.error("Solr error when adding doc: " + doc);
			throw new WFSVNException(e);
		} catch (IOException e) {
			throw new WFSVNException(e);
		}
		
		if(currentWFStage.equals("DONE")){
			// do the post requet to Solr
			SolrServer publishServer = this.getPublishSolrServer();
			
			try {
				publishServer.add(doc);
				if (commit) {
					publishServer.commit();
				}
			} catch (SolrServerException e) {
				LOG.error("Solr publish server error when adding doc: " + doc);
				throw new WFSVNException(e);
			} catch (SolrException e) {
				LOG.error("Solr publsih error when adding doc: " + doc);
				throw new WFSVNException(e);
			} catch (IOException e) {
				throw new WFSVNException(e);
			}
		}
	}
	
	public void writeIndexFieldsToFile(ActionEvent ev) throws WAMCPException, WFSVNException,
			SVNException, XSAException {
		
		Properties ixedFieldsNameType = new Properties();
		
		for (String key : this.getAllFieldNames()) {
			String value = this.getSolrTypeForField(key);
			
			if (!key.startsWith("All")) { // catch all fields don't have an XPath
			
				Set<Entry<XSAInstance, String>> entriesToSearch;
				if (key.indexOf('_') == -1) {
					entriesToSearch = this.advSearchFieldsNames.entrySet();
				} else {
					entriesToSearch = this.ixedFieldsNames.entrySet();
				}
				
				for (Entry<XSAInstance, String> mapping : entriesToSearch) {
					if (key.equals(mapping.getValue())) {
						
						String fieldXPathStr = mapping.getKey().getXPathToAllOccsInAllBaseShifts();
						value += "," + fieldXPathStr;
						
						// A field can take values from more than one XPath break;
					}
				}
			}
			
			ixedFieldsNameType.setProperty(key, value);
		}
		
		try {
			File ixedFieldesNameTypeTempFile = new File(this.getUserDir().getCanonicalPath()
					+ File.separator + "ixedFieldesNameType.properties");
			FileOutputStream ixedFieldesNameTypeTempStream = new FileOutputStream(
					ixedFieldesNameTypeTempFile);
			
			ixedFieldsNameType.store(ixedFieldesNameTypeTempStream, "");
			
			ixedFieldesNameTypeTempStream.flush();
			ixedFieldesNameTypeTempStream.close();
			
			String targetPathAbs = ixedFieldesNameTypeTempFile.getCanonicalPath();
			ServletContext sltCtx = (ServletContext) FacesContext.getCurrentInstance()
					.getExternalContext().getContext();
			String filePathAbs = sltCtx.getRealPath("");
			
			String filePathRel = targetPathAbs.substring(filePathAbs.length());
			filePathRel = filePathRel.replace('\\', '/');
			String fileURL = sltCtx.getContextPath() + filePathRel;
			
			this.setQuickMessage("Index fields file created: <a href='" + fileURL
					+ "'>"
					+ " Click to download! </a>");
		} catch (IOException e) {
			throw new WAMCPException(e);
		}
		
	}
}
