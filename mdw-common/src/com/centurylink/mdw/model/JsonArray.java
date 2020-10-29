package com.centurylink.mdw.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A Jsonable that contains a generic JSON array.
 */
public class JsonArray implements Jsonable {

    public static final String GENERIC_ARRAY = "genericArray";

    private JSONArray array;
    public JSONArray getArray() { return array; }

    public JsonArray(JSONArray array) {
        this.array = array;
    }

    public JsonArray(List<? extends Jsonable> jsonables) {
        array = new JSONArray();
        for (Jsonable jsonable : jsonables) {
            array.put(jsonable.getJson());
        }
    }

    public JsonArray(Collection<String> values) {
        array = new JSONArray(values);
    }

    public List<String> getList() throws JSONException {
        List<String> list = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                list.add(array.getString(i));
            }
        }
        return list;
    }

    public JSONObject getJson() throws JSONException {
        JSONObject jsonObj = create();
        // either array or values should be populated
        if (array != null)
            jsonObj.put(getJsonName(), array);
        return jsonObj;
    }

    public String getJsonName() {
        return GENERIC_ARRAY;
    }
}
