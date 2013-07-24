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

package org.bibalex.sdxe.controller;

import org.bibalex.sdxe.exception.DomBuildException;
import org.bibalex.sdxe.exception.SDXEException;
import org.bibalex.sdxe.suggest.model.dom.SugtnDeclQName;
import org.bibalex.sdxe.suggest.model.dom.SugtnElementNode;
import org.jdom.Document;

/**
 * Callbacks for the changes and the events of the DOM controllers
 * 
 * @author Younos.Naga
 * 
 */
public interface IDomObserver {
	// TODO This doesn't belong here.. but it's ok for now
	public void notifyBeforeSave(Document domTotalClone) throws DomBuildException;
	
	public void notifyDomMovedHorizontal(SugtnElementNode sugtnElt, int index) throws SDXEException;
	
	public void notifyDomMovedVertical(SugtnElementNode sugtnElt) throws SDXEException;
	
	public void notifyDomNSugtnOutOfSync(SugtnDeclQName currDomEltName) throws SDXEException;
}
