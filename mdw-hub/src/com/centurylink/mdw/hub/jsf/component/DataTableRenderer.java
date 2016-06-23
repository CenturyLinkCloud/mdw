/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf.component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.custom.column.HtmlColumn;
import org.apache.myfaces.renderkit.html.ext.HtmlTableRenderer;
import org.apache.myfaces.shared.renderkit.html.HTML;
import org.apache.myfaces.shared_tomahawk.renderkit.RendererUtils;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.jsf.components.DataTable;
import com.centurylink.mdw.web.ui.input.Input;
import com.centurylink.mdw.web.ui.list.ColumnHeader;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.ui.list.ListSearch;

public class DataTableRenderer extends HtmlTableRenderer {
    public static final String RENDERER_TYPE = DataTableRenderer.class.getName();

    public static final String CURRENT_GROUP_COL_VAL = "mdw.datatable.current.group.column.value";

    @Override
    protected void renderRowStyle(FacesContext facesContext, ResponseWriter writer, UIData uiData,
            Styles styles, int rowStyleIndex) throws IOException {
        if (styles.hasRowStyle()) {
            int currentRow = -1;
            String currentRowAttr = (String) FacesVariableUtil.getValue("dataTableCurrentRow_" + uiData.getId());
            if (currentRowAttr != null)
                currentRow = Integer.parseInt(currentRowAttr);

            String rowStyle = styles.getRowStyle(rowStyleIndex);

            // highlight the current row if there is one
            if (currentRow == rowStyleIndex + uiData.getFirst())
                rowStyle += " " + ((DataTable) uiData).getCurrentRowClass();

            writer.writeAttribute("class", rowStyle, null);
        }
    }

