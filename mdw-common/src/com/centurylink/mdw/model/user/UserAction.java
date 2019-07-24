/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.model.user;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.util.DateHelper;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Date;

@ApiModel(value="UserAction", description="MDW user action")
public class UserAction implements Serializable, Comparable<UserAction>, Jsonable {

    public enum Action {
        Create,
        Change,
        Delete,
        Retry,
        Proceed,
        Resume,
        Refresh,
        Rename,
        Import,
        Export,
        Complete,
        Abort,
        Assign,
        Claim,
        Release,
        Cancel,
        Forward,
        Work,
        Trigger,
        Send,
        Run,
        Alert,
        Jeopardy,
        Hold,
        Ping,
        Version,
        Collaborate,
        Read,
        Fail,
        Other
    }

    public enum Entity {
        App,
        Project,
        Package,
        Process,
        Activity,
        Transition,
        Variable,
        Document,
        ActivityImplementor,
        ExternalEvent,
        Cache,
        Element,
        Attribute,
        AttributeHolder,
        Folder,
        File,
        TestCase,
        ProcessInstance,
        ActivityInstance,
        VariableInstance,
        Task,
        TaskTemplate,
        TaskInstance,
        Property,
        User,
        Workgroup,
        Asset,
        Role,
        Rule,
        Category,
        Event,
        Comment,
        Attachment,
        Message,
        Solution,
        BaseData,
        Value,
        ValueHolder,
        Request,
        Discussion,
        Other
    }

    private Long id;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    private Action action;
    public Action getAction() { return action; }
    public void setAction(Action action) { this.action = action; }
    @ApiModelProperty(hidden=true)
    public void setAction(String action) {
        this.action = getAction(action);
    }

    // for non-standard actions
    private String extendedAction;
    public String getExtendedAction() { return extendedAction; }
    public void setExtendedAction(String extendedAction) { this.extendedAction = extendedAction; }

    private Entity entity;
    public Entity getEntity() { return entity; }
    public void setEntity(Entity entity) { this.entity = entity; }

    private Long entityId;
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long id) { this.entityId = id; }

    private String user;
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    private String destination;
    public String getDestination() { return destination; }
    public void setDestination(String dest) { this.destination = dest; }

    private Date date;
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    private Date retrieveDate;
    public Date getRetrieveDate() { return retrieveDate; }
    public void setRetrieveDate(Date retrieveDate) { this.retrieveDate = retrieveDate; }

    private String source;
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    private String description;
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UserAction() {

    }

    public UserAction(JSONObject json) throws JSONException {
        fromJson(json);
    }

    public UserAction(String user, Action action, Entity entity, Long entityId, String description) {
        this.user = user;
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.description = description;
    }

    public UserAction(Action action, String user, Entity entity, Long entityId) {
        this(user, action, entity, entityId, null);
    }

    public static Action getAction(String actionName) {
        try {
            return Action.valueOf(actionName);
        }
        catch (IllegalArgumentException ex) {
            return Action.Other;
        }
        catch (NullPointerException ex) {
            return null;
        }
    }

    public static Entity getEntity(String entityName) {
        try {
            return Entity.valueOf(entityName);
        }
        catch (IllegalArgumentException ex) {
            return Entity.Other;
        }
        catch (NullPointerException ex) {
            return null;
        }
    }

    public static Action[] NOTIFIABLE_TASK_ACTIONS
      = { Action.Create,
          Action.Assign,
          Action.Abort,
          Action.Complete,
          Action.Cancel,
          Action.Work,
          Action.Alert,
          Action.Jeopardy,
          Action.Hold
        };

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        if (id != null)
          json.put("id", id);
        json.put("name", (action.equals(Action.Other) ? extendedAction : action));
        json.put("date", DateHelper.dateToString(date));
        json.put("description", description);
        if (retrieveDate != null)
            json.put("retrieveDate", DateHelper.dateToString(retrieveDate));
        json.put("user", user);
        json.put("source", source);
        if (destination != null)
          json.put("destination", destination);
        JSONObject entityJson = create();
        if (entityId != null)
          entityJson.put("id", entityId);
        entityJson.put("name", entity);
        json.put("entity", entityJson);
        return json;
    }

    public JSONObject getHistoryJson() throws JSONException {
        JSONObject json = create();
        if (id != null)
          json.put("id", id);
        json.put("name", (action.equals(Action.Other) ? extendedAction : action));
        json.put("date", DateHelper.dateToString(date));
        json.put("description", description);
        if (retrieveDate != null)
            json.put("retrieveDate", DateHelper.dateToString(retrieveDate));
        json.put("user", user);
        json.put("source", source);
        if (destination != null)
            json.put("destination", destination);
        if (entityId != null)
            json.put("EntityId", entityId);
        if (entity != null)
            json.put("EntityName", entity);
        return json;
    }

    protected void fromJson(JSONObject json) throws JSONException {
        if (json.has("id")) {
          try {
              id = new Long(json.getString("id"));
          }
          catch (NumberFormatException ex) {
          }
        }
        if (json.has("name"))
            action = getAction(json.getString("name"));
        if (json.has("date"))
            date = DateHelper.stringToDate(json.getString("date"));
        if (json.has("description"))
            description = json.getString("description");
        if (json.has("retrieveDate"))
            retrieveDate = DateHelper.stringToDate(json.getString("retrieveDate"));
        if (json.has("user"))
            user = json.getString("user");
        if (json.has("destination"))
            destination = json.getString("destination");
        if (json.has("entity")) {
            JSONObject entityJson = json.getJSONObject("entity");
            if (entityJson.has("id")) {
                try {
                    entityId = entityJson.getLong("id");
                }
                catch (NumberFormatException ex) {
                }
            }
            if (entityJson.has("name")) {
                entity = getEntity(entityJson.getString("name"));
            }
        }
        if (json.has("source")) {
            source = json.getString("source");
        }
    }

    public String getJsonName() {
        return "userAction";
    }

    @Override
    public int compareTo(UserAction o) {
        return id.compareTo(o.getId());
    }
}
