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

package org.bibalex.gallery.icefaces;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletRequest;

import org.bibalex.gallery.exception.BAGException;
import org.bibalex.gallery.model.BAGAlbum;
import org.bibalex.gallery.model.BAGImage;
import org.bibalex.gallery.model.BAGThumbnail;
import org.bibalex.sdxe.xsa.application.XSARequestBackingBean;

import com.icesoft.faces.component.dragdrop.DragEvent;
import com.icesoft.faces.component.ext.HtmlGraphicImage;
import com.icesoft.faces.component.ext.HtmlPanelGroup;
import com.icesoft.faces.component.paneldivider.PanelDivider;
import com.icesoft.faces.context.effects.CurrentStyle;
import com.icesoft.faces.context.effects.JavascriptContext;

public class BAGAlbumBBean implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8796814318885844684L;
// Not a request BBean because it coexists with one
	private BAGAlbum album;
	private String albumTemplateFilePathStr;
	// TODO refactor this WAMCP specific code
	private XSARequestBackingBean controllingReqBBean;
	
	private HtmlGraphicImage imageComp;
	
	private BAGImage image;
	
	private int viewPaneWidth;
// Maintaining this was impossible! private int viewPaneHeight;
	
	private int windowWidth;
	private int windowHeight;
	
	private static String JSCODE_CALCULATE_VIEWPANE_DIMS = ";"
			+ " var widthHidden = document.getElementById('WAMCPForm:albumViewPaneWidthHolder'); "
			+ " var heightHidden = document.getElementById('WAMCPForm:albumViewPaneHeightHolder'); "
			+ " var viewPane = document.getElementById('WAMCPForm:albumViewPane'); "
			+ " var changed = false; "
			+ " if(viewPane != null){ "
			+ " 	var dims = Element.getDimensions(viewPane); "
			+ " 	vpWidth = dims.width; "
			+ " 	vpHeight = dims.height; "
			+ " 	if(widthHidden.value != vpWidth){ "
			+ " 		widthHidden.value = vpWidth; "
			+ " 		changed = true; "
			+ " 	} "
			+ " 	if(heightHidden.value != vpHeight){ "
			+ " 		heightHidden.value = vpHeight; "
			+ " 		changed = true; "
			+ " 	} "
			+ " } ";
	
	private static String JSCODE_REPORT_VIEWPANE_DIMS = JSCODE_CALCULATE_VIEWPANE_DIMS
				// IE doesn't accept the JS when it sees the word MouseEvent!
			// + " if(MouseEvent) {"
// +
// "   iceSubmitPartial(document.getElementById('WAMCPForm'),document.getElementById('WAMCPForm:setViewPaneDims'),MouseEvent.CLICK); "
// + " } else { "
			+ " if(changed)  iceSubmitPartial(document.getElementById('WAMCPForm'),document.getElementById('WAMCPForm:setViewPaneDims')); ";
