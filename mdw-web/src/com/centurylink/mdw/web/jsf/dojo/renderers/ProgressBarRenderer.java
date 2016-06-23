/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.renderers;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.centurylink.mdw.web.jsf.dojo.components.ProgressBar;

public class ProgressBarRenderer extends DojoRenderer
{
  public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
  {
    renderDojoRequire(facesContext, component, "dijit.ProgressBar");
    super.encodeBegin(facesContext, component);
    
    ProgressBar progressBar = (ProgressBar) component;
    ResponseWriter writer = facesContext.getResponseWriter();     
    writer.writeAttribute("jsId", progressBar.getJsId(), "jsId");
    if (progressBar.isIndeterminate())
      writer.writeAttribute("indeterminate", "true", "indeterminate");
  }
}
