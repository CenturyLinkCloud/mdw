/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.workflow;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.InstanceList;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.util.StringHelper;

/**
 * A collection of activity instances.
 */
public class ActivityList implements Jsonable, InstanceList<ActivityInstance> {

    public static final String ACTIVITY_INSTANCES = "activityInstances";

    public ActivityList(String name, List<ActivityInstance> activities) {
        this.name = name;
        this.activities = activities;
        if (activities != null)
            this.count = activities.size();
    }

    public ActivityList(String name, JSONObject jsonObj) throws JSONException, ParseException {
        this.name = name;
        if (jsonObj.has("retrieveDate"))
            retrieveDate = StringHelper.serviceStringToDate(jsonObj.getString("retrieveDate"));
        if (jsonObj.has("count"))
            count = jsonObj.getInt("count");
        if (jsonObj.has("total"))
            total = jsonObj.getLong("total");
        else if (jsonObj.has("totalCount"))
            total = jsonObj.getLong("totalCount"); // compatibility
        JSONArray activityList = jsonObj.getJSONArray(name);
        for (int i = 0; i < activityList.length(); i++)
            activities.add(new ActivityInstance((JSONObject)activityList.get(i)));
    }

    public String getJsonName() {
        return name;
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (retrieveDate != null)
            json.put("retrieveDate", StringHelper.serviceDateToString(getRetrieveDate()));
        if (count != -1)
            json.put("count", count);
        if (total != -1) {
            json.put("total", total);
            json.put("totalCount", total); // compatibility
        }
        JSONArray array = new JSONArray();
        if (activities != null) {
            for (ActivityInstance act : activities)
                array.put(act.getJson());
        }
        json.put(name, array);
        return json;
    }

    private String name;
    public String getName() { return name;}
    public void setName(String name) { this.name = name; }

    private Date retrieveDate;
    public Date getRetrieveDate() { return retrieveDate; }
    public void setRetrieveDate(Date d) { this.retrieveDate = d; }

    private int count = -1;
    public int getCount() { return count; }
    public void setCount(int ct) { this.count = ct; }

    private long total = -1;
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    private List<ActivityInstance> activities = new ArrayList<>();
    public List<ActivityInstance> getActivities() { return activities; }
    public void setActivities(List<ActivityInstance> activities) { this.activities = activities; }

    public List<ActivityInstance> getItems() {
        return activities;
    }

    public void addActivity(ActivityInstance activity) {
        activities.add(activity);
    }

    public ActivityInstance getActivity(Long activityInstanceId) {
        for (ActivityInstance activity : activities) {
            if (activity.getId().equals(activityInstanceId))
                return activity;
        }
        return null;
    }

    public int getIndex(Long activityInstanceId) {
        for (int i = 0; i < activities.size(); i++) {
            if (activities.get(i).getId().equals(activityInstanceId))
                return i;
        }
        return -1;
    }

    public int getIndex(String id) {
        return (getIndex(Long.parseLong(id)));
    }
}
