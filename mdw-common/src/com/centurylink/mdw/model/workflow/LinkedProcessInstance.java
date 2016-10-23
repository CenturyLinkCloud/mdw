/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.workflow;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

/**
 * Represents a process instance in a hierarchical structure of called/calling processes.
 */
public class LinkedProcessInstance implements Jsonable {

    public LinkedProcessInstance(String json) throws JSONException {
        this(new JSONObject(json));
    }

    public LinkedProcessInstance(JSONObject jsonObj) throws JSONException {
        JSONObject procInstObj = jsonObj.getJSONObject("processInstance");
        this.processInstance = new ProcessInstance(procInstObj);
        if (jsonObj.has("children")) {
            JSONArray childrenArr = jsonObj.getJSONArray("children");
            for (int i = 0; i < childrenArr.length(); i++) {
                JSONObject childObj = (JSONObject)childrenArr.get(i);
                this.children.add(new LinkedProcessInstance(childObj));
            }
        }
    }

    private ProcessInstance processInstance;
    public ProcessInstance getProcessInstance() { return processInstance; }

    private LinkedProcessInstance parent;
    public LinkedProcessInstance getParent() { return parent; }
    public void setParent(LinkedProcessInstance parent) { this.parent = parent; }

    private List<LinkedProcessInstance> children = new ArrayList<LinkedProcessInstance>();
    public List<LinkedProcessInstance> getChildren() { return children; }
    public void setChildren(List<LinkedProcessInstance> children) { this.children = children; }

    public LinkedProcessInstance(ProcessInstance processInstance) {
        this.processInstance = processInstance;
    }

    /**
     * Includes children (not parent).
     */
    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("processInstance", processInstance.getJson());
        if (!children.isEmpty()) {
            JSONArray childrenArr = new JSONArray();
            for (LinkedProcessInstance child : children) {
                childrenArr.put(child.getJson());
            }
            json.put("children", childrenArr);
        }
        return json;
    }

    public String getJsonName() {
        return "LinkedProcessInstance";
    }
}
