/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.renderers;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.component.html.HtmlInputText;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.custom.datascroller.HtmlDataScroller;
import org.apache.myfaces.custom.datascroller.HtmlDataScrollerRenderer;
import org.apache.myfaces.shared_tomahawk.renderkit.RendererUtils;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.property.PropertyUtil;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.jsf.components.DataScroller;
import com.centurylink.mdw.web.ui.list.ListSearch;

/**
 * Custom version of the tomahawk datascroller renderer to give us the ability
 * to make it have the desired look-and-feel.
 */
public class DataScrollerRenderer extends HtmlDataScrollerRenderer
{
  public static final String RENDERER_TYPE = "com.centurylink.mdw.web.jsf.renderers.DataScrollerRenderer";

  /**
   * Override base class method to perform specialized rendering.
   */
  protected void renderScroller(FacesContext facesContext, HtmlDataScroller scroller) throws IOException
  {
    DataScroller dataScroller = (DataScroller) scroller;
    FacesVariableUtil.setValue("mdwListId", scroller.getAttributes().get("listId"));

    ResponseWriter writer = facesContext.getResponseWriter();

    // force refresh of the associated dataTable
    scroller.getUIData().setValue(null);

    // TODO replace layout table with nested divs
    writer.startElement("table", scroller);
    writer.writeAttribute("style", "width:100%;border-collapse:collapse;border-spacing:0", "style");

    String styleClass = scroller.getStyleClass();
    if (styleClass != null)
    {
      writer.writeAttribute("class", styleClass, null);
    }
    String style = scroller.getStyle();
    if (style != null)
    {
      writer.writeAttribute("style", style, null);
    }
    writer.startElement("tr", scroller);

    // summary info
    writer.startElement("td", scroller);
    writer.writeAttribute("class", scroller.getAttributes().get("summaryClass"), "class");

    writer.startElement("table", scroller);
    writer.writeAttribute("style", "border-collapse:collapse;border-spacing:0", "style");
    writer.startElement("tr", scroller);
    writer.startElement("td", scroller);

    // label
    writer.write(scroller.getAttributes().get("summaryLabel").toString());
    if (scroller.getAttributes().get("subLabel") != null)
    {
      writer.startElement("span", scroller);
      writer.writeAttribute("class", scroller.getAttributes().get("subLabelClass"), "class");
      writer.write(scroller.getAttributes().get("subLabel").toString());
      writer.endElement("span");
    }
    writer.startElement("span", scroller);
    writer.writeAttribute("class", scroller.getAttributes().get("totalsClass"), "class");
    int totalRows = scroller.getRowCount();
    if (scroller.getFirstRow() > totalRows)
    {
      scroller.getUIData().setFirst(0);
    }
    int firstRow = totalRows == 0 ? 0 : scroller.getFirstRow() + 1;
    int lastRow = 0;
    if (scroller.getUIData().getRows() == 0)
    {
      // all mode
      lastRow = totalRows;
      int showAllDisplayRows = 0;
      if (scroller.getAttributes().get("showAllDisplayRows") != null)
        showAllDisplayRows = Integer.parseInt(scroller.getAttributes().get("showAllDisplayRows")+"");

      if (showAllDisplayRows != 0 && totalRows > showAllDisplayRows)
        writer.write("<span class=\"mdw_advisoryItem\">(" + firstRow + "-" + showAllDisplayRows + " of " + totalRows + ")</span>");
      else
        writer.write("(" + firstRow + "-" + lastRow + " of " + totalRows + ")");
    }
    else
    {
      int potentialLastRow = firstRow + scroller.getUIData().getRows() - 1;
      lastRow =  potentialLastRow > totalRows ? totalRows : potentialLastRow;
      writer.write("(" + firstRow + "-" + lastRow + " of " + totalRows + ")");
    }
    writer.endElement("span");

    writer.endElement("td");
    writer.startElement("td", scroller);
    writer.writeAttribute("style", "padding:0", "style");

    // list buttons
    writer.startElement("div", scroller);
    DataScroller ds = (DataScroller) scroller;
    renderListButtons(facesContext, ds);

    writer.endElement("div");
    writer.endElement("td");

    String searchValue = getSearchValue(ds);
    if (isSearchable(ds))
    {
      writer.startElement("td", scroller);
      String tdStyle = "padding:0;white-space:nowrap;" + (searchValue == null ? "visibility:hidden;" : "");
      writer.writeAttribute("style", tdStyle, "style");
      writer.startElement("span", scroller);
      writer.writeAttribute("class", "mdw_listSearchLabel", "class");
      writer.write("Search: ");
      writer.endElement("span");
      renderSearchInput(facesContext, ds, searchValue);
      writer.endElement("td");
      writer.startElement("td", scroller);
      writer.writeAttribute("style", tdStyle, "style");
      renderSearchApplyButton(facesContext, ds);
      renderSearchClearButton(facesContext, ds);
      writer.endElement("td");
    }

    // retrieval timings
    Object timingsAttr = dataScroller.getAttributes().get("timingOutput");
    if (timingsAttr != null)
    {
      writer.startElement("td", scroller);
      Object timingsClass = scroller.getAttributes().get("subLabelClass");
      writer.writeAttribute("class", timingsClass == null ? "mdw_advisory" : timingsClass + " mdw_advisory", "class");
      writer.write(timingsAttr.toString());
      writer.endElement("td");
    }

    writer.endElement("tr");
    writer.endElement("table");

    writer.endElement("td");

    UIComponent facetComp = dataScroller.getAllFacet();
    Object columnClass = facetComp.getParent().getAttributes().get("pageControlsClass");
    String columnScript = "this.getElementsByTagName('A')[0].click();";
    if (facetComp != null && facetComp.isRendered())
    {
      writer.startElement("td", scroller);
      if (columnClass != null)
        writer.writeAttribute("class", columnClass, "class");
      if (columnScript != null)
        writer.writeAttribute("onclick", columnScript, "onclick");
      renderFacet(facesContext, scroller, facetComp, "all");
      writer.endElement("td");
    }
    facetComp = scroller.getFirst();
    if (facetComp != null && facetComp.isRendered())
    {
      writer.startElement("td", scroller);
      if (columnClass != null)
        writer.writeAttribute("class", columnClass, "class");
      if (columnScript != null)
        writer.writeAttribute("onclick", columnScript, "onclick");
      renderFacet(facesContext, scroller, facetComp, HtmlDataScroller.FACET_FIRST);
      writer.endElement("td");
    }
    facetComp = scroller.getPrevious();
    if (facetComp != null && facetComp.isRendered())
    {
      writer.startElement("td", scroller);
      if (columnClass != null)
        writer.writeAttribute("class", columnClass, "class");
      if (columnScript != null)
        writer.writeAttribute("onclick", columnScript, "onclick");
      renderFacet(facesContext, scroller, facetComp, HtmlDataScroller.FACET_PREVIOUS);
      writer.endElement("td");
    }
    facetComp = scroller.getNext();
    if (facetComp != null && facetComp.isRendered())
    {
      writer.startElement("td", scroller);
      if (columnClass != null)
        writer.writeAttribute("class", columnClass, "class");
      if (columnScript != null)
        writer.writeAttribute("onclick", columnScript, "onclick");
      renderFacet(facesContext, scroller, facetComp, HtmlDataScroller.FACET_NEXT);
      writer.endElement("td");
    }
    facetComp = scroller.getLast();
    if (facetComp != null && facetComp.isRendered())
    {
      writer.startElement("td", scroller);
      if (columnClass != null && searchValue == null)
        writer.writeAttribute("class", columnClass, "class");
      writer.writeAttribute("style", "border-right-width:0px;" , "style");
      if (columnScript != null)
        writer.writeAttribute("onclick", columnScript, "onclick");
      writer.writeAttribute("onclick", columnScript, "onclick");
      renderFacet(facesContext, scroller, facetComp, HtmlDataScroller.FACET_LAST);
      writer.endElement("td");
    }

    writer.endElement("tr");
    writer.endElement("table");
  }

