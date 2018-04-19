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
    public List<MicroserviceHistory> getMicroservices() { return microservices; }
    public void setMicroservices(List<MicroserviceHistory> microservices) {
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
     * Adds and returns a microservice.
     */
    public MicroserviceHistory addMicroservice(String name) {
        MicroserviceHistory microservice = new MicroserviceHistory(name);
        microservices.add(microservice);
        return microservice;
    }

    public void addInvocation(String microservice, Invocation invocation) {
        MicroserviceHistory history = getMicroservice(microservice);
        if (history == null) {
            history = addMicroservice(microservice);
        }
        getInvocations(microservice).add(invocation);
    }

    public void addUpdate(String microservice, Update update) {
        MicroserviceHistory history = getMicroservice(microservice);
        if (history == null) {
            history = addMicroservice(microservice);
        }
        List<Update> updates = getUpdates(microservice);
        if (updates == null) {
            updates = new ArrayList<Update>();
            history.setUpdates(updates);
        }
        updates.add(update);
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
