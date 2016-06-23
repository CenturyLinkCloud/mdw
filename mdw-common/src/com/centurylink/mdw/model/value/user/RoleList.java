/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.user;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.InstanceList;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.utilities.StringHelper;

public class RoleList implements Jsonable, InstanceList<UserRoleVO> {

    public RoleList(List<UserRoleVO> roles) {
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
                roles.add(new UserRoleVO((JSONObject)roleList.get(i)));
        }
        else if (jsonObj.has("allRoles")) {  // designer compatibility
            JSONArray roleList = jsonObj.getJSONArray("allRoles");
            for (int i = 0; i < roleList.length(); i++)
                roles.add(new UserRoleVO((JSONObject)roleList.get(i)));
        }
    }

    private Date retrieveDate;
    public Date getRetrieveDate() { return retrieveDate; }
    public void setRetrieveDate(Date d) { this.retrieveDate = d; }

    private int count;
    public int getCount() { return count; }
    public void setCount(int ct) { this.count = ct; }

    public long getTotal() { return count; }  // no pagination

    private List<UserRoleVO> roles = new ArrayList<UserRoleVO>();
    public List<UserRoleVO> getRoles() { return roles; }
    public void setRoles(List<UserRoleVO> roles) { this.roles = roles; }

    public List<UserRoleVO> getItems() {
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
            for (UserRoleVO role : roles)
                array.put(role.getJson());
        }
        json.put("roles", array);
        return json;
    }

    public String getJsonName() {
        return "Roles";
    }

    public UserRoleVO get(String name) {
        for (UserRoleVO role : roles) {
            if (role.getName().equals(name))
                return role;
        }
        return null;
    }
}