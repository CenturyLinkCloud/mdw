package com.centurylink.mdw.model.system;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

public class SystemMessage implements Jsonable {

    public static SystemMessage info(String message) {
        return new SystemMessage(Level.Info, message);
    }

    public static SystemMessage error(String message) {
        return new SystemMessage(Level.Error, message);
    }

    public enum Level {
        Info,
        Error
    }

    public SystemMessage(JSONObject json) {
        bind(json);
    }

    public SystemMessage(Level level, String message) {
        this.level = level;
        this.message = message;
    }

    private Level level;
    public Level getLevel() { return level; }
    public void setLevel(Level level) { this.level = level; }

    private String message;
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String toString() {
        return level + ": " + message;
    }
}
