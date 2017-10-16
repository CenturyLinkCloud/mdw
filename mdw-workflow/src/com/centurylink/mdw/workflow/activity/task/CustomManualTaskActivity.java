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

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.SuspendibleActivity;
import com.centurylink.mdw.app.Compatibility;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class CustomManualTaskActivity extends ManualTaskActivity implements SuspendibleActivity {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    /**
     * Creates a task instance unless the INSTANCE_ID_VAR attribute points
     * to a pre-existing instance.  If the attribute is populated but the variable
     * value is null, the variable will be set to the newly-created instanceId.
     */
    @Override
    public void execute() throws ActivityException {
        Long instanceId = null;  // pre-existing instanceId
        String instanceIdSpec = getInstanceIdVariable();
        if (instanceIdSpec != null) {
            Object value = getValue(instanceIdSpec);
            if (value instanceof Long)
                instanceId = (Long) value;
            else if (value != null)
                instanceId = Long.parseLong(value.toString());
        }
        try {
            if (instanceId == null) {
                TaskInstance taskInstance = createTaskInstance();
                instanceId = taskInstance.getTaskInstanceId();
                if (instanceIdSpec != null)
                    setValue(instanceIdSpec, instanceId);
            }
            else {
                // update secondary owner
                updateOwningTransition(instanceId);
            }

            if (needSuspend()) {
                getEngine().createEventWaitInstance(getActivityInstanceId(), "TaskAction-" + getActivityInstanceId(), null, true, true);
                EventWaitInstance received = registerWaitEvents(false,true);
                if (received != null)
                  resume(getExternalEventInstanceDetails(received.getMessageDocumentId()), received.getCompletionCode());
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException(-1, ex.getMessage(), ex);
        }
    }

    protected boolean messageIsTaskAction(String messageString) {
      return messageString.indexOf("ActionRequest") > 0 &&
              messageString.indexOf("TaskAction") > 0;
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
        Process proc = getProcessDefinition();
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
        this.setReturnCode(compCode);
        // old code set activity instance to CANCELED when task action is
        // Cancel; shall we do that?
    }

    protected String getWaitEvent() {
        return "TaskAction-" + getActivityInstanceId();
    }
}
