/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.task;

import org.apache.xmlbeans.XmlException;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.SuspendibleActivity;
import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.data.event.EventType;
import com.centurylink.mdw.model.data.task.TaskAction;
import com.centurylink.mdw.model.data.work.WorkStatus;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.model.value.event.EventWaitInstanceVO;
import com.centurylink.mdw.model.value.event.InternalEventVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskServices;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.services.task.TaskManagerAccess;
import com.centurylink.mdw.workflow.activity.AbstractWait;

public class CustomManualTaskActivity extends AbstractWait implements TaskActivity, SuspendibleActivity {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

     /**
     * Executes the controlled activity
     * @throws ActivityException
     */
    public void execute() throws ActivityException {
        String taskName = getTaskName();
        logger.info("Task will be created:" + taskName);
        try {
            Long secondaryOwnerId = super.getWorkTransitionInstanceId();
            String taskLogicalId = getAttributeValue(ATTRIBUTE_TASK_LOGICAL_ID);
            getEngine().createEventWaitInstance(getActivityInstanceId(), "TaskAction-" + getActivityInstanceId(), null, true, true);

            TaskManagerAccess taskMgrAccess = TaskManagerAccess.getInstance();
            if (taskMgrAccess.isRemoteDetail() || taskMgrAccess.isRemoteSummary() ||
                    "true".equalsIgnoreCase(PropertyManager.getProperty("mdw.create.custom.task.thru.event"))) { // non-standard prop
                // create task through old event mechanism
                TaskManagerAccess.getInstance().createClassicTaskInstance(taskLogicalId, getProcessInstanceId(),
                        getActivityInstanceId(), secondaryOwnerId, getMasterRequestId(), getReturnMessage());
            }
            else {
                String taskTemplate = getAttributeValue(ATTRIBUTE_TASK_TEMPLATE);
                if (taskTemplate != null) {
                    // new-style task templates
                    String templateVersion = getAttributeValue(ATTRIBUTE_TASK_TEMPLATE_VERSION);
                    AssetVersionSpec spec = new AssetVersionSpec(taskTemplate, templateVersion == null ? "0" : templateVersion);
                    TaskVO template = TaskTemplateCache.getTaskTemplate(spec);
                    if (template == null)
                        throw new ActivityException("Task template not found: " + spec);
                    taskLogicalId = template.getLogicalId();
                }
                TaskServices taskServices = ServiceLocator.getTaskServices();
                taskServices.createCustomTaskInstance(taskLogicalId, getMasterRequestId(), getProcessInstanceId(),
                        getActivityInstanceId(), secondaryOwnerId);
            }

            EventWaitInstanceVO received = registerWaitEvents(false,true);
            if (received!=null)
              resume(getExternalEventInstanceDetails(received.getMessageDocumentId()), received.getCompletionCode());
        } catch(Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException(-1, ex.getMessage(), ex);
        }
    }

    public String getTaskName(){
        return getAttributeValue(ATTRIBUTE_TASK_NAME);
    }

    public boolean needSuspend() {
        return true;
    }

    protected boolean messageIsTaskAction(String messageString) {
      return messageString.indexOf("ActionRequest") > 0 &&
              messageString.indexOf("TaskAction") > 0;
    }

    protected EventWaitInstanceVO resumeWaitingForTaskAction() throws ActivityException {
        try {
            EventWaitInstanceVO received;
            received = getEngine().createEventWaitInstance(this.getActivityInstanceId(),
                    "TaskAction-" + this.getActivityInstanceId(), null, true, false);
            if (received == null)
                received = registerWaitEvents(true, true);
            return received;
        }
        catch (Exception e) {
            throw new ActivityException(-1, e.getMessage(), e);
        }
    }

    protected void processTaskAction(String messageString) throws ActivityException {
        ActionRequestDocument message;
        try {
            message = ActionRequestDocument.Factory.parse(messageString,
                    Compatibility.namespaceOptions());
        }
        catch (XmlException e) {
            throw new ActivityException(-1, e.getMessage(), e);
        }
        String compCode = null;
        String taskAction = TaskAction.COMPLETE;
        for (Parameter param : message.getActionRequest().getAction().getParameterList()) {
            if (param.getName().equals("Action"))
                taskAction = param.getStringValue();
        }
        if (TaskAction.CALLBACK.equals(taskAction)) {
            String request = "";
            for (Parameter param : message.getActionRequest().getAction().getParameterList()) {
                if (param.getName().equals("Request"))
                    request = param.getStringValue();
            }
            compCode = processTaskCallRequest(request);
            compCode = WorkStatus.STATUSNAME_HOLD + "::" + (compCode == null ? "" : compCode);
        }
        else {
            ProcessVO proc = getProcessDefinition();
            if (proc.isEmbeddedExceptionHandler()) {
                this.setProcessInstanceCompletionCode(taskAction);
                compCode = null;
            }
            else {
                if (taskAction.equals(TaskAction.CANCEL))
                    compCode = WorkStatus.STATUSNAME_CANCELLED + "::";
                else if (taskAction.equals(TaskAction.ABORT))
                    compCode = WorkStatus.STATUSNAME_CANCELLED + "::" + EventType.EVENTNAME_ABORT;
                else if (taskAction.equals(TaskAction.COMPLETE))
                    compCode = null;
                else
                    compCode = taskAction;
            }
        }
        this.setReturnCode(compCode);
        // old code set activity instance to CANCELED when task action is
        // Cancel; shall we do that?
    }

