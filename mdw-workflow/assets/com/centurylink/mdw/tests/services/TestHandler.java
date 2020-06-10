package com.centurylink.mdw.tests.services;

import com.centurylink.mdw.annotations.Handler;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument;
import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.ProcessVisibilityConstant;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.StringDocument;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.request.Response;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.UserTaskAction;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.request.RequestHandler;
import com.centurylink.mdw.request.RequestHandlerException;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.EventServices;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.services.process.ProcessEngineDriver;
import com.centurylink.mdw.services.request.BaseHandler;
import com.centurylink.mdw.task.types.TaskList;
import com.centurylink.mdw.test.TestException;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlObject;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is used as an old-school action endpoint by a number of test cases.
 * TODO: it would be good to remove or rework this
 */
@Handler(match= RequestHandler.Routing.Content, path="ActionRequest/Action[@Name=RegressionTest]")
public class TestHandler extends BaseHandler {
    @Override
    public Response handleRequest(Request request, Object message, Map<String,String> headers) {
        ActionRequestDocument xmlbean = (ActionRequestDocument)
                ((XmlObject)message).changeType(ActionRequestDocument.type);
        String content = request.getContent();
        String subaction = getSubAction(xmlbean);
        try {
            if (subaction.equals("LaunchProcess"))
                return new Response(handleProcessLaunch(xmlbean, content, headers));
            else if (subaction.equals("Timeout"))
                return new Response(handleTimeout(xmlbean));
            else if (subaction.equals("Signal"))
                return new Response(handleNotifyProcess(xmlbean, content, headers));
            else if (subaction.equals("NotifyProcess"))
                return new Response(handleNotifyProcess(xmlbean, content, headers));
            else if (subaction.equals("Watching"))
                return new Response(handleWatching(xmlbean, content, headers));
            else if (subaction.equals("Stubbing"))
                return new Response(handleStubbing(xmlbean, content, headers));
            else if (subaction.equals("TaskAction"))
                return new Response(handleTaskAction(xmlbean, content, headers));
            else
                throw new Exception("Unknown subaction " + subaction);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return getErrorResponse(request, message, headers, ex);
        }
    }

    private String getSubAction(ActionRequestDocument xmlbean) {
        String subaction = null;
        for (Parameter param : xmlbean.getActionRequest().getAction().getParameterList()) {
            if (param.getName().equalsIgnoreCase("Maintenance"))
                subaction = param.getStringValue();
            else if (param.getName().equalsIgnoreCase("SubAction"))
                subaction = param.getStringValue();
        }
        return subaction == null ? "LaunchProcess" : subaction;
    }

    private String getParameter(ActionRequestDocument xmlbean, String name, boolean required)
            throws RequestHandlerException {
        for (Parameter param : xmlbean.getActionRequest().getAction().getParameterList()) {
            if (param.getName().equals(name)) {
                String value = param.getStringValue();
                if (required && StringUtils.isBlank(value))
                    throw new RequestHandlerException("Parameter is required: " + name);
                return value;
            }
        }
        if (required)
            throw new RequestHandlerException("Parameter is required: " + name);
        return null;
    }

    private String createSuccessResponse(String message) {
        MDWStatusMessageDocument successResponseDoc = MDWStatusMessageDocument.Factory.newInstance();
        MDWStatusMessageDocument.MDWStatusMessage statusMessage = successResponseDoc.addNewMDWStatusMessage();
        statusMessage.setStatusCode(0);
        statusMessage.setStatusMessage(message == null ? "SUCCESS" : message);
        return successResponseDoc.xmlText();
    }

    private String handleProcessLaunch(ActionRequestDocument xmlbean, String message, Map<String,String> metaInfo)
            throws ProcessException, DataAccessException, RequestHandlerException {
        String processName = getParameter(xmlbean, "ProcessName", true);
        String masterRequestId = getParameter(xmlbean, "MasterRequestId", true);
        String perfLevel = getParameter(xmlbean, "PerformanceLevel", false);
        int performanceLevel = perfLevel == null ? 0 : Integer.parseInt(perfLevel);

        Long requestId = new Long(metaInfo.get(Listener.METAINFO_DOCUMENT_ID));
        Process proc = ProcessCache.getProcess(processName);
        Map<String,Object> params = new HashMap<>();
        for (Parameter param : xmlbean.getActionRequest().getAction().getParameterList()) {
            if (param.getName().equals("MasterRequestId"))
                continue;
            if (param.getName().equals("ProcessName"))
                continue;
            if (param.getName().equals("PerformanceLevel"))
                continue;
            Variable var = proc.getVariable(param.getName());
            if (var != null)
                params.put(param.getName(), param.getStringValue());
        }
        String processType = proc.getProcessType();
        if (processType.equals(ProcessVisibilityConstant.SERVICE)) {
            Map<String,String> stringParams = translateInputValues(proc.getId(), params);
            ProcessEngineDriver engineDriver = new ProcessEngineDriver();
            Response resp = engineDriver.invoke(proc.getId(), OwnerType.DOCUMENT, requestId, masterRequestId,
                    message, new HashMap<>(stringParams), null, performanceLevel, null, null, metaInfo);
            return resp == null ? null : resp.getContent();
        } else {
            launchProcess(proc.getId(), requestId, masterRequestId, params, null);
            return createSuccessResponse(null);
        }
    }

