/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.data;

import java.util.ArrayList;
import java.util.List;

import javax.faces.FacesException;

import com.centurylink.mdw.common.query.PaginatedResponse;
import com.centurylink.mdw.common.query.QueryRequest;
import com.centurylink.mdw.taskmgr.ui.layout.ItemUI;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.list.UserPreferenceColumnSpecifier;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.filter.Filter;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.ui.model.RetrievalDataModel;
import com.centurylink.mdw.web.ui.model.ServerSidePagination;
import com.centurylink.mdw.web.ui.model.ServerSideSorting;

/**
 * Wraps a list of paged data for server-side pagination.
 */
public abstract class PagedListDataModel extends RetrievalDataModel
  implements ServerSidePagination, ServerSideSorting
{
  private int _pageSize;
  private int _rowIndex;
  private int _pageIndex;
  private PaginatedResponse _page;
  private RowConverter _rowConverter;
  private Filter _filter;
  private List<ListItem> _wrappedData;
  private String _sortColumn;
  private boolean _sortAscending = true;
  private ListUI _listUI;
  private boolean _allRowsMode;

  public String getSortColumn()
  {
    return _sortColumn;
  }

  public void setSortColumn(String s)
  {
    _sortColumn = s;
  }

  public boolean isSortAscending()
  {
    return _sortAscending;
  }
  public void setSortAscending(boolean b)
  {
    _sortAscending = b;
  }

  public ListUI getListUI()
  {
    return _listUI;
  }

  public PagedListDataModel(ListUI listUI, Filter filter)
  {
    _listUI = listUI;
    _pageSize = _listUI.getDisplayRows();
    _filter = filter;
  }

  public void setRowConverter(RowConverter rowConverter)
  {
    _rowConverter = rowConverter;
  }

  public RowConverter getRowConverter()
  {
    return _rowConverter;
  }

  public int getPageSize()
  {
    return _pageSize;
  }

  public boolean isAllRowsMode()
  {
    return _allRowsMode;
  }

  public void setAllRowsMode(boolean allRows)
  {
    _allRowsMode = allRows;
    if (allRows)
      _pageSize = QueryRequest.ALL_ROWS;
    else
      _pageSize = _listUI.getDisplayRows();
  }

  /**
   * Not used in this class; data is fetched via a callback to the
   * fetchData method rather than by explicitly assigning a list.
   */
  public void setWrappedData(Object o)
  {
    throw new UnsupportedOperationException("setWrappedData");
  }

  protected void convertFetchedRows() throws FacesException
  {
    // the wrapped data is actually a List of ListItems
    List<ListItem> list = new ArrayList<ListItem>();
    Object[] data = _page.getData();
    for (int i = 0; i < data.length; i++)
    {
      ListItem listItem = null;
      if (getRowConverter() != null)
      {
        listItem = (ListItem) getRowConverter().convertRow(data[i]);
      }
      else
      {
        listItem = (ListItem) data[i];
      }
      try
      {
        listItem.setAttributes(getItemAttributes());
        list.add(listItem);
      }
      catch (UIException ex)
      {
        throw new FacesException(ex.getMessage(), ex);
      }
    }
    _wrappedData = list;
  }

  protected List<String> getItemAttributes()
  {
    List<String> itemAttrs = new ArrayList<String>();
    for (ItemUI itemUI : getColumns(getListUI()))
      itemAttrs.add(itemUI.getAttribute());
    return itemAttrs;
  }

  protected List<ItemUI> getColumns(ListUI listUi)
  {
    return new UserPreferenceColumnSpecifier().getColumns(listUi);
  }

  public int getRowIndex()
  {
    return _rowIndex;
  }

  /**
   * Specify what the "current row" within the dataset is. Note that
   * the UIData component will repeatedly call this method followed
   * by getRowData to obtain the objects to render in the table.
   */
  public void setRowIndex(int index)
  {
    if (index != -1)
    {
      _rowIndex = index;
      if (isRowAvailable())
        _pageIndex = _rowIndex / _pageSize;
    }
  }

  public boolean isRowInCurrentPage()
  {
    return (_rowIndex >= 0 && _rowIndex < _page.getTotalNumberOfRows()
        && _rowIndex < (_pageIndex + 1) * _pageSize
        && _rowIndex >= _pageIndex * _pageSize
        && (_rowIndex % _page.getPageSize()) < _page.getNumberOfRowsReturned());
  }

  /**
   * Return the total number of rows.
   */
  public int getRowCount()
  {
    return getPage().getTotalNumberOfRows();
  }

  public void invalidate()
  {
    _page = null;
  }

  /**
   * Return the object corresponding to the current rowIndex.
   */
  @Override
  public ListItem getRowData() throws FacesException
  {
    int datasetSize = getPage().getTotalNumberOfRows();

    // Reassign datasetSize only when toalNumberofRows count is greater than showAllDisplayRows count in All Rows mode
    if (isAllRowsMode() && getPage().getMaxRowsInAllRowsMode() != 0 && getPage().getMaxRowsInAllRowsMode() <  getPage().getTotalNumberOfRows())
    {
      datasetSize = getPage().getMaxRowsInAllRowsMode();
    }

    if (_rowIndex >= datasetSize)
    {
      _rowIndex = datasetSize - 1;
    }
    if (_rowIndex < 0)
    {
      throw new IllegalStateException("Invalid row index: " + _rowIndex);
    }

    if (isAllRowsMode())
    {
      if (datasetSize > _wrappedData.size())
      {
        _page = fetchPage(0, _filter);
        convertFetchedRows();
      }
      return _wrappedData.get(_rowIndex);
    }
    else
    {
      int startRow = _pageIndex * _pageSize;
      int endRow = startRow + getPage().getPageSize() - 1;
      if (_rowIndex < startRow || _rowIndex > endRow)
      {
        _page = fetchPage(_pageIndex, _filter);
        convertFetchedRows();
      }
      int rowInPage = _rowIndex - (_pageSize * _pageIndex);
      return _wrappedData.get(rowInPage);
    }

  }

  /* (non-Javadoc)
   * @see javax.faces.model.DataModel#getWrappedData()
   */
  public Object getWrappedData()
  {
    return _wrappedData;
  }

  /**
   * Return true if the rowIndex value is currently set to a
   * value that matches some element in the total number of rows.
   */
  public boolean isRowAvailable()
  {
    if (_page == null)
      return false;

    // When All rows mode with limited Display rows
    if (_page.getPageSize() == QueryRequest.ALL_ROWS)
      return (_rowIndex >= 0 &&
      (_rowIndex < ((_page.getMaxRowsInAllRowsMode() != 0) ? _page.getMaxRowsInAllRowsMode(): _page.getTotalNumberOfRows()))
      && _rowIndex < _page.getNumberOfRowsReturned());

    return (_rowIndex >= 0 && _rowIndex < _page.getTotalNumberOfRows()
            && (_rowIndex % _page.getPageSize()) < _page.getNumberOfRowsReturned());
  }

  public void sort()
  {
    invalidate();
  }

  public PaginatedResponse getPage()
  {
    if (_page == null || (!isAllRowsMode() && _pageIndex >= 0 && (_pageIndex != _page.getPageIndex())))
    {
      if (getRetrievalCodeTimer() != null)
        getRetrievalCodeTimer().start();
      _page = fetchPage(_pageIndex, _filter);
      convertFetchedRows();
      if (getRetrievalCodeTimer() != null)
        getRetrievalCodeTimer().stop();
    }

    return _page;
  }

  /**
   * Method which must be implemented in cooperation with the
   * managed bean class to fetch data on demand.
   */
  public abstract PaginatedResponse fetchPage(int pageIndex, Filter filter);

}
