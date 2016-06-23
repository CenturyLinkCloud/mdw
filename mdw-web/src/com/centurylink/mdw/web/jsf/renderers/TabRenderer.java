/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.renderers;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorContext;
import javax.faces.component.behavior.ClientBehaviorHint;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.shared_impl.renderkit.ClientBehaviorEvents;
import org.apache.myfaces.shared_impl.renderkit.html.HTML;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlLinkRendererBase;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlRendererUtils;
import org.apache.myfaces.shared_impl.renderkit.html.util.FormInfo;
import org.apache.myfaces.shared_impl.renderkit.html.util.JavascriptUtils;
import org.apache.myfaces.shared_impl.renderkit.html.util.ResourceUtils;

import com.centurylink.mdw.web.jsf.components.Tab;
import com.centurylink.mdw.web.jsf.components.TabPanel;

public class TabRenderer extends HtmlLinkRendererBase
{
  public static final String RENDERER_TYPE = "com.centurylink.mdw.web.jsf.renderers.TabRenderer";


  @Override
  protected void renderCommandLinkStart(FacesContext facesContext, UIComponent component,
      String clientId, Object value, String style, String styleClass) throws IOException
  {

    if (!JavascriptUtils.isJavascriptAllowed(facesContext.getExternalContext()))
      throw new IOException("Javascript must be allowed in faces-config");
    FormInfo formInfo = findNestingForm(component, facesContext);
    if (formInfo == null)
      throw new IOException("No containing form for component ID: " + component.getId());

    ResponseWriter writer = facesContext.getResponseWriter();

    String[] passthroughAttrs;
    Map<String,List<ClientBehavior>> behaviors = ((ClientBehaviorHolder)component).getClientBehaviors();

    if (isDisabled(component))
    {
      if (((TabPanel)((Tab)component).getParent()).getHeaderClass() != null)
        renderMainTagStartCompat(facesContext, writer, component, style, styleClass);
      else
        renderMainTagStart(facesContext, writer, component, style, styleClass);
      writer.writeAttribute(HTML.ONCLICK_ATTR, ((HtmlCommandLink)component).getOnclick(), null);
    }
    else
    {
      renderBehaviorizedJavaScriptTagStart(facesContext, writer, component, clientId, behaviors, formInfo, style, styleClass);
    }

    if (!behaviors.isEmpty())
    {
      HtmlRendererUtils.writeIdAndName(writer, component, facesContext);
    }
    else
    {
      HtmlRendererUtils.writeIdIfNecessary(writer, component, facesContext);
    }
    HtmlRendererUtils.renderBehaviorizedEventHandlersWithoutOnclick(facesContext, writer, component, behaviors);
    HtmlRendererUtils.renderBehaviorizedFieldEventHandlersWithoutOnchangeAndOnselect(facesContext, writer, component, behaviors);
    passthroughAttrs = HTML.UNIVERSAL_ATTRIBUTES_WITHOUT_STYLE;

    HtmlRendererUtils.renderHTMLAttributes(writer, component, passthroughAttrs);
  }

  // TODO move to base class
  protected void renderMainTagStart(FacesContext facesContext, ResponseWriter writer, UIComponent component, String style, String styleClass)
  throws IOException {
      Tab tab = (Tab) component;
      TabPanel tabPanel = tab.getEnclosingPanel(facesContext);

      writer.startElement(HTML.DIV_ELEM, tab);
      if (styleClass != null) {
          writer.writeAttribute(HTML.CLASS_ATTR, styleClass, null);
      }
      else if (tabPanel.getTabClass() != null) {
          writer.writeAttribute(HTML.CLASS_ATTR, tabPanel.getTabClass(), null);
      }
      if (style != null)
          writer.writeAttribute(HTML.STYLE_ATTR, style, null);

      writer.startElement(HTML.DIV_ELEM, tab);
      writer.startElement(HTML.ANCHOR_ELEM, tab);

      writer.writeAttribute(HTML.HREF_ATTR, "#", null);
      if (tab.getTabindex() != null)
          writer.writeAttribute(HTML.TABINDEX_ATTR, tab.getTabindex(), null);
  }

  protected void renderMainTagEnd(FacesContext facesContext, ResponseWriter writer, UIComponent component)
  throws IOException {
      Tab tab = (Tab) component;
      TabPanel tabPanel = tab.getEnclosingPanel(facesContext);

      writer.writeText(tab.getLabel(), "label");
      writer.endElement(HTML.ANCHOR_ELEM);
      writer.endElement(HTML.DIV_ELEM);

      // active tab image
      if (tab.isActive() && tabPanel.getActiveTabImage() != null) {
          writer.startElement(HTML.IMG_ELEM, tab);
          writer.writeAttribute(HTML.SRC_ATTR, facesContext.getExternalContext().getRequestContextPath() + tabPanel.getActiveTabImage(), null);
          writer.writeAttribute(HTML.ALT_ATTR, "selected", null);
          String activeTabClass = tabPanel.getActiveTabImageClass();
          if (tab.getImageClass() != null)
            activeTabClass = activeTabClass == null ? tab.getImageClass() : activeTabClass + " " + tab.getImageClass();
          if (activeTabClass != null)
              writer.writeAttribute(HTML.CLASS_ATTR, activeTabClass, null);
          writer.endElement(HTML.IMG_ELEM);
      }

      writer.endElement(HTML.DIV_ELEM);
  }


