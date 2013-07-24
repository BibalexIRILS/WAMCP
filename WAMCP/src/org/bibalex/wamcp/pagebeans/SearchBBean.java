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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.faces.event.ActionEvent;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.bibalex.gallery.exception.BAGException;
import org.bibalex.gallery.icefaces.BAGGalleryBean;
import org.bibalex.gallery.model.BAGThumbnail;
import org.bibalex.util.KeyValuePair;
import org.bibalex.wamcp.exception.WAMCPException;
import org.bibalex.wamcp.storage.WAMCPIndexedStorage;
import org.bibalex.wamcp.storage.WAMCPIndexedStorage.IndexEntry;
import org.bibalex.workflow.WFProcess;
import org.bibalex.workflow.storage.WFSVNException;

public class SearchBBean implements Serializable { // Not a request BBean because it coexists with one
	public static class FacetFilter {
		
		public String label;
		private int applied = -1;
		public ArrayList<String> filterQueries = new ArrayList<String>();
		public ArrayList<String> filterDescs = new ArrayList<String>();
		
		public void addFilterQuery(String description, String query) {
			this.filterDescs.add(description);
			this.filterQueries.add(query);
		}
		
		public void apply(String query) throws WFSVNException {
			if (this.getFilterApplied()) {
				throw new WFSVNException("A filter already applied on the field");
			}
			this.applied = this.filterQueries.indexOf(query);
		}
		
		public LinkedList<KeyValuePair<String, String>> getAllFilterQueryAndDesc() {
			LinkedList<KeyValuePair<String, String>> result = new LinkedList<KeyValuePair<String, String>>();
			for (int i = 0; i < this.filterQueries.size(); ++i) {
				result.add(new KeyValuePair<String, String>(this.filterDescs.get(i),
						this.filterQueries.get(i)));
			}
			return result;
		}
		
		public String getAppliedDesc() {
			if (!this.getFilterApplied()) {
				return null;
			}
			return this.filterDescs.get(this.applied);
		}
		
		public String getAppliedQuery() {
			if (!this.getFilterApplied()) {
				return null;
			}
			return this.filterQueries.get(this.applied);
		}
		
		public boolean getFilterApplied() {
			return this.applied != -1;
		}
		
		public String getLabel() {
			return this.label;
		}
		
	}
	
	public static interface ISearchResultsListener {
		public void notifyResultsChanged();
	}
	
	public static class SearchResult {
		private BAGThumbnail bagThumb;
		private final IndexEntry entry;
		
		// private String wfStep;
		
		public SearchResult(IndexEntry entry) {
			super();
			this.entry = entry;
			
		}
		
		public SearchResult(IndexEntry entry, String wfStep, BAGThumbnail bagThumb) {
			this(entry);
			// this.wfStep = wfStep;
			this.bagThumb = bagThumb;
		}
		
		public String getLastModifiedBy() {
			return this.entry.lastModifiedBy;
		}
		
		public Date getLastModifiedDate() {
			return this.entry.lastModifiedDate;
		}
		
		public String getName() {
			return this.entry.name;
		}
		
		public long getRevision() {
			return this.entry.revision;
		}
		
		public List<String> getSummaryKeys() {
			LinkedList<String> result = new LinkedList<String>();
			result.addAll(this.entry.summaryFieldsValues.keySet());
			
			Collections.sort(result);
			
			return result;
		}
		
		public HashMap<String, String> getSummaryValues() {
			return this.entry.summaryFieldsValues;
		}
		
		public BAGThumbnail getThumb() {
			return this.bagThumb;
		}
		
		public String getWorkflowStage() {
// return this.wfStep;
			return this.entry.wfStep;
		}
		
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2316592137984689972L;
	
	private static Logger LOG = Logger.getLogger("org.bibalex.wamcp");
	
	private BAGGalleryBean galleryBBean;
	
	private final LinkedList<ISearchResultsListener> listeners = new LinkedList<ISearchResultsListener>();
	
	private SolrQuery query = null;
	
	private String criteria = "Search";
	
	private final HashMap<String, List<SearchResult>> resultsCache = new HashMap<String, List<SearchResult>>(
			1);
	
	private WFProcess wfProcess = null;
	
	private WAMCPIndexedStorage storage = null;
	private String queryFields = null;
	
	private HashMap<String, FacetFilter> facetFilters = null;
	
	private final HashMap<String, FacetFilter> appliedFilters = new HashMap<String, FacetFilter>();
	
