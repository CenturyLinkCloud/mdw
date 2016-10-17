/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.task;

import static com.centurylink.mdw.common.constant.TaskAttributeConstant.AUTO_ASSIGNEE;
import static com.centurylink.mdw.common.constant.TaskAttributeConstant.COMMENTS;
import static com.centurylink.mdw.common.constant.TaskAttributeConstant.DUE_DATE;
import static com.centurylink.mdw.common.constant.TaskAttributeConstant.GROUPS;
import static com.centurylink.mdw.common.constant.TaskAttributeConstant.PRIORITY;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jms.JMSException;
import javax.naming.NamingException;

import org.apache.xmlbeans.XmlException;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.common.constant.JMSDestinationNames;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.exception.MDWException;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.exception.ServiceLocatorException;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.service.types.ActionRequestMessage;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.common.utilities.JMSServices;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.data.task.TaskAction;
import com.centurylink.mdw.model.data.task.TaskStatus;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskException;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.UserManager;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;
import com.centurylink.mdw.services.messenger.IntraMDWMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.services.status.GlobalApplicationStatus;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengNode;

/**
 * This class provides engine access to task managers,
 * and provides accesses between summary and detail task managers.
 *
 * It hides the difference between local and remote task managers.
 *
 */
public class TaskManagerAccess {

    public static final String UPDATE_TASK = "UpdateTask" ;
    private static TaskManagerAccess singleton = null;
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private String taskManagerJndi;
    private String taskManagerQueueName;
    private String applicationName;
    private int timeout;


    public static TaskManagerAccess getInstance() {
        if (singleton==null) {
            singleton = new TaskManagerAccess();
        }
        return singleton;
    }

    private TaskManagerAccess() {
        taskManagerJndi = PropertyManager.getProperty(PropertyNames.MDW_TASKMANAGER_REMOTE_JNDI);
        taskManagerQueueName = PropertyManager.getProperty(PropertyNames.MDW_TASKMANAGER_REMOTE_QUEUENAME);
        if (taskManagerQueueName==null) taskManagerQueueName = JMSDestinationNames.INTRA_MDW_EVENT_HANDLER_QUEUE;
        try {
            applicationName = PropertyManager.getProperty(PropertyNames.APPLICATION_NAME)
                    + "@" + MessengerFactory.getEngineUrl();
        } catch (NamingException e) {
            logger.severeException(e.getMessage(), e);
            applicationName = PropertyManager.getProperty(PropertyNames.APPLICATION_NAME);
        }
        try {
            String v = PropertyManager.getProperty("MDWFramework.TaskManagerWeb/task.create.jms.timeout");
            timeout = (v!=null)?Integer.parseInt(v):30;
        }
        catch (Exception ex) {
            timeout = 30;
        }
    }

    private String invokeEngine(String request, int timeout)
        throws NamingException, MDWException {
        if (taskManagerQueueName.equals(JMSDestinationNames.INTRA_MDW_EVENT_HANDLER_QUEUE)) {
            IntraMDWMessenger msgbroker = MessengerFactory.newIntraMDWMessenger(taskManagerJndi);
            return msgbroker.invoke(request, timeout);
        } else {
            try {
                return JMSServices.getInstance().invoke(taskManagerJndi, taskManagerQueueName, request, timeout);
            } catch (Exception e) {
                throw new MDWException("Failed to invoke engine service", e);
            }
        }
    }

    public String notifySummaryTaskManager(FormDataDocument formdatadoc)
            throws NamingException, ServiceLocatorException, MDWException {
        formdatadoc.setAttribute(FormDataDocument.ATTR_APPNAME, applicationName);
        return invokeEngine(formdatadoc.format(), timeout);
    }

    public List<StatusMessage> notifyDetailTaskManagers(String action, Jsonable json)
    throws NamingException, XmlException, JSONException, MDWException {
        List<Jsonable> jsons = new ArrayList<Jsonable>();
        jsons.add(json);
        return notifyDetailTaskManagers(action, jsons);
    }

