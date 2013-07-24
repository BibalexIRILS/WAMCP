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

package com.sun.facelets.compiler;

import java.io.IOException;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

/**
 * This is the modification for making Facelets dynamic. It treats HTML code as first class citizens of the template,
 * unlike the original code that cares only about components.
 */
public class UIInstructionsCrucial extends UILeaf {
	/**
	 * Applied our modification to the compiled facelet
	 */
	public static void makeUIInstructionsCrucial(List<UIComponent> children) {
		if (children == null) {
			return;
		}
		
		for (int i = 0; i < children.size(); ++i) {
			
			UIComponent child = children.get(i);
			
			if (child instanceof UIInstructions) {
				UIInstructionsCrucial crucial = new UIInstructionsCrucial((UIInstructions) child);
				children.remove(i);
				children.add(i, crucial);
			}
			
			makeUIInstructionsCrucial(child.getChildren());
		}
		
	}
	
	/**
	 * Stores an unmodified version of the instructions, so that calls are delegated to it
	 */
	private UIInstructions orig;
	
	public UIInstructionsCrucial(UIInstructions orig) {
		this.orig = orig;
	}
	
	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		this.orig.encodeBegin(context);
	}
	
	@Override
	public boolean isTransient() {
		return false; // This is the modification from the original code
	}
	
	@Override
	public String toString() {
		return this.orig.toString();
	}
	
}
