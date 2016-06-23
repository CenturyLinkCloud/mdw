/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.list;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.property.PropertyUtil;
import com.centurylink.mdw.taskmgr.ui.events.list.AuditEventsList;
import com.centurylink.mdw.taskmgr.ui.events.list.ExternalEventsList;
import com.centurylink.mdw.taskmgr.ui.filter.FilterManager;
import com.centurylink.mdw.taskmgr.ui.layout.ItemUI;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.layout.ViewUI;
import com.centurylink.mdw.taskmgr.ui.orders.OrderTaskList;
import com.centurylink.mdw.taskmgr.ui.orders.attachments.OrderAttachments;
import com.centurylink.mdw.taskmgr.ui.orders.notes.OrderNotes;
import com.centurylink.mdw.taskmgr.ui.process.ProcessInstances;
import com.centurylink.mdw.taskmgr.ui.reports.TaskCategoryReportList;
import com.centurylink.mdw.taskmgr.ui.reports.TaskReportList;
import com.centurylink.mdw.taskmgr.ui.reports.UserReportList;
import com.centurylink.mdw.taskmgr.ui.roles.Roles;
import com.centurylink.mdw.taskmgr.ui.tasks.Tasks;
import com.centurylink.mdw.taskmgr.ui.tasks.attachments.TaskAttachments;
import com.centurylink.mdw.taskmgr.ui.tasks.attributes.TaskAttributes;
import com.centurylink.mdw.taskmgr.ui.tasks.categories.TaskCategories;
import com.centurylink.mdw.taskmgr.ui.tasks.filter.TaskFilter;
import com.centurylink.mdw.taskmgr.ui.tasks.history.TaskHistory;
import com.centurylink.mdw.taskmgr.ui.tasks.list.SubTaskList;
import com.centurylink.mdw.taskmgr.ui.tasks.list.UserTasks;
import com.centurylink.mdw.taskmgr.ui.tasks.list.WorkgroupTasks;
import com.centurylink.mdw.taskmgr.ui.tasks.notes.TaskNotes;
import com.centurylink.mdw.taskmgr.ui.tasks.variable.TaskVariables;
import com.centurylink.mdw.taskmgr.ui.user.Users;
import com.centurylink.mdw.taskmgr.ui.workgroups.Workgroups;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.jsf.components.DataTable;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.input.DateRangeInput;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.ui.list.ListSearch;
import com.centurylink.mdw.web.ui.list.SortParameters;
import com.centurylink.mdw.taskmgr.ui.workgroups.Workasset;
/**
 * Provides a central, instantiable entity for accessing cached list data.  The
 * UI facelets access the various task lists and filter criteria lists via an
 * instance of the ListManager treated as a managed bean.  The instance is kept
 * at session scope, but data refresh is governed by the ListRefreshPhaseListener.
 */
public class ListManager
{
  protected static StandardLogger logger = LoggerUtil.getStandardLogger();

  private Map<String,SortableList> _lists = new HashMap<String,SortableList>();
  protected Map<String,SortableList> getLists() { return _lists; }

  // sort params , groupBy, and display rows are retained per session
  private Map<String,SortParameters> _sortParameters = new HashMap<String,SortParameters>();
  public void setSortParameters(String listId, SortParameters sortParameters)
  {
    _sortParameters.put(listId, sortParameters);
  }
  public SortParameters getSortParameters(String listId)
  {
    return _sortParameters.get(listId);
  }

  private Map<String,Integer> _displayRows = new HashMap<String,Integer>();
  public void setDisplayRows(String listId, Integer displayRows)
  {
    _displayRows.put(listId, displayRows);
  }
  public Integer getDisplayRows(String listId)
  {
    return _displayRows.get(listId);
  }

  private Map<String,String> _groupBy = new HashMap<String,String>();
  public void setGroupBy(String listId, String groupBy)
  {
    _groupBy.put(listId, groupBy);
  }
  public String getGroupBy(String listId)
  {
    return _groupBy.get(listId);
  }

  public ListManager()
  {
  }

