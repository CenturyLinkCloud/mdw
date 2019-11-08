package com.centurylink.mdw.util.log;

import com.centurylink.mdw.model.Jsonable;

import java.time.Instant;

public class ActivityLogLine implements Jsonable {

    private Long activityInstanceId;
    public Long getActivityInstanceId() { return activityInstanceId; }

    private Instant when;
    public Instant getWhen() { return when; }

    private StandardLogger.LogLevel level;
    public StandardLogger.LogLevel getLevel() { return level; }

    public String message;
    public String getMessage() { return message; }

    public ActivityLogLine(Long activityInstanceId, Instant when, StandardLogger.LogLevel level, String message) {
        this.activityInstanceId = activityInstanceId;
        this.when = when;
        this.level = level;
        this.message = message;
    }
}
