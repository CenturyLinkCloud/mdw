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

public interface RuntimeContext {

    public Package getPackage();

    public Process getProcess();

    public String getMasterRequestId();

    public Map<String,String> getAttributes();

    public Map<String,Object> getVariables();

    public Long getProcessId();

    public Long getProcessInstanceId();

    public void logInfo(String message);

    public void logDebug(String message);

    public void logWarn(String message);

    public void logSevere(String message);

    public void logException(String msg, Exception e);

    public boolean isLogInfoEnabled();

    public boolean isLogDebugEnabled();

    public String getMdwHubUrl();

    public String getMdwVersion();

    public Object evaluate(String expression);
    public String evaluateToString(String expression);

    public String getAttribute(String name);

}
