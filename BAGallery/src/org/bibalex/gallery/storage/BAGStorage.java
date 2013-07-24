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

package org.bibalex.gallery.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs.AllFileSelector;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.VFS;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;
import org.bibalex.gallery.exception.BAGException;
import org.bibalex.util.URLPathStrUtils;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import edu.emory.mathcs.backport.java.util.Collections;

public abstract class BAGStorage {
	
	private static final int DEFAULT_CONN_TIMEOUT = 10000;
	
	public static URL cacheFileLocally(String cacheLocalPath, String fileUrlStr)
			throws BAGException {
		try {
// String extension = "";
//
// int lastDotIx = fileUrlStr.lastIndexOf('.');
// if (lastDotIx > fileUrlStr.lastIndexOf('/')) {
// extension = fileUrlStr.substring(lastDotIx);
// }
// String cacheFileName = "cached" + fileUrlStr.hashCode() + extension;
			
			int lastSlashIx = fileUrlStr.lastIndexOf('/');
			
			String cacheFileName = fileUrlStr.substring(lastSlashIx + 1);
			
			String cacheFilePath = URLPathStrUtils.appendParts(cacheLocalPath, cacheFileName);
			
			FileSystemManager fsMgr = VFS.getManager();
			
			final FileObject cacheFileFO = fsMgr.resolveFile(cacheFilePath);
			final FileObject cacheDirFO = fsMgr.resolveFile(cacheLocalPath);
			
			if (!cacheFileFO.exists()) {
				synchronized (BAGStorage.class) {
					
					if (!cacheDirFO.exists()) {
						cacheDirFO.createFolder();
					}
					
					cacheFileFO.createFile();
					
					OutputStream cacheFileOut = cacheFileFO.getContent().getOutputStream();
					try {
						readRemoteFile(fileUrlStr, cacheFileOut);
					} finally {
						cacheFileOut.close();
					}
					
				}
			}
			
			return cacheFileFO.getURL();
			
		} catch (FileSystemException e) {
			throw new BAGException(e);
		} catch (IOException e) {
			throw new BAGException(e);
		}
	}
	
	public static void copyFile(String sourceUrlStr, String targetUrlsStr) throws BAGException {
		try {
			FileObject targetFO = VFS.getManager().resolveFile(targetUrlsStr);
			FileObject sourceFO = VFS.getManager().resolveFile(sourceUrlStr);
			
			targetFO.copyFrom(sourceFO, new AllFileSelector());
		} catch (FileSystemException e) {
			throw new BAGException(e);
		}
	}
	
	public static List<String> listChildren(String dirUrlStr, FileType childrenType)
			throws BAGException {
		return listChildren(dirUrlStr, childrenType, DEFAULT_CONN_TIMEOUT);
	}
	
