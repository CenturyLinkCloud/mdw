package com.centurylink.mdw.model;

import org.json.JSONObject;

import java.time.Instant;
import java.util.LinkedHashMap;

public class Insight implements Jsonable {

    @SuppressWarnings("unused")
    public Insight(JSONObject json) {
        bind(json);
    }

    public Insight(Instant time, LinkedHashMap<String,Integer> elements) {
        this.time = time;
        this.elements = elements;
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

    /**
     * Optional orthogonal value for overlaying on main elements.
     */
    private Long crossValue;
    public Long getCrossValue() { return crossValue; }
    public void setCrossValue(Long crossValue) { this.crossValue = crossValue; }
}
