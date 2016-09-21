/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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