  protected void renderListButtons(FacesContext facesContext, DataScroller scroller) throws IOException
  {
    if (isSearchable(scroller))
      renderSearchButton(facesContext, scroller);
    if (isRefreshable(scroller))
      renderRefreshButton(facesContext, scroller);
    if (isExportable(scroller))
      renderExportButton(facesContext, scroller);
    if (hasPreferences(scroller))
      renderPrefsButton(facesContext, scroller);

    renderCustomListButtons(facesContext, scroller);
  }

  protected void renderCustomListButtons(FacesContext facesContext, DataScroller scroller) throws IOException
  {
    String customButtons = scroller.getCustomButtons();
    if (customButtons != null && customButtons.length() > 0)
    {
      for (String btnDef : customButtons.split(";"))
      {
        String[] defs = btnDef.split(",");
        if (defs.length != 3 && defs.length != 4)
          throw new FacesException("Invalid customButton definition: " + defs);
        UICommand btn = renderListButton(facesContext, scroller, defs[0], defs[1], defs[2], true, true);
        if (defs.length == 4)
          btn.setActionExpression(FacesVariableUtil.createMethodExpression(defs[3], null, null));
      }
    }
  }

  protected boolean hasPreferences(DataScroller scroller)
  {
    return true;
  }

  protected UICommand renderPrefsButton(FacesContext facesContext, DataScroller scroller) throws IOException
  {
    return renderListButton(facesContext, scroller, "listPrefsButton", "List Preferences", "prefs.gif", false, false);
  }

