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
package com.centurylink.mdw.monitor;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;

import java.util.Map;

/**
 * Activity monitors can be registered through @Monitor annotations to get
 * (optionally) called whenever an MDW workflow activity is invoked.
 *
 * Activities may be dehydrated and then subsequently resumed asynchronously.
 * These methods are invoked on different instances, and instance-level members should not
 * be stored for access from lifecycle methods.
 */
public interface ActivityMonitor extends RegisteredService, Monitor {

    /**
     * Called when an activity instance is to be started.
     * @param context the activity workflow context
     * @return optional map containing new or updated process variable values
     */
    default Map<String,Object> onStart(ActivityRuntimeContext context) {
        return null;
    }

    /**
     * Non-null means bypass execution with the returned result code.
     * @return optional map with variable values to set in this activity's process.
     */
    default String onExecute(ActivityRuntimeContext context) {
        return null;
    }

    /**
     * Called when an activity instance is successfully completed.
     * @return optional map with variable values to override in this activity's process.
     */
    default Map<String,Object> onFinish(ActivityRuntimeContext context) {
        return null;
    }

    /**
     * Called when an activity instance fails due to error.
     */
    default void onError(ActivityRuntimeContext context) {
    }

    default void onSuspend(ActivityRuntimeContext context) {
    }

}
