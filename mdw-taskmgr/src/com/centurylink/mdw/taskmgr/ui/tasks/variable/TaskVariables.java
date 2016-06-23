/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.variable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;
import com.centurylink.mdw.taskmgr.ui.Lister;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.layout.ViewUI;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.taskmgr.ui.tasks.TaskItem;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;

public class TaskVariables extends SortableList implements Lister
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public TaskVariables(ListUI listUI)
  {
    super(listUI);
  }

  /**
   *  Needs a no-arg constructor since Users is a dropdown lister
   *  in addition to being a SortableList implementation.
   */
  public TaskVariables() throws UIException
  {
    super(ViewUI.getInstance().getListUI("taskVariableList"));
  }

  protected DataModel<ListItem> retrieveItems()
  {
    List<ListItem> list = new ArrayList<ListItem>();
    try
    {
      TaskItem taskItem = ((TaskItem) FacesVariableUtil.getValue("taskItem"));
      if (taskItem.getId() != null)
      {
        TaskManager varMgr = RemoteLocator.getTaskManager();
        List<?> varVOs = varMgr.getVariablesForTask(taskItem.getId());
        for (Iterator<?> iter = varVOs.iterator(); iter.hasNext(); )
        {
          list.add(new TaskVariableItem((VariableVO)iter.next()));
        }
      }
      return new ListDataModel<ListItem>(list);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }

  /**
   * Retrieve the variables mapped to a process
   * @param processId
   * @return list of variable VOs
   */
  public static List<VariableVO> getProcessVariables(Long processId)
  {
    List<VariableVO> processVariables = new ArrayList<VariableVO>();
    try
    {
      if (processId != null)
      {
    	ProcessVO procdef = ProcessVOCache.getProcessVO(processId);
        processVariables = procdef.getVariables();
      }
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
    return processVariables;
  }

  /**
   * Get a list of SelectItems populated from an array of task categories.
   *
   * @param categories the categories VOs to include in the list
   * @param firstItem the message for an optional first item (null for none)
   * @return list of SelectItems
   */
  public static List<SelectItem> getVariableSelectItems(List<VariableVO> vars, String firstItem)
  {
    List<SelectItem> selectItems = new ArrayList<SelectItem>();
    if (vars != null && vars.size() > 0)
    {
      if (firstItem != null)
      {
        selectItems.add(new SelectItem("0", firstItem));
      }
      for (VariableVO varVO : vars)
      {
        selectItems.add(new SelectItem(varVO.getVariableId().toString(), varVO.getVariableName()));
      }
    }

    return selectItems;
  }

  /**
   * @see com.centurylink.mdw.taskmgr.ui.Lister#list(java.lang.String)
   */
  public List<SelectItem> list()
  {
    return new ArrayList<SelectItem>();
  }

}
