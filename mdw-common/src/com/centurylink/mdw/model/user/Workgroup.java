/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="Workgroup", description="MDW user workgroup")
public class Workgroup implements Serializable, Comparable<Workgroup>, Jsonable {

    public static final boolean DEFAULT_ALL_ROLES = false;

    /**
     * Site admin group is the parent/ancester group for all other groups.
     * If a group has parent null, the site admin group is considered to be its parent.
     */
    public static final String SITE_ADMIN_GROUP = "Site Admin";
    /**
     * Shared group is a virtual group (not really persisted in database) that
     * everyone belongs to. This is specifically useful for backward compatibility,
     * where resources with no association of groups are assumed to belong to this group.
     */
    public static final String COMMON_GROUP = "Common";
    public static final Long COMMON_GROUP_ID = 0L;
    public static final Workgroup getCommonGroup() {
        return new Workgroup(COMMON_GROUP_ID, Workgroup.COMMON_GROUP, null);
    }

    public Workgroup() {
    }

    public Workgroup(Long id, String name, String comment) {
        this.id = id;
        this.name = name;
        this.description = comment;
    }

    /**
     * This constructor parses name and roles from a string, in the form "group_name(role1;role2)"
     * @param nameAndRoles
     */
    public Workgroup(String nameAndRoles) {
        id = null;
        description = null;
        int k = nameAndRoles.indexOf(':');
        if (k>0) {
            name = nameAndRoles.substring(0,k);
            roles = Arrays.asList(nameAndRoles.substring(k+1).split("/"));
        } else {
            name = nameAndRoles;
            roles = null;
        }
    }

    public Workgroup(JSONObject json) throws JSONException {
        name = json.getString("name");
        if (json.has("description"))
            description = json.getString("description");
        if (json.has("parent"))
            parentGroup = json.getString("parent");
        if (json.has("users")) {
            JSONArray usrs = json.getJSONArray("users");
            users = new User[usrs.length()];
            for (int i = 0; i < usrs.length(); i++) {
                String usr = usrs.getString(i);
                users[i] = new User();
                users[i].setCuid(usr);
            }
        }
        else {
            users = new User[0];
        }
    }

    private Long id;
    @ApiModelProperty(hidden=true)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id;}

    private String name;
    @ApiModelProperty(value="Workgroup unique name", required=true)
    public String getName() { return name; }
    public void setName(String groupName) { this.name = groupName; }

    private String description;
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    private List<String> roles; // populated when the group is a member of UserVO
    @ApiModelProperty(hidden=true)
    public void setRoles(List<String> roles) { this.roles = roles; }
    public List<String> getRoles() { return roles; }

    private User[] users;
    public User[] getUsers() { return users; }
    public void setUsers(User[] users) { this.users = users; }

    @ApiModelProperty(hidden=true)
    public String[] getUserCuids() {
        if (users == null)
            return new String[0];
        List<String> userList = new ArrayList<String>();
        for (User user : users) {
            userList.add(user.getCuid());
        }
        return userList.toArray(new String[0]);
    }

    private String parentGroup;
    public String getParentGroup() { return parentGroup; }
    public void setParentGroup(String parentGroup) { this.parentGroup = parentGroup; }

    private String endDate;
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public boolean isActive() {
        return endDate == null;
    }

    /**
     * Check whether the group has the specified role.
     */
    public boolean hasRole(String roleName){
        if (roles == null)
            return DEFAULT_ALL_ROLES;
            // no specified roles means all roles when DEFAULT_ALL_ROLES is true
        for (String r : roles) {
            if (r.equals(roleName)) return true;
        }
        return false;
    }

    @ApiModelProperty(hidden=true)
    public String getNameAndRolesAsString() {
         StringBuffer buffer = new StringBuffer();
         buffer.append(name);
         if (roles!=null&&roles.size()>0) {
             buffer.append(':');
             for (int i=0; i<roles.size(); i++) {
                 if (i>0) buffer.append('/');
                 buffer.append(roles.get(i));
             }
         }
         return buffer.toString();
    }

    @ApiModelProperty(hidden=true)
    public String getRolesAsString() {
            StringBuffer buffer = new StringBuffer();
        if (roles != null && roles.size() > 0) {
            for (int i = 0; i < roles.size(); i++) {
                if (i > 0)
                    buffer.append('/');
                buffer.append(roles.get(i));
            }
            return buffer.toString();
        } else {
            if (DEFAULT_ALL_ROLES)
                return Role.ALL;
            else
                return Role.VIEW_ONLY;
        }
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("UserGroupVO[");
        buffer.append("groupId = ").append(id);
        buffer.append(" groupName = ").append(name);
        buffer.append(" comment = ").append(description);
        if (users == null) {
            buffer.append(" users = ").append("null");
        }
        else {
            buffer.append(" users = ").append(Arrays.asList(users).toString());
        }
        buffer.append("]");
        return buffer.toString();
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", name);
        if (parentGroup != null)
            json.put("parent", parentGroup);
        if (description != null)
            json.put("description", description);
        if (users != null) {
            JSONArray usersJson = new JSONArray();
            for (User user : users) {
                usersJson.put(user.getJson());
            }
            json.put("users", usersJson);
        }
        return json;
    }

    public int compareTo(Workgroup other) {
        if (this.name == null)
            return 0;
        else
            return this.name.compareToIgnoreCase(other.getName());
    }

    public boolean equals(Object o) {
        if (!(o instanceof Workgroup))
            return false;
        return getId().equals(((Workgroup)o).getId());
    }

    public String getJsonName() { return "Group"; }
}
