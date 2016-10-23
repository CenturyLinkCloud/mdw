/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
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

}
