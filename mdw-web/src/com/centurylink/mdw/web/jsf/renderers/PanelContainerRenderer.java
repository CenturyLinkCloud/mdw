/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.renderers;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.component.html.ext.HtmlGraphicImage;
import org.apache.myfaces.component.html.ext.HtmlPanelGroup;
import org.apache.myfaces.renderkit.html.ext.HtmlGroupRenderer;
import org.apache.myfaces.renderkit.html.util.AddResource;
import org.apache.myfaces.renderkit.html.util.AddResourceFactory;
import org.apache.myfaces.shared_tomahawk.renderkit.RendererUtils;
import org.apache.myfaces.shared_tomahawk.renderkit.html.HTML;
import org.apache.myfaces.shared_tomahawk.renderkit.html.HtmlRendererUtils;
import org.richfaces.component.html.HtmlSimpleTogglePanel;

public class PanelContainerRenderer extends HtmlGroupRenderer
{
  public static final String RENDERER_TYPE = "com.centurylink.mdw.web.jsf.renderers.PanelContainerRenderer";

  public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException
  {
    AddResourceFactory.getInstance(facesContext).addJavaScriptAtPosition(facesContext, AddResource.HEADER_BEGIN, "/script/togglePanel.js");

    ResponseWriter writer = facesContext.getResponseWriter();
    boolean span = false;
    String element = HTML.DIV_ELEM;

    writer.startElement("table", component);
    writer.writeAttribute("cellpadding", "0", "cellpadding");
    writer.writeAttribute("cellspacing", "0", "cellspacing");
    writer.startElement("tr", component);
    writer.startElement("td", component);
    writer.writeAttribute("class", component.getAttributes().get("panelClass"), "class");
    writer.writeAttribute("height", "100%", "height");

    if (component.getId() != null && !component.getId().startsWith(UIViewRoot.UNIQUE_ID_PREFIX))
    {
      span = true;
      writer.startElement(element, component);
      HtmlRendererUtils.writeIdIfNecessary(writer, component, facesContext);
      HtmlRendererUtils.renderHTMLAttributes(writer, component, HTML.COMMON_PASSTROUGH_ATTRIBUTES);
    }
    else
    {
      span = HtmlRendererUtils.renderHTMLAttributesWithOptionalStartElement(writer, component, element, HTML.COMMON_PASSTROUGH_ATTRIBUTES);
    }

    RendererUtils.renderChildren(facesContext, component);

    if (span)
    {
      writer.endElement(element);
    }

    writer.endElement("td");
    renderClosedTogglePanels(facesContext, (HtmlPanelGroup)component);
  }

  private void renderClosedTogglePanels(FacesContext facesContext, HtmlPanelGroup panelContainer) throws IOException
  {
    // add a subsequent panelGroup
    HtmlPanelGroup secondGroup = (HtmlPanelGroup)facesContext.getApplication().createComponent("org.apache.myfaces.HtmlPanelGroup");
    secondGroup.setId(panelContainer.getId() + "_allClosed");
    secondGroup.setStyle("display:none;");
    secondGroup.setTransient(true);

    renderJavascript(facesContext, panelContainer, "somethingOpenedId = '" + panelContainer.getClientId(facesContext) + "';");
    renderJavascript(facesContext, secondGroup, "nothingOpenedId = '" + secondGroup.getClientId(facesContext) + "';");

    panelContainer.getParent().getChildren().add(secondGroup);

    boolean inited = false;
    // add the togglePanels and graphicImages
    for (UIComponent child : panelContainer.getChildren())
    {
      if (child instanceof HtmlSimpleTogglePanel)
      {
        HtmlSimpleTogglePanel openPanel = (HtmlSimpleTogglePanel) child;
        if (!inited)
        {
          String clientId = openPanel.getClientId(facesContext);
          renderJavascript(facesContext, openPanel, "clientIdPrefix = '" + clientId.substring(0, clientId.length() - openPanel.getId().length()) + "';");
          inited = true;
        }

        HtmlSimpleTogglePanel closedPanel = (HtmlSimpleTogglePanel) facesContext.getApplication().createComponent("org.richfaces.SimpleTogglePanel");
        closedPanel.setId(openPanel.getId() + "_closed");
        closedPanel.setSwitchType("client");
        closedPanel.setOpened(false);
        closedPanel.setOncollapse("return false;");
        closedPanel.setOnexpand("expandPanel(event, '" + openPanel.getId() + "'); return false;");
        closedPanel.setStyleClass(String.valueOf(openPanel.getAttributes().get("closedHeaderClass")));
        closedPanel.setHeaderClass(openPanel.getHeaderClass());
        secondGroup.getChildren().add(closedPanel);

        HtmlGraphicImage image = (HtmlGraphicImage)facesContext.getApplication().createComponent("org.apache.myfaces.HtmlGraphicImage");
        image.setValue(openPanel.getAttributes().get("closedHeaderImage"));
        image.setAlt(openPanel.getLabel());
        image.setOnclick("expandPanel(event, '" + openPanel.getId() + "'); return false;");
        image.setStyleClass("mdw_panelContainerImage");
        secondGroup.getChildren().add(image);
      }
    }
    ResponseWriter writer = facesContext.getResponseWriter();
    writer.startElement("td", panelContainer);
    writer.writeAttribute("align", "top", "align");

    secondGroup.encodeEnd(facesContext);

    writer.endElement("td");
    writer.endElement("tr");
    writer.endElement("table");
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