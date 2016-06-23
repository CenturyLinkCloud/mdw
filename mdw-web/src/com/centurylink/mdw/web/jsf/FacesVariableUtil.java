/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.FacesMessage;
import javax.faces.application.NavigationHandler;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.config.element.NavigationCase;
import org.apache.myfaces.config.element.NavigationRule;

import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.web.ui.UIError;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.util.WebUtil;

/**
 * Provides utility methods for interfacing with JSF resolvers.
 */
public class FacesVariableUtil
{
  /**
   * Convenience method for locating a faces context variable.
   * @param varName the name of the variable to locate
   * @return the value bound to the variable
   */
  public static Object getValue(String varName)
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    ELContext elContext = facesContext.getELContext();
    return elContext.getELResolver().getValue(elContext, null, varName);
  }

  /**
   * Convenience method for locating a faces variable for a base object.
   * @param base the base object whose attribute is being evaluated
   * @param varName the name of the variable to locate
   * @return the value bound to the variable
   */
  public static Object getValue(Object base, String varName)
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    ELContext elContext = facesContext.getELContext();
    return elContext.getELResolver().getValue(elContext, base, varName);
  }

  /**
   * Convenience method for setting a faces context variable.
   * @param varName name of the variable
   * @param value value to set
   */
  public static void setValue(String varName, Object value)
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    ELContext elContext = facesContext.getELContext();
    elContext.getELResolver().setValue(elContext, null, varName, value);
    facesContext.getExternalContext().getSessionMap().put(varName, value);
  }

  public static void setRequestValue(String varName, Object value)
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    ELContext elContext = facesContext.getELContext();
    elContext.getELResolver().setValue(elContext, null, varName, value);
  }

  /**
   * Convenience method for removing a faces context variable.
   * @param varName name of the variable to remove
   */
  public static void removeValue(String varName)
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    ELContext elContext = facesContext.getELContext();
    elContext.getELResolver().setValue(elContext, null, varName, null);
    facesContext.getExternalContext().getSessionMap().remove(varName);
  }

  /**
   * Set a variable at application scope.
   * @param varName name of the variable
   * @param value the variable value
   */
  public static void setApplicationValue(String varName, Object value)
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    facesContext.getExternalContext().getApplicationMap().put(varName, value);
  }

  /**
   * Convenience method for locating a request parameter value.
   * @param paramName name of the parameter
   * @return value of the request parameter
   */
  public static Object getRequestParamValue(String paramName)
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    return facesContext.getExternalContext().getRequestParameterMap().get(paramName);
  }

  public static String getRequestHeaderValue(String headerName)
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    return facesContext.getExternalContext().getRequestHeaderMap().get(headerName);
  }

  /**
   * Convenience method for locating a request attribute value.
   * @param attrName request attribute name
   * @return request attribute value
   */
  public static Object getRequestAttrValue(String attrName)
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    return facesContext.getExternalContext().getRequestMap().get(attrName);
  }

  /**
   * Convenience method for setting request attribute value.
   * @param attrName request attribute name
   * @param value request attribute value
   */
  public static void setRequestAttrValue(String attrName, Object value)
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    facesContext.getExternalContext().getRequestMap().put(attrName, value);
  }

  /**
   * Set a value on the session
   * @param name
   * @param value
   */
  public static void setSessionValue(String name, Object value)
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    facesContext.getExternalContext().getSessionMap().put(name, value);
  }

  /**
   * Convenience method for removing a request attribute value.
   * @param attrName request attribute name
   */
  public static void removeRequestAttrValue(String attrName)
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    facesContext.getExternalContext().getRequestMap().remove(attrName);
  }

  /**
   * Add a message to the faces context.
   * @param component the uicomponent responsible for the message
   * @param message the message summary
   */
  public static void addMessage(UIComponent component, String message)
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    FacesMessage facesMessage = new FacesMessage(message, "");
    facesContext.addMessage(component.getClientId(facesContext), facesMessage);
  }

  /**
   * Add a message to the faces context.
   * @param component the uicomponent responsible for the message
   * @param summaryMessage
   * @param detailMessage
   */
  public static void addMessage(UIComponent component, String summaryMessage, String detailMessage)
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    FacesMessage facesMessage = new FacesMessage(summaryMessage, detailMessage);
    facesContext.addMessage(component.getClientId(facesContext), facesMessage);
  }

  /**
   * Add a message to the faces context.
   * @param componentId the client id of the uicomponent responsible for the message
   * @param message the message summary
   */
  public static void addMessage(String componentId, String message)
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    FacesMessage facesMessage = new FacesMessage(message,"");
    String clientId = findComponentById(componentId).getClientId(facesContext);
    facesContext.addMessage(clientId, facesMessage);
  }

  /**
   * Add a message to faces context
   * @param componentId the client id of the uicomponent responsible for the message
   * @param summaryMessage
   * @param detailMessage
   */
  public static void addMessage(String componentId, String summaryMessage, String detailMessage)
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    FacesMessage facesMessage = new FacesMessage(summaryMessage,detailMessage);
    String clientId = findComponentById(componentId).getClientId(facesContext);
    facesContext.addMessage(clientId, facesMessage);
  }

  /**
   * Add a message to the faces context.
   * @param message the message summary
   */
  public static void addMessage(String message)
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    FacesMessage facesMessage = new FacesMessage(message, "");
    facesContext.addMessage(null, facesMessage);
  }

  public static void addMessage(Throwable t)
  {
    Throwable cause = t;
    while (cause.getCause() != null)
    {
      cause = cause.getCause();
    }
    if (cause instanceof UIException)
      addMessage(cause.getMessage());  // user message
    else
      addMessage(cause.toString());
  }

  public static Map<String,List<FacesMessage>> getMessages()
  {
    Map<String,List<FacesMessage>> facesMessages = null;

    FacesContext facesContext = FacesContext.getCurrentInstance();
    for (Iterator<String> idIter = facesContext.getClientIdsWithMessages(); idIter.hasNext();)
    {
      if (facesMessages == null)
        facesMessages = new HashMap<String,List<FacesMessage>>();

      String clientId = idIter.next();
      List<FacesMessage> clientList = new ArrayList<FacesMessage>();
      for (Iterator<FacesMessage> msgIter = facesContext.getMessages(clientId); msgIter.hasNext();)
        clientList.add(msgIter.next());
      facesMessages.put(clientId == null ? "null" : clientId, clientList);
    }

    return facesMessages;
  }

  public static void addMessages(Map<String,List<FacesMessage>> messageMap)
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();

    for (String clientId : messageMap.keySet())
    {
      for (FacesMessage message : messageMap.get(clientId))
      {
        if (clientId.equals("null"))
          facesContext.addMessage(null, message);
        else
          facesContext.addMessage(clientId, message);
      }
    }
  }

  /**
   * Programmatically perform faces navigation.
   * @param outcome the desired navigation outcome
   */
  public static void navigate(String outcome)
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    NavigationHandler navigator = facesContext.getApplication().getNavigationHandler();
    navigator.handleNavigation(facesContext, null, outcome);
  }

  /**
   * Navigate to an arbitrary (ie: external) URL by sending a client redirect.
   * @param url
   */
  public static void navigate(URL url) throws IOException
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    ExternalContext externalContext = facesContext.getExternalContext();

    externalContext.getFlash().setRedirect(true);
    externalContext.getFlash().setKeepMessages(true);
    try
    {
      externalContext.redirect(url.toString());
      facesContext.responseComplete();
      return;
    }
    catch (IOException e)
    {
      throw new FacesException(e.getMessage(), e);
    }
  }

  /**
   * Navigate to the UI error display.
   * @param message - error message
   * @param throwable - exception or error
   */
  public static void error(String message, Throwable throwable)
  {
    UIError error = new UIError(message, throwable);
    setValue("error", error);
    navigate("go_error");
  }

  /**
   * Determines whether a string is a value binding expression.
   */
  public static boolean isValueBindingExpression(String expression)
  {
    if (expression == null)
    {
      return false;
    }

    int start = 0;
    if (((start = expression.indexOf("#{")) != -1) && (start < expression.indexOf('}')))
    {
      return true;
    }

    return false;
  }

  /**
   * Returns the user who is currently logged in.
   * @return the authenticated user
   */
  public static AuthenticatedUser getCurrentUser()
  {
    return (AuthenticatedUser) getValue("authenticatedUser");
  }

  /**
   * Returns null if the package managed bean is empty.
   */
  public static PackageVO getCurrentPackage()
  {
    PackageVO packageVO = (PackageVO) getValue("mdwPackage");
    if (packageVO == null || packageVO.getPackageName() == null || packageVO.getPackageName().length() == 0)
      return null;
    else
      return packageVO;
  }

  /**
   * Check whether fromOutcome leads to the specified viewId in any JSF navigation rule.
   * @param fromOutcome
   * @param viewId
   * @return true if correspondence is found
   */
  public static boolean checkFromOutcomeLeadsToViewId(String fromOutcome, String viewId)
  {
    if (viewId == null || fromOutcome == null)
      return false;

    FacesContext facesContext = FacesContext.getCurrentInstance();
    RuntimeConfig runtimeConfig = RuntimeConfig.getCurrentInstance(facesContext.getExternalContext());
    Collection<NavigationRule> navigationRules = runtimeConfig.getNavigationRules();
    for (NavigationRule navigationRule : navigationRules)
    {
      // use reflection due to API change 2.0 -> 2.2 in MyFaces
      Object navCases = null;
      try
      {
        Method method = navigationRule.getClass().getMethod("getNavigationCases", (Class<?>[])null);
        navCases = method.invoke(navigationRule, (Object[])null);
      }
      catch (Exception ex)
      {
        // better not happen
        ex.printStackTrace();
      }

      if (navCases instanceof Collection)
      {

        Collection<?> navigationCases = (Collection<?>) navCases;
        for (Iterator<?> iter = navigationCases.iterator(); iter.hasNext(); )
        {
          NavigationCase navigationCase = (NavigationCase) iter.next();
          String viewIdToCompare = viewId;
          PackageVO packageVO = getCurrentPackage();
          if (packageVO != null)
          {
            int pkgIdx = viewId.indexOf(packageVO.getPackageName());
            if (pkgIdx >= 0)
                viewIdToCompare = viewId.substring(0, pkgIdx) + "facelets" + viewId.substring(pkgIdx + packageVO.getPackageName().length());
          }
          if (trimExt(viewIdToCompare).equals(trimExt(navigationCase.getToViewId())))
          {
            if (fromOutcome.equals(navigationCase.getFromOutcome()))
            {
              return true;
            }
            else if (isValueBindingExpression(fromOutcome))
            {
              try
              {
                Object x = evaluateExpression(fromOutcome);
                if (x != null && x.equals(navigationCase.getFromOutcome()))
                  return true;
              }
              catch (Exception ex)
              {
                // can't evaluate
              }
            }
          }
        }
      }
    }

    // handle JSF2 action navigation (action=pageOne.xhtml?faces-redirect=true)
    String outcomeNoParamsNoExt = fromOutcome;
    if (isValueBindingExpression(outcomeNoParamsNoExt))
    {
      try
      {
        Object obj = evaluateExpression(outcomeNoParamsNoExt);
        if (obj == null)
          return false;
        else
          outcomeNoParamsNoExt = obj.toString();
      }
      catch (Exception ex)
      {
        // can't evaluate
      }
    }
    if (outcomeNoParamsNoExt.indexOf('?') > 0)
      outcomeNoParamsNoExt = outcomeNoParamsNoExt.substring(0, outcomeNoParamsNoExt.indexOf('?'));
    if (outcomeNoParamsNoExt.indexOf('.') > 0)
      outcomeNoParamsNoExt = outcomeNoParamsNoExt.substring(0, outcomeNoParamsNoExt.lastIndexOf('.'));

    String viewIdNoExt = viewId;
    if (viewId.indexOf('.') > 0)
      viewIdNoExt = viewIdNoExt.substring(0, viewId.lastIndexOf('.'));

    if (viewIdNoExt.equals(outcomeNoParamsNoExt))
      return true;

    String path = viewIdNoExt;
    if (path.indexOf('/') >= 0)
      path = path.substring(0, path.lastIndexOf('/') + 1);
    if (viewIdNoExt.equals(path + outcomeNoParamsNoExt))
      return true;

    // workflow asset pages with custom navigation actions
    if (viewId.endsWith("/page.xhtml") && (fromOutcome.startsWith("nav.xhtml?toPage=") || fromOutcome.startsWith("navigation/")))
    {
      try
      {
        String pageString = fromOutcome.startsWith("nav.xhtml?toPage=") ? fromOutcome.substring(17) : fromOutcome.substring("navigation/".length());
        String pageName = URLEncoder.encode(pageString, "UTF-8");
        if (pageName.equals(getValue("pageName")))
          return true;
      }
      catch (UnsupportedEncodingException ex)
      {
        ex.printStackTrace();  // TODO
      }
    }

    // special handling for custom task pages
    if ("go_taskDetail".equals(fromOutcome) && viewId != null && viewId.endsWith("/page.xhtml"))
      return true;

    return false;
  }

  public static Object getObject(ValueExpression valueExpression)
  {
    ELContext elContext = FacesContext.getCurrentInstance().getELContext();
    return valueExpression == null ? null : valueExpression.getValue(elContext);
  }

  public static String getString(ValueExpression valueExpression)
  {
    return getString(valueExpression, null);
  }
  public static String getString(ValueExpression valueExpression, String defaultValue)
  {
    ELContext elContext = FacesContext.getCurrentInstance().getELContext();
    return valueExpression == null ? defaultValue : (String)valueExpression.getValue(elContext);
  }

  public static boolean getBoolean(ValueExpression valueExpression)
  {
    return getBoolean(valueExpression, false);
  }
  public static boolean getBoolean(ValueExpression valueExpression, boolean defaultValue)
  {
    ELContext elContext = FacesContext.getCurrentInstance().getELContext();
    Boolean value = valueExpression == null ? null : (Boolean)valueExpression.getValue(elContext);
    return value == null ? defaultValue : value.booleanValue();
  }

  public static int getInt(ValueExpression valueExpression)
  {
    return getInt(valueExpression, 0);
  }
  public static int getInt(ValueExpression valueExpression, int defaultValue)
  {
    ELContext elContext = FacesContext.getCurrentInstance().getELContext();
    Integer value = valueExpression == null ? null : (Integer)valueExpression.getValue(elContext);
    return value == null ? defaultValue : value.intValue();
  }

  public static Object evaluateExpression(String expr)
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    ELContext elContext = facesContext.getELContext();
    ValueExpression valueExpr = facesContext.getApplication().getExpressionFactory().createValueExpression(elContext, expr, Object.class);
    return valueExpr.getValue(elContext);
  }

  private static String trimExt(String in)
  {
    int idx = in.lastIndexOf('.');
    if (idx >= 0)
      return in.substring(0, in.lastIndexOf('.'));
    else
      return in;
  }

  public static UIComponent findComponentById(String componentId)
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    return facesContext.getViewRoot().findComponent(componentId);
  }

  public static void showError(String message) throws UIException
  {
    try
    {
      setValue("error", new UIError(message));
      FacesContext facesContext = FacesContext.getCurrentInstance();
      ExternalContext externalContext = facesContext.getExternalContext();
      HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
      HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();
      response.sendRedirect(request.getContextPath() + "/error.jsf");
      facesContext.responseComplete();
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public static String getProperty(String name)
  {
    PackageVO packageVO = FacesVariableUtil.getCurrentPackage();
    String value = null;
    if (packageVO != null)
        value = packageVO.getProperty(name);
    if (value == null)
        value = PropertyManager.getProperty(name);
    return value;
  }

  public static MethodExpression createMethodExpression(String expression, Class<?> returnType, Class<?>[] paramTypes)
  {
    if (expression == null)
      throw new NullPointerException("expression");
    if (paramTypes == null)
      paramTypes = new Class<?>[0];

    FacesContext facesContext = FacesContext.getCurrentInstance();
    ELContext elContext = facesContext.getELContext();
    return facesContext.getApplication().getExpressionFactory().createMethodExpression(elContext, expression, returnType, paramTypes);
  }

  public static ValueExpression createValueExpression(String expression, Class<?> expectedType)
  {
    if (expression == null)
      throw new NullPointerException("expression");

    FacesContext facesContext = FacesContext.getCurrentInstance();
    ELContext elContext = facesContext.getELContext();
    return facesContext.getApplication().getExpressionFactory().createValueExpression(elContext, expression, expectedType);
  }

  public static void clearFormComponents(UIComponent component)
  {
    UIComponent form = findParentForm(component);
    if (form != null)
      clearComponentHierarchy(form);
  }

  public static UIComponent findParentForm(UIComponent component)
  {
    for (UIComponent parent = component; parent != null; parent = parent.getParent())
    {
      if (parent instanceof UIForm)
      {
        return parent;
      }
    }
    return null;
  }

  public static void clearComponentHierarchy(UIComponent component)
  {
    if (component.isRendered())
    {
      if (component instanceof EditableValueHolder)
      {
        EditableValueHolder editableValueHolder = (EditableValueHolder) component;
        editableValueHolder.setSubmittedValue(null);
        editableValueHolder.setValue(null);
        editableValueHolder.setLocalValueSet(false);
        editableValueHolder.setValid(true);
      }

      for (Iterator<UIComponent> iterator = component.getFacetsAndChildren(); iterator.hasNext();)
      {
        clearComponentHierarchy(iterator.next());
      }
    }
  }

  public static UIComponent findComponentById(FacesContext context, UIComponent root, String id)
  {
    UIComponent component = null;
    for (int i = 0; i < root.getChildCount() && component == null; i++)
    {
      UIComponent child = (UIComponent) root.getChildren().get(i);
      component = findComponentById(context, child, id);
    }
    if (root.getId() != null)
    {
      if (component == null && root.getId().equals(id))
      {
        component = root;
      }
    }
    return component;
  }

  public static boolean isHtml5Rendering()
  {
    try
    {
      Class.forName("com.centurylink.mdw.hub.jsf.FacesUtil");
      return true;
    }
    catch (ClassNotFoundException ex)
    {
      return false;
    }
  }

  public static String getRequestContextRoot()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    return facesContext.getExternalContext().getRequestContextPath();
  }

  public static WebUtil getWebUtil()
  {
    return (WebUtil)getValue("webUtil");
  }

  public static void setSkin(String skin) throws FacesException
  {
    Object mdwObj = getValue("mdw");
    try
    {
      // use reflection as this may be called from MDWHub with a different MDW class
      Method setSkin = mdwObj.getClass().getMethod("setSkin", new Class[]{String.class});
      setSkin.invoke(mdwObj, new Object[]{skin});
    }
    catch (Exception ex)
    {
      throw new FacesException(ex.getMessage(), ex);
    }
  }
}
