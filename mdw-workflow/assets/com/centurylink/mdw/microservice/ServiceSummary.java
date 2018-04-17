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

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

/**
 * Summarizes microservice invocations and updates from runtime state.
 * This is designed to be used and updated throughout a master flow.
 */
public class ServiceSummary implements Jsonable {

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

    private List<MicroserviceHistory> microservices = new ArrayList<>();
    public List<MicroserviceHistory> getMicroserviceSummaries() { return microservices; }
    public void setMicroserviceSummaries(List<MicroserviceHistory> microservices) {
        this.microservices = microservices;
    }

    public MicroserviceHistory getMicroservice(String name) {
        for (MicroserviceHistory microservice : microservices) {
            if (microservice.getMicroservice().equals(name))
                return microservice;
        }
        return null;
    }

    public List<Invocation> getInvocations(String microservice) {
        MicroserviceHistory history = getMicroservice(microservice);
        if (history == null)
            return null;
        else
            return history.getInvocations();
    }

    public List<Update> getUpdates(String microservice) {
        MicroserviceHistory history = getMicroservice(microservice);
        if (history == null)
            return null;
        else
            return history.getUpdates();
    }

    /**
     * Add a microservice without any invocations.
     */
    public List<Invocation> addMicroservice(String name) {
        MicroserviceHistory microservice = new MicroserviceHistory(name);
        microservices.add(microservice);
        return microservice.getInvocations();
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
     * will definitely be there, update won't, so initialize only when needed
     * @param microservice
     * @param update
     */
    public void addUpdate(String microservice, Update update) {
        MicroserviceHistory history = retrieveHistory(microservice);
        if (history.getUpdates() == null) {
            history.setUpdates(new ArrayList<Update>());
        }
        history.getUpdates().add(update);
    }

    /**
     * Note: side effect is that if no history is found, one is created.
     */
    public MicroserviceHistory retrieveHistory(String microservice) {
        for (MicroserviceHistory history : microservices) {
            if (history.getMicroservice().equals(microservice)) {
                return history;
            }
        }
        // guarantees updates will be initialized
        MicroserviceHistory history = new MicroserviceHistory(microservice);
        microservices.add(history);
        return history;
    }

    public ServiceSummary() {
    }

    public ServiceSummary(JSONObject json) {
        if (json.has("requestId"))
            this.requestId = json.getString("requestId");
        if (json.has("microservices")) {
            JSONObject summaryJson = json.getJSONObject("microservices");
            for (String microservice : JSONObject.getNames(summaryJson)) {
                MicroserviceHistory history = new MicroserviceHistory(microservice, summaryJson.getJSONObject(microservice));
                microservices.add(history);
            }
        }
    }

    public JSONObject getJson() {
        JSONObject json = create();
        if (requestId != null)
            json.put("requestId", requestId);
        JSONObject historiesJson = create();
        for (MicroserviceHistory history : microservices) {
            JSONObject historyJson = history.getJson();
            String microservice = (String)historyJson.remove("microservice");
            historiesJson.put(microservice, historyJson);
        }
        json.put("microservices", historiesJson);
        return json;
    }

}
