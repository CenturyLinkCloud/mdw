package com.centurylink.mdw.util.log;

import com.centurylink.mdw.model.Jsonable;

import java.time.Instant;

public class LogLine implements Jsonable {

    private Instant when;
    public Instant getWhen() { return when; }

    private StandardLogger.LogLevel level;
    public StandardLogger.LogLevel getLevel() { return level; }

    public String message;
    public String getMessage() { return message; }

    public LogLine(Instant when, StandardLogger.LogLevel level, String message) {
        this.when = when;
        this.level = level;
        this.message = message;
    }
}
