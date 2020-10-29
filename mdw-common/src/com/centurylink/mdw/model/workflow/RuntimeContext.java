package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.model.Attributes;
import com.centurylink.mdw.model.Jsonable;

import java.util.Map;

public interface RuntimeContext {

    Package getPackage();

    Process getProcess();

    String getMasterRequestId();

    Attributes getAttributes();

    Map<String,Object> getValues();

    Long getProcessId();

    Long getProcessInstanceId();

    void logInfo(String message);

    void logDebug(String message);

    void logWarn(String message);

    void logSevere(String message);

    void logException(String msg, Throwable t);

    boolean isLogInfoEnabled();

    boolean isLogDebugEnabled();

    String getMdwHubUrl();

    String getMdwVersion();

    Object evaluate(String expression);
    String evaluateToString(String expression);

    String getAttribute(String name);

    Long getInstanceId();
}
