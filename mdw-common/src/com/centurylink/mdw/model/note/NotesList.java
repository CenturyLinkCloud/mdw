/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.note;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.InstanceList;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.util.StringHelper;

public class NotesList implements InstanceList<InstanceNote>, Jsonable {

    public NotesList(String name, String json) throws JSONException, ParseException {
        this.name = name;
        JSONObject jsonObj = new JSONObject(json);
        if (jsonObj.has("ownerType"))
            ownerType = jsonObj.getString("ownerType");
        if (jsonObj.has("ownerId"))
            ownerId = jsonObj.getLong("ownerId");
        if (jsonObj.has("retrieveDate"))
            retrieveDate = StringHelper.serviceStringToDate(jsonObj.getString("retrieveDate"));
        if (jsonObj.has("count"))
            count = jsonObj.getInt("count");
        if (jsonObj.has(name)) {
            JSONArray notesList = jsonObj.getJSONArray(name);
            for (int i = 0; i < notesList.length(); i++)
                notes.add(new InstanceNote((JSONObject)notesList.get(i)));
        }
    }

    public NotesList(String name, String ownerType, Long ownerId, List<InstanceNote> notes) {
        this.name = name;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.notes = notes;
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("retrieveDate", StringHelper.serviceDateToString(getRetrieveDate()));
        json.put("count", count);
        if (ownerType != null)
            json.put("ownerType", ownerType);
        if (ownerId != null)
            json.put("ownerId", ownerId);
        JSONArray array = new JSONArray();
        if (notes != null) {
            for (InstanceNote note : notes)
                array.put(note.getJson());
        }
        json.put(name, array);
        return json;
    }

    public String getJsonName() {
        return name;
    }

    private String name;
    public String getName() { return name;}
    public void setName(String name) { this.name = name; }

    private String ownerType;
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }

    private Long ownerId;
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    private Date retrieveDate;
    public Date getRetrieveDate() { return retrieveDate; }
    public void setRetrieveDate(Date d) { this.retrieveDate = d; }

    private int count;
    public int getCount() { return count; }
    public void setCount(int ct) { this.count = ct; }

    public long getTotal() { return count; } // no pagination

    private List<InstanceNote> notes = new ArrayList<InstanceNote>();
    public List<InstanceNote> getNotes() { return notes; }
    public void setNotes(List<InstanceNote> notes) { this.notes = notes; }

    public List<InstanceNote> getItems() {
        return notes;
    }

    public void addNote(InstanceNote note) {
        notes.add(note);
    }

    public int getIndex(Long noteId) {
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).getId().equals(noteId))
                return i;
        }
        return -1;
    }

    public int getIndex(String id) {
        return (getIndex(Long.parseLong(id)));
    }
}