    public List<StatusMessage> notifyDetailTaskManagers(String action, List<Jsonable> jsons)
    throws NamingException, XmlException, JSONException, MDWException {
        // build the JSON request
        ActionRequestMessage msg = new ActionRequestMessage();
        msg.setAction(action);
        msg.addParameter("appName", applicationName);
        JSONObject msgJson = msg.getJson();
        if (jsons != null) {
            for (Jsonable json : jsons) {
                msgJson.put(json.getJsonName(), json.getJson());
            }
        }

        // invoke the service(s)
        // TODO: ability to filter (notify only a subset)
        try {
            List<StatusMessage> responses = new ArrayList<StatusMessage>();
            Map<String,String> detailUrls = getDetailTaskManagerServiceUrls();
            for (String serverName : detailUrls.keySet()) {
                String serverUrl = detailUrls.get(serverName);
                if (logger.isDebugEnabled())
                    logger.debug("Notifying Detail TaskManager '" + serverName + "' at " + serverUrl + ", action=" + action);
                try {
                    responses.add(notifyDetailTaskManager(msgJson.toString(2), serverUrl));
                }
                catch (Exception ex) {
                    logger.severeException(ex.getMessage(), ex);  // don't interfere with other notifications
                    responses.add(new StatusMessage(ex,serverName));
                }
            }
            return responses;
        }
        catch (PropertyException ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new MDWException(ex.getMessage(), ex);
        }
    }

    @Deprecated
    public Map<String, String> getDetailTaskManagerUrls() throws PropertyException {
        return getDetailTaskManagerServiceUrls();
    }

    public Map<String, String> getDetailTaskManagerServiceUrls() throws PropertyException {
        Map<String, String> urls = new HashMap<String, String>();
        Properties props = PropertyManager.getInstance().getProperties(PropertyNames.MDW_REMOTE_SERVER);
        for (Object key : props.keySet()) {
            String serverName = key.toString().substring(PropertyNames.MDW_REMOTE_SERVER.length() + 1);
            urls.put(serverName, props.getProperty(key.toString()));
        }
        return urls;
    }

    public String getSummaryTaskManagerServiceUrl() {
        return taskManagerJndi;
    }

    public StatusMessage notifyDetailTaskManager(String action, Jsonable json, String detailOwnerApp)
    throws NamingException, XmlException, JSONException, MDWException {
        // build the JSON request
        ActionRequestMessage msg = new ActionRequestMessage();
        msg.setAction(action);
        msg.addParameter("appName", applicationName);
        JSONObject msgJson = msg.getJson();
        if (json != null) {
            msgJson.put(json.getJsonName(), json.getJson());
        }
        String detailUrl = getDetailTaskManagerServiceUrls().get(detailOwnerApp);
        if (detailUrl == null)
            throw new MDWException("No detail URL found for detailOwnerApp=" + detailOwnerApp);
        return notifyDetailTaskManager(msgJson.toString(2), detailUrl);
    }

    public StatusMessage notifyDetailTaskManager(String request, String detailTaskManagerUrl)
    throws NamingException, ServiceLocatorException, MDWException {
        return notify(request, detailTaskManagerUrl);
    }

    public StatusMessage notifySummaryTaskManager(String action, Jsonable json)
    throws NamingException, XmlException, JSONException, MDWException {
        List<Jsonable> jsons = new ArrayList<Jsonable>(1);
        jsons.add(json);
        return notifySummaryTaskManager(action, jsons);
    }

    public StatusMessage notifySummaryTaskManager(String action, List<Jsonable> jsons)
    throws NamingException, XmlException, JSONException, MDWException {
        // build the JSON request
        ActionRequestMessage msg = new ActionRequestMessage();
        msg.setAction(action);
        msg.addParameter("appName", applicationName);
        JSONObject msgJson = msg.getJson();
        if (jsons != null) {
            for (Jsonable json : jsons) {
                msgJson.put(json.getJsonName(), json.getJson());
            }
        }

        String response = invokeEngine(msgJson.toString(2), timeout);
        return new StatusMessage(new JSONObject(response));
    }


    public AuthenticatedUser getUserAuthorization(String cuid, boolean allowUnauthorized)
        throws RemoteException
    {
        AuthenticatedUser user;
        try {
            UserManager userMgr = ServiceLocator.getUserManager();
            user = userMgr.loadUser(cuid);
            if (user==null) throw new Exception("User does not exist");
        } catch (Exception e) {
            String errmsg = "Failed to obtain user authorization";
            logger.severeException(errmsg, e);
            if (allowUnauthorized) user = createAdHocUser(cuid);
            else throw new RemoteException(errmsg);
        }
        return user;
    }

