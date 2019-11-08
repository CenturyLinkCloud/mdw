package com.centurylink.mdw.util.log;

import com.centurylink.mdw.model.Jsonable;

import java.util.ArrayList;
import java.util.List;

public class ActivityLog implements Jsonable {

    private Long processInstanceId;
    public Long getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(Long processInstanceId) { this.processInstanceId = processInstanceId; }

    /**
     * Seconds.
     */
    private long dbZoneOffset;
    public long getDbZoneOffset() { return dbZoneOffset; }
    public void setDbZoneOffset(long dbZoneOffset) { this.dbZoneOffset = dbZoneOffset; }

    /**
     * Seconds.
     */
    private long serverZoneOffset;
    public long getServerZoneOffset() { return serverZoneOffset; }
    public void setServerZoneOffset(long serverZoneOffset) { this.serverZoneOffset = serverZoneOffset; }

    private List<ActivityLogLine> logLines = new ArrayList<>();
    public List<ActivityLogLine> getLogLines() { return logLines; }
    public void setLogLines(List<ActivityLogLine> logLines) { this.logLines = logLines; }

    public ActivityLog(Long processInstanceId) {
        this.processInstanceId = processInstanceId;
    }
}
