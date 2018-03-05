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
package com.centurylink.mdw.services;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.event.EventInstance;
import com.centurylink.mdw.model.event.EventLog;
import com.centurylink.mdw.model.monitor.ScheduledEvent;
import com.centurylink.mdw.model.monitor.UnscheduledEvent;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.TransitionInstance;
import com.centurylink.mdw.services.event.ServiceHandler;
import com.centurylink.mdw.services.event.WorkflowHandler;

public interface EventManager {

    public void createAuditLog(UserAction userAction)
    throws DataAccessException, EventException;

    public Long createEventLog(String pEventName, String pEventCategory, String pEventSubCat, String pEventSource,
        String pEventOwner, Long pEventOwnerId, String user, String modUser, String comments)
    throws DataAccessException, EventException;

    public List<EventLog> getEventLogs(String pEventName, String pEventSource,
            String pEventOwner, Long pEventOwnerId) throws DataAccessException;

    public Integer notifyProcess(String pEventName, Long pEventInstId, String message, int delay)
    throws DataAccessException, EventException;

    /**
     * get variable instance by its ID
     */
    public VariableInstance getVariableInstance(Long varInstId)
    throws DataAccessException;

    /**
     * Get variable instance for a process instance.
     * The method does not take care of embedded process, so the caller needs to pass
     * the parent process instance id when looking for variables for embedded process
     */
    public VariableInstance getVariableInstance(Long procInstId, String name)
    throws DataAccessException;

    /**
     * Set the variable instance value.
     * The method does not take care of document variables, for which you must
     * pass in DocumentReferenceObject.
     * The method does not take care of embedded process, so the caller needs to pass
     * the parent process instance id when looking for variables for embedded process
     *
     * @param procInstId
     * @param name
     * @param value
     * @return
     * @throws DataAccessException
     */
    public VariableInstance setVariableInstance(Long procInstId, String name, Object value)
    throws DataAccessException;

    public Document getDocumentVO(Long documentId)
    throws DataAccessException;

    public void updateDocumentContent(Long docid, Object doc, String type, Package pkg)
    throws DataAccessException;

    public void updateDocumentInfo(Long docid, String documentType, String ownerType, Long ownerId)
    throws DataAccessException;

    public Long createDocument(String type, String ownerType, Long ownerId, Object doc, Package pkg)
    throws DataAccessException;

    public void sendDelayEventsToWaitActivities(String masterRequestId)
    throws DataAccessException, ProcessException;

    public void retryActivity(Long activityId, Long activityInstId)
    throws DataAccessException, ProcessException;

    /**
     * Skip the activity by sending an activity finish event, with the given completion code.
     * The status of the activity instance is ???
     */
    public void skipActivity(Long activityId, Long activityInstId, String completionCode)
    throws DataAccessException, ProcessException;

    /**
     * Returns the transition instance object by ID
     *
     * @param transInstId
     * @return transition instance object
     */
    public TransitionInstance getWorkTransitionInstance(Long transInstId)
    throws DataAccessException, ProcessException;

    /**
     * Returns the ActivityInstance identified by the passed in Id
     *
     * @param pActivityInstId
     * @return ActivityInstance
     */
    public ActivityInstance getActivityInstance(Long pActivityInstId)
    throws ProcessException, DataAccessException;

    /**
     * Returns the activity instances by process name, activity logical ID, and  master request ID.
     *
     * @param processName
     * @param activityLogicalId
     * @param masterRequestId
     * @return the list of activity instances. If the process definition or the activity
     *         with the given logical ID is not found, null
     *         or no such activity instances are found, an empty list is returned.
     * @throws ProcessException
     * @throws DataAccessException
     */
    public List<ActivityInstance> getActivityInstances(String masterRequestId,
            String processName, String activityLogicalId)
    throws ProcessException, DataAccessException;

    /**
     * Returns the Process instance object identified by the passed in Id
     *
     * @param procInstId
     * @return ProcessInstanceVO
     */
    public ProcessInstance getProcessInstance(Long procInstId)
    throws ProcessException, DataAccessException;

    /**
     * Returns the process instances by process name and master request ID.
     *
     * @param processName
     * @param masterRequestId
     * @return the list of process instances. If the process definition is not found, null
     *         is returned; if process definition is found but no process instances are found,
     *         an empty list is returned.
     * @throws ProcessException
     * @throws DataAccessException
     */
    public List<ProcessInstance> getProcessInstances(String masterRequestId, String processName)
    throws ProcessException, DataAccessException;

    /**
     * Load all internal event and scheduled jobs before cutoff time.
     * If cutoff time is null, load only unscheduled events
     */
    public List<ScheduledEvent> getScheduledEventList(Date cutoffTime)
    throws DataAccessException;

    /**
     * Load all internal events older than the specified time up to a max of batchSize.
     */
    public List<UnscheduledEvent> getUnscheduledEventList(Date olderThan, int batchSize)
    throws DataAccessException;

    public void offerScheduledEvent(ScheduledEvent event)
    throws DataAccessException;

    public void processScheduledEvent(String eventName, Date now)
    throws DataAccessException;

    public boolean processUnscheduledEvent(String eventName);

    public List<UnscheduledEvent> processInternalEvents(List<UnscheduledEvent> eventList);

    public void updateEventInstance(String eventName,
            Long documentId, Integer status, Date consumeDate, String auxdata, String reference, int preserveSeconds)
    throws DataAccessException;

    public void updateEventInstance(String eventName,
            Long documentId, Integer status, Date consumeDate, String auxdata, String reference, int preserveSeconds, String comments)
    throws DataAccessException;

    public EventInstance getEventInstance(String eventName)
    throws DataAccessException;

    /**
     * Register a service handler to respond to MDW listener requests.
     * The handlers's getProtocol() and getPath() methods are used to uniquely
     * identify the types of requests that it responds to.
     */
    public void registerServiceHandler(ServiceHandler handler)
    throws EventException;

    public void unregisterServiceHandler(ServiceHandler handler)
    throws EventException;

    public ServiceHandler getServiceHandler(String protocol, String path)
    throws EventException;

    /**
     * Register a workflow handler to respond to activity triggers.
     * The handlers's getAsset() and getParameters() methods are used to uniquely identify
     * the types of flows the handler responds to.
     * Note: asset should include the workflow package (eg: MyPackage/MyCamelRoute.xml).
     */
    public void registerWorkflowHandler(WorkflowHandler handler)
    throws EventException;

    public void unregisterWorkflowHandler(WorkflowHandler handler)
    throws EventException;

    public WorkflowHandler getWorkflowHandler(String asset, Map<String,String> parameters)
    throws EventException;

    public Process findProcessByProcessInstanceId(Long processInstanceId)
    throws DataAccessException, ProcessException;

}
