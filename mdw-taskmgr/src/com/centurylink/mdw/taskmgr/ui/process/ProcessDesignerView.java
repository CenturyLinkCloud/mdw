/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.process;

import java.rmi.RemoteException;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.tasks.FullTaskInstance;
import com.centurylink.mdw.taskmgr.ui.tasks.detail.TaskDetail;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.util.RemoteLocator;

public class ProcessDesignerView
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public String launchDesigner() throws UIException
  {
    DetailManager detailManager = DetailManager.getInstance();
    TaskDetail taskDetail = detailManager.getTaskDetail();
    return launchDesigner(taskDetail.getFullTaskInstance());
  }

  public String launchDesigner(FullTaskInstance taskInstance) throws UIException
  {
    try
    {
      ProcessInstanceVO pi = getProcessInstance(taskInstance.getProcessInstanceId());
      ProcessVO processVO = ProcessVOCache.getProcessVO(pi.getProcessId());
      if (processVO.isEmbeddedProcess() || pi.isNewEmbedded())
      {
        // pi is the fallout process - get its parent
        pi = getProcessInstance(pi.getOwnerId());
      }
      if (pi == null)
      {
        throw new UIException("Failed to locate the ProcessInstance for TaskInstanceId:" + taskInstance.getInstanceId());
      }

      return launchDesigner(pi.getId());
    }
    catch (RemoteException ex)
    {
      logger.severeException(ex.getMessage(), ex);
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public String launchDesigner(Long processInstanceId) throws UIException
  {
    // TODO launch designer RCP locally with appropriate params
    return null;
  }

  private ProcessInstanceVO getProcessInstance(Long processInstanceId) throws RemoteException
  {
    try
    {
      EventManager eventManager = RemoteLocator.getEventManager();
      return eventManager.getProcessInstance(processInstanceId);
    }
    catch (Exception e)
    {
      logger.severeException(e.getMessage(), e);
      throw new RemoteException("Unable to get Variable information of the process instance", e);
    }
  }
}
