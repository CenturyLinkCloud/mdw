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
import io.swagger.annotations.ApiModel;
import org.json.JSONException;
import org.json.JSONObject;

@ApiModel(value="TransitionInstance", description="MDW transition instance")
public class TransitionInstance implements Jsonable {

    private Long transitionInstanceID;
    private Long transitionID;
    private Long processInstanceID;
    private Long destinationID;     // to-activity ID at initialization; to-activity instance ID at completion
    private int statusCode;
    private String startDate;
    private String endDate;

    public TransitionInstance(){
        this.processInstanceID = null;
        this.statusCode = 0;
        this.startDate = null;
        this.endDate = null;
    }

    public TransitionInstance(JSONObject json) throws JSONException {
        if (json.has("id"))
            transitionInstanceID = json.getLong("id");
        if (json.has("transitionId"))
            transitionID = json.getLong("transitionId");
        if (json.has("processInstanceId"))
            processInstanceID = json.getLong("processInstanceId");
        if (json.has("destinationId"))
            destinationID = json.getLong("destinationId");
        if (json.has("statusCode"))
            statusCode = json.getInt("statusCode");
        if (json.has("startDate"))
            startDate = json.getString("startDate");
        if (json.has("endDate"))
            endDate = json.getString("endDate");
    }

    public Long getProcessInstanceID() {
        return processInstanceID;
    }

    public void setProcessInstanceID(Long pID) {
        this.processInstanceID = pID;
    }

     public Long getTransitionInstanceID() {
        return transitionInstanceID;
    }

    public void setTransitionInstanceID(Long pID) {
        this.transitionInstanceID = pID;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int stCD) {
        this.statusCode = stCD;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Long getTransitionID() {
        return transitionID;
    }

    public void setTransitionID(Long transitionID) {
        this.transitionID = transitionID;
    }

    public Long getDestinationID() {
        return destinationID;
    }

    public void setDestinationID(Long destinationID) {
        this.destinationID = destinationID;
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        if (transitionInstanceID != null)
            json.put("id", transitionInstanceID);
        if (transitionID != null)
            json.put("transitionId", transitionID);
        if (processInstanceID != null)
            json.put("processInstanceId", processInstanceID);
        if (destinationID != null)
            json.put("destinationId", destinationID);
        if (statusCode > 0)
            json.put("statusCode", statusCode);
        if (startDate != null)
            json.put("startDate", startDate);
        if (endDate != null)
            json.put("endDate", endDate);
        return json;
    }

    public String getJsonName() {
        return "TransitionInstance";
    }
}
