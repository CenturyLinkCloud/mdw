/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.services;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.services.ServiceSummaryConstants;

/**
 * Summarizes planned microservice invocations runtime state.
 * <p>
 * This is designed to be used and updated throughout a workflow
 * </p>
 */
public class ServiceSummary implements Jsonable {

    private String publicUserId;
    public String getPublicUserId() { return publicUserId; }
    public void setPublicUserId(String publicUserId) { this.publicUserId = publicUserId; }

    private String requestId;
    public String getRequestId() { return requestId; }
    public void setRequestId(String id) { this.requestId = id; }

    private List<MicroserviceSummary> microserviceSummaries = new ArrayList<MicroserviceSummary>();
    public List<MicroserviceSummary> getMicroserviceSummaries() { return microserviceSummaries; }
    public void setMicroserviceSummaries(List<MicroserviceSummary> invocations) {
        this.microserviceSummaries = invocations;
    }

    public MicroserviceSummary getSummary(String microservice) {
        for (MicroserviceSummary microSummary : microserviceSummaries) {
            if (microSummary.getMicroservice().equals(microservice))
                return microSummary;
        }
        return null;
    }

    public List<Invocation> getInvocations(String microservice) {
        MicroserviceSummary microSummary = getSummary(microservice);
        if (microSummary == null)
            return null;
        else
            return microSummary.getInvocations();
    }

    //No guarantee that "micro.getUpdates() returns a initialized array
    //due to the fact that initially the updates attribute doesn't exist
    public List<Update> getUpdates(String microservice) {
        for (MicroserviceSummary micro : microserviceSummaries) {
            if (micro.getMicroservice().equals(microservice))
                return micro.getUpdates();
        }
        return null;
    }

    /**
     * Add a microservice without any invocations.
     */
    public List<Invocation> addMicroservice(String microservice) {
        MicroserviceSummary microInvokes = new MicroserviceSummary(microservice);
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
        MicroserviceSummary microserviceSummary = retrieveMicroserviceSummary(microservice);
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
    public MicroserviceSummary retrieveMicroserviceSummary(String microservice) {
        for (MicroserviceSummary micro : microserviceSummaries) {
            if (micro.getMicroservice().equals(microservice)) {
                return micro;
            }
        }
        //Guarantees updates will be initialized
        MicroserviceSummary microSummary = new MicroserviceSummary(microservice);
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
                MicroserviceSummary microInvokes = new MicroserviceSummary(microservice, summaryJson.getJSONObject(microservice));
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
        for (MicroserviceSummary microInvoke : microserviceSummaries) {
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
        return ServiceSummaryConstants.SERVICE_SUMMARY;
    }

}
