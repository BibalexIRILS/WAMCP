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

package org.bibalex.icefaces.jdomtree;

import javax.swing.tree.DefaultMutableTreeNode;

import org.bibalex.jdom.treemodel.IJDTNode;
import org.bibalex.jdom.treemodel.JDTAttributeNode;
import org.bibalex.jdom.treemodel.JDTElementNode;
import org.bibalex.jdom.treemodel.JDTTextNode;
import org.jdom.Attribute;

import com.icesoft.faces.component.tree.IceUserObject;

public class IDTUserObject extends IceUserObject {
	private static long idtNextNodeSerial = 0;
	
	private IJDTNode jdtNode;
	private final long nodeSerial;
	
	public IDTUserObject(DefaultMutableTreeNode wrapper) {
		super(wrapper);
		wrapper.setUserObject(this);
		
		this.nodeSerial = idtNextNodeSerial++;
	}
	
	public IJDTNode getJdtNode() {
		return this.jdtNode;
	}
	
	public long getNodeSerial() {
		return this.nodeSerial;
	}
	
	public void setJdtNode(IJDTNode jdtNode) {
		this.jdtNode = jdtNode;
		
		String text = null;
		
		if (jdtNode instanceof JDTElementNode) {
			
			text = ((JDTElementNode) jdtNode).getQualifier();
			
			this.setExpanded(true);
			this.setLeaf(false);
		} else {
			this.setExpanded(false);
			this.setLeaf(true);
			
			if (jdtNode instanceof JDTAttributeNode) {
				Attribute attrNode = (Attribute) jdtNode.getNode();
				text = "@" + jdtNode.getNodeName() + "=" + attrNode.getValue();
			} else if (jdtNode instanceof JDTTextNode) {
				text = "" + jdtNode.getNode();
			}
			
		}
		
		this.setText(text);
	}
	
	@Override
	public void setWrapper(DefaultMutableTreeNode wrapper) {
		throw new UnsupportedOperationException();
	}
}
