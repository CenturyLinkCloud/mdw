package com.centurylink.mdw.model;

import org.json.JSONObject;

/**
 * Dummy implementation.
 */
public class JsonableImpl implements Jsonable {
    private final JSONObject json;
    public JsonableImpl(JSONObject json) {
        this.json = json;
    }
    @Override
    public JSONObject getJson() {
        return json;
    }
}
