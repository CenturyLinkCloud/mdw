/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.value.activity.ActivityCount;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.activity.ActivityInstance;
import com.centurylink.mdw.model.value.activity.ActivityList;
import com.centurylink.mdw.model.value.process.ProcessCount;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessList;
import com.centurylink.mdw.model.value.process.ProcessRuntimeContext;
import com.centurylink.mdw.model.value.process.ProcessVO;

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

    public void registerTaskWaitEvent(Long taskInstanceId, String eventName)
            throws ServiceException;

    public void registerTaskWaitEvent(Long taskInstanceId, String eventName, String completionCode)
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
            String completionCode, boolean recurring) throws ServiceException;
    /**
     * @param activityInstanceId
     * @param action
     * @param completionCode
     */
    public void actionActivity(String activityInstanceId, String action, String completionCode)
            throws ServiceException;

    ProcessInstanceVO getProcess(Long instanceId) throws ServiceException;
    ProcessInstanceVO getProcess(Long instanceId, boolean withSubprocs) throws ServiceException;

    ProcessRuntimeContext getContext(Long instanceId) throws ServiceException;

    Map<String,Value> getProcessValues(Long instanceId) throws ServiceException;
    Value getProcessValue(Long instanceId, String name) throws ServiceException;

    ProcessList getProcesses(Query query) throws ServiceException;

    public ActivityList getActivities(Query query) throws ServiceException;

    public List<ProcessCount> getTopThroughputProcesses(Query query) throws ServiceException;

    public Map<Date,List<ProcessCount>> getProcessInstanceBreakdown(Query query) throws ServiceException;

    public List<ActivityCount> getTopThroughputActivities(Query query) throws ServiceException;

    public Map<Date,List<ActivityCount>> getActivityInstanceBreakdown(Query query) throws ServiceException;

    public List<ProcessVO> getProcessDefinitions(Query query) throws ServiceException;
    public ProcessVO getProcessDefinition(String assetPath, Query query) throws ServiceException;

    public ActivityList getActivityDefinitions(Query query) throws ServiceException;

    public ActivityInstance getActivity(Long instanceId) throws ServiceException;

    public List<ActivityImplementorVO> getImplementors() throws ServiceException;
    public ActivityImplementorVO getImplementor(String className) throws ServiceException;
}
