package com.centurylink.mdw.model.report;

import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.Jsonable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Does not use limberest reflection for max performance in system metrics serialization.
 */
public class Metric implements Jsonable {

    private String name;
    public String getName() { return name; }

    private String id;
    public String getId() { return id; }

    public long value;
    public long getValue() { return value; }
    public void setValue(long value) { this.value = value; }

    public Metric(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Metric(String id, String name, long value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = new JsonObject();
        json.put("name", name);
        json.put("id", id);
        json.put("value", value);
        return json;
    }
}
