package com.centurylink.tom.model;

import java.util.Date;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

public class Remark implements Jsonable {

    public Remark(JSONObject json) {
        bind(json);    
    }

    private String type;
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    private String text;
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    
    private String author;
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    private Date created;
    public Date getCreated() { return created; }
    public void setCreated(Date created) { this.created = created; }
}
