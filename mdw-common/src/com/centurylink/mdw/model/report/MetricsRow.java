package com.centurylink.mdw.model.report;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.LinkedJsonObject;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;

/**
 * For exporting to Excel via JsonArray.
 */
public class MetricsRow implements Jsonable {

    private LinkedHashMap<String,Object> map;

    public MetricsRow(String dateTime, JSONArray values) {
        map = new LinkedHashMap<>();
        map.put("Time", dateTime);
        for (int i = 0; i < values.length(); i++) {
            JSONObject value = values.getJSONObject(i);
            map.put(value.getString("name"), value.getLong("value"));
        }
    }

    @Override
    public JSONObject getJson() {
        return new LinkedJsonObject(map);
    }
}
