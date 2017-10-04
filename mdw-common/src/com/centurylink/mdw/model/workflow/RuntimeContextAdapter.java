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
package com.centurylink.mdw.model.workflow;

import java.util.Map;

import com.centurylink.mdw.app.ApplicationContext;

public class RuntimeContextAdapter implements RuntimeContext {

    @Override
    public Package getPackage() {
        return null;
    }

    @Override
    public Process getProcess() {
        return null;
    }

    @Override
    public String getMasterRequestId() {
        return null;
    }

    @Override
    public Map<String,String> getAttributes() {
        return null;
    }

    @Override
    public Map<String,Object> getVariables() {
        return null;
    }

    @Override
    public Long getProcessId() {
        return null;
    }

    @Override
    public Long getProcessInstanceId() {
        return null;
    }

    @Override
    public void logInfo(String message) {
        if (isLogInfoEnabled())
            System.out.println(message);
    }

    @Override
    public void logDebug(String message) {
        if (isLogDebugEnabled())
            System.out.println(message);
    }

    @Override
    public void logWarn(String message) {
        System.out.println(message);
    }

    @Override
    public void logSevere(String message) {
        System.err.println(message);
    }

    @Override
    public void logException(String msg, Exception e) {
        System.err.println(msg);
        e.printStackTrace();
    }

    @Override
    public boolean isLogInfoEnabled() {
        return true;
    }

    @Override
    public boolean isLogDebugEnabled() {
        return false;
    }

    @Override
    public String getMdwHubUrl() {
        return ApplicationContext.getMdwHubUrl();
    }

    @Override
    public String getMdwVersion() {
        return ApplicationContext.getMdwVersion();
    }
}
