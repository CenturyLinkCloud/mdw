/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

import com.centurylink.mdw.bpm.MDWStatusMessageDocument;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument.MDWStatusMessage;
import com.centurylink.mdw.bpm.WorkTypeDocument.WorkType;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.MDWException;
import com.centurylink.mdw.constant.ActivityResultCodeConstant;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.VariableConstants;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.event.EventHandlerException;
import com.centurylink.mdw.listener.ExternalEventHandlerBase;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.service.Action;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.service.data.task.TaskDataAccess;
import com.centurylink.mdw.service.data.task.TaskTemplateCache;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.UserManager;
import com.centurylink.mdw.services.process.ProcessEngineDriver;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class InstanceLevelActionHandler extends ExternalEventHandlerBase {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public String handleEventMessage(String msg, Object xmlBean, Map<String, String> metaInfo)
    throws EventHandlerException {

        try {
            XmlOptions opts = new XmlOptions().setDocumentType(ActionRequestDocument.type);
            ActionRequestDocument actionRequestDocument = ActionRequestDocument.Factory.parse(msg, opts);
            Action action = actionRequestDocument.getActionRequest().getAction();
            String workType = getActionParam(action, "mdw.WorkType", true);
            if (workType.equals(WorkType.PROCESS.toString())) {
                String actionType = getActionParam(action, "mdw.Action", true);
                if (actionType.equals("Launch")) {
                    Long processId = new Long(getActionParam(action, "mdw.DefinitionId", true));
                    Process procdef = getProcessDefinition(processId);
                    if (procdef == null)
                        throw new EventHandlerException("Process Definition not found for ID: " + processId);
                    Package pkg = PackageCache.getProcessPackage(processId);
                    if (pkg != null && !pkg.isDefaultPackage())
                        setPackage(pkg); // prefer process package over default of EventHandler
                    String masterRequestId = getActionParam(action, "mdw.MasterRequestId", true);
                    String actIdStr = getActionParam(action, "mdw.ActivityId", false);
                    Long activityId = actIdStr!=null?new Long(actIdStr):null;
                    String owner = getActionParam(action, "mdw.Owner", true);
                    Long ownerId = new Long(getActionParam(action, "mdw.OwnerId", true));
                    boolean synchronous = Boolean.parseBoolean(getActionParam(action, "mdw.Synchronous", false));
                    String responseVarName = getActionParam(action, "mdw.ResponseVariableName", false);
                    Map<String,Object> processParams = new HashMap<String,Object>();
                    for (Parameter parameter : action.getParameterList()) {
                        if (!standardParams.contains(parameter.getName())) {
                          String paramType = parameter.getType();
                          if (paramType == null) {
                              // treat parameter as string
                              processParams.put(parameter.getName(), parameter.getStringValue());
                          }
                          else {
                              com.centurylink.mdw.variable.VariableTranslator translator = VariableTranslator.getTranslator(pkg, paramType);
                              if (translator instanceof DocumentReferenceTranslator) {
                                  DocumentReferenceTranslator docTranslator = (DocumentReferenceTranslator)translator;;
                                  Object document = docTranslator.realToObject(parameter.getStringValue());
                                  DocumentReference docRef = createDocument(paramType, document, owner, ownerId, new Long(0), null, null);
                                  processParams.put(parameter.getName(), docRef.toString());
                              }
                              else {
                                  processParams.put(parameter.getName(), translator.toObject(parameter.getStringValue()));
                              }
                          }
                        }
                    }
                    Variable requestVar = procdef.getVariable(VariableConstants.REQUEST);
                    DocumentReference docRef = createDocument(owner, ownerId, actionRequestDocument, requestVar == null ? null : requestVar.getVariableType());
                    if (synchronous) {
                        return invokeServiceProcess(processId, docRef.getDocumentId(), masterRequestId, msg, processParams, responseVarName, 0);
                    }
                    else if (activityId != null) {
                        Map<String,String> params = translateParameters(processId, processParams);
                        ProcessEngineDriver engine = new ProcessEngineDriver();
                        Long procInstId = engine.startProcessFromActivity(processId, activityId, masterRequestId, OwnerType.TESTER, docRef.getDocumentId(), params, null);
                        return createSuccessResponse("Process '" + procdef.getProcessName() + "' successfully launched (Instance ID=" + procInstId + ")");
                    }
                    else {
                        Map<String,String> params = translateParameters(processId, processParams);
                        ProcessEngineDriver driver = new ProcessEngineDriver();
                        Long procInstId = driver.startProcess(processId, masterRequestId, OwnerType.TESTER, docRef.getDocumentId(), params, null, null, null);
                        return createSuccessResponse("Process '" + procdef.getProcessName() + "' successfully launched (Instance ID=" + procInstId + ")");
                    }
                }
                else {
                    return createErrorResponse("Unsupported Process action: " + actionType);
                }
            }
            else if (workType.equals(WorkType.ACTIVITY.toString())) {
                String actionType = getActionParam(action, "mdw.Action", true);
                Long definitionId = new Long(getActionParam(action, "mdw.DefinitionId", true));
                Long instanceId = new Long(getActionParam(action, "mdw.InstanceId", true));
                EventManager eventMgr = ServiceLocator.getEventManager();
                ActivityInstance actInstVO = eventMgr.getActivityInstance(instanceId);
                ProcessInstance procInstVO = eventMgr.getProcessInstance(actInstVO.getOwnerId());

                if (actionType.equals(ActivityResultCodeConstant.RESULT_RETRY)) {
                    checkProcessInstanceStatus(procInstVO, actionType);

                    eventMgr.retryActivity(definitionId, instanceId);
                    return createSuccessResponse("Activity instance ID: '" + instanceId + "' Retry initiated.");
                }
                else if (actionType.equals(ActivityResultCodeConstant.RESULT_SKIP)) {
                    String completionCode = getActionParam(action, "mdw.CompletionCode", false);
                    checkProcessInstanceStatus(procInstVO, actionType);

                    eventMgr.skipActivity(definitionId, instanceId, completionCode);
                    return createSuccessResponse("Activity instance ID: '" + instanceId + "' Skip initiated with Completion Code: " + completionCode + ".");
                }
                else {
                    return createErrorResponse("Unsupported Activity action: " + actionType);
                }
            }
            else if (workType.equals("Task")) {
                String actionType = getActionParam(action, "mdw.Action", true);
                String user = getActionParam(action, "mdw.User", true);

                Long taskInstanceId = null;
                String instanceId = getActionParam(action, "mdw.InstanceId", false);
                if (instanceId != null) {
                    taskInstanceId = new Long(instanceId);
                }
                else {
                    String taskName = getActionParam(action, "mdw.TaskName", true);
                    String masterRequestId = getActionParam(action, "mdw.MasterRequestId", true);
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
                        return createErrorResponse("Task definition not found for: '" + taskName + "'");
                    List<Long> tiList = new TaskDataAccess(new DatabaseAccess(null)).findTaskInstance(taskVo.getTaskId(), masterRequestId);
                    if (tiList.size() < k + 1)
                        return createErrorResponse("Cannot find the task instance for masterRequestId: " + masterRequestId + " and name: '" + taskName + "'");
                    taskInstanceId = tiList.get(k);
                }
                UserManager userManager = ServiceLocator.getUserManager();
                Long userId = userManager.getUser(user).getId();
                TaskManager taskManager = ServiceLocator.getTaskManager();
                taskManager.performActionOnTaskInstance(actionType, taskInstanceId, userId, userId, null, null, true);
                return createSuccessResponse("Action performed on Task: '" + actionType + "'");
            }
            else {
                return createErrorResponse("Unable to handle resource request for Action: " + action.getName());
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return createErrorResponse(ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }

    }

    private void checkProcessInstanceStatus(ProcessInstance processInstanceVO, String action) throws MDWException {
        int statusCode = processInstanceVO.getStatusCode();
        if (statusCode == WorkStatus.STATUS_COMPLETED.intValue())
            throw new MDWException("Cannot perform action " + action + " on Completed process instance ID: " + processInstanceVO.getId());
        if (statusCode == WorkStatus.STATUS_CANCELLED.intValue())
            throw new MDWException("Cannot perform action " + action + " on Cancelled process instance ID: " + processInstanceVO.getId());
    }

    private String getActionParam(Action action, String paramName, boolean required) throws MDWException {
        String paramValue = null;
        for (Parameter parameter : action.getParameterList()) {
            if (parameter.getName().equals(paramName)) {
                paramValue = parameter.getStringValue();
                break;
            }
        }
        if (required && paramValue == null)
            throw new MDWException("Missing action parameter: " + paramName);
        else
            return paramValue;
    }

    private String createSuccessResponse(String message) {
        MDWStatusMessageDocument successResponseDoc = MDWStatusMessageDocument.Factory.newInstance();
        MDWStatusMessage statusMessage = successResponseDoc.addNewMDWStatusMessage();
        statusMessage.setStatusCode(0);
        statusMessage.setStatusMessage(message);
        return successResponseDoc.xmlText(getXmlOptions());
    }

    private String createErrorResponse(String message) {
        MDWStatusMessageDocument errorResponseDoc = MDWStatusMessageDocument.Factory.newInstance();
        MDWStatusMessage statusMessage = errorResponseDoc.addNewMDWStatusMessage();
        statusMessage.setStatusCode(-1);
        statusMessage.setStatusMessage(message);
        return errorResponseDoc.xmlText(getXmlOptions());
    }

    private XmlOptions getXmlOptions() {
        return new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2);
    }

    private DocumentReference createDocument(String ownerType, Long ownerId, XmlObject xmlBean, String requestDocType)
    throws DataAccessException {
        EventManager eventMgr = ServiceLocator.getEventManager();
        String docType = requestDocType == null ? XmlObject.class.getName() : requestDocType;
        Long docid = eventMgr.createDocument(docType, ownerType, ownerId, xmlBean.xmlText());
        return new DocumentReference(docid);
    }

    private static List<String> standardParams = new ArrayList<String>();
    static {
      standardParams.add("mdw.WorkType");
      standardParams.add("mdw.Action");
      standardParams.add("mdw.DefinitionId");
      standardParams.add("mdw.MasterRequestId");
      standardParams.add("mdw.Owner");
      standardParams.add("mdw.OwnerId");
      standardParams.add("mdw.InstanceId");
      standardParams.add("mdw.CompletionCode");
      standardParams.add("mdw.Synchronous");
      standardParams.add("mdw.ResponseVariableName");
      standardParams.add("mdw.DbUrl");
      standardParams.add("mdw.User");
      standardParams.add("mdw.Protocol");
      standardParams.add("mdw.Message");
      standardParams.add("mdw.ActivityId");
      standardParams.add("mdw.PackageId");
    }
}
