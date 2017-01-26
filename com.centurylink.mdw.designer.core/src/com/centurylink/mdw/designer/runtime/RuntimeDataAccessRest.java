/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.runtime;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlException;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.types.ActionRequestMessage;
import com.centurylink.mdw.common.task.TaskList;
import com.centurylink.mdw.dataaccess.DataAccessOfflineException;
import com.centurylink.mdw.dataaccess.RuntimeDataAccess;
import com.centurylink.mdw.designer.ServerAccessRest;
import com.centurylink.mdw.designer.utils.RestfulServer;
import com.centurylink.mdw.model.data.event.EventLog;
import com.centurylink.mdw.model.value.activity.ActivityList;
import com.centurylink.mdw.model.value.event.ExternalMessageVO;
import com.centurylink.mdw.model.value.process.LinkedProcessInstance;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessList;
import com.centurylink.mdw.model.value.task.TaskActionVO;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;

/**
 * Designer runtime data access through REST communication with server for VCS Assets.
 */
public class RuntimeDataAccessRest extends ServerAccessRest implements RuntimeDataAccess {

    public RuntimeDataAccessRest(RestfulServer server) {
        super(server);
    }

    public ProcessList getProcessInstanceList(Map<String, String> criteria, int pageIndex, int pageSize, String orderBy)
    throws DataAccessException {
        return getProcessInstanceList(criteria, null, pageIndex, pageSize, orderBy);
    }

