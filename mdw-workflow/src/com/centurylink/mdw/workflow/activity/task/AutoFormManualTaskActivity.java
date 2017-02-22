/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.task;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.constant.FormConstants;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.service.data.task.TaskTemplateCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.TaskServices;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.CallURL;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.AbstractWait;

@Tracked(LogLevel.TRACE)
public class AutoFormManualTaskActivity extends AbstractWait implements TaskActivity {

    protected static final String WAIT_FOR_TASK = "Wait for Task";

    public boolean needSuspend() {
        String waitForTask = this.getAttributeValue(WAIT_FOR_TASK);
        return waitForTask==null || waitForTask.equalsIgnoreCase("true");
    }

    /**
     *
     * The method creates a new task instance, or in case a task instance ID
     * is already known (the method getTaskInstanceId() return non-null),
     * the method sends a response back to the task instance.
     *
     */
    @Override
    public void execute() throws ActivityException {
        try {
            String taskTemplate = getAttributeValue(ATTRIBUTE_TASK_TEMPLATE);
            if (taskTemplate == null)
                throw new ActivityException("Missing attribute: " + ATTRIBUTE_TASK_TEMPLATE);
            // new-style task templates
            String templateVersion = getAttributeValue(ATTRIBUTE_TASK_TEMPLATE_VERSION);
            AssetVersionSpec spec = new AssetVersionSpec(taskTemplate, templateVersion == null ? "0" : templateVersion);
            TaskTemplate template = TaskTemplateCache.getTaskTemplate(spec);
            if (template == null)
                throw new ActivityException("Task template not found: " + spec);
            TaskServices taskServices = ServiceLocator.getTaskServices();
            Long taskInstanceId = taskServices.createTaskInstance(spec,
                    getMasterRequestId(), getProcessInstanceId(), getActivityInstanceId(), getWorkTransitionInstanceId()).getTaskInstanceId();

            String taskInstCorrelationId = FormConstants.TASK_CORRELATION_ID_PREFIX + taskInstanceId.toString();
            super.loginfo("Task instance created - ID " + taskInstanceId);
            if (this.needSuspend()) {
                getEngine().createEventWaitInstance(
                        this.getActivityInstanceId(),
                        taskInstCorrelationId,
                        EventType.EVENTNAME_FINISH, true, true);
                EventWaitInstance received = registerWaitEvents(false,true);
                if (received!=null)
                        resume(getExternalEventInstanceDetails(received.getMessageDocumentId()),
                                         received.getCompletionCode());
            }
        } catch (Exception e) {
            throw new ActivityException(-1, e.getMessage(), e);
        }
    }

    public final boolean resume(InternalEvent event)
            throws ActivityException {
        // secondary owner type must be OwnerType.EXTERNAL_EVENT_INSTANCE
        String messageString = super.getMessageFromEventMessage(event);
        return resume(messageString, event.getCompletionCode());
    }

    protected boolean resume(String message, String completionCode) throws ActivityException {
        if (messageIsTaskAction(message)) {
            processTaskAction(message);
            return true;
        } else {
            this.setReturnCode(completionCode);
            processOtherMessage(message);
            Integer actInstStatus = super.handleEventCompletionCode();
            if (actInstStatus.equals(WorkStatus.STATUS_CANCELLED)) {
                try {
                    ServiceLocator.getTaskManager().
                        cancelTasksOfActivityInstance(getActivityInstanceId());
                } catch (Exception e) {
                    logger.severeException("Failed to cancel task instance - process moves on", e);
                }
            } else if (actInstStatus.equals(WorkStatus.STATUS_WAITING)) {
                try {
                    TaskManager taskMgr = ServiceLocator.getTaskManager();
                    TaskInstance taskInst = taskMgr.getTaskInstanceByActivityInstanceId(this.getActivityInstanceId());
                    String eventName = FormConstants.TASK_CORRELATION_ID_PREFIX + taskInst.getTaskInstanceId().toString();

                    getEngine().createEventWaitInstance(
                            this.getActivityInstanceId(),
                            eventName,
                            null, true, true);
                } catch (Exception e) {
                    logger.severeException("Failed to re-register task action listening", e);
                }
                // unsolicited event listening is already registered by handleEventCompletionCode
            }
            return true;
        }
    }

    public final boolean resumeWaiting(InternalEvent event) throws ActivityException {
        boolean done;
        EventWaitInstance received;
        try {
            // re-register wait events
            TaskManager taskMgr = ServiceLocator.getTaskManager();
            TaskInstance taskInst = taskMgr.getTaskInstanceByActivityInstanceId(this.getActivityInstanceId());
            String eventName = FormConstants.TASK_CORRELATION_ID_PREFIX + taskInst.getTaskInstanceId().toString();

            received = getEngine().createEventWaitInstance(
                    this.getActivityInstanceId(),
                    eventName,
                    null, true, false);
            if (received==null) received = registerWaitEvents(false,true);
        } catch (Exception e) {
            throw new ActivityException(-1, e.getMessage(), e);
        }
        if (received!=null) {
            done = resume(getExternalEventInstanceDetails(received.getMessageDocumentId()),
                    received.getCompletionCode());
        } else {
            done = false;
        }
        return done;
    }

