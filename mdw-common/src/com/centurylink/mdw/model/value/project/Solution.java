/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.project;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.requests.Request;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;

/**
 * TODO: members and values
 */
public class Solution implements Jsonable {

    public enum MemberType {
        MasterRequest,
        TaskInstance,
        ProcessInstance,
        Solution,
        Other
    }

    private static Map<MemberType,String> memberListNames = new HashMap<MemberType,String>();
    static {
        memberListNames.put(MemberType.MasterRequest, "requests");
        memberListNames.put(MemberType.TaskInstance, "tasks");
        memberListNames.put(MemberType.ProcessInstance, "processes");
        memberListNames.put(MemberType.Solution, "solutions");
        memberListNames.put(MemberType.Other, "other");
    }
    public static String getMemberListName(MemberType type) {
        return memberListNames.get(type);
    }
    public static MemberType getMemberType(String listName) {
        for (MemberType type : MemberType.values()) {
            if (memberListNames.get(type).equals(listName))
                return type;
        }
        return null;
    }

    /**
     * Solution ID is the db sequence number.
     */
    private Long solutionId;
    public Long getSolutionId() { return solutionId; }
    public void setSolutionId(Long solutionId) { this.solutionId = solutionId; }

    /**
     * ID is the user-friendly unique ID.
     */
    private String id;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private String description;
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    private String ownerType;
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }

    private String ownerId;
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    private Date created;
    public Date getCreated() { return created; }
    public void setCreated(Date created) { this.created = created; }

    private String createdBy;
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    private Date modified;
    public Date getModified() { return modified; }
    public void setModified(Date modified) { this.modified = modified; }

    private String modifiedBy;
    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }

    private Map<MemberType,List<Jsonable>> members;
    public Map<MemberType,List<Jsonable>> getMembers() { return members; }
    public void setMembers(Map<MemberType,List<Jsonable>> members) { this.members = members; }

    private Map<String,String> values;
    public Map<String,String> getValues() { return values; }
    public void setValues(Map<String,String> values) { this.values = values; }

    public Solution(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Solution(Long solutionId, String id, String name, String ownerType, String ownerId, Date created, String createdBy) {
        this.solutionId = solutionId;
        this.id = id;
        this.name = name;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.created = created;
        this.createdBy = createdBy;
    }

    public Solution(JSONObject json) throws JSONException {
        id = json.getString("id");
        name = json.getString("name");
        if (json.has("ownerType"))
            ownerType = json.getString("ownerType");
        if (json.has("ownerId"))
            ownerId = json.getString("ownerId");
        if (json.has("description"))
            description = json.getString("description");
        if (json.has("members")) {
            JSONObject mems = json.getJSONObject("members");
            members = new HashMap<MemberType,List<Jsonable>>();
            String[] memberTypeListNames = JSONObject.getNames(mems);
            if (memberTypeListNames != null) {
                for (int i = 0; i < memberTypeListNames.length; i++) {
                    String memberTypeListName = memberTypeListNames[i];
                    MemberType memberType = getMemberType(memberTypeListName);
                    JSONArray memsArray = (JSONArray)mems.get(memberTypeListName);
                    List<Jsonable> membersList = getMembersList(memberType, memsArray);
                    members.put(memberType, membersList);
                }
            }
        }
        if (json.has("values")) {
            JSONObject vals = json.getJSONObject("values");
            values = new HashMap<String,String>();
            String[] valNames = JSONObject.getNames(vals);
            if (valNames != null) {
                for (int i = 0; i < valNames.length; i++)
                    values.put(valNames[i], vals.getString(valNames[i]));
            }
        }
    }

    private List<Jsonable> getMembersList(MemberType type, JSONArray memsArray) throws JSONException {
        List<Jsonable> members = new ArrayList<Jsonable>();
        for (int i = 0; i < memsArray.length(); i++)
            members.add(getMember(type, memsArray.getJSONObject(i)));
        return members;
    }

    private Jsonable getMember(MemberType type, JSONObject mem) throws JSONException {
        switch(type) {
            case MasterRequest:
                return new Request(mem);
            case TaskInstance:
                return new TaskInstanceVO(mem);
            case ProcessInstance:
                return new ProcessInstanceVO(mem);
            case Solution:
                return new Solution(mem);
            default:
                throw new IllegalArgumentException("Unsupported solution MemberType: " + type);
        }
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        if (name != null)
            json.put("name", name);
        json.put("ownerType", ownerType);
        json.put("ownerId", ownerId);
        json.put("created", StringHelper.serviceDateToString(created));
        json.put("createdBy", createdBy);
        if (modified != null)
            json.put("modified", StringHelper.serviceDateToString(modified));
        if (modifiedBy != null)
            json.put("modifiedBy", modifiedBy);
        if (description != null)
            json.put("description", description);
        if (members != null) {
            JSONObject membersJson = new JSONObject();
            for (MemberType memberType : members.keySet()) {
                List<Jsonable> memberList = members.get(memberType);
                JSONArray membersArray = new JSONArray();
                for (Jsonable member : memberList)
                    membersArray.put(member.getJson());
                membersJson.put(getMemberListName(memberType), membersArray);
            }
            json.put("members", membersJson);
        }
        if (values != null) {
            JSONObject valuesJson = new JSONObject();
            for (String name : values.keySet()) {
                String value = values.get(name);
                valuesJson.put(name, value == null ? "" : value);
            }
            json.put("values", valuesJson);
        }
        return json;
    }

    public String getJsonName() {
        return getClass().getSimpleName();
    }

}
