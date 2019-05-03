/*
 * Copyright (C) 2019 CenturyLink, Inc.
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
package com.centurylink.mdw.model.monitor;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.util.CallURL;

@FunctionalInterface
public interface ScheduledJob extends RegisteredService {

    void run(CallURL args);

    /**
     * Implement this for asynchronous jobs that you want to run exclusively
     * (no overlap between scheduled executions, even if one runs long).
     * If you implement this with annotation attribute "isExclusive=true" and your job runs
     * asynchronously, you MUST invoke the callback to indicate the job is complete.
     * Otherwise the job will be deemed to be always running, and subsequent executions
     * will never occur.
     */
    default void run(CallURL args, JobCompletionCallback callback) {
        run(args);
        callback.onComplete(0);
    }
}
