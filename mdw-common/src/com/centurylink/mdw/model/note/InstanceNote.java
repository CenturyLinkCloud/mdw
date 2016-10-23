/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.note;

import java.io.Serializable;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.util.StringHelper;

public class InstanceNote implements Serializable, Jsonable, Comparable<InstanceNote> {

    private String noteName;
    private String noteDetails;
    private Long id;
    private String ownerType;
    private Long ownerId;
    private Date createdDate, modifiedDate;
    private String createdBy, modifiedBy;

    public InstanceNote() {

    }

    public InstanceNote(JSONObject json) throws JSONException {
        noteName = json.getString("name");
        if (json.has("id"))
            id = json.getLong("id");
        if (json.has("details"))
            noteDetails = json.getString("details");
        if (json.has("ownerType"))
            ownerType = json.getString("ownerType");
        if (json.has("ownerId"))
            ownerId = json.getLong("ownerId");
        createdBy = json.getString("user");
        createdDate = new Date();
    }

    /**
     * Method that returns the note name
     * @return NoteName
     */
    public String getNoteName() { return noteName; }

    /**
     * Method that sets the note name
     * @param pNoteName
     *
     */
    public void setNoteName(String pNoteName) { this.noteName = pNoteName; }

    /**
     * Method that returns the note Details
     * @return NoteDetails
     */
    public String getNoteDetails() { return this.noteDetails; }

    /**
     * Method that sets the note Details
     * @param pNoteDetails
     *
     */
    public void setNoteDetails(String pNoteDetails) { this.noteDetails = pNoteDetails; }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public Date getLastModified() {
        return modifiedDate == null ? createdDate : modifiedDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public String getJsonName() { return "Note"; }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", getId());
        json.put("name", getNoteName());
        json.put("details", getNoteDetails());
        String user = getModifiedBy();
        if (user == null)
            user = getCreatedBy();
        if (user != null)
            json.put("user", user);
        Date modDate = getModifiedDate();
        if (modDate == null)
            modDate = getCreatedDate();
        if (modDate != null)
            json.put("date", StringHelper.serviceDateToString(modDate));
        return json;
    }

    public int compareTo(InstanceNote other) {
        return this.getLastModified().compareTo(other.getLastModified());
    }
}
