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
import com.centurylink.mdw.model.project.Data;
import com.centurylink.mdw.monitor.MonitorAttributes;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Activity implements Comparable<Activity>, Jsonable, Linkable {
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

    @Override
    public String getQualifiedLabel() {
        return getLogicalId() + ": " + oneLineName();
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

    public JSONObject getSummaryJson() {
        JSONObject json = create();
        json.put("name", getName());
        json.put("id", getLogicalId());
        json.put("implementor", getImplementor());
        return json;
    }

    @Override
    public JSONObject getSummaryJson(int detail) {
        JSONObject json = getSummaryJson();
        if (detail > 0) {
            if (processId != null)
                json.put("processId", processId);
            if (processName != null)
                json.put("processName", processName);
            if (processVersion != null)
                json.put("processVersion", processVersion);
            if (packageName != null)
                json.put("packageName", packageName);
        }
        if (detail > 1) {
            if (milestoneName != null)
                json.put("milestoneName", milestoneName);
            if (milestoneGroup != null)
                json.put("milestoneGroup", milestoneGroup);
        }
        return json;
    }

    public String milestoneGroup() {
        String monitorsAttr = getAttribute(WorkAttributeConstant.MONITORS);
        if (monitorsAttr != null) {
            MonitorAttributes monitorAttributes = new MonitorAttributes(monitorsAttr);
            if (monitorAttributes.isEnabled(Milestone.MONITOR_CLASS)) {
                String text = monitorAttributes.getOptions(Milestone.MONITOR_CLASS);
                if (text != null && !text.isEmpty()) {
                    int bracket = text.lastIndexOf('[');
                    if (bracket >= 0) {
                        text = text.substring(bracket + 1);
                        bracket = text.indexOf(']');
                        if (bracket > 0) {
                            return text.substring(0, bracket);
                        }
                    }
                }
            }
        }
        if (Data.Implementors.START_IMPL.equals(getImplementor()))
            return "Start";
        else if (Data.Implementors.STOP_IMPL.equals(getImplementor()))
            return "Stop";
        else if (Data.Implementors.PAUSE_IMPL.equals(getImplementor()))
            return "Pause";
        return null;
    }

    public String milestoneName() {
        String monitorsAttr = getAttribute(WorkAttributeConstant.MONITORS);
        if (monitorsAttr != null) {
            MonitorAttributes monitorAttributes = new MonitorAttributes(monitorsAttr);
            if (monitorAttributes.isEnabled(Milestone.MONITOR_CLASS)) {
                String text = monitorAttributes.getOptions(Milestone.MONITOR_CLASS);
                if (text != null) {
                    int bracket = text.lastIndexOf('[');
                    if (bracket >= 0) {
                        text = text.substring(0, bracket);
                    }
                    return text.trim().replaceAll("\\\\n", "\n");
                }
            }
        }
        return null;
    }

    /**
     * TODO: expressions not supported here.
     */
    public final boolean isSynchronous() {
       return "true".equalsIgnoreCase(getAttribute("synchronous"));
    }

    public String oneLineName() {
        return getName().replaceAll("\r", "").replace('\n', ' ');
    }

    // for labeling only
    private String processName;
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }

    // transient for milestones
    private String packageName;
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    private String processVersion;
    public String getProcessVersion() { return processVersion; }
    public void setProcessVersion(String version) { this.processVersion = version; }

    private Long processId;
    public Long getProcessId() { return processId; }
    public void setProcessId(Long processId) { this.processId = processId; }

    private String milestoneGroup;
    public String getMilestoneGroup() { return milestoneGroup; }
    public void setMilestoneGroup(String group) { this.milestoneGroup = group; }

    private String milestoneName;
    public String getMilestoneName() { return milestoneName; }
    public void setMilestoneName(String milestoneName) { this.milestoneName = milestoneName; }

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

    public final boolean isStop() {
        return Data.Implementors.STOP_IMPL.equals(getImplementor());
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Activity))
            return false;
        Activity activity = (Activity) other;
        if (!activity.id.equals(id))
            return false;
        if (Objects.equals(activity.processId, processId))
            return true;
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        return ("" + id + (processId == null ? "" : processId)).hashCode();
    }
}
