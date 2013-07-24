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

package org.bibalex.gallery.model;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.bibalex.gallery.exception.BAGException;
import org.bibalex.gallery.model.BAGGalleryAbstract.EnumResolutions;
import org.bibalex.util.URLPathStrUtils;

public class BAGImage {
	private static final Logger LOG = Logger.getLogger("org.bibalex.gallery");
	
	private final BAGGalleryAbstract gallery;
	private final BAGAlbum album;
	private final String name;
	
	private final String thumbLocalUrl;
	
	// for internal use when zoomning in/out to switch between resolutions
	private final String lowResLocalUrl;
	private final String highResUrlStr;
	
	private final long fullWidth;
	private final long fullHeight;
	private final long zoomLevels;
	
	private final HttpClient httpclient;
	
	private byte[] zoomedBytes;
	
	private long zoomedX;
	private long zoomedY;
	
	private long zoomedWidth;
	private long zoomedHeight;
	
	private long zoomedRotate;
	
	private long viewPaneWidth;
	private long viewPaneHeight;
	
	private final ArrayList<NameValuePair> djatokaParams;
	
	private long enhancementFactor = 1;
	
	// impossible initial value
	private long djatokaLevel = -1;
	
	private double djatokaLevelScaleRatio = -1;
	private double zoomMultiplier = 1.5;
	
	private long rotateDelta = 90;
	
	private int minDjatokaLevel = 3;
	
	private double scaleFactor = 1.0; // the effect of this is very bad when width is large 0.66;
	
	public BAGImage(BAGGalleryAbstract gallery, BAGAlbum album,
			String name, long viewPaneWidth, long viewPaneHeight) throws BAGException {
		super();
		this.gallery = gallery;
		this.album = album;
		this.name = name;
		HttpGet djatokaReq = null;
		try {
			
			this.highResUrlStr = this.gallery.getImageDirectAccUrlStr(
					album.getName(),
					name, EnumResolutions.high);
			
// this.highResUrlEncoded = this.highResUrlStr.replaceAll(" ", "%20");
// new URLCodec("US-ASCII").encode(this.highResUrlStr);
			
			this.thumbLocalUrl = this.gallery.getThumbLocalUrl(this.album.getName(), this.name);
			
			Integer tempFullWidth = null;
			Integer tempFullHeight = null;
			Integer tempZoomLevels = null;
			
			// Execute HTTP request
			this.httpclient = new DefaultHttpClient();
			
			List<NameValuePair> qparams = new ArrayList<NameValuePair>();
			qparams.add(new BasicNameValuePair("url_ver", "Z39.88-2004"));
			qparams.add(new BasicNameValuePair("rft_id",
					this.highResUrlStr)); // "http://memory.loc.gov/gmd/gmd433/g4330/g4330/np000066.jp2"));
			qparams.add(new BasicNameValuePair("svc_id", "info:lanl-repo/svc/getMetadata"));
			
			URI serverUri = new URI(gallery.getDjatokaServerUrlStr());
			
			URI reqUri = URIUtils.createURI(serverUri.getScheme(), serverUri.getHost(), serverUri
					.getPort(), serverUri.getPath(),
					URLEncodedUtils.format(qparams, "US-ASCII"), null);
			
			djatokaReq = new HttpGet(reqUri);
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("Getting metadata of image via URL: " + djatokaReq.getURI());
			}
			
			HttpResponse response = this.httpclient.execute(djatokaReq);
			
			if (response.getStatusLine().getStatusCode() / 100 != 2) {
				throw new BAGException("Connection error: " + response.getStatusLine().toString());
			}
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("Response from URL: " + djatokaReq.getURI() + " => "
						+ response.getStatusLine());
			}
			
			// Get hold of the response entity
			HttpEntity entity = response.getEntity();
			
