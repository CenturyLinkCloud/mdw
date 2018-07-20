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

import java.util.Map;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;

/**
 * Activity monitors can be registered through @RegisteredService annotations to get
 * called whenever an MDW workflow activity is invoked.
 */
public interface ActivityMonitor extends RegisteredService {

    /**
     * Called when an activity instance is to be started.
     * @param runtimeContext the activity workflow context
     * @return optional map containing new or updated process variable values
     * TODO: return type void (variables should be passed and set in runtimeContext)
     */
    public Map<String,Object> onStart(ActivityRuntimeContext runtimeContext);

    /**
     * Non-null means bypass execution with the returned result code.
     * @return optional map with variable values to set in this activity's process.
     */
    public String onExecute(ActivityRuntimeContext runtimeContext);

    /**
     * Called when an activity instance is successfully completed.
     * @return optional map with variable values to override in this activity's process.
     */
    public Map<String,Object> onFinish(ActivityRuntimeContext runtimeContext);

    /**
     * Called when an activity instance fails due to error.
     * TODO: make exception available
     */
    public void onError(ActivityRuntimeContext runtimeContext);

}
