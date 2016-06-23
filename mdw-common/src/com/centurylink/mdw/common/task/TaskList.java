/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.task;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.InstanceList;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;

public class TaskList implements Jsonable, InstanceList<TaskInstanceVO> {

    public static final String TASKS = "tasks";
    public static final String USER_TASKS = "userTasks";
    public static final String WORKGROUP_TASKS = "workgroupTasks";
    public static final String PROCESS_TASKS = "processTasks";

    public TaskList(String name, String json) throws JSONException, ParseException {
        this(name, new JSONObject(json));
    }

    public TaskList(String name, JSONObject jsonObj) throws JSONException, ParseException {
        this.name = name;
        if (jsonObj.has("retrieveDate"))
            retrieveDate = StringHelper.serviceStringToDate(jsonObj.getString("retrieveDate"));
        if (jsonObj.has("count"))
            count = jsonObj.getInt("count");
        if (jsonObj.has(name)) {
            JSONArray taskList = jsonObj.getJSONArray(name);
            for (int i = 0; i < taskList.length(); i++)
                tasks.add(new TaskInstanceVO((JSONObject)taskList.get(i)));
        }
    }

    public TaskList(String name, List<TaskInstanceVO> tasks) {
        this.name = name;
        this.tasks = tasks;
    }

    public TaskList() {
        this.name = TASKS;
        this.tasks = new ArrayList<TaskInstanceVO>();
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("retrieveDate", StringHelper.serviceDateToString(getRetrieveDate()));
        json.put("count", count);
        JSONArray array = new JSONArray();
        if (tasks != null) {
            for (TaskInstanceVO task : tasks)
                array.put(task.getJson());
        }
        json.put(name, array);
        return json;
    }

    public String getJsonName() {
        return name;
    }

    private String name;
    public String getName() { return name;}
    public void setName(String name) { this.name = name; }

    private Date retrieveDate;
    public Date getRetrieveDate() { return retrieveDate; }
    public void setRetrieveDate(Date d) { this.retrieveDate = d; }

    private int count;
    public int getCount() { return count; }
    public void setCount(int ct) { this.count = ct; }

    public long getTotal() {
        return count; // TODO: pagination
    }

    private List<TaskInstanceVO> tasks = new ArrayList<TaskInstanceVO>();
    public List<TaskInstanceVO> getTasks() { return tasks; }
    public void setTasks(List<TaskInstanceVO> tasks) { this.tasks = tasks; }

    public List<TaskInstanceVO> getItems() {
        return tasks;
    }

    public void addTask(TaskInstanceVO task) {
        tasks.add(task);
    }

    public TaskInstanceVO getTask(Long taskInstanceId) {
        for (TaskInstanceVO task : tasks) {
            if (task.getTaskInstanceId().equals(taskInstanceId))
                return task;
        }
        return null;
    }

    public int getIndex(Long taskInstanceId) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getTaskInstanceId().equals(taskInstanceId))
                return i;
        }
        return -1;
    }

    public int getIndex(String id) {
        return (getIndex(Long.parseLong(id)));
    }
}
