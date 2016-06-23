/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.navigation;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.application.NavigationCase;
import javax.faces.application.NavigationHandler;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;

import org.apache.myfaces.application.NavigationHandlerImpl;

import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.MDWBase;

public class MDWNavigationHandler extends NavigationHandlerImpl
{
  private NavigationHandler wrappedHandler;
  private boolean passthrough;

  public MDWNavigationHandler(NavigationHandler navHandler)
  {
    this(navHandler, true);
  }

  public MDWNavigationHandler(NavigationHandler navHandler, boolean passthrough)
  {
    this.wrappedHandler = navHandler;
  }

  @Override
  public void handleNavigation(FacesContext facesContext, String fromAction, String outcome)
  {
    if (outcome != null && outcome.startsWith("nav.xhtml"))
    {
      int toPageIdx = outcome.indexOf("toPage=");
      if (toPageIdx > 0)
      {
        String toPage = outcome.substring(toPageIdx + 7);

        try
        {
          FacesVariableUtil.setValue("pageName", URLEncoder.encode(toPage, "UTF-8"));
          ViewHandler viewHandler = facesContext.getApplication().getViewHandler();
          UIViewRoot viewRoot = viewHandler.createView(facesContext, "/page.jsf");
          facesContext.setViewRoot(viewRoot);
          facesContext.renderResponse();
        }
        catch (Exception ex)
        {
          ex.printStackTrace();
        }
      }
      else
      {
        int classIdx = outcome.indexOf("class=");
        if (classIdx > 0)
        {
          try
          {
            MDWBase mdw = (MDWBase) FacesVariableUtil.getValue("mdw");
            mdw.performAction(outcome);
            ViewHandler viewHandler = facesContext.getApplication().getViewHandler();
            UIViewRoot viewRoot = viewHandler.createView(facesContext, "/form.jsf");
            facesContext.setViewRoot(viewRoot);
            facesContext.renderResponse();
          }
          catch (Exception ex)
          {
            ex.printStackTrace();
          }
        }
        else
        {
          System.err.println("Missing toPage or class parameter.");
          throw new IllegalStateException("Missing toPage or class parameter for custom navigation.");
        }
      }
    }
    else
    {
      // compensate for myfaces hosing up the to view id (excluding richfaces ajax requests)
      if ((outcome == null || outcome.trim().length() == 0) && FacesVariableUtil.getRequestParamValue("AJAXREQUEST") == null)
      {
        outcome = facesContext.getViewRoot().getViewId();  // stay on the same page
      }

      if ("customPage".equals(outcome))
        outcome = "/" + FacesVariableUtil.getValue("pageName");

      ExternalContext externalContext = facesContext.getExternalContext();
      if (externalContext.getRequestParameterMap().get("AJAXREQUEST") == null)
      {
        PackageVO packageVO = FacesVariableUtil.getCurrentPackage();
        if (packageVO != null)
        {
          NavigationCase navCase = getNavigationCase(facesContext, fromAction, outcome);
          String toViewId = navCase.getToViewId(facesContext);
          if (toViewId != null && (toViewId.startsWith("/facelets/") || toViewId.startsWith("/page.jsf")))
          {
            // redirect to package-specific URL since packageVO is not null
            String pkgName = packageVO.getPackageName();
            String pkgPath = toViewId.startsWith("/facelets/") ? toViewId.substring(10).replaceAll("\\.xhtml", ".jsf") : toViewId.substring(1).replaceAll("\\.xhtml", ".jsf");
            String newToViewId = "/" + pkgName + "/" + pkgPath;

            ViewHandler viewHandler = facesContext.getApplication().getViewHandler();
            String redirectPath = viewHandler.getRedirectURL(facesContext, newToViewId, navCase.getParameters(), navCase.isIncludeViewParams());

            // clear ViewMap if we are redirecting to other resource
            UIViewRoot viewRoot = facesContext.getViewRoot();
            if (viewRoot != null && !viewRoot.getViewId().equals(newToViewId))
            {
              // call getViewMap(false) to prevent unnecessary map creation
              Map<String, Object> viewMap = viewRoot.getViewMap(false);
              if (viewMap != null)
                viewMap.clear();
            }

            PartialViewContext partialViewContext = facesContext.getPartialViewContext();
            if (partialViewContext.isPartialRequest() && !partialViewContext.isRenderAll() && !facesContext.getViewRoot().getViewId().equals(newToViewId))
                partialViewContext.setRenderAll(true);

            externalContext.getFlash().setRedirect(true);
            externalContext.getFlash().setKeepMessages(true);
            try
            {
              externalContext.redirect(redirectPath);
              facesContext.responseComplete();
              return;
            }
            catch (IOException e)
            {
              throw new FacesException(e.getMessage(), e);
            }

          }
        }
      }

      if (passthrough)
        wrappedHandler.handleNavigation(facesContext, fromAction, outcome);
      else
        super.handleNavigation(facesContext, fromAction, outcome);
    }
  }
}
