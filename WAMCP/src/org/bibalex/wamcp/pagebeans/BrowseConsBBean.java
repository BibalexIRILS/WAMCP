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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.apache.log4j.Logger;
import org.bibalex.gallery.model.BAGThumbnail;
import org.bibalex.wamcp.pagebeans.SearchBBean.ISearchResultsListener;
import org.bibalex.wamcp.pagebeans.SearchBBean.SearchResult;
import org.bibalex.wamcp.storage.WAMCPStorage;
import org.bibalex.workflow.storage.WFSVNClient.WFSVNEntry;

public class BrowseConsBBean extends WAMCPRequestBBean implements Serializable {
	public static class ConsMSDesc {
		private SearchResult searchRes;
		private WFSVNEntry wfsvnEntry;
		
		public ConsMSDesc() {
			super();
		}
		
		public ConsMSDesc(SearchResult searchRes, WFSVNEntry wfsvnEntry) {
			super();
			this.searchRes = searchRes;
			this.wfsvnEntry = wfsvnEntry;
			
		}
		
		public boolean getIsDeleteRequested() {
			return this.wfsvnEntry.getIsDeleteRequested();
		}
		
		public String getLastModifiedBy() {
			return this.searchRes.getLastModifiedBy();
		}
		
		public Date getLastModifiedDate() {
			return this.searchRes.getLastModifiedDate();
		}
		
		public String getLockHolder() {
			return this.wfsvnEntry.getLockHolder();
		}
		
		public String getName() {
			return this.searchRes.getName();
		}
		
		public long getRevision() {
			return this.wfsvnEntry.getRevision();
		}
		
		public List<String> getSummaryKeys() {
			return this.searchRes.getSummaryKeys();
		}
		
		public HashMap<String, String> getSummaryValues() {
			return this.searchRes.getSummaryValues();
		}
		
		public BAGThumbnail getThumb() {
			return this.searchRes.getThumb();
		}
		
		public String getWorkflowStage() {
			return this.searchRes.getWorkflowStage();
		}
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7032459310270745255L;
	
	private static Logger LOG = Logger.getLogger("org.bibalex.wamcp");
	private final SearchBBean searchBBean;
	private final WAMCPStorage wamcpStorage;
	
	private List<ConsMSDesc> msDescs;
	
	private String localSortColumnName;
	
	private boolean localSortAscending;
	
	private final String[] columnsList = { "Ms. Name", "Stage of Development",
			"Revision", "Last Modified By", "Last Modified Date",
			"Lock Holder", "Delete Requested" };
	
	private final ISearchResultsListener searchResListener = new ISearchResultsListener() {
		
		@Override
		public void notifyResultsChanged() {
			BrowseConsBBean.this.init();
			
		}
	};
	
	public BrowseConsBBean(SearchBBean searchBBean, WAMCPStorage wamcpStorage) {
		
		// Now that PhaseListener calls init We could also get rid of
		// constructor args now tha
		super();
		this.searchBBean = searchBBean;
		this.wamcpStorage = wamcpStorage;
		
	}
	
	public String[] getColumnsList() {
		return this.columnsList;
	}
	
	public boolean getLocalSortAscending() {
		return this.localSortAscending;
	}
	
	public String getLocalSortColumnName() {
		return this.localSortColumnName;
	}
	
	public List<ConsMSDesc> getMsDescs() {
		
		return this.msDescs;
		
	}
	
	public int getNumMsDescs() {
		return this.msDescs.size();
	}
	
	public void init() {
		try {
			// The page is paresed before these lines are processed resulting in
			// null pointer exception
			// --> so they are moved to the WAMCPEditPagePhaseListener
			// this.searchBBean.setCriteria("");
			//
			// this.searchBBean.doSearch();
			//
			// this.wamcpStorage.cacheEntries();
			
			this.msDescs = new ArrayList<ConsMSDesc>();
			this.localSortColumnName = "";
			this.localSortAscending = true;
			
			HashMap<String, WFSVNEntry> svnEntriesMap = new HashMap<String, WFSVNEntry>();
			for (WFSVNEntry svnEntry : this.wamcpStorage.getEntries()) {
				svnEntriesMap.put(svnEntry.getName(), svnEntry);
			}
			
			List<String> idsToDel = new LinkedList<String>();
			
			for (SearchResult sRes : this.searchBBean.getResults()) {
				
				ConsMSDesc consMSDesc = new ConsMSDesc();
				consMSDesc.searchRes = sRes;
				
				String msName = sRes.getName();
				
				// This could cause a newly inserted Solr Doc to be deleted
				// because it is not in the cached results
				// for (WFSVNEntry svnEntry :
				// this.wamcpStorage.getCachedEntries()) {
				
				// YA 20110217 Get rid of this stupid Linear Search
				// for (WFSVNEntry svnEntry : this.wamcpStorage.getEntries()) {
				// if (msName.equals(svnEntry.getName())) {
				// consMSDesc.wfsvnEntry = svnEntry;
				// break;
				// }
				// }
				
				if (svnEntriesMap.containsKey(msName)) {
					consMSDesc.wfsvnEntry = svnEntriesMap.get(msName);
				}
				// END stupid linear search
				
				if ((consMSDesc.wfsvnEntry == null)) {
					// bad index entry
					LOG
							.warn("An entry in the Solr index doesn't have corresponding data on SVN. Deleting from Solr:  "
									+ msName);
					idsToDel.add(msName);
				} else {
					this.msDescs.add(consMSDesc);
				}
			}
			if (!idsToDel.isEmpty()) {
				this.searchBBean.deleteFromIndex(idsToDel);
			}
			
		} catch (Exception e) {
			this.handleException(e);
		}
	}
	
