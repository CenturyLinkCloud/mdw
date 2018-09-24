/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.monitor;

import java.util.Map;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.task.TaskRuntimeContext;

/**
 * Activity monitors can be registered through @Monitor annotations to get
 * (optionally) called whenever an MDW workflow activity is invoked.
 */
public interface TaskMonitor extends RegisteredService, Monitor {

    /**
     * Called when a task instance is created.
     * @param context the task runtime context
     * @return optional map containing new or updated values
     */
    default public Map<String,Object> onCreate(TaskRuntimeContext context) {
        return null;
    }

    /**
     * Called when a task instance is assigned.
     * @param context the task runtime context
     * @return optional map containing new or updated values
     */
    default public Map<String,Object> onAssign(TaskRuntimeContext context) {
        return null;
    }

    /**
     * Called when a task instance assumes the optional state of in-progress
     * (meaning the assignee has begun work on the task).
     * @param context the task runtime context
     * @return optional map containing new or updated values
     */
    default public Map<String,Object> onInProgress(TaskRuntimeContext context) {
        return null;
    }

    /**
     * Called when a task instance reaches alert status (scheduled
     * completion date is drawing near).
     * @param runtimeContext the task runtime context
     * @return optional map containing new or updated values
     */
    default public Map<String,Object> onAlert(TaskRuntimeContext runtimeContext) {
        return null;
    }

    /**
     * Called when a task instance reaches jeopardy status (scheduled
     * completion date has passed).
     * @param context the task runtime context
     * @return optional map containing new or updated values
     */
    default public Map<String,Object> onJeopardy(TaskRuntimeContext context) {
        return null;
    }

    /**
     * Called when a task instance is forwarded from one workgroup to another.
     * @param context the task runtime context
     * @return optional map containing new or updated values
     */
    default public Map<String,Object> onForward(TaskRuntimeContext context) {
        return null;
    }

    /**
     * Called when a task instance is completed.
     * @param context the task runtime context
     */
    default public void onComplete(TaskRuntimeContext context) {
    }

    /**
     * Called when a task instance is cancelled.
     * @param context the task runtime context
     */
    default public void onCancel(TaskRuntimeContext context) {
    }
}
