/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.jsf.phase;

import java.net.URL;
import java.net.URLEncoder;

import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;
import com.centurylink.mdw.taskmgr.ui.process.FullProcessInstance;
import com.centurylink.mdw.taskmgr.ui.tasks.list.TaskListScopeActionController;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.util.RemoteLocator;

public class ProcessStartPhaseListener implements PhaseListener
{
  private static final long serialVersionUID = 1L;
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public void beforePhase(PhaseEvent event)
  {
    Object request = event.getFacesContext().getExternalContext().getRequest();
    if (request instanceof HttpServletRequest)
    {
      String path = event.getFacesContext().getExternalContext().getRequestServletPath();
      if (path != null && path.endsWith("start.jsf"))
      {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        if (httpServletRequest.getMethod().equalsIgnoreCase("get"))
        {
          String process = httpServletRequest.getParameter("process");
          if (process == null)
          {
            FacesVariableUtil.addMessage("Missing 'process' parameter.");
            event.getFacesContext().renderResponse();
            return;
          }
          String processVersion = httpServletRequest.getParameter("processVersion");
          try
          {
            Long processId = findProcessId(process, processVersion == null ? 0 : Integer.parseInt(processVersion));
            ProcessVO processVO = ProcessVOCache.getProcessVO(processId);
            String customStartPage = processVO.getAttribute(WorkAttributeConstant.PROCESS_START_PAGE);
            String customStartPageVersion = processVO.getAttribute(WorkAttributeConstant.PROCESS_START_PAGE_ASSET_VERSION);
            if (customStartPage != null)
            {
              FacesVariableUtil.setValue("customStartPage", customStartPage);
            }
            if (customStartPageVersion != null && WorkAttributeConstant.HTML5_RENDERING.equals(processVO.getRenderingEngine()))
            {
              FacesVariableUtil.setValue("customStartPageVersion", customStartPageVersion);
            }
          }
          catch (Exception ex)
          {
            logger.severeException(ex.getMessage(), ex);
            FacesVariableUtil.addMessage("Problem finding process: '" + process + "'");
            event.getFacesContext().renderResponse();
            return;
          }
        }
      }
    }

  }

  public PhaseId getPhaseId()
  {
    return PhaseId.RESTORE_VIEW;
  }

  public void afterPhase(PhaseEvent event)
  {
    Object request = event.getFacesContext().getExternalContext().getRequest();
    if (request instanceof HttpServletRequest)
    {
      String path = event.getFacesContext().getExternalContext().getRequestServletPath();
      if (path != null && path.endsWith("start.jsf"))
      {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        if (httpServletRequest.getMethod().equalsIgnoreCase("get"))
        {
          String process = httpServletRequest.getParameter("process");
          if (process == null)
          {
            FacesVariableUtil.addMessage("Missing 'process' parameter.");
            event.getFacesContext().renderResponse();
            return;
          }
          String processVersion = (httpServletRequest.getParameter("processVersion"));
          try
          {
            Long processId = findProcessId(process, processVersion == null ? 0 : Integer.parseInt(processVersion));
            ProcessVO processVO = ProcessVOCache.getProcessVO(processId);
            FullProcessInstance procInst = new FullProcessInstance(processVO);
            FacesVariableUtil.setValue("process", procInst);

            String customStartPage = processVO.getAttribute(WorkAttributeConstant.PROCESS_START_PAGE);
            String customStartPageVersion = processVO.getAttribute(WorkAttributeConstant.PROCESS_START_PAGE_ASSET_VERSION);
            if (customStartPage != null)
            {
              procInst.setStartPage(customStartPage);
              boolean isHubRequest = TaskListScopeActionController.getInstance().isMdwHubRequest();
              boolean isCompatRendering = processVO.isCompatibilityRendering();
              if (isHubRequest)
              {
                if (isCompatRendering)
                {
                  // redirect to process start page in Task Manager before initiating the session and redirecting to the custom page
                  FacesVariableUtil.navigate(new URL(ApplicationContext.getTaskManagerUrl() + "/start.jsf" + (httpServletRequest.getQueryString() == null ? "" : "?" + httpServletRequest.getQueryString())));
                }
                else
                {
                  FacesVariableUtil.setValue("pageName", customStartPage);
                  FacesVariableUtil.setValue(TaskAttributeConstant.CUSTOM_PAGE_ASSET_VERSION, customStartPageVersion);
                  FacesVariableUtil.navigate(new URL(ApplicationContext.getMdwHubUrl() + "/" + TaskAttributeConstant.PAGE_PATH + customStartPage));
                }
              }
              else
              {
                String page = URLEncoder.encode(customStartPage, "UTF-8");
                FacesVariableUtil.navigate(new URL(ApplicationContext.getTaskManagerUrl() + "/" + TaskAttributeConstant.PAGE_COMPATIBILITY_PATH + page));
              }
            }
            else
            {
              FacesVariableUtil.navigate("go_processStart");
            }
          }
          catch (Exception ex)
          {
            logger.severeException(ex.getMessage(), ex);
            FacesVariableUtil.addMessage("Problem in navigating to custom start page for process: '" + process + "'");
            event.getFacesContext().renderResponse();
            return;
          }
        }
      }
    }
  }

  public Long findProcessId(String processName) throws DataAccessException
  {
    return findProcessId(processName, 0);
  }

  public Long findProcessId(String processName, int version) throws DataAccessException
  {
    try
    {
	  EventManager eventMgr = RemoteLocator.getEventManager();
      return eventMgr.findProcessId(processName, version);
    }
    catch (Exception ex)
    {
      throw new DataAccessException(-1, ex.getMessage(), ex);
    }
  }
}
