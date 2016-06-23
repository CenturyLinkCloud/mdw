/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.list;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.model.DataModel;
import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.data.task.TaskAction;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.taskmgr.ui.data.PagedListDataModel;
import com.centurylink.mdw.taskmgr.ui.data.RowConverter;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.taskmgr.ui.tasks.FullTaskInstance;
import com.centurylink.mdw.taskmgr.ui.tasks.action.AllowableAction;
import com.centurylink.mdw.taskmgr.ui.tasks.action.TaskActions;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

/**
 * Handles paginated responses for workgroup-level tasks.
 */
public class WorkgroupTasks extends SortableList
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public WorkgroupTasks(ListUI listUI)
  {
    super(listUI);
  }

  /**
   * @see com.centurylink.mdw.taskmgr.ui.list.SortableList#retrieveItems()
   */
  public DataModel<ListItem> retrieveItems() throws UIException
  {
    try
    {
      PagedListDataModel pagedDataModel = new WorkgroupTasksDataModel(getListUI(), getFilter(), getUserPreferredDbColumns());

      // set the converter for FullTaskInstances
      pagedDataModel.setRowConverter(new RowConverter()
        {
          public Object convertRow(Object o)
          {
            TaskInstanceVO taskInstance = (TaskInstanceVO) o;
            FullTaskInstance instance = new FullTaskInstance(taskInstance);
            return instance;
          }
        });

      return pagedDataModel;
    }
    catch (Exception ex)
    {
      String msg = "Problem retrieving Workgroup Tasks.";
      logger.severeException(msg, ex);
      throw new UIException(msg, ex);
    }
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
    for (AllowableAction aa : TaskActions.getAllowableBulkActions())
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
