package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.model.Attributes;

import java.util.Map;

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
    public Attributes getAttributes() {
        return new Attributes();
    }

    @Override
    public Map<String,Object> getValues() {
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
    public void logException(String msg, Throwable t) {
        System.err.println(msg);
        t.printStackTrace();
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

    @Override
    public String evaluateToString(String expression) {
        return expression;
    }

    public Object evaluate(String expression) {
        return expression;
    }

    public String getAttribute(String name) {
        return null;
    }

    public Long getInstanceId() { return 0L; }
}
