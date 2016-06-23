/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.renderers;

import java.io.IOException;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.richfaces.component.html.HtmlSimpleTogglePanel;
import org.richfaces.renderkit.html.SimpleToggleControlTemplate;

public class TogglePanelRenderer extends SimpleToggleControlTemplate
{
  public static final String RENDERER_TYPE = "com.centurylink.mdw.web.jsf.renderers.TogglePanelRenderer";

  @Override
  public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
  {
    HtmlSimpleTogglePanel togglePanel = (HtmlSimpleTogglePanel) component;
    Object attr = togglePanel.getAttributes().get("opened");
    boolean opened = Boolean.parseBoolean(String.valueOf(attr));
    ValueExpression valueExpression = togglePanel.getValueExpression("opened");
    if (valueExpression != null)
      opened = Boolean.parseBoolean(String.valueOf(valueExpression.getValue(facesContext.getELContext())));
    togglePanel.setOpened(opened);
    if (opened)
      renderJavascript(facesContext, togglePanel, "currentlyExpanded = '" + togglePanel.getId() + "';");
    togglePanel.setOncollapse("collapsePanel('" + togglePanel.getId() + "');");
    togglePanel.setOnexpand("expandPanel(event, '" + togglePanel.getId() + "');");
    super.encodeBegin(facesContext, component);
  }

  protected void renderJavascript(FacesContext facesContext, UIComponent component, String script) throws IOException
  {
    ResponseWriter writer = facesContext.getResponseWriter();
    writer.startElement("script", component);
    writer.writeAttribute("type", "text/javascript", null);
    writer.write(script);
    writer.endElement("script");
  }



}
