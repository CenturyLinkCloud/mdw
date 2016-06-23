/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.phase;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.email.TaskEmailModel;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.dataaccess.RuntimeDataAccess;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessRuntimeContext;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskRuntimeContext;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.services.task.TaskManagerAccess;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.MDWDataAccess;
import com.centurylink.mdw.web.ui.model.MDWProcessInstance;

public class NoticeTemplatePhaseListener implements PhaseListener
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public PhaseId getPhaseId()
  {
    return PhaseId.RESTORE_VIEW;
  }

  private ProcessInstanceVO processInstanceInfo;

  /**
   * If request is for a templated notice, populate the values that
   * can be referenced in the template's EL expressions.
   */
  public void beforePhase(PhaseEvent event)
  {
    FacesContext facesContext = event.getFacesContext();
    ExternalContext externalContext = facesContext.getExternalContext();
    Object request = externalContext.getRequest();
    if (request instanceof HttpServletRequest)
    {
      HttpServletRequest httpServletRequest = (HttpServletRequest) request;
      if (httpServletRequest.getRequestURI().endsWith("notice.jsf"))
      {
        // this is a request for a notice based on a template
        Map<String,String> params = externalContext.getRequestParameterMap();
        MDWDataAccess dataAccess = (MDWDataAccess) FacesVariableUtil.getValue("dataAccess");
        try
        {
          Long processInstanceId = new Long(params.get("processInstanceId"));
          RuntimeDataAccess runtimeDataAccess = dataAccess.getRuntimeDataAccess();
          processInstanceInfo = runtimeDataAccess.getProcessInstanceAll(processInstanceId);
          boolean isSubProc = processInstanceInfo.isNewEmbedded() ? true:false;
          if (isSubProc) {
            processInstanceInfo = runtimeDataAccess.getProcessInstanceAll(processInstanceInfo.getOwnerId());
          }

          String taskInstanceIdParam = params.get("taskInstanceId");
          Map<String,Object> variablesProvider = new VariablesProvider(processInstanceInfo, runtimeDataAccess);
          MDWProcessInstance mdwInstance = new MDWProcessInstance();
          mdwInstance.setProcessInstanceVO(processInstanceInfo);
          if (taskInstanceIdParam != null)
          {
            Long taskInstanceId = new Long(taskInstanceIdParam);
            String taskInstanceJson = params.get("taskInstanceJson");
            JSONObject json = new JSONObject(URLDecoder.decode(taskInstanceJson, "UTF-8"));
            TaskEmailModel taskEmailModel = new TaskEmailModel(json, variablesProvider);
            if (json.has("taskId"))
            {
              TaskVO taskVO = TaskTemplateCache.getTaskTemplate(json.getLong("taskId"));
              if (taskVO != null)
                taskEmailModel.setDescription(taskVO.getComment());
            }
            String userId = params.get("userId");
            if (userId != null)
            {
              String oneClickTaskManagerUrl = TaskManagerAccess.getInstance().findOneClickTaskManagerUrl();
              taskEmailModel.setTaskActionUrl(oneClickTaskManagerUrl + "/facelets/tasks/taskAction.jsf?taskInstanceId=" + taskInstanceId + "&mdw.UserId=" + URLEncoder.encode(userId, "UTF-8"));
            }
            FacesVariableUtil.setValue("context", this.getTaskRunTimeContext(processInstanceInfo, taskEmailModel.getTaskInstance(), variablesProvider));
          }
          else {
            FacesVariableUtil.setValue("context", this.getProcessRunTimeContext(processInstanceInfo, variablesProvider));
          }
          FacesVariableUtil.setValue("process", mdwInstance);
          FacesVariableUtil.setValue("variables", variablesProvider);
          // no longer supporting compatibility, but give an error message
          FacesVariableUtil.setValue("masterRequestId", "ERROR: use context.masterRequestId");
          FacesVariableUtil.setValue("mdwWebUrl", "ERROR: use context.mdwWebUrl");
          FacesVariableUtil.setValue("mdwTaskManagerUrl", "ERROR: use context.taskManagerUrl");
          FacesVariableUtil.setValue("processInstanceId", "ERROR: use context.processInstanceId");
          FacesVariableUtil.setValue("processName", "ERROR: user context.process.processName");
        }
        catch (Exception ex)
        {
          logger.severeException(ex.getMessage(), ex);
        }
      }
    }
  }

  public void afterPhase(PhaseEvent event)
  {
    // do nothing
  }


  private class VariablesProvider extends HashMap<String,Object>
  {
    private static final long serialVersionUID = 1L;

    private ProcessInstanceVO processInstance;
    private RuntimeDataAccess runtimeDataAccess;

    public VariablesProvider(ProcessInstanceVO processInstance, RuntimeDataAccess runtimeDataAccess)
    {
      this.processInstance = processInstance;
      this.runtimeDataAccess = runtimeDataAccess;
    }

    @Override
    public Object get(Object key) {
      VariableInstanceInfo varInst = processInstance.getVariable(key.toString());
      Object value = varInst == null ? null : varInst.getData();
      if (value instanceof DocumentReference)
      {
        try
        {
          DocumentVO docVO = runtimeDataAccess.getDocument(((DocumentReference)value).getDocumentId());
          value = docVO.getObject(docVO.getDocumentType());
        }
        catch (DataAccessException ex)
        {
          logger.severeException(ex.getMessage(), ex);
        }
      }
      return value;
    }
  }

  public Object getProcessRunTimeContext(ProcessInstanceVO procInstVO, Map<String, Object> variablesProvider)
  {
      ProcessRuntimeContext pRunTime = null;
      try
      {
        Long processId = procInstVO.getProcessId();
        ProcessVO processVO = ProcessVOCache.getProcessVO(processId);
        PackageVO packageVO = PackageVOCache.getProcessPackage(processId);
        processVO = ProcessVOCache.getProcessVO(processId);
        pRunTime = new ProcessRuntimeContext(packageVO, processVO, procInstVO, variablesProvider);
      }
      catch (Exception e)
      {
        logger.severeException("Failed to get Task Runtime context" + e.getMessage(), e);
      }
      return pRunTime;
  }

  public Object getTaskRunTimeContext(ProcessInstanceVO procInstVO, TaskInstanceVO taskInstanceVO, Map<String, Object> variablesProvider) {
      TaskRuntimeContext taskRunTime = null;
      try {
          TaskVO taskVO = TaskTemplateCache.getTaskTemplate(taskInstanceVO.getTaskId());  //Can Task id be null, Do we need TaskVO ? Ask Don? If not then I can remove it from constructor
          TaskManager taskManager = ServiceLocator.getTaskManager();
          TaskInstanceVO taskInst = taskManager.getTaskInstance(taskInstanceVO.getTaskInstanceId());
          taskManager.getTaskInstanceAdditionalInfo(taskInst);
          Long processId = procInstVO.getProcessId();
          ProcessVO processVO = ProcessVOCache.getProcessVO(processId);
          PackageVO packageVO = PackageVOCache.getProcessPackage(processId);
          processVO = ProcessVOCache.getProcessVO(processId);
          taskRunTime = new TaskRuntimeContext(packageVO, processVO, procInstVO, taskVO, taskInst, variablesProvider);
      } catch (Exception e) {
          logger.severeException("Failed to get Task Runtime context" + e.getMessage(), e);
      }
      return taskRunTime;
  }
}