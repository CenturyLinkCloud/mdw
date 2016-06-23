/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.renderers;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.centurylink.mdw.web.jsf.dojo.components.CheckBox;

public class CheckBoxRenderer extends DojoRenderer
{
  public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
  {
    renderDojoRequire(facesContext, component, "dijit.form.CheckBox");
    
    CheckBox checkBox = (CheckBox) component;
    ResponseWriter writer = facesContext.getResponseWriter();      
    writer.startElement("input", checkBox);
    writer.writeAttribute("dojoType", checkBox.getDojoType(), "dojoType");
    if (checkBox.getId() != null)
      writer.writeAttribute("id", checkBox.getId(), "id");
    writer.writeAttribute("type", "checkbox", null);
    if (checkBox.getStyleClass() != null)
      writer.writeAttribute("class", checkBox.getStyleClass(), "styleClass");
    if (checkBox.getStyle() != null)
      writer.writeAttribute("style", checkBox.getStyle(), "style");
    if (checkBox.getOnclick() != null)
      writer.writeAttribute("onclick", checkBox.getOnclick(), "onclick");
    writer.endElement("input");
    writer.write("\n");
    if (checkBox.getLabel() != null)
    {
      writer.startElement("label", checkBox);
      writer.writeAttribute("for", checkBox.getId(), null);
      if (checkBox.getLabelClass() != null)
        writer.writeAttribute("class", checkBox.getLabelClass(), null);
      writer.endElement("label");
      writer.write("\n");
    }
  }
  
  public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException
  {
  }
  
}
