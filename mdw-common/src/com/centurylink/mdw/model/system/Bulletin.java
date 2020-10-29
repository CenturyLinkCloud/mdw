package com.centurylink.mdw.model.system;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.system.SystemMessage.Level;

public class Bulletin implements Jsonable {

    public enum Signal {
        On,
        Off
    }

    public Bulletin(JSONObject json) {
        bind(json);
    }

    public Bulletin(String message) {
        this(new SystemMessage(Level.Info, message));
    }

    public Bulletin(Level level, String message) {
        this(new SystemMessage(level, message));
    }

    public Bulletin(SystemMessage systemMessage) {
        this.message = systemMessage;
        this.signal = Signal.On;
        this.id = Integer.toHexString(systemMessage.hashCode());
    }

    public Bulletin on() {
        this.signal = Signal.On;
        return this;
    }

    public Bulletin off() {
        return off(null);
    }

    public Bulletin off(String message) {
        this.message.setMessage(message);
        this.signal = Signal.Off;
        return this;
    }

    public Bulletin level(Level level) {
        this.message.setLevel(level);
        return this;
    }

    private SystemMessage message;
    public SystemMessage getMessage() { return message; }
    public void setMessage(SystemMessage message) { this.message = message; }

    private Signal signal;
    public Signal getSignal() { return signal; }
    public void setSignal(Signal signal) { this.signal = signal; }

    /**
     * Id is for correlating.
     */
    private String id;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
