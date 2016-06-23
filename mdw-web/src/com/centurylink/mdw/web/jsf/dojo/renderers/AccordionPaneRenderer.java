/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.dojo.renderers;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.centurylink.mdw.web.jsf.dojo.components.AccordionPane;

public class AccordionPaneRenderer extends DojoRenderer
{
  public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
  {
    renderDojoRequire(facesContext, component, "dijit.layout.AccordionContainer");

    super.encodeBegin(facesContext, component);

    AccordionPane accordionPane = (AccordionPane) component;
    ResponseWriter writer = facesContext.getResponseWriter();
    if (accordionPane.getTitle() != null)
      writer.writeAttribute("title", accordionPane.getTitle(), "title");
    writer.writeAttribute("selected", new Boolean(accordionPane.isSelected()), "selected");
  }

}