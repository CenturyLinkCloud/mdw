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
