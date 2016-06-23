/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.data.task;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.model.data.common.Category;

public class TaskCategory extends Category implements Jsonable, Comparable<TaskCategory> {

    public TaskCategory(Long id, String code, String name) {
        setId(id);
        setCode(code);
        setName(name);
    }

    public TaskCategory(JSONObject json) throws JSONException {
        this.setId(json.getLong("id"));
        this.setCode(json.getString("code"));
        this.setName(json.getString("name"));
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", getId());
        json.put("code", getCode());
        json.put("name", getName());
        return json;
    }

    public String getName() {
        return getDescription();
    }
    public void setName(String name) {
        setDescription(name);
    }

    public int compareTo(TaskCategory other) {
        if (this.getName() == null)
            this.setName(""); // bad data
        return this.getName().compareTo(other.getName());
    }

    public String getJsonName() {
        return "TaskCategory";
    }
}
