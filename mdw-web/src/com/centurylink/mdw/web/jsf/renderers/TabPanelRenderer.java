/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.renderers;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ValueChangeEvent;
import javax.faces.event.ValueChangeListener;
import javax.faces.render.Renderer;

import org.apache.myfaces.shared_impl.renderkit.html.HTML;

import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.jsf.components.Tab;
import com.centurylink.mdw.web.jsf.components.TabPanel;

public class TabPanelRenderer extends Renderer
{
  public static final String RENDERER_TYPE = "com.centurylink.mdw.web.jsf.renderers.TabPanelRenderer";

  public boolean getRendersChildren()
  {
    return true;
  }

  @Override
  public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
  {
    TabPanel tabPanel = (TabPanel) component;
    PackageVO packageVO = FacesVariableUtil.getCurrentPackage();
    if (packageVO == null)
      tabPanel.setPackageName(null);
    else
      tabPanel.setPackageName(packageVO.getPackageName());

    ResponseWriter writer = facesContext.getResponseWriter();
    writer.startElement(HTML.TABLE_ELEM, tabPanel);
    writer.writeAttribute(HTML.ID_ATTR, component.getClientId(), null);
    if (tabPanel.getStyleClass() != null)
      writer.writeAttribute(HTML.CLASS_ATTR, tabPanel.getStyleClass(), "styleClass");
    if (tabPanel.getStyle() != null)
      writer.writeAttribute(HTML.STYLE_ATTR, tabPanel.getStyle(), "style");
    writer.writeAttribute(HTML.BORDER_ATTR, "0", null);
    writer.writeAttribute(HTML.CELLSPACING_ATTR, "0", null);
    writer.writeAttribute(HTML.CELLPADDING_ATTR, "0", null);
    writer.startElement(HTML.TBODY_ELEM, tabPanel);
    writer.startElement(HTML.TR_ELEM, tabPanel);
  }

  @Override
  public void encodeChildren(FacesContext facesContext, UIComponent component)
  throws IOException {

    TabPanel tabPanel = (TabPanel) component;
    if (tabPanel.getHeaderClass() != null)
    {
      // indicates old-style tabs.xhtml
      encodeChildrenCompat(facesContext, component);
      return;
    }

    if (component.getChildCount() > 0)
    {
      ResponseWriter writer = facesContext.getResponseWriter();
      determineActiveTab(facesContext, tabPanel);
      int allTabs = getTabCount(facesContext, tabPanel);
      int tabIdx = 0;
      for (UIComponent child : component.getChildren())
      {
        if (!child.isRendered())
          continue;

        Tab tab = (Tab) child;
        if (tab.checkRoleAccess(facesContext))
        {
          // leading spacer
          if (tabIdx != 0)
          {
            writer.startElement(HTML.TD_ELEM, tabPanel);
            renderSpacer(facesContext, writer, tabPanel);
            writer.endElement(HTML.TD_ELEM);
          }

          // each tab
          writer.startElement(HTML.TD_ELEM, tab);
          writer.writeAttribute(HTML.ID_ATTR, tab.getClientId() + "_tab", null);

          String tabStyleClass = null;
          if (tab.isActive())
            tabStyleClass = appendStyleClass(tabPanel.getActiveTabClass(), tabStyleClass);
          else
            tabStyleClass = appendStyleClass(tabPanel.getInActiveTabClass(), tabStyleClass);
          if (tabIdx == 0)
            tabStyleClass = appendStyleClass(tabPanel.getFirstTabClass(), tabStyleClass);
          else if (tabIdx == allTabs - 1)
            tabStyleClass = appendStyleClass(tabPanel.getLastTabClass(), tabStyleClass);
          writer.writeAttribute(HTML.CLASS_ATTR, tabStyleClass, "inActiveTabClass");

          writer.writeAttribute(HTML.ONCLICK_ATTR, "document.getElementById('" + tab.getClientId()  + "').click();", "onclick");

          child.encodeAll(facesContext);

          writer.endElement(HTML.TD_ELEM);

          tabIdx++;
        }
      }
    }
  }

  private String appendStyleClass(String styleClass, String previousClass)
  {
    if (styleClass == null)
      return previousClass;
    else if (previousClass == null)
      return styleClass;
    else
      return previousClass + " " + styleClass;
  }

