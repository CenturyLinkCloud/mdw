package com.centurylink.mdw.model.report;

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONObject;

import java.time.Instant;
import java.util.LinkedHashMap;

public class Insight implements Jsonable {

    public Insight(Instant time, LinkedHashMap<String,Integer> elements) {
        this.time = time;
        this.elements = elements;
    }

    @SuppressWarnings("unused")
    public Insight(JSONObject json) {
        bind(json);
    }

    /**
     * Temporal unit for the collected data (Day, Week, Month).
     */
    private Instant time;
    public Instant getTime() { return time; }
    public void setTime(Instant time) { this.time = time; }

    /**
     * Ordered map of data elements (eg: Status to Count).
     */
    private LinkedHashMap<String,Integer> elements;
    public LinkedHashMap<String,Integer> getElements() { return elements; }
    public void setElements(LinkedHashMap<String,Integer> elements) { this.elements = elements; }
}