  /**
   * Employs the factory pattern to return a ListManager instance
   * dictated by the property "list.manager" (default is listManager).
   * @return a handle to a ListManager instance
   */
  public static ListManager getInstance()
  {
    String managedBeanProp = "list.manager";
    // special handling for injected managed beans
    String injected = (String)FacesVariableUtil.getRequestAttrValue("injectedListManager");
    if (injected != null)
      managedBeanProp = injected;

    String listManagerBean = null;
    try
    {
      PropertyManager propMgr = PropertyUtil.getInstance().getPropertyManager();
      listManagerBean = propMgr.getStringProperty("MDWFramework.TaskManagerWeb", managedBeanProp);
    }
    catch (PropertyException ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
    if (listManagerBean == null)
    {
      listManagerBean = "listManager";
    }
    ListManager lm = (ListManager)FacesVariableUtil.getValue(listManagerBean);
    if (lm == null)
    {
      logger.info("No managed bean found for name '" + listManagerBean + "'");
      lm = createInstance();  // default
      FacesVariableUtil.setValue(listManagerBean, lm);
    }
    return lm;
  }

  protected static ListManager createInstance()
  {
    return new ListManager();
  }

  public UserTasks getMyTasks() throws UIException
  {
    return (UserTasks) getList("userTaskList");
  }

  // TODO handle different task lists through user-customizable quickviews
  // the definition xml to be stored in the rule_set table?
  public UserTasks getMyTasksToday() throws UIException
  {
    TaskFilter userTasksFilter = (TaskFilter) FilterManager.getInstance().getUserTasksFilter();
    DateRangeInput dueDateInput = (DateRangeInput) userTasksFilter.getInput("dueDate");
    dueDateInput.setFromDate(null);
    dueDateInput.setToDate(getMidnight().getTime());
    return (UserTasks) getList("userTaskListToday");
  }
  public UserTasks getMyTasksTomorrow() throws UIException
  {
    TaskFilter userTasksFilter = (TaskFilter) FilterManager.getInstance().getUserTasksFilter();
    DateRangeInput dueDateInput = (DateRangeInput) userTasksFilter.getInput("dueDate");
    Calendar calendar = getMidnight();
    calendar.add(Calendar.DATE, 1);
    dueDateInput.setFromDate(calendar.getTime());
    dueDateInput.setToDate(calendar.getTime());
    return (UserTasks) getList("userTaskListTomorrow");
  }
  public UserTasks getMyTasksFuture() throws UIException
  {
    TaskFilter userTasksFilter = (TaskFilter) FilterManager.getInstance().getUserTasksFilter();
    DateRangeInput dueDateInput = (DateRangeInput) userTasksFilter.getInput("dueDate");
    Calendar calendar = getMidnight();
    calendar.add(Calendar.DATE, 2);
    dueDateInput.setFromDate(calendar.getTime());
    dueDateInput.setToDate(null);
    return (UserTasks) getList("userTaskListFuture");
  }

  public Calendar getMidnight()
  {
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar;
  }

  public WorkgroupTasks getWorkgroupTasks() throws UIException
  {
    return (WorkgroupTasks) getList("workgroupTaskList");
  }
  public TaskNotes getTaskNotes() throws UIException
  {
    return (TaskNotes) getList("taskNotes");
  }
  public TaskAttachments getTaskAttachments() throws UIException
  {
    return (TaskAttachments) getList("taskAttachments");
  }
  public OrderNotes getOrderNotes() throws UIException
  {
    return (OrderNotes) getList("orderNotes");
  }
  public OrderAttachments getOrderAttachments() throws UIException
  {
    return (OrderAttachments) getList("orderAttachments");
  }
  public TaskHistory getTaskHistory() throws UIException
  {
    return (TaskHistory) getList("taskHistory");
  }
  public SubTaskList getSubTasks() throws UIException
  {
    return (SubTaskList) getList("subTaskList");
  }
  public OrderTaskList getOrderTasks() throws UIException
  {
    return (OrderTaskList) getList("orderTaskList");
  }
  public Workgroups getWorkgroups() throws UIException
  {
    return (Workgroups) getList("workgroupsList");
  }

  public Workasset getAssets() throws UIException
  {
    return (Workasset) getList("workassetsList");
  }

  public Users getUsers() throws UIException
  {
    return (Users) getList("usersList");
  }
  public TaskCategories getTaskCategoryList() throws UIException
  {
    return (TaskCategories) getList("taskCategoryList");
  }
  public ExternalEventsList getExternalEvents() throws UIException
  {
    return (ExternalEventsList) getList("externalEvents");
  }
  @Deprecated
  public Tasks getTaskList() throws UIException
  {
    return (Tasks) getList("taskList");
  }
  public Tasks getTaskTemplateList() throws UIException
  {
    return (Tasks) getList("taskTemplateList");
  }
  public Roles getRoles() throws UIException
  {
    return (Roles) getList("rolesList");
  }
   public TaskAttributes getTaskAttributeList() throws UIException
  {
    return (TaskAttributes) getList("taskAttributeList");
  }
  public TaskVariables getTaskVariableList() throws UIException
  {
    return (TaskVariables) getList("taskVariableList");
  }
  public TaskReportList getTaskReport() throws UIException
  {
    return (TaskReportList) getList("taskReport");
  }
  public UserReportList getUserReport() throws UIException
  {
    return (UserReportList) getList("userReport");
  }
  public TaskCategoryReportList getTaskCategoryReport() throws UIException
  {
    return (TaskCategoryReportList) getList("taskCategoryReport");
  }
  public AuditEventsList getAuditEvents() throws UIException
  {
    return (AuditEventsList) getList("auditEvents");
  }
  public ProcessInstances getProcessInstances() throws UIException
  {
    return (ProcessInstances) getList("processInstanceList");
  }

  /**
   * Looks up and returns an instance of a list based on its id in TaskView.xml.
   * Use search suggestion selected by user if it is selected
   * @param listId identifies the filter
   * @return an instance of a list associated with the user's session
   */
  public SortableList getList(String listId) throws UIException
  {
    SortableList sList = findList(listId);
    if (sList.isSearchable() && !sList.isPagedRetrieval())
      return filterListForSearchedItem(sList,listId);
    else
      return sList;
  }

  /**
   * Looks up and returns an instance of a list based on its id in TaskView.xml.
   * it would return all list items to display search suggestions.
   * @param listId identifies the filter
   * @return an instance of a list associated with the user's session
   */
  public SortableList getListForSuggestions(String listId) throws UIException
  {
    return findList(listId);
  }

  public void setList(String listId, SortableList list) throws UIException
  {
    _lists.put(listId, list);
  }

  protected SortableList findList(String listId) throws UIException
  {
    return findList(listId, getClass().getClassLoader());
  }

  protected SortableList findList(String listId, ClassLoader classLoader) throws UIException
  {
    SortableList sList = (SortableList) _lists.get(listId);

    if (sList == null)
    {
      sList = createList(listId, classLoader);
      sList.populate();
      Map<String,String> prefs = FacesVariableUtil.getCurrentUser().getAttributes();
      if (prefs.containsKey(listId+":groupByValue") && _groupBy.get(listId) == null)
        setGroupBy(listId, prefs.get(listId+":groupByValue"));
      sList.setGroupBy(_groupBy.get(listId));
      SortParameters sortParameters = _sortParameters.get(listId);
      if (sortParameters != null)
        sList.setSortParameters(sortParameters);
      sList.sort();
      _lists.put(listId, sList);
    }
    return sList;
  }

  public SortableList createList(String listId) throws UIException
  {
    return createList(listId, getClass().getClassLoader());
  }

  /**
   * Creates a new list instance without populating
   * @param listId the View UI identifier
   * @return the constructed list
   */
  public SortableList createList(String listId, ClassLoader classLoader) throws UIException
  {
    try
    {
      SortableList sList = null;
      ListUI listUI = ViewUI.getInstance().getListUI(listId);
      FilterManager filterManager = FilterManager.getInstance();
      String listModelName = listUI.getModel();
      if (FacesVariableUtil.isValueBindingExpression(listModelName)) // Dynamic List Model class - #{userDefects}
      {
        Object dynModelObj = FacesVariableUtil.evaluateExpression(listModelName);
        if (dynModelObj != null) {
          sList = (SortableList)dynModelObj;
          sList.setListUI(listUI);
        }
      }
      else
      {
        Class<?> toInstantiate = Class.forName(listModelName, true, classLoader);
        logger.debug("Creating new list instance: " + toInstantiate);
        Constructor<?> listCtor = toInstantiate.getConstructor(new Class[] {ListUI.class} );
        sList = (SortableList) listCtor.newInstance(new Object[] { listUI } );
      }
      if (listUI.getFilter() != null)
      {
        sList.setFilter(filterManager.getFilter(listUI.getFilter()));
      }
      sList.initialize();
      if (listUI.isColumnarFilters())
        filterManager.getFilter(listUI.getFilter()).setUserPrefrdColumns(sList.getColumnHeaders());
      return sList;
    }
    catch (Exception ex)
    {
      String msg = "Problem creating List: ID=" + listId;
      logger.severeException(msg, ex);
      throw new UIException(msg, ex);
    }
  }

  public void invalidate()
  {
    _lists = new HashMap<String, SortableList>();
  }

  public DataTable getDataTable(String listId)
  {
    return (DataTable) FacesVariableUtil.findComponentById(listId);
  }

  public void clearList(String listId)
  {
    _lists.remove(listId);
  }

  public void clearCurrentRows()
  {
    ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();

    // clear existing sessionMap values
    Map<String, Object> sessionMap = externalContext.getSessionMap();
    for (Iterator<String> iter = sessionMap.keySet().iterator(); iter.hasNext(); )
    {
      String key = iter.next().toString();
      if (key.startsWith("dataTableCurrentRow"))
        sessionMap.remove(key);
    }

    // preserve specifically-requested values
    Map<String, Object> requestMap = externalContext.getRequestMap();
    for (Iterator<String> iter = requestMap.keySet().iterator(); iter.hasNext(); )
    {
      String key = iter.next().toString();
      if (key.startsWith("dataTableCurrentRow"))
        sessionMap.put(key, requestMap.get(key));
    }
  }

  private SortableList filterListForSearchedItem(SortableList origList,String listId) throws UIException
  {
    SortableList filteredList = origList;
    ListSearch listSearch = (ListSearch) FacesVariableUtil.getValue(ListSearch.LIST_SEARCH);
    if (listSearch != null && !listSearch.isValueEmpty())
      filteredList = findFilteredList(listId, listSearch.getSearch(), origList);
    return filteredList;
  }

  private SortableList findFilteredList(String listId,Object searchkey,SortableList origList) throws UIException
  {
    SortableList filteredList = (SortableList) _lists.get(listId+"_"+searchkey);
    if (filteredList == null && origList != null)
    {
    	filteredList = createFilteredList(listId,(List<?>)origList.getItems().getWrappedData(),searchkey);
    	Set<String> keySet = _lists.keySet();
    	List<String> removeKeyList = new ArrayList<String>();
    	for (String key : keySet) {
    		if (key.startsWith(listId+"_")) {
    			removeKeyList.add(listId);
    		}
    	}
    	for (String removekey : removeKeyList) {
    		_lists.remove(removekey);
    	}
      _lists.put(listId+"_"+searchkey, filteredList);
    }
    return filteredList;
  }

  private SortableList createFilteredList (String listId,List<?> items,Object searchKey) throws UIException
  {
	  SortableList filteredList = createList(listId);
	  DataModel<ListItem> dataModel = new ListDataModel<ListItem>();
	  filteredList.setItems(dataModel);
	  List<ListItem> includedItemList = new ArrayList<ListItem>();
	  List<ItemUI> itemList = filteredList.getListUI().getColumns();
	  if(searchKey instanceof Long){
	    for (int i = 0; items!= null && i < items.size(); i++) {
	      ListItem listItem = (ListItem) items.get(i);
	      if(listItem.getId().equals(searchKey)){
            includedItemList.add(listItem);
            break;
          }
	    }
	  }else{
	    for (int i = 0; items!= null && i < items.size(); i++) {
	      ListItem listItem = (ListItem) items.get(i);
          for (ItemUI viewItem : itemList) {
            if(!viewItem.getName().isEmpty()){
                  Object columnValue = listItem.getAttributeValue(viewItem.getAttribute());
                    if (columnValue != null &&
                            (columnValue.toString().toLowerCase()).contains(searchKey.toString().toLowerCase())) {
                        includedItemList.add(listItem);
                        break;
                    }
            }
          }
	    }
	  }
	  dataModel.setWrappedData(includedItemList);
	  return filteredList;
  }
}
