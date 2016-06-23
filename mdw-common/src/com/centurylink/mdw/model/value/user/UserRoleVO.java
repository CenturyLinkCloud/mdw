/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.constant.UserRoleConstants;
import com.centurylink.mdw.common.service.Jsonable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="Role", description="MDW user role")
public class UserRoleVO implements Serializable, Comparable<UserRoleVO>, Jsonable  {

    // new, per-group roles
    public static final String PROCESS_DESIGN = "Process Design";
    public static final String ASSET_DESIGN = PROCESS_DESIGN;  // synonym for process design role
    public static final String PROCESS_EXECUTION = "Process Execution";
    public static final String USER_ADMIN = "User Admin";
    public static final String SUPERVISOR = "Supervisor";
    public static final String TASK_EXECUTION = "Task Execution";
    public static final String VIEW_ONLY = "View Only";     // only needed when user does not have any other role

    public static final String ALL = "All";        // has all roles applicable to the group

    public UserRoleVO() {
    }

    public UserRoleVO(JSONObject json) throws JSONException {
        name = json.getString("name");
        if (json.has("description"))
            description = json.getString("description");
        if (json.has("users")) {
            JSONArray usrs = json.getJSONArray("users");
            users = new UserVO[usrs.length()];
            for (int i = 0; i < usrs.length(); i++) {
                String usr = usrs.getString(i);
                users[i] = new UserVO();
                users[i].setCuid(usr);
            }
        }
        else {
            users = new UserVO[0];
        }
    }

    private Long id;
    @ApiModelProperty(hidden=true)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    private String name;
    @ApiModelProperty(value="Role's unique name", required=true)
    public String getName() { return name;  }
    public void setName(String roleName) { this.name = roleName; }

    private String description;
    public String getDescription() { return description; }
    public void setDescription(String comment) { this.description = comment; }

    private UserVO[] users;
    public UserVO[] getUsers() { return users; }
    public void setUsers(UserVO[] users) { this.users = users; }

    public int compareTo(UserRoleVO other) {
        if (this.name == null)
            return 0;
        else
            return this.name.compareToIgnoreCase(other.getName());
    }

    public boolean equals(Object o) {
        if (!(o instanceof UserRoleVO))
            return false;
        return getId().equals(((UserRoleVO)o).getId());
    }

    public boolean isOldGlobalRole() {
        return name.equals(UserRoleConstants.ADMINISTRATORS)
            || name.equals(UserRoleConstants.PROCESS_ADMIN)
            || name.equals(UserRoleConstants.PROCESS_INSTANCE_ADMIN);

    }

    public static String toNewName(String role) {
        if (role.equals(UserRoleConstants.PROCESS_ADMIN)) return PROCESS_DESIGN;
        if (role.equals(UserRoleConstants.PROCESS_INSTANCE_ADMIN)) return PROCESS_EXECUTION;
        if (role.equals(UserRoleConstants.ADMINISTRATORS)) return USER_ADMIN;
        return role;
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", getName());
        if (getDescription() != null)
            json.put("description", getDescription());
        if (users != null) {
            JSONArray usersJson = new JSONArray();
            for (UserVO user : users) {
                usersJson.put(user.getJson());
            }
            json.put("users", usersJson);
        }
        return json;
    }

    public String getJsonName() { return "Role"; }

    @ApiModelProperty(hidden=true)
    public String[] getUserCuids() {
        if (users == null)
            return new String[0];
        List<String> userList = new ArrayList<String>();
        for (UserVO user : users) {
            userList.add(user.getCuid());
        }
        return userList.toArray(new String[0]);
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("UserRoleVO[");
        buffer.append("id = ").append(id);
        buffer.append(" name = ").append(name);
        buffer.append(" description = ").append(description);
        if (users == null) {
            buffer.append(" users = ").append("null");
        }
        else {
            buffer.append(" users = ").append(Arrays.asList(users).toString());
        }
        buffer.append("]");
        return buffer.toString();
    }

    // compatibility
    /**
     * @deprecated use {@link #getName()}
     */
    @Deprecated
    @ApiModelProperty(hidden=true)
    public String getRoleName() { return getName(); }
    /**
     * @deprecated use {@link #setName()}
     */
    @Deprecated
    public void setRoleName(String roleName) { setName(roleName); }
    /**
     * @deprecated use {@link #getDescription()}
     */
    @Deprecated
    @ApiModelProperty(hidden=true)
    public String getComment() { return getDescription(); }
    /**
     * @deprecated use {@link #setDescription()}
     */
    @Deprecated
    @ApiModelProperty(hidden=true)
    public void setComment(String description) { setDescription(description); }

}
