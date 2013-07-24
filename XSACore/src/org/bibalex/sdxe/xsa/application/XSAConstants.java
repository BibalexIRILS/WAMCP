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

package org.bibalex.sdxe.xsa.application;

public class XSAConstants {
	/**
	 * IDCONVENTION_ contstants are used in formulating IDs for JSF controls from the XPath of what they correspond to
	 */
	public static final char IDCONVENTION_ATTR_PREFIX = '-';
	public static final char IDCONVENTION_XPSTEP_DELIMITER = '_';
	public static final char IDCONVENTION_XPPRED_OPEN = '0';// '~';
	public static final char IDCONVENTION_XPPRED_CLOSE = '0';// '~';
	public static final char IDCONVENTION_NS_PREFIX = '0';
	public static final String IDCONVENTION_GRP_PREFIX = "grpPnlPfx";
	
	/**
	 * BEANNAME_ constants are used to programatically creat EL expressions and use them to get hold of managed beans
	 */
	public static final String BEANNAME_ACTION_LISTENER = "uiActionListener";
	public static final String BEANNAME_BINDERS_BEAN = "xsaBindersBean";
	public static final String BEANNAME_XSADOCCache = "xsaDocCache";
	public static final String BEANNAME_XSAINSTANCECache = "xsaInstanceCache";
	public static final String BEANNAME_SugtnTreeContollerCache = "sugtnTreeContollerCache";
	public static final String BEANNAME_SugtnTreebuilderCache = "sugtnTreeBuilderCache";
	
	/**
	 * ID_ prefixed constants are the keys of dynamically generated controls in the dynamic controls map
	 */
	// PanelGroup holding the Input control of an Occurrence
	public static final String ID_occIpHldrId = "occIpHldrId";
	// panelGroup holding the hidden popup for editing attributes of an Occurrence
	public static final String ID_occPopAttrHldrId = "occPopAttrHldrId";
	// commandLink to show the popup for editing attributes of an Occurrence
	public static final String ID_occPopAttrLink = "ID_occPopAttrLink";
	// panelGroup holding the Delete control of an Occurrence
	public static final String ID_occBtnDelHldrId = "occBtnDelHldrId";
	// commandLink to undo the last edit in an Occurrence (if two phase commit is enabled)
	public static final String ID_occBtnRevert = "occBtnRevert";
	// message control of an Occurrence
	public static final String ID_occMsg = "ID_occMsg";
	
	// The commandLink for creating a new occurrence of a Field
	public static final String ID_fldBtnNew = "fldBtnNew";
	// panelGroup holding all occurrences of a Field
	public static final String ID_fldOccsPnl = "fldOccsPnl";
	// panelGroup holding the help text of a Field
	public static final String ID_fldDescr = "fldDescr";
	// message control of the Field
	public static final String ID_fldMsg = "ID_fldMsg";
	
	// panelGroup holding the help text of a Container
	public static final String ID_contDesc = "contDesc";
	// commandLink to show the popup for editing attributes of the corresponding XML element of a Container
	public static final String ID_contEditAttrsLink = "contEditAttrsLink";
	// commandLink to move to the previous occurrence of a Container
	public static final String ID_contPrev = "contPrev";
	// outputText showing the index of the current occurrence of a Container
	public static final String ID_contIx = "contIx";
	// commandLink to move to the next occurrence of a Container
	public static final String ID_contNext = "contNext";
	// commandLink to delete an occurrence of a Container
	public static final String ID_contDel = "contDel";
	// commandLink to create a new occurrence of a Container
	public static final String ID_contNew = "contNew";
	// panelGroup holding the fields and containers contained by a Container
	public static final String ID_contFields = "contFields";
	// commandLink to show a popup for fast navigation between occurrences of a Container
	public static final String ID_contJump = "ID_contJump";
	// message control of Container
	public static final String ID_contMsg = "ID_contMsg";
	
	// commandLink to go into an Area
	public static final String ID_areaBtnOpen = "areaBtnOpen";
	// help text of an Area
	public static final String ID_areaTxtHelp = "areaTxtHelp";
	
	// commandLink to apply changes in the current screen (if two phase commit is enabled)
	public static final String ID_arBtnApply = "ID_arBtnApply";
	// commandLink to revert changes in the current screen (if two phase commit is enabled)
	public static final String ID_arBtnRevert = "ID_arBtnRevert";
	// panelGroup to hold the Apply and Revert buttons, if the current screen is the popup of editing attributes
	public static final String ID_pnlAppRevPopAttrs = "ID_pnlAppRevPopAttrs";
	// panelGroup to hold the Apply and Revert buttons, if the current screen is a section
	public static final String ID_pnlAppRevSection = "ID_pnlAppRevSection";
	// panelGroup to hold the Apply and Revert buttons, if the current screen is a whole area or the site
	public static final String ID_pnlAppRevForm = "ID_pnlAppRevForm";
	
