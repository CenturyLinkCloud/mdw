/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.user;

import java.util.List;

import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.taskmgr.ui.filter.Filter;
import com.centurylink.mdw.taskmgr.ui.filter.FilterManager;
import com.centurylink.mdw.taskmgr.ui.list.ListManager;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;

public class UserPreferences
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public Filter getCurrentFilter() throws UIException
  {
    String filterId = (String)FacesVariableUtil.getValue("filterId");
    return FilterManager.getInstance().getFilter(filterId);
  }

  public String saveFilterDefaults() throws UIException
  {
    String filterId = (String) FacesVariableUtil.getValue("filterId");
    if (getCurrentList().isColumnarFilters())
    {
      if (isSaveFilterValues())
      {
        getCurrentFilter().saveFilterPrefs();
      }
    }
    else if (filterId != null)
    {
      getCurrentFilter().saveFilterPrefs();
    }

    return null;
  }

  public String saveListPreferences() throws UIException
  {
    saveFilterDefaults();
    String listId = (String) FacesVariableUtil.getValue("mdwListId");
    if (listId == null)
      return null;
    if (isSaveDisplayRowsValue() || isSaveGroupByValue())
    {
      getCurrentList().saveListMenuPrefs(isSaveGroupByValue(), isSaveDisplayRowsValue(),
          ListManager.getInstance().getGroupBy(listId), ListManager.getInstance().getDisplayRows(listId));
    }
    return null;
  }

  public String resetFilterDefaults() throws UIException
  {
    getCurrentFilter().resetFilterPrefs();
    FilterManager.getInstance().invalidate();
    ListManager.getInstance().invalidate();
    return null;
  }

  public String resetListPrefs() throws UIException
  {
    String result = getCurrentList().resetListPrefs();
    ListManager.getInstance().invalidate();
    return result;
  }

  /**
   * To reset filter defaults and column preferences (both)
   * @return
   * @throws UIException
   */
  public String resetPreferences() throws UIException
  {
    if (getCurrentList().isColumnarFilters())
    {
      resetFilterDefaults();
    }
    resetListPrefs();
    return null;
  }

  public List<SelectItem> getSelectedColumns()
  {
    SortableList currentList = getCurrentList();
    if (currentList == null)
      return null;
    return currentList.getSelectedColumns();
  }

  public List<SelectItem> getUnselectedColumns()
  {
    SortableList currentList = getCurrentList();
    if (currentList == null)
      return null;
    return getCurrentList().getUnselectedColumns();
  }

  public SortableList getCurrentList()
  {
    try
    {
      String listId = (String)FacesVariableUtil.getValue("mdwListId");
      if (listId == null)
        return null;
      return ListManager.getInstance().getList(listId);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }

  public void selectedColumnsChanged(ValueChangeEvent event)
  {
    getCurrentList().selectedColumnsChanged(event);

    ListManager listManager = ListManager.getInstance();
    listManager.invalidate();
    listManager.clearCurrentRows();
  }

  public void setSelectedColumns(List<SelectItem> selected)
  {
    // jsf managed bean setter
  }

  public void setUnselectedColumns(List<SelectItem> unselected)
  {
    // jsf managed bean setter
  }

  private boolean saveFilterValues;
  private boolean saveGroupByValue;
  private boolean saveDisplayRowsValue;

  public boolean isSaveFilterValues()
  {
    return saveFilterValues;
  }

  public void setSaveFilterValues(boolean saveFilterValues)
  {
    this.saveFilterValues = saveFilterValues;
  }

  public boolean isSaveGroupByValue()
  {
    return saveGroupByValue;
  }

  public void setSaveGroupByValue(boolean saveGroupByValue)
  {
    this.saveGroupByValue = saveGroupByValue;
  }

  public boolean isSaveDisplayRowsValue()
  {
    return saveDisplayRowsValue;
  }

  public void setSaveDisplayRowsValue(boolean saveDisplayRowsValue)
  {
    this.saveDisplayRowsValue = saveDisplayRowsValue;
  }

}
