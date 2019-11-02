/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.services.process;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.RetryableTransaction;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.*;
import com.centurylink.mdw.service.data.process.EngineDataAccess;
import com.centurylink.mdw.service.data.process.EngineDataAccessCache;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.EventException;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.util.ServiceLocatorException;
import com.centurylink.mdw.util.TransactionUtil;
import com.centurylink.mdw.util.TransactionWrapper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import org.apache.commons.lang.StringUtils;

import javax.transaction.SystemException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ProcessExecutor implements RetryableTransaction {

    private ProcessExecutorImpl engineImpl;

    private int transactionRetryCount = 0;
    private int transactionRetryMax = -1;
    private int transactionRetryInterval = -1;

    public ProcessExecutor(EngineDataAccess edao,
            InternalMessenger internalMessenger, boolean forServiceProcess) {
        engineImpl = new ProcessExecutorImpl(edao, internalMessenger, forServiceProcess);
    }

    ProcessExecutor(ProcessExecutorImpl engineImpl) {
        this.engineImpl = engineImpl;
    }

    ///////////////////////////////////////
    // methods about process instances
    ///////////////////////////////////////

    public ProcessInstance createProcessInstance(Long processId, String ownerType,
            Long ownerId, String secondaryOwnerType, Long secondaryOwnerId,
            String masterRequestId, Map<String,String> parameters)
    throws ProcessException, DataAccessException {

        String label = null, template = null;
        Process process = ProcessCache.getProcess(processId);
        if (process != null) {
            String pkgName = process.getPackageName();
            if (StringUtils.isBlank(pkgName)) { // This should never happen, but just in case
                Package pkg = PackageCache.getProcessPackage(processId);
                if (pkg != null && !pkg.isDefaultPackage())
                    pkgName = pkg.getName();
            }
            if (process.getName().startsWith("$") && parameters.containsKey(process.getName())) {
                // template process -- name is provided in params
                label = parameters.get(process.getName());
                parameters.remove(process.getName());
                template = process.getLabel();
                if (!StringUtils.isBlank(pkgName))
                    template = pkgName + "/" + template;
            }
            else {
                label = process.getLabel();
                if (!StringUtils.isBlank(pkgName))
                    label = pkgName + "/" + label;
            }
        }

        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.createProcessInstance(processId, ownerType,
                    ownerId, secondaryOwnerType, secondaryOwnerId,
                    masterRequestId, parameters, label, template);
        } catch (MdwException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).createProcessInstance(processId, ownerType,
                        ownerId, secondaryOwnerType, secondaryOwnerId, masterRequestId, parameters);
            }
            else
                throw e;
        } finally {
            stopTransaction(transaction);
        }
    }

    public ProcessInstance getProcessInstance(Long procInstId)
    throws ProcessException, DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.getDataAccess().getProcessInstance(procInstId);
        } catch (DataAccessException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).getProcessInstance(procInstId);
            }
            else
                throw e;
        } catch (SQLException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).getProcessInstance(procInstId);
            }
            else
                throw new ProcessException(0, "Failed to load process instance:" + procInstId, e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void startProcessInstance(ProcessInstance processInst, int delay)
    throws ProcessException, DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.startProcessInstance(processInst, delay);
        } catch (MdwException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).startProcessInstance(processInst, delay);
            }
            else
                throw e;
        } finally {
            stopTransaction(transaction);
        }
    }

    public void handleProcessFinish(InternalEvent event)
    throws ProcessException, DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.handleProcessFinish(event);
        } catch (MdwException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).handleProcessFinish(event);
            }
            else
                throw e;
        } finally {
            stopTransaction(transaction);
        }
    }

    public void updateProcessInstanceStatus(Long pProcInstId, Integer status)
    throws DataAccessException,ProcessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.updateProcessInstanceStatus(pProcInstId, status);
        } catch (MdwException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).updateProcessInstanceStatus(pProcInstId, status);
            }
            else
                throw e;
        } finally {
            stopTransaction(transaction);
        }
    }

    public void abortProcessInstance(InternalEvent event)
    throws ProcessException, DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.abortProcessInstance(event);
        } catch (MdwException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).abortProcessInstance(event);
            }
            else
                throw e;
        } finally {
            stopTransaction(transaction);
        }
    }

    public void setProcessInstanceCompletionCode(Long procInstId, String completionCode)
     throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.getDataAccess().setProcessInstanceCompletionCode(procInstId, completionCode);
        } catch (DataAccessException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).setProcessInstanceCompletionCode(procInstId, completionCode);
            }
            else
                throw e;
        } catch (SQLException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).setProcessInstanceCompletionCode(procInstId, completionCode);
            }
            else
                throw new DataAccessException(0, "Failed to set process instance completion code", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void setProcessInstanceStartTime(Long processInstanceId) throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            engineImpl.getDataAccess().setProcessInstanceStartTime(processInstanceId);
        } catch (DataAccessException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).setProcessInstanceStartTime(processInstanceId);
            }
            else
                throw e;
        } catch (SQLException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).setProcessInstanceStartTime(processInstanceId);
            }
            else
                throw new DataAccessException(0, "Failed to set process instance completion code", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    ///////////////////////////////////////
    // methods about activity instances
    ///////////////////////////////////////

    public ActivityInstance getActivityInstance(Long pActivityInstId)
    throws ProcessException, DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.getDataAccess().getActivityInstance(pActivityInstId);
        } catch (DataAccessException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).getActivityInstance(pActivityInstId);
            }
            else
                throw e;
        } catch (SQLException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).getActivityInstance(pActivityInstId);
            }
            else
                throw new ProcessException(0, "Failed to get activity instance", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void executeActivityInstance(BaseActivity activity)
    throws ActivityException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            if (activity.getTimer() != null) {
                activity.executeTimed(this);
            } else {
                activity.execute(this);
            }
        } catch (DataAccessException e) {
            throw new ActivityException(-1, "Failed to execute activity", e);
        } finally {
            try {
                stopTransaction(transaction);
            } catch (DataAccessException e) {
                LoggerUtil.getStandardLogger().error("Fail to stop transaction in execute()", e);
            }
        }
    }

    public void failActivityInstance(InternalEvent event,
            ProcessInstance processInst, Long activityId, Long activityInstId,
            BaseActivity activity, Throwable cause) {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.failActivityInstance(event,
                    processInst, activityId, activityInstId,
                    activity, cause);
        } catch (MdwException | SQLException e) {
            if (canRetryTransaction(e)) {
                try {
                    transaction = (TransactionWrapper)initTransactionRetry(transaction);
                    ((ProcessExecutor)getTransactionRetrier()).failActivityInstance(event, processInst, activityId, activityInstId, activity, cause);
                } catch (DataAccessException ex) {
                    StandardLogger logger = LoggerUtil.getStandardLogger();
                    logger.severeException("Exception thrown during failActivityInstance", ex);
                }
            }
            else {
                StandardLogger logger = LoggerUtil.getStandardLogger();
                logger.severeException("Exception thrown during failActivityInstance", e);
            }
        } finally {
            try {
                stopTransaction(transaction);
            } catch (DataAccessException e) {
                StandardLogger logger = LoggerUtil.getStandardLogger();
                logger.severeException("Fail to stop transaction in failActivityInstance", e);
            }
        }
    }

    public DocumentReference createActivityExceptionDocument(ProcessInstance processInst, ActivityInstance actInstVO, BaseActivity activityImpl, Throwable cause) throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.createActivityExceptionDocument(processInst, actInstVO, activityImpl, cause);
        } catch (DataAccessException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper) initTransactionRetry(transaction);
                return ((ProcessExecutor) getTransactionRetrier()).createActivityExceptionDocument(processInst, actInstVO, activityImpl, cause);
            }
            else {
                throw e;
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public DocumentReference createProcessExceptionDocument(ProcessInstance processInst, Throwable cause) throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.createProcessExceptionDocument(processInst, cause);
        } catch (DataAccessException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper) initTransactionRetry(transaction);
                return ((ProcessExecutor) getTransactionRetrier()).createProcessExceptionDocument(processInst, cause);
            }
            else {
                throw e;
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public ActivityRuntime prepareActivityInstance(InternalEvent event, ProcessInstance procInst)
            throws ProcessException, DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.prepareActivityInstance(event, procInst);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void cancelActivityInstance(ActivityInstance actInst, ProcessInstance procinst, String pStatusMsg)
            throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            engineImpl.cancelActivityInstance(actInst, procinst, pStatusMsg);
        } catch (MdwException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).cancelActivityInstance(actInst, procinst, pStatusMsg);
            }
            else
                throw e;
        } catch (SQLException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).cancelActivityInstance(actInst, procinst, pStatusMsg);
            }
            else
                throw new DataAccessException(0, "Failed to set activity instance status to cancel", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void holdActivityInstance(ActivityInstance actInst, Long procId)
    throws ProcessException, DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.holdActivityInstance(actInst, procId);
        } catch (MdwException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).holdActivityInstance(actInst, procId);
            }
            else
                throw e;
        } catch (SQLException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).holdActivityInstance(actInst, procId);
            }
            else
                throw new DataAccessException(0, "Failed to set activity instance status to hold", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public CompletionCode finishActivityInstance(BaseActivity activity,
            ProcessInstance pi, ActivityInstance ai, InternalEvent event, boolean bypassWait)
            throws ProcessException,ActivityException, DataAccessException, ServiceLocatorException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.finishActivityInstance(activity, pi, ai, event, bypassWait);
        }
        catch (MdwException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).finishActivityInstance(activity, pi, ai, event, bypassWait);
            }
            else
                throw e;
        } finally {
            stopTransaction(transaction);
        }
    }

    public ActivityRuntime resumeActivityPrepare(ProcessInstance procInst,
            InternalEvent event, boolean resumeOnHold) throws ProcessException, DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.resumeActivityPrepare(procInst, event, resumeOnHold);
        }
        catch (MdwException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).resumeActivityPrepare(procInst, event, resumeOnHold);
            }
            else
                throw e;
        } finally {
            stopTransaction(transaction);
        }
    }

    public boolean resumeActivityExecute(
            ActivityRuntime ar, InternalEvent event, boolean resumeOnHold)
        throws ActivityException, DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.resumeActivityExecute(ar, event, resumeOnHold);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void resumeActivityFinish(ActivityRuntime ar, boolean finished,
            InternalEvent event, boolean resumeOnHold)
            throws ProcessException,DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.resumeActivityFinish(ar, finished, event, resumeOnHold);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void resumeActivityException(ProcessInstance procInst, Long actInstId,
            BaseActivity activity, Throwable cause) {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.resumeActivityException(procInst, actInstId, activity, cause);
        } catch (DataAccessException e) {
            StandardLogger logger = LoggerUtil.getStandardLogger();
            logger.severeException("Failed to process resume activity exception", e);
        } finally {
            try {
                stopTransaction(transaction);
            } catch (DataAccessException e) {
                StandardLogger logger = LoggerUtil.getStandardLogger();
                logger.severeException("Fail to stop transaction in resumeActivityException", e);
            }
        }
    }

    public Long getRequestCompletionTime(String ownerType, Long ownerId) throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.getDataAccess().getRequestCompletionTime(ownerType, ownerId);
        } catch (SQLException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).getRequestCompletionTime(ownerType, ownerId);
            }
            else
                throw new DataAccessException(0, "Failed to get request completion time", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void setElapsedTime(String ownerType, Long instanceId, Long elapsedTime) throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.getDataAccess().setElapsedTime(ownerType, instanceId, elapsedTime);
        } catch (SQLException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).setElapsedTime(ownerType, instanceId, elapsedTime);
            }
            else
                throw new DataAccessException(0, "Failed to set elapsed time", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void setActivityInstanceStatus(ActivityInstance actInst,
            Integer status, String status_message) throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.getDataAccess().setActivityInstanceStatus(actInst, status, status_message);
        } catch (DataAccessException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).setActivityInstanceStatus(actInst, status, status_message);
            }
            else
                throw e;
        } catch (SQLException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).setActivityInstanceStatus(actInst, status, status_message);
            }
            else
                throw new DataAccessException(0, "Failed to set activity instance status", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void updateActivityInstanceEndTime(Long actInstId, Date endtime)
     throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.getDataAccess().updateActivityInstanceEndTime(actInstId, endtime);
        } catch (DataAccessException e) {
                if (canRetryTransaction(e)) {
                    transaction = (TransactionWrapper)initTransactionRetry(transaction);
                    ((ProcessExecutor)getTransactionRetrier()).updateActivityInstanceEndTime(actInstId, endtime);
                }
                else
                    throw e;
        } catch (SQLException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).updateActivityInstanceEndTime(actInstId, endtime);
            }
            else
                throw new DataAccessException(0, "Failed to update activity end time", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public int countActivityInstances(Long procInstId, Long activityId, Integer[] statuses)
     throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.getDataAccess().countActivityInstances(procInstId, activityId, statuses);
        } catch (DataAccessException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).countActivityInstances(procInstId, activityId, statuses);
            }
            else
                throw e;
        } catch (SQLException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).countActivityInstances(procInstId, activityId, statuses);
            }
            else
                throw new DataAccessException(0, "Failed to count activity instances", e);
        } finally {
            stopTransaction(transaction);
        }
    }


    ///////////////////////////////////////
    // methods about transition instances
    ///////////////////////////////////////

    /**
     * Creates a new instance of the WorkTransationInstance entity
     *
     * @param transition
     * @param pProcessInstId
     * @return WorkTransitionInstance object
     */
    public TransitionInstance createTransitionInstance(Transition transition, Long pProcessInstId)
    throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.createTransitionInstance(transition, pProcessInstId);
        } catch (DataAccessException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).createTransitionInstance(transition, pProcessInstId);
            }
            else
                throw e;
        } finally {
            stopTransaction(transaction);
        }
    }

    public void createTransitionInstances(ProcessInstance processInstanceVO,
            List<Transition> transitions, Long fromActivityInstanceId)
          throws ProcessException, DataAccessException{
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.createTransitionInstances(processInstanceVO, transitions, fromActivityInstanceId);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void determineCompletedTransitions(Long pProcInstId, List<Transition> transitions)
       throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.getDataAccess().determineCompletedTransitions(pProcInstId, transitions);
        } catch (DataAccessException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).determineCompletedTransitions(pProcInstId, transitions);
            }
            else
                throw e;
        } catch (SQLException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).determineCompletedTransitions(pProcInstId, transitions);
            }
            else
                throw new DataAccessException(0, "Failed to determine completed transitions", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    ///////////////////////////////////////
    // methods about variable instances and documents
    ///////////////////////////////////////

    public void updateDocumentInfo(DocumentReference docref,
            String documentType, String ownerType, Long ownerId, Integer statusCode, String statusMessage) throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.updateDocumentInfo(docref, documentType, ownerType, ownerId, statusCode, statusMessage);
        } catch (DataAccessException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).updateDocumentInfo(docref, documentType, ownerType, ownerId, statusCode, statusMessage);
            }
            else
                throw e;
        } finally {
            stopTransaction(transaction);
        }
    }

    public VariableInstance createVariableInstance(ProcessInstance pi,
            String varname, Object value)
    throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.createVariableInstance(pi, varname, value);
        } catch (DataAccessException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).createVariableInstance(pi, varname, value);
            }
            else
                throw e;
        } catch (SQLException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).createVariableInstance(pi, varname, value);
            }
            else
                throw new DataAccessException(0, "Failed to create variable instance", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void updateVariableInstance(VariableInstance var)
     throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.getDataAccess().updateVariableInstance(var);
        } catch (DataAccessException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).updateVariableInstance(var);
            }
            else
                throw e;
        } catch (SQLException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).updateVariableInstance(var);
            }
            else
                throw new DataAccessException(0, "Failed to update variable instance", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public VariableInstance getVariableInstance(Long procInstId, String varname)
     throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.getDataAccess().getVariableInstance(procInstId, varname);
        } catch (DataAccessException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).getVariableInstance(procInstId, varname);
            }
            else
                throw e;
        } catch (SQLException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).getVariableInstance(procInstId, varname);
            }
            else
                throw new DataAccessException(0, "Failed to get variable instance", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public Document getDocument(DocumentReference docref, boolean forUpdate)
     throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.getDocument(docref, forUpdate);
        } catch (DataAccessException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).getDocument(docref, forUpdate);
            }
            else
                throw e;
        } finally {
            if (!forUpdate)  // Do not release the locked "for update" document row
                stopTransaction(transaction);
        }
    }

    /**
     * Always goes to the database.
     */
    public Document loadDocument(DocumentReference docref, boolean forUpdate)
    throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.loadDocument(docref, forUpdate);
        } catch (DataAccessException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).loadDocument(docref, forUpdate);
            }
            else
                throw e;
        } finally {
            stopTransaction(transaction);
        }
    }

    public DocumentReference createDocument(String type, String ownerType, Long ownerId, Object doc)
    throws DataAccessException {
        return createDocument(type, ownerType, ownerId, null, null, doc);
    }

    public DocumentReference createDocument(String type, String ownerType, Long ownerId, Integer statusCode, String statusMessage, Object doc)
            throws DataAccessException {
        return createDocument(type, ownerType, ownerId, statusCode, statusMessage, null, doc);
    }

    public DocumentReference createDocument(String type, String ownerType, Long ownerId, Integer statusCode, String statusMessage, String path, Object doc)
    throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.createDocument(type, ownerType, ownerId, statusCode, statusMessage, path, doc);
        } catch (DataAccessException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).createDocument(type, ownerType, ownerId, statusCode, statusMessage, path, doc);
            }
            else
                throw e;
        } finally {
            stopTransaction(transaction);
        }
    }

    public void updateDocumentContent(DocumentReference docref, Object doc, String type, Package pkg)
     throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.updateDocumentContent(docref, doc, type, pkg);
        } catch (DataAccessException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).updateDocumentContent(docref, doc, type, pkg);
            }
            else
                throw e;
        } finally {
            stopTransaction(transaction);
        }
    }

    public void addDocumentToCache(Document docvo) {
        if (engineImpl.getDataAccess() instanceof EngineDataAccessCache) {
            ((EngineDataAccessCache)engineImpl.getDataAccess()).addDocumentToCache(docvo);
        }
    }

    ///////////////////////////////////////
    // methods about event instances, event wait instances, event notifications
    ///////////////////////////////////////

    public void cancelEventWaitInstances(Long activityInstanceId)
        throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.cancelEventWaitInstances(activityInstanceId);
        } catch (DataAccessException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).cancelEventWaitInstances(activityInstanceId);
            }
            else
                throw e;
        } finally {
            stopTransaction(transaction);
        }
    }

    public boolean deleteInternalEvent(String eventName) throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.deleteInternalEvent(eventName);
        } catch (DataAccessException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).deleteInternalEvent(eventName);
            }
            else
                throw new DataAccessException(0, "Failed to delete internal event" + eventName, e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public EventWaitInstance createEventWaitInstance(Long procInstId, Long actInstId, String pEventName,
            String compCode, boolean pRecurring, boolean notifyIfArrived, boolean reregister)
    throws DataAccessException, ProcessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.createEventWaitInstance(procInstId, actInstId,
                    pEventName, compCode, pRecurring, notifyIfArrived, reregister);
        }
        catch (MdwException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).createEventWaitInstance(procInstId, actInstId, pEventName, compCode, pRecurring, notifyIfArrived, reregister);
            }
            else
                throw e;
        } finally {
            stopTransaction(transaction);
        }
    }

    public EventWaitInstance createEventWaitInstance(Long procInstId,
            Long actInstId, String pEventName, String compCode,
            boolean pRecurring, boolean notifyIfArrived)
    throws DataAccessException, ProcessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.createEventWaitInstance(procInstId, actInstId, pEventName, compCode, pRecurring,
                    notifyIfArrived);
        }
        catch (MdwException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).createEventWaitInstance(procInstId, actInstId,
                        pEventName, compCode, pRecurring, notifyIfArrived);
            }
            else
                throw e;
        } finally {
            stopTransaction(transaction);
        }
    }

    public EventWaitInstance createEventWaitInstances(Long procInstId, Long actInstId, String[] pEventNames,
            String[] pWakeUpEventTypes, boolean[] pEventOccurances, boolean notifyIfArrived, boolean reregister)
    throws DataAccessException, ProcessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.createEventWaitInstances(procInstId, actInstId,
                    pEventNames, pWakeUpEventTypes, pEventOccurances, notifyIfArrived, reregister);
        }
        catch (MdwException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).createEventWaitInstances(procInstId, actInstId,
                        pEventNames, pWakeUpEventTypes, pEventOccurances, notifyIfArrived, reregister);
            }
            else
                throw e;
        } finally {
            stopTransaction(transaction);
        }
    }

    public EventWaitInstance createEventWaitInstances(Long procInstId, Long actInstId, String[] pEventNames,
            String[] pWakeUpEventTypes, boolean[] pEventOccurances, boolean notifyIfArrived)
    throws DataAccessException, ProcessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.createEventWaitInstances(procInstId, actInstId, pEventNames, pWakeUpEventTypes,
                    pEventOccurances, notifyIfArrived);
        }
        catch (MdwException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).createEventWaitInstances(procInstId, actInstId,
                        pEventNames, pWakeUpEventTypes, pEventOccurances, notifyIfArrived);
            }
            else
                throw e;
        } finally {
            stopTransaction(transaction);
        }
    }

    public void removeEventWaitForActivityInstance(Long activityInstanceId, String reason)
    throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.getDataAccess().removeEventWaitForActivityInstance(activityInstanceId, reason);
        } catch (DataAccessException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).removeEventWaitForActivityInstance(activityInstanceId, reason);
            }
            else
                throw e;
        } catch (SQLException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).removeEventWaitForActivityInstance(activityInstanceId, reason);
            }
            else
                throw new DataAccessException(0, "Failed to remove event wait for activity instance", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public Integer notifyProcess(String pEventName, Long pEventInstId,
                String message, int delay)
    throws DataAccessException, EventException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.notifyProcess(pEventName, pEventInstId, message, delay);
        } catch (DataAccessException | EventException e){
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).notifyProcess(pEventName, pEventInstId, message, delay);
            }
            else
                throw e;
        } catch (SQLException e){
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).notifyProcess(pEventName, pEventInstId, message, delay);
            }
            else
                throw new DataAccessException(0, "Failed to notify process of event arrival", e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void sendInternalEvent(InternalEvent event)
            throws MdwException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.sendInternalEvent(event);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void sendDelayedInternalEvent(InternalEvent event, int delaySeconds, String msgid, boolean isUpdate)
        throws MdwException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.sendDelayedInternalEvent(event, delaySeconds, msgid, isUpdate);
        } finally {
            stopTransaction(transaction);
        }
    }

    //////////////////////////////////////////////////
    // miscellaneous methods with transaction wrapper
    /////////////////////////////////////////////////////

    public String getSynchronousProcessResponse(Long procInstId, String varname)
    throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.getServiceProcessResponse(procInstId, varname);
        } catch (Exception e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).getSynchronousProcessResponse(procInstId, varname);
            }
            else
                throw new DataAccessException(0, "Failed to get value for variable " + varname, e);
        } finally {
            stopTransaction(transaction);
        }
    }

    public Map<String, String> getOutPutParameters(Long procInstId, Long procId)
    throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.getOutputParameters(procInstId, procId);
        } catch (Exception e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).getOutPutParameters(procInstId, procId);
            }
            else
                throw new DataAccessException(0, "Failed to get output parameters:" + procInstId, e);
        } finally {
            stopTransaction(transaction);
        }
    }

    /**
     * For running your own code inside a transaction.
     * To be used in execute() method when transactions are not turned on globally.
     * @param runnable
     * @throws ProcessException
     * @throws DataAccessException
     */
    public void runInTransaction(Runnable runnable)
    throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            runnable.run();
        } finally {
            stopTransaction(transaction);
        }
    }

    /**
     * For running your own code inside a retryable transaction.
     * To be used in execute() method when transactions are not turned on globally.
     * @param runnable
     * @throws ProcessException
     * @throws DataAccessException
     */
    public void runInRetryableTransaction(Runnable runnable)
    throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            runnable.run();
        } catch (Throwable e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).runInTransaction(runnable);
            }
            else
                throw e;
        } finally {
            stopTransaction(transaction);
        }
    }

    //////////////////////////////////////////////////
    // Methods that are not transaction wrappers
    //////////////////////////////////////////////////

    /**
     * This method must be called within the same transaction scope (namely engine is already started
     * @param actInstId
     * @throws DataAccessException
     */
    public Integer lockActivityInstance(Long actInstId)
       throws DataAccessException {
        try {
            if (!isInTransaction()) throw
                new DataAccessException("Cannot lock activity instance without a transaction");
            return engineImpl.getDataAccess().lockActivityInstance(actInstId);
        } catch (SQLException e) {
            throw new DataAccessException(0, "Failed to lock activity instance", e);
        }
    }

    /**
     * this method must be called within the same transaction scope (namely engine is already started
     * @param procInstId
     * @throws DataAccessException
     */
    public Integer lockProcessInstance(Long procInstId)
       throws DataAccessException {
        try {
            if (!isInTransaction()) throw
                new DataAccessException("Cannot lock activity instance without a transaction");
            return engineImpl.getDataAccess().lockProcessInstance(procInstId);
        } catch (SQLException e) {
            throw new DataAccessException(0, "Failed to lock process instance", e);
        }
    }

    InternalMessenger getInternalMessenger() {
        return engineImpl.getInternalMessenger();
    }

    public int getPerformanceLevel() {
        return engineImpl.getDataAccess().getPerformanceLevel();
    }

    public boolean isInMemory() {
        return engineImpl.isInMemory();
    }

    public boolean isInService() {
        return engineImpl.isInService();
    }

    public DatabaseAccess getDatabaseAccess() throws SQLException {
        DatabaseAccess db = engineImpl.getDatabaseAccess();
        if (db==null) throw new SQLException("Engine database is not open");
        return db;
    }

    public TransactionWrapper startTransaction() throws DataAccessException {
        return engineImpl.getDataAccess().startTransaction();
    }

    public void stopTransaction(TransactionWrapper transaction) throws DataAccessException {
        engineImpl.getDataAccess().stopTransaction(transaction);
    }

    public void rollbackTransaction() throws DataAccessException {
        engineImpl.getDatabaseAccess().rollback();
    }

    private boolean isInTransaction() {
        try {
            return TransactionUtil.getInstance().isInTransaction();
        } catch (SystemException e) {
            StandardLogger logger = LoggerUtil.getStandardLogger();
            logger.severeException("Fail to check if inside transaction - treated as not", e);
            return false;
        }
    }

    /**
     * Notify registered ProcessMonitors.
     */
    public void notifyMonitors(ProcessInstance processInstance, WorkStatus.InternalLogMessage logMessage) {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            engineImpl.notifyMonitors(processInstance, logMessage);
        }
        catch (Exception ex) {
            // do not stop processing due to notification failure
            LoggerUtil.getStandardLogger().severeException(ex.getMessage(), ex);
        }
        finally {
            try {
                stopTransaction(transaction);
            }
            catch (DataAccessException ex) {
                LoggerUtil.getStandardLogger().severeException(ex.getMessage(), ex);
            }
        }
    }

    public boolean canRetryTransaction(Throwable e) {
        if (transactionRetryMax < 0)
            transactionRetryMax = PropertyManager.getIntegerProperty(PropertyNames.MDW_TRANSACTION_RETRY_MAX, 3);

        if (transactionRetryCount < transactionRetryMax) {
            if (engineImpl.getDatabaseAccess().isMySQL()) {
                if (e.getMessage() != null && e.getMessage().contains("try restarting transaction")) {
                    ProcessExecutorImpl.logger.warn("Encountered the following MySQL issue - will retry transaction (Attempt " + (transactionRetryCount+1) + " of " + transactionRetryMax + "): " + e.getMessage());
                    return true;
                }
                if (e.getCause() != null)
                    return canRetryTransaction(e.getCause());
            }
        }
        else {
            ProcessExecutorImpl.logger.severe("Encountered the following issue - will NOT retry transaction (Max attempts reached): " + e.getMessage());
        }
        return false;
    }

    public Object getTransactionRetrier() throws DataAccessException {
        ProcessExecutor retrier = this;
        if (transactionRetryCount == 0) {
            retrier = new ProcessExecutor(engineImpl);
            retrier.setTransactionRetryCount(1);
        }
        else
            retrier.setTransactionRetryCount(transactionRetryCount + 1);

        return retrier;
    }

    public Object initTransactionRetry(TransactionWrapper transaction) throws DataAccessException {
        this.rollbackTransaction();
        stopTransaction(transaction);

        if (transactionRetryInterval < 0)
            transactionRetryInterval = PropertyManager.getIntegerProperty(PropertyNames.MDW_TRANSACTION_RETRY_INTERVAL, 1000);

        try {
            Thread.sleep(transactionRetryInterval);
        }
        catch (InterruptedException e) {  // If interrupted, continue with transaction retry
        }
        return null;
    }

    public int getTransactionRetryCount() {
        return transactionRetryCount;
    }

    public void setTransactionRetryCount(int count) {
        transactionRetryCount = count;
    }

}
