/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.renderers;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

/**
 * Renders an individual criterion row inside a SortableList filter.
 */
public class CriteriaRenderer extends Renderer
{
  public static final String RENDERER_TYPE = "com.centurylink.mdw.web.jsf.renderers.CriteriaRenderer";

  public boolean getRendersChildren() { return false; }

  public void encodeBegin(FacesContext context, UIComponent crit)
    throws IOException
  {
    ResponseWriter writer = context.getResponseWriter();
    writer.write('\n');
    writer.writeComment("CRITERIA START");

    writer.write('\n');
    writer.startElement("tr", crit);
    writer.write('\n');
    writer.startElement("td", crit);
    writer.writeAttribute("class", crit.getParent().getAttributes().get("labelRowClass"), "class");
    writer.write('\n');
    writer.startElement("strong", crit);
    writer.write('\n');
    writer.startElement("label", crit);
    writer.write("" + crit.getAttributes().get("label"));
    writer.endElement("label");
    writer.write('\n');
    writer.endElement("strong");
    writer.write('\n');
    writer.endElement("td");
    writer.write('\n');
    writer.endElement("tr");
    writer.write('\n');
    writer.startElement("tr", crit);
    writer.write('\n');
    writer.startElement("td", crit);
    writer.writeAttribute("class", crit.getParent().getAttributes().get("inputRowClass"), "class");
    writer.writeAttribute("height", crit.getParent().getAttributes().get("inputTdHeight"), "class");
    writer.writeAttribute("valign", "top", "class");
  }

  public void encodeEnd(FacesContext context, UIComponent crit)
    throws IOException
  {
    ResponseWriter writer = context.getResponseWriter();

    writer.endElement("td");
    writer.write('\n');
    writer.endElement("tr");
    writer.write('\n');

    writer.writeComment("CRITERIA END");
  }

}
