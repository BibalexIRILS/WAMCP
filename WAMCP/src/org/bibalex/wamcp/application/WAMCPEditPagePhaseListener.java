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

package org.bibalex.wamcp.application;

import java.io.IOException;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.apache.solr.client.solrj.SolrServerException;
import org.bibalex.sdxe.xsa.application.XSAConstants;
import org.bibalex.wamcp.pagebeans.BrowseConsBBean;
import org.bibalex.wamcp.pagebeans.SearchBBean;
import org.bibalex.wamcp.storage.WAMCPStorage;
import org.bibalex.workflow.storage.WFSVNException;

public class WAMCPEditPagePhaseListener implements PhaseListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1784501545592236356L;
	/**
	 * 
	 */
	
	// TODO: TEST!! Is this per user session or application scoped... must be per user.. and I "Think" it is!
	protected boolean inEditSession = false;
	protected boolean inBrowseSession = false;
	
	protected WAMCPUiActionListener acquireUiActionListener() {
		FacesContext fCtx = FacesContext.getCurrentInstance();
		ELContext elCtx = fCtx.getELContext();
		ExpressionFactory exprFactory = fCtx.getApplication().getExpressionFactory();
		
		ValueExpression actionListenerEl = exprFactory.createValueExpression(elCtx,
				"#{"
						+ XSAConstants.BEANNAME_ACTION_LISTENER + "}",
				WAMCPUiActionListener.class);
		
		WAMCPUiActionListener uiActionListener = (WAMCPUiActionListener) actionListenerEl
				.getValue(FacesContext.getCurrentInstance().getELContext());
		return uiActionListener;
	}
	
	@Override
	public void afterPhase(PhaseEvent ev) {
		// Nothing
	}
	
	@Override
	public void beforePhase(PhaseEvent ev) {
		FacesContext fCtx = FacesContext.getCurrentInstance();
		ELContext elCtx = fCtx.getELContext();
		ExpressionFactory exprFactory = fCtx.getApplication()
				.getExpressionFactory();
		boolean isPostBack = fCtx.getRenderKit()
				.getResponseStateManager().isPostback(FacesContext.getCurrentInstance());
		
// // prevent cache.. TODO: move to another phase listener..
//
// HttpServletResponse response = (HttpServletResponse) fCtx
// .getExternalContext().getResponse();
// response.addHeader("Pragma", "no-cache");
// response.addHeader("Cache-Control", "no-cache");
// // Stronger according to blog comment below that references HTTP spec
// response.addHeader("Cache-Control", "no-store");
// response.addHeader("Cache-Control", "must-revalidate");
// // some date in the past
// response.addHeader("Expires", "Mon, 8 Aug 2006 10:00:00 GMT");
		
		// init edit form
		if (!isPostBack) { // Navigation happened (redirect or new request)
		
			String viewId = fCtx.getViewRoot().getViewId();
			if ("/EditForm.xhtml".equals(viewId)
					|| "/Review1.xhtml".equals(viewId)
					|| "/Review2.xhtml".equals(viewId)
					|| "/ViewForm.xhtml".equals(viewId)
					|| "/Released.xhtml".equals(viewId)) {
				
				WAMCPUiActionListener actionListener = this.acquireUiActionListener();
				
				try {
					actionListener.initEditSession();
					this.inEditSession = true;
					
				} catch (Exception e) {
					try {
						fCtx.getExternalContext().redirect("Welcome.iface");
						e.printStackTrace();
					} catch (IOException e1) {
						// Auto-generated catch block
						e1.printStackTrace();
					}
					
				}
				
			} else {
				if (this.inEditSession) {
					// TODO redirect back to the "CORRECT" form
					this.acquireUiActionListener().uninitEditSession();
					
					this.inEditSession = false;
					
				}
				
				if ("/BrowseCons.xhtml".equals(viewId)) {
					
					if (!this.inBrowseSession) {
						
						ValueExpression searchBBeanEl = exprFactory.createValueExpression(elCtx,
								"#{searchBBean}",
								SearchBBean.class);
						
						SearchBBean searchBBean = (SearchBBean) searchBBeanEl
								.getValue(elCtx);
						
						ValueExpression storageBeanEl = exprFactory.createValueExpression(elCtx,
								"#{storageBean}", WAMCPStorage.class);
						
// WAMCPStorage storageBean = (WAMCPStorage) storageBeanEl.getValue(elCtx);
						
						ValueExpression browseBBeanEL = exprFactory.createValueExpression(elCtx,
								"#{browseConsBBean}", BrowseConsBBean.class);
						
						BrowseConsBBean browseBBean = (BrowseConsBBean) browseBBeanEL
								.getValue(elCtx);
						
						try {
							browseBBean.startListeningToSearchRes();
							
							searchBBean.setCriteria("");
							searchBBean.doSearch();
							
// We don't cache this anymore: storageBean.cacheEntries();
							
							// Will be called by the listener: browseBBean.init();
							
							this.inBrowseSession = true;
							
						} catch (WFSVNException e) {
							browseBBean.handleException(e);
						} catch (SolrServerException e) {
							browseBBean.handleException(e);
						}
					}
				} else if (this.inBrowseSession) {
					ValueExpression browseBBeanEL = exprFactory.createValueExpression(elCtx,
							"#{browseConsBBean}", BrowseConsBBean.class);
					
					BrowseConsBBean browseBBean = (BrowseConsBBean) browseBBeanEL.getValue(elCtx);
					
					browseBBean.stopListeningToSearchRes();
					
					this.inBrowseSession = false;
				}
			}
		}
	}
	
	@Override
	public PhaseId getPhaseId() {
		return PhaseId.RENDER_RESPONSE;
	}
	
}
