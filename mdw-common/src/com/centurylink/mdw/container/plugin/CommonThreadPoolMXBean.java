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
package com.centurylink.mdw.container.plugin;

public interface CommonThreadPoolMXBean {

    public void start();

    public void stop();

    public boolean hasAvailableThread(String workerName);

    public void pause();

    public void resume();

    public boolean isPaused();

    public String currentStatus();

    public int getCurrentThreadPoolSize();

    public int getCoreThreadPoolSize();

    public int getMaxThreadPoolSize();

    public int getActiveThreadCount();

    public int getCurrentQueueSize();

    public long getTaskCount();

    public long getCompletedTaskCount();

    public String workerInfo();

    public String defaultWorkerInfo();

}