    /**
     * Send a DELAY message to all activities in WAITING status of all process instances with
     * the given master request ID
     */
    private String handleTimeout(ActionRequestDocument xmlbean) throws RequestHandlerException {
        String masterRequestId = getParameter(xmlbean, "MasterRequestId", true);
        EventServices eventMgr = ServiceLocator.getEventServices();
        try {
            eventMgr.sendDelayEventsToWaitActivities(masterRequestId);
            return createSuccessResponse(null);
        } catch (Exception ex) {
            throw new RequestHandlerException(ex.getMessage(), ex);
        }
    }

    // send an internal message with the given event name and content
    private String handleNotifyProcess(ActionRequestDocument xmlbean, String message,  Map<String,String> metaInfo)
            throws ProcessException, DataAccessException, RequestHandlerException {
        String eventName = getParameter(xmlbean, "EventName", true);
        String msgContent = getParameter(xmlbean, "Message", true);
        EventServices eventMgr = ServiceLocator.getEventServices();
        eventName = translatePlaceHolder(eventName, xmlbean, eventMgr);
        Long docid = eventMgr.createDocument(StringDocument.class.getName(), OwnerType.DOCUMENT,
                new Long(metaInfo.get(Listener.METAINFO_DOCUMENT_ID)), msgContent, null, StringDocument.class.getName());
        logger.debug("Regression tester notify process with event '" + eventName + "'");
        super.notifyProcesses(eventName, docid, msgContent, 0);
        return createSuccessResponse(null);
    }

    private String getProcessInstanceId(ActionRequestDocument xmlbean, EventServices eventMgr)
            throws ProcessException, DataAccessException, RequestHandlerException {
        String masterRequestId = getParameter(xmlbean, "MasterRequestId", true);
        String processName = getParameter(xmlbean, "ProcessName", true);
        List<ProcessInstance> procInstList = eventMgr.getProcessInstances(masterRequestId, processName);
        if (procInstList != null && procInstList.size() > 0)
            return procInstList.get(0).getId().toString();
        else return "$ProcessInstanceId";
    }

    private String getActivityInstanceId(ActionRequestDocument xmlbean, EventServices eventMgr)
            throws ProcessException, DataAccessException, RequestHandlerException {
        String masterRequestId = getParameter(xmlbean, "MasterRequestId", true);
        String processName = getParameter(xmlbean, "ProcessName", true);
        String activityLogicalId = getParameter(xmlbean, "ActivityLogicalId", true);
        List<ActivityInstance> actInstList = eventMgr.getActivityInstances(masterRequestId, processName, activityLogicalId);
        if (actInstList != null && actInstList.size() > 0)
            return actInstList.get(0).getId().toString();
        else
            return "$ActivityInstanceId";
    }

    private String getVariableValue(ActionRequestDocument xmlbean, EventServices eventMgr, String varname)
            throws ProcessException, DataAccessException, RequestHandlerException {
        String masterRequestId = getParameter(xmlbean, "MasterRequestId", true);
        String processName = getParameter(xmlbean, "ProcessName", true);
        List<ProcessInstance> procInstList = eventMgr.getProcessInstances(masterRequestId, processName);
        if (procInstList == null || procInstList.size() == 0)
            return "$" + varname;
        VariableInstance varinst = eventMgr.getVariableInstance(procInstList.get(0).getId(), varname);
        if (varinst == null)
            return "";
        return varinst.getStringValue(PackageCache.getPackage(ProcessCache.getProcess(processName).getPackageName()));
    }

