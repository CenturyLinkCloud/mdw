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
package com.centurylink.mdw.container;

public interface ThreadPoolProvider {

    // following are provider names
    String MDW = "MDW";

    // following are worker names
    String WORKER_ENGINE = "Engine";
    String WORKER_LISTENER = "Listener";
    String WORKER_SCHEDULER = "Scheduler";
    String WORKER_MONITOR = "Monitor";
    String WORKER_DEFAULT = "Default";

    void start();

    void stop();

    boolean hasAvailableThread(String workerName);

    boolean execute(String workerName, String assignee, Runnable command);

    String currentStatus();
}
