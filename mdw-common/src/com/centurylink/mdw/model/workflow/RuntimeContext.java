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

import com.centurylink.mdw.model.Jsonable;

import java.util.Map;

public interface RuntimeContext {

    Package getPackage();

    Process getProcess();

    String getMasterRequestId();

    Map<String,String> getAttributes();

    Map<String,Object> getVariables();

    Long getProcessId();

    Long getProcessInstanceId();

    void logInfo(String message);

    void logDebug(String message);

    void logWarn(String message);

    void logSevere(String message);

    void logException(String msg, Exception e);

    boolean isLogInfoEnabled();

    boolean isLogDebugEnabled();

    String getMdwHubUrl();

    String getMdwVersion();

    Object evaluate(String expression);
    String evaluateToString(String expression);

    String getAttribute(String name);
}
