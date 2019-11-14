package com.centurylink.mdw.util.log;

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONException;
import org.json.JSONObject;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;

import java.time.Instant;

public class ActivityLogLine implements Jsonable {

    private Long activityInstanceId;
    public Long getActivityInstanceId() { return activityInstanceId; }
    public void setActivityInstanceId(Long activityInstanceId) { this.activityInstanceId = activityInstanceId; }

    private Instant when;
    public Instant getWhen() { return when; }
    public void setWhen(Instant when) { this.when = when; }

    private LogLevel level;
    public LogLevel getLevel() { return level; }
    public void setLevel(LogLevel level) { this.level = level; }

    private String thread;
    public String getThread() { return thread; }
    public void setThread(String thread) { this.thread = thread; }

    public String message;
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public ActivityLogLine(Long activityInstanceId, Instant when, StandardLogger.LogLevel level, String thread, String message) {
        this.activityInstanceId = activityInstanceId;
        this.when = when;
        this.level = level;
        this.thread = thread;
        this.message = message;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = Jsonable.super.getJson();
        if (activityInstanceId == 0)
            json.remove("activityInstanceId");
        return json;
    }

    @SuppressWarnings("unused")
    public ActivityLogLine(JSONObject json) {
        bind(json);
        if (activityInstanceId != null && activityInstanceId == 0)
            activityInstanceId = null;
    }
}