  protected boolean isRefreshable(DataScroller scroller)
  {
    return true;
  }

  protected UICommand renderRefreshButton(FacesContext facesContext, DataScroller scroller) throws IOException
  {
    return renderListButton(facesContext, scroller, "listRefreshButton", "Refresh", "refresh.gif", true, false);
  }

  protected boolean isSearchable(DataScroller scroller)
  {
    Object searchAttr = scroller.getAttributes().get("searchable");
    return searchAttr == null ? false : Boolean.parseBoolean(String.valueOf(searchAttr));
  }

  protected String getSearchValue(DataScroller scroller)
  {
    if (!isSearchable(scroller))
      return null;
    ListSearch listSearch = (ListSearch) FacesVariableUtil.getValue(ListSearch.LIST_SEARCH);
    if (listSearch == null)
      return null;
    return listSearch.getSearch();
  }

  protected UICommand renderSearchButton(FacesContext facesContext, DataScroller scroller) throws IOException
  {
    return renderListButton(facesContext, scroller, "listSearchButton", "Search", "search.gif", false, false, "showSearchInput(this);return false;", null);
  }

  protected UIInput renderSearchInput(FacesContext facesContext, DataScroller scroller, String value) throws IOException
  {
    String id = ListSearch.LIST_SEARCH_INPUT;
    UIComponent existing = FacesVariableUtil.findComponentById(facesContext, facesContext.getViewRoot(), id);
    if (existing instanceof UIInput)
      return (UIInput) existing;  // avoid duplicate component id

    HtmlInputText searchInput = (HtmlInputText)facesContext.getApplication().createComponent(HtmlInputText.COMPONENT_TYPE);

    searchInput.setId(id);
    searchInput.setValue(value);
    searchInput.setStyleClass("mdw_listSearch");
    searchInput.setImmediate(true);
    searchInput.setTransient(true);
    searchInput.setOnkeydown("submitOnEnter(event, this.form);");
    scroller.getChildren().add(searchInput);

    // render the button
    searchInput.encodeBegin(facesContext);
    searchInput.encodeEnd(facesContext);

    return searchInput;
  }

  protected UICommand renderSearchApplyButton(FacesContext facesContext, DataScroller scroller) throws IOException
  {
    return renderListButton(facesContext, scroller, ListSearch.LIST_SEARCH_APPLY, "Apply", "apply_btn.gif", true, false, null, "mdw_listSearchBtn");
  }

  protected UICommand renderSearchClearButton(FacesContext facesContext, DataScroller scroller) throws IOException
  {
    return renderListButton(facesContext, scroller, ListSearch.LIST_SEARCH_CLEAR, "Clear", "clear.gif", true, false, null, "mdw_listSearchBtn");
  }

  protected boolean isExportable(DataScroller scroller)
  {
    return scroller.getExportListId() != null && scroller.getExportListId().trim().length() > 0;
  }

  protected UICommand renderExportButton(FacesContext facesContext, DataScroller scroller) throws IOException
  {
    String listId = scroller.getExportListId();
    facesContext.getExternalContext().getSessionMap().put("excelExportListId", listId);
    return renderListButton(facesContext, scroller, "excelExport", "Export to Excel", "excel.gif", true, false);
  }


  /**
   * Replaces base class getLink() method for ajax support and to apply style class and set immediate.
   */
  protected UICommand getCommandLink(FacesContext facesContext, HtmlDataScroller scroller, String facetName)
  {
    UICommand link = isAjaxEnabled(scroller) ? getAjaxLink(facesContext, scroller, facetName) : super.getLink(facesContext, scroller, facetName);
    link.setImmediate(true);
    return link;
  }

  protected boolean isAjaxEnabled(HtmlDataScroller scroller)
  {
    boolean ajaxEnabled = false;
    Object ajaxAttr = scroller.getAttributes().get("ajaxEnabled");
    if (ajaxAttr != null)
      ajaxEnabled = Boolean.parseBoolean(String.valueOf(ajaxAttr));
    return ajaxEnabled;
  }

