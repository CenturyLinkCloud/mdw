/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.reports;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.task.TaskInstanceReportVO;
import com.centurylink.mdw.model.value.task.TaskInstanceReportVO.TaskInstanceReportItemVO;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;

public class TaskReportList extends SortableList
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  
  public TaskReportList(ListUI listUI)
  {
    super(listUI);
  }

  /**
   * @see com.centurylink.mdw.taskmgr.ui.list.SortableList#retrieveItems()
   */
  protected DataModel<ListItem> retrieveItems()
  {
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();

      TaskInstanceReportVO[] taskInstanceReports = taskMgr
          .getTaskInstanceReportVOs(TaskInstanceReportVO.REPORT_TYPE_TASK_NAME);
      return new ListDataModel<ListItem>(convertTaskInstanceReportVOs(taskInstanceReports));
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }

  /**
   * Converts a collection of task instances report VOs retrieved from the workflow.
   *
   * @param taskInstances - collection retrieved from the workflow
   * @return list of ui data model items
   */
  protected List<ListItem> convertTaskInstanceReportVOs(TaskInstanceReportVO[] tiReportVOs )
  {
    List<ListItem> rowList = new ArrayList<ListItem>();

    for (int i = 0; i < tiReportVOs.length; i++)
    {
      TaskInstanceReportItemVO[] tiriVO = tiReportVOs[i].getTaskInstanceReportItems();
      for (int j = 0; j < tiriVO.length; j++)
      {
        FullReportInstance instance = new FullReportInstance(tiReportVOs[i].getEntityName());
        instance.setItemState(tiriVO[j].getItemState());
        instance.setItemCount(tiriVO[j].getItemCount());
        rowList.add(instance);
      }
    }

    return rowList;
  }
}