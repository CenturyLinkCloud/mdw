package com.centurylink.mdw.model;

import org.json.JSONObject;

import java.util.*;

/**
 * Key ordering is as inserted.
 */
public class LinkedJsonObject extends JSONObject {

    private LinkedHashMap<String,?> backingMap;

    public LinkedJsonObject(LinkedHashMap<String,?> backingMap) {
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


}
