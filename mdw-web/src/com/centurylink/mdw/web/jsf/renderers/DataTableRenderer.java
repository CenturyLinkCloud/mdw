/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.renderers;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.renderkit.html.ext.HtmlTableRenderer;
import org.apache.myfaces.shared_tomahawk.renderkit.html.HTML;
import org.apache.myfaces.shared_tomahawk.renderkit.html.HtmlRendererUtils;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.jsf.components.DataTable;

/**
 * Override MyFaces table renderer to provide highlighting of
 * currently selected row for editable items.
 *
 */
public class DataTableRenderer extends HtmlTableRenderer
{
  public static final String RENDERER_TYPE = "com.centurylink.mdw.web.jsf.renderers.DataTableRenderer";

  @Override
  protected void renderRowStyle(FacesContext facesContext, ResponseWriter writer, UIData uiData, Styles styles, int rowStyleIndex) throws IOException
  {
    if (styles.hasRowStyle())
    {
      int currentRow = -1;
      String currentRowAttr = (String) FacesVariableUtil.getValue("dataTableCurrentRow_" + uiData.getId());
      if (currentRowAttr != null)
        currentRow = Integer.parseInt(currentRowAttr);

      String rowStyle = styles.getRowStyle(rowStyleIndex);

      // highlight the current row if there is one
      if (currentRow == rowStyleIndex + uiData.getFirst())
        rowStyle += " " + ((DataTable)uiData).getCurrentRowClass();

      writer.writeAttribute("class", rowStyle, null);
    }
  }

  @Override
  protected void renderFacet(FacesContext facesContext, ResponseWriter writer,
      UIComponent component, boolean header) throws IOException
  {
    int colspan = 0;
    boolean hasColumnFacet = false;
    for (Iterator<?> it = getChildren(component).iterator(); it.hasNext();)
    {
      UIComponent uiComponent = (UIComponent) it.next();
      // a UIColumn has a span of 1, anything else has a span of 0
      colspan += determineChildColSpan(uiComponent);

      // hasColumnFacet is true if *any* child column has a facet of
      // the specified type.
      if (!hasColumnFacet)
      {
        hasColumnFacet = hasFacet(header, uiComponent);
      }
    }

    UIComponent facet = header ? (UIComponent) component.getFacets().get(HEADER_FACET_NAME)
        : (UIComponent) component.getFacets().get(FOOTER_FACET_NAME);
    if (facet != null || hasColumnFacet)
    {
      // Header or Footer present on either the UIData or a column, so we
      // definitely need to render the THEAD or TFOOT section.
      String elemName = header ? HTML.THEAD_ELEM : HTML.TFOOT_ELEM;

      HtmlRendererUtils.writePrettyLineSeparator(facesContext);
      writer.startElement(elemName, component);
      if (header)
      {
        String headerStyleClass = getHeaderClass(component);
        if (facet != null)
          renderTableHeaderRow(facesContext, writer, component, facet, headerStyleClass, colspan);
        if (hasColumnFacet)
          renderColumnHeaderRow(facesContext, writer, component, headerStyleClass);
      }
      else
      {
        String footerStyleClass = getFooterClass(component);
        if (hasColumnFacet)
          renderColumnFooterRow(facesContext, writer, component, footerStyleClass);
        if (facet != null)
          renderTableFooterRow(facesContext, writer, component, facet, footerStyleClass, colspan);
      }
      writer.endElement(elemName);
    }
  }

  @Override
  protected void renderRowStart(FacesContext facesContext, ResponseWriter writer, UIData uiData,
      Styles styles, int rowStyleIndex) throws IOException
  {
    DataTable dataTable = (DataTable) uiData;

    boolean renderRow = true;

    if (!dataTable.isServerSidePagination())
    {
      // read ahead
      uiData.setRowIndex(uiData.getRowIndex() + 1);

      if (!hasRenderedChildren(uiData))
        renderRow = false;

      uiData.setRowIndex(uiData.getRowIndex() - 1);
    }

    if (renderRow)
      super.renderRowStart(facesContext, writer, uiData, styles, rowStyleIndex);
  }

  @Override
  protected void renderRowEnd(FacesContext facesContext, ResponseWriter writer, UIData uiData)
      throws IOException
  {
    if (!hasRenderedChildren(uiData))
      return;
    else
      super.renderRowEnd(facesContext, writer, uiData);
  }

  private boolean hasRenderedChildren(UIData uiData)
  {
    List<?> children = getChildren(uiData);
    for (int i = 0, size = getChildCount(uiData); i < size; i++)
    {
      UIComponent child = (UIComponent) children.get(i);
      if (child.isRendered())
      {
        return true;
      }
    }
    return false;
  }
}