  protected void renderSpacer(FacesContext facesContext, ResponseWriter writer, TabPanel tabPanel) throws IOException
  {
    writer.startElement(HTML.IMG_ELEM, tabPanel);
    String spacerImg = tabPanel.getSpacerImage() == null ? "/images/blank.gif" : tabPanel.getSpacerImage();
    writer.writeAttribute(HTML.SRC_ATTR, facesContext.getExternalContext().getRequestContextPath() + spacerImg, null);
    writer.writeAttribute(HTML.ALT_ATTR, "spacer", null);
    if (tabPanel.getSpacerClass() != null)
      writer.writeAttribute(HTML.CLASS_ATTR, tabPanel.getSpacerClass(), "spacerClass");
    writer.endElement(HTML.IMG_ELEM);
  }

  public void encodeChildrenCompat(FacesContext facesContext, UIComponent component) throws IOException
  {
    TabPanel tabPanel = (TabPanel) component;

    if (component.getChildCount() > 0)
    {
      ResponseWriter writer = facesContext.getResponseWriter();
      determineActiveTab(facesContext, tabPanel);
      boolean isFirstRendered = true;
      for (UIComponent child : component.getChildren())
      {
        if (!child.isRendered())
          continue;

        Tab tab = (Tab) child;
        if (tab.checkRoleAccess(facesContext))
        {
          // leading spacer
          if (!isFirstRendered)
          {
            writer.startElement(HTML.TD_ELEM, tabPanel);
            renderBlank(facesContext, writer, tabPanel);
            writer.endElement(HTML.TD_ELEM);
          }

          // each tab
          writer.startElement(HTML.TD_ELEM, tab);
          writer.writeAttribute(HTML.ID_ATTR, tab.getClientId() + "_cell", null);
          if (tab.isActive())
            writer.writeAttribute(HTML.CLASS_ATTR, tabPanel.getActiveHeaderCellClass(), "activeHeaderCellClass");
          else
            writer.writeAttribute(HTML.CLASS_ATTR, tabPanel.getInActiveHeaderCellClass(), "inActiveHeaderCellClass");
          writer.startElement(HTML.TABLE_ELEM, tab);
          writer.writeAttribute(HTML.ID_ATTR, tab.getClientId() + "_shifted", null);
          if (tab.isActive() && tabPanel.getActiveTabHeaderClass() != null)
            writer.writeAttribute(HTML.CLASS_ATTR, tabPanel.getActiveTabHeaderClass(), "activeTabHeaderClass");
          writer.writeAttribute(HTML.BORDER_ATTR, "0", null);
          writer.writeAttribute(HTML.CELLSPACING_ATTR, "0", null);
          writer.writeAttribute(HTML.CELLPADDING_ATTR, "0", null);
          writer.startElement(HTML.TBODY_ELEM, tab);
          writer.startElement(HTML.TR_ELEM, tab);
          writer.startElement(HTML.TD_ELEM, tab);
          if (tabPanel.getHeaderSideBorderClass() != null)
            writer.writeAttribute(HTML.CLASS_ATTR, tabPanel.getHeaderSideBorderClass(), "headerSideBorderClass");
          renderBlank(facesContext, writer, tabPanel);
          writer.endElement(HTML.TD_ELEM);
          writer.startElement(HTML.TD_ELEM, tab);
          if (tabPanel.getHeaderSideCellClass() != null)
            writer.writeAttribute(HTML.CLASS_ATTR, tabPanel.getHeaderSideCellClass(), "headerSideCellClass");

          child.encodeAll(facesContext);

          writer.endElement(HTML.TD_ELEM);
          writer.startElement(HTML.TD_ELEM, tab);
          if (tabPanel.getHeaderSideBorderClass() != null)
            writer.writeAttribute(HTML.CLASS_ATTR, tabPanel.getHeaderSideBorderClass(), "headerSideBorderClass");
          renderBlank(facesContext, writer, tabPanel);
          writer.endElement(HTML.TD_ELEM);
          writer.endElement(HTML.TR_ELEM);
          writer.endElement(HTML.TBODY_ELEM);
          writer.endElement(HTML.TABLE_ELEM);
          writer.endElement(HTML.TD_ELEM);

          isFirstRendered = false;
        }
      }
    }
  }

