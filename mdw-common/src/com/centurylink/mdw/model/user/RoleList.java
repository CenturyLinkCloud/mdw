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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.InstanceList;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.util.StringHelper;

public class RoleList implements Jsonable, InstanceList<Role> {

    public RoleList(List<Role> roles) {
        this.roles = roles;
        this.count = roles.size();
    }

    public RoleList(String json) throws JSONException {
        JSONObject jsonObj = new JsonObject(json);
        if (jsonObj.has("retrieveDate"))
            retrieveDate = StringHelper.serviceStringToDate(jsonObj.getString("retrieveDate"));
        if (jsonObj.has("count"))
            count = jsonObj.getInt("count");
        if (jsonObj.has("roles")) {
            JSONArray roleList = jsonObj.getJSONArray("roles");
            for (int i = 0; i < roleList.length(); i++)
                roles.add(new Role((JSONObject)roleList.get(i)));
        }
        else if (jsonObj.has("allRoles")) {  // designer compatibility
            JSONArray roleList = jsonObj.getJSONArray("allRoles");
            for (int i = 0; i < roleList.length(); i++)
                roles.add(new Role((JSONObject)roleList.get(i)));
        }
    }

    private Date retrieveDate;
    public Date getRetrieveDate() { return retrieveDate; }
    public void setRetrieveDate(Date d) { this.retrieveDate = d; }

    private int count;
    public int getCount() { return count; }
    public void setCount(int ct) { this.count = ct; }

    public long getTotal() { return count; }  // no pagination

    private List<Role> roles = new ArrayList<Role>();
    public List<Role> getRoles() { return roles; }
    public void setRoles(List<Role> roles) { this.roles = roles; }

    public List<Role> getItems() {
        return roles;
    }

    public int getIndex(String id) {
        for (int i = 0; i < roles.size(); i++) {
            if (roles.get(i).getId().equals(id))
                return i;
        }
        return -1;
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("retrieveDate", StringHelper.serviceDateToString(getRetrieveDate()));
        json.put("count", count);
        JSONArray array = new JSONArray();
        if (roles != null) {
            for (Role role : roles)
                array.put(role.getJson());
        }
        json.put("roles", array);
        return json;
    }

    public String getJsonName() {
        return "Roles";
    }

    public Role get(String name) {
        for (Role role : roles) {
            if (role.getName().equals(name))
                return role;
        }
        return null;
    }
}