			// If the response does not enclose an entity, there is no need
			// to bother about connection release
			if ((entity != null)) {
				
				entity = new BufferedHttpEntity(entity);
				
				if ("application/json".equalsIgnoreCase(entity.getContentType().getValue())) {
					// Since the djatoka returned JSON is not properly escaped and I cannot find
					// any library that escapes JSON while parsing it I had to do this:
					String jsonString = EntityUtils.toString(entity);
					
					// remove the braces:
					jsonString = jsonString.substring(1);
					jsonString = jsonString.substring(0, jsonString.length() - 1);
					
					StringTokenizer pairTokens = new StringTokenizer(jsonString, ",", false);
					while (pairTokens.hasMoreElements()) {
						String pair = pairTokens.nextToken().trim();
						
						int colonIx = pair.indexOf(':');
						String memberName = pair.substring(0, colonIx);
						memberName = memberName.substring(1);
						memberName = memberName.substring(0, memberName.length() - 1);
						
						String memberValue = pair.substring(colonIx + 1).trim();
						memberValue = memberValue.substring(memberValue.indexOf('"') + 1);
						memberValue = memberValue.substring(0, memberValue.lastIndexOf('"'));
						
						if ("width".equals(memberName)) {
							tempFullWidth = Integer.valueOf(memberValue);
						} else if ("height".equals(memberName)) {
							tempFullHeight = Integer.valueOf(memberValue);
						} else if ("levels".equals(memberName)) {
							// FIXME replace "dwtLevels" by "levels" according to
// http://sourceforge.net/apps/mediawiki/djatoka/index.php?title=Djatoka_Level_Logic
// "dwtLevels" are the native JP2 DWT levels
							tempZoomLevels = Integer.valueOf(memberValue);
						}
					}
					
				}
			}
			
