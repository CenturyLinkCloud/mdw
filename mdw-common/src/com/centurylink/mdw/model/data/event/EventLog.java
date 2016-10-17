/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.data.event;

// CUSTOM IMPORTS -----------------------------------------------------

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.service.Jsonable;


/**
 * EventLog
 *
 * This used to be an interface for hibernate class EventLogImpl.
 * It is now converted to a VO class. We did not name it EventLogVO
 * because there are too many references to EventLog.
 *
 * @version 1.0
 */
public class EventLog implements Serializable, Jsonable {


    public static final long serialVersionUID = 1L;

    public static final String CATEGORY_BAM = "BAM";
    public static final String CATEGORY_AUDIT = "AUDIT";
    public static final String CATEGORY_EVENT_HISTORY = "EVENT_HISTORY";
    public static final String CATEGORY_SCHEDULED_JOB_HISTORY = "SCHEDULED_JOB_HISTORY";
    public static final String STANDARD_EVENT_SOURCE = "N/A";

    public static final String SUBCAT_REGISTER = "Register";
    public static final String SUBCAT_ARRIVAL = "Arrival";
    public static final String SUBCAT_DEREGISTER = "Deregister";

    private Long id;
    private String eventName;
    private String createDate;
    private String createUser;
    private String source;
    private String category;
    private String subCategory;
    private String ownerType;
    private Long ownerId;
    private String comment;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    /* (non-Javadoc)
     * @see com.centurylink.mdw.common.service.Jsonable#getJson()
     */
    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        if (eventName != null)
            json.put("eventName", eventName);
        if (createDate != null) {
            json.put("createDate", createDate);
        }
        if (createUser != null) {
            json.put("createUser", createUser);
        }
        if (source != null) {
            json.put("source", source);
        }
        if (category != null) {
            json.put("category", category);
        }
        if (subCategory != null) {
            json.put("subCategory", subCategory);
        }
        if (ownerType != null) {
            json.put("ownerType", ownerType);
        }
        if (ownerId != null) {
            json.put("ownerId", ownerId);
        }
        if (comment != null) {
            json.put("comment", comment);
        }
        return json;
    }

    /* (non-Javadoc)
     * @see com.centurylink.mdw.common.service.Jsonable#getJsonName()
     */
    @Override
    public String getJsonName() {
        return TaskAttributeConstant.TASK_HISTORY_JSONNAME;
    }

}
