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

import com.centurylink.mdw.model.workflow.RuntimeContext;

/**
 * Indicates that a monitor should be run in a separate thread so as not
 * to bog down mainstream workflow processing.  The onStart() method has
 * a return value compatible with process and activity monitors, but when
 * run as offline, the return value is ignored.
 */
public interface OfflineMonitor<T extends RuntimeContext> {

    public boolean handlesEvent(T context, String event);

    public Map<String,Object> onStart(T context);
    public Map<String,Object> onFinish(T context);
    public void onError(T context);

}