    /**
     * This method is invoked to process a received event (other than task completion).
     * You will need to override this method to customize processing of the event.
     *
     * The default method does nothing.
     *
     * The status of the activity after processing the event is configured in the designer, which
     * can be either Hold or Waiting.
     *
     * When you override this method, you can optionally set different completion
     * code from those configured in the designer by calling setReturnCode().
     *
     * @param messageString the entire message content of the external event (from document table)
     * @throws ActivityException
     */
    protected void processOtherMessage(String messageString) throws ActivityException {
    }

    /**
     * The resume method for ManualTaskAndEventWait is handling internal functions related to
     * task completion as well as custom events, so it is not supposed to be overriden. The method
     * is therefore declared as final. To customize handling of events, please override
     * the method {@link #processOtherMessage(String, String)}
     */
    public final boolean resume(InternalEventVO event) throws ActivityException {
        // secondary owner type must be OwnerType.EXTERNAL_EVENT_INSTANCE
        String messageString = super.getMessageFromEventMessage(event);
        return resume(messageString, event.getCompletionCode());
    }

    protected boolean resume(String message, String completionCode) throws ActivityException {
        if (messageIsTaskAction(message)) {
            processTaskAction(message);
            return true;
        }
        else {
            this.setReturnCode(completionCode);
            processOtherMessage(message);
            Integer actInstStatus = super.handleEventCompletionCode();

            if (actInstStatus.equals(WorkStatus.STATUS_CANCELLED)) {
                try {
                    TaskManagerAccess.getInstance()
                            .cancelTasksOfActivityInstance(this.getActivityInstanceId(), null);
                }
                catch (Exception e) {
                    logger.severeException("Failed to cancel task instance - process moves on", e);
                }
            }
            else if (actInstStatus.equals(WorkStatus.STATUS_WAITING)) {
                try {
                    getEngine().createEventWaitInstance(this.getActivityInstanceId(),
                            "TaskAction-" + this.getActivityInstanceId(), null, true, true);
                }
                catch (Exception e) {
                    logger.severeException("Failed to re-register task action listening", e);
                }
                // unsolicited event listening is already registered by
                // handleEventCompletionCode
            }
            return true;
        }
    }

    protected boolean isRespondingToCall(InternalEventVO eventMessageDoc) {
        String compcode = eventMessageDoc.getCompletionCode();
        if (compcode == null)
            return false;
        if (compcode.equals(EventType.EVENTNAME_RESUME + ":" + TaskAction.CALLBACK))
            return true;
        return false;
    }

    /**
     * This method is made final for the class, as it contains internal logic handling resumption
     * of waiting. It re-register the event waits including waiting for task to complete.
     * If any event has already arrived, it processes it immediately.
     *
     * Customization should be done with the methods {@link #processOtherMessage(String, String)}
     * and {@link #registerWaitEvents()}.
     */
    public final boolean resumeWaiting(InternalEventVO event) throws ActivityException {
        if (isRespondingToCall(event)) {
            String response = processTaskCallResponse();
            try {
                String correlationId = "TaskAction-" + this.getActivityInstanceId().toString();
                broadcastResponse(response, correlationId);
            }
            catch (Exception e) {
                super.logexception("Failed to send jms response to task call", e);
            }
        }
        boolean done;
        EventWaitInstanceVO received;
        try {
            received = getEngine().createEventWaitInstance(this.getActivityInstanceId(),
                    "TaskAction-" + this.getActivityInstanceId(), null, true, false);
            if (received == null)
                received = registerWaitEvents(false, true);
        }
        catch (Exception e) {
            throw new ActivityException(-1, e.getMessage(), e);
        }
        if (received != null) {
            done = resume(getExternalEventInstanceDetails(received.getMessageDocumentId()),
                    received.getCompletionCode());
        }
        else {
            done = false;
        }
        return done;
    }

    protected void broadcastResponse(String message, String correlationId)
            throws JSONException, ProcessException {
        JSONObject json = new JSONObject();
        json.put("ACTION", "NOTIFY");
        json.put("CORRELATION_ID", correlationId);
        json.put("MESSAGE", message);
        InternalMessenger messenger = MessengerFactory.newInternalMessenger();
        messenger.broadcastMessage(json.toString());
    }

    /**
     * This method is invoked when the task instance makes a call to the engine
     * through the method callEngine() of the task manager.
     * The argument request is the request message of callEngine().
     * This method is intended to be overridden to do things like
     * recording the request message in some variables or documents
     * and returning a completion code to transition to appropriate activities.
     * The method does not handle response, as that will be handled by
     * processTaskCallResponse() after the process transitions back
     * to the activity.
     *
     * The default method does nothing and return "Callback" as the completion code.
     *
     * @param request
     * @return completion code
     */
    protected String processTaskCallRequest(String request) {
        return "Callback";
    }

    /**
     * This method is used during processing callEngine(), task manager calls
     * to the engine. It is invoked when the process instance transitions
     * back to the task activity (which should be in hold status) and the method
     * should return a response message which will be received by the task manager
     * as the return value of the method callEngine().
     *
     * The default method does nothing and returns "<undefined/>", and it is expected that
     * the application will override this method to return appropriate response message.
     *
     * @return the response message which will be the return value of the task
     *  manager method callEngine().
     */
    protected String processTaskCallResponse() {
        return "<undefined/>";
    }

}
