/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.components;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.myfaces.component.html.ext.HtmlDataTable;
import org.apache.myfaces.custom.crosstable.UIColumns;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.model.ServerSidePagination;

/**
 * Extends MyFaces dataTable component to enable preservation of
 * current row context for editable items.  Also performs special
 * processing to prevent duplicate retrieval for SortableList
 * data.  Should only be used in the mdw:list component.
 */
public class DataTable extends HtmlDataTable
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.web.jsf.components.DataTable";
  public static final int NON_ALL_ROWS_MODE = -1 ;

  private boolean _hasRowsSet;

  public int getRows()
  {
    int rows = getDisplayRows();
    if (isServerSidePagination())
      setAllRowsMode(rows == 0);
    return rows;
  }
  public void setRows(int rows)
  {
    if (isServerSidePagination())
    {
      if (rows == NON_ALL_ROWS_MODE)
      {
        setDisplayRows(getPageSizeAfterAllRowsModeSet(false));
      } else {
        setAllRowsMode(rows == 0);
        setDisplayRows(rows);
      }
    }
    else
      setDisplayRows(rows);
    _hasRowsSet = true;
  }

  public void setAllRowsMode(boolean allRows)
  {
    if (!isServerSidePagination())
    {
      throw new IllegalStateException("Cannot set allRowsMode unless ServerSidePagination");
    }

    ServerSidePagination ssp = (ServerSidePagination) getDataModel();
    ssp.setAllRowsMode(allRows);
  }

  public int getPageSizeAfterAllRowsModeSet(boolean allRows)
  {
    ServerSidePagination ssp = (ServerSidePagination) getDataModel();
    ssp.setAllRowsMode(allRows);
    return ssp.getPageSize();
  }

  @Override
  public int getFirst()
  {
    String currentRowAttr = (String) FacesVariableUtil.getValue("dataTableCurrentRow_" + this.getId());
    if (currentRowAttr != null && getRows() > 0)
    {
      int currentRow = Integer.parseInt(currentRowAttr);
      int page = currentRow / getRows();
      setFirst(page * getRows());
    }

    return getFirstRow();
  }

  @Override
  public void setFirst(int first)
  {
    setFirstRow(first);
  }

  private String _currentRowClass;
  public void setCurrentRowClass(String s) { _currentRowClass = s; }
  public String getCurrentRowClass()
  {
    if (_currentRowClass != null)
      return _currentRowClass;
    return FacesVariableUtil.getString(getValueExpression("currentRowClass"));
  }

  private Integer _newspaperColumns;
  public int getNewspaperColumns()
  {
    if (_newspaperColumns != null)
      return _newspaperColumns.intValue();
    return FacesVariableUtil.getInt(getValueExpression("newspaperColumns"), 1);
  }
  public void setNewspaperColumns(int newspaperColumns)
  {
    _newspaperColumns = new Integer(newspaperColumns);
  }

  private Integer _firstRow;
  public int getFirstRow()
  {
    if (_firstRow != null)
      return _firstRow.intValue();
    return FacesVariableUtil.getInt(getValueExpression("firstRow"), 0);
  }
  public void setFirstRow(int firstRow)
  {
    _firstRow = firstRow;

    // need to update the model because: HtmlCommandSortHeader.isImmediate() == true
    ValueExpression vb = getValueExpression("firstRow");
    if (vb != null)
      vb.setValue(getFacesContext().getELContext(), _firstRow);
  }

  private Integer _displayRows;
  public int getDisplayRows()
  {
    if (_displayRows != null)
      return _displayRows.intValue();
    return FacesVariableUtil.getInt(getValueExpression("displayRows"), 0);
  }
  public void setDisplayRows(int displayRows)
  {
    _displayRows = displayRows;

    // need to update the model because: HtmlCommandSortHeader.isImmediate() == true
    ValueExpression vb = getValueExpression("displayRows");
    if (vb != null)
      vb.setValue(getFacesContext().getELContext(), _displayRows);
  }

  private String _groupByColumn;
  public void setGroupByColumn(String s) { _groupByColumn = s; }
  public String getGroupByColumn()
  {
    if (_groupByColumn != null)
      return _groupByColumn;
    return FacesVariableUtil.getString(getValueExpression("groupByColumn"));
  }

  private String _groupHeaderClass;
  public void setGroupHeaderClass(String s) { _groupHeaderClass = s; }
  public String getGroupHeaderClass()
  {
    if (_groupHeaderClass != null)
      return _groupHeaderClass;
    return FacesVariableUtil.getString(getValueExpression("groupHeaderClass"));
  }

  private String _groupLabelClass;
  public void setGroupLabelClass(String s) { _groupLabelClass = s; }
  public String getGroupLabelClass()
  {
    if (_groupLabelClass != null)
      return _groupLabelClass;
    return FacesVariableUtil.getString(getValueExpression("groupLabelClass"));
  }

  private String _groupExpandImage;
  public void setGroupExpandImage(String s) { _groupExpandImage = s; }
  public String getGroupExpandImage()
  {
    if (_groupExpandImage != null)
      return _groupExpandImage;
    return FacesVariableUtil.getString(getValueExpression("groupExpandImage"));
  }

  private String _groupCollapseImage;
  public void setGroupCollapseImage(String s) { _groupCollapseImage = s; }
  public String getGroupCollapseImage()
  {
    if (_groupCollapseImage != null)
      return _groupCollapseImage;
    return FacesVariableUtil.getString(getValueExpression("groupCollapseImage"));
  }

  private String _groupingImageClass;
  public void setGroupingImageClass(String s) { _groupingImageClass = s; }
  public String getGroupingImageClass()
  {
    if (_groupingImageClass != null)
      return _groupingImageClass;
    return FacesVariableUtil.getString(getValueExpression("groupingImageClass"));
  }

  public Object saveState(FacesContext context)
  {
    Object[] values = new Object[12];
    values[0] = super.saveState(context);
    values[1] = _currentRowClass;
    values[2] = _hasRowsSet;
    values[3] = _newspaperColumns;
    values[4] = getFirstRow();
    values[5] = getDisplayRows();
    values[6] = getGroupByColumn();
    values[7] = getGroupHeaderClass();
    values[8] = getGroupLabelClass();
    values[9] = getGroupExpandImage();
    values[10] = getGroupCollapseImage();
    values[11] = getGroupingImageClass();
    return values;
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object[] values = (Object[]) state;
    super.restoreState(context, values[0]);
    _currentRowClass = (String) values[1];
    _hasRowsSet = (Boolean) values[2];
    _newspaperColumns = (Integer) values[3];
    if (values[4] != null)
    {
      _firstRow = (Integer) values[4];
      ValueExpression vb = getValueExpression("firstRow");
      if (vb != null)
        vb.setValue(getFacesContext().getELContext(), _firstRow);
    }
    if (values[5] != null)
    {
      _displayRows = (Integer) values[5];
      ValueExpression vb = getValueExpression("displayRows");
      if (vb != null)
        vb.setValue(getFacesContext().getELContext(), _displayRows);
    }
    _groupByColumn = (String) values[6];
    _groupHeaderClass = (String) values[7];
    _groupLabelClass = (String) values[8];
    _groupExpandImage = (String) values[9];
    _groupCollapseImage = (String) values[10];
    _groupingImageClass = (String) values[11];
  }

  public boolean isServerSidePagination()
  {
    return getDataModel() instanceof ServerSidePagination;
  }

  public boolean isShowAll()
  {
    if (!_hasRowsSet)
      return false;
    else
      return getRows() == 0;
  }

  public void processDecodes(FacesContext context)
  {
    if (!isRendered())
    {
      return;
    }

    setRowIndex(-1);
    processColumnDecodes(context);
    setRowIndex(-1);
  }

  private void processColumnDecodes(FacesContext context)
  {
    for (UIComponent child : getChildren())
    {
      if (child instanceof UIColumns)
      {
        child.processDecodes(context);
      }
    }
  }

  public void processValidators(FacesContext context)
  {
    return; // no validation inside the data table
  }

  public void processUpdates(FacesContext context)
  {
    if (!isRendered())
    {
      return;
    }

    processColumnUpdates(context);
    setRowIndex(-1);
  }

  private void processColumnUpdates(FacesContext context)
  {
    for (UIComponent child : getChildren())
    {
      if (child instanceof UIColumns)
      {
        child.processUpdates(context);
      }
    }
  }

}