    public ProcessList getProcessInstanceList(Map<String,String> criteria, Map<String,String> variables, int pageIndex, int pageSize, String orderBy)
    throws DataAccessException {
        try {
            String pathWithArgs = "ProcessInstances?format=json";
            String criteriaParams = queryParams(criteria);
            if (criteriaParams.length() > 0)
                pathWithArgs += "&" + criteriaParams;
            String varParams = queryParams(variables);
            if (varParams.length() > 0)
                pathWithArgs += "&" + varParams;
            if (pageIndex > 0)
                pathWithArgs += "&pageIndex=" + pageIndex;
            if (pageSize != 0)
                pathWithArgs += "&pageSize=" + pageSize;
            if (orderBy != null) {
                pathWithArgs += "&orderBy=" + URLEncoder.encode(orderBy.trim(), "UTF-8");
            }
            String response = invokeResourceService(pathWithArgs);
            return new ProcessList(ProcessList.PROCESS_INSTANCES, response);
        }
        catch (IOException ex) {
            throw new DataAccessOfflineException(ex.getMessage(), ex);
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public ProcessList getProcessInstanceList(Map<String,String> criteria, List<String> variableNames, Map<String,String> variables, int pageIndex, int pageSize, String orderBy)
    throws DataAccessException {
        if (variableNames == null)
            return getProcessInstanceList(criteria, variables, pageIndex, pageSize, orderBy);
        else
            throw new DataAccessException("Parameter variableNames currently not supported.");
    }

    public ProcessInstanceVO getProcessInstanceAll(Long processInstanceId) throws DataAccessException {
        try {
            String pathWithArgs = "ProcessInstance?format=json&instanceId=" + processInstanceId;
            String response = invokeResourceService(pathWithArgs);
            return new ProcessInstanceVO(new JSONObject(response));
        }
        catch (IOException ex) {
            throw new DataAccessOfflineException(ex.getMessage(), ex);
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public ProcessInstanceVO getProcessInstanceBase(Long processInstanceId) throws DataAccessException {
        try {
            String pathWithArgs = "ProcessInstance?format=json&shallow=true&instanceId=" + processInstanceId;
            String response = invokeResourceService(pathWithArgs);
            return new ProcessInstanceVO(new JSONObject(response));
        }
        catch (IOException ex) {
            throw new DataAccessOfflineException(ex.getMessage(), ex);
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public List<ProcessInstanceVO> getProcessInstanceList(String owner, String secondaryOwner, Long secondaryOwnerId, String orderBy) throws DataAccessException {
        throw new UnsupportedOperationException("Currently not supported");
    }

    public List<TaskInstanceVO> getTaskInstancesForProcessInstance(Long processInstanceId) throws DataAccessException {
        try {
            String pathWithArgs = "ProcessTasks?format=json&processInstanceId=" + processInstanceId;
            String response = invokeResourceService(pathWithArgs);
            return new TaskList(TaskList.PROCESS_TASKS, response).getItems();
        }
        catch (IOException ex) {
            throw new DataAccessOfflineException(ex.getMessage(), ex);
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public DocumentVO getDocument(Long documentId) throws DataAccessException {
        try {
            String pathWithArgs = "DocumentValue?format=json&id=" + documentId;
            String response = getServer().invokeResourceServiceRaw(pathWithArgs);
            DocumentVO docVO = new DocumentVO();
            docVO.setContent(response);
            docVO.setDocumentId(documentId);
            return docVO;
        }
        catch (IOException ex) {
            throw new DataAccessOfflineException(ex.getMessage(), ex);
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public boolean hasProcessInstances(Long processId) throws DataAccessException {
        try {
            String pathWithArgs = "ProcessInstances?format=json&pageSize=1&processId=" + processId;
            String response = invokeResourceService(pathWithArgs);
            return new ProcessList(ProcessList.PROCESS_INSTANCES, response).getCount() > 0;
        }
        catch (IOException ex) {
            throw new DataAccessOfflineException(ex.getMessage(), ex);
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public int deleteProcessInstances(List<Long> processInstanceIds) throws DataAccessException {
        try {
            List<ProcessInstanceVO> instances = new ArrayList<ProcessInstanceVO>();
            for (Long id : processInstanceIds)
                instances.add(new ProcessInstanceVO(id));
            ProcessList processList = new ProcessList(ProcessList.PROCESS_INSTANCES, instances);
            if (getServer().getSchemaVersion() >= 6000) {
                try {
                    getServer().delete("Processes", processList.getJson().toString(2));
                }
                catch (IOException ex) {
                    throw new DataAccessOfflineException("Unable to connect to " + getServer().getServiceUrl(), ex);
                }
            }
            else {
                ActionRequestMessage actionRequest = new ActionRequestMessage();
                actionRequest.setAction("DeleteProcessInstances");
                actionRequest.addParameter("appName", "MDW Designer");
                JSONObject msgJson = actionRequest.getJson();
                msgJson.put(processList.getJsonName(), processList.getJson());
                invokeActionService(msgJson.toString(2));
            }
            return processList.getCount();
        }
        catch (XmlException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
        catch (JSONException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public int deleteProcessInstancesForProcess(Long processId) throws DataAccessException {
        try {
            if (getServer().getSchemaVersion() >= 6000) {
                try {
                    getServer().delete("Processes?processId=" + processId, null);
                }
                catch (IOException ex) {
                    throw new DataAccessOfflineException("Unable to connect to " + getServer().getServiceUrl(), ex);
                }
            }
            else {
                ActionRequestMessage actionRequest = new ActionRequestMessage();
                actionRequest.setAction("DeleteProcessInstances");
                actionRequest.addParameter("appName", "MDW Designer");
                actionRequest.addParameter("processId", String.valueOf(processId));
                invokeActionService(actionRequest.getJson().toString(2));
            }
            return 0; // not used
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    // used by designer for displaying adapter raw message content
    public ExternalMessageVO getExternalMessage(Long activityId, Long activityInstId, Long eventInstId) throws DataAccessException {
        try {
            String pathWithArgs = "ExternalMessageInstance?format=json&activityId=" + activityId + "&activityInstId=" + activityInstId;

            if (eventInstId != null) {
                pathWithArgs += "&eventInstId=" + eventInstId;
            }
            String response = invokeResourceService(pathWithArgs);
            return new ExternalMessageVO(response);
        }
        catch (IOException ex) {
            throw new DataAccessOfflineException(ex.getMessage(), ex);
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public LinkedProcessInstance getProcessInstanceCallHierarchy(Long processInstanceId) throws DataAccessException {
        try {
            String pathWithArgs = "ProcessInstances?format=json&callHierarchyFor=" + processInstanceId;
            String response = invokeResourceService(pathWithArgs);
            return new LinkedProcessInstance(response);
        }
        catch (IOException ex) {
            throw new DataAccessOfflineException(ex.getMessage(), ex);
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    // used by designer for updating variables
    public void updateVariableInstance(VariableInstanceInfo var) throws DataAccessException {
        // for now DesignerDataAccess connects directly to the server using the old service
        // TODO implement this as an authorized service in the new REST API
    }

    // used by designer for updating variables
    public void updateDocumentContent(Long documentId, String content) throws DataAccessException {
        // for now DesignerDataAccess connects directly to the server using the old service
        // TODO implement this as an authorized service in the new REST API
    }

    /*
     * methods not relevant for Rest data access (either obsolete or used by runtime container -- not Designer)
     */

    // used indirectly by ImageHelperServlet (obsolete)
    public ProcessInstanceVO getProcessInstanceForSecondary(String pSecOwner, Long pSecOwnerId) throws DataAccessException {
        return null;
    }
    public ProcessInstanceVO getCauseForTaskInstance(Long pTaskInstanceId) throws DataAccessException {
        return null;
    }

    // used by designer for old FormDataDocument manual tasks (obsolete)
    public String getExternalEventDetails(Long externalEventId) throws DataAccessException {
        return null;
    }

    // used by regression test event handler (not designer)
    public List<Long> findTaskInstance(Long taskId, String masterRequestId) throws DataAccessException {
        return null;
    }

    // used for searching documents (from EventManager -- not designer)
    public List<DocumentVO> findDocuments(Long procInstId, String type, String searchKey1,
            String searchKey2, String ownerType, Long ownerId, Date createDateStart,
            Date createDateEnd, String orderByClause) throws DataAccessException {
        return null;
    }

    // used by EventManager (runtime)
    public List<EventLog> getEventLogs(String pEventName, String pEventSource, String pEventOwner, Long pEventOwnerId)
    throws DataAccessException {
        return null;
    }

    // used by TaskActions service (not designer)
    public List<TaskActionVO> getUserTaskActions(String[] groups, Date startDate) throws DataAccessException {
        return null;
    }

    // used by BLV from runtime container
    public ProcessInstanceVO getProcessInstanceForCalling(Long procInstId) throws DataAccessException {
        return null;
    }

    // used by AdminUI Workflow tab (not designer)
    public ActivityList getActivityInstanceList(Query query) throws DataAccessException {
        throw new UnsupportedOperationException("Only supported for VCS Assets");
    }

    // not used by designer (TODO: implement this and update calls to handle null return value)
    public ProcessInstanceVO getProcessInstance(Long instanceId) throws DataAccessException {
        return null;
    }
}
