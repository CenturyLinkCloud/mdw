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

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.query.QueryRequest;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.common.utilities.TransactionWrapper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.timer.CodeTimer;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.RuntimeDataAccess;
import com.centurylink.mdw.dataaccess.file.AggregateDataAccessVcs;
import com.centurylink.mdw.dataaccess.version4.CommonDataAccess;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.value.activity.ActivityCount;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.activity.ActivityInstance;
import com.centurylink.mdw.model.value.activity.ActivityList;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.asset.Asset;
import com.centurylink.mdw.model.value.asset.AssetHeader;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessCount;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessList;
import com.centurylink.mdw.model.value.process.ProcessRuntimeContext;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.dao.WorkflowDAO;
import com.centurylink.mdw.services.dao.process.EngineDataAccessDB;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;

public class WorkflowServicesImpl implements WorkflowServices {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private WorkflowDAO getWorkflowDao() {
        return new WorkflowDAO();
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
            TaskInstanceVO taskVo = taskMgr.getTaskInstance(taskInstanceId);
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
                ActivityInstanceVO activityVo = ServiceLocator.getEventManager().getActivityInstance(new Long(activityInstanceId).longValue());
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

    public ProcessInstanceVO getProcess(Long instanceId) throws ServiceException {
        return getProcess(instanceId, false);
    }