	private ArrayList<Integer> pages = new ArrayList<Integer>();
	
	private final int pageCountDisplay = 4;
	private Integer rows = 25;
	
	private long numFound = -1;
	private boolean displayFrontDotes = false;
	private boolean displayBackDotes = false;
	
	private String sortField;
	private ORDER sortOrder = ORDER.asc;
	
	public static String FACETQUERY_WF_EDIT = WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_WFSTEP
			+ ":EDIT";
	public static String FACETQUERY_WF_REVIEW1 = WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_WFSTEP
			+ ":REVIEW1";
	public static String FACETQUERY_WF_REVIEW2 = WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_WFSTEP
			+ ":REVIEW2";
	public static String FACETQUERY_WF_DONE = WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_WFSTEP
			+ ":DONE";
	
	public static String FACETQUERY_DATE_TODAY = WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_timestamp
			+ ":[NOW/DAY TO NOW]";
	
	public static String FACETQUERY_DATE_YESTERDAY = WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_timestamp
			+ ":[NOW/DAY-1DAY TO NOW/DAY]";
	
	public static String FACETQUERY_DATE_WIN2D = WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_timestamp
			+ ":[NOW/DAY-2DAY TO NOW]";
	
	public static String FACETQUERY_DATE_WIN1WK = WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_timestamp
			+ ":[NOW/DAY-7DAY TO NOW]";
	
	public static String FACETQUERY_DATE_BEF1WK = WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_timestamp
			+ ":[* TO NOW/DAY-7DAY]";
	
	private static String[] solrColumnsList = { "Ms. Name", // Revision is not indexed in Solr: "Revision",
			"Last Modified By", "Last Modified Date" };
	
	public static String graphicToSolrColumnsMapping(String graphicColumnName) {
		
		if ("Ms. Name".equals(graphicColumnName)) {
			return WAMCPIndexedStorage.WAMCP_INDEX_IDFIELDNAME;
// } else if ("Revision".equals(graphicColumnName)) {
// return WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_revision;
		} else if ("Last Modified By".equals(graphicColumnName)) {
			return WAMCPIndexedStorage.WAMCP_INDEX_lastModifiedByFieldNAME;
		} else if ("Last Modified Date".equals(graphicColumnName)) {
			return WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_timestamp;
		}
		
		return null;
	}
	
	public static String solrToGraphicColumnsMapping(String solrColumnName) {
		
		if (WAMCPIndexedStorage.WAMCP_INDEX_IDFIELDNAME.equals(solrColumnName)) {
			return solrColumnsList[0];
// } else if (WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_revision.equals(solrColumnName)) {
// return solrColumnsList[1];
		} else if (WAMCPIndexedStorage.WAMCP_INDEX_lastModifiedByFieldNAME.equals(solrColumnName)) {
			return solrColumnsList[1];
		} else if (WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_timestamp.equals(solrColumnName)) {
			return solrColumnsList[2];
		}
		
		return null;
	}
	
	private long lastUpdateTime = -1;
	
	public void adjustPaging() {
		
		this.pages.clear();
		int mycurrPage = this.getCurrPage();
		int count = 0;
		int lastPage = this.getTotalPages();
		
		// 1 2 <3> 4 5
		if ((mycurrPage - this.pageCountDisplay > 0)
				&& (mycurrPage + this.pageCountDisplay <= lastPage)) {
			for (int i = mycurrPage - this.pageCountDisplay + 1; (count < this.pageCountDisplay * 2)
					&& (i < lastPage); i++) {
				this.pages.add(i);
				count++;
			}
		}

		// <1> 2
		else if ((mycurrPage - this.pageCountDisplay <= 0)
				&& (mycurrPage + this.pageCountDisplay > lastPage)) {
			for (int i = 2; i <= lastPage - 1; i++) {
				this.pages.add(i);
			}
		}

		// <1> 2 3 4 5
		
		else if ((mycurrPage - this.pageCountDisplay <= 0)
				&& (mycurrPage + this.pageCountDisplay <= lastPage)) {
			
			for (int i = 2; (count < this.pageCountDisplay * 2) && (i < lastPage); i++) {
				
				this.pages.add(i);
				
				count++;
				
			}
			
		}

		// 2 3 4 5 <6>
		
		else if ((mycurrPage - this.pageCountDisplay >= 0)
				&& (mycurrPage + this.pageCountDisplay > lastPage)) {
			
			int value = this.pageCountDisplay - (lastPage - mycurrPage);
			
			for (int i = mycurrPage - value + 1; i <= lastPage - 1; i++) {
				
				this.pages.add(i);
				
			}
			
		}
		
		if (this.pages.size() == 0) {
			return;
		}
		
		if (this.pages.get(0) != 2) {
			this.displayFrontDotes = true;
		} else {
			this.displayFrontDotes = false;
		}
		
		if (this.pages.get(this.pages.size() - 1) != lastPage - 1) {
			this.displayBackDotes = true;
		} else {
			this.displayBackDotes = false;
		}
		
		return;
		
	}
	
