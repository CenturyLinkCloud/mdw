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
package com.centurylink.mdw.listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.bpm.MDWStatusMessageDocument;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument.MDWStatusMessage;
import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.ProcessVisibilityConstant;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.event.EventHandlerException;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.StringDocument;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.model.task.UserTaskAction;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.service.data.task.TaskDataAccess;
import com.centurylink.mdw.service.data.task.TaskTemplateCache;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.util.StringHelper;

public class RegressionTestEventHandler extends ExternalEventHandlerBase {

    public String handleEventMessage(String message, Object msgdoc, Map<String,String> metaInfo)
            throws EventHandlerException {
        ActionRequestDocument xmlbean = (ActionRequestDocument)
                ((XmlObject)msgdoc).changeType(ActionRequestDocument.type);
        String subaction = getSubAction(xmlbean);
        try {
            if (subaction.equals("LaunchProcess")) return handleProcessLaunch(xmlbean, message, metaInfo);
            else if (subaction.equals("Timeout")) return handleTimeout(xmlbean, message, metaInfo);
            else if (subaction.equals("Signal")) return handleNotifyProcess(xmlbean, message, metaInfo);
            else if (subaction.equals("NotifyProcess")) return handleNotifyProcess(xmlbean, message, metaInfo);
            else if (subaction.equals("Watching")) return handleWatching(xmlbean, message, metaInfo);
            else if (subaction.equals("Stubbing")) return handleStubbing(xmlbean, message, metaInfo);
            else if (subaction.equals("TaskAction")) return handleTaskAction(xmlbean, message, metaInfo);
            else throw new Exception("Unknown subaction " + subaction);
        } catch (Exception e) {
            logger.severeException(e.getMessage(), e);
            return createErrorResponse(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private String getSubAction(ActionRequestDocument xmlbean) {
        String subaction = null;
        for (Parameter param : xmlbean.getActionRequest().getAction().getParameterList()) {
            if (param.getName().equalsIgnoreCase("Maintenance")) subaction = param.getStringValue();
            else if (param.getName().equalsIgnoreCase("SubAction")) subaction = param.getStringValue();
        }
        return subaction==null?"LaunchProcess":subaction;
    }

    private String getParameter(ActionRequestDocument xmlbean, String name, boolean required) throws Exception {
        for (Parameter param : xmlbean.getActionRequest().getAction().getParameterList()) {
            if (param.getName().equals(name)) {
                String value = param.getStringValue();
                if (required && StringHelper.isEmpty(value)) throw new Exception("Parameter is required: " + name);
                return value;
            }
        }
        if (required) throw new Exception("Parameter is required: " + name);
        return null;
    }

    private String createSuccessResponse(String message) {
        MDWStatusMessageDocument successResponseDoc = MDWStatusMessageDocument.Factory.newInstance();
        MDWStatusMessage statusMessage = successResponseDoc.addNewMDWStatusMessage();
        statusMessage.setStatusCode(0);
        statusMessage.setStatusMessage(message==null?"SUCCESS":message);
        return successResponseDoc.xmlText();
    }

    private String createErrorResponse(String message) {
        MDWStatusMessageDocument errorResponseDoc = MDWStatusMessageDocument.Factory.newInstance();
        MDWStatusMessage statusMessage = errorResponseDoc.addNewMDWStatusMessage();
        statusMessage.setStatusCode(-1);
        statusMessage.setStatusMessage(message);
        return errorResponseDoc.xmlText();
    }

    private String handleProcessLaunch(ActionRequestDocument xmlbean, String message, Map<String,String> metaInfo)
    throws Exception {
         String processName = getParameter(xmlbean, "ProcessName", true);
         String masterRequestId = getParameter(xmlbean, "MasterRequestId", true);
         String performanceLevel = getParameter(xmlbean, "PerformanceLevel", false);
         int performance_level = performanceLevel==null?0:Integer.parseInt(performanceLevel);

         String resp;
         Long eventInstId = new Long(metaInfo.get(Listener.METAINFO_DOCUMENT_ID));
         Long processId = getProcessId(processName);
         Process procVO = getProcessDefinition(processId);
         Map<String,Object> params = new HashMap<String,Object>();
         for (Parameter param : xmlbean.getActionRequest().getAction().getParameterList()) {
             if (param.getName().equals("MasterRequestId")) continue;
             if (param.getName().equals("ProcessName")) continue;
             if (param.getName().equals("PerformanceLevel")) continue;
             Variable var = procVO.getVariable(param.getName());
             if (var!=null) params.put(param.getName(), param.getStringValue());
         }
         String processType = procVO.getProcessType();
         if (processType.equals(ProcessVisibilityConstant.SERVICE)) {
             resp = invokeServiceProcess(procVO.getProcessId(), eventInstId, masterRequestId,
                     message, params, null, performance_level, null);
         } else {
             launchProcess(processId, eventInstId, masterRequestId, params, null);
             resp = createSuccessResponse(null);
         }
         return resp;
    }

    // send a DELAY message to all activities in WAITING status of all process instances with
    // the given master request ID
    private String handleTimeout(ActionRequestDocument xmlbean, String message,  Map<String,String> metaInfo)
    throws Exception {
        String masterRequestId = getParameter(xmlbean, "MasterRequestId", true);
        EventManager eventMgr = ServiceLocator.getEventManager();
        eventMgr.sendDelayEventsToWaitActivities(masterRequestId);
        return createSuccessResponse(null);
    }

    // send an internal message with the given event name and content
    private String handleNotifyProcess(ActionRequestDocument xmlbean, String message,  Map<String,String> metaInfo)
    throws Exception {
        String eventName = getParameter(xmlbean, "EventName", true);
        String msgContent = getParameter(xmlbean, "Message", true);
        EventManager eventMgr = ServiceLocator.getEventManager();
        eventName = translatePlaceHolder(eventName, xmlbean, eventMgr);
        Long docid = eventMgr.createDocument(StringDocument.class.getName(), OwnerType.DOCUMENT,
                new Long(metaInfo.get(Listener.METAINFO_DOCUMENT_ID)), msgContent, null);
        logger.debug("Regression tester notify process with event '" + eventName + "'");
        super.notifyProcesses(eventName, docid, msgContent, 0);
        return createSuccessResponse(null);
    }

    private String getProcessInstanceId(ActionRequestDocument xmlbean, EventManager eventMgr) throws Exception {
        String masterRequestId=getParameter(xmlbean, "MasterRequestId", true);
        String processName=getParameter(xmlbean, "ProcessName", true);
        List<ProcessInstance> procInstList = eventMgr.getProcessInstances(masterRequestId, processName);
        if (procInstList!=null && procInstList.size()>0)
            return procInstList.get(0).getId().toString();
        else return "$ProcessInstanceId";
    }

    private String getActivityInstanceId(ActionRequestDocument xmlbean, EventManager eventMgr) throws Exception {
        String masterRequestId=getParameter(xmlbean, "MasterRequestId", true);
        String processName=getParameter(xmlbean, "ProcessName", true);
        String activityLogicalId=getParameter(xmlbean, "ActivityLogicalId", true);
        List<ActivityInstance> actInstList = eventMgr.getActivityInstances(masterRequestId,
                processName, activityLogicalId);
        if (actInstList!=null && actInstList.size()>0) return actInstList.get(0).getId().toString();
        else return "$ActivityInstanceId";
    }

    private String getVariableValue(ActionRequestDocument xmlbean, EventManager eventMgr, String varname) throws Exception {
        String masterRequestId=getParameter(xmlbean, "MasterRequestId", true);
        String processName=getParameter(xmlbean, "ProcessName", true);
        List<ProcessInstance> procInstList = eventMgr.getProcessInstances(masterRequestId, processName);
        if (procInstList==null || procInstList.size()==0) return "$" + varname;
        VariableInstance varinst = eventMgr.getVariableInstance(procInstList.get(0).getId(), varname);
        if (varinst==null) return "";
        return varinst.getStringValue();
    }

    private String translatePlaceHolder(String eventName, ActionRequestDocument xmlbean, EventManager eventMgr)
    throws Exception {
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
    throws Exception {
        String server=getParameter(xmlbean, "Server", false);
        String mode=getParameter(xmlbean, "Mode", true);
        if (mode.equals("on")) {
            if (server==null) throw new Exception("Need Server specified");
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
            if (server==null) throw new Exception("Need Server specified");
            setPropertyGlobally(PropertyNames.MDW_STUB_SERVER, server);
        } else {
            setPropertyGlobally(PropertyNames.MDW_STUB_SERVER, null);
        }
        return createSuccessResponse(null);
    }

    private Long findTaskInstanceId(String taskName, String masterRequestId) throws Exception {
        int n = taskName.length();
        int k = 0;
        if (n > 3 && taskName.charAt(n - 3) == '[' &&
                Character.isDigit(taskName.charAt(n - 2)) &&
                taskName.charAt(n - 1)==']') {
            k = Integer.parseInt(taskName.substring(n - 2, n - 1));
            taskName = taskName.substring(0, n - 3);
        }
        TaskTemplate taskVo = TaskTemplateCache.getTemplateForName(taskName);
        if (taskVo == null)
            throw new Exception("No task found for name: '" + taskName + "'");
        List<Long> tiList = new TaskDataAccess(new DatabaseAccess(null)).findTaskInstance(taskVo.getTaskId(), masterRequestId);
        if (tiList.size() < k + 1)
            throw new Exception("Cannot find the task instance with request: " + masterRequestId + " and name: '" + taskName + "'");
        Long taskInstId;
        if (k > 0)
            taskInstId = tiList.get(tiList.size() - k - 1); //Task instance Id is returned in desc order so reverse the order here.
        else
            taskInstId = tiList.get(k);
        return taskInstId;
    }

    // handle emulation of task action specified in form data document
    private String handleTaskAction(ActionRequestDocument xmlbean, String message, Map<String,String> metaInfo)
    throws Exception {
        String cuid = getParameter(xmlbean, "User", true);
        String taskName = getParameter(xmlbean, "TaskName", true);
        String directAction = getParameter(xmlbean, "DirectAction", false);
        String masterRequestId = getParameter(xmlbean, "MasterRequestId", true);
        Long taskInstId = this.findTaskInstanceId(taskName, masterRequestId);
        List<Parameter> params = xmlbean.getActionRequest().getAction().getParameterList();
        if (params!=null && !params.isEmpty()) {
            TaskInstance taskInst = ServiceLocator.getTaskServices().getInstance(taskInstId);
            if (taskInst.getOwnerType().equals(OwnerType.PROCESS_INSTANCE)) {
                EventManager eventManager = ServiceLocator.getEventManager();
                Long procInstId = taskInst.getOwnerId();
                ProcessInstance procInst = eventManager.getProcessInstance(procInstId);
                Process procdef = super.getProcessDefinition(procInst.getProcessId());
                if (procInst.isEmbedded()) {
                    procInstId = procInst.getOwnerId();
                }
                for (Parameter param : params) {
                    String pname = param.getName();
                    if (pname.startsWith("formdata.")) {
                        String varname = pname.substring(9);
                        VariableInstance var = eventManager.getVariableInstance(procInstId, varname);
                        if (var==null) {
                            Variable vardef = procdef.getVariable(varname);
                            if (vardef==null) throw new Exception("The variable is not defined: " + varname);
                            if (vardef.isDocument()) {
                                Long docid = eventManager.createDocument(vardef.getVariableType(),
                                    OwnerType.PROCESS_INSTANCE, procInstId, param.getStringValue(), null);
                                eventManager.setVariableInstance(procInstId, varname, new DocumentReference(docid));
                            } else {
                                eventManager.setVariableInstance(procInstId, varname, param.getStringValue());
                            }
                        } else {
                            if (var.isDocument()) {
                                DocumentReference docref = (DocumentReference)var.getData();
                                eventManager.updateDocumentContent(docref.getDocumentId(),
                                        param.getStringValue(), var.getType(), null);
                            } else {
                                eventManager.setVariableInstance(procInstId, varname, param.getStringValue());
                            }
                        }
                    }
                }
            }
        }
        ServiceLocator.getTaskServices().performAction(taskInstId, directAction, cuid, cuid, null, null, true);
        UserTaskAction taskAction = new UserTaskAction();
        taskAction.setTaskAction(directAction);
        taskAction.setTaskInstanceId(taskInstId);
        taskAction.setUser(cuid);
        return createSuccessResponse(taskAction.getJson().toString(2));
    }

    private void setPropertyGlobally(String name, String value)
            throws MdwException, JSONException {
        JSONObject json = new JsonObject();
        json.put("ACTION", "REFRESH_PROPERTY");
        json.put("NAME", name);
        json.put("VALUE", value==null?"":value);
        InternalMessenger messenger = MessengerFactory.newInternalMessenger();
        messenger.broadcastMessage(json.toString());
    }

}
