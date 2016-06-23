/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.detail;

import static com.centurylink.mdw.common.constant.TaskAttributeConstant.COMMENTS;
import static com.centurylink.mdw.common.constant.TaskAttributeConstant.DUE_DATE;
import static com.centurylink.mdw.common.constant.TaskAttributeConstant.PRIORITY;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.exception.ServiceLocatorException;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.variable.VariableInstanceVO;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.TaskException;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.services.task.TaskManagerAccess;
import com.centurylink.mdw.taskmgr.ui.InstanceActionController;
import com.centurylink.mdw.taskmgr.ui.detail.DetailItem;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.detail.InstanceDataItem;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * Handles the Save action for manual tasks.
 */
public class TaskInstanceActionController extends InstanceActionController implements ActionListener
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public void processAction(ActionEvent event) throws AbortProcessingException
  {
    try
    {
      Map<String,Object> changesMap = new HashMap<String,Object>();
      String cuid = FacesVariableUtil.getCurrentUser().getCuid();

      TaskDetail taskDetail = DetailManager.getInstance().getTaskDetail();
      TaskInstanceVO ti = taskDetail.getFullTaskInstance().getTaskInstance();

      // due date , priority and comments
      TaskInstanceVO oldTi = RemoteLocator.getTaskManager().getTaskInstance(ti.getTaskInstanceId());
      Date oldDueDate = oldTi.getDueDate() == null ? null : new Date(oldTi.getDueDate().getTime());
      Date dueDate = taskDetail.getFullTaskInstance().getDueDate();
      Calendar oldDueDateCal = Calendar.getInstance();
      if (oldDueDate != null)
      {
        // adjust for comparison
        oldDueDateCal.setTime(oldDueDate);
        if (dueDate != null)
        {
          // disregard seconds
          Calendar dueDateCal = Calendar.getInstance();
          dueDateCal.setTime(dueDate);
          oldDueDateCal.set(Calendar.SECOND, dueDateCal.get(Calendar.SECOND));
        }
        oldDueDate = oldDueDateCal.getTime();
      }
      boolean dueDateChanged = oldDueDate == null ? dueDate != null : !oldDueDate.equals(dueDate);
      String oldComments = oldTi.getComments();
      String comments = taskDetail.getFullTaskInstance().getComments();
      boolean commentsChanged = oldComments == null ? comments != null && !comments.isEmpty() : !oldComments.equals(comments);

      int priority = taskDetail.getFullTaskInstance().getPriority();
      boolean priorityChanged = (oldTi.getPriority() == null || oldTi.getPriority() != priority) ? true : false;
      FormDataDocument formdatadoc = null;
      if (ti.isGeneralTask())
      { // handle also auto form tasks
        TaskVO taskVO = TaskTemplateCache.getTaskTemplate(ti.getTaskId());
        boolean isAutoformTask = taskVO.isAutoformTask();
        if (isAutoformTask)
        {
          TaskManager taskMgr = RemoteLocator.getTaskManager();
          DocumentVO docvo = taskMgr.getTaskInstanceData(ti);
          formdatadoc = new FormDataDocument();
          formdatadoc.load(docvo.getContent());
          List<InstanceDataItem> instanceDataList = taskDetail.getInstanceDataItems();
          for (int i = 0; i < instanceDataList.size(); i++)
          {
            TaskInstanceDataItem tidi = (TaskInstanceDataItem) instanceDataList.get(i);
            if (tidi.isValueEditable())
            {
              formdatadoc.setValue(tidi.getTaskInstanceData().getName(), tidi.getTaskInstanceData().getRealStringValue());
            }
          }
        }
        else
        {
          formdatadoc = taskDetail.getDataDocument();
        }

        if (dueDateChanged)
        {
          String v = dueDate == null ? null : StringHelper.dateToString(dueDate);
          formdatadoc.setMetaValue(FormDataDocument.META_TASK_DUE_DATE, v);
          changesMap.put(DUE_DATE, dueDate);
        }
        if (commentsChanged)
        {
          formdatadoc.setMetaValue(FormDataDocument.META_TASK_COMMENT, comments);
          changesMap.put(COMMENTS,comments);
        }
        if(priorityChanged)
        {
          formdatadoc.setMetaValue(FormDataDocument.META_TASK_PRIORITY, Integer.toString(priority));
          changesMap.put(PRIORITY,priority);
        }
        if(!changesMap.isEmpty())
        {
          processTaskInstanceUpdate(changesMap, ti, cuid);
        }
        RemoteLocator.getEventManager().updateDocumentContent(ti.getSecondaryOwnerId(), formdatadoc.format(), FormDataDocument.class.getName());
      }
      List<InstanceDataItem> instanceDataList = taskDetail.getInstanceDataItems();
      for (int i = 0; i < instanceDataList.size(); i++)
      {
        TaskInstanceDataItem tidi = (TaskInstanceDataItem) instanceDataList.get(i);
        if (tidi.isValueEditable())
        {
          Object newValue = tidi.getTaskInstanceData().getData();
          if (tidi.isDocument())
          {
            // reset the value to docref instead of document object
            tidi.getTaskInstanceData().setData(tidi.getDocumentReference());
          }
          createOrUpdateTaskInstanceData(ti.getTaskInstanceId(), tidi.getTaskInstanceData(), newValue);
        }
      }
      List<DetailItem> detailDataList = taskDetail.getItems();
      if (detailDataList != null)
      {
        // old-style taskDetail based on TaskView.xml definition
        for (int i = 0; i < detailDataList.size(); i++)
        {
          DetailItem detailItem = (DetailItem) detailDataList.get(i);
          if (detailItem.isValueEditable())
          {
            // only specific attributes are updated
            if (detailItem.getAttribute().equals("dueDate"))
            {
              Object dueDateObj = detailItem.getDataValue();
              dueDate = null;
              if (dueDateObj != null)
              {
                if (dueDateObj instanceof Date)
                  dueDate = (Date)dueDateObj;
                else
                  dueDate = StringHelper.stringToDate(dueDateObj.toString());
              }
              updateTaskDueDate(ti, dueDate, ti.getComments());
            }
          }
        }
      }
      else
      {
        if (!ti.isGeneralTask())
        {
          // new-style
          if (dueDateChanged)
            changesMap.put(DUE_DATE, dueDate);
          if (commentsChanged)
            changesMap.put(COMMENTS, comments);
          if (priorityChanged)
            changesMap.put(PRIORITY, priority);
          if (!changesMap.isEmpty())
          {
            processTaskInstanceUpdate(changesMap, ti, cuid);
          }
        }
      }
      saveInstanceVariables(taskDetail.getProcessInstanceId());
      saveIndexValues(taskDetail.getFullTaskInstance(), taskDetail.getProcessInstanceId(), formdatadoc);
      if(!changesMap.isEmpty())
        FacesVariableUtil.addMessage("Task Details saved");
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage(ex.toString());
      throw new AbortProcessingException(ex.toString());
    }
  }

  private void processTaskInstanceUpdate(Map<String, Object> changesMap, TaskInstanceVO ti, String cuid) throws TaskException
  {
    TaskManagerAccess.getInstance().processTaskInstanceUpdate(changesMap, ti, cuid);
  }

  public void createOrUpdateTaskInstanceData(Long taskInstanceId, VariableInstanceVO variableInstanceVO, Object value)
  throws RemoteException, TaskException, ProcessException, DataAccessException, ServiceLocatorException
  {
	  TaskManager taskMgr = RemoteLocator.getTaskManager();

	  String user = FacesVariableUtil.getCurrentUser().getCuid();

      if (logger.isDebugEnabled())
      {
        logger.debug("\ncreateOrUpdateTaskInstanceData:\n" + "   task instance id = "
            + taskInstanceId + "   " + "dataKey = "
            + variableInstanceVO.getName() + "   " + "dataValue = "
            + value + "   " + "user = " + user);
      }
      TaskInstanceVO ti = taskMgr.getTaskInstance(taskInstanceId);
      VariableInstanceInfo vi = taskMgr.createTaskInstanceData(taskInstanceId, variableInstanceVO, (Serializable)value, FacesVariableUtil.getCurrentUser().getUserId());
      if(vi != null)
      {
        variableInstanceVO.setInstanceId(vi.getInstanceId());
        variableInstanceVO.setProcessInstanceId(ti.getOwnerId());
        if (VariableTranslator.isDocumentReferenceVariable(variableInstanceVO.getType()))
            variableInstanceVO.setData(vi.getData());
      }

  }

  public void updateTaskDueDate(TaskInstanceVO taskInst, Date taskDueDate, String comments)
  throws TaskException
  {
    TaskManagerAccess taskMgrAccess = TaskManagerAccess.getInstance();
	taskMgrAccess.updateTaskInstanceDueDate(taskInst, taskDueDate, FacesVariableUtil.getCurrentUser().getCuid(), comments);
  }
}
