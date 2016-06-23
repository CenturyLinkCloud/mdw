/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.view;

import java.io.IOException;
import java.util.Locale;

import javax.faces.FacesException;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.faces.view.ViewDeclarationLanguage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIError;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;

/**
 * ViewHandler decorator to provide support for Expression Language
 * evaluations in navigation rules.  Also adds support for URL parameters
 * in to-view-id for redirects.
 */
@SuppressWarnings("deprecation")
public class DynamicViewHandler extends ViewHandler
{
  private ViewHandler decoratedViewHandler;
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public DynamicViewHandler(ViewHandler viewHandler)
  {
    super();
    decoratedViewHandler = viewHandler;
  }

  /**
   * This is our custom behavior
   */
  public String getActionURL(FacesContext context, String viewId)
  {
    String value = viewId;
    if (isValueBindingExpression(viewId))
    {
      ValueBinding vb = context.getApplication().createValueBinding(viewId);
      value = vb.getValue(context).toString();
    }

    String result = decoratedViewHandler.getActionURL(context, value);

    // fix myfaces result ending in .xhtml suffix
    if (isJsfView(viewId) && result.endsWith(".xhtml"))
      result = result.substring(0, result.length() - 6) + ".jsf";

    int idxQueryStart = value.indexOf("?");
    if ((idxQueryStart > 0) && (result.indexOf("?") == -1))
    {
      result += value.substring(idxQueryStart);
    }
    return result;
  }

  private boolean isJsfView(String viewId)
  {
    int jsfIdx = viewId.indexOf(".jsf");
    if (jsfIdx == -1)
      return false;
    int qIdx = viewId.indexOf('?');
    if (qIdx == -1)
      return true;
    return viewId.substring(0, qIdx).endsWith(".jsf");
  }

  /**
   * Determines whether string is a value binding expression.
   */
  private boolean isValueBindingExpression(String expression)
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

  // delegating methods

  public Locale calculateLocale(FacesContext context)
  {
    return decoratedViewHandler.calculateLocale(context);
  }

  public String calculateRenderKitId(FacesContext context)
  {
    return decoratedViewHandler.calculateRenderKitId(context);
  }

  public UIViewRoot createView(FacesContext context, String viewId)
  {
    return decoratedViewHandler.createView(context, viewId);
  }

  public String getResourceURL(FacesContext context, String path)
  {
	  if (path!=null && path.startsWith("/database/")) {
			  int paramValIdx = path.lastIndexOf('/')+1;
			  String contextPath = context.getExternalContext().getRequestContextPath();
			  String url = contextPath + "/resource.xhtml?resourceName="
				  + path.substring(paramValIdx);
			  return url;
	  } else return decoratedViewHandler.getResourceURL(context, path);
  }

  public void renderView(FacesContext context, UIViewRoot viewToRender)
  throws IOException, FacesException
  {
    if (viewToRender.getViewId() == null)
    {
      FacesException facesException = new FacesException("MDW cannot locate the requested page: " + context.getExternalContext().getRequestServletPath());
      FacesVariableUtil.setValue("error", new UIError(facesException.getMessage(), facesException));
      viewToRender.setViewId("/error.xhtml");
      facesException.printStackTrace();
    }

    decoratedViewHandler.renderView(context, viewToRender);
  }

  public UIViewRoot restoreView(FacesContext context, String viewId)
  {
    if (viewId.endsWith("/error.jsf"))
    {
      // hack to overcome exception causing failure to display error page
      try
      {
        decoratedViewHandler.restoreView(context, viewId);
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
        // TODO: handle portlet
        HttpServletRequest request = (HttpServletRequest)context.getExternalContext().getRequest();
        HttpServletResponse response = (HttpServletResponse)context.getExternalContext().getResponse();
        if (request.getSession().getAttribute("error") == null)
        {
          logger.severe("Rendering Error"+ ex);
          UIError error = new UIError("Rendering Error", ex);
          request.getSession().setAttribute("error", error);
        }
        try
        {
          response.sendRedirect(request.getContextPath() + "/error.jsf");
          context.responseComplete();
          return null;
        }
        catch (IOException ioex)
        {
          ex.printStackTrace();
        }
      }
    }
    return decoratedViewHandler.restoreView(context, viewId);
  }

  public void writeState(FacesContext context) throws IOException
  {
    decoratedViewHandler.writeState(context);
  }

  private ViewDeclarationLanguageFactory vdlFactory;
  @Override
  public ViewDeclarationLanguage getViewDeclarationLanguage(FacesContext context, String viewId)
  {
    if (vdlFactory == null)
      vdlFactory = new ViewDeclarationLanguageFactory();
    return vdlFactory.getViewDeclarationLanguage(viewId);
  }

}
