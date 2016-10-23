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

public class UserList implements Jsonable, InstanceList<User> {

    public UserList(List<User> users) {
        this.users = users;
        this.count = users.size();
    }

    public UserList(String json) throws JSONException {
        JSONObject jsonObj = new JSONObject(json);
        if (jsonObj.has("retrieveDate"))
            retrieveDate = StringHelper.serviceStringToDate(jsonObj.getString("retrieveDate"));
        if (jsonObj.has("count"))
            count = jsonObj.getInt("count");
        if (jsonObj.has("total"))
            total = jsonObj.getLong("total");
        if (jsonObj.has("users")) {
            JSONArray userList = jsonObj.getJSONArray("users");
            for (int i = 0; i < userList.length(); i++)
                users.add(new User((JSONObject)userList.get(i)));
        }
    }

    private Date retrieveDate;
    public Date getRetrieveDate() { return retrieveDate; }
    public void setRetrieveDate(Date d) { this.retrieveDate = d; }

    private int count;
    public int getCount() { return count; }
    public void setCount(int ct) { this.count = ct; }

    private long total = -1;
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    private List<User> users = new ArrayList<User>();
    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }

    public List<User> getItems() {
        return users;
    }

    public int getIndex(String id) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(id))
                return i;
        }
        return -1;
    }

    public User get(String cuid) {
        for (User user : users) {
            if (user.getCuid().equals(cuid))
                return user;
        }
        return null;
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("retrieveDate", StringHelper.serviceDateToString(getRetrieveDate()));
        json.put("count", count);
        if (total != -1)
            json.put("total", total);
        JSONArray array = new JSONArray();
        if (users != null) {
            for (User user : users)
                array.put(user.getJson());
        }
        json.put("users", array);
        return json;
    }

    public String getJsonName() {
        return "Users";
    }
}
