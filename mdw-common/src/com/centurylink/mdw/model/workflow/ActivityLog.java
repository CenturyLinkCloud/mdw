package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.util.log.LogLine;

import java.util.ArrayList;
import java.util.List;

public class ActivityLog implements Jsonable {

    private Long processInstanceId;
    public Long getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(Long processInstanceId) { this.processInstanceId = processInstanceId; }

    private Long activityInstanceId;
    public Long getActivityInstanceId() { return activityInstanceId; }
    public void setActivityInstanceId(Long activityInstanceId) { this.activityInstanceId = activityInstanceId; }

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

    private List<LogLine> logLines = new ArrayList<>();
    public List<LogLine> getLogLines() { return logLines; }
    public void setLogLines(List<LogLine> logLines) { this.logLines = logLines; }

    public ActivityLog(Long processInstanceId, Long activityInstanceId) {
        this.processInstanceId = processInstanceId;
        this.activityInstanceId = activityInstanceId;
    }

}