    @Override
    protected void renderFacet(FacesContext facesContext, ResponseWriter writer,
            UIComponent component, boolean header) throws IOException {
        int colspan = 0;
        boolean hasColumnFacet = false;
        for (Iterator<?> it = getChildren(component).iterator(); it.hasNext();) {
            UIComponent uiComponent = (UIComponent) it.next();
            // a UIColumn has a span of 1, anything else has a span of 0
            colspan += determineChildColSpan(uiComponent);

            // hasColumnFacet is true if *any* child column has a facet of
            // the specified type.
            if (!hasColumnFacet) {
                hasColumnFacet = hasFacet(header, uiComponent);
            }
        }

        UIComponent facet = header ? (UIComponent) component.getFacets().get(HEADER_FACET_NAME)
                : (UIComponent) component.getFacets().get(FOOTER_FACET_NAME);
        if (facet != null || hasColumnFacet) {
            // Header or Footer present on either the UIData or a column, so we
            // definitely need to render the THEAD or TFOOT section.
            String elemName = header ? HTML.THEAD_ELEM : HTML.TFOOT_ELEM;

            writer.startElement(elemName, component);
            if (header) {
                String headerStyleClass = getHeaderClass(component);
                if (facet != null)
                    renderTableHeaderRow(facesContext, writer, component, facet, headerStyleClass, colspan);
                if (hasColumnFacet)
                    renderColumnHeaderRow(facesContext, writer, component, headerStyleClass);
            }
            else {
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
    protected void beforeBody(FacesContext facesContext, UIData uiData) throws IOException {
        if (uiData instanceof DataTable) {
            DataTable dataTable = (DataTable) uiData;
            if (dataTable.getGroupByColumn() != null && !dataTable.getGroupByColumn().isEmpty() && !dataTable.getGroupByColumn().equals("none"))
                renderGroupToggleJavaScript(facesContext, dataTable);
        }
        super.beforeBody(facesContext, uiData);
    }

    /**
     * Overridden primarily to provide grouping functionality.  Renders the expander column
     * and the extra group summary row (hidden by default, toggled by expander onclick).
     */
    @Override
    protected void renderRowStart(FacesContext facesContext, ResponseWriter writer, UIData uiData,
            Styles styles, int rowStyleIndex) throws IOException {
        if (uiData instanceof DataTable) {
            DataTable dataTable = (DataTable) uiData;

            boolean renderRow = true;

            if (!dataTable.isServerSidePagination()) {
                // read ahead
                uiData.setRowIndex(uiData.getRowIndex() + 1);
                if (!hasRenderedChildren(uiData))
                    renderRow = false;
                uiData.setRowIndex(uiData.getRowIndex() - 1);
            }

            if (renderRow) {
                boolean limitToPageSize = dataTable.getDisplayRows() != 0; // TODO: auto-switch to all-rows mode?

                String groupByCol = dataTable.getGroupByColumn();
                boolean isGrouping = groupByCol != null && !groupByCol.isEmpty() && !groupByCol.equals("none");
                if (isGrouping) {
                    Object oldGroupColVal = facesContext.getExternalContext().getRequestMap().get(CURRENT_GROUP_COL_VAL);
                    ListItem listItem = (ListItem) uiData.getRowData();
                    Object newGroupColVal = listItem.getAttributeValue(groupByCol);
                    boolean groupColValChanged = !isColumnAttributeIdentical(oldGroupColVal, newGroupColVal);
                    // filteredListForm:myTasks:groupBy_name:Dons_Inline_Task
                    String groupByColId = getGroupSummaryText(newGroupColVal).replace(' ', '_').replace('\'',  '_');
                    String groupById = dataTable.getClientId() + ":groupBy_" + groupByCol + ":" + groupByColId; // for html elements
                    dataTable.getAttributes().put("rowId", groupById + "_" + uiData.getRowIndex());
                    if (uiData.getRowIndex() == 0 || groupColValChanged) {
                        // insert grouping row
                        writer.startElement(HTML.TR_ELEM, uiData);
                        writer.writeAttribute(HTML.ID_ATTR, groupById, null);
                        if (dataTable.getRowClasses() != null)
                            writer.writeAttribute(HTML.CLASS_ATTR, dataTable.getRowClasses(), "rowClasses");

                        // read ahead to count grouped items
                        int itemCount = 0;
                        int rowIdx = uiData.getRowIndex();
                        int readAheadIdx = rowIdx + 1;
                        Object oldReadAheadVal = newGroupColVal;
                        boolean readAheadValChanged = false;
                        while (uiData.isRowAvailable() && !readAheadValChanged && (!limitToPageSize || readAheadIdx <= dataTable.getDisplayRows())) {
                            itemCount++;
                            uiData.setRowIndex(readAheadIdx++);
                            if (uiData.isRowAvailable()) {
                                ListItem readAhead = (ListItem) uiData.getRowData();
                                Object newReadAheadVal = readAhead.getAttributeValue(groupByCol);
                                readAheadValChanged = !isColumnAttributeIdentical(oldReadAheadVal, newReadAheadVal);
                            }
                        }
                        uiData.setRowIndex(rowIdx);

                        // first column is expander
                        writer.startElement(HTML.TD_ELEM, uiData);
                        if (dataTable.getGroupHeaderClass() != null)
                            writer.writeAttribute(HTML.CLASS_ATTR, dataTable.getGroupHeaderClass(), "groupHeaderClass");
                        renderGroupingImage(facesContext, writer, dataTable, groupById, true);
                        writer.endElement(HTML.TD_ELEM);
                        for (int i = 0; i < listItem.getAttributes().size(); i++) {
                            writer.startElement(HTML.TD_ELEM, uiData);
                            if (dataTable.getGroupHeaderClass() != null)
                                writer.writeAttribute(HTML.CLASS_ATTR, dataTable.getGroupHeaderClass(), "groupHeaderClass");
                            if (listItem.getAttributeName(i).equals(groupByCol)) {
                                writer.startElement(HTML.ANCHOR_ELEM, uiData);
                                writer.writeAttribute(HTML.HREF_ATTR, "#", null);
                                writer.writeAttribute(HTML.ONCLICK_ATTR, "toggleGroupExpandState('" + groupById + "', false);", null);
                                if (dataTable.getGroupLabelClass() != null)
                                    writer.writeAttribute(HTML.CLASS_ATTR, dataTable.getGroupLabelClass(), "groupLabelClass");
                                writer.writeText(getGroupSummaryText(listItem.getAttributeValue(i)) + " (" + itemCount + ")", null);
                                writer.endElement(HTML.ANCHOR_ELEM);
                            }
                            writer.endElement(HTML.TD_ELEM);
                        }
                        writer.endElement(HTML.TR_ELEM);
                        facesContext.getExternalContext().getRequestMap().put(CURRENT_GROUP_COL_VAL, newGroupColVal);
                    }
                    super.renderRowStart(facesContext, writer, uiData, styles, rowStyleIndex);
                    writer.writeAttribute(HTML.STYLE_ATTR, "display:none", null);
                    writer.startElement(HTML.TD_ELEM, dataTable);
                    if (uiData.getRowIndex() == 0 || groupColValChanged) {
                        if (dataTable.getChildCount() > 0 && dataTable.getChildren().get(0).getAttributes().get("styleClass") != null)
                            writer.writeAttribute(HTML.CLASS_ATTR, dataTable.getChildren().get(0).getAttributes().get("styleClass"), null);
                        renderGroupingImage(facesContext, writer, dataTable, groupById, false);
                    }
                    writer.endElement(HTML.TD_ELEM);
                }
                else {
                    // no grouping
                    super.renderRowStart(facesContext, writer, uiData, styles, rowStyleIndex);
                }

            }
        }
        else {
            super.renderRowStart(facesContext, writer, uiData, styles, rowStyleIndex);
        }
    }

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM-dd-yyyy");
    protected boolean isColumnAttributeIdentical(Object oldValue, Object newValue) {
        if (oldValue == null)
            return newValue == null;
        if (newValue == null)
            return oldValue == null;

        // compare by day (TODO option to compare by week)
        if (oldValue instanceof Date) {
            if (!(newValue instanceof Date))
                return false;
            return dateFormat.format((Date)oldValue).equals(dateFormat.format((Date)newValue));
        }
        else {
            return oldValue.equals(newValue);
        }
    }

    protected String getGroupSummaryText(Object value) {
        if (value == null)
            return "";
        else if (value instanceof Date)
            return dateFormat.format((Date)value);
        else
            return value.toString();
    }

    /**
     * Extra cell for group expand/collapse.
     */
    protected void renderGroupingImage(FacesContext facesContext, ResponseWriter writer, DataTable dataTable, String groupById, boolean isExpand) throws IOException {
        if ((isExpand && dataTable.getGroupExpandImage() != null) || (!isExpand && dataTable.getGroupCollapseImage() != null)) {
            writer.startElement(HTML.IMG_ELEM, dataTable);
            if (dataTable.getGroupingImageClass() != null)
                writer.writeAttribute(HTML.CLASS_ATTR, dataTable.getGroupingImageClass(), "groupingImageClass");
            if (isExpand) {
                writer.writeAttribute(HTML.SRC_ATTR, facesContext.getExternalContext().getRequestContextPath() + dataTable.getGroupExpandImage(), "groupExpandImage");
                writer.writeAttribute(HTML.ALT_ATTR, "Expand", null);
            }
            else {
                writer.writeAttribute(HTML.SRC_ATTR, facesContext.getExternalContext().getRequestContextPath() + dataTable.getGroupCollapseImage(), "groupCollapseImage");
                writer.writeAttribute(HTML.ALT_ATTR, "Collapse", null);
            }
            writer.writeAttribute(HTML.ONCLICK_ATTR, "toggleGroupExpandState('" + groupById + "', " + isExpand + ");", null);
            writer.endElement(HTML.IMG_ELEM);
        }
    }

    @Override
    protected void renderRowEnd(FacesContext facesContext, ResponseWriter writer, UIData uiData)
            throws IOException {
        if (!hasRenderedChildren(uiData))
            return;
        else
            super.renderRowEnd(facesContext, writer, uiData);
    }

    private boolean hasRenderedChildren(UIData uiData) {
        List<?> children = getChildren(uiData);
        for (int i = 0, size = getChildCount(uiData); i < size; i++) {
            UIComponent child = (UIComponent) children.get(i);
            if (child.isRendered()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void renderColumnChildHeaderOrFooterRow(FacesContext facesContext, ResponseWriter writer,
            UIComponent component, String styleClass, boolean header) throws IOException {
        if (component.getParent() instanceof DataTable) {
            DataTable dataTable = (DataTable) component.getParent();
            String groupByCol = dataTable.getGroupByColumn();
            if (groupByCol != null && !groupByCol.isEmpty() && !groupByCol.equals("none")) {
                // if grouping, render an extra column for expand/collapse
                if (header) {
                    writer.startElement(HTML.TH_ELEM, component);
                    if (styleClass != null)
                        writer.writeAttribute(HTML.CLASS_ATTR, styleClass, null);
                    writer.endElement(HTML.TH_ELEM);
                }
                else {
                    writer.startElement(HTML.TD_ELEM, component);
                    if (styleClass != null)
                        writer.writeAttribute(HTML.CLASS_ATTR, styleClass, null);
                    writer.endElement(HTML.TD_ELEM);
                }
            }
        }
        super.renderColumnChildHeaderOrFooterRow(facesContext, writer, component, styleClass, header);
    }

    protected void renderGroupToggleJavaScript(FacesContext facesContext, DataTable dataTable) throws IOException {
        ResponseWriter writer = facesContext.getResponseWriter();
        writer.write("\n");
        writer.startElement(HTML.SCRIPT_ELEM, dataTable);
        writer.writeAttribute(HTML.TYPE_ATTR, "text/javascript", null);
        StringBuffer script = new StringBuffer();
        script.append("\n");
        script.append("function toggleGroupExpandState(groupById, expand)\n");
        script.append("{\n");
        script.append("  //@ sourceURL=dynamic.js;\n");
        script.append("  if (expand)\n");
        script.append("    document.getElementById(groupById).style.display = 'none';\n");
        script.append("  else\n");
        script.append("    document.getElementById(groupById).style.display = 'table-row';\n");
        script.append("  var tbl = document.getElementById('" + dataTable.getClientId() + "');\n");
        script.append("  var trs = tbl.getElementsByTagName('tr');\n");
        script.append("  var trMatch = null;\n");
        script.append("  for (var i = 0; i < trs.length; i++)\n");
        script.append("  {\n");
        script.append("    var tr = trs[i];\n");
        script.append("    if (tr.id.indexOf(groupById + '_') == 0)\n");  // in group but not the expander row
        script.append("    {\n");
        script.append("      trMatch = tr;\n");
        script.append("      trMatch.style.display = expand ? 'table-row' : 'none';\n");
        script.append("    }\n");
        script.append("  }\n");
        if (dataTable.getGroupHeaderClass() != null) {
            script.append("  if (expand && trMatch != null)\n");  // last tr gets group header styling
            script.append("  {\n");
            script.append("    var tds = trMatch.getElementsByTagName('td');\n");
            script.append("    for (var i = 0; i < tds.length; i++)\n");
            script.append("      tds[i].setAttribute('class', '" + dataTable.getGroupHeaderClass() + "');\n");
            script.append("  }\n");
        }
        script.append("}\n");

        writer.write(script.toString());
        writer.endElement(HTML.SCRIPT_ELEM);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void renderColumnHeaderCell(FacesContext facesContext, ResponseWriter writer,
            UIComponent uiComponent, UIComponent facet, String headerStyleClass, int colspan)
            throws IOException {
        if (uiComponent instanceof HtmlColumn) {
            HtmlColumn column = (HtmlColumn) uiComponent;
            if (amISpannedOver("header", uiComponent)) {
                return;
            }
            String headerCellTag = determineHeaderCellTag(facesContext, uiComponent.getParent());
            writer.startElement(headerCellTag, uiComponent);

            String columnId = column.getColumnId();
            if (columnId != null) {
                writer.writeAttribute(HTML.ID_ATTR, columnId, null);
            }
            if (colspan > 1) {
                writer.writeAttribute(HTML.COLSPAN_ATTR, new Integer(colspan), null);
            }
            String styleClass = ((HtmlColumn) uiComponent).getHeaderstyleClass();
            if (styleClass == null) {
                styleClass = headerStyleClass;
            }
            if (styleClass != null) {
                writer.writeAttribute(HTML.CLASS_ATTR, styleClass, null);
            }
            if (uiComponent instanceof UIData && HTML.TH_ELEM.equals(headerCellTag)) {
                Object rowData = ((UIData)uiComponent).getRowData();
                if (rowData instanceof ColumnHeader) {
                    Input filterInput = ((ColumnHeader)rowData).getFilterInput();
                    ListSearch listSearch = (ListSearch) FacesVariableUtil.getValue(ListSearch.LIST_SEARCH);
                    if (filterInput != null && filterInput.isValueEmpty() && (listSearch == null || listSearch.isValueEmpty())) {
                        writer.writeAttribute(HTML.ONMOUSEOVER_ATTR, "showFilterOption(true, this);", null);
                        writer.writeAttribute(HTML.ONMOUSEOUT_ATTR, "showFilterOption(false, this);", null);
                    }
                }
            }
            renderHtmlColumnAttributes(facesContext, writer, uiComponent, "header");
            if (facet != null) {
                RendererUtils.renderChild(facesContext, facet);
            }
            writer.endElement(determineHeaderCellTag(facesContext, uiComponent.getParent()));
        }
        else
            super.renderColumnHeaderCell(facesContext, writer, uiComponent, facet, headerStyleClass, colspan);
    }

}
