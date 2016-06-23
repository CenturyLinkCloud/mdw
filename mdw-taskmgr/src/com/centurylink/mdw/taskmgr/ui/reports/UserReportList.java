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

public class UserReportList extends SortableList
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  
  public UserReportList(ListUI listUI)
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

      TaskInstanceReportVO[] taskInstanceReportVOs = taskMgr
          .getTaskInstanceReportVOs(TaskInstanceReportVO.REPORT_TYPE_USER_ID);
      return new ListDataModel<ListItem>(convertTaskInstanceReportVOs(taskInstanceReportVOs));
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }

  /**
   * Converts a collection of task instance report VOs retrieved from the workflow.
   *
   * @param taskInstances - collection retrieved from the workflow
   * @return list of ui data model items
   */
  protected List<ListItem> convertTaskInstanceReportVOs(TaskInstanceReportVO[] repVO)
  {
    List<ListItem> rowList = new ArrayList<ListItem>();

    for (int i = 0; i < repVO.length; i++)
    {
      TaskInstanceReportItemVO[] tiriVO = repVO[i].getTaskInstanceReportItems() ;
      for( int j = 0; j < tiriVO.length; j++ ) {
        FullReportInstance instance = new FullReportInstance(repVO[i].getEntityName() );
        instance.setItemState( tiriVO[j].getItemState() ) ;
        instance.setItemCount( tiriVO[j].getItemCount() ) ;
        rowList.add(instance);
      }
    }

    return rowList;
  }
}