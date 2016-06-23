/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.renderers;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.centurylink.mdw.web.jsf.dojo.components.Tooltip;

public class TooltipRenderer extends DojoRenderer
{
  public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
  {
    renderDojoRequire(facesContext, component, "dijit.Tooltip");
    
    Tooltip tooltip = (Tooltip) component;    
    ResponseWriter writer = facesContext.getResponseWriter();

    writer.startElement("span", tooltip);
    writer.writeAttribute("dojoType", tooltip.getDojoType(), "dojoType");
    if (tooltip.getId() != null)
      writer.writeAttribute("id", tooltip.getId(), "id");
    if (tooltip.getConnectId() != null)
      writer.writeAttribute("connectId", tooltip.getConnectId(), "connectId");
    if (tooltip.getClass() != null)
      writer.writeAttribute("class", tooltip.getClass(), null);

  }
  
  public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException
  {
    ResponseWriter writer = facesContext.getResponseWriter();
    writer.endElement("span");
    writer.write("\n");    
  }

  public void encodeChildren(FacesContext facesContext, UIComponent component) throws IOException
  {
    Tooltip tooltip = (Tooltip) component;    
    ResponseWriter writer = facesContext.getResponseWriter();
    if (tooltip.getLabel() != null)
    {
      writer.write(tooltip.getLabel());
    }
  }
  
}