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

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a workflow process in a hierarchical structure of called/calling processes.
 */
public class LinkedProcess implements Jsonable {

    private static final int INDENT = 2;

    private Process process;
    public Process getProcess() { return process; }

    private LinkedProcess parent;
    public LinkedProcess getParent() { return parent; }
    public void setParent(LinkedProcess parent) { this.parent = parent; }

    private List<LinkedProcess> children = new ArrayList<>();
    public List<LinkedProcess> getChildren() { return children; }
    public void setChildren(List<LinkedProcess> children) { this.children = children; }

    private boolean circular;
    public boolean isCircular() { return circular; }
    public void setCircular(boolean snipped) { this.circular = snipped; }

    public LinkedProcess(Process process) {
        this.process = process;
    }

    public LinkedProcess(JSONObject jsonObj) throws JSONException {
        JSONObject procObj = jsonObj.getJSONObject("process");
        this.process = new Process(procObj);
        if (jsonObj.has("children")) {
            JSONArray childrenArr = jsonObj.getJSONArray("children");
            for (int i = 0; i < childrenArr.length(); i++) {
                JSONObject childObj = (JSONObject)childrenArr.get(i);
                this.children.add(new LinkedProcess(childObj));
            }
        }
    }

    /**
     * Includes children (not parent).
     */
    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("process", process.getSummaryJson());
        if (!children.isEmpty()) {
            JSONArray childrenArr = new JSONArray();
            for (LinkedProcess child : children) {
                childrenArr.put(child.getJson());
            }
            json.put("children", childrenArr);
        }
        if (isCircular())
            json.put("circular", true);
        return json;
    }

    public String getJsonName() {
        return "LinkedProcess";
    }

    @Override
    public String toString() {
        return process.getFullLabel() + (isCircular() ? " (+)" : "");
    }

    /**
     * Indented per specified depth
     */
    public String toString(int depth) {
        if (depth == 0) {
            return toString();
        }
        else {
            return String.format("%1$" + (depth * INDENT) + "s", "") + " - " + toString();
        }
    }

    /**
     * @return direct line call chain to me
     */
    private LinkedProcess callChain;  // cached
    public LinkedProcess getCallChain() {
        if (callChain == null) {
            LinkedProcess parent = getParent();
            if (parent == null) {
                callChain = new LinkedProcess(getProcess());
            }
            else {
                LinkedProcess chainedParent = new LinkedProcess(getProcess());
                while (parent != null) {
                    LinkedProcess newParent = new LinkedProcess(parent.getProcess());
                    newParent.getChildren().add(chainedParent);
                    chainedParent.setParent(newParent);
                    chainedParent = newParent;
                    parent = parent.getParent();
                }
                callChain = chainedParent;
            }
        }
        return callChain;
    }

    public boolean checkCircular() {
        LinkedProcess p = getCallChain();
        List<Process> called = new ArrayList<>();
        List<LinkedProcess> c;
        while (!(c = p.children).isEmpty()) {
            LinkedProcess child = c.get(0);
            if (called.contains(child.process)) {
                this.circular = true;
                return true;
            }
            called.add(child.process);
            p = child;
        }
        return false;
    }

    /**
     * Remove all branches that don't lead to specified process.
     */
    public void prune(Process process) {
        if (process.equals(this.process))
            return;
        LinkedProcess caller = this;
        List<LinkedProcess> toRemove = new ArrayList<>();
        for (LinkedProcess child : caller.children) {
            if (!child.contains(process)) {
                toRemove.add(child);
            }
            child.prune(process);
        }
        caller.children.removeAll(toRemove);
    }

    /**
     * Returns true if the call hierarchy contains the specified process.
     */
    public boolean contains(Process process) {
        if (process.equals(this.process))
            return true;
        for (LinkedProcess child : children) {
            if (child.contains(process))
                return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LinkedProcess && ((LinkedProcess)o).process.equals(process);
    }
}
