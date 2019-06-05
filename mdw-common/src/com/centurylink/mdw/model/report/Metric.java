package com.centurylink.mdw.model.report;

import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.Jsonable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Does not use limberest reflection for max performance in system metrics serialization.
 */
public class Metric implements Jsonable {

    private String id;
    public String getId() { return id; }

    private String name;
    public String getName() { return name; }

    private long value;
    public long getValue() { return value; }
    public void setValue(long value) { this.value = value; }

    public Metric(JSONObject json) {
        this.id = json.getString("id");
        this.name = json.getString("name");
        if (json.has("value"))
            this.value = json.getLong("value");
    }
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
