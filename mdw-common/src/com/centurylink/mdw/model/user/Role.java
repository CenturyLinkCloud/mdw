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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="Role", description="MDW user role")
public class Role implements Serializable, Comparable<Role>, Jsonable  {

    public static final String PROCESS_DESIGN = "Process Design";
    public static final String ASSET_DESIGN = PROCESS_DESIGN;  // synonym for process design role
    public static final String PROCESS_EXECUTION = "Process Execution";
    public static final String TASK_EXECUTION = "Task Execution";
    public static final String USER_ADMIN = "User Admin";
    public static final String TASK_ADMIN = "Task Admin";

    // if these roles exist, then read access is restricted
    public static final String ASSET_VIEW = "Asset View";
    public static final String RUNTIME_VIEW = "Runtime View";
    public static final String USER_VIEW = "User View";
    // (task view access is restricted by way of workgroups)

    public Role() {
    }

    public Role(JSONObject json) throws JSONException {
        name = json.getString("name");
        if (json.has("description"))
            description = json.getString("description");
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
    public void setId(Long id) { this.id = id; }

    private String name;
    @ApiModelProperty(value="Role's unique name", required=true)
    public String getName() { return name;  }
    public void setName(String roleName) { this.name = roleName; }

    private String description;
    public String getDescription() { return description; }
    public void setDescription(String comment) { this.description = comment; }

    private User[] users;
    public User[] getUsers() { return users; }
    public void setUsers(User[] users) { this.users = users; }

    public int compareTo(Role other) {
        if (this.name == null)
            return 0;
        else
            return this.name.compareToIgnoreCase(other.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Role))
            return false;
        return getId().equals(((Role)o).getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();

    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("name", getName());
        if (getDescription() != null)
            json.put("description", getDescription());
        if (users != null) {
            JSONArray usersJson = new JSONArray();
            for (User user : users) {
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
        for (User user : users) {
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
}
