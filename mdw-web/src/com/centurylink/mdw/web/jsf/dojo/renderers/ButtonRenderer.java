/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.renderers;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.centurylink.mdw.web.jsf.dojo.components.Button;

public class ButtonRenderer extends DojoRenderer
{
  public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
  {
    renderDojoRequire(facesContext, component, "dijit.form.Button");
    
    super.encodeBegin(facesContext, component);
    
    Button button = (Button) component;    
    ResponseWriter writer = facesContext.getResponseWriter();
    if (button.getIconClass() != null)
      writer.writeAttribute("iconClass", button.getIconClass(), "iconClass");
    if (button.getTitle() != null)
      writer.writeAttribute("title", button.getTitle(), "title");
  }
}
