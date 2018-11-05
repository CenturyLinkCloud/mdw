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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.monitor.ServiceLevelAgreement;

public class Activity implements Serializable, Comparable<Activity>, Jsonable {

    public static final String DEFAULT_IMPL = "com.centurylink.mdw.workflow.activity.DefaultActivityImpl";

    private Long id;
    private String name;
    private String description;

    private String implementor;
    private List<Attribute> attributes;

    public Activity() {
    }

    public Activity(Long id, String name, String description, String implementor, List<Attribute> attributes){
        this.id = id;
        this.name = name;
        this.description = description;
        this.implementor = implementor;
        this.attributes = attributes;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }
    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public String getImplementor() {
        return implementor;
    }
    public void setImplementor(String implementor) {
        this.implementor = implementor;
    }

    /**
     * Method that returns the sla
     */
    public int getSlaSeconds() {
        String sla = this.getAttribute(WorkAttributeConstant.SLA);
        if (sla == null || sla.length() == 0)
            return 0;
        String unit = this.getAttribute(WorkAttributeConstant.SLA_UNITS);
        if (unit == null || unit.isEmpty())
            unit = this.getAttribute(WorkAttributeConstant.SLA_UNIT);
        if (unit == null || unit.isEmpty())
            unit = ServiceLevelAgreement.INTERVAL_SECONDS;
        return ServiceLevelAgreement.unitsToSeconds(sla, unit);
    }

    public int getSla() {
        return getSlaSeconds();
    }

    /**
     * method that sets the sla
     */
    public void setSlaSeconds(int slaSeconds) {
        String unit = this.getAttribute(WorkAttributeConstant.SLA_UNITS);
        if (unit == null || unit.isEmpty())
            getAttribute(WorkAttributeConstant.SLA_UNIT);
        if (unit == null || unit.isEmpty()) {
            unit = ServiceLevelAgreement.INTERVAL_SECONDS;
            setAttribute(WorkAttributeConstant.SLA_UNIT, unit);
        }
        setAttribute(WorkAttributeConstant.SLA, ServiceLevelAgreement.secondsToUnits(slaSeconds, unit));
    }

    public void setSla(int sla) {
        setSlaSeconds(sla);
    }

    public String getAttribute(String name) {
        return Attribute.findAttribute(attributes, name);
    }

    public void setAttribute(String name, String value) {
        if (attributes == null)
            attributes = new ArrayList<Attribute>();
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

    public String getReferenceId() {
        return getAttribute(WorkAttributeConstant.REFERENCE_ID);
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

    // for labeling only
    private String processName;
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }

    private int sequenceId;
    public int getSequenceId() { return sequenceId; }
    public void setSequenceId(int sequenceId) { this.sequenceId = sequenceId; }

}