  protected void renderBlank(FacesContext facesContext, ResponseWriter writer, TabPanel tabPanel) throws IOException
  {
    writer.startElement(HTML.IMG_ELEM, tabPanel);
    writer.writeAttribute(HTML.SRC_ATTR, facesContext.getExternalContext().getRequestContextPath() + "/images/blank.gif", null);
    writer.writeAttribute(HTML.ALT_ATTR, "", null);
    if (tabPanel.getSpacerClass() != null)
      writer.writeAttribute(HTML.CLASS_ATTR, tabPanel.getSpacerClass(), "spacerClass");
    writer.endElement(HTML.IMG_ELEM);
  }

  @Override
  public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException
  {
    ResponseWriter writer = facesContext.getResponseWriter();
    writer.endElement(HTML.TR_ELEM);
    writer.endElement(HTML.TBODY_ELEM);
    writer.endElement(HTML.TABLE_ELEM);
  }

  private void determineActiveTab(FacesContext facesContext, TabPanel tabPanel)
  {
    ExternalContext externalContext = facesContext.getExternalContext();
    Map<String, Object> sessionMap = externalContext.getSessionMap();
    String sessionAttr = tabPanel.getId() + "_selectedTab";

    String prevTabId = (String) sessionMap.get(sessionAttr);

    // check whether tab id is explicitly specified on the request
    Map<String,String> requestParamMap = externalContext.getRequestParameterMap();
    String reqTab = requestParamMap.get(tabPanel.getId()); // compatibility -- deprecated param
    if (reqTab == null)
      reqTab = requestParamMap.get(sessionAttr);
    if (reqTab != null)
    {
      sessionMap.put(sessionAttr, reqTab);
    }
    else
    {
      // check whether the request corresponds to a tab action
      String viewId = facesContext.getViewRoot().getViewId();
      //String requestPath = externalContext.getRequestServletPath();
      for (UIComponent child : tabPanel.getChildren())
      {
        if (child instanceof Tab)
        {
          Tab tab = (Tab) child;
          String action = tab.getActionExpression().getExpressionString();
          if (action != null)
          {
            if (FacesVariableUtil.checkFromOutcomeLeadsToViewId(action, viewId))
            {
              sessionMap.put(sessionAttr, tab.getId());
            }
          }
        }
      }
    }

    String activeTabId = (String) sessionMap.get(sessionAttr);
    if (activeTabId != null)
      tabPanel.setActiveTab(activeTabId);
    else if (tabPanel.getDefaultTab() != null)
      tabPanel.setActiveTab(tabPanel.getDefaultTab());

    handleTabChange(tabPanel, prevTabId, tabPanel.getActiveTab());
  }

  protected void handleTabChange(TabPanel tabPanel, String oldActiveTabId, String newActiveTabId)
  {
    boolean changed = false;
    if (newActiveTabId == null)
      changed = oldActiveTabId != null;
    else
      changed = !newActiveTabId.equals(oldActiveTabId);

    if (changed)
    {
      String listenerSpec = tabPanel.getTabChangeListener();
      if (listenerSpec != null)
      {
        ValueChangeListener listener = null;
        try
        {
          if (FacesVariableUtil.isValueBindingExpression(listenerSpec))
            listener = (ValueChangeListener) FacesVariableUtil.getValue(listenerSpec);
          else
            listener = Class.forName(listenerSpec).asSubclass(ValueChangeListener.class).newInstance();

          if (listener != null)
            listener.processValueChange(new ValueChangeEvent(tabPanel, oldActiveTabId, newActiveTabId));
        }
        catch (Exception ex)
        {
          throw new AbortProcessingException(ex.getMessage(), ex);
        }
      }
    }
  }

  public int getTabCount(FacesContext facesContext, TabPanel tabPanel)
  {
    int tabCount = 0;
    for (UIComponent child : tabPanel.getChildren())
    {
      if (child instanceof Tab && child.isRendered() && ((Tab)child).checkRoleAccess(facesContext))
        tabCount++;
    }
    return tabCount;
  }
}
