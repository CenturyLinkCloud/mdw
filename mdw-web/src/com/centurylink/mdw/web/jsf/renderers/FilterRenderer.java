/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.renderers;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.PhaseId;
import javax.faces.render.Renderer;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.property.PropertyUtil;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.jsf.components.Filter;
import com.centurylink.mdw.web.jsf.components.FilterActionEvent;
import com.centurylink.mdw.web.jsf.components.FilterActionListener;

/**
 * Custom renderer for a SortableList.
 */
public class FilterRenderer extends Renderer
{
  public static final String RENDERER_TYPE = "com.centurylink.mdw.web.jsf.renderers.FilterRenderer";

  public boolean getRendersChildren() { return true; }

  public void encodeBegin(FacesContext context, UIComponent component)
    throws IOException
  {
    ResponseWriter writer = context.getResponseWriter();
    Filter filter = (Filter) component;

    String filterId = (String) component.getAttributes().get("filterId");
    FacesVariableUtil.setValue("filterId", filterId);

    writer.write('\n');
    writer.writeComment("FILTER START");

    if (((Boolean)component.getAttributes().get("showHeader")).booleanValue())
    {
      writer.write('\n');
      writer.startElement("table", component);
      writer.writeAttribute("width", "100%", "width");
      writer.writeAttribute("border", "0", "border");
      writer.writeAttribute("cellspacing", "0", "cellspacing");
      writer.writeAttribute("cellpadding", "0", "cellpadding");
      writer.write('\n');
      writer.startElement("tr", component);
      writer.write('\n');
      writer.startElement("td", component);
      writer.writeAttribute("class", component.getAttributes().get("headerClass"), "class");
      writer.write(component.getAttributes().get("name").toString());
      writer.endElement("td");
      writer.startElement("td", component);
      writer.writeAttribute("width", "100%", "width");
      writer.writeAttribute("class", component.getAttributes().get("headerClass"), "class");
      writer.startElement("div", component);
      writer.writeAttribute("style", "position:relative;", "style");
      if (filter.isShowPrefsButton())
        renderUserPrefsButton(context, (Filter)component);
      writer.endElement("div");
      writer.endElement("td");
      writer.write('\n');
      writer.endElement("tr");
      writer.write('\n');
      writer.endElement("table");
    }
    writer.write('\n');
    writer.startElement("table", component);
    writer.writeAttribute("width", filter.getWidth(), "width");
    writer.writeAttribute("class", filter.getAttributes().get("styleClass"), "class");
  }

  public void encodeEnd(FacesContext context, UIComponent component)
    throws IOException
  {
    ResponseWriter writer = context.getResponseWriter();
    Filter filter = (Filter) component;

    writer.endElement("table");
    writer.write('\n');

    if (((Boolean)component.getAttributes().get("showCommandButtons")).booleanValue())
    {
      // programatically add rendering for command buttons
      writer.startElement("table", component);
      writer.writeAttribute("class", component.getAttributes().get("footerClass"), "class");
      writer.write('\n');
      writer.startElement("tr", component);
      writer.write('\n');
      writer.startElement("td", component);
      writer.writeAttribute("class", component.getAttributes().get("inputRowClass"), "class");
      writer.write('\n');
      writer.startElement("div", component);
      writer.writeAttribute("align", "center", "align");

      // change direction of buttons when swapButtonPosition is true
      if ( ((Boolean)component.getAttributes().get("swapButtonPosition")).booleanValue() )
        writer.writeAttribute("dir", "rtl", "dir");

      renderCommandButtons(context, component);
      String actionListener = filter.getActionListener();
      if (actionListener != null && !actionListener.isEmpty())
      {
        try
        {
          Class<? extends FilterActionListener> clz = Class.forName(actionListener).asSubclass(FilterActionListener.class);
          filter.addFilterActionListener(clz.newInstance());
        }
        catch (Exception ex)
        {
          throw new FacesException(ex.getMessage(), ex);
        }
      }

      writer.write('\n');
      writer.endElement("div");
      writer.write('\n');
      writer.endElement("td");
      writer.write('\n');
      writer.endElement("tr");
      writer.write('\n');
      writer.endElement("table");
      writer.write('\n');
    }

    writer.writeComment("FILTER END");
  }

