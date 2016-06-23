/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.ajax.renderers;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.centurylink.mdw.web.jsf.ajax.components.ProjectPanel;
import com.centurylink.mdw.web.jsf.dojo.renderers.DojoRenderer;

public class ProjectPanelRenderer extends DojoRenderer
{
  public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
  {
    renderJavascriptResource(facesContext, component, "../script/projectPanel.js");
    renderJavascriptResource(facesContext, component, "../script/AjaxRequest.js");
    
    renderDojoRequire(facesContext, component, "dojo.data.ItemFileReadStore");
    
    ProjectPanel projectPanel = (ProjectPanel) component;
    
    ResponseWriter writer = facesContext.getResponseWriter();      
    writer.startElement("div", component);
    writer.writeAttribute("id", projectPanel.getId(), "id");
    writer.writeAttribute("dojoType", "dojo.data.ItemFileReadStore", null);
    writer.writeAttribute("url", projectPanel.getDataUrl(), "dataUrl");
    writer.writeAttribute("jsid", projectPanel.getId(), "id");
  }

  public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException
  {
    super.encodeEnd(facesContext, component);
  }  
}