	public static List<String> listChildren(String dirUrlStr, FileType childrenType, int timeout)
			throws BAGException {
		
		try {
			List<String> result;
			
			if (new URI(dirUrlStr).getScheme().startsWith("http")) {
				BasicHttpParams httpParams = new BasicHttpParams();
				
				HttpConnectionParams.setConnectionTimeout(httpParams,
						timeout);
				
				DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);
				try {
					result = listChildrenApacheHttpd(dirUrlStr, childrenType,
							httpClient);
					
				} finally {
					// When HttpClient instance is no longer needed,
					// shut down the connection manager to ensure
					// immediate deallocation of all system resources
					httpClient.getConnectionManager().shutdown();
				}
			} else {
				
				result = new ArrayList<String>();
				
				FileSystemManager fsMgr = VFS.getManager();
				
				FileObject dir = fsMgr.resolveFile(dirUrlStr);
				
				final FileObject[] children = dir.getChildren();
				
				for (final FileObject child : children) {
					if ((childrenType == FileType.FILE_OR_FOLDER)
							|| (child.getType() == childrenType)) {
						result.add(child.getName().getBaseName());
					}
				}
			}
			// TODO define a comparator that orders the files properly as in
			// http://forums.sun.com/thread.jspa?threadID=5289401
			Collections.sort(result);
			
			return result;
			
		} catch (FileSystemException e) {
			throw new BAGException(e);
		} catch (URISyntaxException e) {
			throw new BAGException(e);
		}
	}
	
	protected static List<String> listChildrenApacheHttpd(String dirUrlStr, FileType childrenType,
			DefaultHttpClient httpclient) throws BAGException {
		ArrayList<String> result = new ArrayList<String>();
		
		HttpGet httpReq = new HttpGet(dirUrlStr);
		try {
			
			HttpResponse response = httpclient.execute(httpReq);
			
			if (response.getStatusLine().getStatusCode() / 100 != 2) {
				throw new BAGException("Connection error: " + response.getStatusLine().toString());
			}
			
			// Get hold of the response entity
			HttpEntity entity = response.getEntity();
			
			// If the response does not enclose an entity, there is no need
			// to bother about connection release
			if ((entity != null)) {
				
				entity = new BufferedHttpEntity(entity);
				
				if (entity.getContentType().getValue().startsWith("text/html")) {
					SAXBuilder saxBuilder = new SAXBuilder();
					
					saxBuilder.setFeature("http://xml.org/sax/features/validation", false);
					saxBuilder.setFeature("http://xml.org/sax/features/namespaces", true);
					saxBuilder.setFeature("http://xml.org/sax/features/namespace-prefixes",
							true);
					
					String htmlresponse = EntityUtils.toString(entity);
					String xmlResponse = light_html2xml.Html2Xml(htmlresponse);
					
					Document doc = saxBuilder.build(new StringReader(xmlResponse));
					XPath hrefXP = XPath.newInstance("//a/@href");
					
					for (Object obj : hrefXP.selectNodes(doc)) {
						Attribute attr = (Attribute) obj;
						String name = attr.getValue();
						if (name.startsWith("/")) {
							// parent dir
							continue;
						} else if (name.endsWith("/")) {
							if (childrenType.equals(FileType.FOLDER)
									|| childrenType.equals(FileType.FILE_OR_FOLDER)) {
								result.add(name.substring(0, name.length() - 1));
							}
						} else {
							if (childrenType.equals(FileType.FILE)
									|| childrenType.equals(FileType.FILE_OR_FOLDER)) {
								result.add(name);
							}
						}
					}
				}
			}
			
			return result;
			
		} catch (IOException ex) {
			
			// In case of an IOException the connection will be released
			// back to the connection manager automatically
			throw new BAGException(ex);
			
		} catch (RuntimeException ex) {
			
			// In case of an unexpected exception you may want to abort
			// the HTTP request in order to shut down the underlying
			// connection and release it back to the connection manager.
			httpReq.abort();
			throw ex;
			
		} catch (JDOMException e) {
			throw new BAGException(e);
		} finally {
			// connection closing is left for the caller to reuse connections
			
		}
		
	}
	
	public static InputStream openFileForInput(String fileUrlStr) throws BAGException {
		try {
			FileObject FO = VFS.getManager().resolveFile(fileUrlStr);
			return FO.getContent().getInputStream();
		} catch (FileSystemException e) {
			throw new BAGException(e);
		}
		
	}
	
	public static OutputStream openFileForOutput(String fileUrlStr, boolean bAppend)
			throws BAGException {
		try {
			FileSystemManager mgr = VFS.getManager();
			
			FileObject fileFO = mgr.resolveFile(fileUrlStr);
			
			return fileFO.getContent().getOutputStream(bAppend);
			
		} catch (FileSystemException e) {
			throw new BAGException(e);
		}
	}
	
	public static boolean putFile(String remoteUrlStr, File localFile,
			String contentType) throws BAGException {
		return putFile(remoteUrlStr, localFile, contentType, DEFAULT_CONN_TIMEOUT);
	}
	
	public static boolean putFile(String remoteUrlStr, File localFile,
			String contentType, int timeout) throws BAGException {
		HttpPut httpput = null;
		DefaultHttpClient httpclient = null;
		try {
			BasicHttpParams httpParams = new BasicHttpParams();
			
			HttpConnectionParams.setConnectionTimeout(httpParams,
					timeout);
			
			httpclient = new DefaultHttpClient(httpParams);
			URI remoteUrl = new URI(remoteUrlStr);
			String userInfo = remoteUrl.getUserInfo();
			if ((userInfo != null) && !userInfo.isEmpty()) {
				int colonIx = userInfo.indexOf(':');
				httpclient.getCredentialsProvider().setCredentials(
						new AuthScope(remoteUrl.getHost(), remoteUrl.getPort()),
						new UsernamePasswordCredentials(userInfo.substring(0, colonIx),
								userInfo.substring(colonIx + 1)));
			}
			
			if ((contentType == null) || contentType.isEmpty()) {
				contentType = "text/plain; charset=\"UTF-8\"";
			}
			
			FileEntity entity = new FileEntity(localFile, contentType);
			
			httpput = new HttpPut(remoteUrlStr);
			httpput.setEntity(entity);
			
			HttpResponse response = httpclient.execute(httpput);
			return response.getStatusLine().getStatusCode() - 200 < 100;
			
		} catch (IOException ex) {
			
			// In case of an IOException the connection will be released
			// back to the connection manager automatically
			throw new BAGException(ex);
			
		} catch (RuntimeException ex) {
			
			// In case of an unexpected exception you may want to abort
			// the HTTP request in order to shut down the underlying
			// connection and release it back to the connection manager.
			httpput.abort();
			throw ex;
			
		} catch (URISyntaxException e) {
// will be still null: httpput.abort();
			throw new BAGException(e);
		} finally {
			
			// When HttpClient instance is no longer needed,
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources
			httpclient.getConnectionManager().shutdown();
			
		}
	}
	
	public static void readRemoteFile(String fileUrlStr, OutputStream localOut) throws BAGException {
		try {
			try {
				FileObject imageFO;
				
				imageFO = VFS.getManager().resolveFile(fileUrlStr);
				
				InputStream imageIS = imageFO.getContent().getInputStream();
				
				byte buffer[] = new byte[10240];
				int bytesRead = 0;
				do {
					bytesRead = imageIS.read(buffer);
					
					if (bytesRead > 0) {
						localOut.write(buffer, 0, bytesRead);
					} else {
						break;
					}
				} while (true);
				
			} finally {
				localOut.flush();
			}
			
		} catch (FileSystemException e) {
			throw new BAGException(e);
		} catch (IOException e) {
			throw new BAGException(e);
			
		}
	}
	
}
