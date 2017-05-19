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
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.monitor.ServiceLevelAgreement;
import com.centurylink.mdw.util.JsonUtil;
import com.centurylink.mdw.util.StringHelper;

public class Activity implements Serializable, Comparable<Activity>, Jsonable {

    private Long activityId;
    private String activityName;
    private String activityDescription;

    private String implementorClassName;
    private List<Attribute> attributes;
    private Long[] synchronzingIds;

    public Activity() {
    }

    public Activity(Long pActId, String pActName, String pDesc, String pActImplClass, List<Attribute> pAttribs){
        this.activityId = pActId;
        this.activityName = pActName;
        this.activityDescription = pDesc;
        this.implementorClassName = pActImplClass;
        this.attributes = pAttribs;
    }

    /**
     * @return the activityId
     */
    public Long getActivityId() {
        return activityId;
    }

    /**
     * @param activityId the activityId to set
     */
    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    /**
     * @return the activityName
     */
    public String getActivityName() {
        return activityName;
    }

    /**
     * @param activityName the activityName to set
     */
    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    /**
     * @return the activityDescription
     */
    public String getActivityDescription() {
        return activityDescription;
    }

    /**
     * @param activityDescription the activityName to set
     */
    public void setActivityDescription(String activityDescription) {
        this.activityDescription = activityDescription;
    }

    /**
     * @return the attributes
     */
    public List<Attribute> getAttributes() {
        return attributes;
    }

    /**
     * @param attributes the attributes to set
     */
    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public Long[] getSynchronzingIds() {
        return synchronzingIds;
    }

    public void setSynchronzingIds(Long[] pIds) {
        this.synchronzingIds = pIds;
    }

    /**
     * @return the implementorClassName
     */
    public String getImplementorClassName() {
        return implementorClassName;
    }

    /**
     * @param implementorClassName the implementorClassName to set
     */
    public void setImplementorClassName(String implementorClassName) {
        this.implementorClassName = implementorClassName;
    }

    public void addSynchronizationId(Long pId){
        List<Long> temp = null;
        if(this.synchronzingIds != null && this.synchronzingIds.length > 0){
            temp = new ArrayList<Long>(Arrays.asList(this.synchronzingIds));
            if(temp.contains(pId)){
                return;
            }
        }else{
           temp = new ArrayList<Long>();
        }
        temp.add(pId);
        this.synchronzingIds = temp.toArray(new Long[temp.size()]);
    }

    public void deleteSynchronizationId(Long pId){
        if(this.synchronzingIds == null || this.synchronzingIds.length==0) return;
        List<Long> temp = new ArrayList<Long>(Arrays.asList(this.synchronzingIds));
        temp.remove(pId);
        this.synchronzingIds = temp.toArray(new Long[]{});
    }

    /**
     * Method that returns the sla
     */
    public int getSlaSeconds(){
        String sla = this.getAttribute(WorkAttributeConstant.SLA);
        if (sla==null || sla.length()==0) return 0;
        String unit = this.getAttribute(WorkAttributeConstant.SLA_UNITS);
        if (StringHelper.isEmpty(unit)) unit = this.getAttribute(WorkAttributeConstant.SLA_UNIT);
        if (StringHelper.isEmpty(unit)) unit = ServiceLevelAgreement.INTERVAL_HOURS;
        return ServiceLevelAgreement.unitsToSeconds(sla, unit);
    }

    public int getSla() {
        return getSlaSeconds();
    }

    /**
     * method that sets the sla
     */
    public void setSlaSeconds(int slaSeconds){
        String unit = this.getAttribute(WorkAttributeConstant.SLA_UNITS);
        if (StringHelper.isEmpty(unit)) this.getAttribute(WorkAttributeConstant.SLA_UNIT);
        if (StringHelper.isEmpty(unit)) {
            unit = ServiceLevelAgreement.INTERVAL_MINUTES;
            setAttribute(WorkAttributeConstant.SLA_UNIT, unit);
        }
        setAttribute(WorkAttributeConstant.SLA, ServiceLevelAgreement.secondsToUnits(slaSeconds, unit));
    }

    public void setSla(int sla) {
        setSlaSeconds(sla);
    }

    /**
     * Returns the value of a process attribute.
     * @param attrname
     * @return the value of the attribute, or null if the attribute does not exist
     */
    public String getAttribute(String attrname) {
        return Attribute.findAttribute(attributes, attrname);
    }

    /**
     * Set the value of a process attribute.
     * If the value is null, the attribute is removed.
     * If the attribute does not exist and the value is not null, the attribute
     * is created.
     * @param attrname
     * @param value
     */
    public void setAttribute(String attrname, String value) {
        if (attributes==null) attributes = new ArrayList<Attribute>();
        Attribute.setAttribute(attributes, attrname, value);
    }

    public int compareTo(Activity other) {
        if (other == null)
            return 1;

        return this.getActivityName().compareTo(other.getActivityName());
    }

    public String getLogicalId() {
        return getAttribute(WorkAttributeConstant.LOGICAL_ID);
    }

    public String getReferenceId() {
        return getAttribute(WorkAttributeConstant.REFERENCE_ID);
    }

    private int sequenceId;
    public int getSequenceId() { return sequenceId; }
    public void setSequenceId(int sequenceId) { this.sequenceId = sequenceId; }


    public Activity(JSONObject json) throws JSONException {
        setActivityName(json.getString("name"));
        String logicalId = json.getString("id");
        if (logicalId.startsWith("Activity"))
            logicalId = "A" + logicalId.substring(8);
        activityId = Long.valueOf(logicalId.substring(1));
        setImplementorClassName(json.getString("implementor"));
        if (json.has("description"))
            setActivityDescription(json.getString("description"));
        if (json.has("attributes"))
            this.attributes = JsonUtil.getAttributes(json.getJSONObject("attributes"));
        setAttribute(WorkAttributeConstant.LOGICAL_ID, logicalId);
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("name", getActivityName());
        json.put("id", getLogicalId());
        json.put("description", getActivityDescription());
        json.put("implementor", getImplementorClassName());
        if (attributes != null && !attributes.isEmpty())
            json.put("attributes", JsonUtil.getAttributesJson(attributes));
        return json;
    }

    public String getJsonName() {
        return JsonUtil.padLogicalId(getLogicalId());
    }

    // for labeling only
    private String processName;
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }
}
