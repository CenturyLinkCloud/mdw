package com.centurylink.mdw.model;

import java.util.Date;

import org.json.JSONObject;

public class Attachment implements Jsonable, Comparable<Attachment> {

    private Long id;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private String ownerType;
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }

    private Long ownerId;
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    private Date created;
    public Date getCreated() { return created; }
    public void setCreated(Date created) { this.created = created; }

    private String createUser;
    public String getCreateUser() { return createUser; }
    public void setCreateUser(String createUser) { this.createUser = createUser; }

    private Date modified;
    public Date getModified() { return modified; }
    public void setModified(Date modified) { this.modified = modified; }

    private String modifyUser;
    public String getModifyUser() { return modifyUser; }
    public void setModifyUser(String modifyUser) { this.modifyUser = modifyUser; }

    private String location;
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    private String contentType;
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Attachment() {
    }

    public Attachment(JSONObject json) {
        bind(json);
    }

    public int compareTo(Attachment other) {
        return this.getModified().compareTo(other.getModified());
    }
}