    public ProcessInstanceVO getProcess(Long instanceId, boolean withSubprocs) throws ServiceException {
        try {
            RuntimeDataAccess runtimeDataAccess = getRuntimeDataAccess();
            ProcessInstanceVO process = runtimeDataAccess.getProcessInstanceAll(instanceId);
            if (process == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Process instance not found: " + instanceId);
            if (withSubprocs) {
                // embedded subprocs
                Map<String,String> criteria = new HashMap<String,String>();
                criteria.put("owner", OwnerType.MAIN_PROCESS_INSTANCE);
                criteria.put("ownerId", process.getId().toString());
                criteria.put("processId", process.getProcessId().toString());
                ProcessList subprocList = runtimeDataAccess.getProcessInstanceList(criteria, 0, QueryRequest.ALL_ROWS, "order by process_instance_id");
                if (subprocList != null && subprocList.getItems() != null && subprocList.getItems().size() > 0) {
                    List<ProcessInstanceVO> subprocs = new ArrayList<ProcessInstanceVO>();
                    for (ProcessInstanceVO subproc : subprocList.getItems()) {
                        ProcessInstanceVO fullsub = runtimeDataAccess.getProcessInstance(subproc.getId());
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
        ProcessRuntimeContext runtimeContext = getContext(instanceId);
        Map<String,Value> values = new HashMap<String,Value>();
        Map<String,Object> variables = runtimeContext.getVariables();
        if (variables != null) {
            for (String key : variables.keySet()) {
                String stringVal = runtimeContext.getValueAsString(key);
                if (stringVal != null) {
                    Value value = new Value(key, stringVal);
                    values.put(key, value);
                    // TODO: process labels, etc (like autoform)
                }
            }
        }
        return values;
    }

    public Value getProcessValue(Long instanceId, String name) throws ServiceException {
        ProcessRuntimeContext runtimeContext = getContext(instanceId);
        if (!runtimeContext.isExpression(name) && runtimeContext.getProcess().getVariable(name) == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "No variable defined: " + name);
        String stringVal = runtimeContext.getValueAsString(name);
        if (stringVal == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "No value '" + name + "' found for instance: " + instanceId);
        Value value = new Value(name, stringVal);
        // TODO: process labels, etc (like autoform)
        return value;
    }

    public ProcessRuntimeContext getContext(Long instanceId) throws ServiceException {
        ProcessInstanceVO instance = getProcess(instanceId);
        ProcessVO process = ProcessVOCache.getProcessVO(instance.getProcessId());
        PackageVO pkg = PackageVOCache.getProcessPackage(instance.getProcessId());
        if (process == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Process definition not found for id: " + instance.getProcessId());

        Map<String,Object> vars = new HashMap<String,Object>();
        try {
            if (instance.getVariables() != null) {
                for (VariableInstanceInfo var : instance.getVariables()) {
                    Object value = var.getData();
                    if (value instanceof DocumentReference) {
                        DocumentVO docVO = getWorkflowDao().getDocument(((DocumentReference)value).getDocumentId());
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
            return getWorkflowDao().getProcessInstances(query);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, "Error retrieving process instance for query: " + query, ex);
        }
    }

    @Override
    public ActivityInstance getActivity(Long instanceId) throws ServiceException {
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
            ProcessVO process = ProcessVOCache.getProcessVO(pc.getId());
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
                pc.setVersion(RuleSetVO.formatVersion(process.getVersion()));
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
        List<ActivityInstance> aList = activityList.getActivities();
        ArrayList<ActivityInstance> matchActivities = new ArrayList<ActivityInstance>();
        for (ActivityInstance activityInstance : aList) {
            ProcessVO process = ProcessVOCache.getProcessVO(activityInstance.getProcessId());
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
                ActivityVO actdef = process.getActivityById(logicalId);
                if (actdef != null) {
                    activityInstance.setName(actdef.getActivityName().replaceAll("\\r", "").replace('\n', ' '));                }
                else {
                    activityInstance.setName("Unknown (" + activityInstance.getDefinitionId() + ")");
                }
                activityInstance.setProcessName(process.getName());
                activityInstance.setProcessVersion(RuleSetVO.formatVersion(process.getVersion()));
                activityInstance.setPackageName(process.getPackageName());
            }
        }

        String activityName = query.getFilter("activityName");
        if (activityName != null) {
            for (ActivityInstance activityInstance : aList) {
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
            ProcessVO process = ProcessVOCache.getProcessVO(ac.getProcessId());
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
                ac.setVersion(RuleSetVO.formatVersion(process.getVersion()));
                ac.setPackageName(process.getPackageName());
                String logicalId = ac.getDefinitionId();
                ActivityVO actdef = process.getActivityById(logicalId);
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

    public ProcessVO getProcessDefinition(String assetPath, Query query) throws ServiceException {

        int lastSlash = assetPath.lastIndexOf('/');
        if (lastSlash <= 0)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Bad asset path: " + assetPath);
        String packageName = assetPath.substring(0, lastSlash);
        String processName = assetPath.substring(lastSlash + 1);
        if (assetPath.endsWith(".proc"))
            processName = processName.substring(0, processName.length() - ".proc".length());
        else
            assetPath += ".proc";
        Asset asset = new Asset(ApplicationContext.getAssetRoot(), assetPath);
        if (!asset.getFile().isFile())
            throw new ServiceException(ServiceException.NOT_FOUND, "Process definition not found: " + assetPath);

        try {
            String version = query.getFilter("version"); // TODO honor version
            String json = new String(FileHelper.read(asset.getFile()));
            ProcessVO process = new ProcessVO(new JSONObject(json));
            process.setName(processName);
            process.setPackageName(packageName);
            return process;
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error reading: " + asset.getFile(), ex);
        }
    }

    public List<ProcessVO> getProcessDefinitions(Query query) throws ServiceException {
        try {
            String find = query.getFind();
            if (find == null) {
                return ProcessVOCache.getAllProcesses();
            }
            else {
                List<ProcessVO> found = new ArrayList<ProcessVO>();
                for (ProcessVO processVO : ProcessVOCache.getAllProcesses()) {
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
            List<ActivityInstance> activityInstanceList = new ArrayList<ActivityInstance>();
            ActivityList found = new ActivityList(ActivityList.ACTIVITY_INSTANCES, activityInstanceList);

            if (find == null) {
                List<ProcessVO> processes =  ProcessVOCache.getAllProcesses();
                for (ProcessVO processVO : processes) {
                    processVO =  ProcessVOCache.getProcessVO(processVO.getProcessId());
                    List<ActivityVO> activities = processVO.getActivities();
                    for (ActivityVO activityVO : activities) {
                        if (activityVO.getActivityName() != null && activityVO.getActivityName().startsWith(find))
                        {
                            ActivityInstance ai = new ActivityInstance();
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
                for (ProcessVO processVO : ProcessVOCache.getAllProcesses()) {
                    processVO =  ProcessVOCache.getProcessVO(processVO.getProcessId());
                    List<ActivityVO> activities = processVO.getActivities();
                    for (ActivityVO activityVO : activities) {
                        if (activityVO.getActivityName() != null && activityVO.getActivityName().startsWith(find))
                        {
                            ActivityInstance ai = new ActivityInstance();
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

    /**
     * TODO: these can be cached
     */
    public List<ActivityImplementorVO> getImplementors() throws ServiceException {
        try {
            List<ActivityImplementorVO> impls = DataAccess.getProcessLoader().getActivityImplementors();
            for (ActivityImplementorVO impl : impls) {
                // qualify the icon location
                if (impl.getIconName() != null && !impl.getIconName().startsWith("shape:")) {
                    String icon = impl.getIconName();
                    for (PackageVO pkg : PackageVOCache.getPackages()) {
                        for (RuleSetVO ruleSet : pkg.getRuleSets()) {
                            if (ruleSet.getName().equals(icon)) {
                                impl.setIconName(pkg.getName() + "/" + icon);
                                break;
                            }
                        }
                    }
                }
            }
            return impls;
        }
        catch (CachingException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    public ActivityImplementorVO getImplementor(String className) throws ServiceException {
        for (ActivityImplementorVO implementor : getImplementors()) {
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

}
