/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.ProcessVisibilityConstant;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.constant.VariableConstants;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.form.CallURL;
import com.centurylink.mdw.common.utilities.form.FormAction;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.event.EventHandlerException;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.data.task.TaskAction;
import com.centurylink.mdw.model.data.task.TaskStatus;
import com.centurylink.mdw.model.data.task.TaskStatuses;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskIndexes;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskRuntimeContext;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.observer.task.RemoteNotifier;
import com.centurylink.mdw.observer.task.TaskNotifier;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskException;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.UserManager;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;
import com.centurylink.mdw.services.process.ProcessEngineDriver;
import com.centurylink.mdw.services.task.TaskManagerAccess;
import com.centurylink.mdw.services.task.factory.TaskInstanceNotifierFactory;
import com.centurylink.mdw.task.SubTask;
import com.centurylink.mdw.task.SubTaskPlanDocument.SubTaskPlan;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengNode;

/**
 * Event handler for old-style task notifications for remote/central.
 * @deprecated
 * New Services API examples are here:
 * http://cshare.ad.qintra.com/sites/MDW/Developer%20Resources/Environment/schemas/Samples
 */
@Deprecated
public class FormEventHandler extends ExternalEventHandlerBase {

    public static final String CREATE_TASK = "CreateTask";

    public String handleEventMessage(String message, Object msgdoc, Map<String, String> metaInfo)
            throws EventHandlerException {
        boolean loadedFromJson = (msgdoc instanceof JSONObject);
        FormDataDocument datadoc = new FormDataDocument();
        try {
            try {
                datadoc.load(message);
            }
            catch (MbengException e1) {
                throw new EventHandlerException(-1, "Failed to parse data document", e1);
            }
            String action = datadoc.getAttribute(FormDataDocument.ATTR_ACTION);
            if (action == null || action.length() == 0)
                throw new EventHandlerException("Action is not specified");
            // add metainfo from action string
            CallURL callurl;
            try {
                callurl = new CallURL(action);
            }
            catch (Exception e) {
                throw new EventHandlerException("Failed to parse action parameters: " + action);
            }
            metaInfo.put(FormDataDocument.META_ACTION, action);
            metaInfo.putAll(callurl.getParameters());
            metaInfo.putAll(datadoc.collectMetaData());
            // by now metaInfo contains 4 source of data, in precedence:
            // 1. meta data within form data document
            // 2. action (as "ACTION") and also its parameters
            // 3. parameters from external event handler specifications
            // 4. listener specific parameters
            // we need to clear errors and action
            datadoc.setAttribute(FormDataDocument.ATTR_ACTION, null);
            datadoc.clearErrors();

            action = callurl.getAction();
            if (action.startsWith(FormConstants.SPECIAL_ACTION_PREFIX)) {
                if (action.equals(FormConstants.ACTION_CREATE_TASK)) { // engine->taskmgr; detail->summary
                    datadoc = createTask(datadoc, message, metaInfo);
                }
                else if (action.equals(FormConstants.ACTION_START_PROCESS)) { // taskmgr->engine
                    datadoc = startProcess(datadoc, message, metaInfo);
                }
                else if (action.equals(FormConstants.ACTION_TEST)) { // taskmgr->engine
                    datadoc = handleTestAction(action, datadoc, metaInfo);
                }
                else if (action.equals(FormConstants.ACTION_GET_TASK_TEMPLATE)) { // taskmgr->engine
                    datadoc = getTaskDefinition(datadoc, metaInfo);
                }
                else if (action.equals(FormConstants.ACTION_CANCEL_TASKS)) { // engine->taskmgr
                    datadoc = cancelTasks(datadoc, metaInfo);
                }
                else if (action.equals(FormConstants.ACTION_RESPOND_TASK)) { // engine->taskmgr
                    // response for asynchronous engine call
                    datadoc = processEngineCallResponse(datadoc, metaInfo);
                }
                else if (action.equals(FormConstants.ACTION_COMPLETE_TASK)) { // taskmgr->engine; detail->summary
                    String recepient = metaInfo.get("Recepient");
                    if ("SummaryTaskManager".equals(recepient))
                        datadoc = closeTask(datadoc, metaInfo);
                    else
                        datadoc = handleTaskAction(datadoc, message, metaInfo);
                }
                else if (action.equals(FormConstants.ACTION_CANCEL_TASK)) { // taskmgr->engine
                    String recepient = metaInfo.get("Recepient");
                    if ("SummaryTaskManager".equals(recepient))
                        datadoc = closeTask(datadoc, metaInfo);
                    else
                        datadoc = handleTaskAction(datadoc, message, metaInfo);
                }
                else if (action.equals(FormConstants.ACTION_CHANGE_DUEDATE_TASK)) { // detail->summary
                    datadoc = changeTaskDueDate(datadoc, metaInfo);
                }
                else if (action.equals(FormConstants.ACTION_AUTHORIZE)) { // detail->summary taskmgr
                    datadoc = handleAuthorization(datadoc, metaInfo);
                }
                else if (action.equals(FormConstants.ACTION_ASSIGN_TASK)) { // detail->summary taskmgr
                    datadoc = handleTaskAssignment(datadoc, metaInfo);
                }
                else if (action.equals(FormConstants.ACTION_GET_TASK)) { // detail->summary taskmgr
                    datadoc = getTask(datadoc, metaInfo);
                }
                else if (action.equals(FormConstants.ACTION_TASK_NOTIFICATION)) { // remote taskmgr -> engine
                    datadoc = handleTaskNotification(datadoc, metaInfo);
                }
                else if (action.equals(FormConstants.ACTION_GET_EMAILS)) { // engine -> remote taskmgr
                    datadoc = handleGetEmails(datadoc, metaInfo);
                }
                else {
                    throw new EventHandlerException("Unknown special action " + action);
                }
            }
            else {
                datadoc = handleFormAction(action, datadoc, metaInfo);
            }
        }
        catch (EventHandlerException e) {
            logger.severeException(e.getMessage(), e);
            datadoc.addError(e.getMessage());
        }
        if (loadedFromJson) {
            try {
                return datadoc.formatJson();
            }
            catch (JSONException e) {
                logger.severeException("Failed to format JSON", e);
                return "{\"ERROR\": \"Failed to format JSON\"}";
            }
        }
        else
            return datadoc.xmlText();
    }

    protected FormDataDocument handleFormAction(String action, FormDataDocument datadoc, Map<String, String> metainfo)
            throws EventHandlerException {
        FormAction actionProc;
        try {
            if (this instanceof FormAction && getClass().getName().equals(action)) {
                actionProc = (FormAction) this;
            }
            else {
                String packageName = datadoc.getMetaValue(FormDataDocument.META_PACKAGE_NAME);
                PackageVO pkg = PackageVOCache.getPackage(packageName);
                actionProc = pkg.getFormAction(action);
            }
        }
        catch (Exception e) {
            String errmsg = "Cannot find or instantiate action class " + action;
            logger.severeException(errmsg, e);
            throw new EventHandlerException(-1, errmsg, e);
        }
        return actionProc.handleAction(datadoc, metainfo);
    }

