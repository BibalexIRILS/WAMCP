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

package org.bibalex.util;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.faces.webapp.UIComponentTag;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.icesoft.faces.el.ELContextImpl;
import com.sun.facelets.el.DefaultVariableMapper;

public class FacesUtils {
	public static class ManagerinvFacesUtils {
		
		/**
		 * Add error message.
		 * 
		 * @param msg
		 *            the error message
		 */
		public static void addErrorMessage(String msg) {
			addErrorMessage(null, msg);
		}
		
		/**
		 * Add error message to a sepcific client.
		 * 
		 * @param clientId
		 *            the client id
		 * @param msg
		 *            the error message
		 */
		public static void addErrorMessage(String clientId, String msg) {
			FacesContext.getCurrentInstance().addMessage(clientId,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, msg));
		}
		
		/**
		 * Add information message.
		 * 
		 * @param msg
		 *            the information message
		 */
		public static void addInfoMessage(String msg) {
			addInfoMessage(null, msg);
		}
		
		/**
		 * Add information message to a sepcific client.
		 * 
		 * @param clientId
		 *            the client id
		 * @param msg
		 *            the information message
		 */
		public static void addInfoMessage(String clientId, String msg) {
			FacesContext.getCurrentInstance().addMessage(clientId,
					new FacesMessage(FacesMessage.SEVERITY_INFO, msg, msg));
		}
		
		/**
		 * Evaluate the integer value of a JSF expression.
		 * 
		 * @param el
		 *            the JSF expression
		 * @return the integer value associated with the JSF expression
		 */
		@SuppressWarnings("deprecation")
		public static Integer evalInt(String el) {
			if (el == null) {
				return null;
			}
			
			if (UIComponentTag.isValueReference(el)) {
				Object value = getElValue(el);
				
				if (value == null) {
					return null;
				} else if (value instanceof Integer) {
					return (Integer) value;
				} else {
					return new Integer(value.toString());
				}
			} else {
				return new Integer(el);
			}
		}
		
		private static Application getApplication() {
			ApplicationFactory appFactory = (ApplicationFactory) FactoryFinder
					.getFactory(FactoryFinder.APPLICATION_FACTORY);
			return appFactory.getApplication();
		}
		
		public static String getDireccionRemota() {
			return getServletRequest().getRemoteAddr();
		}
		
		@SuppressWarnings("deprecation")
		private static Object getElValue(String el) {
			return getValueBinding(el).getValue(FacesContext.getCurrentInstance());
		}
		
		private static String getJsfEl(String value) {
			return "#{" + value + "}";
		}
		
		/**
		 * Get managed bean based on the bean name.
		 * 
		 * @param beanName
		 *            the bean name
		 * @return the managed bean associated with the bean name
		 */
		@SuppressWarnings("deprecation")
		public static Object getManagedBean(String beanName) {
			Object o = getValueBinding(getJsfEl(beanName)).getValue(
					FacesContext.getCurrentInstance());
			
			return o;
		}
		
		public static String getMessageByKey(String key) {
			try {
				String messageBundleName = FacesContext.getCurrentInstance()
						.getApplication().getMessageBundle();
				ResourceBundle resourceBundle = ResourceBundle
						.getBundle(messageBundleName);
				
				return resourceBundle.getString(key);
			} catch (Exception e) {
				return key;
			}
			
		}
		
		/**
		 * Get parameter value from request scope.
		 * 
		 * @param name
		 *            the name of the parameter
		 * @return the parameter value
		 */
		public static String getRequestParameter(String name) {
			return (String) FacesContext.getCurrentInstance().getExternalContext()
					.getRequestParameterMap().get(name);
		}
		
		/**
		 * Get servlet context.
		 * 
		 * @return the servlet context
		 */
		public static ServletContext getServletContext() {
			return (ServletContext) FacesContext.getCurrentInstance().getExternalContext()
					.getContext();
		}
		
		public static HttpServletRequest getServletRequest() {
			return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
					.getRequest();
		}
		
		/**
		 * Retorna la session web.
		 * 
		 * @return
		 */
		public static HttpSession getSession() {
			ExternalContext ec = FacesContext.getCurrentInstance()
					.getExternalContext();
			HttpServletRequest request = (HttpServletRequest) ec.getRequest();
			HttpSession session = request.getSession();
			return session;
		}
		
