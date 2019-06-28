package com.centurylink.mdw.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * Key ordering is as inserted or per constructor map.
 */
public class LinkedJsonObject extends JSONObject {

    private LinkedHashMap<String,Object> backingMap;

    public LinkedJsonObject() {
        this.backingMap = new LinkedHashMap<>();
    }

    public LinkedJsonObject(String json) {
        super(json);
    }

    public LinkedJsonObject(LinkedHashMap<String,Object> backingMap) {
        super(backingMap);
        this.backingMap = backingMap;
    }

    public Set<String> keySet() {
        return backingMap.keySet();
    }
    protected Set<Map.Entry<String, Object>> entrySet() {
        Set<Map.Entry<String,Object>> entries = new LinkedHashSet<>();
        for (String key : keySet()) {
            entries.add(new AbstractMap.SimpleEntry(key, get(key)));
        }
        return entries;
    }

    @Override
    public int length() {
        return this.backingMap.size();
    }

    @Override
    public JSONArray names() {
        if(this.backingMap.isEmpty()) {
            return null;
        }
        return new JSONArray(this.backingMap.keySet());
    }

    @Override
    public Object opt(String key) {
        return key == null ? null : this.backingMap.get(key);
    }

    @Override
    public JSONObject put(String key, Object value) throws JSONException {
        if (key == null) {
            throw new NullPointerException("Null key.");
        }
        if (value != null) {
            testValidity(value);
            this.backingMap.put(key, value);
        }
        else {
            this.remove(key);
        }
        return this;
    }

    @Override
    public Object remove(String key) {
        return this.backingMap.remove(key);
    }

    @Override
    public boolean has(String key) {
        return this.backingMap.containsKey(key);
    }
}
