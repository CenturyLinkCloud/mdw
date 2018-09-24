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
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;

/**
 * Process monitors can be registered through @Monitor annotations to get
 * notified during the lifecycle of an MDW workflow process.
 */
public interface ProcessMonitor extends RegisteredService, Monitor {

    /**
     * Return map contains variable values to be populated on the process instance.
     * Variables should be defined in the process as INPUT mode.
     * Return null if no variable updates are required.
     */
    default public Map<String,Object> onStart(ProcessRuntimeContext context) {
        return null;
    }

    /**
     * Invoked on process completion.
     * Return map is currently not used.
     */
    default public Map<String,Object> onFinish(ProcessRuntimeContext context) {
        return null;
    }

}
