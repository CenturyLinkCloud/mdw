/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui;

import java.util.Map;

import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.dataaccess.RuntimeDataAccess;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskIndexes;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.services.status.GlobalApplicationStatus;
import com.centurylink.mdw.services.task.TaskManagerAccess;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.tasks.FullTaskInstance;
import com.centurylink.mdw.taskmgr.ui.tasks.detail.TaskDetail;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.MDWDataAccess;
import com.centurylink.mdw.web.ui.UIDocument;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.model.MDWProcessInstance;
import com.centurylink.mdw.web.util.RemoteLocator;

public class InstanceActionController implements ActionListener
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  /**
   * Default behavior is simply to persist variable data held in the process instance
   * managed bean.  Override for additional functionality.
   */
  public void processAction(ActionEvent event) throws AbortProcessingException
  {
    try
    {
      TaskDetail taskDetail = DetailManager.getInstance().getTaskDetail();
      saveInstanceVariables(taskDetail.getProcessInstanceId());
    }
    catch (UIException ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage(ex.getMessage());
      throw new AbortProcessingException(ex.getMessage());
    }

  }

  /**
   * Saves variable values for a process instance.
   * @param processInstanceId
   */
  public void saveInstanceVariables(Long processInstanceId) throws UIException
  {
    try
    {
      // update directly-accessed variables stored in process managed bean
      MDWProcessInstance mdwProcessInstance = (MDWProcessInstance) FacesVariableUtil.getValue("process");
      if (mdwProcessInstance != null && mdwProcessInstance.hasDirtyVariables())
      {
        EventManager eventMgr = RemoteLocator.getEventManager();
        RuntimeDataAccess runtimeDataAccess = ((MDWDataAccess)FacesVariableUtil.getValue("dataAccess")).getRuntimeDataAccess();

        // TODO: re-retrieving the process instance seems horribly inefficient
        ProcessInstanceVO processInstance = runtimeDataAccess.getProcessInstanceAll(mdwProcessInstance.getId());
        ProcessVO processVO = ProcessVOCache.getProcessVO(processInstance.getProcessId());
        for (String var : mdwProcessInstance.getDirtyVariables())
        {
          Object value = mdwProcessInstance.getVariables().get(var);
          VariableVO vo = processVO.getVariable(var);
          // variable type is StringDocument(editable)
          if (vo != null && value != null
              && VariableTranslator.isDocumentReferenceVariable(null, vo.getVariableType())
              && value instanceof String && !((String) value).startsWith("DOCUMENT"))
          {
            VariableInstanceInfo varsInst = eventMgr.getVariableInstance(processInstance.getId(), var);
            if (varsInst == null || !varsInst.isDocument())
            {
              if (!((String)value).isEmpty()) {
                Long docid = eventMgr.createDocument(vo.getVariableType(), processInstance.getId(), OwnerType.DOCUMENT, processInstance.getOwnerId(), null, null, value);
                eventMgr.setVariableInstance(processInstance.getId(), var, new DocumentReference(docid, null));
              }
            }
            else
            {
              eventMgr.updateDocumentContent(varsInst.getDocumentId(), value, varsInst.getType());
              mdwProcessInstance.setVariableValue(var, new DocumentReference(varsInst.getDocumentId(), null));
            }
          }
          else if (value instanceof DocumentReference)
          {
            DocumentReference docRef = (DocumentReference) value;
            UIDocument uiDoc = mdwProcessInstance.getDocument(docRef);
            eventMgr.updateDocumentContent(docRef.getDocumentId(), uiDoc.getObject(), uiDoc.getType());
          }
          else
          {
            eventMgr.setVariableInstance(processInstance.getId(), var, value);
          }
        }
      }
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public void saveIndexValues(FullTaskInstance taskInstance, Long processInstanceId, FormDataDocument formdatadoc) throws UIException, DataAccessException
  {
    TaskVO taskVO = TaskTemplateCache.getTaskTemplate(taskInstance.getTaskId());
    TaskManager taskMgr = RemoteLocator.getTaskManager();
    Map<String, String> indices = taskMgr.collectIndices(taskVO.getTaskId(), processInstanceId, null);
    if (indices != null && !indices.isEmpty())
    {
      if (taskInstance.isDetailOnly())
      {
        if (GlobalApplicationStatus.getInstance().getSystemStatusMap().isEmpty()
            || GlobalApplicationStatus.ONLINE.equals(GlobalApplicationStatus.getInstance()
                .getSystemStatusMap().get(GlobalApplicationStatus.SUMMARY_TASK_APPNAME)))
        {
          TaskIndexes taskIndexes = new TaskIndexes(new Long(taskInstance.getAssociatedInstanceId()), indices);
          try
          {
            StatusMessage response = TaskManagerAccess.getInstance().notifySummaryTaskManager("UpdateTaskIndexes", taskIndexes);
            if (!response.isSuccess())
              logger.warn("Failed to create/update remote task indices" + taskInstance.getInstanceId());
          }
          catch (Exception ex)
          {
            logger.warn("Failed to create/update remote task indices" + taskInstance.getInstanceId());
          }
        }
      }
      else
      {
        taskMgr.updateTaskIndices(taskInstance.getInstanceId(), indices);
      }
    }
  }

}
