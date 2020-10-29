package com.centurylink.mdw.model;

import org.json.JSONObject;

import java.util.List;

public class JsonList<E extends Jsonable> extends io.limberest.json.JsonList<E> implements Jsonable {

    public JsonList(List<E> list, String jsonName) {
        super(list, jsonName);
    }

    public JsonList(JSONObject json, Class<E> type) {
        super(json, type);
    }

    public JSONObject getJson() {
        return toJson();
    }
}