// + " } ";
	
	private static String JSCODE_REPOSITION_DRAGGABLE = ";"
			+ " var draggable = document.getElementById('WAMCPForm:imageDraggable'); "
			+ " draggable.style.left = '0px'; "
			+ " draggable.style.top = '0px'; ";
	
	private static String createJSCodeAdjustBGImageLeftTop(long pannedX, long pannedY,
			long levelWidthZoomed) {
		long left = -pannedX;
		long top = -pannedY;
		
		String result = ";"
						// The renderer calculates the absolute left and top relatvie to the viewpane alreaydy!!
// + " var parentViewPane = document.getElementById('WAMCPForm:albumViewPane'); "
// + " var viewPaneleft = viewPanetop = 0; "
// + " var obj = parentViewPane; "
// + " if (obj.offsetParent) { "
// + " 	do { "
// + " 		viewPaneleft += obj.offsetLeft; "
// + " 		viewPanetop += obj.offsetTop; "
// + " 	} while (obj = obj.offsetParent); "
// + " } "
				+ " var bgImage = document.getElementById('WAMCPForm:albumViewPaneFloatingBG'); "
								// + " bgImage.style.left = viewPaneleft - " + pannedX + "; "
// + " bgImage.style.top = viewPaneleft - " + pannedY + "; "
				+ " bgImage.style.left = '" + left + "px'; "
				+ " bgImage.style.top = '" + top + "px'; "
				+ " bgImage.style.width = '" + levelWidthZoomed + "px'; ";
		
		return result;
	}
	
	private long selectionMarkerTop = -1;
	
	private long selectionMarkerLeft = -1;
	
	private long selectionMarkerWidth = -1;
	
	private long selectionMarkerHeight = -1;
	
	private double panningDelta = 0.1;
	
	private HtmlGraphicImage backgroundFloatingImage;
	
	// Using this caused strange behavior for the "top" attribute!!!!!
	private String imageDraggableCursor = "move";
	
	private boolean pendingDjatokaRequest = false;
	
	private PanelDivider verticalDivider = null;
	
	private PanelDivider horDivider = null;
	
	private int headerHeight = 113;
	
	private int toolbarHeight = 45;
	
	public BAGAlbumBBean() {
		HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance()
				.getExternalContext().getRequest();
		this.albumTemplateFilePathStr = "albumThumbsSouth.xhtml";
		
		String userAgent = req.getHeader("user-agent");
		if (userAgent.contains("MSIE")) {
			this.albumTemplateFilePathStr = "IE7_" + this.albumTemplateFilePathStr;
		}
	}
	
	public void closeAlbum() {
		this.album = null;
		this.image = null;
	}
	
	protected int doClacViewPaneHeight() {
		// The view pane height is ready to be measure from the rendered output
		// only after the image is output (because when it had height:100% it caused
		// other kinds of problems). So we calculate it from the window height.
		if (this.horDivider == null) {
			return 0;
		}
		return Math.round(this.horDivider.getDividerPosition() / 100.0F
				* (this.windowHeight - this.headerHeight - this.toolbarHeight));
	}
	
	public void doSelectImageName(String imageName) throws BAGException {
		if (this.pendingDjatokaRequest) {
			return;
		} else {
			this.pendingDjatokaRequest = true;
			this.imageDraggableCursor = "wait";
		}
		this.setImage(this.album.openImage(imageName, this.viewPaneWidth, this
				.doClacViewPaneHeight()));
	}
	
	public String getAlbumTemplateFilePathStr() {
		return this.albumTemplateFilePathStr;
	}
	
	public HtmlGraphicImage getBackgroundFloatingImage() {
		return this.backgroundFloatingImage;
	}
	
	public int getHeaderHeight() {
		return this.headerHeight;
	}
	
	public PanelDivider getHorDivider() {
		return this.horDivider;
	}
	
	public BAGImage getImage() {
		return this.image;
	}
	
	public byte[] getImageBytes() {
		if (this.image != null) {
			return this.image.getZoomedBytes();
		} else {
			return new byte[] { 0 };
		}
	}
	
	public HtmlGraphicImage getImageComp() {
		return this.imageComp;
	}
	
	public String getImageDraggableCursor() {
		return this.imageDraggableCursor;
	}
	
	public Integer getImagesCount() {
		if (this.album == null) {
			return 0;
		} else {
			return Integer.valueOf(this.album.getImagesCount());
		}
	}
	
	public boolean getIsAlbumLoaded() {
		return this.album != null;
	}
	
	public double getPanningDelta() {
		return this.panningDelta;
	}
	
	public long getRotateDelta() {
		return this.image.getRotateDelta();
	}
	
	public Integer getSelectedIx() {
		if ((this.album == null) || (this.image == null)) {
			return 0;
		}
		int result = -1;
		try {
			result = Integer.valueOf(this.album.getIx(this.image));
		} catch (BAGException e) {
			this.controllingReqBBean.handleException(e);
		}
		return result;
	}
	
	public long getSelectionMarkerHeight() {
		return this.selectionMarkerHeight;
	}
	
	public long getSelectionMarkerLeft() {
		return this.selectionMarkerLeft;
	}
	
	public long getSelectionMarkerTop() {
		return this.selectionMarkerTop;
	}
	
	public long getSelectionMarkerWidth() {
		return this.selectionMarkerWidth;
	}
	
	public List<BAGThumbnail> getThumbnails() throws BAGException {
		List<BAGThumbnail> result;
		if (this.album == null) {
			result = new LinkedList<BAGThumbnail>();
		} else {
			result = this.album.getThumbnails();
		}
		
		return result;
		
	}
	
	public int getToolbarHeight() {
		return this.toolbarHeight;
	}
	
	public PanelDivider getVerticalDivider() {
		return this.verticalDivider;
	}
	
	public int getViewPaneHeight() {
		return this.doClacViewPaneHeight();
	}
	
	public int getViewPaneWidth() {
		return this.viewPaneWidth;
	}
	
	public int getWindowHeight() {
		return this.windowHeight;
	}
	
	public int getWindowWidth() {
		return this.windowWidth;
	}
	
	public double getZoomMultiplier() {
		return this.image.getZoomMultiplier();
	}
	
	public void imageDragListener(DragEvent ev) {
		if (this.image.getZoomedWidth() == this.image.getFullWidth()) {
			JavascriptContext.addJavascriptCall(FacesContext.getCurrentInstance(),
					JSCODE_REPOSITION_DRAGGABLE);
			return;
		}
		
		if (DragEvent.DROPPED != ev.getEventType()) {
			return;
		}
		
		if (this.pendingDjatokaRequest) {
			return;
		} else {
			this.pendingDjatokaRequest = true;
			this.imageDraggableCursor = "wait";
		}
		
		HtmlPanelGroup panel = (HtmlPanelGroup) ev.getComponent();
		
		CurrentStyle currStyle = panel.getCurrentStyle();
		String cssStr = currStyle.getCssString();
		
		int leftStart = cssStr.indexOf("left:") + 5;
		String deltaXStr =
				cssStr.substring(leftStart);
		deltaXStr = deltaXStr.substring(0, deltaXStr.indexOf("px"));
		int deltaX = Integer.parseInt(deltaXStr);
		
		int topStart = cssStr.indexOf("top:") + 4;
		String deltaYStr =
				cssStr.substring(topStart);
		deltaYStr = deltaYStr.substring(0, deltaYStr.indexOf("px"));
		int deltaY = Integer.parseInt(deltaYStr);
		
		if ((Math.abs(deltaX) >= 30) || (Math.abs(deltaY) >= 30)) {
			try {
				this.image.setViewPaneDims(this.viewPaneWidth, this.doClacViewPaneHeight(), false);
				this.setImage(this.image.pan(-deltaX, -deltaY));
			} catch (BAGException e) {
				this.controllingReqBBean.handleException(e);
			}
		}
		JavascriptContext.addJavascriptCall(FacesContext.getCurrentInstance(),
				JSCODE_REPOSITION_DRAGGABLE);
		
	}
	
	public void panDown(ActionEvent ev) {
		if (this.pendingDjatokaRequest) {
			return;
		} else {
			this.pendingDjatokaRequest = true;
			this.imageDraggableCursor = "wait";
		}
		try {
			this.image.setViewPaneDims(this.viewPaneWidth, this.doClacViewPaneHeight(), false);
			this.setImage(this.image.pan(0L, Math.round(this.panningDelta
					* this.doClacViewPaneHeight())));
		} catch (BAGException e) {
			this.controllingReqBBean.handleException(e);
		}
	}
	
	public void panLeft(ActionEvent ev) {
		if (this.pendingDjatokaRequest) {
			return;
		} else {
			this.pendingDjatokaRequest = true;
			this.imageDraggableCursor = "wait";
		}
		try {
			this.image.setViewPaneDims(this.viewPaneWidth, this.doClacViewPaneHeight(), false);
			this.setImage(this.image.pan(Math.round(this.panningDelta * this.viewPaneWidth), 0L));
		} catch (BAGException e) {
			this.controllingReqBBean.handleException(e);
		}
	}
	
	public void panRight(ActionEvent ev) {
		if (this.pendingDjatokaRequest) {
			return;
		} else {
			this.pendingDjatokaRequest = true;
			this.imageDraggableCursor = "wait";
		}
		try {
			this.image.setViewPaneDims(this.viewPaneWidth, this.doClacViewPaneHeight(), false);
			this.setImage(this.image.pan(-Math.round(this.panningDelta * this.viewPaneWidth), 0L));
		} catch (BAGException e) {
			this.controllingReqBBean.handleException(e);
		}
	}
	
	public void panUp(ActionEvent ev) {
		if (this.pendingDjatokaRequest) {
			return;
		} else {
			this.pendingDjatokaRequest = true;
			this.imageDraggableCursor = "wait";
		}
		try {
			this.image.setViewPaneDims(this.viewPaneWidth, this.doClacViewPaneHeight(), false);
			this.setImage(this.image.pan(0L, -Math.round(this.panningDelta
					* this.doClacViewPaneHeight())));
		} catch (BAGException e) {
			this.controllingReqBBean.handleException(e);
		}
	}
	
	public void refitToWindow(ActionEvent ev) {
		String style = this.verticalDivider.getStyle();
		StringTokenizer styleTokens = new StringTokenizer(style, ";", false);
		
		style = "";
		while (styleTokens.hasMoreTokens()) {
			String styleTok = styleTokens.nextToken();
			if (!styleTok.contains("height:")) {
				style += styleTok + ";";
			}
		}
		
		style += "height:" + (this.windowHeight - this.headerHeight) + "px;";
		
		this.verticalDivider.setStyle(style);
		
		JavascriptContext.addJavascriptCall(FacesContext.getCurrentInstance(),
				JSCODE_REPORT_VIEWPANE_DIMS);
	}
	
	public void requestBGImageReposition(ActionEvent ev) {
		if (this.image != null) {
			JavascriptContext.addJavascriptCall(FacesContext.getCurrentInstance(),
					createJSCodeAdjustBGImageLeftTop(this.image.getViewPaneX(), this.image
							.getViewPaneY(), this.image.getViewPaneFullWidth()));
		}
	}
	
	public void rotateAntiClockwise(ActionEvent ev) {
		if (this.pendingDjatokaRequest) {
			return;
		} else {
			this.pendingDjatokaRequest = true;
			this.imageDraggableCursor = "wait";
		}
		try {
			this.image.setViewPaneDims(this.viewPaneWidth, this.doClacViewPaneHeight(), false);
			this.setImage(this.image.rotateAntiClockwise());
		} catch (BAGException e) {
			this.controllingReqBBean.handleException(e);
		}
	}
	
	public void rotateClockwise(ActionEvent ev) {
		if (this.pendingDjatokaRequest) {
			return;
		} else {
			this.pendingDjatokaRequest = true;
			this.imageDraggableCursor = "wait";
		}
		try {
			this.setImage(this.image.rotateClockwise());
		} catch (BAGException e) {
			this.controllingReqBBean.handleException(e);
		}
	}
	
	public void selectImageIx(ActionEvent ev) {
		if (this.pendingDjatokaRequest) {
			return;
		} else {
			this.pendingDjatokaRequest = true;
			this.imageDraggableCursor = "wait";
		}
		try {
			int imgIx = ((Long) ev.getComponent().getAttributes().get(
					"PARAM_SELECTIMAGE_IX")).intValue();
			
			this.setImage(this.album.openImage(imgIx, this.viewPaneWidth, this
					.doClacViewPaneHeight()));
			
		} catch (BAGException e) {
			this.controllingReqBBean.handleException(e);
		}
		
	}
	
	public void selectImageName(ActionEvent ev) {
		try {
			String imageName = (String) ev.getComponent().getAttributes().get(
					"PARAM_SELECTIMAGE_NAME");
			
			this.doSelectImageName(imageName);
			
		} catch (BAGException e) {
			this.controllingReqBBean.handleException(e);
		}
	}
	
	public void setAlbum(BAGAlbum album) {
		this.album = album;
		
		try {
			this.setImage(this.album.openImage(0, this.viewPaneWidth, this.doClacViewPaneHeight()));
			
		} catch (BAGException e) {
			this.album = null;
			this.controllingReqBBean.handleException(e);
		}
		
	}
	
	public void setBackgroundFloatingImage(HtmlGraphicImage backgroundFloatingImage) {
		this.backgroundFloatingImage = backgroundFloatingImage;
	}
	
	public void setControllingReqBBean(XSARequestBackingBean controllingReqBBean) {
		this.controllingReqBBean = controllingReqBBean;
	}
	
	public void setHeaderHeight(int headerHeight) {
		this.headerHeight = headerHeight;
	}
	
	public void setHorDivider(PanelDivider horDivider) {
		this.horDivider = horDivider;
	}
	
	protected void setImage(BAGImage image) {
		
		this.image = image;
// YA 20101130B reducing client server communictation
// JavascriptContext.addJavascriptCall(FacesContext.getCurrentInstance(),
// JSCODE_REPORT_VIEWPANE_DIMS);
		
		// This is necessary
		this.requestBGImageReposition(null);
// END YA 20101130B
		
// if (this.imageComp != null) {
// // this.imageComp.setValue(image.getZoomedBytes());
//
// }
		
		this.pendingDjatokaRequest = false;
		this.imageDraggableCursor = "move";
	}
	
	public void setImageComp(HtmlGraphicImage imageComp) {
		this.imageComp = imageComp;
	}
	
	public void setPanningDelta(double panningDelta) {
		this.panningDelta = panningDelta;
	}
	
	public void setRotateDelta(long rotateDelta) {
		try {
			this.image.setRotateDelta(rotateDelta);
		} catch (BAGException e) {
			this.controllingReqBBean.handleException(e);
		}
	}
	
	public void setSelectionMarkerHeight(long selectionMarkerHeight) {
		this.selectionMarkerHeight = selectionMarkerHeight;
	}
	
	public void setSelectionMarkerLeft(long selectionMarkerLeft) {
		this.selectionMarkerLeft = selectionMarkerLeft;
	}
	
	public void setSelectionMarkerTop(long selectionMarkerTop) {
		this.selectionMarkerTop = selectionMarkerTop;
	}
	
	public void setSelectionMarkerWidth(long selectionMarkerWidth) {
		this.selectionMarkerWidth = selectionMarkerWidth;
	}
	
	public void setToolbarHeight(int toolbarHeight) {
		this.toolbarHeight = toolbarHeight;
	}
	
	public void setVerticalDivider(PanelDivider verticalDivider) {
		this.verticalDivider = verticalDivider;
	}
	
	public void setViewPaneDims(ActionEvent ev) {
		try {
			
			if (this.image != null) {
// YA 20101129 Trying to get the viewer to always work
				
				long viewPaneHeight = this.doClacViewPaneHeight();
				
				if ((this.image.getViewPaneWidth() == this.viewPaneWidth)
						&& (this.image.getViewPaneHeight() == viewPaneHeight)) {
					return;
				}
				
				this.setImage(this.image.setViewPaneDims(this.viewPaneWidth, viewPaneHeight, true));
				// Didn't have a very good effect that
// this.image.setViewPaneDims(this.viewPaneWidth,
// this.doClacViewPaneHeight(), false);
// this.setImage(this.image.zoomReset());
// END YA 20101129
				
				// cannot be done from the JS because the call is asynch and has to wait
				// but already done in setImage this.requestBGImageReposition(ev);
				
				JavascriptContext.addJavascriptCall(FacesContext.getCurrentInstance(),
						JSCODE_REPOSITION_DRAGGABLE);
			}
			
		} catch (BAGException e) {
			this.controllingReqBBean.handleException(e);
		} finally {
// this.viewPaneDimsChanged = false;
		}
		
	}
	
	public void setViewPaneWidth(int viewPaneWidth) {
// this.viewPaneDimsChanged = this.viewPaneDimsChanged
// || (Math.abs(this.viewPaneWidth - viewPaneWidth) > 50);
		this.viewPaneWidth = viewPaneWidth;
	}
	
	public void setWindowHeight(int windowHeight) {
		if (this.windowHeight == windowHeight) {
			return;
		}
		this.windowHeight = windowHeight;
		
		// this.calculateViewPaneHeight(null);
		this.setViewPaneDims(null);
	}
	
	public void setWindowWidth(int windowWidth) {
		this.windowWidth = windowWidth;
	}
	
	public void setZoomMultiplier(double zoomMultiplier) {
		this.image.setZoomMultiplier(zoomMultiplier);
	}
	
	// This is always reported as 0 or wrong:
