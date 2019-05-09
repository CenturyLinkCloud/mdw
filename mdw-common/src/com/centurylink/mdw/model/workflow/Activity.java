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

import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.monitor.MonitorAttributes;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Activity implements Serializable, Comparable<Activity>, Jsonable {

    public static final String DEFAULT_IMPL = "com.centurylink.mdw.workflow.activity.DefaultActivityImpl";

    public Activity() {
    }

    private Long id;
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    private String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    private String description;
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    private String implementor;
    public String getImplementor() {
        return implementor;
    }
    public void setImplementor(String implementor) {
        this.implementor = implementor;
    }

    private List<Attribute> attributes;
    public List<Attribute> getAttributes() {
        return attributes;
    }
    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public String getAttribute(String name) {
        return Attribute.findAttribute(attributes, name);
    }

    public void setAttribute(String name, String value) {
        if (attributes == null)
            attributes = new ArrayList<>();
        Attribute.setAttribute(attributes, name, value);
    }

    public int compareTo(Activity other) {
        if (other == null)
            return 1;
        return this.getName().compareTo(other.getName());
    }

    public String getLogicalId() {
        return getAttribute(WorkAttributeConstant.LOGICAL_ID);
    }

    public Activity(JSONObject json) throws JSONException {
        setName(json.getString("name"));
        String logicalId = json.getString("id");
        if (logicalId.startsWith("Activity"))
            logicalId = "A" + logicalId.substring(8);
        id = Long.valueOf(logicalId.substring(1));
        if (json.has("implementor"))
            setImplementor(json.getString("implementor"));
        else
            setImplementor(DEFAULT_IMPL);
        if (json.has("description"))
            setDescription(json.getString("description"));
        if (json.has("attributes"))
            this.attributes = Attribute.getAttributes(json.getJSONObject("attributes"));
        setAttribute(WorkAttributeConstant.LOGICAL_ID, logicalId);
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("name", getName());
        json.put("id", getLogicalId());
        json.put("description", getDescription());
        json.put("implementor", getImplementor());
        if (attributes != null && !attributes.isEmpty())
            json.put("attributes", Attribute.getAttributesJson(attributes));
        return json;
    }

    public String oneLineName() {
        return getName().replaceAll("\r", "").replace('\n', ' ');
    }

    // for labeling only
    private String processName;
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }

    private int sequenceId;
    public int getSequenceId() { return sequenceId; }
    public void setSequenceId(int sequenceId) { this.sequenceId = sequenceId; }

    /**
     * TODO: MicroserviceOrchestratorActivity
     */
    public boolean invokesSubprocess(Process subproc) {
        String procName = getAttribute(WorkAttributeConstant.PROCESS_NAME);
        if (procName != null) {
            if (procName.endsWith(".proc"))
                procName = procName.substring(0, procName.length() - 5);
            if (procName.equals(subproc.getQualifiedName())) {
                String verSpec = getAttribute(WorkAttributeConstant.PROCESS_VERSION);
                if (subproc.meetsVersionSpec(verSpec))
                    return true;
            }
        }
        else {
            String procMap = getAttribute(WorkAttributeConstant.PROCESS_MAP);
            if (procMap != null) {
                List<String[]> procmap = Attribute.parseTable(procMap, ',', ';', 3);
                for (String[] strings : procmap) {
                    String nameSpec = strings[1];
                    if (nameSpec != null) {
                        if (nameSpec.endsWith(".proc"))
                            nameSpec = nameSpec.substring(0, nameSpec.length() - 5);
                        if (nameSpec.equals(subproc.getQualifiedName())) {
                            String verSpec = strings[2];
                            if (subproc.meetsVersionSpec(verSpec))
                                return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public List<Process> findInvoked(List<Process> processes) {
        List<Process> invoked = new ArrayList<>();
        String procName = getAttribute(WorkAttributeConstant.PROCESS_NAME);
        if (procName != null) {
            Process latestMatch = null;
            for (Process process : processes) {
                if (invokesSubprocess(process) &&
                        (latestMatch == null || latestMatch.getVersion() < process.getVersion())) {
                    latestMatch = process;
                }
            }
            if (latestMatch != null && !invoked.contains(latestMatch))
                invoked.add(latestMatch);
        }
        else {
            String procMap = getAttribute(WorkAttributeConstant.PROCESS_MAP);
            if (procMap != null) {
                List<String[]> procmap = Attribute.parseTable(procMap, ',', ';', 3);
                for (int i = 0; i < procmap.size(); i++) {
                    String nameSpec = procmap.get(i)[1];
                    if (nameSpec != null) {
                        Process latestMatch = null;
                        for (Process process : processes) {
                            if (nameSpec.endsWith(".proc"))
                                nameSpec = nameSpec.substring(0, nameSpec.length() - 5);
                            if (nameSpec.equals(process.getQualifiedName()) &&
                                    (latestMatch == null || latestMatch.getVersion() < process.getVersion())) {
                                latestMatch = process;
                            }
                        }
                        if (latestMatch != null && !invoked.contains(latestMatch))
                            invoked.add(latestMatch);
                    }
                }
            }
        }
        return invoked;
    }

    private static final String MONITOR_CLASS = "com.centurylink.mdw.milestones.ActivityMilestone";

    public Milestone getMilestone() {
        String monitorsAttr = getAttribute(WorkAttributeConstant.MONITORS);
        if (monitorsAttr != null) {
            MonitorAttributes monitorAttributes = new MonitorAttributes(monitorsAttr);
            if (monitorAttributes.isEnabled(MONITOR_CLASS)) {
                Milestone milestone = new Milestone(this);
                milestone.setText(monitorAttributes.getOptions(MONITOR_CLASS));
            }
        }
        return null;
    }
}
