/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.process;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.taskmgr.ui.detail.Detail;
import com.centurylink.mdw.taskmgr.ui.detail.ModelWrapper;
import com.centurylink.mdw.taskmgr.ui.layout.DetailUI;
import com.centurylink.mdw.web.ui.UIException;

public class ProcessDetail extends Detail
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public String getMasterRequestId()
  {
    return ((ProcessInstanceVO)getModelWrapper().getWrappedInstance()).getMasterRequestId();
  }

  public String getOrderId()
  {
    return getMasterRequestId();
  }

  public ProcessDetail(DetailUI detailUI)
  {
    super(detailUI);
  }

  public String getInstanceId()
  {
    if (getModelWrapper() == null)
      return null;

    return getModelWrapper().getWrappedId();
  }

  /**
   * Default implementation does nothing but provide the instanceId.
   * TODO: implement retrieval
   */
  protected void retrieveInstance(String instanceId) throws UIException
  {
    EventManager eventMgr = ServiceLocator.getEventManager();
    try
    {
      final ProcessInstanceVO procInstVO = eventMgr.getProcessInstance(Long.parseLong(instanceId));

      setModelWrapper(new ModelWrapper()
      {
        public Object getWrappedInstance()
        {
          return procInstVO;
        }

        public String getWrappedId()
        {
          return String.valueOf(procInstVO.getId());
        }
      });
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      throw new UIException(ex.getMessage(), ex);
    }
  }
}
