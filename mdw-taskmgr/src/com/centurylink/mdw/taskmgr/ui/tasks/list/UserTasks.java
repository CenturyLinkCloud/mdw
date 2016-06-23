/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.list;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.data.task.TaskAction;
import com.centurylink.mdw.model.data.task.TaskState;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.taskmgr.ui.tasks.FullTaskInstance;
import com.centurylink.mdw.taskmgr.ui.tasks.action.AllowableAction;
import com.centurylink.mdw.taskmgr.ui.tasks.action.TaskActions;
import com.centurylink.mdw.taskmgr.ui.tasks.filter.TaskFilter;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;

public class UserTasks extends SortableList
{
  protected static StandardLogger logger = LoggerUtil.getStandardLogger();

  public UserTasks(ListUI listUI)
  {
    super(listUI);
  }

  protected DataModel<ListItem> retrieveItems() throws UIException
  {
    Long userId = FacesVariableUtil.getCurrentUser().getUserId();

    try
    {
      TaskFilter taskFilter = (TaskFilter) getFilter();
      // allow override of userId
      if (taskFilter != null && taskFilter.getUserName() != null)
        userId = UserGroupCache.getUser(taskFilter.getUserName()).getId();

      TaskManager taskMgr = RemoteLocator.getTaskManager();

      Map<String,String> criteriaMap = getFilter() == null ? new HashMap<String,String>() : ((com.centurylink.mdw.taskmgr.ui.filter.Filter)getFilter()).buildCriteriaMap();
      Map<String,String> specialCriteria = getFilter() == null ? null : getFilter().getSpecialCriteria();
      Map<String,String> indexCriteria = getFilter() == null ? null : getFilter().getIndexCriteria();

      if (!criteriaMap.containsKey("stateCode"))
        criteriaMap.put("stateCode", " != " + TaskState.STATE_CLOSED.toString());

      TaskInstanceVO[] taskInstances = taskMgr.getClaimedTaskInstanceVOs(userId, criteriaMap, getSpecialColumns(), specialCriteria, getIndexColumns(), indexCriteria);
      return new ListDataModel<ListItem>(convertTaskInstances(taskInstances));
    }
    catch (Exception ex)
    {
      String msg = "Problem retrieving User Tasks for userId: " + userId + ".";
      logger.severeException(msg, ex);
      throw new UIException(msg, ex);
    }

  }

  /**
   * Converts a collection of TaskInstanceVOs retrieved from the workflow.
   *
   * @param taskInstances array of VOs
   * @return list of ui data model items
   */
  public List<ListItem> convertTaskInstances(TaskInstanceVO[] taskInstances)
  {
    List<ListItem> rowList = new ArrayList<ListItem>();

    for (int i = 0; i < taskInstances.length; i++)
    {
      TaskInstanceVO taskInstance = taskInstances[i];
      FullTaskInstance instance = new FullTaskInstance(taskInstance);
      rowList.add(instance);
    }

    return rowList;
  }

  /**
   * Get a list of SelectItems populated from the current user's allowable
   * TaskActions for a bulk operation dropdown.
   *
   * @return list of SelectItems
   */
  public List<SelectItem> getTaskActionSelectItems()
  {
    return TaskActions.buildSelectItemList(getTaskActions());
  }

  public List<TaskAction> getTaskActions()
  {
    List<TaskAction> bulkActions = new ArrayList<TaskAction>();
    for (AllowableAction aa : TaskActions.getAssignedBulkActions())
    {
      TaskAction taskAction = new TaskAction();
      taskAction.setTaskActionName(aa.getName());
      taskAction.setAlias(aa.getAlias());
      taskAction.setRequireComment(aa.isRequireComment());

      if (!bulkActions.contains(taskAction))
      {
        bulkActions.add(taskAction);
      }
    }

    Collections.sort(bulkActions);
    return bulkActions;
  }

  public UIComponent getTaskActionMenu() throws UIException
  {
    // avoid classloader dependency on RichFaces 3.3
    try
    {
      Class<?> toInstantiate = Class.forName("com.centurylink.mdw.taskmgr.ui.tasks.action.TaskActionMenu");
      Constructor<?> ctor = toInstantiate.getConstructor(new Class[] {String.class, List.class} );
      return (UIComponent) ctor.newInstance(new Object[] { getId(), getTaskActions() } );
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
    // return new TaskActionMenu(getId(), getTaskActions());
  }

  public void setTaskActionMenu(UIComponent menu)
  {
    // does nothing; required by JSF binding
  }

}