    private AuthenticatedUser createAdHocUser(String cuid) {
        AuthenticatedUser user = new AuthenticatedUser();
        user.setId(0L);
        user.setCuid(cuid);
        user.setWorkgroups(new UserGroupVO[0]);
        user.setAllowableActions(new TaskAction[0]);
        user.setAttributes(new HashMap<String,String>());
        return user;
    }

    public void sendMessage(FormDataDocument formdatadoc)
    throws NamingException, MDWException {
        formdatadoc.setMetaValue(FormDataDocument.ATTR_APPNAME, null);
        IntraMDWMessenger msgbroker = MessengerFactory.newIntraMDWMessenger(null);
        msgbroker.sendMessage(formdatadoc.format());
    }

    public StatusMessage notify(String request, String url)
    throws NamingException, MDWException {
        IntraMDWMessenger msgbroker = MessengerFactory.newIntraMDWMessenger(url);
        try {
            String response = msgbroker.invoke(request, timeout);
            return new StatusMessage(new JSONObject(response));
        }
        catch (Exception ex) {
            throw new MDWException(ex.getMessage(), ex);
        }
    }

    /**
     * Create general task instance
     * task logical id can be MDW4_<actId> for in-flight compatibility
     * @param formdata
     * @return
     * @throws NamingException
     * @throws JMSException
     * @throws ProcessException
     * @throws XmlException
     * @throws MbengException
     * @throws ServiceLocatorException
     */
    public Long createGeneralTaskInstance(FormDataDocument formdata)
            throws NamingException, MDWException, MbengException, JSONException {
        formdata.setAttribute(FormDataDocument.ATTR_APPNAME, null);
        IntraMDWMessenger msgbroker = MessengerFactory.newIntraMDWMessenger(null);
        String response = msgbroker.invoke(formdata.format(), timeout);
        String taskInstanceId = parseTaskInstanceId(response);
        return new Long(taskInstanceId);
    }

