/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.InstanceList;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.utilities.StringHelper;

public class TaskTemplateList implements Jsonable, InstanceList<TaskVO> {

    public TaskTemplateList(List<TaskVO> taskTemplates) {
        this.taskTemplates = taskTemplates;
        this.count = taskTemplates.size();
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

    private List<TaskVO> taskTemplates = new ArrayList<TaskVO>();
    public List<TaskVO> getTaskTemplates() { return taskTemplates; }
    public void settaskTemplates(List<TaskVO> taskTemplates) { this.taskTemplates = taskTemplates; }

    public List<TaskVO> getItems() {
        return taskTemplates;
    }

    public int getIndex(String id) {
        for (int i = 0; i < taskTemplates.size(); i++) {
            if (taskTemplates.get(i).getTaskId().equals(id))
                return i;
        }
        return -1;
    }

    public TaskVO get(String cuid) {
        for (TaskVO taskTemplate : taskTemplates) {
            if (taskTemplate.getTaskId().equals(cuid))
                return taskTemplate;
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
        if (taskTemplates != null) {
            for (TaskVO taskTemplate : taskTemplates)
                array.put(taskTemplate.getJson());
        }
        json.put("taskTemplates", array);
        return json;
    }

    public String getJsonName() {
        return "TaskTemplates";
    }
}
