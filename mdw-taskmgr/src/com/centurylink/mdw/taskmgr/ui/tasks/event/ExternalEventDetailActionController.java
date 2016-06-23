/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.event;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.web.ui.UIException;

public class ExternalEventDetailActionController
{
  /**
   * Use DetailManager to populate the external event detail based on the 
   * currently-selected task instance id. 
   */
  public void setTaskInstanceId(String taskInstanceId) throws UIException
  {
    DetailManager.getInstance().setExternalEventDetail(OwnerType.PROCESS_INSTANCE, DetailManager.getInstance().getTaskDetail().getProcessInstanceId().toString());
  }

}
