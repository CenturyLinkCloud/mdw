package com.centurylink.mdw.demo.bugs;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

public class Bug implements Jsonable {
    
    private Long id;
    public Long getId() { return id; }
    
    private String title;
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    private String description;
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Bug(JSONObject json) {
        bind(json);
    }
    
    
}
