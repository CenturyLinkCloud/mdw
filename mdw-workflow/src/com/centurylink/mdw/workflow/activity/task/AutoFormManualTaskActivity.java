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
package com.centurylink.mdw.workflow.activity.task;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.constant.TaskAttributeConstant;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.CallURL;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;

@Tracked(LogLevel.TRACE)
public class AutoFormManualTaskActivity extends ManualTaskActivity {

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
            TaskInstance taskInstance = createTaskInstance();
            String taskInstCorrelationId = TaskAttributeConstant.TASK_CORRELATION_ID_PREFIX + taskInstance.getId();
            super.loginfo("Task instance created - ID " + taskInstance.getId());
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

    protected String getWaitEvent() throws ActivityException {
        try {
            TaskInstance taskInst = ServiceLocator.getTaskServices().getTaskInstanceForActivity(getActivityInstanceId());
            return TaskAttributeConstant.TASK_CORRELATION_ID_PREFIX + taskInst.getTaskInstanceId();
        }
        catch (Exception ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    protected boolean messageIsTaskAction(String messageString) throws ActivityException {
        if (messageString.startsWith("{")) {
            JSONObject jsonobj;
            try {
                jsonobj = new JsonObject(messageString);
                JSONObject meta = jsonobj.has("META")?jsonobj.getJSONObject("META"):null;
                if (meta==null || !meta.has(TaskAttributeConstant.TASK_ACTION)) return false;
                String action = meta.getString(TaskAttributeConstant.TASK_ACTION);
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
            JSONObject datadoc = new JsonObject(messageString);
            String compCode = extractFormData(datadoc); // this handles both embedded proc and not
            datadoc = datadoc.getJSONObject("META");
            String action = datadoc.getString(TaskAttributeConstant.TASK_ACTION);
            CallURL callurl = new CallURL(action);
            action = callurl.getAction();
            if (compCode==null) compCode = datadoc.has(TaskAttributeConstant.URLARG_COMPLETION_CODE) ? datadoc.getString(TaskAttributeConstant.URLARG_COMPLETION_CODE) : null;
            if (compCode==null) compCode = callurl.getParameter(TaskAttributeConstant.URLARG_COMPLETION_CODE);
            String subaction = datadoc.has(TaskAttributeConstant.URLARG_ACTION) ? datadoc.getString(TaskAttributeConstant.URLARG_ACTION) : null;
            if (subaction==null) subaction = callurl.getParameter(TaskAttributeConstant.URLARG_ACTION);
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
        // w/o above, hit oracle constraints that variable value must not be null
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
}
