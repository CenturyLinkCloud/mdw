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

import org.json.JSONException;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.event.Event;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.workflow.ActivityCount;
import com.centurylink.mdw.model.workflow.ActivityImplementor;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.ActivityList;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessCount;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.ProcessList;
import com.centurylink.mdw.model.workflow.ProcessRun;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;

public interface WorkflowServices {

    public static final int PAGE_SIZE = 50; // must match Query.DEFAULT_MAX

    public Map<String,String> getAttributes(String ownerType, Long ownerId) throws ServiceException;
    /**
     * Replace <b>all</b> attributes for this ownerId
     * @param ownerType
     * @param ownerId Id of owner
     * @param attributes new attributes to add
     * @throws ServiceException
     */
    public void setAttributes(String ownerType, Long ownerId, Map<String,String> attributes) throws ServiceException;
    /**
     * <p>
     * Update specific attributes <b>without</b> clearing all attributes first
     *
     * This method can be used to update a subset of attributes without
     * removing <b>all</b> attributes for this ownerId first
     * </p>
     * @param ownerType
     * @param ownerId
     * @param attributes
     * @throws ServiceException
     */
    public void updateAttributes(String ownerType, Long ownerId, Map<String,String> attributes) throws ServiceException;
    public Map<String,String> getValues(String ownerType, String ownerId) throws ServiceException;
    /**
     * Replace <b>all</b> values for this ownerId
     * @param ownerType
     * @param ownerId Id of owner
     * @param values new values to add
     * @throws ServiceException
     */
    public void setValues(String ownerType, String ownerId, Map<String,String> values) throws ServiceException;
    /**
     * Update specific values for this ownerId
     * @param ownerType
     * @param ownerId Id of owner
     * @param values new values to add
     * @throws ServiceException
     */
    public void updateValues(String ownerType, String ownerId, Map<String,String> values) throws ServiceException;

    /**
     * Get ValueHolder IDs for the specified name and pattern
     * @param valueName
     * @param valuePattern can be a value or a patter with wildcards (*)
     */
    public List<String> getValueHolderIds(String valueName, String valuePattern) throws ServiceException;

    /**
     * Get ValueHolder IDs for the specified name, pattern and ownerType
     * @param valueName
     * @param valuePattern can be a value or a patter with wildcards (*)
     * @param ownerType the value holder type
     */
    public List<String> getValueHolderIds(String valueName, String valuePattern, String ownerType) throws ServiceException;

    public void registerTaskWaitEvent(Long taskInstanceId, Event event)
            throws ServiceException;

    public void registerTaskWaitEvent(Long taskInstanceId, String eventName)
            throws ServiceException;

    /**
     * @param taskInstanceId (Task Instance Id for Task)
     * @param eventName
     * @param recurring
     * @param completionCode null for default outcome
     * @return
     * @throws ServiceException
     */
    public void registerTaskWaitEvent(Long taskInstanceId, String eventName,
            String completionCode) throws ServiceException;
    /**
     * @param activityInstanceId
     * @param action
     * @param completionCode
     */
    public void actionActivity(String activityInstanceId, String action, String completionCode)
            throws ServiceException;

    ProcessInstance getProcess(Long instanceId) throws ServiceException;
    ProcessInstance getProcess(Long instanceId, boolean withSubprocs) throws ServiceException;
    ProcessInstance getProcessForTrigger(Long triggerId) throws ServiceException;
    /**
     * If multiple matches, returns latest.
     */
    ProcessInstance getMasterProcess(String masterRequestId) throws ServiceException;

    ProcessRuntimeContext getContext(Long instanceId) throws ServiceException;

    Map<String,Value> getProcessValues(Long instanceId, boolean includeEmpty) throws ServiceException;
    Map<String,Value> getProcessValues(Long instanceId) throws ServiceException;
    /**
     * name can be an expression
     */
    Value getProcessValue(Long instanceId, String name) throws ServiceException;

    ProcessList getProcesses(Query query) throws ServiceException;

    public ActivityList getActivities(Query query) throws ServiceException;

    public List<ProcessCount> getTopThroughputProcesses(Query query) throws ServiceException;

    public Map<Date,List<ProcessCount>> getProcessInstanceBreakdown(Query query) throws ServiceException;

    public List<ActivityCount> getTopThroughputActivities(Query query) throws ServiceException;

    public Map<Date,List<ActivityCount>> getActivityInstanceBreakdown(Query query) throws ServiceException;

    public List<Process> getProcessDefinitions(Query query) throws ServiceException;
    public Process getProcessDefinition(String assetPath, Query query) throws ServiceException;
    public Process getProcessDefinition(Long id) throws ServiceException;

    public ActivityList getActivityDefinitions(Query query) throws ServiceException;

    public ActivityInstance getActivity(Long instanceId) throws ServiceException;

    public List<ActivityImplementor> getImplementors() throws ServiceException;
    public ActivityImplementor getImplementor(String className) throws ServiceException;

    public Long launchProcess(Process process, String masterRequestId, String ownerType,
            Long ownerId, Map<String, String> params) throws ServiceException;

    public Object invokeServiceProcess(String processName, Object masterRequest, String masterRequestId,
            Map<String,Object> parameters, Map<String,String> headers) throws ServiceException;
    /**
     * responseHeaders will be populated from process variable, if any
     */
    public Object invokeServiceProcess(String processName, Object masterRequest, String masterRequestId,
            Map<String,Object> parameters, Map<String,String> headers, Map<String,String> responseHeaders) throws ServiceException;
    public String invokeServiceProcess(Process process, String masterRequestId, String ownerType,
            Long ownerId, Map<String,String> params) throws ServiceException;

    public Integer notify(String event, String message, int delay) throws ServiceException;
    public Integer notify(Package runtimePackage, String eventName, Object eventMessage) throws ServiceException ;
    public Integer notify(Package runtimePackage, String eventName, Object eventMessage, int delay) throws ServiceException;

    public void setVariable(Long processInstanceId, String varName, Object value) throws ServiceException;
    public void setVariable(ProcessRuntimeContext context, String varName, Object value) throws ServiceException;
    public void setVariables(Long processInstanceId, Map<String,Object> values) throws ServiceException;
    public void setVariables(ProcessRuntimeContext context, Map<String,Object> values) throws ServiceException;
    public void setDocumentValue(ProcessRuntimeContext context, String varName, Object value) throws ServiceException;
    public void createDocument(ProcessRuntimeContext context, String varName, Object value) throws ServiceException;
    public void updateDocument(ProcessRuntimeContext context, String varName, Object value) throws ServiceException;

    public Document getDocument(Long id) throws ServiceException;

    /**
     * Converts a document to a string, applying a consistent format for XML and JSON.
     * Use when comparing document values (such as in Automated Tests).
     */
    public String getDocumentStringValue(Long id) throws ServiceException;

    public String getDocType(Object docObj);

    public ProcessRun runProcess(ProcessRun runRequest) throws ServiceException, JSONException;

    public void createProcess(String assetPath) throws ServiceException, JSONException;

}
