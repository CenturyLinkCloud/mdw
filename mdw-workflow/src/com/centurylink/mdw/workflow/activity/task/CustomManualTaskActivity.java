package com.centurylink.mdw.workflow.activity.task;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.SuspendableActivity;
import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.Parameter;
import org.apache.xmlbeans.XmlException;

@Activity(value="Custom Manual Task", category=TaskActivity.class, icon="com.centurylink.mdw.base/task.png",
        pagelet="com.centurylink.mdw.base//customTask.pagelet")
public class CustomManualTaskActivity extends ManualTaskActivity implements SuspendableActivity {

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
                if (getTaskInstance(instanceId) == null)
                    throw new ActivityException("Task instance not found: " + instanceId);
                // update secondary owner
                updateOwningTransition(instanceId);
            }

            if (needSuspend()) {
                getEngine().createEventWaitInstance(getProcessInstanceId(), getActivityInstanceId(),
                        "TaskAction-" + getActivityInstanceId(), null, true, true);
                EventWaitInstance received = registerWaitEvents(false);
                if (received != null)
                  resume(getExternalEventInstanceDetails(received.getMessageDocumentId()), received.getCompletionCode());
            }
        }
        catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);
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
            message = ActionRequestDocument.Factory.parse(messageString);
        }
        catch (XmlException e) {
            throw new ActivityException(e.getMessage(), e);
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