    /**
     * create classic task instance
     * task logical id can be MDW4_<actId> for in-flight compatibility
     * @throws ServiceLocatorException
     * @throws ProcessException
     */
    public void createClassicTaskInstance(String taskLogicalId, Long procInstId, Long actInstId, Long transInstId,
            String masterRequestId, String errmsg)
    throws NamingException, MDWException {
        FormDataDocument formdatadoc = new FormDataDocument();
        formdatadoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_CREATE_TASK);
        formdatadoc.setMetaValue(FormDataDocument.META_ACTIVITY_INSTANCE_ID, actInstId.toString());
        formdatadoc.setMetaValue(FormDataDocument.META_TASK_LOGICAL_ID, taskLogicalId);
        formdatadoc.setMetaValue(FormDataDocument.META_PROCESS_INSTANCE_ID, procInstId.toString());
        formdatadoc.setMetaValue(FormDataDocument.META_TASK_TRANS_INST_ID, transInstId.toString());
        formdatadoc.setMetaValue(FormDataDocument.META_MASTER_REQUEST_ID, masterRequestId);
        if (errmsg!=null) formdatadoc.setMetaValue(FormDataDocument.META_TASK_ERRMSG, errmsg);
        sendMessage(formdatadoc);
    }

    public String parseTaskInstanceId(String response)
            throws ProcessException, JSONException, MbengException {
        FormDataDocument datadoc = new FormDataDocument();
        datadoc.load(response);
        String v = datadoc.getMetaValue(FormDataDocument.META_STATUS);
        if (v==null || v.equals("0")) {
            v = datadoc.getMetaValue(FormDataDocument.META_TASK_INSTANCE_ID);
            if (v!=null) return v;
            v = datadoc.getMetaValue(FormDataDocument.META_PROMPT);
            if (v==null) throw new ProcessException("Unknown error in task creation");
            if (!v.startsWith(TaskActivity.TASK_CREATE_RESPONSE_ID_PREFIX))
                throw new ProcessException(v);
            int s = TaskActivity.TASK_CREATE_RESPONSE_ID_PREFIX.length();
            int e = v.indexOf(',', s);
            return v.substring(s,e);
        } else {
            List<String> errorList = datadoc.getErrors();
            if (errorList.size()>0) throw new ProcessException(errorList.get(0));
            else throw new ProcessException("Unknown error in task creation");
        }
    }

    /**
     * Get local task instance, and if the local task instance is for detail only,
     * retrieve also summary task instance and override the local copy
     * (override in memory only - does not override local database copy)
     *
     * @param taskInstId
     * @return
     * @throws TaskException
     * @throws DataAccessException
     */
    public TaskInstanceVO getTaskInstance(Long taskInstId)
            throws TaskException, DataAccessException {
        TaskManager taskManager = ServiceLocator.getTaskManager();
        TaskInstanceVO taskInst = taskManager.getTaskInstance(taskInstId);
        if (taskInst==null) throw new DataAccessException("Task " + taskInstId + " does not exist");
        if (taskInst.isDetailOnly()) {
            try {
                FormDataDocument calldoc = new FormDataDocument();
                calldoc.setMetaValue(FormDataDocument.META_TASK_INSTANCE_ID,
                        taskInst.getAssociatedTaskInstanceId().toString());
                calldoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_GET_TASK);
                String response = notifySummaryTaskManager(calldoc);
                calldoc.load(response);
                String status = calldoc.getMetaValue(FormDataDocument.META_STATUS);
                if (status!=null && !status.equals("0")) {
                    List<String> errors = calldoc.getErrors();
                    if (errors.size()>0) throw new Exception(errors.get(0));
                    else throw new Exception("Unknown error");
                }
                taskInst.setStartDate(calldoc.getMetaValue(FormDataDocument.META_TASK_START_DATE));
                taskInst.setEndDate(calldoc.getMetaValue(FormDataDocument.META_TASK_END_DATE));
                taskInst.setDueDate(StringHelper.stringToDate(calldoc.getMetaValue(FormDataDocument.META_TASK_DUE_DATE)));
                String v = calldoc.getMetaValue(FormDataDocument.META_TASK_STATUS);
                for (int i=0; i<TaskStatus.allStatusNames.length; i++) {
                    if (v.equals(TaskStatus.allStatusNames[i])) {
                        taskInst.setStatusCode(TaskStatus.allStatusCodes[i]);
                        break;
                    }
                }
                taskInst.setTaskClaimUserCuid(calldoc.getMetaValue(FormDataDocument.META_TASK_ASSIGNEE));
                taskInst.setTaskName(calldoc.getMetaValue(FormDataDocument.META_TASK_NAME));
                taskInst.setOrderId(calldoc.getMetaValue(FormDataDocument.META_MASTER_REQUEST_ID));
                taskInst.setComments(calldoc.getMetaValue(FormDataDocument.META_TASK_COMMENT));
                // indicate non-shallow and template based
                taskInst.setGroups(new ArrayList<String>());
            } catch (Exception e) {
                throw new TaskException("Failed to retrieve summary task instance", e);
            }
        } else {
            taskManager.getTaskInstanceAdditionalInfo(taskInst);
        }
        return taskInst;
    }

    public UserVO[] getAssignableUsersForTaskInstance(TaskInstanceVO taskInst) throws Exception {
        UserVO[] users;
        if (taskInst.isDetailOnly()) {
            FormDataDocument calldoc = new FormDataDocument();
            calldoc.setMetaValue(FormDataDocument.META_ACTION, FormConstants.ACTION_AUTHORIZE);
            calldoc.setMetaValue(FormDataDocument.META_TASK_INSTANCE_ID,
                    taskInst.getAssociatedTaskInstanceId().toString());
            String response = this.invokeEngine(calldoc.format(), timeout);
            calldoc.load(response);
            if (calldoc.hasErrors()) {
                throw new Exception(calldoc.getErrors().get(0));
            } else {
                MbengNode usersNode = calldoc.getNode("users");
                List<UserVO> userlist = new ArrayList<UserVO>();
                for (MbengNode one=usersNode.getFirstChild();
                    one!=null; one=one.getNextSibling()) {
                    UserVO user = new UserVO();
                    user.setId(new Long(calldoc.getValue(one,"id")));
                    user.setCuid(calldoc.getValue(one,"cuid"));
                    userlist.add(user);
                }
                users = userlist.toArray(new UserVO[userlist.size()]);
            }
        } else {
            List<String> groups;
            if (taskInst.isTemplateBased()) groups = taskInst.getGroups();
            else {
                TaskManager taskMgr = ServiceLocator.getTaskManager();
                groups = taskMgr.getGroupsForTaskInstance(taskInst);
            }
            UserManager userMgr = ServiceLocator.getUserManager();
            users = userMgr.getUsersForGroups(groups.toArray(new String[groups.size()]));
        }
        return users;
    }

    public void cancelTasksOfActivityInstance(Long actInstId, Long procInstId)
    throws NamingException, MDWException {
        FormDataDocument formdatadoc = new FormDataDocument();
        formdatadoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_CANCEL_TASKS);
        formdatadoc.setMetaValue(FormDataDocument.META_ACTIVITY_INSTANCE_ID, actInstId.toString());
        if (procInstId!=null) formdatadoc.setMetaValue(FormDataDocument.META_PROCESS_INSTANCE_ID, procInstId.toString());
        sendMessage(formdatadoc);
    }

    public void cancelTasksOfProcessInstances(List<Long> procInstIdList)
        throws NamingException, MbengException, SQLException, MDWException {
        FormDataDocument formdatadoc = new FormDataDocument();
        formdatadoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_CANCEL_TASKS);
        formdatadoc.setMetaValue(FormDataDocument.META_PROCESS_INSTANCE_ID, procInstIdList.get(0).toString());
        MbengNode table = formdatadoc.setTable("ProcessInstances");
        for (Long procInstId : procInstIdList) {
            formdatadoc.addEntry(table, procInstId.toString());
        }
        sendMessage(formdatadoc);
    }

    /**
     * Complete/cancel task instance. Called when initiated from task detail view.
     *
     * Implementation note for complete/cancel tasks from summary view (not covered
     * by this method):
     *   - invoke TaskManager.performActionOnTaskInstance, which sets status and
     *           perform audit/notification/etc
     *   - send a message to engine with action FormConstants.ACTION_COMPLET/CANCEL_TASK
     *           using EngineAccess.sendMessageToEngine
     *           (sample implementation in FormServer.processTaskCloseAction)
     *           This works in all cases, including the case when summary
     *           and detail views are hosted by different task manager, in which
     *           case detail task manager does not need to know.
     * There is no need to call this method.
     *
     * @param taskInst
     * @param taskAction TaskAction.COMPLETE/RETRY/CANCEL/ABORT
     * @param cuid
     * @param comment
     * @throws Exception
     */
    public void closeTaskInstance(TaskInstanceVO taskInst, String taskAction, String cuid, String comment)
            throws Exception {
        TaskManager taskManager = ServiceLocator.getTaskManager();
        if (taskInst.isDetailOnly()) {
            taskManager.closeTaskInstance(taskInst, taskAction, comment);
            FormDataDocument calldoc = new FormDataDocument();
            calldoc.setMetaValue(FormDataDocument.META_TASK_INSTANCE_ID,
                    taskInst.getAssociatedTaskInstanceId().toString());
            calldoc.setMetaValue(FormDataDocument.META_USER, cuid);
            calldoc.setAttribute(FormDataDocument.ATTR_ACTION,
                    FormConstants.ACTION_COMPLETE_TASK + "?Recepient=SummaryTaskManager");
            calldoc.setValue("TaskAction", taskAction);
            if (comment!=null) calldoc.setValue("Comment", comment);
            String response = TaskManagerAccess.getInstance().notifySummaryTaskManager(calldoc);
            String errmsg = FormDataDocument.parseSimpleResponse(response);
            if (errmsg!=null) throw new TaskException(errmsg);
            // does not update local copy - expect the caller will call getTaskInstance
            // in this class which gets remote instance
        } else {
            UserVO user = UserGroupCache.getUser(cuid);
            taskManager.performActionOnTaskInstance(taskAction,
                    taskInst.getTaskInstanceId(), user.getId(), null, comment, null, false);
        }
    }

    public List<String> getGroupEmailAddresses(String[] groups)
    throws ActivityException {
        try {
            UserManager userManager = ServiceLocator.getUserManager();
            List<String> addressList = userManager.getEmailAddressesForGroups(groups);
            return addressList;
        }
        catch (Exception e) {
            logger.severeException(e.getMessage(), e);
            throw new ActivityException(-1, e.getMessage(), e);
        }
    }

    public void updateTaskInstanceDueDate(TaskInstanceVO taskInst, Date dueDate, String cuid, String comment)
            throws TaskException {
        try {
            TaskManager taskManager = ServiceLocator.getTaskManager();
            if (taskInst.isDetailOnly()) {
                FormDataDocument calldoc = new FormDataDocument();
                calldoc.setMetaValue(FormDataDocument.META_TASK_INSTANCE_ID,
                        taskInst.getAssociatedTaskInstanceId().toString());
                calldoc.setMetaValue(FormDataDocument.META_USER, cuid);
                calldoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_CHANGE_DUEDATE_TASK);
                calldoc.setMetaValue(FormDataDocument.META_TASK_DUE_DATE, StringHelper.dateToString(dueDate));
                String response = TaskManagerAccess.getInstance().notifySummaryTaskManager(calldoc);
                String errmsg = FormDataDocument.parseSimpleResponse(response);
                if (errmsg!=null) throw new RemoteException(errmsg);
            } else {
                taskManager.updateTaskInstanceDueDate(taskInst.getTaskInstanceId(), dueDate, cuid, comment);
            }
        } catch (Exception e) {
            throw new TaskException("Failed to update task instance due date", e);
        }
    }

    public void processTaskInstanceUpdate(final Map<String, Object> changesMap, final TaskInstanceVO taskInst,
            final String cuid) throws TaskException {
        try {
            TaskManager taskManager = ServiceLocator.getTaskManager();

            // Get dueDate, priority after applying prioritization strategy - Also get workgroups and assignee after reapplying the routing strategy
            final Map<String, Object> changes = taskManager.getChangesAfterApplyStrategy(changesMap, taskInst);
            UserVO assignee = (UserVO) changes.get(AUTO_ASSIGNEE);
            Long autoAssignee = assignee != null ? assignee.getId() : null;
            final String autoAssigneeCuid = assignee != null ? assignee.getCuid() : null;
            @SuppressWarnings("unchecked")
            final List<String> workGroups = (List<String>) changes.get(GROUPS);
            changes.remove(AUTO_ASSIGNEE);
            changes.remove(GROUPS);
            final boolean clearDueDate = changes.containsKey(DUE_DATE) && changes.get(DUE_DATE) == null? true : false;

            if (taskInst.isDetailOnly()) {
                if (GlobalApplicationStatus.getInstance().getSystemStatusMap().isEmpty()|| GlobalApplicationStatus.ONLINE.equals(GlobalApplicationStatus.getInstance()
                                .getSystemStatusMap().get(GlobalApplicationStatus.SUMMARY_TASK_APPNAME))) {
                    TaskInstanceVO taskInstance = new TaskInstanceVO();
                    taskInstance.setAssociatedTaskInstanceId(taskInst.getAssociatedTaskInstanceId());
                    taskInstance.setDueDate((Date) changes.get(DUE_DATE));
                    taskInstance.setComments((String) changes.get(COMMENTS));
                    taskInstance.setPriority((Integer) changes.get(PRIORITY));
                    taskInstance.setTaskClaimUserCuid(autoAssigneeCuid);
                    taskInstance.setGroups(workGroups);
                    @SuppressWarnings("serial")
                    TaskInstanceVO notifyTaskInstanceUpdate = new TaskInstanceVO(taskInstance.getJson()) {
                        @Override
                        public JSONObject getJson() throws JSONException {
                            JSONObject json = null;
                            json = super.getJson();
                            json.put("user", cuid);
                            json.put("clearDueDate", clearDueDate);
                            return json;
                        }
                    };

                    StatusMessage response = TaskManagerAccess.getInstance().notifySummaryTaskManager(UPDATE_TASK, notifyTaskInstanceUpdate);
                    if (!response.isSuccess())
                        throw new RemoteException(response.getMessage());
                }
                else {
                    logger.warn("The summary task manager application is not reachable. Hence unable to process the request. " + taskInst.getTaskInstanceId());
                    throw new RemoteException("The summary task manager application is not reachable. Hence unable to process the request");
                }
            }
            taskManager.updateTaskInstanceData(changes, workGroups, autoAssignee, taskInst, cuid);
        }
        catch (Exception ex) {
            throw new TaskException(ex.getMessage(), ex);
        }
    }

    public String getTaskInstanceUrl(TaskInstanceVO taskInst) throws TaskException {
        String baseUrl = ApplicationContext.getMdwHubUrl();
        if (!baseUrl.endsWith("/"))
          baseUrl += "/";
        return baseUrl + "#/tasks/" + taskInst.getTaskInstanceId();
    }
}
