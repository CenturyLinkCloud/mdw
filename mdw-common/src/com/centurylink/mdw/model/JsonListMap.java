package com.centurylink.mdw.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Used to make aggregate values Jsonable.
 */
public class JsonListMap<T extends Jsonable> implements Jsonable {

    private LinkedHashMap<String,List<T>> jsonables;
    public LinkedHashMap<String,List<T>> getJsonables() { return jsonables; }

    public JsonListMap(LinkedHashMap<String,List<T>> jsonables) {
        this.jsonables = jsonables;
    }

    @SuppressWarnings("unused")
    public JsonListMap(String name, LinkedHashMap<String,List<T>> jsonables) {
        this.name = name;
        this.jsonables = jsonables;
    }

    public JsonListMap(JSONObject json, Class<T> type) throws JSONException {
        this.jsonables = new LinkedHashMap<>();
        for (String name : JSONObject.getNames(json)) {
            JSONArray jsonArr = json.getJSONArray(name);
            List<T> list = new ArrayList<>();
            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject jsonObj = jsonArr.getJSONObject(i);
                try {
                    Constructor<T> c = type.getConstructor(JSONObject.class);
                    T jsonable = c.newInstance(jsonObj);
                    list.add(jsonable);
                }
                catch (Exception ex) {
                    throw new JSONException(ex);
                }
            }
            jsonables.put(name, list);
        }
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JsonObject() {
            public Set<String> keySet() {
                return jsonables.keySet();
            }
            protected Set<Map.Entry<String, Object>> entrySet() {
                Set<Map.Entry<String,Object>> entries = new LinkedHashSet<>();
                for (String key : keySet()) {
                    entries.add(new AbstractMap.SimpleEntry(key, get(key)));
                }
                return entries;
            }
        };
        for (String key : jsonables.keySet()) {
            JSONArray jsonArr = new JSONArray();
            for (Jsonable jsonable : jsonables.get(key)) {
                jsonArr.put(jsonable.getJson());
            }
            json.put(key, jsonArr);
        }
        return json;
    }

    private String name;
    public String getJsonName() {
        return name;
    }
}
