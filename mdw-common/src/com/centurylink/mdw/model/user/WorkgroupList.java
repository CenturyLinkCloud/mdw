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

import com.centurylink.mdw.common.service.InstanceList;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.util.StringHelper;

import io.swagger.annotations.ApiModel;

@ApiModel(value="WorkgroupList", description="List of MDW workgroups")
public class WorkgroupList implements Jsonable, InstanceList<Workgroup> {

    public WorkgroupList(List<Workgroup> groups) {
        this.groups = groups;
        this.count = groups.size();
    }

    public WorkgroupList(String json) throws JSONException {
        JSONObject jsonObj = new JSONObject(json);
        if (jsonObj.has("retrieveDate"))
            retrieveDate = StringHelper.serviceStringToDate(jsonObj.getString("retrieveDate"));
        if (jsonObj.has("count"))
            count = jsonObj.getInt("count");
        if (jsonObj.has("workgroups")) {
            JSONArray groupList = jsonObj.getJSONArray("workgroups");
            for (int i = 0; i < groupList.length(); i++)
                groups.add(new Workgroup((JSONObject)groupList.get(i)));
        }
        else if (jsonObj.has("allWorkgroups")) { // designer compatibility
            JSONArray groupList = jsonObj.getJSONArray("allWorkgroups");
            for (int i = 0; i < groupList.length(); i++)
                groups.add(new Workgroup((JSONObject)groupList.get(i)));
        }
    }

    private Date retrieveDate;
    public Date getRetrieveDate() { return retrieveDate; }
    public void setRetrieveDate(Date d) { this.retrieveDate = d; }

    private int count;
    public int getCount() { return count; }
    public void setCount(int ct) { this.count = ct; }

    public long getTotal() { return count; }  // no pagination

    private List<Workgroup> groups = new ArrayList<Workgroup>();
    public List<Workgroup> getGroups() { return groups; }
    public void setGroups(List<Workgroup> groups) { this.groups = groups; }

    public List<Workgroup> getItems() {
        return groups;
    }

    public int getIndex(String id) {
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).getId().equals(id))
                return i;
        }
        return -1;
    }

    public Workgroup get(String name) {
        for (Workgroup group : groups) {
            if (group.getName().equals(name))
                return group;
        }
        return null;
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("retrieveDate", StringHelper.serviceDateToString(getRetrieveDate()));
        json.put("count", count);
        JSONArray array = new JSONArray();
        if (groups != null) {
            for (Workgroup group : groups)
                array.put(group.getJson());
        }
        json.put("workgroups", array);
        return json;
    }

    public String getJsonName() {
        return "Workgroups";
    }
}
