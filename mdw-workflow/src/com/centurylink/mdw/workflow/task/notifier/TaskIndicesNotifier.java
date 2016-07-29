/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.task.notifier;

import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.exception.ObserverException;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskIndexes;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskRuntimeContext;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.observer.task.RemoteNotifier;
import com.centurylink.mdw.observer.task.TaskNotifier;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.services.task.TaskManagerAccess;

/**
 * Notifies a Summary TaskManager with the current values of the configured indices.
 *
 */
public class TaskIndicesNotifier implements TaskNotifier, RemoteNotifier {

    public void sendNotice(TaskInstanceVO taskInstance, String outcome) throws ObserverException {

        Map<String,String> indices = collectIndices(taskInstance);
        TaskIndexes taskIndexes = new TaskIndexes(taskInstance.getAssociatedTaskInstanceId(), indices);
        try {
            TaskManagerAccess.getInstance().notifySummaryTaskManager("UpdateTaskIndexes", taskIndexes);
        }
        catch (Exception ex) {
            throw new ObserverException(ex.getMessage(), ex);
        }
    }

    public void sendNotice(TaskRuntimeContext runtimeContext, String taskAction, String outcome)
            throws ObserverException {
        sendNotice(runtimeContext, outcome);
    }

    public void sendNotice(TaskRuntimeContext runtimeContext, String outcome) throws ObserverException {
        sendNotice(runtimeContext.getTaskInstanceVO(), outcome);
    }

    protected Map<String,String> collectIndices(TaskInstanceVO taskInstance) throws ObserverException {
        Map<String,String> indices = new HashMap<String,String>();
        TaskVO task = TaskTemplateCache.getTaskTemplate(taskInstance.getTaskId());
        TaskManager taskMgr = ServiceLocator.getTaskManager();

        try {
            if (task.isAutoformTask()) {
                EventManager eventMgr = ServiceLocator.getEventManager();
                if (procInstVO == null || processVO == null) {
                    procInstVO = eventMgr.getProcessInstance(taskInstance.getOwnerId());
                    processVO = ProcessVOCache.getProcessVO(procInstVO.getProcessId());
                }

                if (processVO.isEmbeddedProcess() || procInstVO.isNewEmbedded())
                    indices = taskMgr.collectIndices(task.getTaskId(), procInstVO.getOwnerId(), null);
                else
                    indices = taskMgr.collectIndices(task.getTaskId(), taskInstance.getOwnerId(), null);
            }
            else {
                indices = taskMgr.collectIndices(task.getTaskId(), taskInstance.getOwnerId(), null);
            }
        }
        catch (ProcessException ex) {
            throw new ObserverException(ex.getMessage(), ex);
        }
        catch (DataAccessException ex) {
            throw new ObserverException(ex.getMessage(), ex);
        }

        return indices;
    }

    private ProcessInstanceVO procInstVO;
    private ProcessVO processVO;

    protected VariableInstanceInfo getVariableInstance(Long procInstId, String name)
    throws ProcessException, DataAccessException {
        EventManager eventMgr = ServiceLocator.getEventManager();
        if (procInstVO == null || processVO == null) {
            procInstVO = eventMgr.getProcessInstance(procInstId);
            processVO = ProcessVOCache.getProcessVO(procInstVO.getProcessId());
        }

        VariableInstanceInfo vi = null;
        if (processVO.isEmbeddedProcess() || procInstVO.isNewEmbedded())
            vi = eventMgr.getVariableInstance(procInstVO.getOwnerId(), name);  // embedded subproc
        else
            vi = eventMgr.getVariableInstance(procInstId, name);

        return vi;

    }

}
