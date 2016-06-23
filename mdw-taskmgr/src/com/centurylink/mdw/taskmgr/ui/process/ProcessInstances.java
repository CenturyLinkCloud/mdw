/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.process;


import javax.faces.model.DataModel;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.taskmgr.ui.data.RowConverter;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

public class ProcessInstances extends SortableList
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public ProcessInstances(ListUI listUI)
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
      ProcessFilter filter = (ProcessFilter) getFilter();
      ProcessInstancesDataModel pagedDataModel = new ProcessInstancesDataModel(getListUI(), filter);

      // set the converter for ProcessInstanceVO
      pagedDataModel.setRowConverter(new RowConverter()
        {
          public Object convertRow(Object o)
          {
            ProcessInstanceVO ProcessInstanceVO = (ProcessInstanceVO) o;
            ProcessInstanceItem item = new ProcessInstanceItem(ProcessInstanceVO);
            return item;
          }
        });

      return pagedDataModel;
    }
    catch (Exception ex)
    {
      String msg = "Problem retrieving Process Instances.";
      logger.severeException(msg, ex);
      throw new UIException(msg, ex);
    }
  }
}
