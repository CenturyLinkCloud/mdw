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
package com.centurylink.mdw.model.workflow;

import java.text.ParseException;
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

/**
 * A collection of workflow process instances.
 */
public class ProcessList implements Jsonable, InstanceList<ProcessInstance> {

    public static final String PROCESS_INSTANCES = "processInstances";

    public ProcessList(String name, String json) throws JSONException, ParseException {
        this(name, new JsonObject(json));
    }

    public ProcessList(String name, JSONObject jsonObj) throws JSONException, ParseException {
        this.name = name;
        if (jsonObj.has("retrieveDate"))
            retrieveDate = StringHelper.serviceStringToDate(jsonObj.getString("retrieveDate"));
        if (jsonObj.has("count"))
            count = jsonObj.getInt("count");
        if (jsonObj.has("total"))
            total = jsonObj.getLong("total");
        else if (jsonObj.has("totalCount"))
            total = jsonObj.getLong("totalCount"); // compatibility
        JSONArray processList = jsonObj.getJSONArray(name);
        for (int i = 0; i < processList.length(); i++)
            processes.add(new ProcessInstance((JSONObject)processList.get(i)));
    }

    public ProcessList(String name, List<ProcessInstance> processes) {
        this.name = name;
        this.processes = processes;
        if (processes != null)
            this.count = processes.size();
    }

    public String getJsonName() {
        return name;
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        if (retrieveDate != null)
            json.put("retrieveDate", StringHelper.serviceDateToString(getRetrieveDate()));
        if (count != -1)
            json.put("count", count);
        if (total != -1) {
            json.put("total", total);
            json.put("totalCount", total); // compatibility
        }
        JSONArray array = new JSONArray();
        if (processes != null) {
            for (ProcessInstance process : processes)
                array.put(process.getJson());
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

    private List<ProcessInstance> processes = new ArrayList<ProcessInstance>();
    public List<ProcessInstance> getProcesses() { return processes; }
    public void setProcesses(List<ProcessInstance> processes) { this.processes = processes; }

    public List<ProcessInstance> getItems() {
        return processes;
    }

    public void addProcess(ProcessInstance process) {
        processes.add(process);
    }

    public ProcessInstance getProcess(Long processInstanceId) {
        for (ProcessInstance process : processes) {
            if (process.getId().equals(processInstanceId))
                return process;
        }
        return null;
    }

    public int getIndex(Long processInstanceId) {
        for (int i = 0; i < processes.size(); i++) {
            if (processes.get(i).getId().equals(processInstanceId))
                return i;
        }
        return -1;
    }

    public int getIndex(String id) {
        return (getIndex(Long.parseLong(id)));
    }
}
