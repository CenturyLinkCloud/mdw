/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import com.centurylink.mdw.designer.runtime.ProcessInstancePage;

public class ProcessInstanceUpdater
        extends com.centurylink.mdw.designer.testing.ProcessInstanceUpdater {
    private Long processId;

    public Long getProcessId() {
        return processId;
    }

    private Long processInstanceId;

    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    private String subtype;

    public String getSubType() {
        return subtype;
    }

    private String time;

    public String getTime() {
        return time;
    }

    private String id;

    public String getId() {
        return id;
    }

    private String msg;

    public String getMsg() {
        return msg;
    }

    public ProcessInstanceUpdater(Long processId, Long processInstanceId,
            ProcessInstancePage processInstancePage, String subtype, String time, String id,
            String msg) {
        super(processInstancePage);
        this.processId = processId;
        this.processInstanceId = processInstanceId;
        this.subtype = subtype;
        this.time = time;
        this.id = id;
        this.msg = msg;
    }

    public void handleMessage() {
        if (getProcessInstancePage().getProcessInstance() != null)
            super.handleMessage(processId, processInstanceId, subtype, time, id, msg);
    }

    protected boolean isShowingThisInstance(Long mainProcInstId) {
        return true; // instance is opened in LogWatcher
    }

    protected void showInstance(ProcessInstancePage procInstPage) {
        // won't be called
    }

    public String toString() {
        return "proc=" + processId + " procInst=" + processInstanceId + " " + subtype + " " + time
                + " " + id + " " + msg;
    }

}
