/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.process;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;
import com.centurylink.mdw.taskmgr.ui.tasks.list.TaskListScopeActionController;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.model.MDWProcessInstance;

/**
 * Extends the standard process launch action controller to automatically
 * setting Master Request ID (generated) and Process Owner ID (start page id).
 */
public class ProcessStartActionController extends ProcessLaunchActionController
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  @Override
  public void processAction(ActionEvent actionEvent) throws AbortProcessingException
  {
    FacesVariableUtil.setValue("processStartActionController", this);
    FullProcessInstance processInst = (FullProcessInstance) FacesVariableUtil.getValue("process");

    try
    {
      String startPageName = processInst.getStartPage();
      if (startPageName == null)
      {
        processInst.setOwner("Process Start");
        processInst.setOwnerId(processInst.getProcessId());
      }
      else
      {
        processInst.setOwner("Process Start Page");
        processInst.setOwnerId(RuleSetCache.getRuleSet(startPageName, RuleSetVO.FACELET).getId());
      }

      if (processInst.getMasterRequestId() == null || processInst.getMasterRequestId().trim().length() == 0)
      {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd-HHmmss");
        processInst.setMasterRequestId(FacesVariableUtil.getCurrentUser().getCuid() + "~" + sdf.format(new Date()));
      }

      if (processInst.getStartPage() == null)
        launchProcess(processInst);
      else
        launchProcess((MDWProcessInstance)processInst);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage(ex.toString());
    }
  }

  // TODO : Don, help me finding the purpose of my existence - What's this for
  public String navOutcome()
  {
    FullProcessInstance processInst = (FullProcessInstance) FacesVariableUtil.getValue("process");

    try
    {
      boolean isHubRequest = TaskListScopeActionController.getInstance().isMdwHubRequest();
      ProcessVO proc = ProcessVOCache.getProcessVO(processInst.getProcessId());
      boolean isCompatRendering = proc.isCompatibilityRendering();
      String page = URLEncoder.encode(processInst.getStartPage(), "UTF-8");
      if (!isHubRequest || isCompatRendering)
        return ApplicationContext.getTaskManagerUrl() + "/" + TaskAttributeConstant.PAGE_COMPATIBILITY_PATH + page;
      else
        return ApplicationContext.getMdwHubUrl() + "/" + TaskAttributeConstant.PAGE_PATH + page;
    }
    catch (UnsupportedEncodingException ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }
}
