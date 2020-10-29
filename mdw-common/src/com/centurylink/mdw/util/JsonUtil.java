package com.centurylink.mdw.util;

import com.centurylink.mdw.model.JsonArray;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.Jsonable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Helper methods and access to JSON configuration values.
 * Takes into account PaaS vs File-based configurations.
 */
public class JsonUtil {

    public static JSONObject getJson(Map<String,String> map) throws JSONException {
        if (map == null)
            return null;
        JSONObject jsonObj = new JsonObject();
        for (String key : map.keySet()) {
            String value = map.get(key);
            if (value != null)
                jsonObj.put(key, value);
        }
        return jsonObj;
    }

    public static Map<String,String> getMap(JSONObject jsonObj) throws JSONException {
        Map<String,String> map = new HashMap<>();
        String[] names =  JSONObject.getNames(jsonObj);
        if (names != null) {
            for (String name : names)
                map.put(name, jsonObj.getString(name));
        }
        return map;
    }

    public static Map<String,JSONObject> getJsonObjects(JSONObject json) throws JSONException {
        Map<String,JSONObject> objects = new HashMap<>();
        Iterator<?> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next().toString();
            objects.put(key, json.getJSONObject(key));
        }
        return objects;
    }

    public static JsonArray getJsonArray(List<? extends Jsonable> jsonables) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (Jsonable jsonable : jsonables) {
            jsonArray.put(jsonable.getJson());
        }
        return new JsonArray(jsonArray);
    }

    public static boolean isJson(File file) throws IOException {
        if (!file.isFile())
            return false;
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader reader = new InputStreamReader(fis, "UTF-8")) {
            char[] chars = new char[1];
            if (reader.read(chars) == -1)
                return false;
            return chars[0] == '{';
        }
    }
}