	public boolean isLocalSortAscending() {
		return this.localSortAscending;
	}
	
	public void localSortOnColumn(ActionEvent ev) {
		String colName = (String) ev.getComponent().getAttributes()
				.get("PARAM_COLNAME");
		
		if (colName.equals(this.localSortColumnName)) {
			return;
		}
		
		this.localSortColumnName = colName;
		this.localSortAscending = true;
		
		this.sortMsDescs();
	}
	
	public void setLocalSortAscending(boolean sortAscending) {
		this.localSortAscending = sortAscending;
	}
	
	public void setLocalSortColumnName(String sortColumnName) {
		this.localSortColumnName = sortColumnName;
	}
	
	private void sortMsDescs() {
		Comparator<ConsMSDesc> comparator = new Comparator<ConsMSDesc>() {
			@Override
			public int compare(ConsMSDesc arg0, ConsMSDesc arg1) {
				int result = 0;
				// "Ms. Name", "Stage of Development",
				// "Revision","Last Modified By",
				// "Last Modified Date","Lock Holder", "Delete Requested"
				if (BrowseConsBBean.this.columnsList[0]
						.equals(BrowseConsBBean.this.localSortColumnName)) {
					
					// Name
					result = arg0.searchRes.getName().compareTo(
							arg1.searchRes.getName());
					
				} else if (BrowseConsBBean.this.columnsList[1]
						.equals(BrowseConsBBean.this.localSortColumnName)) {
					
					// Workflow stage
					result = arg0.getWorkflowStage().compareTo(
							arg1.getWorkflowStage());
					
				} else if (BrowseConsBBean.this.columnsList[2]
						.equals(BrowseConsBBean.this.localSortColumnName)) {
					
					result = Long.valueOf(arg0.getRevision()).compareTo(
							Long.valueOf(arg1.getRevision()));
					
				} else if (BrowseConsBBean.this.columnsList[3]
						.equals(BrowseConsBBean.this.localSortColumnName)) {
					
					result = arg0.getLastModifiedBy().compareTo(
							arg1.getLastModifiedBy());
					
				} else if (BrowseConsBBean.this.columnsList[4]
						.equals(BrowseConsBBean.this.localSortColumnName)) {
					
					result = arg0.getLastModifiedDate().compareTo(
							arg1.getLastModifiedDate());
					
				} else if (BrowseConsBBean.this.columnsList[5]
						.equals(BrowseConsBBean.this.localSortColumnName)) {
					String lh0 = arg0.getLockHolder();
					String lh1 = arg1.getLockHolder();
					if ((lh0 == null) || lh0.isEmpty()) {
						result = Integer.MAX_VALUE;
					} else if ((lh1 == null) || lh1.isEmpty()) {
						result = Integer.MIN_VALUE;
					} else {
						result = lh0.compareTo(lh1);
					}
					
				} else if (BrowseConsBBean.this.columnsList[6]
						.equals(BrowseConsBBean.this.localSortColumnName)) {
					
					result = Boolean
							.valueOf(arg0.getIsDeleteRequested())
							.compareTo(
									Boolean.valueOf(arg1.getIsDeleteRequested()));
					
				}
				
				if (!BrowseConsBBean.this.localSortAscending) {
					result *= -1;
				}
				
				return result;
			}
			
		};
		Collections.sort(this.msDescs, comparator);
	}
	
	public void startListeningToSearchRes() {
		this.searchBBean.registerListener(this.searchResListener);
	}
	
	public void stopListeningToSearchRes() {
		this.searchBBean.unregisterListener(this.searchResListener);
	}
	
	public void toggleLocalAscending(ActionEvent ev) {
		this.localSortAscending = !this.localSortAscending;
		this.sortMsDescs();
	}
	
}