  protected void renderMainTagStartCompat(FacesContext facesContext, ResponseWriter writer, UIComponent component, String style, String styleClass)
  throws IOException
  {
    Tab tab = (Tab) component;

    writer.startElement(HTML.TABLE_ELEM, tab);
    writer.writeAttribute(HTML.STYLE_ATTR, "width:100%;height:100%;" + style, null);
    if (styleClass != null)
      writer.writeAttribute(HTML.CLASS_ATTR, styleClass, null);
    writer.writeAttribute(HTML.BORDER_ATTR, "0", null);
    writer.writeAttribute(HTML.CELLSPACING_ATTR, "0", null);
    writer.writeAttribute(HTML.CELLPADDING_ATTR, "0", null);
    if (tab.getTabindex() != null)
      writer.writeAttribute(HTML.TABINDEX_ATTR, tab.getTabindex(), null);
  }

  protected void renderMainTagEndCompat(FacesContext facesContext, ResponseWriter writer, UIComponent component)
  throws IOException
  {
    Tab tab = (Tab) component;
    TabPanel tabPanel = tab.getEnclosingPanel(facesContext);

    writer.startElement(HTML.TBODY_ELEM, tab);
    writer.startElement(HTML.TR_ELEM, tab);
    writer.startElement(HTML.TD_ELEM, tab);
    writer.writeAttribute(HTML.ID_ATTR, tab.getClientId() + "_lbl", null);
    if (tab.isActive())
      writer.writeAttribute(HTML.CLASS_ATTR, tabPanel.getHeaderClass() + " " + tabPanel.getActiveTabClass(), null);
    else
      writer.writeAttribute(HTML.CLASS_ATTR, tabPanel.getHeaderClass() + " " + tabPanel.getInActiveTabClass(), null);
    writer.writeText(tab.getLabel(), "label");

    writer.endElement(HTML.TD_ELEM);
    writer.endElement(HTML.TR_ELEM);
    writer.endElement(HTML.TBODY_ELEM);
    writer.endElement(HTML.TABLE_ELEM);
  }

  @Override
  protected void renderCommandLinkEnd(FacesContext facesContext, UIComponent component)
  throws IOException
  {
    ResponseWriter writer = facesContext.getResponseWriter();
    if (((TabPanel)((Tab)component).getParent()).getHeaderClass() != null)
      renderMainTagEndCompat(facesContext, writer, component);
    else
      renderMainTagEnd(facesContext, writer, component);
  }

  protected void renderBehaviorizedJavaScriptTagStart(FacesContext facesContext, ResponseWriter writer,  UIComponent component,
      String clientId, Map<String, List<ClientBehavior>> behaviors, FormInfo formInfo, String style, String styleClass) throws IOException
  {
    String commandOnclick = ((HtmlCommandLink)component).getOnclick();

    // Calculate the script necessary to submit form
    String serverEventCode = buildServerOnclick(facesContext, component, clientId, formInfo);

    String onclick = null;

    if (commandOnclick == null &&
         (behaviors.isEmpty() ||
           (!behaviors.containsKey(ClientBehaviorEvents.CLICK) && !behaviors.containsKey(ClientBehaviorEvents.ACTION))))
    {
      // render only the submit script
      onclick = serverEventCode;
    }
    else
    {
      boolean hasSubmittingBehavior = hasSubmittingBehavior(behaviors, ClientBehaviorEvents.CLICK)
                || hasSubmittingBehavior(behaviors, ClientBehaviorEvents.ACTION);
      if (!hasSubmittingBehavior)
      {
        // ensure required resource javascript is available
        ResourceUtils.renderDefaultJsfJsInlineIfNecessary(facesContext, writer);
      }

      // render javascript that chains the related client code
      Collection<ClientBehaviorContext.Parameter> paramList = HtmlRendererUtils
          .getClientBehaviorContextParameters(HtmlRendererUtils.mapAttachedParamsToStringValues(facesContext, component));

      onclick = HtmlRendererUtils.buildBehaviorChain(facesContext, component,
          ClientBehaviorEvents.CLICK, paramList, ClientBehaviorEvents.ACTION, paramList, behaviors,
          commandOnclick, hasSubmittingBehavior ? null : serverEventCode);
    }

    if (((TabPanel)((Tab)component).getParent()).getHeaderClass() != null)
      renderMainTagStartCompat(facesContext, writer, component, style, styleClass);
    else
      renderMainTagStart(facesContext, writer, component, style, styleClass);

    writer.writeAttribute(HTML.ONCLICK_ATTR, onclick, null);
  }

  private boolean hasSubmittingBehavior(Map<String, List<ClientBehavior>> clientBehaviors, String eventName)
  {
    List<ClientBehavior> eventBehaviors = clientBehaviors.get(eventName);
    if (eventBehaviors != null && !eventBehaviors.isEmpty())
    {
      for (ClientBehavior behavior : eventBehaviors)
      {
        if (behavior.getHints().contains(ClientBehaviorHint.SUBMITTING))
        {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isDisabled(UIComponent component)
  {
    Object obj = component.getAttributes().get("disabled");
    if (obj instanceof String)
      return new Boolean((String)obj);
    else if (obj instanceof Boolean)
      return ((Boolean)obj).booleanValue();
    else
      return false;
  }

}