	public void changePage(ActionEvent ev) {
		
		try {
			
			Integer newPage = Integer.parseInt((String) ev.getComponent().getAttributes().get(
					"PARAM_PageNum").toString());
			
			this.setCurrPage(newPage);
			
			this.doSearch();
			
		} catch (WFSVNException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void deleteFromIndex(List<String> ids) throws WFSVNException {
		
		SolrServer server;
		
		server = this.storage.getDefaultSolrServer();
		
		try {
			server.deleteById(ids);
			server.commit();
		} catch (SolrServerException e) {
			throw new WFSVNException(e);
		} catch (IOException e) {
			throw new WFSVNException(e);
		}
		
	}
	
	public void doSearch() throws WFSVNException, SolrServerException {
		try {
			SolrServer server = this.storage.getDefaultSolrServer();
			
			if (this.query == null) {
				this.query = new SolrQuery();
				
				if ((this.criteria == null) || this.criteria.isEmpty() || "*".equals(this.criteria)) {
					this.query.setQuery("*").setQueryType("standard");
				} else {
					this.queryFields = "id^10";
					for (String catchAllField : WAMCPIndexedStorage.IXED_STORAGE_CATCHALLFIELDNAMES) {
						this.queryFields += " " + catchAllField;
					}
					
// YA20110404 Search in catch all field only
// for (String ixedField : this.storage.getRegularFieldNames()) {
// String solrType = this.storage.getSolrTypeForField(ixedField);
//
// if (solrType.startsWith("text")) {
// // text fields are indexed in the Catch All fields
// continue;
// } else {
// this.queryFields += " " + ixedField;
// }
// }
					
					this.query.setQuery(this.criteria)
							.setQueryType("dismax")
							.setParam("qf", this.queryFields);
				}
				
				this.query
						.setRows(this.rows)
						.setStart(0)
						// Start is not really start but rather offset
						.setFacet(true)
						.addFacetQuery(FACETQUERY_DATE_TODAY)
						.addFacetQuery(FACETQUERY_DATE_YESTERDAY)
						.addFacetQuery(FACETQUERY_DATE_WIN2D)
						.addFacetQuery(FACETQUERY_DATE_WIN1WK)
						.addFacetQuery(FACETQUERY_DATE_BEF1WK)
						.addFacetQuery(FACETQUERY_WF_EDIT)
						.addFacetQuery(FACETQUERY_WF_REVIEW1)
						.addFacetQuery(FACETQUERY_WF_REVIEW2)
						.addFacetQuery(FACETQUERY_WF_DONE)
						.addFacetField(WAMCPIndexedStorage.WAMCP_INDEX_lastModifiedByFieldNAME,
								WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_WFSTEP);
				
				// TODO highlights
				// .setHighlight(true)
				// .setHighlightSnippets(1); // set other params as needed
				// this.query.setParam("hl.fl", "content");
				
				this.sortField = WAMCPIndexedStorage.WAMCP_INDEX_IDFIELDNAME;
				this.sortOrder = ORDER.asc;
				this.query.setSortField("score", ORDER.desc).addSortField(this.sortField,
						this.sortOrder);
// TODO: if needed .addSortField("timestamp", SolrQuery.ORDER.desc);
			}
			
			QueryResponse rsp = server.query(this.query);
// this.numFound = rsp.getResults().getNumFound();
			List<SearchResult> results = new LinkedList<SearchResult>();
			
// YA 20110217 Get WF Step from Solr
			LinkedList<String> inIds = new LinkedList<String>();
			HashMap<String, SearchResult> resWithNoWFStage = new HashMap<String, SearchBBean.SearchResult>();
			
			HashMap<String, BAGThumbnail> albumCovers;
			try {
				albumCovers = this.galleryBBean
						.getMetaDataToAlbumCoverMap();
			} catch (BAGException e) {
				throw new WFSVNException(e);
			}
			
			List<String> idsToDel = new LinkedList<String>();
			
			for (IndexEntry ixEntry : this.storage.parseSolrResp(rsp)) {
				SearchResult res = new SearchResult(ixEntry);
				
				if ((res.getWorkflowStage() == null) || res.getWorkflowStage().isEmpty()) {
					inIds.add(res.getName());
					resWithNoWFStage.put(res.getName(), res);
				}
				
				res.bagThumb = albumCovers.get(res.getName());
				
				if ((res.bagThumb == null)) {
					// bad index entry
					LOG
							.warn("An entry in the Solr index doesn't have corresponding data in Meta Gallery. Deleting from Solr:  "
									+ res.getName());
					idsToDel.add(res.getName());
				} else {
					
					results.add(res);
				}
			}
			
			if (!inIds.isEmpty()) {
				HashMap<String, String> resWFStages = this.wfProcess
						.getStepNamesForArtifactNames(inIds);
				for (String resName : resWithNoWFStage.keySet()) {
					resWithNoWFStage.get(resName).entry.wfStep = resWFStages.get(resName);
				}
			}
			
// LinkedList<String> inIds = new LinkedList<String>();
// for (IndexEntry ixEntry : this.storage.parseSolrResp(rsp)) {
// inIds.add(ixEntry.name);
// results.add(new SearchResult(ixEntry));
// }
//
// HashMap<String, String> resSteps = this.wfProcess.getStepNamesForArtifactNames(inIds);
//
// HashMap<String, BAGThumbnail> albumCovers;
// try {
// albumCovers = this.galleryBBean
// .getMetaDataToAlbumCoverMap();
// } catch (BAGException e) {
// throw new WFSVNException(e);
// }
//
// List<String> idsToDel = new LinkedList<String>();
//
// for (SearchResult res : results) {
// res.wfStep = resSteps.get(res.entry.name);
// res.bagThumb = albumCovers.get(res.getName());
//
// if ((res.bagThumb == null)) {
// // bad index entry
// LOG
// .warn("An entry in the Solr index doesn't have corresponding data in Meta Gallery. Deleting from Solr:  "
// + res.getName());
// idsToDel.add(res.getName());
// }
// }
// END YA 20110217 Get WF Step from Solr
			
			if (!idsToDel.isEmpty()) {
				this.deleteFromIndex(idsToDel);
			}
			
			this.resultsCache.put(this.criteria, results);
			
			this.numFound = rsp.getResults().getNumFound() - idsToDel.size();
			
			// Faceting (static) TODO dyanamic from facet fields that storage knows about
			Map<String, Integer> facetCounts = rsp.getFacetQuery();
			
			this.facetFilters = new HashMap<String, FacetFilter>();
			
			if (this.appliedFilters
					.containsKey(WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_timestamp)) {
				this.facetFilters.put(WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_timestamp,
						this.appliedFilters
								.get(WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_timestamp));
			} else {
				FacetFilter lastChangeDateFilter = new FacetFilter();
				lastChangeDateFilter.label = "Last modified date";
				
				lastChangeDateFilter.addFilterQuery("Today ("
						+ facetCounts.get(FACETQUERY_DATE_TODAY) + ")",
						FACETQUERY_DATE_TODAY);
				lastChangeDateFilter.addFilterQuery("Yesterday("
						+ facetCounts.get(FACETQUERY_DATE_YESTERDAY) + ")",
						FACETQUERY_DATE_YESTERDAY);
				lastChangeDateFilter.addFilterQuery(
						"Within 2 days(" + facetCounts.get(FACETQUERY_DATE_WIN2D) + ")",
						FACETQUERY_DATE_WIN2D);
				lastChangeDateFilter.addFilterQuery(
						"Within this week(" + facetCounts.get(FACETQUERY_DATE_WIN1WK) + ")",
						FACETQUERY_DATE_WIN1WK);
				lastChangeDateFilter.addFilterQuery(
						"Before this week(" + facetCounts.get(FACETQUERY_DATE_BEF1WK) + ")",
						FACETQUERY_DATE_BEF1WK);
				
				this.facetFilters.put(WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_timestamp,
						lastChangeDateFilter);
			}
			
			if (this.appliedFilters.containsKey(WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_WFSTEP)) {
				this.facetFilters.put(WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_WFSTEP,
						this.appliedFilters.get(WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_WFSTEP));
			} else {
				FacetFilter wfStageFilter = new FacetFilter();
				wfStageFilter.label = "Workflow Stage";
				
				wfStageFilter.addFilterQuery("Edit ("
						+ facetCounts.get(FACETQUERY_WF_EDIT) + ")",
						FACETQUERY_WF_EDIT);
				wfStageFilter.addFilterQuery("Review I ("
						+ facetCounts.get(FACETQUERY_WF_REVIEW1) + ")",
						FACETQUERY_WF_REVIEW1);
				wfStageFilter.addFilterQuery(
						"Review II (" + facetCounts.get(FACETQUERY_WF_REVIEW2) + ")",
						FACETQUERY_WF_REVIEW2);
				wfStageFilter.addFilterQuery(
						"Published (" + facetCounts.get(FACETQUERY_WF_DONE) + ")",
						FACETQUERY_WF_DONE);
				
				this.facetFilters.put(WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_WFSTEP,
						wfStageFilter);
			}
			
			for (FacetField ffld : rsp.getFacetFields()) {
				if (this.appliedFilters.containsKey(ffld.getName())) {
					
					this.facetFilters.put(ffld.getName(),
							this.appliedFilters.get(ffld.getName()));
					
				} else {
					List<Count> ffldValues = ffld.getValues();
					if ((ffldValues == null)
							|| WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_WFSTEP.equals(ffld
									.getName())) {
						// Already done above!
						continue;
					}
					
					FacetFilter facetFilter = new FacetFilter();
					
					facetFilter.label = ffld.getName().replace('.', ' ');
					
					for (Count val : ffldValues) {
						
						facetFilter.addFilterQuery(val.getName()
								+ "(" + val.getCount() + ")", ffld.getName() + ":" + val.getName());
					}
					
					this.facetFilters.put(ffld.getName(), facetFilter);
				}
			}
			
			for (ISearchResultsListener listener : this.listeners) {
				listener.notifyResultsChanged();
			}
		} finally {
			this.adjustPaging();
		}
	}
	
	public void filterRes(String filterFieldName, String query) throws WFSVNException,
			SolrServerException {
		
		FacetFilter ff = this.facetFilters.get(filterFieldName);
		
		ff.apply(query);
		
		this.appliedFilters.put(filterFieldName, ff);
		
		this.query.addFilterQuery(query);
		this.setCurrPage(1);
		this.doSearch();
	}
	
	public HashMap<String, FacetFilter> getAppliedFilters() {
		return this.appliedFilters;
	}
	
	public String getCriteria() {
		return this.criteria;
	}
	
	public int getCurrPage() {
		// return this.currPage;
		
		if ((this.numFound == 0) || (this.query == null)) {
			return 0;
		}
		
		int result = -1;
		
		try {
			result = (int) Math
					.ceil((double) this.query.getStart() / (double) this.query.getRows()) + 1;
		} catch (Exception e) {
			// Sometimes the Log contains error while readig curr page.. couldn't reporoduce, but ok!
			result = 0;
		}
		return result;
	}
	
	public boolean getDisplayBackDotes() {
		return this.displayBackDotes;
	}
	
	public boolean getDisplayFrontDotes() {
		return this.displayFrontDotes;
	}
	
	public HashMap<String, FacetFilter> getFacetFilters() {
		return this.facetFilters;
	}
	
	public LinkedList<String> getFacetFiltersKeys() {
		LinkedList<String> result = this.getFacetFiltersKeysInternal();
		if (result == null) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// continue your work
			}
			result = this.getFacetFiltersKeysInternal();
		}
		return result;
	}
	
