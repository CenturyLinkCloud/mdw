package com.centurylink.mdw.model;

import java.io.Serializable;
import java.util.Date;

import org.json.JSONObject;

public class Comment implements Serializable, Jsonable, Comparable<Comment> {

    private Long id;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    /**
     * May contain markdown and/or emojis.
     */
    private String content;
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

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

    public Comment() {
    }

    public Comment(JSONObject json) {
        bind(json);
    }

    public int compareTo(Comment other) {
        return this.getModified().compareTo(other.getModified());
    }
}
