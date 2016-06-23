/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.ajax;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.web.filepanel.FileEdit;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.property.groups.PropEdit;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * General-purpose phase listener to support Ajax requests.
 */
public class AjaxPhaseListener implements PhaseListener
{
  private static final long serialVersionUID = 1L;

  private static final Logger _logger = Logger.getLogger(AjaxPhaseListener.class.getName());

  public static final String RESPONSE_STATUS_SUCCESS = "success";
  public static final String AJAX_REQUEST = "ajax-request";
  public static final String AJAX_XML_REQUEST = "ajax-xml-request";

  public PhaseId getPhaseId()
  {
    return PhaseId.RESTORE_VIEW;
  }

  public void beforePhase(PhaseEvent event)
  {
    ExternalContext externalContext = event.getFacesContext().getExternalContext();
    // TODO: portlet support
    HttpServletRequest servletRequest = (HttpServletRequest)externalContext.getRequest();
    if (servletRequest.getSession().isNew())
    {
      _logger.info("-- new ajax session --");
      // set session timeout to a reasonable value since managed beans are stored on the session
      servletRequest.getSession().setMaxInactiveInterval(3600);
    }
    String uri = servletRequest.getRequestURI();

    if (externalContext.getRequestParameterMap().containsKey("user"))
    {
      // set the user on the session
      String cuid = (String) externalContext.getRequestParameterMap().get("user");
      try
      {
        AuthenticatedUser user = RemoteLocator.getUserManager().loadUser(cuid);
        FacesVariableUtil.setValue("authenticatedUser", user);
        externalContext.redirect("index.jsf");
        event.getFacesContext().responseComplete();
      }
      catch (Exception ex)
      {
        _logger.log(Level.SEVERE, ex.getMessage(), ex);
      }
    }
    else if (uri.endsWith("/filepanel") || uri.endsWith("/property") || uri.endsWith("/configManager"))
    {
      try
      {
        externalContext.redirect(uri + "/index.jsf");
        event.getFacesContext().responseComplete();
      }
      catch (IOException ex)
      {
        _logger.log(Level.SEVERE, ex.getMessage(), ex);
      }
    }
    else if (externalContext.getRequestParameterMap().containsKey("filepath"))
    {
      ELContext elContext = event.getFacesContext().getELContext();
      FileEdit fileEdit = (FileEdit) elContext.getELResolver().getValue(elContext, null, "fileEdit");
      if (fileEdit != null)
      {
        try
        {
          fileEdit.setFilePath(externalContext.getRequestParameterMap().get("filepath"));
        }
        catch (IOException ex)
        {
          _logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
      }
    }
    else if (externalContext.getRequestParameterMap().containsKey("proppath"))
    {
      ELContext elContext = event.getFacesContext().getELContext();
      PropEdit propEdit = (PropEdit) elContext.getELResolver().getValue(elContext, null, "propEdit");
      if (propEdit != null)
      {
        propEdit.setFullName(externalContext.getRequestParameterMap().get("proppath"));
      }
    }
  }

  public void afterPhase(PhaseEvent event)
  {
    FacesContext facesContext = event.getFacesContext();
    UIViewRoot viewRoot = facesContext.getViewRoot();
    String rootId = null;
    if (viewRoot != null)
      rootId = viewRoot.getViewId();
    if (rootId != null)
    {
      if (rootId.indexOf(AJAX_REQUEST) != -1)
        handleAjaxRequest(event, false);
      if (rootId.indexOf(AJAX_XML_REQUEST) != -1)
        handleAjaxRequest(event, true);
    }
  }

  private void handleAjaxRequest(PhaseEvent event, boolean isXml)
  {
    FacesContext facesContext = event.getFacesContext();
    ExternalContext externalContext = facesContext.getExternalContext();
    ELContext elContext = facesContext.getELContext();

    // TODO: portlet support
    HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

    long start = System.currentTimeMillis();
    Map<?,?> params = externalContext.getRequestParameterMap();
    // handle params for setting input values before evaluation
    if (params.containsKey("configView.workflowApp")) {
      // do this first since order dependent
      ValueExpression valueExpr = facesContext.getApplication().getExpressionFactory().createValueExpression(elContext, "#{configView.workflowApp}", Object.class);
      valueExpr.setValue(elContext, params.get("configView.workflowApp"));
    }
    for (Iterator<?> paramKeys = params.keySet().iterator(); paramKeys.hasNext(); )
    {
      String paramKey = (String) paramKeys.next();
      if (paramKey.startsWith("dojo"))
        continue;
      String paramVal = (String) params.get(paramKey);
      if (!paramKey.equals("values"))
      {
        String paramExpression = paramKey;
        ValueExpression valueExpr = facesContext.getApplication().getExpressionFactory().createValueExpression(elContext, "#{" + paramExpression + "}", Object.class);
        Class<?> valueType = valueExpr.getType(elContext);

        // support int and boolean native types
        if (valueType.getName().equals("int"))
          valueExpr.setValue(elContext, new Integer(paramVal));
        else if (valueType.getName().equals("boolean"))
          valueExpr.setValue(elContext, new Boolean(paramVal));
        else
          valueExpr.setValue(elContext, paramVal);
      }
    }

    StringBuffer responseBuf = new StringBuffer();
    String[] valueExpressions = null;
    for (Iterator<?> paramKeys = params.keySet().iterator(); paramKeys.hasNext(); )
    {
      String paramKey = (String) paramKeys.next();
      if (paramKey.equals("values"))
      {
        valueExpressions = ((String)externalContext.getRequestParameterMap().get("values")).split(",");
      }
    }

    if (isXml)
    {
      // html-response
      responseBuf.append(getResponseValue(valueExpressions[0], facesContext));
    }
    else
    {
      // json-response: create value bindings for the response contents and build response
      responseBuf.append("{");
      for (int i = 0; i < valueExpressions.length; i++)
      {
        String valueExpression = valueExpressions[i];
        responseBuf.append("\"").append(valueExpression.replace('.', '_')).append("\":");
        responseBuf.append("\"").append(getResponseValue(valueExpression, facesContext).replaceAll("\n","\\\\n")).append("\"");
        if (i < valueExpressions.length - 1)
          responseBuf.append(",");
      }
      responseBuf.append("}");
    }

    long finish = System.currentTimeMillis();
    _logger.fine("ajax request processing time: " + (finish - start));

    try
    {
      response.setContentType("text/plain");
      response.setHeader("Cache-Control", "no-cache");
      response.getWriter().write(responseBuf.toString());
      response.getWriter().flush();
    }
    catch (IOException ex)
    {
      _logger.log(Level.SEVERE, ex.getMessage(), ex);
    }

    event.getFacesContext().responseComplete();
  }

  private String getResponseValue(String valueExpression, FacesContext facesContext)
  {
    ELContext elContext = facesContext.getELContext();
    ValueExpression valueExpr = facesContext.getApplication().getExpressionFactory().createValueExpression(elContext, "#{" + valueExpression + "}", Object.class);
    Object value = valueExpr.getValue(elContext);
    return value == null ? "" : value.toString();
  }

}