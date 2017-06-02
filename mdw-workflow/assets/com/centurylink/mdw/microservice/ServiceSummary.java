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
package com.centurylink.mdw.microservice;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.Status;

/**
 * Summarizes planned microservice invocations runtime state.
 * This is designed to be used and updated throughout a master workflow.
 */
public class ServiceSummary implements Jsonable {

    // constants
    public static final String SERVICE_SUMMARY = "serviceSummary";
    public static final String REQUEST_ID_VAR = "Request ID";
    public static final String DEFAULT_REQUEST_ID_VAR = "requestId";
    public static final String MICROSERVICE = "Microservice";
    public static final String SERVICE_SUMMARY_NOTIFICATION = "servicesummary-update-";

    private String publicUserId;
    public String getPublicUserId() { return publicUserId; }
    public void setPublicUserId(String publicUserId) { this.publicUserId = publicUserId; }

    private String requestId;
    public String getRequestId() { return requestId; }
    public void setRequestId(String id) { this.requestId = id; }

    private List<Microservice> microserviceSummaries = new ArrayList<Microservice>();
    public List<Microservice> getMicroserviceSummaries() { return microserviceSummaries; }
    public void setMicroserviceSummaries(List<Microservice> invocations) {
        this.microserviceSummaries = invocations;
    }

    public Microservice getSummary(String microservice) {
        for (Microservice microSummary : microserviceSummaries) {
            if (microSummary.getMicroservice().equals(microservice))
                return microSummary;
        }
        return null;
    }

    public List<Invocation> getInvocations(String microservice) {
        Microservice microSummary = getSummary(microservice);
        if (microSummary == null)
            return null;
        else
            return microSummary.getInvocations();
    }

    //No guarantee that "micro.getUpdates() returns a initialized array
    //due to the fact that initially the updates attribute doesn't exist
    public List<Update> getUpdates(String microservice) {
        for (Microservice micro : microserviceSummaries) {
            if (micro.getMicroservice().equals(microservice))
                return micro.getUpdates();
        }
        return null;
    }

    /**
     * Add a microservice without any invocations.
     */
    public List<Invocation> addMicroservice(String microservice) {
        Microservice microInvokes = new Microservice(microservice);
        microserviceSummaries.add(microInvokes);
        return microInvokes.getInvocations();
    }

    public void addInvocation(String microservice, Invocation invocation) {
        List<Invocation> existing = getInvocations(microservice);
        if (existing == null)
            addMicroservice(microservice).add(invocation);
        else
            existing.add(invocation);
    }
    /**
     * Dealt with slightly differently to invocations since invocation
     * will definitely be there, update won't, so initiliaze only when needed
     * @param microservice
     * @param update
     */
    public void addUpdate(String microservice, Update update) {
        Microservice microserviceSummary = retrieveMicroserviceSummary(microservice);
        if (microserviceSummary.getUpdates() == null) {
            microserviceSummary.setUpdates(new ArrayList<Update>());
        }
        microserviceSummary.getUpdates().add(update);
    }

    /**
     * Gets the microserviceSummary for a microservice
     * Note, the side effect is that if it doesn't find one, it creates it
     * @param microservice
     * @return MicroserviceSummary object
     */
    public Microservice retrieveMicroserviceSummary(String microservice) {
        for (Microservice micro : microserviceSummaries) {
            if (micro.getMicroservice().equals(microservice)) {
                return micro;
            }
        }
        //Guarantees updates will be initialized
        Microservice microSummary = new Microservice(microservice);
        microserviceSummaries.add(microSummary);
        return microSummary;
    }
    public ServiceSummary() {
    }

    public ServiceSummary(JSONObject json) throws JSONException {
        if (json.has("publicUserId"))
            this.publicUserId = json.getString("publicUserId");
        if (json.has("requestId"))
            this.requestId = json.getString("requestId");
        if (json.has("microservices")) {
            JSONObject summaryJson = json.getJSONObject("microservices");
            for (String microservice : JSONObject.getNames(summaryJson)) {
                Microservice microInvokes = new Microservice(microservice, summaryJson.getJSONObject(microservice));
                microserviceSummaries.add(microInvokes);
            }
        }
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        if (publicUserId != null)
            json.put("publicUserId", publicUserId);
        if (requestId != null)
            json.put("requestId", requestId);
        JSONObject invocationsJson = create();
        for (Microservice microInvoke : microserviceSummaries) {
            invocationsJson.put(microInvoke.getMicroservice(), microInvoke.getJson());
        }
        json.put("microservices", invocationsJson);
        return json;
    }
    /**
     * Convenience method to check if a message has been received as an update
     * @param microservice
     * @param messageType
     * @return true if message has been received, false otherwise
     */
    public boolean isMessageReceived(String microservice, String messageType) {
     List<Update> updates = getUpdates(microservice);
     Jsonable message = updates == null ? null
             : updates.stream().filter((update) -> Status.OK.getCode() == update
                     .getStatus().getCode() && messageType.equals(update
                     .getStatus().getMessage())).findFirst().orElse(null);
     return message != null;
    }


    public String getJsonName() {
        return SERVICE_SUMMARY;
    }

}
