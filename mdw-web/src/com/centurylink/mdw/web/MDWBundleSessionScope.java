/**
 * Copyright (c) 2013 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.faces.context.FacesContext;
import javax.faces.event.PostConstructCustomScopeEvent;
import javax.faces.event.PreDestroyCustomScopeEvent;
import javax.faces.event.ScopeContext;

public class MDWBundleSessionScope extends ConcurrentHashMap<String,Object>
{
  static final String SCOPE_NAME = "mdwBundleSessionScope";

  public void notifyCreate(FacesContext facesContext)
  {
    ScopeContext context = new ScopeContext(SCOPE_NAME, this);
    facesContext.getApplication().publishEvent(facesContext, PostConstructCustomScopeEvent.class, context);
  }

  public void notifyDestroy(FacesContext facesContext)
  {
    ScopeContext scopeContext = new ScopeContext(SCOPE_NAME, this);
    facesContext.getApplication().publishEvent(facesContext, PreDestroyCustomScopeEvent.class, scopeContext);
  }

  public static void destroyScope()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    Map<String,Object> sessionMap = facesContext.getExternalContext().getSessionMap();
    MDWBundleSessionScope customScope = (MDWBundleSessionScope) sessionMap.remove(SCOPE_NAME);
    customScope.notifyDestroy(facesContext);
  }
}