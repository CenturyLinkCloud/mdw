/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui.list;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.timer.CodeTimer;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.services.UserManager;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIError;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.filter.Filter;
import com.centurylink.mdw.web.ui.model.RetrievalDataModel;
import com.centurylink.mdw.web.ui.model.ServerSideSorting;
import com.centurylink.mdw.web.util.RemoteLocator;

public abstract class SortableList
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  protected static final int SORT_ASCENDING = 1;
  protected static final int SORT_DESCENDING = -1;

  public abstract String getListId();
  public abstract String getName();

  public SortableList()
  {
  }

  public void initialize()
  {
    populateHeaders();
    String defaultSort = getDefaultSortColumn();
    // in case ID is specified, translate to label
    ColumnHeader header = getHeader(defaultSort);
    if (header != null)
      defaultSort = header.getLabel();
    setSortParameters(new SortParameters(defaultSort, isDefaultSortAscending()));
    setGroupBy(getDefaultGroupBy());
  }

  public String getId() { return getListId(); }

  private Filter _filter;
  public Filter getFilter() { return _filter; }
  public void setFilter(Filter filter) { _filter = filter; }

  private DataModel<ColumnHeader> _headers;
  public DataModel<ColumnHeader> getHeaders() { return _headers; }
  public void setHeaders(DataModel<ColumnHeader> dm) { _headers = dm; }

  public ColumnHeader getHeader(String id)
  {
    if (_headers != null)
    {
      for (ColumnHeader header : _headers)
      {
        if (id.equals(header.getAttribute()))
          return header;
      }
    }
    return null;
  }

  /**
   * @return column headers for this list
   * (filtered according to user preferences and any other criteria)
   */
  public abstract List<ColumnHeader> getColumnHeaders();
  /**
   * @return unfiltered column list
   */
  public abstract List<ColumnHeader> getAllColumnHeaders();

  private int _displayRows;
  /**
   * default is zero (which means display all rows)
   */
  public int getDisplayRows() { return _displayRows; }
  public void setDisplayRows(int rows) { _displayRows = rows; }

  public abstract String getDefaultSortColumn();
  public abstract boolean isDefaultSortAscending();

  private SortParameters _sortParameters;
  public SortParameters getSortParameters() { return _sortParameters; }
  public void setSortParameters(SortParameters sp) { _sortParameters = sp; }

  private String _groupBy;
  public String getGroupBy() { return _groupBy; }
  public void setGroupBy(String groupBy) { _groupBy = groupBy; }

  private String _defaultGroupBy;
  public String getDefaultGroupBy() { return _defaultGroupBy; }
  public void setDefaultGroupBy(String dgb) { _defaultGroupBy = dgb; }

  public String getSort() { return _sortParameters.getSort(); }
  /**
   * Invoked when user clicks on a sort column header.
   */
  public void setSort(String sort)
  {
    // in case ID is specified, translate to label
    ColumnHeader header = getHeader(sort);
    if (header != null)
      sort = header.getLabel();

    _sortParameters.setSort(sort);
  }

  public boolean isAscending() { return _sortParameters.isAscending(); }
  /**
   * Invoked when user clicks on a sort column header.
   */
  public void setAscending(boolean ascending)
  {
    _sortParameters.setAscending(ascending);
  }

  /**
   * provided for saving currently selected page with component state
   */
  private int _firstRow;
  public int getFirstRow() { return _firstRow; }
  public void setFirstRow(int firstRow) { _firstRow = firstRow; }

  private List<String> _itemAttributes;
  public List<String> getItemAttributes()
  {
    if (_itemAttributes == null)
    {
      _itemAttributes = new ArrayList<String>();
      for (ColumnHeader columnHeader : getColumnHeaders())
        _itemAttributes.add(columnHeader.getAttribute());
    }
    return _itemAttributes;
  }
  protected void setItemAttributes(List<String> itemAttrs)
  {
    _itemAttributes = itemAttrs;
  }

  private ListActionController _actionController;
  public ListActionController getActionController() { return _actionController; }
  public void setActionController(ListActionController lac) { _actionController = lac; }

  private DataModel<ListItem> _items;
  public DataModel<ListItem> getItems()
  {
    if (_items == null)
    {
      try
      {
        populate();
        sort();
      }
      catch (UIException ex)
      {
        logger.severeException(ex.getMessage(), ex);
      }
    }
    return _items;
  }
  public void setItems(DataModel<ListItem> dm) { _items = dm; }

  public boolean isEmpty()
  {
    if (getItems() == null)
      return true;

    return getItems().getRowCount() == 0;
  }

  public boolean isHasItems()
  {
    return !isEmpty();
  }

  public void clear()
  {
    _items = null;
    _itemAttributes = null;
    _showAll = false;
    populateHeaders(); // selected columns may have changed
  }

  public abstract boolean isExportable();
  public abstract boolean isAllRowsLink();
  public abstract boolean isHighlightCurrentRow();

  private boolean _showAll = false;
  public boolean isShowAll() { return _showAll; }
  public void setShowAll(boolean showAll) { _showAll = showAll; }

  public boolean isShowTimings()
  {
    return false;
  }

  /**
   * Override to disable sorting for this list.
   */
  public boolean isSortable()
  {
    boolean columnSortable = true;
    if (_items.isRowAvailable() && _headers.isRowAvailable())
      columnSortable = ((ColumnHeader) _headers.getRowData()).isSortable();

    return columnSortable && getItems().isRowAvailable() && getHeaders().isRowAvailable();
  }

  /**
   * Return true to indicate that only one page of data is retrieved at a time.
   */
  public boolean isPagedRetrieval()
  {
    return false;
  }

  /**
   * For paged retrieval, return the equivalent column name (for sorting).
   */
  public String getPagedListSortColumn(String columnName)
  {
    return null;
  }

  /**
   * Special columns are treated differently for paginated retrieval.
   */
  protected List<String> getSpecialColumns()
  {
    return null;
  }

  protected List<String> getIndexColumns()
  {
    return null;
  }

  /**
   * Called when isShowTimings() returns true.
   */
  public String getTimingOutput()
  {
    String msg = "Retrieval Time: " + _retrievalCodeTimer.getDuration() + " ms";
    logger.info(getName() + " " + msg);
    return msg;
  }

  /**
   * Read the list items in from the data source.  The implementation of this
   * method should return items collections based on the passed filter's criteria
   * contents.  The results should be limited to meet the criteria in this list's
   * filter.
   *
   * @return dm wrapping the list of value objects used to populate the list items
   */
  protected abstract DataModel<ListItem> retrieveItems() throws UIException;

  private CodeTimer _retrievalCodeTimer; // for timings output
  /**
   * Populates the list items.  This method needs to be called before the list
   * can be displayed, and whenever the list contents need to be refreshed.
   * The results are meant to be limited according to this list's filter.
   */
  public void populate() throws UIException
  {
    _retrievalCodeTimer = new CodeTimer(true);

    DataModel<ListItem> dataModel = retrieveItems();

    if (dataModel == null)
    {
      setItems(new ListDataModel<ListItem>(new ArrayList<ListItem>())); // don't disrupt the ui
      throw new UIException("UIError populating UI List: " + getListId());
    }

    if (isPagedRetrieval())
    {
      ServerSideSorting sorting = (ServerSideSorting) dataModel;
      sorting.setSortColumn(getPagedListSortColumn(_sortParameters.getSort()));
      sorting.setSortAscending(_sortParameters.isAscending());
    }
    else
    {
      // set the attributes on each item
      List<?> items = (List<?>) dataModel.getWrappedData();
      for (int i = 0; i < items.size(); i++)
      {
        ListItem listItem = (ListItem) items.get(i);
        listItem.setAttributes(getItemAttributes());
      }
    }

    setItems(dataModel);

    if (dataModel instanceof RetrievalDataModel)
    {
      RetrievalDataModel pagedListDataModel = (RetrievalDataModel) dataModel;
      pagedListDataModel.setRetrievalCodeTimer(_retrievalCodeTimer);
      pagedListDataModel.setSpecialColumns(getSpecialColumns());
      pagedListDataModel.setIndexColumns(getIndexColumns());
    }
    else
    {
      _retrievalCodeTimer.stop();
    }
  }

  protected void populateHeaders()
  {
    setHeaders(new ListDataModel<ColumnHeader>(getColumnHeaders()));
  }

  /**
   * Sort the list.
   */
  public void sort()
  {
    if (logger.isDebugEnabled())
      logger.debug("Sorting list : " + getListId() + " " + getSortParameters());

    String column = getSortParameters().getSort();
    boolean ascending = getSortParameters().isAscending();

    if (column != null)
    {
      int columnIndex = getColumnIndex(column);
      if (columnIndex != -1)  // sorting desired
      {
        if (isPagedRetrieval())
        {
          ServerSideSorting dataModel = (ServerSideSorting) getItems();
          dataModel.setSortColumn(getPagedListSortColumn(column));
          dataModel.setSortAscending(ascending);
          dataModel.sort();
        }
        else
        {
          int direction = (ascending) ? SORT_ASCENDING : SORT_DESCENDING;
          sort(columnIndex, direction);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  protected void sort(final int columnIndex, final int direction)
  {
    Collections.sort((List<ListItem>)_items.getWrappedData(), new Comparator<ListItem>()
      {
        @SuppressWarnings("rawtypes")
        public int compare(ListItem item1, ListItem item2)
        {
          int result = 0;
          Object val1 = item1.getAttributeValue(columnIndex);
          Object val2 = item2.getAttributeValue(columnIndex);
          if (val1 == null && val2 != null)
            result = -direction;
          else if (val1 == null && val2 == null)
            result = 0;
          else if (val1 != null && val2 == null)
            result = direction;
          else if (val1 instanceof String && val2 instanceof String)
            result = ((String)val1).compareToIgnoreCase((String)val2) * direction;
          else
            result = ((Comparable)val1).compareTo(val2) * direction;

          return result;
        }
      });
  }

  public String getColumnWidth()
  {
    String columnWidth = null;
    if (_headers.isRowAvailable())
    {
      columnWidth = ((ColumnHeader) _headers.getRowData()).getWidth();
    }
    return columnWidth;
  }

  public boolean isValueModifiable()
  {
    boolean valueModifiable = false;
    if (_items.isRowAvailable() && _headers.isRowAvailable())
    {
      valueModifiable = ((ColumnHeader) _headers.getRowData()).isEditable();
    }
    return valueModifiable;
  }

  public boolean isHeaderCheckbox()
  {
    return _headers.isRowAvailable() && ((ColumnHeader) _headers.getRowData()).isCheckbox();
  }

  public boolean isValueCheckbox()
  {
    boolean checkbox = false;
    if (_items.isRowAvailable() && _headers.isRowAvailable())
    {
      checkbox = ((ColumnHeader) _headers.getRowData()).isCheckbox();
    }
    return checkbox;
  }

  public boolean isValueLink() throws UIException
  {
    boolean link = false;
    if (_items.isRowAvailable() && _headers.isRowAvailable())
    {
      ColumnHeader header = ((ColumnHeader) _headers.getRowData());
      if (header.isLink() && ! header.getLinkAction().startsWith("/"))
      {
        // evaluate condition if required
        if (header.getLinkCondition() != null)
        {
          if (!FacesVariableUtil.isValueBindingExpression(header.getLinkCondition()))
          {
            throw new UIException("Link Condition should be a value binding expression: " + header.getLinkCondition());
          }
          else
          {
            // set value of implicit "item" faces variable
            FacesVariableUtil.setValue("item", getRow());
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ELContext elContext = facesContext.getELContext();
            ValueExpression valueExpr = facesContext.getApplication().getExpressionFactory().createValueExpression(elContext, header.getLinkCondition(), Object.class);
            return new Boolean(valueExpr.getValue(elContext).toString()).booleanValue();
          }
        }
        else
        {
        	link = true;
        }
      }
    }
    return link;
  }

  public boolean isHtmlLink() throws UIException{
	  boolean htmlLink = false;
	  if (_items.isRowAvailable() && _headers.isRowAvailable())
	    {
	      ColumnHeader header = ((ColumnHeader) _headers.getRowData());
	      if (header.isLink() && header.getLinkAction().startsWith("/"))
	      {
	    	// set value of implicit "item" faces variable
	        FacesVariableUtil.setValue("item", getRow());
	        // evaluate condition if required
	        if (header.getLinkCondition() != null)
	        {
	          if (!FacesVariableUtil.isValueBindingExpression(header.getLinkCondition()))
	          {
	            throw new UIException("Link Condition should be a value binding expression: " + header.getLinkCondition());
	          }
	          else
	          {

	            FacesContext facesContext = FacesContext.getCurrentInstance();
	            ELContext elContext = facesContext.getELContext();
	            ValueExpression valueExpr = facesContext.getApplication().getExpressionFactory().
	            		createValueExpression(elContext, header.getLinkCondition(), Object.class);
	            return new Boolean(valueExpr.getValue(elContext).toString()).booleanValue();
	          }
	        }
	        else
	        {
	        	htmlLink = true;
	        }
	      }
	    }
	  return htmlLink;
  }

  public boolean isValueImage()
  {
    boolean isImage = false;
    if ((_items.isRowAvailable() && _headers.isRowAvailable()))
      isImage = ((ColumnHeader) _headers.getRowData()).isImage();
    return isImage;
  }

  public String getImage()
  {
    String image = null;
    if ((_items.isRowAvailable() && _headers.isRowAvailable()))
      image = (String)getExpressionValue(((ColumnHeader)_headers.getRowData()).getImage());
    return image;
  }

  public boolean isValueScript()
  {
    boolean link = false;
    if (_items.isRowAvailable() && _headers.isRowAvailable())
    {
      link = ((ColumnHeader) _headers.getRowData()).isScript();
    }
    return link;
  }

  public boolean isValueDate()
  {
    return getColumnValue() instanceof Date;
  }

  public boolean isExpandable()
  {
    boolean expandable = false;
    if (_headers.isRowAvailable())
      expandable = ((ColumnHeader) _headers.getRowData()).isExpandable();
    return expandable;
  }

  /**
   * Returns the expandedContent value for the first column that has it.
   */
  public String getExpandedContent()
  {
    for (ColumnHeader header : getColumnHeaders())
    {
      if (header.getExpandedContent() != null)
        return header.getExpandedContent();
    }
    return null;
  }

  /**
   * True only if the current column has expanded content
   */
  public boolean isHasExpandedContent()
  {
    boolean hasExpandedContent = false;
    if (_headers.isRowAvailable())
      hasExpandedContent = ((ColumnHeader) _headers.getRowData()).getExpandedContent() != null;
    return hasExpandedContent;
  }

  /**
   * @deprecated old-style expandable
   */
  public String getExpandAttribute()
  {
    String expandAttr = null;
    if (_headers.isRowAvailable())
      expandAttr = ((ColumnHeader) _headers.getRowData()).getExpandAttribute();
    return expandAttr;
  }

  public Object getExpandedValue()
  {
    ListItem row = getRow();
    String expandAttr = ((ColumnHeader) _headers.getRowData()).getExpandAttribute();
    return row.getAttributeValue(expandAttr);
  }

  public String getLinkAction()
  {
    String action = null;
    FacesContext facesContext = null;
    if (_items.isRowAvailable() && _headers.isRowAvailable())
    {
      action = ((ColumnHeader)_headers.getRowData()).getLinkAction();
      if (FacesVariableUtil.isValueBindingExpression(action)) {
    	  facesContext = FacesContext.getCurrentInstance();
          ELContext elContext = facesContext.getELContext();
          ValueExpression valueExpr = facesContext.getApplication().getExpressionFactory().
        		  createValueExpression(elContext, action, Object.class);
          action = valueExpr.getValue(elContext).toString();
          String contextPath = facesContext.getExternalContext().getRequestContextPath();
          if (!action.startsWith("http") && !action.contains(contextPath) ) {
        	  if (! StringHelper.isEmpty(contextPath)) {
            	  action = contextPath + action;
              }
          }
      }
    }
    return action;
  }

  public Object getColumnValue()
  {
    Object columnValue = null;
    if (_items.isRowAvailable() && _headers.isRowAvailable())
    {
      String attrName = getRow().getAttributeName(_headers.getRowIndex());
      columnValue = getRow().getAttributeValue(attrName);
      if (columnValue instanceof Date)
      {
        String dateFormat = ((ColumnHeader) _headers.getRowData()).getDateFormat();
        if (dateFormat != null)
        {
          SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
          columnValue = sdf.format((Date)columnValue);
        }
      }
    }
    return columnValue;
  }

  @SuppressWarnings("unchecked")
  protected int getColumnIndex(final String columnName)
  {
    List<ColumnHeader> headers = (List<ColumnHeader>) _headers.getWrappedData();
    for (int i = 0; i < headers.size(); i++)
    {
      ColumnHeader header = (ColumnHeader) headers.get(i);
      if (header.getAttribute().equals(columnName))
        return i;
    }
    // fall back on column label for compatibility
    for (int i = 0; i < headers.size(); i++)
    {
      ColumnHeader header = (ColumnHeader) headers.get(i);
      if (header.getLabel().equals(columnName))
        return i;
    }
    return -1; // not found
  }

  public static void sortSelectItems(List<SelectItem> selectItems)
  {
    Collections.sort(selectItems, new Comparator<SelectItem>()
    {
      public int compare(SelectItem item1, SelectItem item2)
      {
        int result = 0;
        String label1 = item1.getLabel();
        String label2 = item2.getLabel();
        if (label1 == null && label2 != null)
          result = -1;
        else if (label1 == null && label2 == null)
          result = 0;
        else if (label1 != null && label2 == null)
          result = 1;
        else
          result = label1.compareTo(label2);
        return result;
      }
    });
  }

  public String getStyleClass() throws UIException
  {
    if (_items.isRowAvailable() && _headers.isRowAvailable())
    {
      ColumnHeader header = ((ColumnHeader) _headers.getRowData());

      if (header.getStyleClass() != null)
      {
        // evaluate condition if required
        if (header.getStyleCondition() != null)
        {
          if (!FacesVariableUtil.isValueBindingExpression(header.getStyleCondition()))
          {
            throw new UIException("Style Condition should be a value binding expression: " + header.getStyleCondition());
          }
          else
          {
            // set value of implicit "item" faces variable
            FacesVariableUtil.setValue("item", getRow());
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ELContext elContext = facesContext.getELContext();
            ValueExpression valueExpr = facesContext.getApplication().getExpressionFactory().createValueExpression(elContext, header.getStyleCondition(), Object.class);
            if (new Boolean(valueExpr.getValue(elContext).toString()).booleanValue())
              return header.getStyleClass();
            else
              return null;
          }
        }
      }
      return header.getStyleClass();
    }
    else
    {
      return null;
    }
  }

  public ListItem getRow()
  {
    return ((ListItem) getItems().getRowData());
  }

  public int getRowIndex()
  {
    return getItems().getRowIndex();
  }

  public Long getItemId()
  {
    ListItem item = getRow();
    if (item == null)
      return null;
    else
      return item.getId();
  }

  @SuppressWarnings("unchecked")
  public List<ListItem> getMarked()
  {
    List<ListItem> marked = new ArrayList<ListItem>();
    List<ListItem> all = (List<ListItem>)getItems().getWrappedData();
    if (all != null)
    {
      for (int i = 0; i < all.size(); i++)
      {
        ListItem item = (ListItem) all.get(i);
        if (item.isMarked())
          marked.add(item);
      }
    }

    return marked;
  }


  @SuppressWarnings("unchecked")
  public void addItem(ListItem item)
  {
    ((List<ListItem>)getItems().getWrappedData()).add(item);
  }

  /**
   * Called when an action link is clicked.
   *
   * @return the jsf navigation outcome
   */
  public Object performLinkAction()
  {
    try
    {
      if (isHighlightCurrentRow())
      {
        String listId = (String) FacesVariableUtil.getRequestParamValue("sortableListId");
        FacesVariableUtil.setSessionValue("dataTableCurrentRow_" + listId, String.valueOf(getRowIndex()));
      }
      return getActionController().performAction(getLinkAction(), getRow());
    }
    catch (UIException ex)
    {
      logger.severeException(ex.getMessage(), ex);
      UIError error = new UIError(ex.getMessage(), ex);
      FacesVariableUtil.setValue("error", error);
      return "go_error";
    }
  }

  /**
   * @return javascript click handler as specified in TaskView.xml
   */
  public String getLinkScript()
  {
    String script = null;
    if (_items.isRowAvailable() && _headers.isRowAvailable())
    {
      script = (String)getExpressionValue(((ColumnHeader)_headers.getRowData()).getLinkScript());
    }

    return script;
  }

  /**
   * Checks for (and evaluates) binding expressions.
   */
  public Object getExpressionValue(String value)
  {
    if (FacesVariableUtil.isValueBindingExpression(value))
    {
      // set value of implicit "item" faces variable
      FacesVariableUtil.setValue("item", getRow());
      FacesContext facesContext = FacesContext.getCurrentInstance();
      ELContext elContext = facesContext.getELContext();
      ValueExpression valueExpr = facesContext.getApplication().getExpressionFactory().createValueExpression(elContext, value, Object.class);
      return valueExpr.getValue(elContext);
    }
    return value;
  }

  /**
   * Builds the javascript for the onclick handler.  Argument is the
   * result of getOnClickHandlerArg() on the current row item.
   *
   * @return javascript onclick handler
   */
  public String getOnClickScript()
  {
    String linkScript = getLinkScript();
    if (linkScript == null || linkScript.trim().length() == 0)
      return "";

    String scriptArg = getOnClickScriptArg();
    if (scriptArg == null || scriptArg.isEmpty())
      return linkScript;
    else
      return linkScript + "('" + getOnClickScriptArg() + "');return false;";
  }

  /**
   * @return finds the onclick handler arg for the current row item.
   */
  private String getOnClickScriptArg()
  {
    return ((ListItem)getItems().getRowData()).getOnClickHandlerArg();
  }

  /**
   * User preference columns change event listener.
   */
  public void selectedColumnsChanged(ValueChangeEvent event)
  {
    List<String> prefColumns = new ArrayList<String>();
    StringTokenizer st = new StringTokenizer(event.getNewValue().toString(), ",");
    while (st.hasMoreTokens())
    {
      prefColumns.add(st.nextToken());
    }

    saveListPrefs(prefColumns);
  }

  /**
   * Returns the selected user prefs columns.
   */
  public List<SelectItem> getSelectedColumns()
  {
    List<SelectItem> selectItems = new ArrayList<SelectItem>();
    List<ColumnHeader> selCols = getUserPrefColumns();
    for (ColumnHeader col : selCols)
    {
      if (col.getLabel().length() > 0)
        selectItems.add(new SelectItem(col.getLabel()));
    }
    return selectItems;
  }

  /**
   * Returns the unselected user prefs columns.
   */
  public List<SelectItem> getUnselectedColumns()
  {
    List<SelectItem> selectItems = new ArrayList<SelectItem>();
    List<ColumnHeader> selCols = getUserPrefColumns();
    for (ColumnHeader col : getAllColumnHeaders())
    {
      if (col.getLabel().length() > 0)
      {
        boolean found = false;
        for (ColumnHeader selCol : selCols)
        {
          if (selCol.getAttribute().equals(col.getAttribute()))
          {
            found = true;
            break;
          }
        }
        if (!found)
          selectItems.add(new SelectItem(col.getLabel()));
      }
    }
    return selectItems;
  }

  public List<ColumnHeader> getUserPrefColumns()
  {
    AuthenticatedUser user = FacesVariableUtil.getCurrentUser();
    Map<String,String> prefs = user.getAttributes();
    String columnsPref = prefs.get(getListId() + ":columns");
    List<ColumnHeader> allColumns = getAllColumnHeaders();
    if (columnsPref == null)
      return allColumns;  // no preferences set

    List<ColumnHeader> someColumns = new ArrayList<ColumnHeader>();

    StringTokenizer tokenizer = new StringTokenizer(columnsPref, ", ");
    while (tokenizer.hasMoreTokens())
    {
      String prefsColAttr = tokenizer.nextToken();

      for (ColumnHeader column : allColumns)
      {
        if (column.getLabel().length() != 0 && prefsColAttr.equals(column.getAttribute()))
          someColumns.add(column);
      }
    }

    // add the mandatory columns regardless
    for (ColumnHeader column : allColumns)
    {
      if (column.getLabel().length() == 0)
      {
        if (column.getLinkAction() != null)
          someColumns.add(column);
        else if (column.isCheckbox())
          someColumns.add(0, column);
      }
    }

    return someColumns;
  }

  public void saveListPrefs(List<String> prefColumns)
  {
    String prefVal = "";
    for (String prefColumn : prefColumns)
    {
      for (ColumnHeader colHeader : getAllColumnHeaders())
      {
        if (colHeader.getLabel().equals(prefColumn))
        {
          prefVal += colHeader.getAttribute() + ",";
          break;
        }
      }
    }

    // remove last comma
    if (prefVal.length() > 0)
      prefVal = prefVal.substring(0, prefVal.length() - 1);

    Map<String,String> prefs = FacesVariableUtil.getCurrentUser().getAttributes();
    prefs.put(getListId() + ":columns", prefVal);

    try
    {
      UserManager userMgr = RemoteLocator.getUserManager();
      userMgr.updateUserPreferences(FacesVariableUtil.getCurrentUser().getId(), prefs);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  public void saveListMenuPrefs(boolean saveGroupByValue, boolean saveDisplayRowsValue, String groupByValue, Integer displayRows)
  {
    Map<String,String> prefs = FacesVariableUtil.getCurrentUser().getAttributes();

    if (saveGroupByValue && groupByValue != null)
      prefs.put(getListId() + ":groupByValue", groupByValue);

    if (saveDisplayRowsValue && displayRows != null)
      prefs.put(getListId() + ":displayRows", displayRows.toString());

    try
    {
      UserManager userMgr = RemoteLocator.getUserManager();
      userMgr.updateUserPreferences(FacesVariableUtil.getCurrentUser().getId(), prefs);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  public String resetListPrefs() throws UIException
  {
    try
    {
      // remove existing prefs for this list
      Map<String,String> prefs = FacesVariableUtil.getCurrentUser().getAttributes();
      prefs.remove(getListId() + ":columns");
      prefs.remove(getListId() + ":groupByValue");
      prefs.remove(getListId() + ":displayRows");
      UserManager userMgr = RemoteLocator.getUserManager();
      userMgr.updateUserPreferences(FacesVariableUtil.getCurrentUser().getId(), prefs);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage("Error: " + ex);
    }
    return null;  // return value required for action controller method
  }

  // the methods below are for RichFaces dataTable functionality
  // see web/demo/dataTable.xhtml for example usage

  private ListItem _currentItem;
  public ListItem getCurrentItem() { return _currentItem;  }
  public void setCurrentItem(ListItem item) { this._currentItem = item; }

  private int _currentRow;
  public int getCurrentRow() { return _currentRow; }
  public void setCurrentRow(int row) { this._currentRow = row; }

  private Set<String> _keys;
  public Set<String> getKeys() { return _keys; }
  public void setKeys(Set<String> keys) { this._keys = keys; }

  public void saveCurrentItem() throws UIException
  {
    throw new UIException("The method saveCurrentItem() needs to be implemented for " + getClass().getName());
  }

  public void deleteCurrentItem() throws UIException
  {
    throw new UIException("The method deleteCurrentItem() needs to be implemented for " + getClass().getName());
  }

  public abstract boolean isAjaxEnabled();

  public abstract boolean isSearchable();

  /**
   * btnOneId,btnOneLabel,btnOneIcon;btnTwoId,btnTwoLabel,btnTwoIcon
   */
  public String getCustomButtons()
  {
    return null;
  }

  public String getExportFormat()
  {
    return "xlsx";
  }

  public boolean isHtml5()
  {
    return FacesVariableUtil.isHtml5Rendering();
  }

  public String getAjaxListButtonClass()
  {
    if (isHtml5())
      return "org.richfaces.CommandButton";
    else
      return "org.ajax4jsf.CommandButton";
  }

  public String getAjaxListLinkClass()
  {
    if (isHtml5())
      return "com.centurylink.mdw.hub.jsf.component.AjaxCommandLink";
    else
      return "org.ajax4jsf.CommandLink";
  }

  public List<GroupingOption> getGroupingOptions()
  {
    return null;
  }

  public List<Integer> getPageSizeOptions()
  {
    return null;
  }

  public UIComponent getListMenu() throws UIException
  {
    return null;
  }

  public void setListMenu(UIComponent listMenu)
  {
    // required by JSF
  }

  public UIComponent getActionMenu() throws UIException, CachingException
  {
    return null;
  }

  public void setActionMenu(UIComponent listMenu)
  {
    // required by JSF
  }

  public boolean isColumnarFilters()
  {
    return false;
  }

  public String getOnclick()
  {
    if (_items.isRowAvailable() && _headers.isRowAvailable())
    {
      ColumnHeader header = ((ColumnHeader) _headers.getRowData());
      return header.getOnclick();
    }
    return null;
  }

  public String getLinkTarget()
  {
    if (_items.isRowAvailable() && _headers.isRowAvailable())
    {
      ColumnHeader header = ((ColumnHeader) _headers.getRowData());
      return header.getLinkTarget();
    }
    return null;
  }

}