	protected LinkedList<String> getFacetFiltersKeysInternal() {
		LinkedList<String> result = new LinkedList<String>();
		if ((this.facetFilters == null) || (this.facetFilters.keySet() == null)) {
			return null;
		}
		for (Object key : this.facetFilters.keySet().toArray()) {
			result.add((String) key);
		}
		Collections.sort(result);
		return result;
	}
	
	public Integer getFirstResNum() {
		if (this.numFound == 0) {
			return 0;
		}
		int start = 0;
		if (this.query != null) {
			start = this.query.getStart();
		}
		return start + 1; // Start is not really start but rather an offset
		
	}
	
	public BAGGalleryBean getGalleryBBean() {
		return this.galleryBBean;
	}
	
	public long getLastResNum() {
		int start = 0;
		if (this.query != null) {
			start = this.query.getStart();
		}
		long result = start + this.rows;
		if (result > this.numFound) {
			result = this.numFound;
		}
		return result;
		
	}
	
	public long getNumFound() throws WAMCPException {
		return this.numFound;
	}
	
	public ArrayList<Integer> getPages() {
		return this.pages;
	}
	
	public List<SearchResult> getResults() throws WAMCPException {
		List<SearchResult> resp;
		
		if (this.resultsCache.containsKey(this.criteria)) {
			resp = this.resultsCache.get(this.criteria);
		} else {
			throw new WAMCPException("After the criteria is changed you must first call doSearch");
		}
		
		return resp;
	}
	