  protected UICommand getAjaxLink(FacesContext facesContext, HtmlDataScroller scroller, String facetName)
  {
    Application application = facesContext.getApplication();
    Object listLinkAttr = scroller.getAttributes().get("ajaxListLink");
    String listLinkClass = listLinkAttr == null ? "org.ajax4jsf.CommandLink" : listLinkAttr.toString();
    UICommand link = (UICommand) application.createComponent(listLinkClass);
    link.setId(scroller.getId() + facetName);
    link.setTransient(true);
    link.setImmediate(true);
    UIParameter parameter = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
    parameter.setId(scroller.getId() + facetName + "_param");
    parameter.setTransient(true);
    parameter.setName(scroller.getClientId(facesContext));
    parameter.setValue(facetName);
    List<UIComponent> children = link.getChildren();
    children.add(parameter);
    scroller.getChildren().add(link);
    return link;
  }

  protected UICommand renderListButton(FacesContext facesContext, DataScroller scroller, String id, String label, String image, boolean postBack, boolean custom) throws IOException
  {
    return renderListButton(facesContext, scroller, id, label, image, postBack, custom, null, null);
  }

  protected UICommand renderListButton(FacesContext facesContext, DataScroller scroller, String id, String label, String image, boolean postBack, boolean custom, String onclick, String styleClass) throws IOException
  {
    Application application = facesContext.getApplication();

    UIComponent existing = FacesVariableUtil.findComponentById(facesContext, facesContext.getViewRoot(), id);
    if (existing instanceof HtmlCommandButton)
      return (UICommand) existing;  // avoid duplicate component id

    boolean ajaxEnabled = isAjaxEnabled(scroller);
    String bg = "background-color:transparent;background-repeat:no-repeat;background-position:center;";

    UICommand listButton;
    if (ajaxEnabled && postBack)
    {
      Object listBtnAttr = scroller.getAttributes().get("ajaxListButton");
      String listBtnClass = listBtnAttr == null ? "org.ajax4jsf.CommandButton" : listBtnAttr.toString();
      listButton = (UICommand) application.createComponent(listBtnClass);
      // user reflection to avoid importing a4j classes
      Class<?> btnClass = listButton.getClass();
      Class<?>[] strParam = new Class<?>[] { String.class };
      try
      {
        btnClass.getMethod("setStyle", strParam).invoke(listButton, new Object[] { "background-image:url('" + getImageBaseUrl() + "/" + image + "');" + bg });
        btnClass.getMethod("setStyleClass", strParam).invoke(listButton, new Object[] { "mdw_listButton" + (styleClass == null ? "" : " " + styleClass) });
        btnClass.getMethod("setTitle", strParam).invoke(listButton, new Object[] { label });
        if (onclick == null)
        {
          if (!custom)
            btnClass.getMethod("setOnclick", strParam).invoke(listButton, new Object[] { "listItemLoading(this.parentNode);" });
        }
        else
        {
          btnClass.getMethod("setOnclick", strParam).invoke(listButton, new Object[] { onclick });
        }
      }
      catch (Exception ex)
      {
        throw new IOException(ex.getMessage(), ex);
      }
    }
    else
    {
      listButton = (UICommand) application.createComponent(HtmlCommandButton.COMPONENT_TYPE);
      ((HtmlCommandButton)listButton).setStyle("background-image:url('" + getImageBaseUrl() + "/" + image + "');" + bg);
      ((HtmlCommandButton)listButton).setStyleClass("mdw_listButton" + (styleClass == null ? "" : " " + styleClass));
      ((HtmlCommandButton)listButton).setTitle(label);
      if (onclick != null)
        ((HtmlCommandButton)listButton).setOnclick(onclick);
      else if (!postBack)
        ((HtmlCommandButton)listButton).setOnclick("return false;");
    }
    listButton.setValue("");
    listButton.setId(id);
    listButton.setImmediate(true);
    if (!custom || ajaxEnabled)
      listButton.setTransient(true);
    scroller.getChildren().add(listButton);

    // render the button
    listButton.encodeBegin(facesContext);
    listButton.encodeEnd(facesContext);

    return listButton;
  }


  protected HtmlCommandLink renderListLink(FacesContext facesContext, DataScroller scroller, String id, String label, boolean postBack) throws IOException
  {
    Application application = facesContext.getApplication();

    UIComponent existing = FacesVariableUtil.findComponentById(facesContext, facesContext.getViewRoot(), id);
    if (existing instanceof HtmlCommandLink)
      return (HtmlCommandLink) existing;

    HtmlCommandLink listLink = (HtmlCommandLink) application.createComponent(HtmlCommandLink.COMPONENT_TYPE);
    listLink.setStyleClass((String)scroller.getAttributes().get("tdClass"));
    listLink.setStyle("border-right:none;");
    listLink.setValue(label);
    listLink.setId(id);
    listLink.setImmediate(true);
    listLink.setTransient(true);
    scroller.getChildren().add(listLink);

    // render the link
    listLink.encodeBegin(facesContext);
    listLink.encodeEnd(facesContext);
    return listLink;
  }

