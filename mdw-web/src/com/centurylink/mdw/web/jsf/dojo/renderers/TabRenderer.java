/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.renderers;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.centurylink.mdw.web.jsf.dojo.components.Tab;

public class TabRenderer extends DojoLayoutRenderer
{
  public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
  {
    super.encodeBegin(facesContext, component);
    
    Tab tab = (Tab) component;
    ResponseWriter writer = facesContext.getResponseWriter();     
    writer.writeAttribute("title", tab.getTitle(), "title");
    if (tab.isSelected())
      writer.writeAttribute("selected", "true", "selected");
    if (tab.isClosable())
      writer.writeAttribute("closable", "true", "closable");
  }
}