	public Integer getRows() {
		return this.rows;
	}
	
	public String[] getSolrColumnsList() {
		return solrColumnsList;
	}
	
	public boolean getSolrSortAscending() {
		return (this.getSortOrder() == ORDER.asc);
	}
	
	public String getSolrSortColumn() {
		return this.getSortField();
	}
	
	public String getSolrSortColumnName() {
		return solrToGraphicColumnsMapping(this.getSortField());
	}
	
	public String getSortField() {
		return this.sortField;
	}
	
	public ORDER getSortOrder() {
		return this.sortOrder;
	}
	
	public int getTotalPages() {
		int result = (int) (Math.ceil((double) this.numFound / (double) this.rows));
		return result;
	}
	
	public boolean isSolrSortAscending() {
		return (this.getSortOrder() == ORDER.asc);
	}
	
	public void registerListener(ISearchResultsListener listener) {
		this.listeners.add(listener);
	}
	
	public void setCriteria(String criteria) throws WFSVNException {
		if (criteria == null) {
			criteria = "*";
		}
		
		this.criteria = criteria;
		
// Real caching is not what is intended, but rather that we prevent
// repeatative assignment of the criteria from the search box
		if ((System.currentTimeMillis() - this.lastUpdateTime > 60000)
				|| !this.resultsCache.containsKey(criteria)) {
			this.lastUpdateTime = System.currentTimeMillis();
			this.resultsCache.clear();
			this.resultsCache.put(criteria, null);
			
			this.query = null;
			this.appliedFilters.clear();
			this.sortField = null;
// will access the null query: setCurrPage(1);
			
		}
	}
	