  protected String getImageBaseUrl() throws IOException
  {
    return ApplicationContext.getTaskManagerUrl() + "/images";
  }

  protected String getProperty(String propName) throws IOException
  {
    PropertyManager propMgr = PropertyUtil.getInstance().getPropertyManager();
    return propMgr.getStringProperty(propName);
  }

  /**
   * Overridden for ajax support.
   */
  @Override
  protected void renderFacet(FacesContext facesContext, HtmlDataScroller scroller, UIComponent facetComp, String facetName) throws IOException
  {
    UIComponent existing = FacesVariableUtil.findComponentById(facesContext, facesContext.getViewRoot(), scroller.getId() + facetName + "_param");
    if (existing == null)
    {
      try
      {
        UICommand link = getCommandLink(facesContext, scroller, facetName);

        // ajax checking avoids dependency on A4J classes
        String onclick = scroller.getOnclick();
        if (link.getClass().getName().endsWith("AjaxCommandLink") || link.getClass().getPackage().getName().startsWith("org.richfaces"))
          onclick = onclick == null ? "listItemLoading(this);" : "listItemLoading(this);" + onclick;
        onclick = onclick == null ? "event.stopPropagation();" : "event.stopPropagation();" + onclick;
        if (onclick != null)
        {
          if (link instanceof HtmlCommandLink)
            ((HtmlCommandLink)link).setOnclick(onclick);
          else if (link.getClass().getName().endsWith("AjaxCommandLink") || link.getClass().getPackage().getName().startsWith("org.richfaces"))
            link.getClass().getMethod("setOnclick", new Class<?>[] { String.class }).invoke(link, new Object[] { onclick });
        }

        String ondblclick = scroller.getOndblclick();
        if (ondblclick != null)
        {
          if (link instanceof HtmlCommandLink)
            ((HtmlCommandLink)link).setOndblclick(ondblclick);
          else if (link.getClass().getName().endsWith("AjaxCommandLink") || link.getClass().getPackage().getName().startsWith("org.richfaces"))
            link.getClass().getMethod("setOndblclick", new Class<?>[] { String.class }).invoke(link, new Object[] { ondblclick });
        }

        String linksClass = (String)scroller.getAttributes().get("paginationLinksClass");
        if (linksClass != null)
        {
          if (link instanceof HtmlCommandLink)
            ((HtmlCommandLink)link).setStyleClass(linksClass);
          else if (link.getClass().getName().endsWith("AjaxCommandLink") || link.getClass().getPackage().getName().startsWith("org.richfaces"))
            link.getClass().getMethod("setStyleClass", new Class<?>[] { String.class }).invoke(link, new Object[] { linksClass });
        }

        link.encodeBegin(facesContext);
        facetComp.encodeBegin(facesContext);
        if (facetComp.getRendersChildren())
          facetComp.encodeChildren(facesContext);
        facetComp.encodeEnd(facesContext);
        link.encodeEnd(facesContext);
      }
      catch (Exception ex)
      {
        throw new IOException(ex.getMessage(), ex);
      }
    }
  }

  /**
   * Override to avoid re-rendering non-transient children.
   */
  @Override
  public void encodeChildren(FacesContext facescontext, UIComponent component) throws IOException
  {
    RendererUtils.checkParamValidity(facescontext, component, HtmlDataScroller.class);

    if (component.getChildCount() > 0)
    {
      HtmlDataScroller scroller = (HtmlDataScroller) component;
      String scrollerIdPagePrefix = scroller.getId() + HtmlDataScrollerRenderer.PAGE_NAVIGATION;

      for (Iterator<?> it = component.getChildren().iterator(); it.hasNext();)
      {
            UIComponent child = (UIComponent) it.next();
            String childId = child.getId();
            if (childId != null && !childId.startsWith(scrollerIdPagePrefix) && shouldRenderChild((DataScroller)scroller, child)) {
                RendererUtils.renderChild(facescontext, child);
            }
        }
      }
  }

  protected boolean shouldRenderChild(DataScroller scroller, UIComponent child)
  {
    String customButtons = scroller.getCustomButtons();
    if (customButtons != null && customButtons.length() > 0)
    {
      for (String btnDef : customButtons.split(";"))
      {
        String[] defs = btnDef.split(",");
        if (child.getId().equals(defs[0]))
          return false;
      }
    }
    return true;
  }

}
