/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.user;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.InstanceList;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.util.StringHelper;

public class RoleList implements Jsonable, InstanceList<Role> {

    public RoleList(List<Role> roles) {
        this.roles = roles;
        this.count = roles.size();
    }

    public RoleList(String json) throws JSONException {
        JSONObject jsonObj = new JSONObject(json);
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
        JSONObject json = new JSONObject();
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