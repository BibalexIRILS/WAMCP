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

package org.bibalex.sdxe.suggest.model.dom;

/**
 * 
 * Class represents declaration qualified name (QName)
 *
 */
public class SugtnDeclQName {
	
	/**
	 * Target namespace URI
	 */
	String targetNamespaceURI = null;
	
	/**
	 * Local name of the declaration qualified name
	 */
	String localName = null;
	
	/**
	 * Constructs a new declaration qualified name with empty namespace URI and local name.  
	 */
	public SugtnDeclQName() {
		this.targetNamespaceURI = "";
		this.localName = "";
	}
	
	/**
	 * Constructs a new declaration qualified name with the namespace URI and the local name passed as arguments.
	 * 
	 * @param targetNamespaceURI
	 * 			Target namespace URI
	 * @param localName
	 * 			Local name of the declaration
	 */
	public SugtnDeclQName(String targetNamespaceURI, String localName) {
		this.targetNamespaceURI = targetNamespaceURI;
		this.localName = localName;
	}
	
	/**
	 * Compares reference of this object to the reference of the object passed as arguments.
	 * 
	 * @param arg0 
	 * 			Object to be compared with
	 * 
	 * @return true if this object has the same reference as object in arguments, false if otherwise. 		
	 * 		
	 */
	@Override
	public boolean equals(Object arg0) {
		boolean result;
		if (arg0 instanceof SugtnDeclQName) {
			result = this.equals((SugtnDeclQName) arg0);
		} else {
			result = super.equals(arg0);
		}
		return result;
	}
	
	
	/**
	 * Compares the values of this declaration QName to the values of the declaration QName passed as an argument.
	 * 
	 *  It returns true if and only if both have the same values and argument is not null.
	 *  
	 * @param other
	 * 		Declaration qualified name to be compared with.
	 * @return true if  this declaration QName has the same values as the declaration QName passed as an argument, false if otherwise.
	 * 
	 */
	public boolean equals(SugtnDeclQName other) {
		if (other == null) {
			return false;
		} else {
			return (this.localName == null ? other.localName == null
					: this.localName.equals(other.localName))
					&& (this.targetNamespaceURI == null ? other.targetNamespaceURI == null
							: this.targetNamespaceURI.equals(other.targetNamespaceURI));
		}
	}
	
	/**
	 * Gets local name of the declaration qualified name.
	 * 
	 * @return the localName of the declaration qualified name.
	 */
	public String getLocalName() {
		return this.localName;
	}
	
	/**
	 * Gets Target namespace URI.
	 * 
	 * @return the target namespace URI.
	 */
	public String getTargetNamespaceURI() {
		return this.targetNamespaceURI;
	}
	
	/**
	 * Returns a hashcode for this qualified name using hashcode function of String on local name and namespace URI.
	 * 
	 * @return hashcode number for this qualified name.
	 */
	@Override
	public int hashCode() {
		int result = -1;
		int nameHash = this.localName.hashCode();
		
		int nsHash = this.targetNamespaceURI != null ? this.targetNamespaceURI.hashCode()
				: nameHash;
		
		result = nameHash == nsHash ? nameHash : nameHash ^ nsHash;
		return result;
	}
	
	/**
	 *  Returns a String object representing this declaration qualified name.
	 *  
	 *  @return a string representation of this object.
	 */
	@Override
	public String toString() {
		String result = this.localName + "@" + this.targetNamespaceURI;
		return result;
	}
}