    protected boolean messageIsTaskAction(String messageString) throws ActivityException {
        if (messageString.startsWith("{")) {
            JSONObject jsonobj;
            try {
                jsonobj = new JSONObject(messageString);
                JSONObject meta = jsonobj.has("META")?jsonobj.getJSONObject("META"):null;
                if (meta==null || !meta.has(FormConstants.FORMATTR_ACTION)) return false;
                String action = meta.getString(FormConstants.FORMATTR_ACTION);
                return action!=null && action.startsWith("@");
            } catch (JSONException e) {
                throw new ActivityException(0, "Failed to parse JSON message", e);
            }
        } else {
            int k = messageString.indexOf("FORMDATA");
            return k>0 && k<8;
        }
    }

    protected void processTaskAction(String messageString) throws ActivityException {
        try {
            JSONObject datadoc = new JSONObject(messageString);
            String compCode = extractFormData(datadoc); // this handles both embedded proc and not
            datadoc = datadoc.getJSONObject("META");
            String action = datadoc.getString(FormConstants.FORMATTR_ACTION);
            CallURL callurl = new CallURL(action);
            action = callurl.getAction();
            if (compCode==null) compCode = datadoc.has(FormConstants.URLARG_COMPLETION_CODE) ? datadoc.getString(FormConstants.URLARG_COMPLETION_CODE) : null;
            if (compCode==null) compCode = callurl.getParameter(FormConstants.URLARG_COMPLETION_CODE);
            String subaction = datadoc.has(FormConstants.URLARG_ACTION) ? datadoc.getString(FormConstants.URLARG_ACTION) : null;
            if (subaction==null) subaction = callurl.getParameter(FormConstants.URLARG_ACTION);
            if (this.getProcessInstance().isEmbedded()) {
                if (subaction==null)
                    subaction = compCode;
                if (action.equals("@CANCEL_TASK")) {
                    if (TaskAction.ABORT.equalsIgnoreCase(subaction))
                        compCode = EventType.EVENTNAME_ABORT + ":process";
                    else compCode = EventType.EVENTNAME_ABORT;
                } else {    // FormConstants.ACTION_COMPLETE_TASK
                    if (TaskAction.RETRY.equalsIgnoreCase(subaction))
                        compCode = TaskAction.RETRY;
                    else if (compCode==null) compCode = EventType.EVENTNAME_FINISH;
                    else compCode = EventType.EVENTNAME_FINISH + ":" + compCode;
                }
                this.setProcessInstanceCompletionCode(compCode);
                setReturnCode(null);
            } else {
                if (action.equals("@CANCEL_TASK")) {
                    if (TaskAction.ABORT.equalsIgnoreCase(subaction))
                        compCode = WorkStatus.STATUSNAME_CANCELLED + "::" + EventType.EVENTNAME_ABORT;
                    else compCode = WorkStatus.STATUSNAME_CANCELLED + "::";
                    setReturnCode(compCode);
                } else {    // FormConstants.ACTION_COMPLETE_TASK
                    setReturnCode(compCode);
                }
            }
        } catch (Exception e) {
            String errmsg = "Failed to parse task completion message";
            logger.severeException(errmsg, e);
            throw new ActivityException(-1, errmsg, e);
        }
    }

    /**
     * This method is used to extract data from the message received from the task manager.
     * The method updates all variables specified as non-readonly
     *
     * @param datadoc
     * @return completion code; when it returns null, the completion
     *   code is taken from the completionCode parameter of
     *   the message with key FormDataDocument.ATTR_ACTION
     * @throws ActivityException
     * @throws JSONException
     */
    protected String extractFormData(JSONObject datadoc)
            throws ActivityException, JSONException {
        String varstring = this.getAttributeValue(TaskActivity.ATTRIBUTE_TASK_VARIABLES);
        List<String[]> parsed = StringHelper.parseTable(varstring, ',', ';', 5);
        for (String[] one : parsed) {
            String varname = one[0];
            String displayOption = one[2];
            if (displayOption.equals(TaskActivity.VARIABLE_DISPLAY_NOTDISPLAYED)) continue;
            if (displayOption.equals(TaskActivity.VARIABLE_DISPLAY_READONLY)) continue;
            if (varname.startsWith("#{") || varname.startsWith("${")) continue;
            String data = datadoc.has(varname) ? datadoc.getString(varname) : null;
            setDataToVariable(varname, data);
        }
        return null;
    }

    protected void setDataToVariable(String datapath, String value)
            throws ActivityException {
        if (value == null || value.length() == 0)
            return;
        // w/o above, hit oracle constraints that variable value must not be
        // null
        // shall we consider removing that constraint? and shall we check
        // if the variable should be updated?
        String pType = this.getParameterType(datapath);
        if (pType == null)
            return; // ignore data that is not a variable
        if (VariableTranslator.isDocumentReferenceVariable(getPackage(), pType))
            this.setParameterValueAsDocument(datapath, pType, value);
        else
            this.setParameterValue(datapath, value);
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
    protected void processOtherMessage(String messageString)
        throws ActivityException {
    }

}