		public static String getUrlActual() {
			return getServletRequest().getRequestURL().toString();
		}
		
		/**
		 * Convenience method to get the application's URL based on request
		 * variables.
		 */
		public static String getUrlAplicacion() {
			
			HttpServletRequest peticion = getServletRequest();
			
			StringBuffer url = new StringBuffer();
			int port = peticion.getServerPort();
			if (port < 0) {
				port = 80; // Work around java.net.URL bug
			}
			String scheme = peticion.getScheme();
			url.append(scheme);
			url.append("://");
			url.append(peticion.getServerName());
			if ((scheme.equals("http") && (port != 80))
					|| (scheme.equals("https") && (port != 443))) {
				url.append(':');
				url.append(port);
			}
			url.append(peticion.getContextPath());
			return url.toString();
		}
		
		@SuppressWarnings("deprecation")
		private static ValueBinding getValueBinding(String el) {
			return getApplication().createValueBinding(el);
		}
		
		/**
		 * Remove the managed bean based on the bean name.
		 * 
		 * @param beanName
		 *            the bean name of the managed bean to be removed
		 */
		@SuppressWarnings("deprecation")
		public static void resetManagedBean(String beanName) {
			getValueBinding(getJsfEl(beanName)).setValue(
					FacesContext.getCurrentInstance(), null);
		}
		
		/**
		 * Store the managed bean inside the session scope.
		 * 
		 * @param beanName
		 *            the name of the managed bean to be stored
		 * @param managedBean
		 *            the managed bean to be stored
		 */
		// @SuppressWarnings("unchecked")
		public static void setManagedBeanInSession(String beanName, Object managedBean) {
			FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put(beanName,
					managedBean);
		}
	}
	
	public static void addCtxVariable(FacesContext fCtx, String name, String value) {
		
		ELContext elCtx = fCtx.getELContext();
		ExpressionFactory expFactory = fCtx.getApplication().getExpressionFactory();
		
		VariableMapper varMapper = elCtx.getVariableMapper();
		if (varMapper == null) {
			varMapper = new DefaultVariableMapper();
			((ELContextImpl) elCtx).setVariableMapper(varMapper);
		}
		
		ValueExpression valExpr = expFactory.createValueExpression(elCtx, "#{" + name + "}",
				String.class);
		fCtx.getExternalContext().getRequestMap().put(name, value);
		varMapper.setVariable(name, valExpr);
		
	}
	
	/**
	 * Cleans up the component tree from facets and children under parent
	 * 
	 * @param container
	 */
	public static void clearChildrenAndFacets(UIComponent container) {
		
		List<UIComponent> children = container.getChildren();
		if (children != null) {
			children.clear();
		}
		
		Map<String, UIComponent> facets = container.getFacets();
		if (facets != null) {
			facets.clear();
		}
	}
	
	public static UIComponent findComponent(UIComponent base, String id) {
		if (id.equals(base.getId())) {
			return base;
		}
		
		UIComponent kid = null;
		UIComponent result = null;
		Iterator kids = base.getFacetsAndChildren();
		while (kids.hasNext() && (result == null)) {
			kid = (UIComponent) kids.next();
			if (id.equals(kid.getId())) {
				result = kid;
				break;
			}
			result = findComponent(kid, id);
			if (result != null) {
				break;
			}
		}
		return result;
	}
	
	public static UIComponent findComponentInRoot(String id) {
		UIComponent component = null;
		
		FacesContext facesContext = FacesContext.getCurrentInstance();
		if (facesContext != null) {
			UIComponent root = facesContext.getViewRoot();
			component = findComponent(root, id);
		}
		
		return component;
	}
	
	@Deprecated
	public static String getManagedBeanName(Object manBean) {
		String result = null;
		ExternalContext extConext = FacesContext.getCurrentInstance().getExternalContext();
		
		Map<String, Object> map = extConext.getApplicationMap();
		
		if (map.containsValue(manBean)) {
			for (Object key : extConext.getApplicationMap().keySet().toArray()) {
				if (manBean == map.get(key)) {
					result = (String) key;
					break;
				}
			}
		}
		
		// same for sessions, request, and none scopes.. .duh!!
		return result;
	}
	
}
