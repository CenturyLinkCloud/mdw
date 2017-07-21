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

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.service.data.task.TaskTemplateCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskServices;
import com.centurylink.mdw.workflow.activity.AbstractWait;

public abstract class ManualTaskActivity extends AbstractWait implements TaskActivity {

    /**
     * Creates a new task instance based on the configured template for this activity.
     */
    protected TaskInstance createTaskInstance() throws ActivityException {
        try {
            String taskTemplate = getAttributeValue(ATTRIBUTE_TASK_TEMPLATE);
            if (taskTemplate == null)
                throw new ActivityException("Missing attribute: " + ATTRIBUTE_TASK_TEMPLATE);
            String templateVersion = getAttributeValue(ATTRIBUTE_TASK_TEMPLATE_VERSION);
            AssetVersionSpec spec = new AssetVersionSpec(taskTemplate, templateVersion == null ? "0" : templateVersion);
            TaskTemplate template = TaskTemplateCache.getTaskTemplate(spec);
            if (template == null)
                throw new ActivityException("Task template not found: " + spec);
            String comments = null;
            Exception exception = (Exception) getVariableValue("exception");
            if (exception != null) {
                comments = exception.toString();
                if (exception instanceof ActivityException) {
                    ActivityRuntimeContext rc = ((ActivityException)exception).getRuntimeContext();
                    if (rc != null && rc.getProcess() != null) {
                        comments = rc.getProcess().getFullLabel() + "\n" + comments;
                    }
                }
            }
            return createTaskInstance(spec, getMasterRequestId(), getProcessInstanceId(),
                            getActivityInstanceId(), getWorkTransitionInstanceId(), comments);
        }
        catch (Exception ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    protected TaskInstance createTaskInstance(AssetVersionSpec spec, String masterRequestId, Long processInstanceId,
            Long activityInstanceId, Long transitionId, String comments) throws ServiceException, DataAccessException {

        TaskTemplate taskVO = TaskTemplateCache.getTaskTemplate(spec);
        if (taskVO == null)
            throw new DataAccessException("Task template not found: " + spec);

        TaskServices taskServices = ServiceLocator.getTaskServices();

        TaskInstance instance = taskServices.createTask(taskVO.getTaskId(), masterRequestId, processInstanceId,
                OwnerType.WORK_TRANSITION_INSTANCE, transitionId, comments);

        logger.info("Created task instance " + instance.getId() + " (" + taskVO.getTaskName() + ")");

        return instance;
    }

    protected static final String WAIT_FOR_TASK = "Wait for Task";

    public boolean needSuspend() {
        String waitForTask = this.getAttributeValue(WAIT_FOR_TASK);
        return waitForTask==null || waitForTask.equalsIgnoreCase("true");
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
                    ServiceLocator.getTaskServices().cancelTaskForActivity(getActivityInstanceId());
                }
                catch (Exception ex) {
                    logger.severeException("Failed to cancel task instance - process moves on", ex);
                }
            } else if (actInstStatus.equals(WorkStatus.STATUS_WAITING)) {
                try {
                    getEngine().createEventWaitInstance(getActivityInstanceId(), getWaitEvent(), null, true, true);
                }
                catch (Exception ex) {
                    logger.severeException("Failed to re-register task action listening", ex);
                }
                // unsolicited event listening is already registered by handleEventCompletionCode
            }
            return true;
        }
    }

    protected abstract String getWaitEvent() throws ActivityException;
    protected abstract boolean messageIsTaskAction(String message) throws ActivityException;
    protected abstract void processTaskAction(String message) throws ActivityException;

    /**
     * The resume method for ManualTaskAndEventWait is handling internal functions related to
     * task completion as well as custom events, so it is not supposed to be overriden. The method
     * is therefore declared as final. To customize handling of events, please override
     * the method {@link #processOtherMessage(String, String)}
     */
    public final boolean resume(InternalEvent event) throws ActivityException {
        // secondary owner type must be OwnerType.EXTERNAL_EVENT_INSTANCE
        String messageString = super.getMessageFromEventMessage(event);
        return resume(messageString, event.getCompletionCode());
    }

    /**
     * This method is made final for the class, as it contains internal logic handling resumption
     * of waiting. It re-register the event waits including waiting for task to complete.
     * If any event has already arrived, it processes it immediately.
     *
     * Customization should be done with the methods {@link #processOtherMessage(String, String)}
     * and {@link #registerWaitEvents()}.
     */
    public final boolean resumeWaiting(InternalEvent event) throws ActivityException {
        boolean done;
        EventWaitInstance received;
        try {
            received = getEngine().createEventWaitInstance(getActivityInstanceId(), getWaitEvent(), null, true, false);
            if (received == null)
                received = registerWaitEvents(true, true);
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
     * @param message the entire message content of the event
     */
    protected void processOtherMessage(String message) throws ActivityException {
    }

}
