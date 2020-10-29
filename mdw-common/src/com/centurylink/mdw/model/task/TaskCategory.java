package com.centurylink.mdw.model.task;

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONException;
import org.json.JSONObject;

public class TaskCategory implements Jsonable, Comparable<TaskCategory> {

    private Long id;
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    private String code;
    public String getCode() { return code; }
    public void setCode(String pCode) { this.code = pCode; }

    private String description;
    public String getDescription() { return this.description; }
    public void setDescription(String pDesc) { this.description = pDesc; }


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
        JSONObject json = create();
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
