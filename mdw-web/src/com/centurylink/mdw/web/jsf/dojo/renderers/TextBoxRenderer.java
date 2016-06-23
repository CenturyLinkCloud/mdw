/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.renderers;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.centurylink.mdw.web.jsf.dojo.components.TextBox;

public class TextBoxRenderer extends DojoRenderer
{
  public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
  {
    TextBox textBox = (TextBox) component;
    renderDojoRequire(facesContext, component, textBox.getDojoType());

    ResponseWriter writer = facesContext.getResponseWriter();
    writer.startElement("input", textBox);
    writer.writeAttribute("type", "text", null);
    writer.writeAttribute("dojoType", textBox.getDojoType(), "dojoType");
    if (textBox.getId() != null)
      writer.writeAttribute("id", textBox.getId(), "id");
    if (textBox.getInputValue() != null)
      writer.writeAttribute("value", textBox.getInputValue(), "inputValue");
    if (textBox.getSize() != 0)
      writer.writeAttribute("size", new Integer(textBox.getSize()), "size");
    writer.writeAttribute("required", new Boolean(textBox.isRequired()), "required");
    if (textBox.getConstraints() != null)
      writer.writeAttribute("constraints", textBox.getConstraints(), "constraints");
    if (textBox.getPromptMessage() != null)
      writer.writeAttribute("promptMessage", textBox.getPromptMessage(), "promptMessage");
    if (textBox.getInvalidMessage() != null)
      writer.writeAttribute("invalidMessage", textBox.getInvalidMessage(), "invalidMessage");
    if (textBox.getStyle() != null)
      writer.writeAttribute("style", textBox.getStyle(), "style");
    if (textBox.getOnchange() != null)
      writer.writeAttribute("onchange", textBox.getOnchange(), "onchange");
  }
  
  public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException
  {
    ResponseWriter writer = facesContext.getResponseWriter();
    writer.endElement("input");
    writer.write("\n");    
  }
}