  public void encodeChildren(FacesContext context, UIComponent component) throws IOException
  {
    UIData uiData = (UIData) component;

    int first = uiData.getFirst();
    int rows = uiData.getRows();
    int rowCount = uiData.getRowCount();
    if (rows <= 0)
    {
      rows = rowCount - first;
    }
    int last = first + rows;
    if (last > rowCount)
      last = rowCount;

    for (int i = first; i < last; i++)
    {
      uiData.setRowIndex(i);
      if (uiData.isRowAvailable())
      {
        List<UIComponent> children = component.getChildren();
        for (int j = 0, size = component.getChildCount(); j < size; j++)
        {
          UIComponent child = children.get(j);
          if (child.isRendered())
          {
            renderChild(context, child);
          }
        }
      }
    }

  }

  public void renderChildren(FacesContext context, UIComponent component) throws IOException
  {
    if (component.getChildCount() > 0)
    {
      for (Iterator<UIComponent> it = component.getChildren().iterator(); it.hasNext();)
      {
        UIComponent child = it.next();
        renderChild(context, child);
      }
    }
  }

  public void renderChild(FacesContext context, UIComponent child) throws IOException
  {
    if (!child.isRendered() || child.getId().indexOf("filterPrefsButton") >= 0)
    {
      return;
    }

    child.encodeBegin(context);
    if (child.getRendersChildren())
    {
      child.encodeChildren(context);
    }
    else
    {
      renderChildren(context, child);
    }
    child.encodeEnd(context);
  }

  public void decode(FacesContext context, UIComponent component)
  {
    super.decode(context, component);

    Filter filter = (Filter) component;

    Map<String,String> parameters = context.getExternalContext().getRequestParameterMap();
    String param = (String) parameters.get(filter.getClientId(context) + ":_filter_submit");
    if (param != null && param.equals(filter.getSubmitButtonLabel()))
    {
      FilterActionEvent event = new FilterActionEvent(filter, FilterActionEvent.ACTION_SUBMIT);
      event.setPhaseId(PhaseId.UPDATE_MODEL_VALUES);
      component.queueEvent(event);
    }
    param = (String) parameters.get(filter.getClientId(context) + ":_filter_reset");
    if (param != null && param.equals(filter.getResetButtonLabel()))
    {
      FilterActionEvent event = new FilterActionEvent(filter, FilterActionEvent.ACTION_RESET);
      event.setPhaseId(PhaseId.APPLY_REQUEST_VALUES);
      component.queueEvent(event);
    }
  }

  protected void renderCommandButtons(FacesContext context, UIComponent component)
    throws IOException
  {
    Filter filter = (Filter) component;

    renderSubmitButton(context, filter);
    renderResetButton(context, filter);
  }

  protected void renderSubmitButton(FacesContext context, Filter filter)
    throws IOException
  {
    Application application = context.getApplication();

    HtmlCommandButton submitButton = (HtmlCommandButton) application.createComponent(HtmlCommandButton.COMPONENT_TYPE);
    submitButton.setStyleClass((String)filter.getAttributes().get("commandButtonClass"));
    submitButton.setValue((String)filter.getAttributes().get("submitButtonLabel"));
    submitButton.setId("_filter_submit");
    submitButton.setTransient(true);
    filter.getChildren().add(submitButton);
    renderChild(context, submitButton);
  }

  protected void renderResetButton(FacesContext context, Filter filter)
    throws IOException
  {
    Application application = context.getApplication();

    HtmlCommandButton resetButton = (HtmlCommandButton) application.createComponent(HtmlCommandButton.COMPONENT_TYPE);
    resetButton.setStyleClass((String)filter.getAttributes().get("commandButtonClass"));
    resetButton.setValue((String)filter.getAttributes().get("resetButtonLabel"));
    resetButton.setId("_filter_reset");
    resetButton.setTransient(true);
    resetButton.setImmediate(true);
    filter.getChildren().add(resetButton);
    renderChild(context, resetButton);
  }

  protected void renderUserPrefsButton(FacesContext facesContext, Filter filter) throws IOException
  {
    ResponseWriter writer = facesContext.getResponseWriter();
    writer.startElement("input", filter);
    writer.writeAttribute("id", "filterPrefsButton", null);
    writer.writeAttribute("type", "button", null);
    writer.writeAttribute("onclick", "return false;", null);
    writer.writeAttribute("class", "mdw_listButton", null);
    String bg = "background-color:transparent;background-repeat:no-repeat;";
    writer.writeAttribute("style", "background-image:url('" + ApplicationContext.getTaskManagerUrl() + "/images/prefs.gif');" + bg, null);
    writer.writeAttribute("value", "", null);
    writer.writeAttribute("title", "Filter Preferences", null);
    writer.endElement("input");
  }

  protected String getProperty(String propName) throws IOException
  {
    PropertyManager propMgr = PropertyUtil.getInstance().getPropertyManager();
    return propMgr.getStringProperty(propName);
  }

}
