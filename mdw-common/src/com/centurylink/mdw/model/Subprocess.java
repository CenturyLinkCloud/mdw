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
package com.centurylink.mdw.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Execution plan subprocess.
 */
public class Subprocess {
    String logicalName;
    public String getLogicalName() { return logicalName; }
    public void setLogicalName(String name) { this.logicalName = name; }

    Long instanceId;
    public Long getInstanceId() { return instanceId; }
    public void setInstanceId(Long id) { this.instanceId = id; }

    Integer statusCode;
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer code) { this.statusCode = code; }

    Map<String,String> parameters = new HashMap<String,String>();
    public Map<String,String> getParameters() { return parameters; }
    public void setParameters(Map<String,String> params) { this.parameters = params; }
}