    private FormDataDocument handleTestAction(String action, FormDataDocument datadoc, Map<String, String> metainfo)
            throws EventHandlerException {
        try {
            String casename = metainfo.get("case");
            if (casename == null) {
                datadoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_PROMPT);
                datadoc.setMetaValue(FormDataDocument.META_PROMPT, "Hello, World!");
            }
            else if (casename.equals("openwindow")) {
                datadoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_WINDOW + "?" + FormConstants.URLARG_FORMNAME + "=html:htmlDocAndSample");
                datadoc.setValue("COUNTRY", "DE", FormDataDocument.KIND_FIELD);
            }
            else if (casename.equals("opendialog")) {
                datadoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_DIALOG + "?" + FormConstants.URLARG_UNIQUE_ID + "=dialog1");
                datadoc.setValue("COUNTRY", "UK", FormDataDocument.KIND_FIELD);
            }
            else if (casename.equals("closewindow")) {
                datadoc.setMetaValue(FormDataDocument.META_PROMPT, "Closing the window!");
                datadoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_CANCEL);
            }
            else if (casename.equals("tableselect")) {
                String table = metainfo.get("table");
                String row = metainfo.get("row");
                datadoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_PROMPT);
                datadoc.setMetaValue(FormDataDocument.META_PROMPT, "Table " + table + " selects row " + row);
            }
            else if (casename.equals("editrow")) {
                String tablepath = metainfo.get("table");
                String dialog = metainfo.get("dialog");
                MbengNode tablenode = datadoc.getNode(tablepath);
                MbengNode row, selected = null;
                if (tablenode != null) {
                    for (row = tablenode.getFirstChild(); selected == null && row != null; row = row.getNextSibling()) {
                        if ("true".equalsIgnoreCase(row.getAttribute(FormDataDocument.ATTR_SELECTED))) {
                            selected = row;
                        }
                    }
                }
                if (selected == null) {
                    datadoc.addError("You need to select a row");
                }
                else {
                    datadoc.setValue("SelectedRow.R", datadoc.getValue(selected, "R"), "SUBFORM.FIELD");
                    datadoc.setValue("SelectedRow.G", datadoc.getValue(selected, "G"), "SUBFORM.FIELD");
                    datadoc.setValue("SelectedRow.B", datadoc.getValue(selected, "B"), "SUBFORM.FIELD");
                    datadoc.setAttribute(FormDataDocument.ATTR_FORM, dialog);
                    datadoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_DIALOG);
                }
            }
            else if (casename.equals("saverow")) {
                String tablepath = metainfo.get("table");
                MbengNode tablenode = datadoc.getNode(tablepath);
                MbengNode row, selected = null;
                if (tablenode != null) {
                    for (row = tablenode.getFirstChild(); selected == null && row != null; row = row.getNextSibling()) {
                        if ("true".equalsIgnoreCase(row.getAttribute(FormDataDocument.ATTR_SELECTED))) {
                            selected = row;
                        }
                    }
                }
                if (selected != null) {
                    datadoc.setValue(selected, "R", datadoc.getValue("SelectedRow.R"), FormDataDocument.KIND_ENTRY);
                    datadoc.setValue(selected, "G", datadoc.getValue("SelectedRow.G"), FormDataDocument.KIND_ENTRY);
                    datadoc.setValue(selected, "B", datadoc.getValue("SelectedRow.B"), FormDataDocument.KIND_ENTRY);
                }
                datadoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_OK);
            }
            else if (casename.equals("addrow")) {
                String tablepath = metainfo.get("table");
                String dialog = metainfo.get("dialog");
                MbengNode tablenode = datadoc.getNode(tablepath);
                MbengNode row;
                if (tablenode == null)
                    tablenode = datadoc.setValue(tablepath, null, FormDataDocument.KIND_TABLE);
                for (row = tablenode.getFirstChild(); row != null; row = row.getNextSibling()) {
                    row.setAttribute(FormDataDocument.ATTR_SELECTED, null);
                }
                row = datadoc.newNode("ROW", null, FormDataDocument.KIND_ROW, ' ');
                tablenode.appendChild(row);
                row.setAttribute(FormDataDocument.ATTR_SELECTED, "TRUE");
                datadoc.setValue("SelectedRow.R", null, "SUBFORM.FIELD");
                datadoc.setValue("SelectedRow.G", null, "SUBFORM.FIELD");
                datadoc.setValue("SelectedRow.B", null, "SUBFORM.FIELD");
                datadoc.setAttribute(FormDataDocument.ATTR_FORM, dialog);
                datadoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_DIALOG);
            }
            else if (casename.equals("deleterow")) {
                String tablepath = metainfo.get("table");
                MbengNode tablenode = datadoc.getNode(tablepath);
                MbengNode row, next = null;
                if (tablenode != null) {
                    for (row = tablenode.getFirstChild(); row != null; row = next) {
                        next = row.getNextSibling();
                        if ("true".equalsIgnoreCase(row.getAttribute(FormDataDocument.ATTR_SELECTED))) {
                            datadoc.removeNode(row);
                        }
                    }
                }
            }
            else if (casename.equals("xmlinput")) {
                String xmldata = datadoc.getValue("xmldata");
                // String id = datadoc.getAttribute(FormDataDocument.ATTR_ID);
                datadoc.load(xmldata);
                // String formname = metainfo.get("formName");
                // datadoc.setAttribute(FormDataDocument.ATTR_FORM, formname);
            }
            else if (casename.equals("logoff")) {
                datadoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_LOGOFF);
                datadoc.setAttribute(FormDataDocument.ATTR_FORM, "html:sampleLogoff");
            }
            else /* if (casename.equals("prompt")) */{
                datadoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_PROMPT);
                datadoc.setMetaValue(FormDataDocument.META_PROMPT, "Hello, Test Case " + casename + "!");
            }
            return datadoc;
        }
        catch (MbengException e) {
            throw new EventHandlerException(-1, "Test action failed", e);
        }
    }

    protected FormDataDocument startProcess(FormDataDocument datadoc, String request, Map<String, String> metainfo)
            throws EventHandlerException {
        String resp;
        boolean isServiceProcess = false;
        try {
            String processName = metainfo.get(FormDataDocument.META_PROCESS_NAME);
            String documentIdString = metainfo.get(Listener.METAINFO_DOCUMENT_ID);
            Long documentId = new Long(documentIdString);
            if (documentId.longValue() == 0L) {
                EventManager eventManager = ServiceLocator.getEventManager();
                documentId = eventManager.createDocument(FormDataDocument.class.getName(), 0L, OwnerType.LISTENER_REQUEST, 1L, null, null, request);
            }
            String masterRequestId = metainfo.get(FormDataDocument.META_MASTER_REQUEST_ID);
            if (masterRequestId == null) {
                masterRequestId = datadoc.getValue(FormDataDocument.META_MASTER_REQUEST_ID);
                if (masterRequestId == null || masterRequestId.length() == 0) {
                    masterRequestId = ApplicationContext.getApplicationName() + documentId.toString();
                }
            }

            Long processId = getProcessId(processName);
            ProcessVO procVO = getProcessDefinition(processId);
            String v = metainfo.get(FormDataDocument.META_PROCESS_INSTANCE_ID);
            Long bindingProcInstId = StringHelper.isEmpty(v) ? null : new Long(v);
            Map<String, String> params;
            Long taskInstId = datadoc.getTaskInstanceId();
            String ownerType = taskInstId == null ? OwnerType.DOCUMENT : OwnerType.TASK_INSTANCE;
            if (bindingProcInstId != null && "true".equalsIgnoreCase(metainfo.get(FormDataDocument.META_AUTOBIND)))
                params = autobindInput(procVO, bindingProcInstId);
            else
                params = new HashMap<String, String>();
            if (ownerType.equals(OwnerType.TASK_INSTANCE)) {
                params.put(VariableConstants.REQUEST, (new DocumentReference(documentId)).toString());
                // engine only binds request automatically when owner is DOCUMENT
            }
            isServiceProcess = procVO.getProcessType().equals(ProcessVisibilityConstant.SERVICE);
            if (isServiceProcess && "async".equalsIgnoreCase(metainfo.get(FormConstants.URLARG_TIMEOUT)))
                isServiceProcess = false;

            Long ownerId = taskInstId == null ? documentId : taskInstId;
            ProcessEngineDriver engineDriver = new ProcessEngineDriver();
            if (isServiceProcess) {
                resp = engineDriver.invokeService(processId, ownerType, ownerId, masterRequestId, request, params,
                        VariableConstants.RESPONSE, procVO.getPerformanceLevel(),
                        bindingProcInstId != null ? OwnerType.PROCESS_INSTANCE : null, bindingProcInstId, null);
                datadoc.load(resp);
                if (bindingProcInstId != null) {
                    autobindOutput(procVO, bindingProcInstId, engineDriver.getMainProcessInstanceId());
                }
            }
            else {
                Long procInstId = engineDriver.startProcess(processId, masterRequestId, ownerType, ownerId, params,
                        bindingProcInstId != null ? OwnerType.PROCESS_INSTANCE : null, bindingProcInstId, null);
                if (bindingProcInstId == null)
                    datadoc.setMetaValue(FormDataDocument.META_PROCESS_INSTANCE_ID, procInstId.toString());
                // else cannot set this, which modifies the task instance's owner process instance ID
                datadoc.setMetaValue(FormDataDocument.META_PROMPT, "Process Launched - Master Request ID " + masterRequestId);
            }
            datadoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_PROMPT);
            datadoc.setMetaValue(FormDataDocument.META_TASK_STATUS, "Completed");
            datadoc.setMetaValue(FormDataDocument.META_MASTER_REQUEST_ID, masterRequestId);
        }
        catch (Exception e) {
            logger.severeException(e.getMessage(), e);
            if (isServiceProcess) {
                datadoc.addError("Failed to execute service process");
                datadoc.addError(e.getClass().getName() + ": " + e.getMessage());
            }
            else {
                datadoc.addError("Failed to start process - " + e.getMessage());
            }
        }
        return datadoc;
    }

    private FormDataDocument getTaskDefinition(FormDataDocument formdatadoc, Map<String, String> metainfo) {
        try {
            String taskLogicalId = formdatadoc.getMetaValue(FormDataDocument.META_TASK_LOGICAL_ID);
            TaskVO task = TaskTemplateCache.getTaskTemplate(null, taskLogicalId);
            if (task == null) {
                formdatadoc.setMetaValue(FormDataDocument.META_STATUS, "1");
                formdatadoc.addError("Task Template '" + taskLogicalId + "' does not exist");
            }
            else {
                formdatadoc.setValue(TaskAttributeConstant.INDICES, task.getAttribute(TaskAttributeConstant.INDICES));
                formdatadoc.setValue(TaskAttributeConstant.NOTICES, task.getAttribute(TaskAttributeConstant.NOTICES));
                formdatadoc.setValue(TaskAttributeConstant.NOTICE_GROUPS, task.getAttribute(TaskAttributeConstant.NOTICE_GROUPS));
                formdatadoc.setValue(TaskAttributeConstant.RECIPIENT_EMAILS, task.getAttribute(TaskAttributeConstant.RECIPIENT_EMAILS));
                formdatadoc.setValue(TaskAttributeConstant.CC_GROUPS, task.getAttribute(TaskAttributeConstant.CC_GROUPS));
                formdatadoc.setValue(TaskAttributeConstant.CC_EMAILS, task.getAttribute(TaskAttributeConstant.CC_EMAILS));
                formdatadoc.setValue(TaskAttributeConstant.ALERT_INTERVAL, task.getAttribute(TaskAttributeConstant.ALERT_INTERVAL));
                formdatadoc.setValue(TaskAttributeConstant.AUTO_ASSIGN, task.getAttribute(TaskAttributeConstant.AUTO_ASSIGN));
                formdatadoc.setValue(TaskAttributeConstant.TASK_SLA, task.getAttribute(TaskAttributeConstant.TASK_SLA));
                formdatadoc.setValue(TaskAttributeConstant.FORM_NAME, task.getFormName());
                formdatadoc.setValue(TaskAttributeConstant.DESCRIPTION, task.getComment());
                formdatadoc.setValue(TaskAttributeConstant.GROUPS, task.getUserGroupsAsString());
                formdatadoc.setValue(TaskAttributeConstant.VARIABLES, task.getAttribute(TaskAttributeConstant.VARIABLES));
                formdatadoc.setValue(TaskAttributeConstant.SERVICE_PROCESSES, task.getAttribute(TaskAttributeConstant.SERVICE_PROCESSES));
                formdatadoc.setValue(TaskActivity.ATTRIBUTE_TASK_NAME, task.getTaskName());
                formdatadoc.setValue(TaskActivity.ATTRIBUTE_TASK_CATEGORY, task.getTaskCategory());
                formdatadoc.setMetaValue(FormDataDocument.META_STATUS, "0");
                // include index in template???
            }
        }
        catch (Exception e) {
            logger.severeException(e.getMessage(), e);
            formdatadoc = FormDataDocument.createSimpleResponse(1, e.getMessage() == null ? e.getClass().getName() : e.getMessage(), null);
        }
        return formdatadoc;
    }

    protected FormDataDocument createTask(FormDataDocument formdatadoc, String content, Map<String, String> metainfo) {
        String taskLogicalId = metainfo.get(FormDataDocument.META_TASK_LOGICAL_ID);
        String sourceApp = formdatadoc.getAttribute(FormDataDocument.ATTR_APPNAME);
        String processInstanceId = metainfo.get(FormDataDocument.META_PROCESS_INSTANCE_ID);
        String dueInSeconds = metainfo.get(FormDataDocument.META_DUE_IN_SECONDS);
        String transInstId = metainfo.get(FormDataDocument.META_TASK_TRANS_INST_ID); // for classic task only
        String errmsg = metainfo.get(FormDataDocument.META_TASK_ERRMSG); // for classic task only
        String taskName = metainfo.get(FormDataDocument.META_TASK_NAME); // if not set, get from task template
        String assignee = metainfo.get(FormDataDocument.META_TASK_ASSIGNEE); // auto-assign variable value
        Long engineTaskInstanceId = null;
        boolean isDetailOnly = false;
        try {
            TaskManager taskManager = ServiceLocator.getTaskManager();
            TaskVO taskVo = TaskTemplateCache.getTaskTemplate(sourceApp, taskLogicalId);
            if (taskVo == null)
                throw new EventHandlerException("Task template '" + (sourceApp == null ? taskLogicalId : sourceApp + ":" + taskLogicalId) + "' is not defined");
            if (TaskInstanceVO.DETAILONLY.equals(sourceApp)) {
                isDetailOnly = true;
            }
            else if (sourceApp != null) { // is summary or summary+detail
                String av = metainfo.get(FormDataDocument.META_TASK_INSTANCE_ID);
                engineTaskInstanceId = (av != null && av.length() > 0) ? new Long(av) : null;
            }

            String documentIdString = metainfo.get(Listener.METAINFO_DOCUMENT_ID);
            Long documentId = new Long(documentIdString);
            if (documentId.longValue() == 0L) {
                EventManager eventManager = ServiceLocator.getEventManager();
                documentId = eventManager.createDocument(FormDataDocument.class.getName(), 0L, OwnerType.LISTENER_REQUEST, 1L, null, null, content);
            }
            Long activityInstanceId = formdatadoc.getActivityInstanceId();
            String secondaryOwnerType = transInstId != null ? OwnerType.WORK_TRANSITION_INSTANCE : OwnerType.DOCUMENT;
            Long secondaryOwnerId = transInstId != null ? new Long(transInstId) : documentId;
            TaskInstanceVO instance;
            String masterRequestId = formdatadoc.getMetaValue(FormDataDocument.META_MASTER_REQUEST_ID);
            if (isDetailOnly) {
                instance = taskManager.createTaskInstance(taskVo.getTaskId(), new Long(processInstanceId), secondaryOwnerType,
                        secondaryOwnerId, errmsg, sourceApp, engineTaskInstanceId, taskName, dueInSeconds == null ? 0
                                : Integer.parseInt(dueInSeconds), null, assignee, masterRequestId);
                formdatadoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_CREATE_TASK);
                formdatadoc.setMetaValue(FormDataDocument.META_TASK_INSTANCE_ID, instance.getTaskInstanceId().toString());
                // priority
                if (instance.getPriority() != null)
                    formdatadoc.setMetaValue(FormDataDocument.META_TASK_PRIORITY, Integer.toString(instance.getPriority()));
                // due date
                if (instance.getDueDate() != null)
                    formdatadoc.setMetaValue(FormDataDocument.META_DUE_IN_SECONDS, Long.toString((instance.getDueDate().getTime() - new Date().getTime()) / 1000));
                // auto-route
                if (instance.getWorkgroups() != null && !instance.getWorkgroups().isEmpty())
                    formdatadoc.setMetaValue(FormDataDocument.META_TASK_GROUPS, instance.getWorkgroupsString());
                // auto-assign
                if (instance.getTaskClaimUserCuid() != null)
                    formdatadoc.setMetaValue(FormDataDocument.META_TASK_ASSIGNEE, instance.getTaskClaimUserCuid());
                String response = TaskManagerAccess.getInstance().notifySummaryTaskManager(formdatadoc);
                String summaryTaskInstId = TaskManagerAccess.getInstance().parseTaskInstanceId(response);
                taskManager.updateAssociatedTaskInstance(instance.getTaskInstanceId(), TaskInstanceVO.DETAILONLY, new Long(summaryTaskInstId));
                logger.info("Task summary instance created - ID " + summaryTaskInstId);
                instance.setAssociatedTaskInstanceId(new Long(summaryTaskInstId));
                Map<String,String> indices = taskManager.collectIndices(taskVo.getTaskId(), new Long(processInstanceId), formdatadoc);
                if (!indices.isEmpty()) {
                    TaskIndexes taskIndexes = new TaskIndexes(instance.getAssociatedTaskInstanceId(), indices);
                    try {
                        TaskManagerAccess.getInstance().notifySummaryTaskManager("UpdateTaskIndexes", taskIndexes);
                    } catch (Exception e) {
                        logger.severeException("Failed to create Remote task indices", e);
                    }
                }
                // invoke registered remoteTaskNotifiers
                TaskRuntimeContext taskRuntime = null;
                List<TaskNotifier> taskNotifiers =  TaskInstanceNotifierFactory.getInstance().getNotifiers(taskVo.getTaskId(),
                        OwnerType.PROCESS_INSTANCE.equals(instance.getOwnerType()) ? new Long(processInstanceId) : null, TaskStatus.STATUSNAME_OPEN);

                for (TaskNotifier notifier : taskNotifiers) {
                    if (notifier instanceof RemoteNotifier) {
                        if (taskRuntime == null)
                            taskRuntime = taskManager.getTaskRuntimeContext(instance);
                        notifier.sendNotice(taskRuntime, TaskAction.CREATE, TaskStatus.STATUSNAME_OPEN);
                    }
                }
            }
            else {
                // the following call to task manager performs the following:
                // - create task instance entry in database
                // - create SLA if passed in or specified in template
                // - create groups for new TEMPLATE based tasks
                // - create indices for new TEMPLATE based general tasks
                // - send notification if specified in template
                // - invoke old-style observer if specified in template
                // - auto-assign if specified in template
                // - record in audit log
                if (assignee == null)
                    assignee = formdatadoc.getMetaValue(FormDataDocument.META_TASK_ASSIGNEE);
                if (dueInSeconds == null)
                    dueInSeconds = formdatadoc.getMetaValue(FormDataDocument.META_DUE_IN_SECONDS);

                Map<String, String> indices = taskManager.collectIndices(taskVo.getTaskId(), new Long(processInstanceId), formdatadoc);
                instance = taskManager.createTaskInstance(taskVo.getTaskId(), new Long(processInstanceId), secondaryOwnerType,
                        secondaryOwnerId, errmsg, sourceApp, engineTaskInstanceId, taskName, dueInSeconds == null ? 0
                                : Integer.parseInt(dueInSeconds), indices, assignee, masterRequestId);
                formdatadoc.setMetaValue(FormDataDocument.META_TASK_INSTANCE_ID, instance.getTaskInstanceId().toString());

                String priority = formdatadoc.getMetaValue(FormDataDocument.META_TASK_PRIORITY);
                if (priority != null) {
                    int detailPriority = Integer.parseInt(priority);
                    int summaryPriority = instance.getPriority();
                    if (detailPriority != summaryPriority) {
                        instance.setPriority(detailPriority);
                        taskManager.updateTaskInstancePriority(instance.getTaskInstanceId(), new Integer(detailPriority));
                    }
                }

                String workgroups = formdatadoc.getMetaValue(FormDataDocument.META_TASK_GROUPS);
                if (workgroups != null) {
                    // routing took place in detail tm
                    String summaryGroups = instance.getWorkgroupsString();
                    if (!workgroups.equals(summaryGroups)) {
                        if (summaryGroups == null || summaryGroups.trim().isEmpty()) {
                            instance.setWorkgroupsFromString(workgroups);
                            taskManager.updateTaskInstanceWorkgroups(instance.getTaskInstanceId(), instance.getWorkgroups());
                        }
                        else {
                            // we'll not override groups configured in summary template -- just warn
                            String msg = "Warning: Task Instance " + instance.getTaskInstanceId()
                                    + " has workgroup routing from detail (" + workgroups
                                    + ") that is being overridden by template group assignments in summary ("
                                    + summaryGroups + ")";
                            logger.warnException(msg, new Exception(msg));
                        }
                    }
                }
            }
            // call createSubTask only in detail not in summary
            if (isDetailOnly || sourceApp == null) {
                List<SubTask> subTaskList = getSubTaskList(taskVo.getTaskId(), instance, null);
                if (subTaskList != null && !subTaskList.isEmpty()) {
                    createSubTask(subTaskList, instance, new Long(processInstanceId), isDetailOnly);
                }
            }
            String correlationId = FormConstants.TASK_CORRELATION_ID_PREFIX + instance.getTaskInstanceId().toString();
            formdatadoc.setAttribute(FormDataDocument.ATTR_ID, correlationId);
            DocumentReference docref = new DocumentReference(documentId);
            super.updateDocumentContent(docref, formdatadoc.format(), FormDataDocument.class.toString());
            String message = TaskActivity.TASK_CREATE_RESPONSE_ID_PREFIX + instance.getTaskInstanceId()
                    + ", act inst id=" + activityInstanceId + ", document id=" + documentId.toString();
            if (logger.isInfoEnabled())
                logger.info(message);
            formdatadoc = FormDataDocument.createSimpleResponse(0, message, instance.getTaskInstanceId());
        }
        catch (Exception e) {
            logger.severeException("Failed to create task instance", e);
            formdatadoc = FormDataDocument.createSimpleResponse(1,
                    "Failed to create task instance - " + e.getMessage(), null);
        }
        return formdatadoc;
        // for mdw 5, retire TASK table. Get task name from activity
    }

    private List<SubTask> getSubTaskList(Long taskId, TaskInstanceVO instance, Map<String, String> indices)
            throws TaskException, DataAccessException {
        TaskVO task = TaskTemplateCache.getTaskTemplate(taskId);
        TaskManager taskManager = ServiceLocator.getTaskManager();
        if (task.isTemplate()) {
            SubTaskPlan subTaskPlan = taskManager.getSubTaskPlan(taskManager.getTaskRuntimeContext(instance));
            if (subTaskPlan != null) {
                return subTaskPlan.getSubTaskList();
            }
        }
        return null;
    }

    /*
     * To create Sub tasks
     */
    private void createSubTask(List<SubTask> subTaskList, final TaskInstanceVO instance, final Long processInstanceId, boolean isDetailOnly)
            throws TaskException, DataAccessException {
        TaskManager taskManager = ServiceLocator.getTaskManager();
        for (SubTask subTask : subTaskList) {
            final TaskVO subTaskObj = TaskTemplateCache.getTaskTemplate(null, subTask.getLogicalId());
            if (subTaskObj == null)
                throw new TaskException("Task Template '" + subTask.getLogicalId() + "' does not exist");

            final TaskInstanceVO subTaskInstance = taskManager.createTaskInstance(subTaskObj.getTaskId(),
                    instance.getMasterRequestId(), processInstanceId, OwnerType.TASK_INSTANCE,
                    instance.getTaskInstanceId(), null);
            logger.info("Task summary instance created - ID " + subTaskInstance.getTaskInstanceId());
            if (isDetailOnly) {
                TaskInstanceVO taskInst = new TaskInstanceVO();
                taskInst.setMasterRequestId(subTaskInstance.getMasterRequestId());
                taskInst.setSecondaryOwnerType(OwnerType.TASK_INSTANCE);
                taskInst.setSecondaryOwnerId(instance.getAssociatedTaskInstanceId());
                taskInst.setTaskClaimUserCuid(subTaskInstance.getTaskClaimUserCuid());
                taskInst.setAssociatedTaskInstanceId(subTaskInstance.getTaskInstanceId());
                taskInst.setPriority(subTaskInstance.getPriority());
                taskInst.setDueDate(new Date(DatabaseAccess.getCurrentTime() + subTaskObj.getSlaSeconds() * 1000));
                taskInst.setOwnerId(processInstanceId);
                taskInst.setGroups(subTaskInstance.getWorkgroups());
                try {
                    @SuppressWarnings("serial")
                    TaskInstanceVO notifyTaskDetails = new TaskInstanceVO(taskInst.getJson()) {
                        @Override
                        public JSONObject getJson() throws JSONException {
                            JSONObject json = null;
                            json = super.getJson();
                            json.put(TaskAttributeConstant.LOGICAL_ID, subTaskObj.getLogicalId());
                            json.put(TaskAttributeConstant.PROCESS_INST_ID, processInstanceId);
                            return json;
                        }
                    };

                    StatusMessage response = TaskManagerAccess.getInstance().notifySummaryTaskManager(CREATE_TASK, notifyTaskDetails);
                    if (response.isSuccess()) {
                        Long summaryTaskInstId = new Long(response.getStatus().getStatusMessage());
                        taskManager.updateAssociatedTaskInstance(subTaskInstance.getTaskInstanceId(), TaskInstanceVO.DETAILONLY, summaryTaskInstId);
                        subTaskInstance.setAssociatedTaskInstanceId(summaryTaskInstId);
                    }
                    TaskRuntimeContext taskRuntime = null;
                    List<TaskNotifier> taskNotifiers = TaskInstanceNotifierFactory.getInstance().getNotifiers(subTaskObj.getTaskId(),
                            OwnerType.PROCESS_INSTANCE.equals(instance.getOwnerType()) ? processInstanceId : null, TaskStatus.STATUSNAME_OPEN);
                    for (TaskNotifier notifier : taskNotifiers) {
                        if (notifier instanceof RemoteNotifier) {
                            if (taskRuntime == null)
                                taskRuntime = taskManager.getTaskRuntimeContext(subTaskInstance);
                            notifier.sendNotice(taskRuntime, TaskAction.CREATE, TaskStatus.STATUSNAME_OPEN);
                        }
                    }
                } catch (Exception e) {
                    logger.severeException("Failed to create sub task instance", e);
                    throw new TaskException(e.getMessage());
                }
            }
            List<SubTask> subTasksList = getSubTaskList(subTaskObj.getTaskId(), subTaskInstance, null);
            if (subTasksList != null && !subTasksList.isEmpty()) {
                createSubTask(subTasksList, subTaskInstance, processInstanceId, isDetailOnly);
            }
        }
    }

    protected FormDataDocument handleTaskAction(FormDataDocument datadoc, String content, Map<String, String> metainfo) {
        String taskAction = metainfo.get(FormDataDocument.META_ACTION);
        Long taskInstanceId = datadoc.getTaskInstanceId();
        String documentIdString = metainfo.get(Listener.METAINFO_DOCUMENT_ID);
        Long documentId = new Long(documentIdString);
        String eventName = datadoc.getAttribute(FormDataDocument.ATTR_ID);
        String message = "Task action " + taskAction + " received for task instance " + taskInstanceId + ", event name = " + eventName + ", document ID = " + documentId;
        logger.info(message);
        super.notifyProcesses(eventName, documentId, content, 0);
        return FormDataDocument.createSimpleResponse(0, message, taskInstanceId);
    }

    protected FormDataDocument cancelTasks(FormDataDocument formdatadoc, Map<String, String> metainfo) {
        String activityInstanceId = metainfo.get(FormDataDocument.META_ACTIVITY_INSTANCE_ID);
        String processInstanceId = metainfo.get(FormDataDocument.META_PROCESS_INSTANCE_ID);
        MbengNode procInstTable = formdatadoc.getNode("ProcessInstances");
        String ownerApplName = formdatadoc.getAttribute(FormDataDocument.ATTR_APPNAME);
        try {
            TaskManager taskMgr = ServiceLocator.getTaskManager();
            String message;
            boolean isDetailOnly = TaskInstanceVO.DETAILONLY.equals(formdatadoc.getAttribute(FormDataDocument.ATTR_APPNAME));
            if (activityInstanceId != null) {
                // cancel task instances of the activity instance - detail task manager
                Long actInstId = new Long(activityInstanceId);
                TaskInstanceVO taskInstance = taskMgr.getTaskInstanceByActivityInstanceId(actInstId, processInstanceId == null ? null : new Long(processInstanceId));
                if (taskInstance == null)
                    throw new TaskException("Cannot find the task instance for the activity instance");
                if (taskInstance.getStatusCode().equals(TaskStatus.STATUS_ASSIGNED)
                        || taskInstance.getStatusCode().equals(TaskStatus.STATUS_IN_PROGRESS)
                        || taskInstance.getStatusCode().equals(TaskStatus.STATUS_OPEN)) {
                    taskMgr.cancelTaskInstance(taskInstance);
                }
                if (isDetailOnly) {
                    // cancel directly task instance
                    formdatadoc.setMetaValue(FormDataDocument.META_ACTIVITY_INSTANCE_ID, null);
                    formdatadoc.setMetaValue(FormDataDocument.META_TASK_INSTANCE_ID, taskInstance.getAssociatedTaskInstanceId().toString());
                    formdatadoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_CANCEL_TASKS);
                    TaskManagerAccess.getInstance().notifySummaryTaskManager(formdatadoc);
                }
                message = "Task instances cancelled for activity instance " + activityInstanceId;
            }
            else if (procInstTable != null) {
                // cancel task instances of all listed process instances - detail task manager
                for (MbengNode one = procInstTable.getFirstChild(); one != null; one = one.getNextSibling()) {
                    taskMgr.cancelTaskInstancesForProcessInstance(new Long(one.getValue()), ownerApplName);
                }
                if (isDetailOnly) {
                    formdatadoc.setAttribute(FormDataDocument.ATTR_ACTION, FormConstants.ACTION_CANCEL_TASKS);
                    TaskManagerAccess.getInstance().notifySummaryTaskManager(formdatadoc);
                }
                message = "Task instances cancelled for process instance " + processInstanceId;
            }
            else { // cancel a task instance - summary task manager
                Long taskInstanceId = new Long(metainfo.get(FormDataDocument.META_TASK_INSTANCE_ID));
                TaskInstanceVO taskInstance = taskMgr.getTaskInstance(taskInstanceId);
                taskMgr.cancelTaskInstance(taskInstance);
                message = "Task instance cancelled in summary task manager: " + taskInstanceId;
            }
            if (logger.isInfoEnabled())
                logger.info(message);
            formdatadoc = FormDataDocument.createSimpleResponse(0, message, null);
        }
        catch (Exception e) {
            String errmsg;
            if (activityInstanceId != null)
                errmsg = "Failed to cancel task instances for activity instance " + activityInstanceId;
            else
                errmsg = "Failed to cancel task instances for process instance " + processInstanceId;
            logger.severeException(errmsg, e);
            formdatadoc = FormDataDocument.createSimpleResponse(1, errmsg, null);
        }
        return formdatadoc;
    }

    private FormDataDocument processEngineCallResponse(FormDataDocument formdatadoc, Map<String, String> metainfo) {
        Long taskInstId = formdatadoc.getTaskInstanceId();
        try {
            TaskManager taskMgr = ServiceLocator.getTaskManager();
            TaskInstanceVO taskInst = taskMgr.getTaskInstance(taskInstId);
            EventManager eventManager = ServiceLocator.getEventManager();
            Long documentId = taskInst.getSecondaryOwnerId();
            DocumentVO olddoc = eventManager.getDocumentVO(documentId);
            FormDataDocument oldDatadoc = new FormDataDocument();
            oldDatadoc.load(olddoc.getContent());
            String status = oldDatadoc.getAttribute(FormDataDocument.ATTR_ENGINE_CALL_STATUS);
            if (!"WAITING".equals(status))
                throw new Exception("Task is no longer waiting for resposne");
            eventManager.updateDocumentContent(documentId, formdatadoc.format(), FormDataDocument.class.getName());
            logger.info("Engine async call response received for task instance " + taskInstId.toString()
                    + ", data in document table (id=" + taskInst.getSecondaryOwnerId().toString() + ")");
            return FormDataDocument.createSimpleResponse(0, null, taskInstId);
        }
        catch (Exception e) {
            logger.severeException("Failed to record async response of engine call", e);
            return FormDataDocument.createSimpleResponse(1, "Failed to record async response of engine call", taskInstId);
        }
    }

    protected FormDataDocument handleAuthorization(FormDataDocument datadoc, Map<String, String> metainfo) {
        try {
            String cuid = metainfo.get(FormDataDocument.META_USER);
            String taskInstId = metainfo.get(FormDataDocument.META_TASK_INSTANCE_ID);
            UserManager userMgr = ServiceLocator.getUserManager();
            if (taskInstId != null) { // get list of users assignable to the task
                TaskManager taskMgr = ServiceLocator.getTaskManager();
                TaskInstanceVO taskInst = taskMgr.getTaskInstance(new Long(taskInstId));
                List<String> groups = taskMgr.getGroupsForTaskInstance(taskInst);
                UserVO[] users = userMgr.getUsersForGroups(groups.toArray(new String[groups.size()]));
                MbengNode userTable = datadoc.setTable(null, "users", true);
                // TODO check task action roles within the group
                for (UserVO user : users) {
                    MbengNode row = datadoc.addRow(userTable);
                    datadoc.setCell(row, "id", user.getId().toString());
                    datadoc.setCell(row, "cuid", user.getCuid());
                }
            }
            else { // get authorization info for the user
                AuthenticatedUser user = userMgr.loadUser(cuid);
                if (user == null) {
                    datadoc.addError("User does not exist");
                }
                else {
                    datadoc.setValue("userid", user.getId().toString());
                    MbengNode groups = datadoc.setTable(null, "groups", true);
                    for (String p : user.getGroupNameAndRoles()) {
                        datadoc.addEntry(groups, p);
                    }
                    // MbengNode roles = datadoc.setTable(null, "roles", true);
                    // for (UserRole r : user.getApplicableRoles()) {
                    // datadoc.addEntry(roles, r.getRoleName());
                    // }
                }
            }
        }
        catch (Exception e) {
            logger.severeException(e.getMessage(), e);
            datadoc.addError(e.getMessage() == null ? e.getClass().getName() : e.getMessage());
        }
        return datadoc;
    }

    private boolean userCanWorkOn(UserVO user, TaskInstanceVO taskInst) {
        for (String grp : taskInst.getGroups()) {
            for (UserGroupVO g : user.getWorkgroups()) {
                if (grp.equals(g.getName()))
                    return true;
            }
        }
        return false;
    }

    protected FormDataDocument handleTaskAssignment(FormDataDocument datadoc, Map<String, String> metainfo) {
        Long taskInstId = new Long(datadoc.getMetaValue(FormDataDocument.META_TASK_INSTANCE_ID));
        try {
            TaskManager taskManager = ServiceLocator.getTaskManager();
            String comment = datadoc.getValue("Comment");
            String taskAction = datadoc.getValue("TaskAction");
            TaskInstanceVO taskInst = taskManager.getTaskInstance(taskInstId);
            taskManager.getTaskInstanceAdditionalInfo(taskInst);
            String destination;
            Long assigneeId;
            UserVO currentUser = UserGroupCache.getUser(datadoc.getMetaValue(FormDataDocument.META_USER));
            if (taskAction.equalsIgnoreCase(TaskAction.ASSIGN)) {
                String assignee = datadoc.getValue("AssigneeCuid");
                if (assignee == null || assignee.length() == 0)
                    throw new TaskException("No assignee is specified");
                UserVO user = UserGroupCache.getUser(assignee);
                if (user == null)
                    throw new TaskException(assignee + " is not a valid user");
                if (!userCanWorkOn(user, taskInst))
                    throw new TaskException("The user is not authorized to work on this task");
                assigneeId = user.getId();
                destination = null;
            }
            else if (taskAction.equalsIgnoreCase(TaskAction.CLAIM)) {
                if (!userCanWorkOn(currentUser, taskInst)) {
                    throw new TaskException("You are not authorized to work on this task");
                }
                assigneeId = currentUser.getId();
                destination = null;
            }
            else if (taskAction.equalsIgnoreCase(TaskAction.RELEASE)) {
                assigneeId = null;
                destination = null;
            }
            else if (taskAction.equalsIgnoreCase(TaskAction.WORK)) {
                assigneeId = null;
                destination = null;
            }
            else if (taskAction.equalsIgnoreCase(TaskAction.FORWARD)) {
                assigneeId = null;
                destination = datadoc.getValue("GroupName");
            }
            else
                throw new TaskException("Unsupported task assignment action " + taskAction);
            taskManager.performActionOnTaskInstance(taskAction, taskInst.getTaskInstanceId(), currentUser.getId(),
                    assigneeId, comment, destination, false);
            datadoc = FormDataDocument.createSimpleResponse(0, null, taskInstId);
        }
        catch (Exception e) {
            logger.severeException(e.getMessage(), e);
            datadoc = FormDataDocument.createSimpleResponse(1, (e.getMessage() == null ? e.getClass().getName() : e.getMessage()), taskInstId);
        }
        return datadoc;
    }

    private FormDataDocument getTask(FormDataDocument datadoc, Map<String, String> metainfo) {
        Long taskInstId = new Long(datadoc.getMetaValue(FormDataDocument.META_TASK_INSTANCE_ID));
        try {
            TaskManager taskManager = ServiceLocator.getTaskManager();
            TaskInstanceVO taskInst = taskManager.getTaskInstance(taskInstId);
            taskManager.getTaskInstanceAdditionalInfo(taskInst);
            String v = taskInst.getStartDate();
            if (v != null && v.length() > 10)
                v = v.substring(0, 10);
            else
                v = "";
            datadoc.setMetaValue(FormDataDocument.META_TASK_START_DATE, v);
            v = taskInst.getEndDate();
            if (v != null && v.length() > 10)
                v = v.substring(0, 10);
            else
                v = "";
            datadoc.setMetaValue(FormDataDocument.META_TASK_END_DATE, v);
            datadoc.setMetaValue(FormDataDocument.META_TASK_DUE_DATE, StringHelper.dateToString(taskInst.getDueDate()));
            v = TaskStatuses.getTaskStatuses().get(taskInst.getStatusCode());
            datadoc.setMetaValue(FormDataDocument.META_TASK_STATUS, v);
            datadoc.setMetaValue(FormDataDocument.META_TASK_ASSIGNEE, taskInst.getTaskClaimUserCuid());
            datadoc.setMetaValue(FormDataDocument.META_TASK_NAME, taskInst.getTaskName());
            String taskinstid = taskInst.getTaskInstanceId().toString();
            datadoc.setMetaValue(FormDataDocument.META_TASK_INSTANCE_ID, taskinstid);
            datadoc.setMetaValue(FormDataDocument.META_TASK_OWNER_APPL, taskInst.getOwnerApplicationName());
            datadoc.setMetaValue(FormDataDocument.META_MASTER_REQUEST_ID, taskInst.getOrderId());
            if (taskInst.getComments() != null)
                datadoc.setMetaValue(FormDataDocument.META_TASK_COMMENT, taskInst.getComments());
            datadoc.setMetaValue(FormDataDocument.META_STATUS, "0");
        }
        catch (Exception e) {
            logger.severeException(e.getMessage(), e);
            datadoc = FormDataDocument.createSimpleResponse(1, e.getMessage() != null ? e.getMessage() : e.getClass().getName(), taskInstId);
        }
        return datadoc;
    }

    private FormDataDocument closeTask(FormDataDocument formdatadoc, Map<String, String> metainfo) {
        Long taskInstanceId = new Long(metainfo.get(FormDataDocument.META_TASK_INSTANCE_ID));
        String cuid = formdatadoc.getMetaValue(FormDataDocument.META_USER);
        String taskAction = formdatadoc.getValue("TaskAction");
        String comment = formdatadoc.getValue("Comment");
        try {
            TaskManager taskManager = ServiceLocator.getTaskManager();
            UserVO user = UserGroupCache.getUser(cuid);
            taskManager.performActionOnTaskInstance(taskAction, taskInstanceId, user.getId(), null, comment, null, false);
            formdatadoc = FormDataDocument.createSimpleResponse(0, null, taskInstanceId);
        }
        catch (Exception e) {
            logger.severeException(e.getMessage(), e);
            formdatadoc = FormDataDocument.createSimpleResponse(1, (e.getMessage() == null ? e.getClass().getName() : e.getMessage()), taskInstanceId);

        }
        return formdatadoc;
    }

    private FormDataDocument handleTaskNotification(FormDataDocument formdatadoc, Map<String, String> metainfo) {
        Long taskInstanceId = new Long(metainfo.get(FormDataDocument.META_TASK_INSTANCE_ID));
        String outcome = formdatadoc.getValue("Outcome");
        MbengNode notifierTable = formdatadoc.getNode("Notifiers");
        TaskInstanceVO taskInst = new TaskInstanceVO();
        taskInst.setTaskInstanceId(taskInstanceId);
        taskInst.setTaskName(formdatadoc.getMetaValue(FormDataDocument.META_TASK_NAME));
        taskInst.setTaskClaimUserCuid(formdatadoc.getMetaValue(FormDataDocument.META_TASK_ASSIGNEE));
        taskInst.setDueDate(StringHelper.stringToDate(formdatadoc.getMetaValue(FormDataDocument.META_TASK_DUE_DATE)));
        taskInst.setOwnerId(new Long(formdatadoc.getMetaValue(FormDataDocument.META_PROCESS_INSTANCE_ID)));
        taskInst.setOwnerType(OwnerType.PROCESS_INSTANCE);
        taskInst.setTaskInstanceUrl(formdatadoc.getValue("TaskInstanceUrl"));
        taskInst.setOrderId(formdatadoc.getMetaValue(FormDataDocument.META_MASTER_REQUEST_ID));
        taskInst.setSecondaryOwnerType(OwnerType.INTERNAL_EVENT); // indicating
        // shadow task instance for notification only
        taskInst.setActivityMessage(formdatadoc.format());
        String logicalId = formdatadoc.getMetaValue(FormDataDocument.META_TASK_LOGICAL_ID);
        int k = logicalId.indexOf(":"); // remove application name prefix added
        // by summary task manager
        if (k > 0)
            logicalId = logicalId.substring(k + 1);
        taskInst.setTaskId(TaskTemplateCache.getTaskTemplate(null, logicalId).getTaskId());
        MbengNode groupTable = formdatadoc.getNode("Groups");
        taskInst.setGroups(new ArrayList<String>());
        for (MbengNode groupNode = groupTable.getFirstChild(); groupNode != null; groupNode = groupNode.getNextSibling()) {
            taskInst.getGroups().add(groupNode.getValue());
        }

        TaskRuntimeContext taskRuntime = null;
        for (MbengNode notifierNode = notifierTable.getFirstChild(); notifierNode != null; notifierNode = notifierNode.getNextSibling()) {
            try {
                TaskNotifier notifier = TaskInstanceNotifierFactory.getInstance().getNotifier(notifierNode.getValue(), null);
                if (notifier != null && !(notifier instanceof RemoteNotifier)) {
                    if (taskRuntime == null)
                        taskRuntime = ServiceLocator.getTaskManager().getTaskRuntimeContext(taskInst);
                    notifier.sendNotice(taskRuntime, null, outcome);
                }
            }
            catch (Exception e) {
                logger.severeException("Failed to send notification for task instance " + taskInstanceId, e);
            }
        }
        return FormDataDocument.createSimpleResponse(0, null, taskInstanceId);
    }

    private FormDataDocument handleGetEmails(FormDataDocument formdatadoc, Map<String, String> metainfo) {
        try {
            String groups = formdatadoc.getValue("Groups");
            UserManager userManager = ServiceLocator.getUserManager();
            List<String> addressList = userManager.getEmailAddressesForGroups(groups.split("#"));
            MbengNode emailTable = formdatadoc.setTable("Emails");
            for (String addr : addressList) {
                formdatadoc.addEntry(emailTable, addr);
            }
        }
        catch (Exception e) {
            logger.severeException(e.getMessage(), e);
            String error = (e.getMessage() == null ? e.getClass().getName() : e.getMessage());
            formdatadoc.addError(error);
        }
        return formdatadoc;
    }

    private FormDataDocument changeTaskDueDate(FormDataDocument datadoc, Map<String, String> metaInfo) {
        Long taskInstanceId = new Long(metaInfo.get(FormDataDocument.META_TASK_INSTANCE_ID));
        try {
            TaskManager taskManager = ServiceLocator.getTaskManager();
            Date dueDate = StringHelper.stringToDate(datadoc.getMetaValue(FormDataDocument.META_TASK_DUE_DATE));
            String cuid = metaInfo.get(FormDataDocument.META_USER);
            String comment = metaInfo.get(FormDataDocument.META_TASK_COMMENT);
            taskManager.updateTaskInstanceDueDate(taskInstanceId, dueDate, cuid, comment);
            datadoc = FormDataDocument.createSimpleResponse(0, null, taskInstanceId);
        }
        catch (Exception e) {
            logger.severeException(e.getMessage(), e);
            datadoc = FormDataDocument.createSimpleResponse(1, (e.getMessage() == null ? e.getClass().getName() : e.getMessage()), taskInstanceId);
        }
        return datadoc;
    }

    private Map<String, String> autobindInput(ProcessVO procdef, Long bindingProcInstId) throws DataAccessException {
        EventManager eventManager = ServiceLocator.getEventManager();
        List<VariableInstanceInfo> varInstList = eventManager.getProcessInstanceVariables(new Long(bindingProcInstId));
        Map<String, String> params = new HashMap<String, String>();
        for (VariableVO serviceProcVar : procdef.getVariables()) {
            int varCat = serviceProcVar.getVariableCategory().intValue();
            if (varCat != VariableVO.CAT_INPUT && varCat != VariableVO.CAT_INOUT)
                continue;
            String vn = serviceProcVar.getVariableName();
            if (vn.equals(VariableConstants.REQUEST))
                continue;
            if (vn.equals(VariableConstants.MASTER_DOCUMENT))
                continue;
            for (VariableInstanceInfo varinst : varInstList) {
                if (varinst.getName().equalsIgnoreCase(vn)) {
                    params.put(vn, varinst.getStringValue());
                    break;
                }
            }
        }
        return params;
    }

    private void autobindOutput(ProcessVO procdef, Long bindingProcInstId, Long serviceProcInstId)
            throws DataAccessException {
        EventManager eventManager = ServiceLocator.getEventManager();
        for (VariableVO serviceProcVar : procdef.getVariables()) {
            int varCat = serviceProcVar.getVariableCategory().intValue();
            if (varCat != VariableVO.CAT_OUTPUT && varCat != VariableVO.CAT_INOUT)
                continue;
            String vn = serviceProcVar.getVariableName();
            if (vn.equals(VariableConstants.RESPONSE))
                continue;
            if (vn.equals(VariableConstants.MASTER_DOCUMENT))
                continue;
            VariableInstanceInfo varinst = eventManager.getVariableInstance(serviceProcInstId, vn);
            if (varinst == null)
                continue;
            eventManager.setVariableInstance(bindingProcInstId, vn, varinst.getStringValue());
        }
    }

}