	public void setCurrPage(int pageNum) throws WFSVNException, SolrServerException {
		if (this.getCurrPage() == pageNum) {
			return;
		}
		
		this.query.setStart(((pageNum - 1) * this.rows));// Start is not really start but rather offset
	}
	
	public void setFirstResNum(Integer x) {
		// just to avoid the exception caused by that JSF binding is two ways
	}
	
	public void setGalleryBBean(BAGGalleryBean galleryBBean) {
		this.galleryBBean = galleryBBean;
	}
	
	public void setRows(Integer rows) {
		this.rows = rows;
	}
	
	public void setStorage(WAMCPIndexedStorage storage) {
		
		this.storage = storage;
		
	}
	
	public void setWfProcess(WFProcess wfProcess) {
		this.wfProcess = wfProcess;
		
	}
	
	public void solrSortOnColumn(ActionEvent ev) {
		
		String colName = graphicToSolrColumnsMapping((String) ev.getComponent()
				.getAttributes().get("PARAM_COLNAME"));
		
		try {
			if (colName != null) {
				this.sortRes(colName);
			}
			
		} catch (WFSVNException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void sortRes(String sortFieldName) throws WFSVNException,
			SolrServerException {
		
		ORDER newOrder;
		
		if (((this.sortField != null) && this.sortField.equals(sortFieldName))) {
			newOrder = (this.sortOrder.equals(ORDER.asc) ? ORDER.desc : ORDER.asc);
		} else {
			if (WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_timestamp.equals(sortFieldName)
					|| WAMCPIndexedStorage.WAMCP_INDEX_FIELDNAME_revision.equals(sortFieldName)) {
				newOrder = ORDER.desc;
			} else {
				newOrder = ORDER.asc;
			}
		}
		
		this.query.removeSortField(this.sortField, this.sortOrder);
		
		this.sortOrder = newOrder;
		this.sortField = sortFieldName;
		
		this.query.addSortField(this.sortField, this.sortOrder);
		
		this.setCurrPage(1);
		this.doSearch();
	}
	
	public void unfilterRes(String filterFieldName, String query) throws WFSVNException,
			SolrServerException {
		
		this.appliedFilters.remove(filterFieldName);
		
		this.query.removeFilterQuery(query);
		this.setCurrPage(1);
		this.doSearch();
	}
	
	public void unregisterListener(ISearchResultsListener listener) {
		this.listeners.remove(listener);
	}
	
}
