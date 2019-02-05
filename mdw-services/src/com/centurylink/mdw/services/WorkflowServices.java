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

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.report.Hotspot;
import com.centurylink.mdw.model.report.Insight;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.event.Event;
import com.centurylink.mdw.model.report.Timepoint;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.*;
import org.json.JSONException;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public interface WorkflowServices {

    static final int PAGE_SIZE = 50; // must match Query.DEFAULT_MAX

    Map<String,String> getAttributes(String ownerType, Long ownerId) throws ServiceException;
    /**
     * Replace <b>all</b> attributes for this ownerId
     * @param ownerType
     * @param ownerId Id of owner
     * @param attributes new attributes to add
     * @throws ServiceException
     */
    void setAttributes(String ownerType, Long ownerId, Map<String,String> attributes) throws ServiceException;
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
    void updateAttributes(String ownerType, Long ownerId, Map<String,String> attributes) throws ServiceException;
    Map<String,String> getValues(String ownerType, String ownerId) throws ServiceException;
    String getValue(String ownerType, String ownerId, String name) throws ServiceException;
    /**
     * Replace <b>all</b> values for this ownerId
     * @param ownerType
     * @param ownerId Id of owner
     * @param values new values to add
     * @throws ServiceException
     */
    void setValues(String ownerType, String ownerId, Map<String,String> values) throws ServiceException;
    /**
     * Update specific values for this ownerId
     * @param ownerType
     * @param ownerId Id of owner
     * @param values new values to add
     * @throws ServiceException
     */
    void updateValues(String ownerType, String ownerId, Map<String,String> values) throws ServiceException;

    /**
     * Get ValueHolder IDs for the specified name and pattern
     * @param valueName
     * @param valuePattern can be a value or a patter with wildcards (*)
     */
    List<String> getValueHolderIds(String valueName, String valuePattern) throws ServiceException;

    /**
     * Get ValueHolder IDs for the specified name, pattern and ownerType
     * @param valueName
     * @param valuePattern can be a value or a patter with wildcards (*)
     * @param ownerType the value holder type
     */
    List<String> getValueHolderIds(String valueName, String valuePattern, String ownerType) throws ServiceException;

    void registerTaskWaitEvent(Long taskInstanceId, Event event)
            throws ServiceException;

    void registerTaskWaitEvent(Long taskInstanceId, String eventName)
            throws ServiceException;

    /**
     * @param taskInstanceId (Task Instance Id for Task)
     * @param eventName
     * @param completionCode null for default outcome
     * @return
     * @throws ServiceException
     */
    void registerTaskWaitEvent(Long taskInstanceId, String eventName,
            String completionCode) throws ServiceException;
    /**
     * @param activityInstanceId
     * @param action
     * @param completionCode
     */
    void actionActivity(Long activityInstanceId, String action, String completionCode, String uer)
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

    ActivityList getActivities(Query query) throws ServiceException;

    List<ProcessAggregate> getTopProcesses(Query query) throws ServiceException;
    TreeMap<Date,List<ProcessAggregate>> getProcessBreakdown(Query query) throws ServiceException;
    List<Insight> getProcessInsights(Query query) throws ServiceException;
    List<Timepoint> getProcessTrend(Query query) throws ServiceException;
    List<Hotspot> getProcessHotspots(Query query) throws ServiceException;

    List<ActivityAggregate> getTopActivities(Query query) throws ServiceException;
    TreeMap<Date,List<ActivityAggregate>> getActivityBreakdown(Query query) throws ServiceException;

    List<Process> getProcessDefinitions(Query query) throws ServiceException;
    Process getProcessDefinition(String assetPath, Query query) throws ServiceException;
    Process getProcessDefinition(Long id) throws ServiceException;

    ActivityList getActivityDefinitions(Query query) throws ServiceException;

    ActivityInstance getActivity(Long instanceId) throws ServiceException;

    List<ActivityImplementor> getImplementors() throws ServiceException;
    ActivityImplementor getImplementor(String className) throws ServiceException;

    Long launchProcess(String name, String masterRequestId, String ownerType,
            Long ownerId, Map<String,Object> params) throws ServiceException;

    Long launchProcess(Process process, String masterRequestId, String ownerType,
            Long ownerId, Map<String,String> params) throws ServiceException;

    Object invokeServiceProcess(String processName, Object masterRequest, String masterRequestId,
            Map<String,Object> parameters, Map<String,String> headers) throws ServiceException;
    String invokeServiceProcess(Process process, String masterRequestId, String ownerType,
            Long ownerId, Map<String,String> params, Map<String,String> headers) throws ServiceException;
    /**
     * responseHeaders will be populated from process variable, if any
     */
    Object invokeServiceProcess(String processName, Object masterRequest, String masterRequestId,
            Map<String,Object> parameters, Map<String,String> headers, Map<String,String> responseHeaders) throws ServiceException;
    String invokeServiceProcess(Process process, String masterRequestId, String ownerType,
            Long ownerId, Map<String,String> params) throws ServiceException;

    Integer notify(String event, String message, int delay) throws ServiceException;
    Integer notify(Package runtimePackage, String eventName, Object eventMessage) throws ServiceException ;
    Integer notify(Package runtimePackage, String eventName, Object eventMessage, int delay) throws ServiceException;

    void setVariable(Long processInstanceId, String varName, Object value) throws ServiceException;
    void setVariable(ProcessRuntimeContext context, String varName, Object value) throws ServiceException;
    void setVariables(Long processInstanceId, Map<String,Object> values) throws ServiceException;
    void setVariables(ProcessRuntimeContext context, Map<String,Object> values) throws ServiceException;
    void setDocumentValue(ProcessRuntimeContext context, String varName, Object value) throws ServiceException;
    void createDocument(ProcessRuntimeContext context, String varName, Object value) throws ServiceException;
    void updateDocument(ProcessRuntimeContext context, String varName, Object value) throws ServiceException;

    Document getDocument(Long id) throws ServiceException;

    /**
     * Converts a document to a string, applying a consistent format for XML and JSON.
     * Use when comparing document values (such as in Automated Tests).
     */
    String getDocumentStringValue(Long id) throws ServiceException;

    String getDocType(Object docObj);

    ProcessRun runProcess(ProcessRun runRequest) throws ServiceException, JSONException;

    void createProcess(String assetPath, Query query) throws ServiceException, IOException;

    /**
     * Retrieve process definition for a specific instance from the document table.
     * Quickly returns null if no such definition exists.
     */
    Process getInstanceDefinition(String assetPath, Long instanceId) throws ServiceException;

    void saveInstanceDefinition(String assetPath, Long instanceId, Process process) throws ServiceException;
}
