/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.layout;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;

public class ListUI extends UI
{
  public ListUI() {}

  private String _controller;
  public String getController() { return _controller; }

  private String _columnMapper;
  public String getColumnMapper() { return _columnMapper; }

  private List<ItemUI> _columns = new ArrayList<ItemUI>();
  public List<ItemUI> getColumns() { return _columns; }
  public List<ItemUI> getVisibleColumns()
  {
    // remove columns not visible to user
    List<ItemUI> visCols = new ArrayList<ItemUI>();
    for (ItemUI itemUi : getColumns())
    {
      boolean visible = true;
      if (itemUi.getRolesWhoCanView() != null)
      {
        visible = false;
        for (String role : itemUi.getRolesWhoCanView())
        {
          if (FacesVariableUtil.getCurrentUser().isInRoleForAnyGroup(role))
          {
            visible = true;
            break;
          }
        }
      }
      if (visible)
        visCols.add(itemUi);
    }
    return visCols;
  }

  public List<ItemUI> getConcealColumns()
  {
    List<ItemUI> concealCols = new ArrayList<ItemUI>();
    for (ItemUI item : getColumns())
    {
      if (!item.isDisplay())
      {
        concealCols.add(item);
      }
    }
    return concealCols;
  }

  private String _defaultSortColumn;
  public String getDefaultSortColumn() { return _defaultSortColumn; }

  private String _filter;
  public String getFilter() { return _filter; }

  private int _displayRows;
  public int getDisplayRows() { return _displayRows; }

  private boolean _defaultSortDescending = false;
  public boolean isDefaultSortDescending() { return _defaultSortDescending; }

  private boolean _paginatedResponse = false;
  public boolean isPaginatedResponse() { return _paginatedResponse; }

  private String _viewBy;
  public String getViewBy() { return _viewBy; }
  public void setViewBy(String s) { _viewBy = s; }

  private boolean _exportable;
  public boolean isExportable() { return _exportable; }
  public void setExportable(boolean b) { _exportable = b; }

  private boolean _allRowsLink;
  public boolean isAllRowsLink() { return _allRowsLink; }
  public void setAllRowsLink(boolean b) { _allRowsLink = b; }

  private boolean _showTimings;
  public boolean isShowTimings() { return _showTimings; }
  public void setShowTimings(boolean b) { _showTimings = b; }

  private boolean _ajaxEnabled;
  public boolean isAjaxEnabled() { return _ajaxEnabled; }
  public void setAjaxEnabled(boolean b) { _ajaxEnabled = b; }

  private String _customButtons;
  public String getCustomButtons() { return _customButtons; }
  public void setCustomButtons(String s) { _customButtons = s; }

  private boolean _searchable;
  public boolean isSearchable() { return _searchable; }
  public void setSearchable(boolean searchable) { _searchable = searchable; }

  private String _groupingOptions;
  public String getGroupingOptions() { return _groupingOptions; }
  public void setGroupingOptions(String opts) { _groupingOptions = opts; }

  private String _pageSizeOptions;
  public String getPageSizeOptions() { return _pageSizeOptions; }
  public void setPageSizeOptions(String opts) { _pageSizeOptions = opts; }

  private String _defaultGroupBy;
  public String getDefaultGroupBy() { return _defaultGroupBy; }
  public void setDefaultGroupBy(String gb) { _defaultGroupBy = gb; }

  private boolean _columnarFilters;
  public boolean isColumnarFilters() { return _columnarFilters; }
  public void setColumnarFilters(boolean clmnFilters) { _columnarFilters = clmnFilters; }

  private int _showAllDisplayRows;
  public int getShowAllDisplayRows() { return _showAllDisplayRows; }
  public void setShowAllDisplayRows(int sadr) { _showAllDisplayRows = sadr; }

  public ItemUI getColumn(String colAttr)
  {
    for (ItemUI col : getColumns())
    {
      if (col.getAttribute().equals(colAttr))
        return col;
    }
    return null;
  }

  public ItemUI getColumnByName(String colName)
  {
    for (ItemUI col : getColumns())
    {
      if (col.getName().equals(colName))
        return col;
    }
    return null;
  }

  public void addColumn(ItemUI column)
  {
    getColumns().add(column);
  }

  public String getAttribute(String name)
  {
    for (ItemUI col : getColumns())
    {
      if (col.getName().equals(name))
        return col.getAttribute();
    }
    return null;
  }

  public void addListUI(String id, String name, String model, String filter, String controller, String columnMapper,
      String defaultSortColumn, String defaultSortDescending, String displayRows, String paginatedResponse,
      String viewBy, String exportable, String allRowsLink, String showTimings, String ajaxEnabled,
      String customButtons, String searchable, String groupingOptions, String defaultGroupBy, String pageSizeOptions,
      String columnarFilters, String showAllDisplayRows)
  throws UIException
  {
    setId(id);
    setName(name);
    setModel(model);
    _controller = controller;
    _columnMapper = columnMapper;
    _filter = filter;
    _defaultSortColumn = defaultSortColumn;
    _defaultSortDescending = Boolean.valueOf(defaultSortDescending).booleanValue();
    _displayRows = Integer.parseInt(displayRows);
    _paginatedResponse = Boolean.valueOf(paginatedResponse).booleanValue();
    _viewBy = viewBy;
    _exportable = Boolean.valueOf(exportable).booleanValue();
    _allRowsLink = Boolean.valueOf(allRowsLink).booleanValue();
    _showTimings = Boolean.valueOf(showTimings).booleanValue();
    _ajaxEnabled = Boolean.valueOf(ajaxEnabled).booleanValue();
    _searchable = Boolean.valueOf(searchable).booleanValue();
    _customButtons = customButtons;
    _groupingOptions = groupingOptions;
    _defaultGroupBy = defaultGroupBy;
    _pageSizeOptions = pageSizeOptions;
    _columnarFilters = Boolean.valueOf(columnarFilters).booleanValue();
    if (showAllDisplayRows != null && !showAllDisplayRows.isEmpty())
      _showAllDisplayRows = Integer.parseInt(showAllDisplayRows);

    ViewUI.getInstance().addListUI(id, this);
  }
}
