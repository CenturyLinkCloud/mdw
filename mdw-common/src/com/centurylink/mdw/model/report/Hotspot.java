package com.centurylink.mdw.model.report;

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONObject;

public class Hotspot implements Jsonable {

    private String id;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    private Long ms;
    public Long getMs() { return ms; }
    public void setMs(Long ms) { this.ms = ms; }

    public Hotspot(String id, Long ms) {
        this.id = id;
        this.ms = ms;
    }

    @SuppressWarnings("unused")
    public Hotspot(JSONObject json) {
        bind(json);
    }

}