// public void setViewPaneHeight(int viewPaneHeight) {
// this.viewPaneDimsChanged = this.viewPaneDimsChanged
// || (this.viewPaneHeight != viewPaneHeight);
// this.viewPaneHeight = viewPaneHeight;
// }
	public void verDivResize(ActionEvent ev) {
		this.viewPaneWidth = this.windowWidth;
	}
	
	public void zoomIn(ActionEvent ev) {
		if (this.pendingDjatokaRequest) {
			return;
		} else {
			this.pendingDjatokaRequest = true;
			this.imageDraggableCursor = "wait";
		}
		try {
			this.image.setViewPaneDims(this.viewPaneWidth, this.doClacViewPaneHeight(), false);
			this.setImage(this.image.zoomIn());
		} catch (BAGException e) {
			this.controllingReqBBean.handleException(e);
		}
	}
	
	public void zoomOut(ActionEvent ev) {
		if (this.pendingDjatokaRequest) {
			return;
		} else {
			this.pendingDjatokaRequest = true;
			this.imageDraggableCursor = "wait";
		}
		try {
			this.image.setViewPaneDims(this.viewPaneWidth, this.doClacViewPaneHeight(), false);
			this.setImage(this.image.zoomOut());
		} catch (BAGException e) {
			this.controllingReqBBean.handleException(e);
		}
	}
	
	public void zoomReset(ActionEvent event) {
		if (this.pendingDjatokaRequest) {
			return;
		} else {
			this.pendingDjatokaRequest = true;
			this.imageDraggableCursor = "wait";
		}
		try {
			this.image.setViewPaneDims(this.viewPaneWidth, this.doClacViewPaneHeight(), false);
			this.setImage(this.image.zoomReset());
		} catch (BAGException e) {
			this.controllingReqBBean.handleException(e);
		}
	}
	
	public void zoomSelection(ActionEvent event) {
		if (this.pendingDjatokaRequest) {
			return;
		} else {
			this.pendingDjatokaRequest = true;
			this.imageDraggableCursor = "wait";
		}
		try {
			this.image.setViewPaneDims(this.viewPaneWidth, this.doClacViewPaneHeight(), false);
			this.setImage(this.image.select(this.selectionMarkerLeft, this.selectionMarkerTop,
					this.selectionMarkerWidth)); // , this.selectionMarkerHeight));
		} catch (BAGException e) {
			this.controllingReqBBean.handleException(e);
		}
	}
	
}
