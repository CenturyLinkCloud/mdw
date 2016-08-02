package com.centurylink.mdw.designer.testing;

import groovy.lang.GroovyObjectSupport;

public class TestCaseEvent extends GroovyObjectSupport {

    private String id;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    private String message;
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    private String processName;
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }

    private String activityLogicalId;
    public String getActivityLogicalId() { return activityLogicalId; }
    public void setActivityLogicalId(String logicalId) { this.activityLogicalId = logicalId; }

    public TestCaseEvent(String id) {
        this.id = id;
    }

}