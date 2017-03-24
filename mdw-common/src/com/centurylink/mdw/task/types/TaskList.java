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
package com.centurylink.mdw.task.types;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.InstanceList;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.util.StringHelper;

public class TaskList implements Jsonable, InstanceList<TaskInstance> {

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
        if (jsonObj.has("total"))
            total = jsonObj.getLong("total");
        if (jsonObj.has(name)) {
            JSONArray taskList = jsonObj.getJSONArray(name);
            for (int i = 0; i < taskList.length(); i++)
                tasks.add(new TaskInstance((JSONObject)taskList.get(i)));
        }
    }

    public TaskList(String name, List<TaskInstance> tasks) {
        this.name = name;
        this.tasks = tasks;
        if (tasks != null)
            count = tasks.size();
    }

    public TaskList() {
        this.name = TASKS;
        this.tasks = new ArrayList<TaskInstance>();
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("retrieveDate", StringHelper.serviceDateToString(getRetrieveDate()));
        json.put("count", count);
        if (total > 0)
            json.put("total", total);
        JSONArray array = new JSONArray();
        if (tasks != null) {
            for (TaskInstance task : tasks)
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

    private long total;
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    private List<TaskInstance> tasks = new ArrayList<TaskInstance>();
    public List<TaskInstance> getTasks() { return tasks; }
    public void setTasks(List<TaskInstance> tasks) { this.tasks = tasks; }

    public List<TaskInstance> getItems() {
        return tasks;
    }

    public void addTask(TaskInstance task) {
        tasks.add(task);
    }

    public TaskInstance getTask(Long taskInstanceId) {
        for (TaskInstance task : tasks) {
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
