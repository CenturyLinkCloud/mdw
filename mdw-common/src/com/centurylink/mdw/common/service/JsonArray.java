/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A Jsonable that contains a generic JSON array.
 */
public class JsonArray implements Jsonable {

    private JSONArray array;
    public JSONArray getArray() { return array; }

    public JsonArray(JSONArray array) {
        this.array = array;
    }

    public JsonArray(List<String> values) {
        array = new JSONArray(values);
    }

    public JSONObject getJson() throws JSONException {
        JSONObject jsonObj = new JSONObject();
        // either array or values should be populated
        if (array != null)
            jsonObj.put(getJsonName(), array);
        return jsonObj;
    }

    public String getJsonName() {
        return Jsonable.GENERIC_ARRAY;
    }
}
