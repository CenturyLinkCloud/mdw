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

    public long value;
    public long getValue() { return value; }

    public Metric(JSONObject json) {
        this.name = json.getString("name");
        this.value = json.getLong("value");
    }

    public Metric(String name, long value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = new JsonObject();
        json.put("name", name);
        json.put("value", value);
        return json;
    }
}
