/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.list;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;

import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.taskmgr.ui.data.CompatibilityColumnMapper;
import com.centurylink.mdw.taskmgr.ui.data.DefaultListColumnMapper;
import com.centurylink.mdw.taskmgr.ui.layout.ItemUI;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.layout.ViewUI;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.jsf.components.DataTable;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.input.DateRangeInput;
import com.centurylink.mdw.web.ui.input.Input;
import com.centurylink.mdw.web.ui.list.ColumnHeader;
import com.centurylink.mdw.web.ui.list.GroupingOption;
import com.centurylink.mdw.web.ui.list.ListActionController;
import com.centurylink.mdw.web.ui.list.ListColumnMapper;
import com.centurylink.mdw.web.ui.list.SortParameters;

/**
 * View tier model object representing a sortable list.  Determines
 * the User Interface layout based on a ListUI layout configurator.
 */
public abstract class SortableList extends com.centurylink.mdw.web.ui.list.SortableList
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  private ListUI _listUI;
  public void setListUI(ListUI listUI) { _listUI = listUI; }
  public ListUI getListUI() { return _listUI; }

  /**
   * Invoked when user clicks on a sort column header.  Saves the sort column
   * on the ListManager so that it will be preserved with the session.
   */
  public void setSort(String sort)
  {
    super.setSort(sort);
    ListManager.getInstance().setSortParameters(getListId(), getSortParameters());
  }

  /**
   * Invoked when user clicks on a sort column header.  Saves the ascending value
   * on the ListManager so that it will be preserved with the session.
   */
  public void setAscending(boolean ascending)
  {
    super.setAscending(ascending);
    ListManager.getInstance().setSortParameters(getListId(), getSortParameters());
  }

  /**
   * Preserve display rows with the session.
   */
  public int getDisplayRows()
  {
    Integer displayRows = ListManager.getInstance().getDisplayRows(getListId());
    if (displayRows == null)
    {
      Map<String,String> prefs = FacesVariableUtil.getCurrentUser().getAttributes();
      if(prefs.containsKey(getListId()+":displayRows"))
        displayRows = Integer.parseInt(prefs.get(getListId()+":displayRows"));
    }
    if (displayRows == null)
      displayRows = _listUI.getDisplayRows();
    return displayRows;
  }
  public void setDisplayRows(int rows)
  {
    super.setDisplayRows(rows);
    ListManager.getInstance().setDisplayRows(getListId(), rows);
  }

  public void setGroupBy(String groupBy)
  {
    super.setGroupBy(groupBy);
    if (groupBy != null && !groupBy.equals("none"))
    {
      String prevSort = getSort();
      setSort(groupBy);
      // default to ascending unless already specified
      if (!getSort().equals(prevSort))
        setAscending(true);
    }
  }

  /**
   * Constructor.
   *
   * @param listUI layout configurator
   */
  public SortableList(ListUI listUI)
  {
    _listUI = listUI;
  }
  // Required for Dynamic Java
  public SortableList()
  {
    super();
  }
  @Override
  public void initialize()
  {
    populateHeaders();
    SortParameters sortParams = ListManager.getInstance().getSortParameters(getListId());
    if (sortParams == null)
    {
      String defaultSort = getDefaultSortColumn();
      // in case ID is specified, translate to label
      ColumnHeader header = getHeader(defaultSort);
      if (header != null)
        defaultSort = header.getLabel();
      sortParams = new SortParameters(defaultSort, isDefaultSortAscending());
    }
    setSortParameters(sortParams);
    setGroupBy(getDefaultGroupBy());
    try
    {
      if (getActionController() == null)
      {
        // set the controller
        String controllerName = getListUI().getController();
        if (controllerName != null && !controllerName.equals("none"))
        {
          if (FacesVariableUtil.isValueBindingExpression(controllerName)) // Dynamic Controller class
          {
            String name = controllerName.substring(controllerName.indexOf("#{") + 2, controllerName.indexOf("}"));
            Object dynControllerObj = FacesVariableUtil.getValue(name);
            if (dynControllerObj != null)
              setActionController((ListActionController) dynControllerObj);
          }
          else
          {
            Class<? extends ListActionController> controllerClass = Class.forName(controllerName).asSubclass(ListActionController.class);
            setActionController(controllerClass.newInstance());
          }
        }
      }
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  public String getListId()
  {
    return _listUI.getId();
  }
  public String getName()
  {
    return _listUI.getName();
  }
  public String getDefaultSortColumn()
  {
    return _listUI.getDefaultSortColumn();
  }
  public boolean isDefaultSortAscending()
  {
    return !_listUI.isDefaultSortDescending();
  }
  public boolean isExportable()
  {
    return _listUI.isExportable();
  }
  public boolean isShowTimings()
  {
    return _listUI.isShowTimings();
  }
  public boolean isAllRowsLink()
  {
    return _listUI.isAllRowsLink();
  }
  public boolean isHighlightCurrentRow()
  {
    return true;
  }
  public boolean isAjaxEnabled()
  {
    return _listUI.isAjaxEnabled();
  }
  public boolean isSearchable()
  {
	return _listUI.isSearchable();
  }

  public String getCustomButtons()
  {
    return _listUI.getCustomButtons();
  }

  public String getDefaultGroupBy()
  {
    return _listUI.getDefaultGroupBy();
  }

  public boolean isColumnarFilters()
  {
    return _listUI.isColumnarFilters();
  }

  public int getShowAllDisplayRows()
  {
    return _listUI.getShowAllDisplayRows();
  }

  @Override
  public boolean isSortable()
  {
    boolean columnSortable = true;
    if (getItems().isRowAvailable() && getHeaders().isRowAvailable())
    {
      ColumnHeader header = (ColumnHeader) getHeaders().getRowData();
      String groupBy = getGroupBy();
      boolean isGrouping = groupBy != null && !groupBy.equals("none");
      columnSortable = header.isSortable() && (!isGrouping || groupBy.equals(header.getAttribute()));
    }

    return columnSortable && getItems().isRowAvailable() && getHeaders().isRowAvailable();
  }

  /**
   * Column values in TaskView.xml are like "id1,id2=label2"
   * (if label is not specified, then the column label is used).
   */
  @Override
  public List<GroupingOption> getGroupingOptions()
  {
    if (_listUI.getGroupingOptions() == null)
      return null;
    List<GroupingOption> groupingOptions = new ArrayList<GroupingOption>();
    for (String opt : _listUI.getGroupingOptions().split(","))
    {
      int eqIdx = opt.indexOf('=');
      if (eqIdx > 0)
        groupingOptions.add(new GroupingOption(opt.substring(0, eqIdx), opt.substring(eqIdx + 1, opt.length())));
      else
        groupingOptions.add(new GroupingOption(opt, _listUI.getColumn(opt).getName())); // use column name
    }
    return groupingOptions;
  }

  @Override
  public List<Integer> getPageSizeOptions()
  {
    if (_listUI.getPageSizeOptions() == null)
      return null;
    List<Integer> pageSizeOptions = new ArrayList<Integer>();
    for (String opt : _listUI.getPageSizeOptions().split(","))
      pageSizeOptions.add(new Integer(opt));
    return pageSizeOptions;
  }

  @Override
  public List<String> getItemAttributes()
  {
    List<String> itemAttrs = new ArrayList<String>();
    for (ItemUI itemUI : getColumns(getListUI()))
      itemAttrs.add(itemUI.getAttribute());
    return itemAttrs;
  }

  /**
   * Populate the column headers based on an XML-driven layout object.
   */
  @Override
  public List<ColumnHeader> getAllColumnHeaders()
  {
    List<ItemUI> layoutColumns = getListUI().getVisibleColumns();
    List<ColumnHeader> headers = new ArrayList<ColumnHeader>(layoutColumns.size());

    for (int i = 0; i < layoutColumns.size(); i++)
    {
      ItemUI column = (ItemUI) layoutColumns.get(i);
      ColumnHeader header = new ColumnHeader(column.getName());
      header.setAttribute(column.getAttribute());
      if (header.getWidth() != null)
        header.setWidth(column.getWidth());
      if (column.isCheckbox())
        header.setCheckbox(true);
      if (column.getLinkAction() != null)
        header.setLinkAction(column.getLinkAction());
      if (column.getLinkScript() != null)
        header.setLinkScript(column.getLinkScript());
      if (column.getLinkCondition() != null)
        header.setLinkCondition(column.getLinkCondition());
      if (column.getStyleClass() != null)
        header.setStyleClass(column.getStyleClass());
      if (column.getStyleCondition() != null)
        header.setStyleCondition(column.getStyleCondition());
      if (column.getDateFormat() != null)
        header.setDateFormat(column.getDateFormat());
      if (column.getExpandAttribute() != null)
        header.setExpandAttribute(column.getExpandAttribute());
      if (column.getImage() != null)
        header.setImage(column.getImage());
      header.setSortable(column.isSortable());
      header.setExpandable(column.isExpandable());
      header.setExpandedContent(column.getExpandedContent());
      header.setOnclick(column.getOnclick());
      header.setLinkTarget(column.getLinkTarget());
      headers.add(header);
    }
    return headers;
  }

  /**
   * filtered by user prefs
   */
  @Override
  public List<ColumnHeader> getColumnHeaders()
  {
    List<ItemUI> layoutColumns = getColumns(getListUI());
    List<ColumnHeader> userHeaders = new ArrayList<ColumnHeader>(layoutColumns.size());

    for (ItemUI itemUI : layoutColumns)
    {
      for (ColumnHeader header : getAllColumnHeaders())
      {
        if (itemUI.getAttribute().equals(header.getAttribute()))
        {
          // Code to map filterInput to Column header - Columnar filters
          if (itemUI.getFilterField() != null)
          {
            if (getFilter() != null && getListUI().isColumnarFilters())
            {
              Iterator<Input> filterInputItr = getFilter().getCriteria().iterator();
              while (filterInputItr.hasNext())
              {
                Input filterInput = filterInputItr.next();
                if (itemUI.getFilterField().equals(filterInput.getAttribute()))
                {
                  header.setFilterInput(filterInput.isInputTypeDateRange() ? (DateRangeInput) filterInput : filterInput);
                  break;
                }
              }
            }
          }
          userHeaders.add(header);
        }
       }
    }

    return userHeaders;
  }

  protected List<ItemUI> getColumns(ListUI listUi)
  {
    return new UserPreferenceColumnSpecifier().getColumns(listUi);
  }

  public boolean isPagedRetrieval()
  {
    return _listUI.isPaginatedResponse();
  }

  /**
   * Special columns have attributes prefixed with the '$' character.
   * @return a list containing the special column attrs without the $ prefix
   */
  protected List<String> getSpecialColumns()
  {
    List<String> specialColumns = null;
    for (ItemUI itemUI : getListUI().getColumns())
    {
      if (itemUI.getAttribute().startsWith("$"))
      {
        if (specialColumns == null)
          specialColumns = new ArrayList<String>();
        if (itemUI.getDateFormat() != null)
          specialColumns.add("DATE:" + itemUI.getAttribute().substring(1));
        else
          specialColumns.add(itemUI.getAttribute().substring(1));
      }
    }
    return specialColumns;
  }

  protected List<String> getIndexColumns()
  {
    List<String> indexColumns = null;
    for (ItemUI itemUI : getListUI().getColumns())
    {
      if (itemUI.getAttribute().startsWith("#") && !itemUI.getAttribute().startsWith("#{"))
      {
        if (indexColumns == null)
          indexColumns = new ArrayList<String>();
          indexColumns.add(itemUI.getAttribute().substring(1));
      }
    }
    return indexColumns;
  }

  /**
   * For paginated responses, maps the column name to the db column.
   * The mapping information comes from TaskView.xml via mListUI.
   * @param columnName
   * @return the db column
   */
  public String getPagedListSortColumn(String columnName)
  {
    ListColumnMapper columnMapper = getListColumnMapper();
    ItemUI column = _listUI.getColumnByName(columnName);
    String dbColumn = columnMapper.getDatabaseColumn(column.getAttribute());
    if (dbColumn == null && column.getAttribute().startsWith("#"))
      dbColumn = column.getAttribute().substring(1);
    if (dbColumn == null)
      logger.severe("No DB Column found for ServerSideSorting: " + columnName);
    return dbColumn;
  }

  private ListColumnMapper listColumnMapper;
  public ListColumnMapper getListColumnMapper()
  {
    if (listColumnMapper == null)
    {
      String spec = getListUI().getColumnMapper();
      if (spec == null)
      {
        try
        {
          listColumnMapper = new DefaultListColumnMapper(getListUI().getId());
        }
        catch (IOException ex)
        {
          return new CompatibilityColumnMapper(getListUI());
        }
      }
      else
      {
        try
        {
          Class<? extends ListColumnMapper> mapperClass = Class.forName(spec).asSubclass(ListColumnMapper.class);
          listColumnMapper = mapperClass.newInstance();
        }
        catch (Exception ex)
        {
          logger.severeException(ex.getMessage(), ex);
        }
      }
    }
    return listColumnMapper;
  }

  public boolean isShowAll()
  {
    DataTable dataTable = ListManager.getInstance().getDataTable(getListUI().getId());
    if (dataTable == null)
      return false;
    return dataTable.isShowAll();
  }

  /**
   * This methid will return list searchable columns
   * and will also ensure that first element in list is
   * always have display search attribute as true
   * @return
   */
  public List<String> getSearchColumns(){
	  List<String> columnList = new ArrayList<String>();
	  List<ItemUI> itemList = _listUI.getColumns();
	  for (ItemUI viewItem : itemList) {
		  if (! columnList.contains(viewItem.getAttribute()) && !viewItem.getName().isEmpty() && null == viewItem.getImage()) {
			  columnList.add(viewItem.getAttribute());
		  }
	  }
	  return columnList;
  }

  public int getSearchColumnCount()
  {
    return getSearchColumns().size();
  }

  public String getSearchInstruction() {
	  String searchinstruction = null;
	  List<ItemUI> itemList = _listUI.getColumns();
	  StringBuffer sb = new StringBuffer();
	  for (ItemUI viewItem : itemList) {
		  if (! StringHelper.isEmpty(viewItem.getName())) {
			  if (! StringHelper.isEmpty(sb.toString())) {
				  sb.append(" or ");
			  }
			  sb.append(viewItem.getName());
		  }
	  }
	  if (! StringHelper.isEmpty(sb.toString())) {
		  searchinstruction = "Search with " + sb.toString();
	  }
	  return searchinstruction;
  }

  public static void syncList(String listId)
  {
    try
    {
      if (ViewUI.getInstance().getListUI(listId) != null)
      {
        SortableList list = ListManager.getInstance().getList(listId);
        if (list != null)
        {
          list.populate();
          list.sort();
        }
      }
    }
    catch (UIException ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  @Override
  public void clear()
  {
    setItems(null);
    setItemAttributes(null);
    setShowAll(false);
    if (_listUI != null)
      populateHeaders(); // selected columns may have changed
  }

  public void setCriteriaValue(Object obj)
  {
    if(getHeaders().isRowAvailable())
    {
      ColumnHeader ch = (ColumnHeader)getHeaders().getRowData();
      ch.getFilterInput().setValue(obj);
    }
  }

  public List<String> getUserPreferredDbColumns()
  {
    List<String> userPrefrdDBClmns = new ArrayList<String>();
    ListColumnMapper columnMapper = getListColumnMapper();
    if (columnMapper != null)
    {
      for (ColumnHeader colHeader : getColumnHeaders())
      {
        String dbCol = columnMapper.getDatabaseColumn(colHeader.getAttribute());
        if (dbCol != null)
          userPrefrdDBClmns.add(dbCol);
      }
    }
    return userPrefrdDBClmns;
  }

  private UIComponent listMenu;
  public UIComponent getListMenu() throws UIException
  {
    if (getGroupingOptions() == null && getPageSizeOptions() == null)
      return null;
    if (listMenu == null)
    {
      try
      {
        // use reflection since ListMenu uses RF4 and resides in mdw-hub
        Class<? extends UIComponent> listMenuClass = Class.forName("com.centurylink.mdw.hub.ui.list.ListMenu").asSubclass(UIComponent.class);
        Constructor<? extends UIComponent> constructor = listMenuClass.getConstructor(new Class<?>[]{String.class, List.class, List.class});
        listMenu = constructor.newInstance(new Object[]{getId(), getGroupingOptions(), getPageSizeOptions()});
      }
      catch (Exception ex)
      {
        throw new UIException(ex.getMessage(), ex);
      }
    }
    return listMenu;
  }

}
