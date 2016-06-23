/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.ajax.renderers;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.centurylink.mdw.web.jsf.ajax.components.DirPanel;
import com.centurylink.mdw.web.jsf.dojo.renderers.DojoRenderer;

public class DirPanelRenderer extends DojoRenderer
{
  public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
  {
    renderJavascriptResource(facesContext, component, "../script/dirpanel.js");
    renderJavascriptResource(facesContext, component, "../script/AjaxRequest.js");
    renderJavascriptResource(facesContext, component, "../script/Grep.js");
    renderJavascriptResource(facesContext, component, "../script/FileEdit.js");
    renderJavascriptResource(facesContext, component, "../script/PropEdit.js");
    renderJavascriptResource(facesContext, component, "../script/tail.js");
    
    renderDojoRequire(facesContext, component, "dojo.data.ItemFileReadStore");
    
    DirPanel dirPanel = (DirPanel) component;
    
    ResponseWriter writer = facesContext.getResponseWriter();      
    writer.startElement("div", component);
    writer.writeAttribute("id", dirPanel.getId(), "id");
    writer.writeAttribute("dojoType", "dojo.data.ItemFileReadStore", null);
    writer.writeAttribute("url", dirPanel.getDataUrl(), "dataUrl");
    writer.writeAttribute("showTimeStamps", dirPanel.isShowTimeStamps(), "showTimeStamps");
    writer.writeAttribute("jsid", dirPanel.getId(), "id");
  }

  public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException
  {
    super.encodeEnd(facesContext, component);
  }  
}