    private String translatePlaceHolder(String eventName, ActionRequestDocument xmlbean, EventServices eventMgr)
            throws ProcessException, DataAccessException, RequestHandlerException {
        int k, i, n;
        StringBuffer sb = new StringBuffer();
        n = eventName.length();
        for (i=0; i<n; i++) {
            char ch = eventName.charAt(i);
            if (ch=='{') {
                k = i+1;
                while (k<n) {
                    ch = eventName.charAt(k);
                    if (ch=='}') break;
                    k++;
                }
                if (k<n) {
                    String expression = eventName.substring(i+1,k);
                    String value;
                    if (expression.startsWith("$")) {
                        String varname = expression.substring(1);
                        if (varname.equalsIgnoreCase(Variable.PROCESS_INSTANCE_ID))
                            value = getProcessInstanceId(xmlbean, eventMgr);
                        else if (varname.equalsIgnoreCase(Variable.MASTER_REQUEST_ID))
                            value = getParameter(xmlbean, "MasterRequestId", true);
                        else if (varname.equalsIgnoreCase(Variable.ACTIVITY_INSTANCE_ID))
                            value = getActivityInstanceId(xmlbean, eventMgr);
                        else value = getVariableValue(xmlbean, eventMgr, varname);
                    } else {
                        value = expression;        // keep placeholder itself
                    }
                    sb.append(value);
                } // else  '{' without '}' - ignore string after '{'
                i = k;
            } else if (ch == '\\' ) {
                ++i;
                sb.append(eventName.charAt(i));

            } else sb.append(ch);
        }
        return sb.toString();
    }

    // turn watching mode on and off
    private String handleWatching(ActionRequestDocument xmlbean, String message, Map<String,String> metaInfo)
            throws ProcessException, DataAccessException, RequestHandlerException {
        String server = getParameter(xmlbean, "Server", false);
        String mode = getParameter(xmlbean, "Mode", true);
        if (mode.equals("on")) {
            if (server == null)
                throw new RequestHandlerException("Need Server specified");
            setPropertyGlobally(PropertyNames.MDW_LOGGING_WATCHER, server);
        } else {
            setPropertyGlobally(PropertyNames.MDW_LOGGING_WATCHER, null);
        }
        return createSuccessResponse(null);
    }

    // turn stubbing mode for server on and off
    private String handleStubbing(ActionRequestDocument xmlbean, String message, Map<String,String> metaInfo)
            throws Exception {
        String server=getParameter(xmlbean, "Server", false);
        String mode=getParameter(xmlbean, "Mode", true);
        if (mode.equals("on")) {
            if (server == null)
                throw new Exception("Need Server specified");
            setPropertyGlobally(PropertyNames.MDW_STUB_SERVER, server);
        } else {
            setPropertyGlobally(PropertyNames.MDW_STUB_SERVER, null);
        }
        return createSuccessResponse(null);
    }

    private Long findTaskInstanceId(String taskName, String user, String masterRequestId)
            throws RequestHandlerException {
        Query query = new Query("");
        query.setFilter("masterRequestId", masterRequestId);
        query.setFilter("name", taskName);
        query.setFilter("app", "autotest");
        query.setDescending(true);

        try {
            TaskList taskList = ServiceLocator.getTaskServices().getTasks(query, user);
            List<TaskInstance> taskInstances = taskList.getTasks();
            if (taskInstances.isEmpty())
                throw new TestException("Cannot find task instances: " + query);

            TaskInstance taskInstance = taskInstances.get(0); // latest
            return taskInstance.getTaskInstanceId();
        } catch (ServiceException ex) {
            throw new RequestHandlerException(ex.getMessage(), ex);
        }
    }

    // handle emulation of task action
    private String handleTaskAction(ActionRequestDocument xmlbean, String message, Map<String,String> metaInfo)
            throws ProcessException, DataAccessException, ServiceException, RequestHandlerException, IOException {
        String cuid = getParameter(xmlbean, "User", true);
        String taskName = getParameter(xmlbean, "TaskName", true);
        String directAction = getParameter(xmlbean, "DirectAction", false);
        String masterRequestId = getParameter(xmlbean, "MasterRequestId", true);
        Long taskInstId = findTaskInstanceId(taskName, cuid, masterRequestId);
        List<Parameter> params = xmlbean.getActionRequest().getAction().getParameterList();
        if (params != null && !params.isEmpty()) {
            TaskInstance taskInst = ServiceLocator.getTaskServices().getInstance(taskInstId);
            if (taskInst.getOwnerType().equals(OwnerType.PROCESS_INSTANCE)) {
                EventServices eventManager = ServiceLocator.getEventServices();
                Long procInstId = taskInst.getOwnerId();
                ProcessInstance procInst = eventManager.getProcessInstance(procInstId);
            }
        }
        ServiceLocator.getTaskServices().performAction(taskInstId, directAction, cuid, cuid, null, null, true);
        UserTaskAction taskAction = new UserTaskAction();
        taskAction.setTaskAction(directAction);
        taskAction.setTaskInstanceId(taskInstId);
        taskAction.setUser(cuid);
        return createSuccessResponse(taskAction.getJson().toString(2));
    }

    private void setPropertyGlobally(String name, String value) throws ProcessException {
        JSONObject json = new JsonObject();
        json.put("ACTION", "REFRESH_PROPERTY");
        json.put("NAME", name);
        json.put("VALUE", value == null ? "" : value);
        InternalMessenger messenger = MessengerFactory.newInternalMessenger();
        messenger.broadcastMessage(json.toString());
    }
}
