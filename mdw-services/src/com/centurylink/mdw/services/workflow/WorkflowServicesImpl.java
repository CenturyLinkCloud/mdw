/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.workflow;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.RuntimeDataAccess;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.dataaccess.file.AggregateDataAccessVcs;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetHeader;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.user.UserAction.Action;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.ActivityCount;
import com.centurylink.mdw.model.workflow.ActivityImplementor;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.ActivityInstanceInfo;
import com.centurylink.mdw.model.workflow.ActivityList;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessCount;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.ProcessList;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;
import com.centurylink.mdw.service.data.WorkflowDataAccess;
import com.centurylink.mdw.service.data.process.EngineDataAccessDB;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.process.ProcessEngineDriver;
import com.centurylink.mdw.util.TransactionWrapper;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.CodeTimer;

public class WorkflowServicesImpl implements WorkflowServices {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private WorkflowDataAccess getWorkflowDao() {
        return new WorkflowDataAccess();
    }

    protected RuntimeDataAccess getRuntimeDataAccess() throws DataAccessException {
        return DataAccess.getRuntimeDataAccess(new DatabaseAccess(null));
    }

    protected AggregateDataAccessVcs getAggregateDataAccess() throws DataAccessException {
        return new AggregateDataAccessVcs();
    }

    public Map<String,String> getAttributes(String ownerType, Long ownerId) throws ServiceException {
        try {
            return ServiceLocator.getEventManager().getAttributes(ownerType, ownerId);
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public void setAttributes(String ownerType, Long ownerId, Map<String,String> attributes) throws ServiceException {
        try {
            ServiceLocator.getEventManager().setAttributes(ownerType, ownerId, attributes);
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }
    /**
     * Update attributes without deleting all attributes for this ownerId first
     */
    public void updateAttributes(String ownerType, Long ownerId, Map<String,String> attributes) throws ServiceException {
        try {
            for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                String attributeName = attribute.getKey();
                String attributeValue = attribute.getValue();
                ServiceLocator.getEventManager().setAttribute(ownerType, ownerId, attributeName, attributeValue);
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
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
            CommonDataAccess dao = new CommonDataAccess();
            return dao.getValues(ownerType, ownerId);
        }
        catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public List<String> getValueHolderIds(String valueName, String valuePattern) throws ServiceException {
        return getValueHolderIds(valueName, valuePattern, null);
    }

    public List<String> getValueHolderIds(String valueName, String valuePattern, String ownerType) throws ServiceException {
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

    public void registerTaskWaitEvent(Long taskInstanceId, String eventName) throws ServiceException {
        registerTaskWaitEvent(taskInstanceId, eventName, "FINISH", false);
    }

    public void registerTaskWaitEvent(Long taskInstanceId, String eventName, String completionCode) throws ServiceException {
        registerTaskWaitEvent(taskInstanceId, eventName, completionCode, false);
    }

    // FIXME confirm this implementation
    public void registerTaskWaitEvent(Long taskInstanceId, String eventName, String completionCode,
            boolean recurring) throws ServiceException {
        TransactionWrapper tw = null;
        EngineDataAccessDB dataAccess = null;
        try {
            TaskManager taskMgr = ServiceLocator.getTaskManager();
            TaskInstance taskVo = taskMgr.getTaskInstance(taskInstanceId);
            Long activityInstanceId = taskMgr.getActivityInstanceId(taskVo,false);
            dataAccess = new EngineDataAccessDB();
            tw = dataAccess.startTransaction();
            dataAccess.recordEventWait(eventName, !recurring, 3600, activityInstanceId, completionCode);
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);

        }
        finally {
            if (tw != null)
                try {
                    dataAccess.stopTransaction(tw);
                }
                catch (DataAccessException ex) {
                    throw new ServiceException(ex.getMessage(), ex);
                }
        }
    }

    @Override
    public void actionActivity(String activityInstanceId, String action, String completionCode) throws ServiceException {
        try {
            if (action != null && (action.equalsIgnoreCase(Action.Proceed.toString()))){
                ServiceLocator.getEventManager().skipActivity(null, new Long(activityInstanceId).longValue(), completionCode);
            }
            else if (action != null && (action.equalsIgnoreCase(Action.Retry.toString()))){
                ActivityInstance activityVo = ServiceLocator.getEventManager().getActivityInstance(new Long(activityInstanceId).longValue());
                ServiceLocator.getEventManager().retryActivity(activityVo.getDefinitionId(), new Long(activityInstanceId).longValue());
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public ActivityList getActivities(Query query) throws ServiceException {
        try {
            CodeTimer timer = new CodeTimer(true);
            ActivityList list = getRuntimeDataAccess().getActivityInstanceList(query);
            timer.logTimingAndContinue("getRuntimeDataAccess().getActivityInstanceList()");
            list = populateActivities(list, query);
            timer.stopAndLogTiming("WorkflowServicesImpl.populateActivities()");
            return list;
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
            RuntimeDataAccess runtimeDataAccess = getRuntimeDataAccess();
            ProcessInstance process = runtimeDataAccess.getProcessInstanceAll(instanceId);
            if (process == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Process instance not found: " + instanceId);
            if (withSubprocs) {
                // embedded subprocs
                Map<String,String> criteria = new HashMap<String,String>();
                criteria.put("owner", OwnerType.MAIN_PROCESS_INSTANCE);
                criteria.put("ownerId", process.getId().toString());
                criteria.put("processId", process.getProcessId().toString());
                ProcessList subprocList = runtimeDataAccess.getProcessInstanceList(criteria, 0, Query.MAX_ALL, "order by process_instance_id");
                if (subprocList != null && subprocList.getItems() != null && subprocList.getItems().size() > 0) {
                    List<ProcessInstance> subprocs = new ArrayList<ProcessInstance>();
                    for (ProcessInstance subproc : subprocList.getItems()) {
                        ProcessInstance fullsub = runtimeDataAccess.getProcessInstance(subproc.getId());
                        fullsub.setProcessId(Long.parseLong(subproc.getComment()));
                        subprocs.add(fullsub);
                    }
                    process.setSubprocessInstances(subprocs);
                }
            }
            return process;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error retrieving process instance: " + instanceId + ": " + ex.getMessage(), ex);
        }
    }

    public Map<String,Value> getProcessValues(Long instanceId) throws ServiceException {
        return getProcessValues(instanceId, false);
    }

    public Map<String,Value> getProcessValues(Long instanceId, boolean includeEmpty) throws ServiceException {
        ProcessRuntimeContext runtimeContext = getContext(instanceId);
        Map<String,Value> values = new HashMap<String,Value>();
        Map<String,Variable> varDefs = getVariableDefinitions(runtimeContext.getProcessId());

        Map<String,Object> variables = runtimeContext.getVariables();
        if (variables != null) {
            for (String key : variables.keySet()) {
                String stringVal = runtimeContext.getValueAsString(key);
                if (stringVal != null) {
                    Variable varDef = varDefs.get(key);
                    Value value;
                    if (varDef != null)
                        value = toValue(varDef);
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
                    values.put(name, toValue(varDefs.get(name)));
            }
        }
        return values;
    }

    protected Value toValue(Variable varDef) {
        Value value = new Value(varDef.getName());
        value.setType(varDef.getVariableType());
        if (varDef.getDisplayMode() != null)
            value.setDisplay(Value.getDisplay(varDef.getDisplayMode()));
        if (varDef.getDisplaySequence() != null)
            value.setSequence(varDef.getDisplaySequence());
        if (varDef.getVariableReferredAs() != null)
            value.setLabel(varDef.getVariableReferredAs());
        return value;
    }

    /**
     * @param runtimeContext
     * @return
     */
    protected Map<String,Variable> getVariableDefinitions(Long processId) {
        Process processVo = ProcessCache.getProcess(processId);
        Map<String,Variable> varDefs = new HashMap<String,Variable>();
        List<Variable> varVos = processVo.getVariables();
        if (varVos != null) {
            for (Variable var : varVos)
                varDefs.put(var.getName(), var);
        }
        return varDefs;
    }

    public Value getProcessValue(Long instanceId, String name) throws ServiceException {
        ProcessRuntimeContext runtimeContext = getContext(instanceId);
        if (!runtimeContext.isExpression(name) && runtimeContext.getProcess().getVariable(name) == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "No variable defined: " + name);
        String stringVal = runtimeContext.getValueAsString(name);
        if (stringVal == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "No value '" + name + "' found for instance: " + instanceId);
        Variable varDef = getVariableDefinitions(runtimeContext.getProcessId()).get(name);
        Value value;
        if (varDef != null)
            value = toValue(varDef);
        else
            value = new Value(name);
        value.setValue(stringVal);
        return value;
    }

    public ProcessRuntimeContext getContext(Long instanceId) throws ServiceException {
        ProcessInstance instance = getProcess(instanceId);
        Process process = ProcessCache.getProcess(instance.getProcessId());
        Package pkg = PackageCache.getProcessPackage(instance.getProcessId());
        if (process == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Process definition not found for id: " + instance.getProcessId());

        Map<String,Object> vars = new HashMap<String,Object>();
        try {
            if (instance.getVariables() != null) {
                for (VariableInstance var : instance.getVariables()) {
                    Object value = var.getData();
                    if (value instanceof DocumentReference) {
                        Document docVO = getWorkflowDao().getDocument(((DocumentReference)value).getDocumentId());
                        value = docVO == null ? null : docVO.getObject(var.getType(), pkg);
                    }
                    vars.put(var.getName(), value);
                }
            }
            return new ProcessRuntimeContext(pkg, process, instance, vars);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    @Override
    public ProcessList getProcesses(Query query) throws ServiceException {
        try {
            String procDefSpec = query.getFilter("definition"); // in form of AssetVersionSpec
            if (procDefSpec != null) {
                AssetVersionSpec spec = AssetVersionSpec.parse(procDefSpec);
                if (spec.getName().endsWith(".proc"))
                    spec = new AssetVersionSpec(spec.getPackageName(), spec.getName().substring(0, spec.getName().length() - 5), spec.getVersion());
                Process procDef = ProcessCache.getProcessSmart(spec);
                if (procDef == null)
                    throw new ServiceException(ServiceException.NOT_FOUND, "Process definition not found for spec: " + procDefSpec);
                query.setFilter("processId", procDef.getId());
            }
            return getWorkflowDao().getProcessInstances(query);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, "Error retrieving process instance for query: " + query, ex);
        }
    }

    @Override
    public ActivityInstanceInfo getActivity(Long instanceId) throws ServiceException {
        try {
            Query query = new Query();
            query.setFilter("instanceId", instanceId);
            query.setFind(null);
            ActivityList list = getRuntimeDataAccess().getActivityInstanceList(query);
            if (list.getCount() > 0) {
                list = populateActivities(list, query);
                return list.getActivities().get(0);
            }
            else {
                return null;
            }
        }
        catch (Exception ex) {
            throw new ServiceException(500, "Error retrieving activity instance: " + instanceId + ": " + ex.getMessage(), ex);
        }
    }

    public List<ProcessCount> getTopThroughputProcesses(Query query) throws ServiceException {
        try {
            CodeTimer timer = new CodeTimer(true);
            List<ProcessCount> list = getAggregateDataAccess().getTopThroughputProcessInstances(query);
            timer.logTimingAndContinue("AggregateDataAccessVcs.getTopThroughputProcessInstances()");
            list = populate(list);
            timer.stopAndLogTiming("WorkflowServicesImpl.populate()");
            return list;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, "Error retrieving top throughput processes: query=" + query, ex);
        }
    }

    public Map<Date,List<ProcessCount>> getProcessInstanceBreakdown(Query query) throws ServiceException {
        try {
            Map<Date,List<ProcessCount>> map = getAggregateDataAccess().getProcessInstanceBreakdown(query);
            if (query.getFilters().get("processIds") != null) {
                for (Date date : map.keySet())
                    populate(map.get(date));
            }
            return map;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, "Error retrieving process instance breakdown: query=" + query, ex);
        }
    }

    /**
     * Fills in process header info, consulting latest instance comment if necessary.
     */
    protected List<ProcessCount> populate(List<ProcessCount> processCounts) throws DataAccessException {
        AggregateDataAccessVcs dataAccess = null;
        for (ProcessCount pc : processCounts) {
            Process process = ProcessCache.getProcess(pc.getId());
            if (process == null) {
                logger.severe("Missing definition for process id: " + pc.getId());
                pc.setDefinitionMissing(true);
                // may have been deleted -- infer from comments
                if (dataAccess == null)
                    dataAccess = getAggregateDataAccess();
                CodeTimer timer = new CodeTimer(true);
                String comments = dataAccess.getLatestProcessInstanceComments(pc.getId());
                timer.stopAndLogTiming("getLatestProcessInstanceComments()");
                if (comments != null) {
                    AssetHeader assetHeader = new AssetHeader(comments);
                    pc.setName(assetHeader.getName());
                    pc.setVersion(assetHeader.getVersion());
                    pc.setPackageName(assetHeader.getPackageName());
                }
                else {
                    logger.severe("Unable to infer process name for: " + pc.getId());
                    pc.setName("Unknown (" + pc.getId() + ")");
                }
            }
            else {
                pc.setName(process.getName());
                pc.setVersion(Asset.formatVersion(process.getVersion()));
                pc.setPackageName(process.getPackageName());
            }
        }
        return processCounts;
    }

    /**
     * Fills in process header info, consulting latest instance comment if necessary.
     * @param query
     */
    protected ActivityList populateActivities(ActivityList activityList, Query query) throws DataAccessException {
        AggregateDataAccessVcs dataAccess = null;
        List<ActivityInstanceInfo> aList = activityList.getActivities();
        ArrayList<ActivityInstanceInfo> matchActivities = new ArrayList<ActivityInstanceInfo>();
        for (ActivityInstanceInfo activityInstance : aList) {
            Process process = ProcessCache.getProcess(activityInstance.getProcessId());
            if (process == null) {
                logger.severe("Missing definition for process id: " + activityInstance.getProcessId());
                activityInstance.setDefinitionMissing(true);
                // may have been deleted -- infer from comments
                if (dataAccess == null)
                    dataAccess = getAggregateDataAccess();
                CodeTimer timer = new CodeTimer(true);
                String comments = dataAccess.getLatestProcessInstanceComments(activityInstance.getProcessId());
                timer.stopAndLogTiming("getLatestProcessInstanceComments()");
                if (comments != null) {
                    AssetHeader assetHeader = new AssetHeader(comments);
                    activityInstance.setProcessName(assetHeader.getName());
                    activityInstance.setProcessVersion(assetHeader.getVersion());
                    activityInstance.setPackageName(assetHeader.getPackageName());
                }
                else {
                    logger.severe("Unable to infer process name for: " + activityInstance.getProcessId());
                    activityInstance.setProcessName("Unknown (" + activityInstance.getProcessId() + ")");
                }
                activityInstance.setName("Unknown (" + activityInstance.getDefinitionId() + ")");
            }
            else {
                String logicalId = activityInstance.getDefinitionId();
                Activity actdef = process.getActivityById(logicalId);
                if (actdef != null) {
                    activityInstance.setName(actdef.getActivityName().replaceAll("\\r", "").replace('\n', ' '));                }
                else {
                    activityInstance.setName("Unknown (" + activityInstance.getDefinitionId() + ")");
                }
                activityInstance.setProcessName(process.getName());
                activityInstance.setProcessVersion(Asset.formatVersion(process.getVersion()));
                activityInstance.setPackageName(process.getPackageName());
            }
        }

        String activityName = query.getFilter("activityName");
        if (activityName != null) {
            for (ActivityInstanceInfo activityInstance : aList) {
                try {
                    String decodedActName = java.net.URLDecoder.decode(activityName, "UTF-8");
                    if (activityInstance.getName().startsWith(decodedActName)) {
                        matchActivities.add(activityInstance);
                    }
                }
                catch (UnsupportedEncodingException e) {
                    logger.severe("Unable to decode: " + activityName);
                }
            }
            activityList.setActivities(matchActivities);
        }

        activityList.setCount(activityList.getActivities().size());
        activityList.setTotal(activityList.getActivities().size());
        return activityList;
    }

    /**
     * Fills in process header info, consulting latest instance comment if necessary.
     */
    protected List<ActivityCount> populateAct(List<ActivityCount> activityCounts) throws DataAccessException {
        AggregateDataAccessVcs dataAccess = null;
        for (ActivityCount ac : activityCounts) {
            Process process = ProcessCache.getProcess(ac.getProcessId());
            if (process == null) {
                logger.severe("Missing definition for process id: " + ac.getProcessId());
                ac.setDefinitionMissing(true);
                // may have been deleted -- infer from comments
                if (dataAccess == null)
                    dataAccess = getAggregateDataAccess();
                CodeTimer timer = new CodeTimer(true);
                String comments = dataAccess.getLatestProcessInstanceComments(ac.getProcessId());
                timer.stopAndLogTiming("getLatestProcessInstanceComments()");
                if (comments != null) {
                    AssetHeader assetHeader = new AssetHeader(comments);
                    ac.setProcessName(assetHeader.getName());
                    ac.setActivityName(ac.getDefinitionId());
                    ac.setName(ac.getProcessName() + ": " + ac.getActivityName());
                    ac.setVersion(assetHeader.getVersion());
                    ac.setPackageName(assetHeader.getPackageName());
                }
                else {
                    logger.severe("Unable to infer process name for: " + ac.getProcessId());
                    ac.setProcessName("Unknown (" + ac.getProcessId() + ")");
                }
            }
            else {
                ac.setProcessName(process.getName());
                ac.setVersion(Asset.formatVersion(process.getVersion()));
                ac.setPackageName(process.getPackageName());
                String logicalId = ac.getDefinitionId();
                Activity actdef = process.getActivityById(logicalId);
                if (actdef != null) {
                    String actName = actdef.getActivityName().replaceAll("\\r", "").replace('\n', ' ');
                    ac.setActivityName(actName);
                    ac.setProcessName(actdef.getProcessName()); // in case subproc
                    ac.setName(ac.getProcessName() + ": " + ac.getActivityName());
                }
                else {
                    ac.setName("Unknown (" + ac.getDefinitionId() + ")");
                }

            }
        }
        return activityCounts;
    }

    public List<ActivityCount> getTopThroughputActivities(Query query) throws ServiceException {
        try {
            CodeTimer timer = new CodeTimer(true);
            List<ActivityCount> list = getAggregateDataAccess().getTopThroughputActivityInstances(query);
            timer.logTimingAndContinue("AggregateDataAccessVcs.getTopThroughputActivityInstances()");
            list = populateAct(list);
            timer.stopAndLogTiming("WorkflowServicesImpl.populate()");
            return list;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, "Error retrieving top throughput activities: query=" + query, ex);
        }
    }

    public Map<Date,List<ActivityCount>> getActivityInstanceBreakdown(Query query) throws ServiceException {
        try {
            Map<Date,List<ActivityCount>> map = getAggregateDataAccess().getActivityInstanceBreakdown(query);
            if (query.getFilters().get("activityIds") != null) {
                for (Date date : map.keySet())
                    populateAct(map.get(date));
            }
            return map;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, "Error retrieving activity instance breakdown: query=" + query, ex);
        }
    }

    public Process getProcessDefinition(String assetPath, Query query) throws ServiceException {

        int lastSlash = assetPath.lastIndexOf('/');
        if (lastSlash <= 0)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Bad asset path: " + assetPath);
        String packageName = assetPath.substring(0, lastSlash);
        String processName = assetPath.substring(lastSlash + 1);
        if (assetPath.endsWith(".proc"))
            processName = processName.substring(0, processName.length() - ".proc".length());
        else
            assetPath += ".proc";
        AssetInfo asset = new AssetInfo(ApplicationContext.getAssetRoot(), assetPath);
        if (!asset.getFile().isFile())
            throw new ServiceException(ServiceException.NOT_FOUND, "Process definition not found: " + assetPath);

        try {
            String version = query.getFilter("version"); // TODO honor version
            String json = new String(FileHelper.read(asset.getFile()));
            Process process = new Process(new JSONObject(json));
            process.setName(processName);
            process.setPackageName(packageName);
            return process;
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error reading: " + asset.getFile(), ex);
        }
    }

    public List<Process> getProcessDefinitions(Query query) throws ServiceException {
        try {
            String find = query.getFind();
            if (find == null) {
                return ProcessCache.getAllProcesses();
            }
            else {
                List<Process> found = new ArrayList<Process>();
                for (Process processVO : ProcessCache.getAllProcesses()) {
                    if (processVO.getName() != null && processVO.getName().startsWith(find))
                        found.add(processVO);
                    else if (find.indexOf(".") > 0 && processVO.getPackageName() != null && processVO.getPackageName().startsWith(find))
                        found.add(processVO);
                }
                return found;
            }
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, ex.getMessage(), ex);
        }
    }
    public ActivityList getActivityDefinitions(Query query) throws ServiceException {
        try {
            String find = query.getFind();
            List<ActivityInstanceInfo> activityInstanceList = new ArrayList<ActivityInstanceInfo>();
            ActivityList found = new ActivityList(ActivityList.ACTIVITY_INSTANCES, activityInstanceList);

            if (find == null) {
                List<Process> processes =  ProcessCache.getAllProcesses();
                for (Process processVO : processes) {
                    processVO =  ProcessCache.getProcess(processVO.getProcessId());
                    List<Activity> activities = processVO.getActivities();
                    for (Activity activityVO : activities) {
                        if (activityVO.getActivityName() != null && activityVO.getActivityName().startsWith(find))
                        {
                            ActivityInstanceInfo ai = new ActivityInstanceInfo();
                            ai.setId(activityVO.getActivityId());
                            ai.setName(activityVO.getActivityName());
                            ai.setDefinitionId(activityVO.getLogicalId());
                            ai.setProcessId(processVO.getProcessId());
                            ai.setProcessName(processVO.getProcessName());
                            ai.setProcessVersion(processVO.getVersionString());
                            activityInstanceList.add(ai);
                        }
                    }
                }
            }
            else {
                for (Process processVO : ProcessCache.getAllProcesses()) {
                    processVO =  ProcessCache.getProcess(processVO.getProcessId());
                    List<Activity> activities = processVO.getActivities();
                    for (Activity activityVO : activities) {
                        if (activityVO.getActivityName() != null && activityVO.getActivityName().startsWith(find))
                        {
                            ActivityInstanceInfo ai = new ActivityInstanceInfo();
                            ai.setId(activityVO.getActivityId());
                            ai.setName(activityVO.getActivityName());
                            ai.setDefinitionId(activityVO.getLogicalId());
                            ai.setProcessId(processVO.getProcessId());
                            ai.setProcessName(processVO.getProcessName());
                            ai.setProcessVersion(processVO.getVersionString());
                            activityInstanceList.add(ai);
                        }
                    }
                }
            }
            found.setRetrieveDate(DatabaseAccess.getDbDate());
            found.setCount(activityInstanceList.size());
            found.setTotal(activityInstanceList.size());
            return found;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, ex.getMessage(), ex);
        }
    }

    private static List<ActivityImplementor> activityImplementors;
    /**
     * Does not include pagelet.
     * TODO: cache should be refreshable
     */
    public List<ActivityImplementor> getImplementors() throws ServiceException {
        if (activityImplementors == null) {
            try {
                activityImplementors = DataAccess.getProcessLoader().getActivityImplementors();
                for (ActivityImplementor impl : activityImplementors) {
                    // qualify the icon location
                    if (impl.getIconName() != null && !impl.getIconName().startsWith("shape:")) {
                        String icon = impl.getIconName();
                        for (Package pkg : PackageCache.getPackages()) {
                            for (Asset asset : pkg.getAssets()) {
                                if (asset.getName().equals(icon)) {
                                    impl.setIconName(pkg.getName() + "/" + icon);
                                    break;
                                }
                            }
                        }
                    }
                    impl.setAttributeDescription(null);
                }
            }
            catch (CachingException ex) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
            }
            catch (DataAccessException ex) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
            }
        }
        return activityImplementors;
    }

    public ActivityImplementor getImplementor(String className) throws ServiceException {
        try {
            for (ActivityImplementor implementor : DataAccess.getProcessLoader().getActivityImplementors()) {
                String implClassName = implementor.getImplementorClassName();
                if (className == null) {
                    if (implClassName == null)
                        return implementor;
                }
                else if (implClassName.equals(className)) {
                    return implementor;
                }
            }
            return null;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    public Long launchProcess(Process process, String masterRequestId, String ownerType,
            Long ownerId, Map<String, String> params) throws ServiceException {
        try {
            ProcessEngineDriver driver = new ProcessEngineDriver();
            return driver.startProcess(process.getId(), masterRequestId, ownerType, ownerId, params, null);
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    public String invokeServiceProcess(Process process, String masterRequestId, String ownerType,
            Long ownerId, Map<String, String> params) throws ServiceException {
        try {
            ProcessEngineDriver driver = new ProcessEngineDriver();
            String masterRequest = params == null ? null : params.get("request");
            return driver.invokeService(process.getId(), ownerType, ownerId, masterRequestId,
                    masterRequest, params, null, null);
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    public Integer notify(String event, String message, int delay) throws ServiceException {
        try {
            EventManager eventManager = ServiceLocator.getEventManager();
            return eventManager.notifyProcess(event, 0L, message, delay);
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }
}