	// commandLink to close the popup for editing attributes when editing attributes of a field or container
	public static final String ID_attrPopupClose = "ID_attrPopupClose";
	// commandLink to submit the popup for editing attributes when editing attributes of an annotator
	public static final String ID_attrPopupOk = "ID_attrPopupOk";
	// commandLink to cancel the popup for editing attributes when editing attributes of an annotator
	public static final String ID_attrPopupCancel = "ID_attrPopupCancel";
	
	// panelGroup holding the contents of a Section
	public static final String ID_secContents = "ID_secContents";
	
	// panelGroup that is the header of a Group
	public static final String ID_subsecHead = "ID_subsecHead";
	// panelGroup that is the body of a Group
	public static final String ID_subsecBody = "ID_subsecBody";
	// outText that is the title of a Group
	public static final String ID_subsecTitle = "ID_subsecTitle";
	
	// outText that is the Label of a Base Shift Part
	public static final String ID_lblBaseshift = "ID_lblBaseshift";
	// inputText that is the current index of the base shifter
	public static final String ID_currentBaseshift = "ID_currentBaseshift";
	// outText that is the maximum index of a base shifter
	public static final String ID_maxBaseshift = "ID_maxBaseshift";
	// commandLink to create a new base shifter part
	public static final String ID_createBaseshift = "ID_createBaseshift";
	// inputText that is the target index of the base shifter
	public static final String ID_targetBaseshift = "ID_targetBaseshift";
	// commandLink to go to the targetted base shifter
	public static final String ID_moveBaseshift = "ID_moveBaseshift";
	// commandLink to delete the current base shifter
	public static final String ID_deleteBaseshift = "ID_deleteBaseshift";
	
	// WAMCP specific controls for adding image association via the @facs attribute
	// TODO: refactor WAMCP specific
	public static final String ID_InpFacs = "ID_InpFacs";
	public static final String ID_CmdSetFacs = "ID_CmdSetFacs";
	public static final String ID_CmdShowfacs = "ID_CmdShowfacs";
	public static final String ID_CmdClearfacs = "ID_CmdClearfacs";
	public static final String ID_MsgFacs = "ID_MsgFacs";
	
	public static final String ID_InpFacsFOc = "ID_InpFacsFOc";
	public static final String ID_CmdSetFacsFOc = "ID_CmdSetFacsFOc";
	public static final String ID_CmdShowfacsFOc = "ID_CmdShowfacsFOc";
	public static final String ID_CmdClearfacsFOc = "ID_CmdClearfacsFOc";
	public static final String ID_MsgFacsFOc = "ID_MsgFacsFOc";
	
	public static final String ID_InpFacsCnt = "ID_InpFacsCnt";
	public static final String ID_CmdSetFacsCnt = "ID_CmdSetFacsCnt";
	public static final String ID_CmdShowfacsCnt = "ID_CmdShowfacsCnt";
	public static final String ID_CmdClearfacsCnt = "ID_CmdClearfacsCnt";
	public static final String ID_MsgFacsCnt = "ID_MsgFacsCnt";
	
	/**
	 * PARAM_ prefixed constants were meant to be used so that the parameter names are all collected here in one place,
	 * but this appeared to be an useless overhead, and wasn't continued. WAMCP_ prefixed constants are the same, but
	 * for things that are WAMCP specific (TODO: refactor)
	 * PS: Parameters are sent as attributes... parameters didn't work
	 */
	public static final String PARAM_SAVE_MSG = "PARAM_SAVE_MSG";
	public static final String PARAM_NEWMSDESC_BBEAN = "PARAM_NEWMSDESC_BBEAN";
	public static final String PARAM_SYNC_PARENTLOCATOR = "PARAM_SYNC_PARENTLOCATOR";
	public static final String PARAM_SYNC_PARENTINSTIX = "PARAM_SYNC_PARENTINSTIX";
	public static final String PARAM_LOGIN_BBEAN = "PARAM_LOGIN_BBEAN";
	public static final String PARAM_GOINTOAREA_AREAID = "PARAM_GOINTOAREA_AREAID";
	public static final String PARAM_SYNC_PARENTINSTID = "PARAM_SYNC_PARENTINSTID";
	public static final String PARAM_BASESHIFT_TARGET = "PARAM_BASESHIFT_TARGET";
	public static final String PARAM_BASESHIFT_PART = "PARAM_BASESHIFT_PART";
	public static final String PARAM_BASESHIFT_TARGET_IX = "PARAM_BASESHIFT_TARGET_IX";
	public static final String PARAM_BASESHIFT_AREAID = "PARAM_BASESHIFT_AREAID";
	public static final String WAMCP_FACS_ATTR_NAME = "facs"; // this is not used everywhere
	// TODONOT Use this instead of writing it.. No.. it's working and won't be changed
	public static final String WAMCP_FACS_PARENT_NAME = "facsimile";
	
	// Not an ID_ nor a BEANNAME_
	public static final String ID_ANNOTATORVALUESMAP_FOREL = XSAConstants.BEANNAME_BINDERS_BEAN
			+ ".annotatorAttrsValueMap";
}
