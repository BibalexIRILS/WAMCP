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

package org.bibalex.sdxe.binding;

import java.io.Serializable;

import javax.faces.context.FacesContext;

import org.bibalex.sdxe.controller.DomTreeController;
import org.bibalex.sdxe.controller.SDXEMediator;
import org.bibalex.sdxe.controller.SugtnTreeController;
import org.bibalex.sdxe.suggest.SugtnTreeBuilderCache;

/**
 * This extension for SDXEMediator only facilitates
 * use of SDXE without having to manage 3 beans;
 * the mediator, the dom controller, and the sugtn controller
 * With this you only need to create the mediator which
 * will create its dom and sugtn controllers.
 * 
 * @author younos
 * 
 */
public class SDXEMediatorBean extends SDXEMediator implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5514337404967361179L;
	
	private boolean configMinOccEnf = false;
	
	private boolean configMaxOccEnf = false;
	
	private boolean configTwoPhaseCommit = false;
	
	private static final String BEANNAME_SUGTN_BUILDER_CACHE = "sugtnTreeBuilderCache";
	
	public SDXEMediatorBean() {
		this.domTreeController = new DomTreeController(this.configMinOccEnf,
				this.configMaxOccEnf, this.configTwoPhaseCommit);
		
		FacesContext context = FacesContext.getCurrentInstance();
		SugtnTreeBuilderCache builderCache =
				(SugtnTreeBuilderCache) context.getApplication().getExpressionFactory()
						.createValueExpression(context.getELContext(),
								"#{" + BEANNAME_SUGTN_BUILDER_CACHE + "}",
								SugtnTreeBuilderCache.class)
						.getValue(context.getELContext());
		
		this.sugtnTreeController = new SugtnTreeController(builderCache);
		
	}
	
	/**
	 * @return the domTreeController
	 */
	public DomTreeController getDomTreeController() {
		return this.domTreeController;
	}
	
	/**
	 * @return the sugtnTreeController
	 */
	public SugtnTreeController getSugtnTreeController() {
		return this.sugtnTreeController;
	}
	
	public boolean isConfigMaxOccEnf() {
		return this.configMaxOccEnf;
	}
	
	public boolean isConfigMinOccEnf() {
		return this.configMinOccEnf;
	}
	
	public boolean isConfigTwoPhaseCommit() {
		return this.configTwoPhaseCommit;
	}
	
	public void setConfigMaxOccEnf(boolean configMaxOccEnf) {
		this.configMaxOccEnf = configMaxOccEnf;
	}
	
	public void setConfigMinOccEnf(boolean configMinOccEnf) {
		this.configMinOccEnf = configMinOccEnf;
	}
	
	public void setConfigTwoPhaseCommit(boolean configTwoPhaseCommit) {
		this.configTwoPhaseCommit = configTwoPhaseCommit;
	}
	
}
