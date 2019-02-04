package com.centurylink.mdw.model.report;

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONObject;

import java.time.Instant;

/**
 * Data value for a temporal unit.
 */
public class Timepoint implements Jsonable {

    public Timepoint(Instant time, Long value) {
        this.time = time;
        this.value = value;
    }

    @SuppressWarnings("unused")
    public Timepoint(JSONObject json) {
        bind(json);
    }

    private Instant time;
    public Instant getTime() { return time; }
    public void setTime(Instant time) { this.time = time; }

    public Long value;
    public Long getValue() { return value; }
    public void setValue(Long val) { this.value = val; }
}
