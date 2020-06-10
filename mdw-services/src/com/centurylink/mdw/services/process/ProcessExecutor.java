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
import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.RetryableTransaction;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.request.Response;
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
import com.centurylink.mdw.util.TransactionUtil;
import com.centurylink.mdw.util.TransactionWrapper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import org.apache.commons.lang.StringUtils;

import javax.transaction.SystemException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ProcessExecutor implements RetryableTransaction {

    private final ProcessExecutorImpl engineImpl;

    private int transactionRetryCount = 0;
    private int transactionRetryMax = -1;
    private int transactionRetryInterval = -1;

    public ProcessExecutor(EngineDataAccess edao, InternalMessenger internalMessenger, boolean forServiceProcess) {
        engineImpl = new ProcessExecutorImpl(edao, internalMessenger, forServiceProcess);
        engineImpl.activityTimings = PropertyManager.getBooleanProperty(PropertyNames.MDW_TIMINGS_ACTIVITIES, false);
    }

    ProcessExecutor(ProcessExecutorImpl engineImpl) {
        this.engineImpl = engineImpl;
    }

    public ProcessInstance createProcessInstance(Long processId, String ownerType, Long ownerId,
            String secondaryOwnerType, Long secondaryOwnerId, String masterRequestId, Map<String,Object> values)
            throws ProcessException, DataAccessException {

        String label = null, template = null;
        Process process;
        try {
            process = ProcessCache.getProcess(processId);
        } catch (IOException ex) {
            throw new ProcessException("Cannot load process " + processId, ex);
        }
        if (process != null) {
            String pkgName = process.getPackageName();
            if (StringUtils.isBlank(pkgName)) { // should never happen, but just in case
                Package pkg = PackageCache.getPackage(process.getPackageName());
                if (pkg != null)
                    pkgName = pkg.getName();
            }
            if (process.getName().startsWith("$") && values.containsKey(process.getName())) {
                // template process -- name is provided in params
                label = values.get(process.getName()).toString();
                values.remove(process.getName());
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

        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.createProcessInstance(processId, ownerType,
                    ownerId, secondaryOwnerType, secondaryOwnerId,
                    masterRequestId, values, label, template);
        } catch (MdwException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).createProcessInstance(processId, ownerType,
                        ownerId, secondaryOwnerType, secondaryOwnerId, masterRequestId, values);
            }
            else {
                throw ex;
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public ProcessInstance getProcessInstance(Long procInstId) throws ProcessException, DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.getDataAccess().getProcessInstance(procInstId);
        } catch (DataAccessException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).getProcessInstance(procInstId);
            }
            else {
                throw ex;
            }
        } catch (SQLException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).getProcessInstance(procInstId);
            }
            else {
                throw new ProcessException(0, "Failed to load process instance:" + procInstId, ex);
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public void startProcessInstance(ProcessInstance processInst, int delay)
            throws ProcessException, DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            engineImpl.startProcessInstance(processInst, delay);
        } catch (ProcessException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).startProcessInstance(processInst, delay);
            }
            else {
                throw ex;
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public void handleProcessFinish(InternalEvent event) throws ProcessException, DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.handleProcessFinish(event);
        } catch (MdwException e) {
            if (canRetryTransaction(e)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).handleProcessFinish(event);
            }
            else {
                throw e;
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public void updateProcessInstanceStatus(Long pProcInstId, Integer status)
    throws DataAccessException,ProcessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            engineImpl.updateProcessInstanceStatus(pProcInstId, status);
        } catch (MdwException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).updateProcessInstanceStatus(pProcInstId, status);
            }
            else {
                throw ex;
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public void abortProcessInstance(InternalEvent event) throws ProcessException, DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            engineImpl.abortProcessInstance(event);
        } catch (MdwException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).abortProcessInstance(event);
            }
            else {
                throw ex;
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public void setProcessInstanceCompletionCode(Long procInstId, String completionCode)
            throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            engineImpl.getDataAccess().setProcessInstanceCompletionCode(procInstId, completionCode);
        } catch (DataAccessException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).setProcessInstanceCompletionCode(procInstId, completionCode);
            }
            else {
                throw ex;
            }
        } catch (SQLException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).setProcessInstanceCompletionCode(procInstId, completionCode);
            }
            else {
                throw new DataAccessException("Failed to set process instance completion code", ex);
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public void setProcessInstanceStartTime(Long processInstanceId) throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            engineImpl.getDataAccess().setProcessInstanceStartTime(processInstanceId);
        } catch (DataAccessException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).setProcessInstanceStartTime(processInstanceId);
            }
            else {
                throw ex;
            }
        } catch (SQLException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).setProcessInstanceStartTime(processInstanceId);
            }
            else {
                throw new DataAccessException(0, "Failed to set process instance completion code", ex);
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public ActivityInstance getActivityInstance(Long actInstId) throws ProcessException, DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.getDataAccess().getActivityInstance(actInstId);
        } catch (DataAccessException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).getActivityInstance(actInstId);
            }
            else {
                throw ex;
            }
        } catch (SQLException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).getActivityInstance(actInstId);
            }
            else {
                throw new ProcessException("Failed to get activity instance", ex);
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public void executeActivityInstance(BaseActivity activity) throws ActivityException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            if (activity.getTimer() != null) {
                activity.executeTimed(this);
            } else {
                activity.execute(this);
            }
        } catch (DataAccessException ex) {
            throw new ActivityException(-1, "Failed to execute activity", ex);
        } finally {
            try {
                stopTransaction(transaction);
            } catch (DataAccessException ex) {
                LoggerUtil.getStandardLogger().error("Fail to stop transaction in execute()", ex);
            }
        }
    }

    public void failActivityInstance(InternalEvent event, ProcessInstance processInst, Long activityId,
            Long activityInstId, BaseActivity activity, Throwable cause) {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            engineImpl.failActivityInstance(event,
                    processInst, activityId, activityInstId,
                    activity, cause);
        } catch (MdwException | SQLException ex) {
            if (canRetryTransaction(ex)) {
                try {
                    transaction = (TransactionWrapper)initTransactionRetry(transaction);
                    ((ProcessExecutor)getTransactionRetrier()).failActivityInstance(event, processInst, activityId, activityInstId, activity, cause);
                } catch (DataAccessException ex1) {
                    StandardLogger logger = LoggerUtil.getStandardLogger();
                    logger.error("Exception thrown during failActivityInstance", ex1);
                }
            }
            else {
                StandardLogger logger = LoggerUtil.getStandardLogger();
                logger.error("Exception thrown during failActivityInstance", ex);
            }
        } finally {
            try {
                stopTransaction(transaction);
            } catch (DataAccessException ex) {
                StandardLogger logger = LoggerUtil.getStandardLogger();
                logger.error("Fail to stop transaction in failActivityInstance", ex);
            }
        }
    }

    public DocumentReference createActivityExceptionDocument(ProcessInstance processInst, ActivityInstance actInst,
            BaseActivity activityImpl, Throwable cause) throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.createActivityExceptionDocument(processInst, actInst, activityImpl, cause);
        } catch (DataAccessException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper) initTransactionRetry(transaction);
                return ((ProcessExecutor) getTransactionRetrier()).createActivityExceptionDocument(processInst, actInst, activityImpl, cause);
            }
            else {
                throw ex;
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public DocumentReference createProcessExceptionDocument(ProcessInstance processInst, Throwable cause)
            throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.createProcessExceptionDocument(processInst, cause);
        } catch (DataAccessException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper) initTransactionRetry(transaction);
                return ((ProcessExecutor) getTransactionRetrier()).createProcessExceptionDocument(processInst, cause);
            }
            else {
                throw ex;
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
        } catch (MdwException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).cancelActivityInstance(actInst, procinst, pStatusMsg);
            }
            else {
                throw ex;
            }
        } catch (SQLException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).cancelActivityInstance(actInst, procinst, pStatusMsg);
            }
            else {
                throw new DataAccessException("Failed to set activity instance status to cancel", ex);
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public void holdActivityInstance(ActivityInstance actInst, Long procId) throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            engineImpl.holdActivityInstance(actInst, procId);
        } catch (MdwException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).holdActivityInstance(actInst, procId);
            }
            else {
                throw ex;
            }
        } catch (SQLException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).holdActivityInstance(actInst, procId);
            }
            else {
                throw new DataAccessException("Failed to set activity instance status to hold", ex);
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public CompletionCode finishActivityInstance(BaseActivity activity, ProcessInstance pi, ActivityInstance ai,
            InternalEvent event, boolean bypassWait) throws ProcessException, DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.finishActivityInstance(activity, pi, ai, event, bypassWait);
        }
        catch (MdwException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).finishActivityInstance(activity, pi, ai, event, bypassWait);
            }
            else {
                throw ex;
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public ActivityRuntime resumeActivityPrepare(ProcessInstance procInst, InternalEvent event, boolean resumeOnHold)
            throws ProcessException, DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.resumeActivityPrepare(procInst, event, resumeOnHold);
        }
        catch (MdwException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).resumeActivityPrepare(procInst, event, resumeOnHold);
            }
            else {
                throw ex;
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public boolean resumeActivityExecute(ActivityRuntime ar, InternalEvent event, boolean resumeOnHold)
        throws ActivityException, DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.resumeActivityExecute(ar, event, resumeOnHold);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void resumeActivityFinish(ActivityRuntime ar, boolean finished, InternalEvent event, boolean resumeOnHold)
            throws ProcessException,DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            engineImpl.resumeActivityFinish(ar, finished, event, resumeOnHold);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void resumeActivityException(ProcessInstance procInst, Long actInstId, BaseActivity activity,
            Throwable cause) {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            engineImpl.resumeActivityException(procInst, actInstId, activity, cause);
        } catch (DataAccessException ex) {
            StandardLogger logger = LoggerUtil.getStandardLogger();
            logger.error("Failed to process resume activity exception", ex);
        } finally {
            try {
                stopTransaction(transaction);
            } catch (DataAccessException ex) {
                StandardLogger logger = LoggerUtil.getStandardLogger();
                logger.error("Fail to stop transaction in resumeActivityException", ex);
            }
        }
    }

    public Long getRequestCompletionTime(String ownerType, Long ownerId) throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.getDataAccess().getRequestCompletionTime(ownerType, ownerId);
        } catch (SQLException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).getRequestCompletionTime(ownerType, ownerId);
            }
            else {
                throw new DataAccessException("Failed to get request completion time", ex);
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public void setElapsedTime(String ownerType, Long instanceId, Long elapsedTime) throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            engineImpl.getDataAccess().setElapsedTime(ownerType, instanceId, elapsedTime);
        } catch (SQLException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).setElapsedTime(ownerType, instanceId, elapsedTime);
            }
            else {
                throw new DataAccessException("Failed to set elapsed time", ex);
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public void setActivityInstanceStatus(ActivityInstance actInst, Integer status, String message)
            throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            engineImpl.getDataAccess().setActivityInstanceStatus(actInst, status, message);
        } catch (DataAccessException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).setActivityInstanceStatus(actInst, status, message);
            }
            else {
                throw ex;
            }
        } catch (SQLException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).setActivityInstanceStatus(actInst, status, message);
            }
            else {
                throw new DataAccessException("Failed to set activity instance status", ex);
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public void updateActivityInstanceEndTime(Long actInstId, Date endtime) throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            engineImpl.getDataAccess().updateActivityInstanceEndTime(actInstId, endtime);
        } catch (DataAccessException ex) {
                if (canRetryTransaction(ex)) {
                    transaction = (TransactionWrapper)initTransactionRetry(transaction);
                    ((ProcessExecutor)getTransactionRetrier()).updateActivityInstanceEndTime(actInstId, endtime);
                }
                else {
                    throw ex;
                }
        } catch (SQLException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).updateActivityInstanceEndTime(actInstId, endtime);
            }
            else {
                throw new DataAccessException("Failed to update activity end time", ex);
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public int countActivityInstances(Long procInstId, Long activityId) throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.getDataAccess().countActivityInstances(procInstId, activityId);
        } catch (DataAccessException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).countActivityInstances(procInstId, activityId);
            }
            else {
                throw ex;
            }
        } catch (SQLException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).countActivityInstances(procInstId, activityId);
            }
            else {
                throw new DataAccessException("Failed to count activity instances", ex);
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public void createTransitionInstances(ProcessInstance processInstance, List<Transition> transitions,
            Long fromActivityInstanceId) throws ProcessException, DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            engineImpl.createTransitionInstances(processInstance, transitions, fromActivityInstanceId);
        } finally {
            stopTransaction(transaction);
        }
    }

    public void determineCompletedTransitions(Long processInstanceId, List<Transition> transitions)
            throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            engineImpl.getDataAccess().determineCompletedTransitions(processInstanceId, transitions);
        } catch (DataAccessException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).determineCompletedTransitions(processInstanceId, transitions);
            }
            else {
                throw ex;
            }
        } catch (SQLException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).determineCompletedTransitions(processInstanceId, transitions);
            }
            else {
                throw new DataAccessException("Failed to determine completed transitions", ex);
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public void updateDocumentInfo(DocumentReference docRef, String documentType, String ownerType, Long ownerId,
            Integer statusCode, String statusMessage) throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            engineImpl.updateDocumentInfo(docRef, documentType, ownerType, ownerId, statusCode, statusMessage);
        } catch (DataAccessException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).updateDocumentInfo(docRef, documentType, ownerType, ownerId, statusCode, statusMessage);
            }
            else {
                throw ex;
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public VariableInstance createVariableInstance(ProcessInstance pi, String varName, Object value)
            throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.createVariableInstance(pi, varName, value);
        } catch (DataAccessException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).createVariableInstance(pi, varName, value);
            }
            else {
                throw ex;
            }
        } catch (SQLException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).createVariableInstance(pi, varName, value);
            }
            else {
                throw new DataAccessException("Failed to create variable instance", ex);
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public void updateVariableInstance(VariableInstance variableInstance, Package pkg) throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            engineImpl.getDataAccess().updateVariableInstance(variableInstance, pkg);
        } catch (DataAccessException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).updateVariableInstance(variableInstance, pkg);
            }
            else {
                throw ex;
            }
        } catch (SQLException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).updateVariableInstance(variableInstance, pkg);
            }
            else {
                throw new DataAccessException("Failed to update variable instance", ex);
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public VariableInstance getVariableInstance(Long procInstId, String varName) throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.getDataAccess().getVariableInstance(procInstId, varName);
        } catch (DataAccessException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).getVariableInstance(procInstId, varName);
            }
            else {
                throw ex;
            }
        } catch (SQLException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).getVariableInstance(procInstId, varName);
            }
            else {
                throw new DataAccessException("Failed to get variable instance", ex);
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public Document getDocument(DocumentReference docRef, boolean forUpdate) throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.getDocument(docRef, forUpdate);
        } catch (DataAccessException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).getDocument(docRef, forUpdate);
            }
            else {
                throw ex;
            }
        } finally {
            if (!forUpdate)  // Do not release the locked "for update" document row
                stopTransaction(transaction);
        }
    }

    /**
     * Always goes to the database.
     */
    public Document loadDocument(DocumentReference docRef, boolean forUpdate) throws DataAccessException {
        TransactionWrapper transaction=null;
        try {
            transaction = startTransaction();
            return engineImpl.loadDocument(docRef, forUpdate);
        } catch (DataAccessException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).loadDocument(docRef, forUpdate);
            }
            else {
                throw ex;
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public DocumentReference createDocument(String variableType, String ownerType, Long ownerId, Object docObj, Package pkg)
            throws DataAccessException {
        return createDocument(variableType, ownerType, ownerId, null, null, docObj, pkg);
    }

    public DocumentReference createDocument(String variableType, String ownerType, Long ownerId, Integer statusCode,
            String statusMessage, Object docObj, Package pkg) throws DataAccessException {
        return createDocument(variableType, ownerType, ownerId, statusCode, statusMessage, null, docObj, pkg);
    }

    public DocumentReference createDocument(String variableType, String ownerType, Long ownerId, Integer statusCode,
            String statusMessage, String path, Object docObj, Package pkg) throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.createDocument(variableType, ownerType, ownerId, statusCode, statusMessage, path, docObj, pkg);
        } catch (DataAccessException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).createDocument(variableType, ownerType, ownerId, statusCode, statusMessage, path, docObj, pkg);
            }
            else {
                throw ex;
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public void updateDocumentContent(DocumentReference docRef, Object doc, Package pkg)
            throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            engineImpl.updateDocumentContent(docRef, doc, pkg);
        } catch (DataAccessException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).updateDocumentContent(docRef, doc, pkg);
            }
            else {
                throw ex;
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public void addDocumentToCache(Document doc) {
        if (engineImpl.getDataAccess() instanceof EngineDataAccessCache) {
            ((EngineDataAccessCache)engineImpl.getDataAccess()).addDocumentToCache(doc);
        }
    }

    public void cancelEventWaitInstances(Long activityInstanceId) throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            engineImpl.cancelEventWaitInstances(activityInstanceId);
        } catch (DataAccessException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).cancelEventWaitInstances(activityInstanceId);
            }
            else {
                throw ex;
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public boolean deleteInternalEvent(String eventName) throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.deleteInternalEvent(eventName);
        } catch (DataAccessException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).deleteInternalEvent(eventName);
            }
            else {
                throw new DataAccessException("Failed to delete internal event" + eventName, ex);
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public EventWaitInstance createEventWaitInstance(Long procInstId, Long actInstId, String eventName,
            String compCode, boolean recurring, boolean notifyIfArrived, boolean reregister)
    throws DataAccessException, ProcessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.createEventWaitInstance(procInstId, actInstId,
                    eventName, compCode, recurring, notifyIfArrived, reregister);
        }
        catch (MdwException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).createEventWaitInstance(procInstId, actInstId, eventName, compCode, recurring, notifyIfArrived, reregister);
            }
            else {
                throw ex;
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public EventWaitInstance createEventWaitInstance(Long procInstId, Long actInstId, String eventName,
            String compCode, boolean recurring, boolean notifyIfArrived)
    throws DataAccessException, ProcessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.createEventWaitInstance(procInstId, actInstId, eventName, compCode, recurring, notifyIfArrived);
        }
        catch (MdwException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).createEventWaitInstance(procInstId, actInstId,
                        eventName, compCode, recurring, notifyIfArrived);
            }
            else {
                throw ex;
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public EventWaitInstance createEventWaitInstances(Long procInstId, Long actInstId, String[] eventNames,
            String[] wakeUpEventTypes, boolean[] eventOccurances, boolean notifyIfArrived, boolean reregister)
            throws DataAccessException, ProcessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.createEventWaitInstances(procInstId, actInstId, eventNames, wakeUpEventTypes,
                    eventOccurances, notifyIfArrived, reregister);
        }
        catch (MdwException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).createEventWaitInstances(procInstId, actInstId,
                        eventNames, wakeUpEventTypes, eventOccurances, notifyIfArrived, reregister);
            }
            else {
                throw ex;
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public void removeEventWaitForActivityInstance(Long activityInstanceId, String reason) throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            engineImpl.getDataAccess().removeEventWaitForActivityInstance(activityInstanceId, reason);
        } catch (DataAccessException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).removeEventWaitForActivityInstance(activityInstanceId, reason);
            }
            else {
                throw ex;
            }
        } catch (SQLException ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                ((ProcessExecutor)getTransactionRetrier()).removeEventWaitForActivityInstance(activityInstanceId, reason);
            }
            else {
                throw new DataAccessException("Failed to remove event wait for activity instance", ex);
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public Integer notifyProcess(String eventName, Long docId, String message, int delay)
            throws DataAccessException, EventException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.notifyProcess(eventName, docId, message, delay);
        } catch (DataAccessException | EventException ex){
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).notifyProcess(eventName, docId, message, delay);
            }
            else {
                throw ex;
            }
        } catch (SQLException ex){
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).notifyProcess(eventName, docId, message, delay);
            }
            else {
                throw new DataAccessException("Failed to notify process of event arrival", ex);
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public void sendDelayedInternalEvent(InternalEvent event, int delaySeconds, String msgid, boolean isUpdate)
        throws MdwException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            engineImpl.sendDelayedInternalEvent(event, delaySeconds, msgid, isUpdate);
        } finally {
            stopTransaction(transaction);
        }
    }

    public Response getSynchronousProcessResponse(Long procInstId, String varName, Package pkg)
            throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.getServiceProcessResponse(procInstId, varName, pkg);
        } catch (Exception ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).getSynchronousProcessResponse(procInstId, varName, pkg);
            }
            else {
                throw new DataAccessException("Failed to get value for variable " + varName, ex);
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    public Map<String, String> getOutPutParameters(Long procInstId, Long procId) throws DataAccessException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            return engineImpl.getOutputParameters(procInstId, procId);
        } catch (Exception ex) {
            if (canRetryTransaction(ex)) {
                transaction = (TransactionWrapper)initTransactionRetry(transaction);
                return ((ProcessExecutor)getTransactionRetrier()).getOutPutParameters(procInstId, procId);
            }
            else {
                throw new DataAccessException("Failed to get output parameters:" + procInstId, ex);
            }
        } finally {
            stopTransaction(transaction);
        }
    }

    /**
     * This method must be called within the same transaction scope (namely engine is already started
     */
    public Integer lockActivityInstance(Long actInstId) throws DataAccessException {
        try {
            if (!isInTransaction())
                throw new DataAccessException("Cannot lock activity instance without a transaction");
            return engineImpl.getDataAccess().lockActivityInstance(actInstId);
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to lock activity instance", ex);
        }
    }

    /**
     * this method must be called within the same transaction scope (namely engine is already started
     */
    public Integer lockProcessInstance(Long procInstId) throws DataAccessException {
        try {
            if (!isInTransaction())
                throw new DataAccessException("Cannot lock activity instance without a transaction");
            return engineImpl.getDataAccess().lockProcessInstance(procInstId);
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to lock process instance", ex);
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
        if (db == null)
            throw new SQLException("Engine database is not open");
        return db;
    }

    public TransactionWrapper startTransaction() throws DataAccessException {
        return engineImpl.getDataAccess().startTransaction();
    }

    public void stopTransaction(TransactionWrapper transaction) throws DataAccessException {
        engineImpl.getDataAccess().stopTransaction(transaction);
    }

    public void rollbackTransaction() {
        engineImpl.getDatabaseAccess().rollback();
    }

    private boolean isInTransaction() {
        try {
            return TransactionUtil.getInstance().isInTransaction();
        } catch (SystemException ex) {
            StandardLogger logger = LoggerUtil.getStandardLogger();
            logger.error("Fail to check if inside transaction - treated as not", ex);
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
            LoggerUtil.getStandardLogger().error(ex.getMessage(), ex);
        }
        finally {
            try {
                stopTransaction(transaction);
            }
            catch (DataAccessException ex) {
                LoggerUtil.getStandardLogger().error(ex.getMessage(), ex);
            }
        }
    }

    public boolean canRetryTransaction(Throwable th) {
        if (transactionRetryMax < 0)
            transactionRetryMax = PropertyManager.getIntegerProperty(PropertyNames.MDW_TRANSACTION_RETRY_MAX, 3);

        if (transactionRetryCount < transactionRetryMax) {
            if (engineImpl.getDatabaseAccess().isMySQL()) {
                if (th.getMessage() != null && th.getMessage().contains("try restarting transaction")) {
                    ProcessExecutorImpl.logger.warn("Encountered the following MySQL issue - will retry transaction (Attempt " + (transactionRetryCount+1) + " of " + transactionRetryMax + "): " + th.getMessage());
                    return true;
                }
                if (th.getCause() != null)
                    return canRetryTransaction(th.getCause());
            }
        }
        else {
            ProcessExecutorImpl.logger.error("Encountered the following issue - will NOT retry transaction (Max attempts reached): " + th.getMessage());
        }
        return false;
    }

    public Object getTransactionRetrier() {
        ProcessExecutor retrier = this;
        if (transactionRetryCount == 0) {
            retrier = new ProcessExecutor(engineImpl);
            retrier.setTransactionRetryCount(1);
        }
        else {
            retrier.setTransactionRetryCount(transactionRetryCount + 1);
        }

        return retrier;
    }

    public Object initTransactionRetry(TransactionWrapper transaction) throws DataAccessException {
        rollbackTransaction();
        stopTransaction(transaction);

        if (transactionRetryInterval < 0)
            transactionRetryInterval = PropertyManager.getIntegerProperty(PropertyNames.MDW_TRANSACTION_RETRY_INTERVAL, 1000);

        try {
            Thread.sleep(transactionRetryInterval);
        }
        catch (InterruptedException e) {
            // If interrupted, continue with transaction retry
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
