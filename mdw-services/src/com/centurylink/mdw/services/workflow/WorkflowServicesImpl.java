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
package com.centurylink.mdw.services.workflow;

import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.app.Templates;
import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.translator.impl.JavaObjectTranslator;
import com.centurylink.mdw.common.translator.impl.StringDocumentTranslator;
import com.centurylink.mdw.common.translator.impl.YamlTranslator;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.*;
import com.centurylink.mdw.model.asset.AssetVersion;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.asset.api.AssetInfo;
import com.centurylink.mdw.model.event.Event;
import com.centurylink.mdw.model.event.EventInstance;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.report.Hotspot;
import com.centurylink.mdw.model.report.Insight;
import com.centurylink.mdw.model.report.Timepoint;
import com.centurylink.mdw.model.request.Response;
import com.centurylink.mdw.model.system.SysInfo;
import com.centurylink.mdw.model.system.SysInfoCategory;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.model.user.UserAction.Action;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.*;
import com.centurylink.mdw.service.data.WorkflowDataAccess;
import com.centurylink.mdw.service.data.activity.ActivityAggregation;
import com.centurylink.mdw.service.data.activity.ImplementorCache;
import com.centurylink.mdw.service.data.process.*;
import com.centurylink.mdw.services.EventServices;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.services.process.ActivityLogger;
import com.centurylink.mdw.services.process.ProcessEngineDriver;
import com.centurylink.mdw.services.process.ProcessExecutor;
import com.centurylink.mdw.translator.JsonTranslator;
import com.centurylink.mdw.translator.TranslationException;
import com.centurylink.mdw.translator.XmlDocumentTranslator;
import com.centurylink.mdw.util.JsonUtil;
import com.centurylink.mdw.util.TransactionWrapper;
import com.centurylink.mdw.util.log.ActivityLog;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.CodeTimer;
import com.centurylink.mdw.variable.VariableTranslator;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.json.JSONException;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class WorkflowServicesImpl implements WorkflowServices {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private WorkflowDataAccess getWorkflowDataAccess() {
        return new WorkflowDataAccess();
    }

    protected ProcessAggregation getProcessAggregation() {
        return new ProcessAggregation();
    }
    protected ActivityAggregation getActivityAggregation() {
        return new ActivityAggregation();
    }

    public Map<String,String> getAttributes(String ownerType, Long ownerId) throws ServiceException {
        try {
            return getWorkflowDataAccess().getAttributes(ownerType, ownerId);
        }
        catch (SQLException ex) {
            throw new ServiceException("Failed to load attributes for " + ownerType + "/" + ownerId, ex);
        }
    }

    public void setAttributes(String ownerType, Long ownerId, Map<String,String> attributes) throws ServiceException {
        try {
            getWorkflowDataAccess().setAttributes(ownerType, ownerId, attributes);
        }
        catch (Exception ex) {
            throw new ServiceException("Failed to set attributes for " + ownerType + "/" + ownerId, ex);
        }
    }

    /**
     * Update attributes without deleting all attributes for this ownerId first
     */
    public void updateAttributes(String ownerType, Long ownerId, Map<String,String> attributes)
            throws ServiceException {
        try {
            for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                String attributeName = attribute.getKey();
                String attributeValue = attribute.getValue();
                getWorkflowDataAccess().setAttribute(ownerType, ownerId, attributeName, attributeValue);
            }
        }
        catch (SQLException ex) {
            throw new ServiceException("Failed to update attributes for " + ownerType + "/" + ownerId, ex);
        }
    }
    /**
     * Replace all existing values
     */
    public void setValues(String ownerType, String ownerId, Map<String,String> values) throws ServiceException {
        try {
            CommonDataAccess dao = new CommonDataAccess();
            dao.setValues(ownerType, ownerId, values);
        }
        catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * Update certain values
     */
    public void updateValues(String ownerType, String ownerId, Map<String,String> values) throws ServiceException {
        try {
            CommonDataAccess dao = new CommonDataAccess();
            for (String key : values.keySet())
                dao.setValue(ownerType, ownerId, key, values.get(key));
        }
        catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public Map<String,String> getValues(String ownerType, String ownerId) throws ServiceException {
        try {
            if (OwnerType.SYSTEM.equals(ownerType)) {
                if ("mdwProperties".equals(ownerId)) {
                    Map<String,String> mdwProps = new HashMap<>();
                    SysInfoCategory cat = ServiceLocator.getSystemServices().getMdwProperties();
                    for (SysInfo sysInfo : cat.getSysInfos()) {
                        mdwProps.put(sysInfo.getName(), sysInfo.getValue());
                    }
                    return mdwProps;
                }
                else {
                    throw new ServiceException(ServiceException.BAD_REQUEST, "Unsupported System values: " + ownerId);
                }
            }
            else {
                CommonDataAccess dao = new CommonDataAccess();
                return dao.getValues(ownerType, ownerId);
            }
        }
        catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public VariableInstance getVariableInstance(long processInstanceId, String variableName) throws ServiceException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            return edao.getVariableInstance(processInstanceId, variableName);
        } catch (SQLException | DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Failed to retrieve variable instance: "
                    + processInstanceId + ", " + variableName, ex);
        } finally {
            try {
                edao.stopTransaction(transaction);
            } catch (DataAccessException ex) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
            }
        }
    }

    public List<String> getValueHolderIds(String valueName, String valuePattern) throws ServiceException {
        return getValueHolderIds(valueName, valuePattern, null);
    }

    public List<String> getValueHolderIds(String valueName, String valuePattern, String ownerType)
            throws ServiceException {
        try {
            CommonDataAccess dao = new CommonDataAccess();
            if (ownerType == null)
                return dao.getValueOwnerIds(valueName, valuePattern);
            else
                return dao.getValueOwnerIds(ownerType, valueName, valuePattern);
        }
        catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public void registerTaskWaitEvent(Long taskInstanceId, Event event) throws ServiceException {
        registerTaskWaitEvent(taskInstanceId, event.getId(), event.getCompletionCode() == null ? "FINISH" :
                event.getCompletionCode());
    }

    public void registerTaskWaitEvent(Long taskInstanceId, String eventName) throws ServiceException {
        registerTaskWaitEvent(taskInstanceId, eventName, "FINISH");
    }

    public void registerTaskWaitEvent(Long taskInstanceId, String eventName, String completionCode) throws ServiceException {
        try {
            TaskInstance taskInstance = ServiceLocator.getTaskServices().getInstance(taskInstanceId);
            if (taskInstance != null) {
                Long activityInstanceId = taskInstance.getActivityInstanceId();
                EngineDataAccess edao = new EngineDataAccessDB();
                InternalMessenger msgBroker = MessengerFactory.newInternalMessenger();
                ProcessExecutor engine = new ProcessExecutor(edao, msgBroker, false);
                // When registering an event on a Task via this method, the following rules apply:
                // - Event cannot be a legacy "recurring" event (means there can be multiple waiter for same event)
                // - Event will not trigger notification if pre-arrived. Notification will occur when event is published
                // after this registration occurs
                engine.createEventWaitInstance(taskInstance.getTaskInstanceId(), activityInstanceId, eventName,
                        completionCode, false, false, true);
            }
            else
                throw new ServiceException("Task Instance was not found for ID " + taskInstanceId);
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);

        }
    }

    @Override
    public void actionActivity(Long activityInstanceId, String action, String completionCode, String user)
            throws ServiceException {
        EngineDataAccessDB dataAccess = null;
        try {
            UserAction userAction = auditLog(action, UserAction.Entity.ActivityInstance, activityInstanceId, user,
                    completionCode);
            ActivityInstance activityInstance = ServiceLocator.getEventServices().getActivityInstance(activityInstanceId);
            ActivityLogger.persist(activityInstance.getProcessInstanceId(), activityInstanceId, StandardLogger.LogLevel.INFO,
                    "Activity actioned by user '" + user + "': " + action);
            if (Action.Proceed.toString().equalsIgnoreCase(action)) {
                ServiceLocator.getEventServices().skipActivity(null, activityInstanceId, completionCode);
            }
            else if (Action.Retry.toString().equalsIgnoreCase(action) ||
                    Action.Fail.toString().equalsIgnoreCase(action)) {
                if (Action.Retry.toString().equalsIgnoreCase(action))
                    ServiceLocator.getEventServices().retryActivity(activityInstance.getActivityId(), activityInstanceId);
                else if (activityInstance.getEndDate() == null) {// Only fail it if not yet completed
                    dataAccess = new EngineDataAccessDB();
                    dataAccess.getDatabaseAccess().openConnection();
                    dataAccess.setActivityInstanceStatus(activityInstance, WorkStatus.STATUS_FAILED,
                            "Manually Failed by user " + user);
                }
            }
            else if (Action.Resume.toString().equalsIgnoreCase(action)) {
                String eventName = "mdw.Resume-" + activityInstanceId;
                notify(eventName, userAction.getJson().toString(), 0);
            }
            else {
                throw new ServiceException("Unsupported action: '" + action + "' for activity " + activityInstanceId);
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
        finally {
            if (dataAccess != null)
                dataAccess.getDatabaseAccess().closeConnection();
        }
    }

    @Override
    public ActivityList getActivities(Query query) throws ServiceException {
        try {
            CodeTimer timer = new CodeTimer(true);
            ActivityList activityList = getWorkflowDataAccess().getActivityInstances(query);
            timer.logTimingAndContinue("getWorkflowDao().getActivityInstances()");
            activityList = populateActivities(activityList, query);
            timer.stopAndLogTiming("populateActivities()");
            return activityList;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, "Error retrieving activity instance for query: " + query, ex);
        }
    }

    public ProcessInstance getProcess(Long instanceId) throws ServiceException {
        return getProcess(instanceId, false);
    }

    public ProcessInstance getProcess(Long instanceId, boolean withSubprocs) throws ServiceException {
        try {
            WorkflowDataAccess dataAccess = getWorkflowDataAccess();
            ProcessInstance process = dataAccess.getProcessInstanceAll(instanceId);
            if (process == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Process instance not found: " + instanceId);
            if (withSubprocs) {
                // embedded subprocs
                Map<String,String> criteria = new HashMap<>();
                criteria.put("owner", OwnerType.MAIN_PROCESS_INSTANCE);
                criteria.put("ownerId", process.getId().toString());
                criteria.put("processId", process.getProcessId().toString());
                ProcessList subprocList = dataAccess.getProcessInstanceList(criteria, 0, Query.MAX_ALL,
                        "order by process_instance_id");
                if (subprocList != null && subprocList.getItems() != null && subprocList.getItems().size() > 0) {
                    List<ProcessInstance> subprocs = new ArrayList<>();
                    for (ProcessInstance subproc : subprocList.getItems()) {
                        ProcessInstance fullsub = dataAccess.getProcessInstance(subproc.getId());
                        fullsub.setProcessId(Long.parseLong(subproc.getComment()));
                        subprocs.add(fullsub);
                    }
                    process.setSubprocesses(subprocs);
                }
            }
            return process;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error retrieving process instance: " +
                    instanceId + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public ActivityLog getProcessLog(Long processInstanceId, boolean withActivities) throws ServiceException {
        try {
            return getWorkflowDataAccess().getProcessLog(processInstanceId, withActivities);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }
    }

    @Override
    public ActivityLog getProcessLog(Long processInstanceId, Long[] activityInstanceIds) throws ServiceException {
        try {
            return getWorkflowDataAccess().getProcessLog(processInstanceId, activityInstanceIds);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }
    }

    public ProcessInstance getMasterProcess(String masterRequestId) throws ServiceException {
        Query query = new Query();
        query.setFilter("master", true);
        query.setFilter("masterRequestId", masterRequestId);
        query.setSort("process_instance_id");
        query.setDescending(true);
        List<ProcessInstance> instances = getProcesses(query).getProcesses();
        if (instances.isEmpty())
            return null;
        else
            return getProcess(instances.get(0).getId());
    }

    public Map<String,Value> getProcessValues(Long instanceId) throws ServiceException {
        return getProcessValues(instanceId, false);
    }

    public Map<String,Value> getProcessValues(Long instanceId, boolean includeEmpty) throws ServiceException {
        ProcessRuntimeContext runtimeContext = getContext(instanceId);
        Map<String,Value> values = new HashMap<>();
        Map<String,Variable> varDefs = getVariableDefinitions(runtimeContext.getProcess().getVariables());

        Map<String,Object> variables = runtimeContext.getValues();
        if (variables != null) {
            for (String key : variables.keySet()) {
                String stringVal = runtimeContext.getValueAsString(key);
                if (stringVal != null) {
                    Variable varDef = varDefs.get(key);
                    Value value;
                    if (varDef != null)
                        value = varDef.toValue();
                    else
                        value = new Value(key);
                    value.setValue(stringVal);
                    values.put(key, value);
                }
            }
        }
        if (includeEmpty) {
            for (String name : varDefs.keySet()) {
                if (!values.containsKey(name))
                    values.put(name, varDefs.get(name).toValue());
            }
        }
        return values;
    }

    protected Map<String,Variable> getVariableDefinitions(List<Variable> varList) {
        Map<String,Variable> varDefs = new HashMap<>();
        if (varList != null) {
            for (Variable var : varList)
                varDefs.put(var.getName(), var);
        }
        return varDefs;
    }

    public Value getProcessValue(Long instanceId, String name) throws ServiceException {
        ProcessRuntimeContext runtimeContext = getContext(instanceId);
        Variable var = runtimeContext.getProcess().getVariable(name);
        if (var == null && !ProcessRuntimeContext.isExpression(name))
            throw new ServiceException(ServiceException.NOT_FOUND, "No variable defined: " + name);
        String stringVal = null;
        if (var != null && runtimeContext.getPackage().getTranslator(var.getType()).isDocumentReferenceVariable()) {
            VariableInstance varInst = runtimeContext.getProcessInstance().getVariable(name);
            // ensure consistent formatting for doc values
            String str = varInst.getStringValue(runtimeContext.getPackage());
            if (varInst != null && str != null && str.startsWith("DOCUMENT:"))
                stringVal = getDocumentStringValue(new DocumentReference(varInst.getStringValue(runtimeContext.getPackage())).getDocumentId(),
                        var.getType(), runtimeContext.getPackage());
        }
        else {
            stringVal = runtimeContext.getValueAsString(name);
        }
        if (stringVal == null) {
            throw new ServiceException(ServiceException.NOT_FOUND, "No value '" + name + "' found for instance: " + instanceId);
        }
        Variable varDef = getVariableDefinitions(runtimeContext.getProcess().getVariables()).get(name);
        Value value;
        if (varDef != null)
            value = varDef.toValue();
        else
            value = new Value(name);
        value.setValue(stringVal);
        return value;
    }

    public ProcessRuntimeContext getContext(Long instanceId) throws ServiceException {
        return getContext(instanceId, false);
    }

    public ProcessRuntimeContext getContext(Long instanceId, Boolean embeddedVars) throws ServiceException {
        ProcessInstance instance = getProcess(instanceId);
        // Edge case where we want to get main process variables when we loaded embedded process instance
        // Applies to when looking/setting process variables in manual task located in embedded subproc
        if (instance.isEmbedded() && embeddedVars)
            instance.setVariables(getProcess(instance.getOwnerId()).getVariables());
        Process process = null;
        if (instance.getInstanceDefinitionId() > 0L)
            process = ProcessCache.getInstanceDefinition(instance.getProcessId(), instance.getInstanceDefinitionId());
        if (process == null) {
            try {
                process = ProcessCache.getProcess(instance.getProcessId());
            } catch (IOException ex) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error loading process id " + instance.getProcessId(), ex);
            }
        }
        Package pkg = PackageCache.getPackage(process.getPackageName());
        if (process == null) {
            throw new ServiceException(ServiceException.NOT_FOUND, "Process definition not found for id: " + instance.getProcessId());
        }

        Map<String,Object> vars = new HashMap<>();
        try {
            if (instance.getVariables() != null) {
                for (VariableInstance var : instance.getVariables()) {
                    Object value = var.getData(pkg);
                    if (value instanceof DocumentReference) {
                        try {
                            Document doc = getWorkflowDataAccess().getDocument(((DocumentReference)value).getDocumentId());
                            value = doc == null ? null : doc.getObject(var.getType(), pkg);
                            vars.put(var.getName(), value);
                        }
                        catch (TranslationException ex) {
                            // parse error on one doc should not prevent other vars from populating
                            logger.error("Error translating " + var.getName() + " for process instance " + instanceId, ex);
                        }
                    }
                    else {
                        vars.put(var.getName(), value);
                    }
                }
            }
            return new ProcessRuntimeContext(null, pkg, process, instance, 0, false, vars);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    public long getProcessCount(Query query) throws ServiceException {
        try {
            return getWorkflowDataAccess().getProcessInstanceCount(query);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, "Error retrieving process instance count for query: " + query, ex);
        }
    }

    @Override
    public ProcessList getProcesses(Query query) throws ServiceException {
        try {
            String procDefSpec = query.getFilter("definition"); // in form of AssetVersionSpec
            if (procDefSpec != null) {
                AssetVersionSpec spec = AssetVersionSpec.parse(procDefSpec);
                if (spec.getName().endsWith(".proc")) {
                    spec = new AssetVersionSpec(spec.getPackageName(),
                            spec.getName().substring(0, spec.getName().length() - 5), spec.getVersion());
                }
                try {
                    Process procDef = ProcessCache.getProcessSmart(spec);
                    if (procDef == null) {
                        throw new ServiceException(ServiceException.NOT_FOUND, "Process definition not found for spec: " + procDefSpec);
                    }
                    query.setFilter("processId", procDef.getId());
                } catch (IOException ex) {
                    throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
                }
            }
            return getWorkflowDataAccess().getProcessInstances(query);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, "Error retrieving process instance for query: " + query, ex);
        }
    }

    public ProcessInstance getProcessForTrigger(Long triggerId) throws ServiceException {
        Document doc = getDocument(triggerId);
        String ownerContent = getDocumentStringValue(triggerId, doc.getVariableType(), getPackage(doc));
        try {
            JSONObject json = new JsonObject(ownerContent);
            if (!json.has("runtimeContext")) {
                throw new ServiceException(ServiceException.NOT_FOUND,
                        "Trigger document does not have RuntimeContext information: " + triggerId);
            }
            JSONObject runtimeContext = json.getJSONObject("runtimeContext");
            long procInstId;
            if (runtimeContext.has("activityInstance")) {
                procInstId = runtimeContext.getJSONObject("activityInstance").getLong("processInstanceId");
            }
            else if (runtimeContext.has("processInstance")) {
                procInstId = runtimeContext.getJSONObject("processInstance").getLong("id");
            }
            else {
                throw new ServiceException(ServiceException.NOT_FOUND,
                        "Trigger document does not have instance information: " + triggerId);
            }

            return getProcess(procInstId);
        }
        catch (JSONException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error retrieving triggerId: " + triggerId , ex);
        }
    }

    public Document getDocument(Long id) throws ServiceException {
        try {
            if (!getWorkflowDataAccess().isDocument(id))
                throw new ServiceException(ServiceException.NOT_FOUND, "Document not found: " + id);
            return getWorkflowDataAccess().getDocument(id);
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error retrieving document: " + id, ex);
        }
    }

    @Override
    public ActivityInstance getActivity(Long instanceId) throws ServiceException {
        try {
            ActivityInstance activityInstance = getWorkflowDataAccess().getActivityInstance(instanceId);
            if (activityInstance == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Activity instance not found: " + instanceId);
            return activityInstance;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, "Error retrieving activity instance: " + instanceId, ex);
        }
    }

    @Override
    public ActivityLog getActivityLog(Long activityInstanceId) throws ServiceException {
        try {
            return getWorkflowDataAccess().getActivityLog(activityInstanceId);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }
    }

    public List<ProcessAggregate> getTopProcesses(Query query) throws ServiceException {
        try {
            CodeTimer timer = new CodeTimer(true);
            List<ProcessAggregate> list = getProcessAggregation().getTops(query);
            timer.logTimingAndContinue("WorkflowServicesImpl.getTopProcesses()");
            if ("status".equals(query.getFilter("by"))) {
                list = populateProcessStatuses(list);
            }
            else {
                list = populateProcesses(list);
            }
            timer.stopAndLogTiming("WorkflowServicesImpl.getTopProcesses()");
            return list;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, "Error retrieving top throughput processes: query=" + query, ex);
        }
    }

    public TreeMap<Instant,List<ProcessAggregate>> getProcessBreakdown(Query query) throws ServiceException {
        try {
            TreeMap<Instant,List<ProcessAggregate>> map = getProcessAggregation().getBreakdown(query);
            if (query.getFilters().get("processIds") != null) {
                for (Instant instant : map.keySet())
                    populateProcesses(map.get(instant));
            }
            return map;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, "Error retrieving process breakdown: query=" + query, ex);
        }
    }

    public List<Insight> getProcessInsights(Query query) throws ServiceException {
        try {
            return new ProcessInsights().getInsights(query);
        }
        catch (SQLException | ParseException ex) {
            throw new ServiceException(500, "Error retrieving process insights: query=" + query, ex);
        }
    }

    public List<Timepoint> getProcessTrend(Query query) throws ServiceException {
        try {
            return new ProcessInsights().getTrend(query);
        }
        catch (SQLException | ParseException ex) {
            throw new ServiceException(500, "Error retrieving process trend: query=" + query, ex);
        }
    }

    public List<Hotspot> getProcessHotspots(Query query) throws ServiceException {
        try {
            return new ProcessHotspots().getHotspots(query);
        }
        catch (SQLException ex) {
            throw new ServiceException(500, "Error retrieving process hotspots: query=" + query, ex);
        }
    }

    /**
     * Fills in process header info from definition.
     */
    protected List<ProcessAggregate> populateProcesses(List<ProcessAggregate> processAggregates) {
        for (ProcessAggregate pc : processAggregates) {
            try {
                Process process = ProcessCache.getProcess(pc.getId());
                if (process == null)
                    throw new IOException("Missing definition for process id: " + pc.getId());
                pc.setName(process.getName());
                pc.setVersion(AssetVersion.formatVersion(process.getVersion()));
                pc.setPackageName(process.getPackageName());
            } catch (IOException ex) {
                pc.setName("Unknown");
                logger.error(ex.getMessage(), ex);
            }
        }
        return processAggregates;
    }

    protected List<ProcessAggregate> populateProcessStatuses(List<ProcessAggregate> processAggregates) {
        for (ProcessAggregate processAggregate : processAggregates) {
            processAggregate.setName(WorkStatuses.getName((int)processAggregate.getId()));
        }
        // add any empty statuses
        for (Integer statusCd : WorkStatuses.getWorkStatuses().keySet()) {
            if (!statusCd.equals(WorkStatus.STATUS_PENDING_PROCESS) && !statusCd.equals(WorkStatus.STATUS_HOLD)) {
                boolean found = processAggregates.stream().anyMatch(agg -> agg.getId() == statusCd);
                if (!found) {
                    ProcessAggregate processAggregate = new ProcessAggregate(0);
                    processAggregate.setId(statusCd);
                    processAggregate.setCount(0);
                    processAggregate.setName(WorkStatuses.getWorkStatuses().get(statusCd));
                    processAggregates.add(processAggregate);
                }
            }
        }
        return processAggregates;
    }

    /**
     * Fills in activity header info from process definition
     */
    protected ActivityList populateActivities(ActivityList activityList, Query query) {
        List<ActivityInstance> aList = activityList.getActivities();
        ArrayList<ActivityInstance> matchActivities = new ArrayList<>();
        String activityName = query.getFilter("activityName");
        for (ActivityInstance activityInstance : aList) {
            try {
                Process process = ProcessCache.getProcess(activityInstance.getProcessId());
                if (process == null)
                    throw new IOException("Missing definition for process id: " + activityInstance.getProcessId());
                String logicalId = activityInstance.getDefinitionId();
                Activity actdef = process.getActivity(logicalId);
                if (actdef == null)
                    throw new IOException("Missing activity for process  " + activityInstance.getProcessId() + ": " + logicalId);
                if (activityName != null && activityName.equals(actdef.getProcessName() + 'v' + process.getVersionString() + ':' + logicalId)) {
                    matchActivities.add(activityInstance);
                }
                activityInstance.setName(actdef.getName().replaceAll("\\r", "").replace('\n', ' '));
                activityInstance.setProcessName(process.getName());
                activityInstance.setProcessVersion(AssetVersion.formatVersion(process.getVersion()));
                activityInstance.setPackageName(process.getPackageName());
            }
            catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
                activityInstance.setName("Unknown (" + activityInstance.getDefinitionId() + ")");
            }
        }
        if (activityName != null)
            activityList.setActivities(matchActivities);
        activityList.setCount(activityList.getActivities().size());
        if (activityList.getTotal() <= 0L)
            activityList.setTotal(activityList.getActivities().size());
        return activityList;
    }

    /**
     * Fills in process header info from process definition.
     */
    protected List<ActivityAggregate> populateActivities(List<ActivityAggregate> activityCounts) {
        for (ActivityAggregate ac : activityCounts) {
            try {
                Process process = ProcessCache.getProcess(ac.getProcessId());
                if (process == null)
                    throw new IOException("Missing definition for process id: " + ac.getProcessId());
                ac.setProcessName(process.getName());
                ac.setVersion(AssetVersion.formatVersion(process.getVersion()));
                ac.setPackageName(process.getPackageName());
                String logicalId = ac.getDefinitionId();
                Activity actdef = process.getActivity(logicalId);
                if (actdef == null)
                    throw new IOException("Missing activity: " + logicalId);
                String actName = actdef.getName().replaceAll("\\r", "").replace('\n', ' ');
                ac.setActivityName(actName);
                ac.setProcessName(actdef.getProcessName()); // in case subproc
                ac.setName(ac.getProcessName() + ": " + ac.getActivityName());
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
                ac.setName("Unknown (" + ac.getDefinitionId() + ")");
            }
        }
        return activityCounts;
    }

    protected List<ActivityAggregate> populateActivityStatuses(List<ActivityAggregate> activityAggregates) {
        for (ActivityAggregate activityAggregate : activityAggregates) {
            activityAggregate.setName(WorkStatuses.getName((int)activityAggregate.getId()));
        }
        // add any empty statuses
        int[] statusCds = new int[]{WorkStatus.STATUS_IN_PROGRESS, WorkStatus.STATUS_FAILED, WorkStatus.STATUS_WAITING};
        for (int statusCd : statusCds) {
            boolean found = activityAggregates.stream().anyMatch(agg -> {
                return agg.getId() == statusCd;
            });
            if (!found) {
                ActivityAggregate activityAggregate = new ActivityAggregate(0);
                activityAggregate.setId(statusCd);
                activityAggregate.setCount(0);
                activityAggregate.setName(WorkStatuses.getWorkStatuses().get(statusCd));
                activityAggregates.add(activityAggregate);
            }
        }
        return activityAggregates;
    }

    public List<ActivityAggregate> getTopActivities(Query query) throws ServiceException {
        try {
            CodeTimer timer = new CodeTimer(true);
            List<ActivityAggregate> list = getActivityAggregation().getTops(query);
            timer.logTimingAndContinue("AggregateDataAccessVcs.getTopThroughputActivityInstances()");
            if ("status".equals(query.getFilter("by"))) {
                list = populateActivityStatuses(list);
            }
            else {
                list = populateActivities(list);
            }
            timer.stopAndLogTiming("WorkflowServicesImpl.populate()");
            return list;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, "Error retrieving top throughput activities: query=" + query, ex);
        }
    }

    public TreeMap<Instant,List<ActivityAggregate>> getActivityBreakdown(Query query) throws ServiceException {
        try {
            TreeMap<Instant,List<ActivityAggregate>> map = getActivityAggregation().getBreakdown(query);
            if (query.getFilters().get("activityIds") != null) {
                for (Instant instant : map.keySet())
                    populateActivities(map.get(instant));
            }
            return map;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, "Error retrieving activity instance breakdown: query=" + query, ex);
        }
    }

    public Long launchProcess(String name, String masterRequestId, String ownerType,
                              Long ownerId, Map<String,Object> parameters) throws ServiceException {
        try {
            Process process = ProcessCache.getProcess(name);
            ProcessEngineDriver driver = new ProcessEngineDriver();
            Map<String,String> params = translateParameters(process, parameters);
            return driver.startProcess(process.getId(), masterRequestId, ownerType, ownerId, params, null);
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    public Long launchProcess(Process process, String masterRequestId, String ownerType,
                              Long ownerId, Map<String,String> params) throws ServiceException {
        try {
            ProcessEngineDriver driver = new ProcessEngineDriver();
            return driver.startProcess(process.getId(), masterRequestId, ownerType, ownerId, params, null);
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    public Response invokeServiceProcess(String name, Object masterRequest, String masterRequestId,
            Map<String,Object> parameters, Map<String,String> headers) throws ServiceException {
        return invokeServiceProcess(name, masterRequest, masterRequestId, parameters, headers, null);
    }

    public Response invokeServiceProcess(String processName, Object masterRequest, String masterRequestId,
            Map<String,Object> parameters, Map<String,String> headers, Map<String,String> responseHeaders)
            throws ServiceException {
        try {
            Long docId = 0L;
            String request = null;
            Process process = ProcessCache.getProcess(processName);
            Package pkg = PackageCache.getPackage(process.getPackageName());
            if (masterRequest != null) {
                String varType = StringDocument.class.getName();
                if (masterRequest instanceof JSONObject)
                    varType = JSONObject.class.getName();
                else if (masterRequest instanceof Jsonable)
                    varType = Jsonable.class.getName();
                EventServices eventMgr = ServiceLocator.getEventServices();
                docId = eventMgr.createDocument(varType, OwnerType.LISTENER_REQUEST, 0L, masterRequest, pkg);
                request = pkg.getStringValue(varType, masterRequest, true);
                if (headers == null)
                    headers = new HashMap<>();
                headers.put(Listener.METAINFO_DOCUMENT_ID, docId.toString());
            }

            Map<String,String> stringParams = translateParameters(process, parameters);

            String responseVarName = "response";  // currently not configurable

            ProcessEngineDriver engineDriver = new ProcessEngineDriver();
            Response response = engineDriver.invokeService(process.getId(), OwnerType.DOCUMENT, docId, masterRequestId,
                    request, stringParams, responseVarName, headers);
            Variable responseHeadersVar = process.getVariable("responseHeaders");
            if (responseHeaders != null && responseHeadersVar != null && responseHeadersVar.getType().equals(
                    "java.util.Map<String,String>")) {
                ProcessInstance processInstance = getMasterProcess(masterRequestId);
                if (processInstance != null) {
                    VariableInstance respHeadersVar = processInstance.getVariable("responseHeaders");
                    if (respHeadersVar != null) {
                        Document doc = getDocument(((DocumentReference)respHeadersVar.getData(pkg)).getDocumentId());
                        Map<?,?> respHeaders = (Map<?,?>) doc.getObject("java.util.Map<String,String>",
                                PackageCache.getPackage(processInstance.getPackageName()));
                        for (Object key : respHeaders.keySet())
                            responseHeaders.put(key.toString(), respHeaders.get(key).toString());
                    }
                }
            }
            return response;
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    public Response invokeServiceProcess(Process process, String masterRequestId, String ownerType,
            Long ownerId, Map<String,String> params) throws ServiceException {
        return invokeServiceProcess(process, masterRequestId, ownerType, ownerId, params, null);
    }

    public Response invokeServiceProcess(Process process, String masterRequestId, String ownerType,
            Long ownerId, Map<String,String> params, Map<String,String> headers) throws ServiceException {
        try {
            ProcessEngineDriver driver = new ProcessEngineDriver();
            String masterRequest = params == null ? null : params.get("request");
            return driver.invokeService(process.getId(), ownerType, ownerId, masterRequestId,
                    masterRequest, params, null, headers);
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    /**
     * Saves document as raw StringDocument.
     */
    public Integer notify(String event, String message, int delay) throws ServiceException {
        try {
            EventServices eventManager = ServiceLocator.getEventServices();
            Long docId = eventManager.createDocument(StringDocument.class.getName(), OwnerType.INTERNAL_EVENT, 0L, message, null);
            return eventManager.notifyProcess(event, docId, message, delay);
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    public Integer notify(Package runtimePackage, String eventName, Object eventMessage) throws ServiceException {
        int delay = PropertyManager.getIntegerProperty(PropertyNames.ACTIVITY_RESUME_DELAY, 2);
        return notify(runtimePackage, eventName, eventMessage, delay);
    }

    public Integer notify(Package runtimePackage, String eventName, Object eventMessage, int delay)
            throws ServiceException {
        try {
            Long docId = 0L;
            String message = null;
            if (eventMessage != null) {
                String varType = StringDocument.class.getName();
                if (eventMessage instanceof JSONObject)
                    varType = JSONObject.class.getName();
                else if (eventMessage instanceof Jsonable)
                    varType = Jsonable.class.getName();

                EventServices eventMgr = ServiceLocator.getEventServices();
                docId = eventMgr.createDocument(varType, OwnerType.LISTENER_REQUEST, 0L, eventMessage, runtimePackage);
                message = runtimePackage.getStringValue(StringDocument.class.getName(), eventMessage, true);
            }
            EventServices eventManager = ServiceLocator.getEventServices();
            return eventManager.notifyProcess(eventName, docId, message, delay);
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);  // TODO why not throw?
            return EventInstance.RESUME_STATUS_FAILURE;
        }
    }

    public void setVariable(Long processInstanceId, String varName, Object value) throws ServiceException {
        ProcessRuntimeContext runtimeContext = getContext(processInstanceId);
        if (runtimeContext == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Process instance not found: " + processInstanceId);
        setVariable(runtimeContext, varName, value);
    }

    public void setVariable(ProcessRuntimeContext context, String varName, Object value) throws ServiceException {
        Integer statusCode = context.getProcessInstance().getStatusCode();
        if (WorkStatus.STATUS_COMPLETED.equals(statusCode) || WorkStatus.STATUS_CANCELLED.equals(statusCode)
                || WorkStatus.STATUS_FAILED.equals(statusCode)) {
            throw new ServiceException(ServiceException.BAD_REQUEST, "Cannot set value for process in final status: " +
                    statusCode);
        }
        Variable var = context.getProcess().getVariable(varName);
        if (var == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Process variable not defined: " + varName);
        String type = var.getType();
        if (context.getPackage().getTranslator(type).isDocumentReferenceVariable()) {
            setDocumentValue(context, varName, value);
        }
        else {
            try {
                VariableInstance varInst = context.getProcessInstance().getVariable(varName);
                WorkflowDataAccess workflowDataAccess = getWorkflowDataAccess();
                if (varInst == null) {
                    varInst = new VariableInstance();
                    varInst.setName(varName);
                    varInst.setVariableId(var.getId());
                    varInst.setType(type);
                    if (value != null && !value.equals("")) {
                        if (value instanceof String)
                            varInst.setStringValue((String)value);
                        else
                            varInst.setData(value);
                        Long procInstId = context.getProcessInstance().isEmbedded() ?
                                context.getProcessInstance().getOwnerId() : context.getProcessInstanceId();
                        workflowDataAccess.createVariable(procInstId, varInst, context.getPackage());
                    }
                }
                else {
                    if (value == null || value.equals("")) {
                        workflowDataAccess.deleteVariable(varInst);
                    }
                    else {
                        if (value instanceof String)
                            varInst.setStringValue((String)value);
                        else
                            varInst.setData(value);
                        workflowDataAccess.updateVariable(varInst, context.getPackage());
                    }
                }
            }
            catch (SQLException ex) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error updating "
                        + varName + " for process: " + context.getProcessInstanceId(), ex);
            }
        }
    }

    public void setVariables(Long processInstanceId, Map<String,Object> values) throws ServiceException {
        ProcessRuntimeContext runtimeContext = getContext(processInstanceId);
        if (runtimeContext == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Process instance not found: " + processInstanceId);
        setVariables(runtimeContext, values);
    }

    public void setVariables(ProcessRuntimeContext context, Map<String,Object> values) throws ServiceException {
        for (String name : values.keySet()) {
            setVariable(context, name, values.get(name));
        }
    }

    public void setDocumentValue(ProcessRuntimeContext context, String varName, Object value) throws ServiceException {
        VariableInstance varInst = context.getProcessInstance().getVariable(varName);
        if (varInst == null && value != null && !value.equals("")) {
            createDocument(context, varName, value);
        }
        else {
            if (value == null || value.equals("")) {
                try {
                    // TODO: delete doc content also
                    getWorkflowDataAccess().deleteVariable(varInst);
                }
                catch (SQLException ex) {
                    throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error deleting "
                            + varName + " for process: " + context.getProcessInstanceId(), ex);
                }
            }
            else {
                updateDocument(context, varName, value);
            }
        }
    }

    /**
     * TODO: Many places fail to set the ownerId for documents owned by VARIABLE_INSTANCE,
     * and these need to be updated to use this method.
     */
    public void createDocument(ProcessRuntimeContext context, String varName, Object value) throws ServiceException {
        String varType = context.getProcess().getVariable(varName).getType();
        EventServices eventMgr = ServiceLocator.getEventServices();
        Long procInstId = context.getProcessInstance().isEmbedded() ? context.getProcessInstance().getOwnerId() : context.getProcessInstanceId();
        try {
            Long docId = eventMgr.createDocument(varType, OwnerType.PROCESS_INSTANCE, procInstId, value, context.getPackage());
            VariableInstance varInst = eventMgr.setVariableInstance(procInstId, varName, new DocumentReference(docId), context.getPackage());
            eventMgr.updateDocumentInfo(docId, varType, OwnerType.VARIABLE_INSTANCE, varInst.getId());
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error creating document for process: " + procInstId, ex);
        }
    }

    public ProcessRun runProcess(ProcessRun runRequest) throws ServiceException {
        Long definitionId = runRequest.getDefinitionId();
        if (definitionId == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing definitionId");
        Process proc;
        try {
            proc = ServiceLocator.getDesignServices().getProcessDefinition(definitionId);
            if (proc == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Process not found for id: " + definitionId);
        } catch (IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error loading process " + definitionId, ex);
        }

        ProcessRun actualRun = new ProcessRun(runRequest.getJson());  // clone

        Long runId = runRequest.getId();
        if (runId == null) {
            runId = System.nanoTime();
            actualRun.setId(runId);
        }
        String masterRequestId = runRequest.getMasterRequestId();
        if (masterRequestId == null) {
            masterRequestId = runId.toString();
            actualRun.setMasterRequestId(masterRequestId);
        }
        String ownerType = runRequest.getOwnerType();
        Long ownerId = runRequest.getOwnerId();
        if (ownerType == null) {
            if (ownerId != null)
                throw new ServiceException(ServiceException.BAD_REQUEST, "ownerId not allowed without ownerType");
            EventServices eventMgr = ServiceLocator.getEventServices();
            try {
                ownerType = OwnerType.DOCUMENT;
                actualRun.setOwnerType(ownerType);
                ownerId = eventMgr.createDocument(JSONObject.class.getName(), OwnerType.PROCESS_RUN, runId,
                        runRequest.getJson(), PackageCache.getPackage(proc.getPackageName()));
                actualRun.setOwnerId(ownerId);
            }
            catch (DataAccessException ex) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error creating document for run id: "
                        + runId);
            }
        }
        else if (ownerId == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "ownerType not allowed without ownerId");

        Map<String,String> params = new HashMap<>();
        if (runRequest.getValues() != null) {
            for (String name : runRequest.getValues().keySet()) {
                Value value = runRequest.getValues().get(name);
                if (value.getValue() != null)
                    params.put(name, value.getValue());
            }
        }
        if (proc.isService()) {
            Map<String,String> headers = new HashMap<>();
            invokeServiceProcess(proc, masterRequestId, ownerType, ownerId, params, headers);
            String instIdStr = headers.get(Listener.METAINFO_MDW_PROCESS_INSTANCE_ID);
            if (instIdStr != null)
                actualRun.setInstanceId(Long.parseLong(instIdStr));
        }
        else {
            Long instanceId = launchProcess(proc, masterRequestId, ownerType, ownerId, params);
            actualRun.setInstanceId(instanceId);
        }
        return actualRun;
    }

    public void updateDocument(ProcessRuntimeContext context, String varName, Object value) throws ServiceException {
        EventServices eventMgr = ServiceLocator.getEventServices();
        VariableInstance varInst = context.getProcessInstance().getVariable(varName);
        if (varInst == null)
            throw new ServiceException(ServiceException.NOT_FOUND, varName + " not found for process: " +
                    context.getProcessInstanceId());
        try {
            eventMgr.updateDocumentContent(varInst.getDocumentId(), value, context.getPackage());
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error updating document: " +
                    varInst.getDocumentId());
        }
    }

    private Map<String,String> translateParameters(Process process, Map<String,Object> parameters)
            throws ProcessException {
        Map<String,String> stringParams = new HashMap<>();
        if (parameters != null) {
            for (String key : parameters.keySet()) {
                Object val = parameters.get(key);
                Variable variable = process.getVariable(key);
                if (variable == null) {
                    throw new ProcessException("Variable '" + key + "' not found for process: " + process.getName() +
                            " v" + process.getVersionString() + "(id=" + process.getId() + ")");
                }
                String translated;
                if (val instanceof String)
                    translated = (String) val;
                else {
                    Package pkg = PackageCache.getPackage(process.getPackageName());
                    translated = pkg.getStringValue(variable.getType(), val, true);
                }
                stringParams.put(key, translated);
            }
        }
        return stringParams;
    }

    @SuppressWarnings("deprecation")
    public String getDocumentStringValue(Long id, String variableType, Package pkg) throws ServiceException {
        try {
            Document doc = getWorkflowDataAccess().getDocument(id);
            if (doc.getType() == null)
                throw new ServiceException(ServiceException.INTERNAL_ERROR, "Unable to determine document type.");

            doc.setVariableType(variableType);
            // check raw content for parsability
            if (doc.getContent(pkg) == null || doc.getContent(pkg).isEmpty())
                return doc.getContent(pkg);

            VariableTranslator translator = pkg.getTranslator(doc.getVariableType());
            if (translator instanceof JavaObjectTranslator) {
                Object obj = doc.getObject(Object.class.getName(), pkg);
                return obj.toString();
            }
            else if (translator instanceof StringDocumentTranslator) {
                return doc.getContent(pkg);
            }
            else if (translator instanceof XmlDocumentTranslator && !(translator instanceof YamlTranslator)) {
                org.w3c.dom.Document domDoc = ((XmlDocumentTranslator)translator).toDomDocument(doc.getObject(doc.getVariableType(), pkg));
                XmlObject xmlBean = XmlObject.Factory.parse(domDoc);
                return xmlBean.xmlText(new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(4));
            }
            else if (translator instanceof JsonTranslator && !(translator instanceof YamlTranslator)) {
                JSONObject jsonObj = ((JsonTranslator)translator).toJson(doc.getObject(doc.getVariableType(), pkg));
                if (jsonObj instanceof JsonObject) {
                    return jsonObj.toString(2);
                }
                else {
                    // reformat for predictable prop ordering
                    return new JsonObject(jsonObj.toString()).toString(2);
                }
            }
            return doc.getContent(pkg);
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error retrieving document: " + id, ex);
        }
    }

    private Package getPackage(Document doc) throws ServiceException {
        try {
            EventServices eventMgr = ServiceLocator.getEventServices();
            if (doc.getOwnerId() == 0) // eg: sdwf request headers
                return null;
            if (doc.getOwnerType().equals(OwnerType.VARIABLE_INSTANCE)) {
                VariableInstance varInstInf = eventMgr.getVariableInstance(doc.getOwnerId());
                Long procInstId = varInstInf.getProcessInstanceId();
                ProcessInstance procInstVO = eventMgr.getProcessInstance(procInstId);
                if (procInstVO != null) {
                    Process process = ProcessCache.getProcess(procInstVO.getProcessId());
                    return PackageCache.getPackage(process.getPackageName());
                }
            }
            else if (doc.getOwnerType().equals(OwnerType.PROCESS_INSTANCE)) {
                Long procInstId = doc.getOwnerId();
                ProcessInstance procInst = eventMgr.getProcessInstance(procInstId);
                if (procInst != null) {
                    Process process = ProcessCache.getProcess(procInst.getProcessId());
                    return PackageCache.getPackage(process.getPackageName());
                }
            }
            else { // test case, etc
                Process process = ProcessCache.getProcess(doc.getOwnerId());
                if (process != null)
                    return PackageCache.getPackage(process.getName());
            }
            return PackageCache.getMdwBasePackage();
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public void createProcess(String assetPath, Query query) throws ServiceException {
        if (!assetPath.endsWith(".proc"))
            assetPath += ".proc";
        String template = query.getFilter("template");
        if (template == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing param: template");
        try {
            byte[] content = Templates.getBytes("assets/" + template + ".proc");
            if (content == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Template not found: " + template);
            ServiceLocator.getAssetServices().createAsset(assetPath, content);
        } catch (IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error creating " + assetPath, ex);
        }
    }

    public Process getInstanceDefinition(String assetPath, Long instanceId) throws ServiceException {
        EngineDataAccessDB dataAccess = new EngineDataAccessDB();
        try {
            dataAccess.getDatabaseAccess().openConnection();
            ProcessInstance procInst = dataAccess.getProcessInstance(instanceId); // We need the processID
            if (procInst.getInstanceDefinitionId() > 0L) {
                Process process = ProcessCache.getInstanceDefinition(procInst.getProcessId(),
                        procInst.getInstanceDefinitionId());
                // make sure instanceId is for requested assetPath
                if (process.getPath().equals(assetPath))
                    return process;
            }
            return null;
        }
        catch (SQLException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error retrieving instance document "
                    + assetPath + ": " + instanceId);
        }
        finally {
            if (dataAccess.getDatabaseAccess().connectionIsOpen())
                dataAccess.getDatabaseAccess().closeConnection();
        }
    }

    public void saveInstanceDefinition(String assetPath, Long instanceId, Process process)
            throws ServiceException {

        String procPath = process.getPackageName() + "/" + process.getName();
        AssetInfo processAsset = ServiceLocator.getAssetServices().getAsset(procPath);
        if (processAsset == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Process asset not found: " + procPath);

        EngineDataAccessDB dataAccess = new EngineDataAccessDB();
        try {
            boolean isYaml = !JsonUtil.isJson(processAsset.getFile());
            EventServices eventServices = ServiceLocator.getEventServices();
            dataAccess.getDatabaseAccess().openConnection();
            ProcessInstance procInst = dataAccess.getProcessInstance(instanceId);
            String content = isYaml ? Yamlable.toString(process,2) : process.getJson().toString(2);
            long docId = procInst.getInstanceDefinitionId();
            Package pkg = PackageCache.getPackage(process.getPackageName());
            if (docId == 0L) {
                docId = eventServices.createDocument(isYaml ? Yaml.class.getName() : JSONObject.class.getName(), OwnerType.PROCESS_INSTANCE_DEF, instanceId, content, pkg);
                String[] fields = new String[]{"COMMENTS"};
                String comment = procInst.getComment() == null ? "" : procInst.getComment();
                Object[] args = new Object[]{comment + "|HasInstanceDef|" + docId, null};
                dataAccess.updateTableRow("PROCESS_INSTANCE", "process_instance_id", instanceId, fields, args);
            }
            else {
                eventServices.updateDocumentContent(docId, content, pkg);
                if (!isYaml) // maybe old save was Jsonable doc type
                    eventServices.updateDocumentInfo(docId, JSONObject.class.getName(), OwnerType.PROCESS_INSTANCE_DEF, instanceId);
            }
            // Update any embedded Sub processes to indicate they have instance definition
            for (ProcessInstance inst : dataAccess.getProcessInstances(procInst.getProcessId(),
                    OwnerType.MAIN_PROCESS_INSTANCE, procInst.getId())) {
                String[] fields = new String[]{"COMMENTS"};
                String comment = inst.getComment() == null ? "" : inst.getComment();
                Object[] args = new Object[]{comment + "|HasInstanceDef|" + docId, null};
                dataAccess.updateTableRow("PROCESS_INSTANCE", "process_instance_id", inst.getId(),
                        fields, args);
            }
        }
        catch (DataAccessException | SQLException | IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error creating process instance definition " +
                    assetPath + ": " + instanceId, ex);
        }
        finally {
            if (dataAccess.getDatabaseAccess().connectionIsOpen())
                dataAccess.getDatabaseAccess().closeConnection();
        }
    }

    protected UserAction auditLog(String action, UserAction.Entity entity, Long entityId, String user,
            String completionCode) throws ServiceException {
        UserAction userAction = new UserAction(user, UserAction.getAction(action), entity, entityId, null);
        userAction.setSource("Workflow Services");
        userAction.setDestination(completionCode);
        if (userAction.getAction().equals(UserAction.Action.Other)) {
            userAction.setExtendedAction(action);
        }
        try {
            EventServices eventManager = ServiceLocator.getEventServices();
            eventManager.createAuditLog(userAction);
            return userAction;
        }
        catch (DataAccessException ex) {
            throw new ServiceException("Failed to create audit log: " + userAction, ex);
        }
    }

    public Linked<ProcessInstance> getCallHierearchy(Long processInstanceId) throws ServiceException {
        try {
            return getWorkflowDataAccess().getProcessInstanceCallHierarchy(processInstanceId);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    public MilestonesList getMilestones(Query query) throws ServiceException {
        MilestonesList milestonesList = new MilestonesList(new ArrayList<>(), 0);
        String[] milestonedProcesses = HierarchyCache.getMilestoned().stream()
                .map(String::valueOf).collect(Collectors.toList()).toArray(new String[0]);
        if (milestonedProcesses.length > 0) {
            if (query.getFilter("processId") == null) {
                query.setArrayFilter("processIds", milestonedProcesses);
                query.setFilter("master", true);
            }
            ProcessList masterProcessList = getProcesses(query);
            for (ProcessInstance masterProcessInstance : masterProcessList.getProcesses()) {
                try {
                    Process masterProcess = ProcessCache.getProcess(masterProcessInstance.getProcessId());
                    Milestone milestone = new MilestoneFactory(masterProcess).createMilestone(masterProcessInstance);
                    milestonesList.getMilestones().add(milestone);
                } catch (IOException ex) {
                    throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
                }
            }
            milestonesList.setTotal(masterProcessList.getTotal());
        }
        return milestonesList;
    }

    @Override
    public Linked<Milestone> getMilestones(String masterRequestId, boolean future) throws ServiceException {
        ProcessInstance masterProcessInstance = getMasterProcess(masterRequestId);
        if (masterProcessInstance == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Master process not found: " + masterRequestId);
        return getMilestones(masterProcessInstance, future);
    }

    @Override
    public Linked<Milestone> getMilestones(Long masterProcessInstanceId, boolean future) throws ServiceException {
        ProcessInstance masterProcessInstance = getProcess(masterProcessInstanceId);
        if (masterProcessInstance == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Process not found: " + masterProcessInstanceId);
        return getMilestones(masterProcessInstance, future);
    }

    private Linked<Milestone> getMilestones(ProcessInstance masterProcessInstance, boolean future)
            throws ServiceException {
        try {
            Process masterProcess = ProcessCache.getProcess(masterProcessInstance.getProcessId());
            // retrieve full
            ProcessInstance processInstance = getProcess(masterProcessInstance.getId());

            Linked<ActivityInstance> endToEndActivities = getActivityHierarchy(processInstance);
            Linked<ProcessInstance> parentInstance = getCallHierearchy(masterProcessInstance.getId());
            Activity masterStartActivity = masterProcess.getStartActivity();
            ActivityInstance masterStartInstance =
                    processInstance.getActivities(masterStartActivity.getLogicalId()).get(0);
            // TODO: ability to define top label (from runtime expression)
            String label = masterProcessInstance.getMasterRequestId();
            Milestone startMilestone = new MilestoneFactory(masterProcess).createMilestone(masterProcessInstance,
                    masterStartInstance, label);
            Linked<Milestone> masterMilestones = new Linked<>(startMilestone);
            addMilestones(masterMilestones, endToEndActivities, parentInstance);
            if (future && HierarchyCache.hasMilestones(masterProcess.getId())) {
                for (Linked<Milestone> end : masterMilestones.getEnds()) {
                    Process endProcess = ProcessCache.getProcess(end.get().getProcess().getPath());
                    Linked<Milestone> milestonesDef = HierarchyCache.getMilestones(endProcess.getId());
                    Linked<Milestone> futureMilestone = milestonesDef.find(m -> {
                        return m.getActivity().getId().equals(end.get().getActivity().getId()) &&
                                m.getProcess().getPath().equals(end.get().getProcess().getPath());
                    });
                    if (futureMilestone != null) {
                        end.setChildren(futureMilestone.getChildren());
                        futureMilestone.setParent(end);
                    }
                }
            }
            return masterMilestones;
        } catch (IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error loading milestones for " + masterProcessInstance.getId(), ex);
        }
    }

    private void addMilestones(Linked<Milestone> parent, Linked<ActivityInstance> start,
            Linked<ProcessInstance> instanceHierarchy) throws ServiceException, IOException {
        ActivityInstance activityInstance = start.get();
        ProcessInstance processInstance = instanceHierarchy.
                find(new ProcessInstance(activityInstance.getProcessInstanceId())).get();
        Process process = ProcessCache.getProcess(processInstance.getProcessId());
        Activity activity = process.getActivity(activityInstance.getActivityId());
        Milestone milestone = new MilestoneFactory(process).getMilestone(activity);
        if (milestone != null) {
            if (ProcessRuntimeContext.isExpression(milestone.getLabel())) {
                Package pkg = PackageCache.getPackage(process.getPackageName());
                ProcessInstance loadedInstance = getProcess(processInstance.getId());
                ActivityImplementor implementor = ImplementorCache.get(activity.getImplementor());
                String category = implementor == null ? GeneralActivity.class.getName() : implementor.getCategory();
                ActivityRuntimeContext runtimeContext = new ActivityRuntimeContext(null, pkg, process, loadedInstance, 0, false,
                        activity, category, activityInstance, false);
                // doc variables are not loaded (too expensive)
                for (VariableInstance variableInstance : loadedInstance.getVariables())
                    runtimeContext.getValues().put(variableInstance.getName(), variableInstance.getStringValue(pkg));
                milestone.setLabel(runtimeContext.evaluateToString(milestone.getLabel()));
            }
            milestone.setProcessInstance(processInstance);
            milestone.setActivityInstance(activityInstance);
            Linked<Milestone> linkedMilestone = new Linked<>(milestone);
            linkedMilestone.setParent(parent);
            parent.getChildren().add(linkedMilestone);
            for (Linked<ActivityInstance> child : start.getChildren()) {
                addMilestones(linkedMilestone, child, instanceHierarchy);
            }
        }
        else {
            for (Linked<ActivityInstance> child : start.getChildren()) {
                addMilestones(parent, child, instanceHierarchy);
            }
        }
    }

    @Override
    public Linked<ActivityInstance> getActivityHierarchy(ProcessInstance processInstance) throws ServiceException {
        try {
            Process process = ProcessCache.getProcess(processInstance.getProcessId());
            if (process == null)
                throw new ServiceException("Process not found: " + processInstance.getComment() );

            Linked<ActivityInstance> endToEndActivities = processInstance.getLinkedActivities(process);
            Linked<ProcessInstance> instanceHierarchy = getCallHierearchy(processInstance.getId());
            ScopedActivityInstance scoped = new ScopedActivityInstance(instanceHierarchy, endToEndActivities);
            addSubprocessActivities(scoped, null);
            return endToEndActivities;
        }
        catch (IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error loading activity hierarchy for " + processInstance.getId(), ex);
        }
    }

    private void addSubprocessActivities(ScopedActivityInstance start, List<ScopedActivityInstance> downstreams)
            throws ServiceException, IOException {

        List<ScopedActivityInstance> furtherDowns = downstreams;

        for (ScopedActivityInstance scopedChild : start.getScopedChildren()) {
            ActivityInstance activityInstance = scopedChild.get();
            Process process = ProcessCache.getProcess(scopedChild.get().getProcessId());
            if (process == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Definition not found for process instance " + activityInstance.getProcessInstanceId());

            Activity activity = process.getActivity(activityInstance.getActivityId());

            List<Linked<ProcessInstance>> subhierarchies = getInvoked(scopedChild, activity);
            if (!subhierarchies.isEmpty()) {
                // link downstream children
                if (furtherDowns != null) {
                    for (Linked<ActivityInstance> downstreamChild : scopedChild.getChildren()) {
                        for (Linked<ActivityInstance> end : downstreamChild.getEnds()) {
                            Process endProcess = ProcessCache.getProcess(downstreamChild.get().getProcessId());
                            boolean isStop = endProcess.getActivity(end.get().getActivityId()).isStop();
                            if (isStop) {
                                end.setChildren(new ArrayList<>(furtherDowns));
                            }
                        }
                    }
                }
                if (activity.isSynchronous()) {
                    furtherDowns = scopedChild.getScopedChildren();
                    scopedChild.setChildren(new ArrayList<>());
                }
                for (Linked<ProcessInstance> subhierarchy : subhierarchies) {
                    ProcessInstance loadedSub = getProcess(subhierarchy.get().getId());
                    Process subProcess = ProcessCache.getProcess(loadedSub.getProcessId());
                    if (subProcess == null)
                        throw new ServiceException(ServiceException.NOT_FOUND, "Definition not found for process instance " + loadedSub.getId());
                    Linked<ActivityInstance> subprocActivities = loadedSub.getLinkedActivities(subProcess);
                    scopedChild.getChildren().add(subprocActivities);
                    subprocActivities.setParent(scopedChild);
                    ScopedActivityInstance subprocScoped = new ScopedActivityInstance(subhierarchy, subprocActivities);
                    addSubprocessActivities(subprocScoped, furtherDowns);
                }
                furtherDowns = downstreams;
            }
            else {
                // non-invoker
                boolean isStop = process.getActivity(scopedChild.get().getActivityId()).isStop();
                if (isStop && scopedChild.getChildren().isEmpty() ) {
                    if (furtherDowns != null) {
                        scopedChild.setChildren(new ArrayList<>(furtherDowns));
                        furtherDowns = null;
                    }
                }
                addSubprocessActivities(scopedChild, furtherDowns);
            }
        }
    }

    /**
     * Finds subprocess instances by consulting relevant process hierarchy (excludes ignored).
     */
    private List<Linked<ProcessInstance>> getInvoked(ScopedActivityInstance scopedInstance, Activity activity) {
        List<Linked<ProcessInstance>> invoked = new ArrayList<>();
        for (Linked<ProcessInstance> subprocess : scopedInstance.findInvoked(activity, ProcessCache.getProcesses(true))) {
            if (!isIgnored(subprocess.get()))
                invoked.add(subprocess);
        }
        return invoked;
    }

    private boolean isIgnored(ProcessInstance processInstance) {
        List<String> ignores = PropertyManager.getListProperty(PropertyNames.MDW_MILESTONE_IGNORES);
        return ignores != null &&
                ignores.contains(processInstance.getPackageName() + "/" + processInstance.getProcessName() + ".proc");
    }
}