			if ((tempFullWidth == null) || (tempFullHeight == null) || (tempZoomLevels == null)) {
				throw new BAGException("Cannot retrieve metadata!");
			} else {
				this.fullWidth = tempFullWidth;
				this.fullHeight = tempFullHeight;
				this.zoomLevels = tempZoomLevels;
			}
			
		} catch (IOException ex) {
			
			// In case of an IOException the connection will be released
			// back to the connection manager automatically
			throw new BAGException(ex);
			
		} catch (RuntimeException ex) {
			
			// In case of an unexpected exception you may want to abort
			// the HTTP request in order to shut down the underlying
			// connection and release it back to the connection manager.
			djatokaReq.abort();
			throw ex;
			
		} catch (URISyntaxException e) {
			throw new BAGException(e);
// } catch (EncoderException e) {
// throw new BAGException(e);
		} finally {
			// connection kept alive and closed in finalize
		}
		
		this.djatokaParams = new ArrayList<NameValuePair>();
		this.djatokaParams.add(new BasicNameValuePair("url_ver", "Z39.88-2004"));
		this.djatokaParams.add(new BasicNameValuePair("rft_id",
				this.highResUrlStr)); // "http://memory.loc.gov/gmd/gmd433/g4330/g4330/np000066.jp2"));
		this.djatokaParams.add(new BasicNameValuePair("svc_id", "info:lanl-repo/svc/getRegion"));
		this.djatokaParams.add(new BasicNameValuePair("svc_val_fmt",
				"info:ofi/fmt:kev:mtx:jpeg2000"));
		this.djatokaParams.add(new BasicNameValuePair("svc.format", "image/jpeg"));
		
		this.zoomedX = 0;
		this.zoomedY = 0;
		this.zoomedWidth = this.fullWidth;
		this.zoomedHeight = this.fullHeight;
		this.zoomedRotate = 0;
		
		this.viewPaneHeight = viewPaneHeight;
		this.viewPaneWidth = viewPaneWidth;
		this.calculateDjatokaLevel();
		this.updateZoomedBytes();
		
		String lowResCache = URLPathStrUtils.appendParts(this.gallery.cacheLocalPath,
				"low");
		File tempJpg = new File(URLPathStrUtils.appendParts(lowResCache, name + ".jpg"));
		try {
			
			if (!tempJpg.exists()) {
				synchronized (BAGImage.class) {
					new File(lowResCache).mkdirs();
					tempJpg.createNewFile();
					FileOutputStream tempJpgOs = new FileOutputStream(tempJpg);
					ByteArrayInputStream temlJpgIS = new ByteArrayInputStream(this.zoomedBytes);
					try {
						byte buffer[] = new byte[10240];
						int bytesRead = 0;
						do {
							bytesRead = temlJpgIS.read(buffer);
							if (bytesRead > 0) {
								tempJpgOs.write(buffer, 0, bytesRead);
							} else {
								break;
							}
						} while (true);
						
					} finally {
						tempJpgOs.flush();
						tempJpgOs.close();
						
					}
				}
			}
			
		} catch (IOException e) {
			LOG.error("Couldn't create local cached version of low resolution version of: " + name);
			tempJpg = null;
		}
		if (tempJpg != null) {
			String ctxRootURL = new File(this.gallery.contextRootPath).toURI().toString();
			this.lowResLocalUrl = tempJpg.toURI().toString().substring(ctxRootURL.length());
			
		} else {
			this.lowResLocalUrl = this.thumbLocalUrl;
		}
		
	}
	
	private boolean calculateDjatokaLevel() {
// long newLevel = Math
// .round(Math.sqrt(((float) this.viewPaneWidth / this.zoomedWidth)
// * this.zoomLevels * this.zoomLevels));
//
// if (newLevel > this.zoomLevels) {
// newLevel = this.zoomLevels;
// }
//
// if (newLevel < this.minDjatokaLevel) {
// newLevel = this.minDjatokaLevel;
// }
		
		long regionLongestDim = -1;
		long regionLongestDimAtMaxLevel = -1;
		if (this.fullWidth >= this.fullHeight) {
			// longestDim --> Width
			
			regionLongestDim = this.viewPaneWidth;
			
			regionLongestDimAtMaxLevel = this.zoomedWidth;
			
		} else {
			// longestDim --> Height
			
			regionLongestDim = this.viewPaneHeight;
			
			regionLongestDimAtMaxLevel = this.zoomedHeight;
		}
		
		long newLevel = this.zoomLevels;
		double djLevelScaleFactor = 1.0;
		long tempDimDiv2 = -1;
		long tempDim = regionLongestDimAtMaxLevel;
		
		while (tempDim > regionLongestDim) {
			
			// See if reducing the level will result in using an image with dimensions less than that required
			tempDimDiv2 = tempDim >> 1;
			if (tempDimDiv2 < regionLongestDim) {
				break;
			}
			
			// Reduce the level
			djLevelScaleFactor = djLevelScaleFactor / 2.0;
			--newLevel;
			
			// Assign the new level dimension
			tempDim = tempDimDiv2;
		}
		
		if (this.djatokaLevel == newLevel) {
			return false;
		} else {
			boolean result = true; // (newLevel > this.djatokaLevel);
			
			this.djatokaLevel = newLevel;
			
			this.djatokaLevelScaleRatio = djLevelScaleFactor;
			// Math.pow(2, this.djatokaLevel - this.zoomLevels);
			
			return result;
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		// When HttpClient instance is no longer needed,
		// shut down the connection manager to ensure
		// immediate deallocation of all system resources
		this.httpclient.getConnectionManager().shutdown();
		
		super.finalize();
	}
	
	public BAGAlbum getAlbum() {
		return this.album;
	}
	
	public ArrayList<NameValuePair> getDjatokaParams() {
		return this.djatokaParams;
	}
	
	public long getEnhancementFactor() {
		return this.enhancementFactor;
	}
	
	public long getFullHeight() {
		return this.fullHeight;
	}
	
	public long getFullWidth() {
		return this.fullWidth;
	}
	
	public BAGGalleryAbstract getGallery() {
		return this.gallery;
	}
	
// private final String highResUrlEncoded;
	
	public String getHighResUrlStr() {
		return this.highResUrlStr;
	}
	
	public long getLevelWidth() {
		return Math.round(this.fullWidth * this.djatokaLevelScaleRatio);
	}
	
	public long getLevelWidthZoomed() {
		return Math.round(this.fullWidth * this.djatokaLevelScaleRatio * this.fullWidth
				/ (double) this.zoomedWidth);
	}
	
	public String getLowResLocalUrl() {
		return this.lowResLocalUrl;
	}
	
	public int getMinDjatokaLevel() {
		return this.minDjatokaLevel;
	}
	
	public String getName() {
		return this.name;
	}
	
	public long getRotateDelta() {
		return this.rotateDelta;
	}
	
	public double getScaleFactor() {
		return this.scaleFactor;
	}
	
	public String getThumbLocalUrl() throws BAGException {
		return this.thumbLocalUrl;
	}
	
	public long getViewPaneFullWidth() {
		double zoomRatio = (double) this.zoomedWidth / this.viewPaneWidth; // this.djatokaLevelScaleRatio
		return Math.round(this.fullWidth / zoomRatio);
	}
	
// public String getHighResUrlEncoded() {
// return this.highResUrlEncoded;
// }
	
	public long getViewPaneHeight() {
		return this.viewPaneHeight;
	}
	
	public long getViewPaneWidth() {
		return this.viewPaneWidth;
	}
	
	public long getViewPaneX() {
		double zoomRatio = (double) this.zoomedWidth / this.viewPaneWidth; // this.djatokaLevelScaleRatio
		return Math.round(this.zoomedX / zoomRatio);
	}
	
	public long getViewPaneY() {
		double zoomRatio = (double) this.zoomedWidth / this.viewPaneWidth; // this.djatokaLevelScaleRatio
		return Math.round(this.zoomedY / zoomRatio);
	}
	
	public byte[] getZoomedBytes() {
		return this.zoomedBytes;
	}
	
	public long getZoomedHeight() {
		return this.zoomedHeight;
	}
	
	public long getZoomedRotate() {
		return this.zoomedRotate;
	}
	
	public long getZoomedWidth() {
		return this.zoomedWidth;
	}
	
	public double getZoomedX() {
		return this.zoomedX;
	}
	
	public double getZoomedY() {
		return this.zoomedY;
	}
	
	public long getZoomLevels() {
		return this.zoomLevels;
	}
	
	public double getZoomMultiplier() {
		return this.zoomMultiplier;
	}
	
	public BAGImage pan(long viewPaneDeltaX, long viewPaneDeltaY) throws BAGException {
		double zoomRatio = (double) this.zoomedWidth / this.viewPaneWidth; // (double) this.fullWidth /
		// this.zoomedWidth;
		long deltaX = Math.round(viewPaneDeltaX * zoomRatio);
		long deltaY = Math.round(viewPaneDeltaY * zoomRatio);
		this.panInternal(deltaX, deltaY);
		return this;
	}
	
	private void panInternal(long deltaX, long deltaY) throws BAGException {
		this.zoomedX += deltaX;
		
		long maxX = this.fullWidth - this.zoomedWidth;
		if (this.zoomedX > maxX) {
			this.zoomedX = maxX;
		}
		
		if (this.zoomedX < 0) {
			this.zoomedX = 0;
		}
		
		this.zoomedY += deltaY;
		
		long maxY = this.fullHeight - this.zoomedHeight;
		if (this.zoomedY > maxY) {
			this.zoomedY = maxY;
		}
		
		if (this.zoomedY < 0) {
			this.zoomedY = 0;
		}
		
		this.updateZoomedBytes();
	}
	
	public BAGImage rotateAntiClockwise() throws BAGException {
		this.setZoomedRotate(this.zoomedRotate + this.rotateDelta);
		return this;
	}
	
	public BAGImage rotateClockwise() throws BAGException {
		this.setZoomedRotate(this.zoomedRotate - this.rotateDelta);
		return this;
	}
	
	public BAGImage select(long deltaX, long deltaY, long selWidth)
			throws BAGException {
		double newWidth = ((double) selWidth / this.viewPaneWidth) * this.zoomedWidth;
		
		if (newWidth > this.fullWidth) {
			newWidth = this.fullWidth;
		}
		
		if (newWidth < 100) {
			newWidth = 100;
		}
		
		double newHeight = this.zoomedHeight * newWidth / this.zoomedWidth;
		
		if (newHeight > this.fullHeight) {
			newHeight = this.fullHeight;
		}
		
		if (newHeight < 100) {
			newHeight = 100;
		}
		
		deltaX = Math.round(((double) deltaX / this.viewPaneWidth) * this.zoomedWidth);
		// The viewpane pixel to zoomed image ratio is the same whether you get it from the width or height
		// and since the view pane height is not relevant to zoomed height (scrolling) then we must use width
		deltaY = Math.round(((double) deltaY * this.zoomedWidth) / this.viewPaneWidth);
		
		this.zoomedWidth = Math.round(newWidth);
		
		this.zoomedHeight = Math.round(newHeight);
		
		this.calculateDjatokaLevel();
		
		this.panInternal(deltaX, deltaY);
		return this;
	}
	
	public BAGImage setEnhancementFactor(long enhancementFactor) throws BAGException {
		if (this.enhancementFactor == enhancementFactor) {
			return this;
		}
		this.enhancementFactor = enhancementFactor;
		this.calculateDjatokaLevel();
		this.updateZoomedBytes();
		return this;
	}
	
	public void setMinDjatokaLevel(int minDjatokaLevel) {
		this.minDjatokaLevel = minDjatokaLevel;
	}
	
	public BAGImage setRotateDelta(long rotateDelta) throws BAGException {
		if (rotateDelta % 90 != 0) {
			throw new BAGException("Rotate delta must be a multiple of 90 degrees");
		}
		this.rotateDelta = rotateDelta;
		return this;
	}
	
	public void setScaleFactor(double scaleFactor) {
		this.scaleFactor = scaleFactor;
	}
	
	public BAGImage setViewPaneDims(long viewPaneWidth, long viewPaneHeight, boolean allowUpdate)
			throws BAGException {
		if ((this.viewPaneWidth == viewPaneWidth) && (this.viewPaneHeight == viewPaneHeight)) {
			return this;
		}
		
// YA 20101129 Trying to make the viewer work properly
// boolean update = allowUpdate && ((viewPaneHeight - this.viewPaneHeight > 50)
// || (viewPaneWidth - this.viewPaneWidth > 50));
		
		this.viewPaneHeight = viewPaneHeight;
		this.viewPaneWidth = viewPaneWidth;
		// don't shortcut calculateDjatokaLevel()
		if (this.calculateDjatokaLevel() && allowUpdate) {// update) {
			this.updateZoomedBytes();
		} else {
			LOG.trace("Skipping update of image after view pane resize.");
		}
		return this;
	}
	
	public BAGImage setZoomedRotate(long zoomedRotate) throws BAGException {
		while (zoomedRotate < 0) {
			zoomedRotate += 360;
		}
		while (zoomedRotate > 360) {
			zoomedRotate -= 360;
		}
		if (this.zoomedRotate == zoomedRotate) {
			return this;
		}
		this.zoomedRotate = zoomedRotate;
		this.updateZoomedBytes();
		return this;
	}
	
	public BAGImage setZoomMultiplier(double zoomMultiplier) {
		this.zoomMultiplier = zoomMultiplier;
		return this;
	}
	
	private void updateZoomedBytes() throws BAGException {
		HttpGet getReq = null;
		try {
			
			ArrayList<NameValuePair> reqParams = (ArrayList<NameValuePair>) this.djatokaParams
					.clone();
			
// Curiously the X and Y coordinates need not be scaled using the level scale ratio
// long djatokaY = Math.round(this.zoomedY * this.djatokaLevelScaleRatio);
// long djatokaX = Math.round(this.zoomedX * this.djatokaLevelScaleRatio);
			
			long djatokaWidth = Math.round((this.zoomedWidth > 0 ? this.zoomedWidth
					: this.fullWidth)
					* this.djatokaLevelScaleRatio);
			
			// make use of view pane height to minimize retrieved image
			if (this.zoomedWidth == this.fullWidth) {
				this.zoomedHeight = this.fullHeight; // To allow the retrieval of the full image
			} else {
				double zoomedPixelRatio = (double) this.zoomedWidth / this.viewPaneWidth;
				if (Math.abs(Math.round((double) this.zoomedHeight / zoomedPixelRatio)
						- this.viewPaneHeight) > 10) {
					this.zoomedHeight = Math.round(this.viewPaneHeight * zoomedPixelRatio);
				}
			}
			
			long djatokaHeight = Math.round((this.zoomedHeight > 0 ? this.zoomedHeight
					: this.fullHeight)
					* this.djatokaLevelScaleRatio);
			
			reqParams.add(new BasicNameValuePair("svc.rotate", "" + this.zoomedRotate));
			
			reqParams.add(new BasicNameValuePair("svc.scale", ""
					+ Math.round(this.viewPaneWidth * this.scaleFactor) + ",0"));
			reqParams.add(new BasicNameValuePair("svc.level", "" + this.djatokaLevel));
			reqParams.add(new BasicNameValuePair("svc.region",
								// "" + djatokaY + "," + djatokaX + ","
					"" + this.zoomedY + "," + this.zoomedX + ","
							+ djatokaHeight + "," + djatokaWidth));
			
			URI serverUri = new URI(this.gallery.getDjatokaServerUrlStr());
			
			URI reqUri = URIUtils.createURI(serverUri.getScheme(), serverUri.getHost(), serverUri
					.getPort(), serverUri.getPath(),
					URLEncodedUtils.format(reqParams, "US-ASCII"), null);
			
			getReq = new HttpGet(reqUri);
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("Getting region via URL: " + getReq.getURI());
			}
			
			HttpResponse response = this.httpclient.execute(getReq);
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("Response from URL: " + getReq.getURI() + " => "
						+ response.getStatusLine());
			}
			
			// Get hold of the response entity
			HttpEntity entity = response.getEntity();
			
			// If the response does not enclose an entity, there is no need
			// to bother about connection release
			if ((entity != null)) {
				
				entity = new BufferedHttpEntity(entity);
				
				if ("image/jpeg".equalsIgnoreCase(entity.getContentType().getValue())) {
					this.zoomedBytes = EntityUtils.toByteArray(entity);
				}
			} else {
				
				throw new BAGException("Cannot retrieve region!");
			}
			
		} catch (IOException ex) {
			
			// In case of an IOException the connection will be released
			// back to the connection manager automatically
			throw new BAGException(ex);
			
		} catch (RuntimeException ex) {
			
			// In case of an unexpected exception you may want to abort
			// the HTTP request in order to shut down the underlying
			// connection and release it back to the connection manager.
			getReq.abort();
			throw ex;
			
		} catch (URISyntaxException e) {
			throw new BAGException(e);
		} finally {
			// connection kept alive and closed in finalize
		}
		
	}
	
	public BAGImage zoomIn() throws BAGException {
		double newWidth = this.zoomedWidth / this.zoomMultiplier;
		
		double newHeight = this.zoomedHeight / this.zoomMultiplier;
		
		this.zoomInternal(newWidth, newHeight);
		return this;
	}
	
	private void zoomInternal(double newWidth, double newHeight) throws BAGException {
		
		long deltaX = Math.round(((this.zoomedWidth - newWidth) / 2));
		
		this.zoomedWidth = Math.round(newWidth);
		
		// ////////////////////////////////
		
		long deltaY = Math.round(((this.zoomedHeight - newHeight) / 2));
		
		this.zoomedHeight = Math.round(newHeight);
		
		// //////////////////////
		
		this.calculateDjatokaLevel();
		
		this.panInternal(deltaX, deltaY);
		
	}
	
	public BAGImage zoomOut() throws BAGException {
		
		double newWidth = this.zoomedWidth * this.zoomMultiplier;
		if (newWidth > this.fullWidth) {
			newWidth = this.fullWidth;
		}
		
		double newHeight = this.zoomedHeight * this.zoomMultiplier;
		if (newHeight > this.fullHeight) {
			newHeight = this.fullHeight;
		}
		
		this.zoomInternal(newWidth, newHeight);
		
		return this;
	}
	
	public BAGImage zoomReset() throws BAGException {
		this.zoomedX = 0;
		this.zoomedY = 0;
		this.zoomedWidth = this.fullWidth;
		this.zoomedHeight = this.fullHeight;
		this.zoomedRotate = 0;
		this.calculateDjatokaLevel();
		this.updateZoomedBytes();
		
		return this;
	}
	
}
