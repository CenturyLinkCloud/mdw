package com.centurylink.mdw.microservice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.intellij.util.containers.HashMap;

/**
 * Summarizes microservice invocations and updates from runtime state.
 * This is designed to be used and updated throughout a master flow.
 */
public class ServiceSummary implements Jsonable {

    private String masterRequestId;
    public String getMasterRequestId() { return masterRequestId; }

    private Map<String,List<MicroserviceHistory>> microservices = new HashMap<>();
    public Map<String,List<MicroserviceHistory>> getMicroservices() { return microservices; }
    public void setMicroservices(Map<String,List<MicroserviceHistory>> microservices) {
        this.microservices = microservices;
    }

    public List<MicroserviceHistory> getMicroservices(String name) {
        List<MicroserviceHistory> histories = microservices.get(name);
        return histories == null ? new ArrayList<>() : histories;
    }

    public MicroserviceHistory getMicroservice(String name, Long instanceId) {
        for (MicroserviceHistory history : getMicroservices(name)) {
            if (history.getInstanceId() == instanceId)
                return history;
        }
        return null;
    }

    public List<Invocation> getInvocations(String microserviceName, Long instanceId) {
        for (MicroserviceHistory history : getMicroservices(microserviceName)) {
            if (history.getInstanceId() == instanceId)
                return history.getInvocations();
        }
        return null;
    }

    public List<Update> getUpdates(String microserviceName, Long instanceId) {
        for (MicroserviceHistory history : getMicroservices(microserviceName)) {
            if (history.getInstanceId() == instanceId)
                return history.getUpdates();
        }
        return null;
    }

    /**
     * Adds and returns a microservice.
     */
    public MicroserviceHistory addMicroservice(String name, Long instanceId) {
        MicroserviceHistory microservice = new MicroserviceHistory(name, instanceId);
        List<MicroserviceHistory> histories = microservices.get(name);
        if (histories == null) {
            histories = new ArrayList<>();
            microservices.put(name, histories);
        }
        histories.add(microservice);
        return microservice;
    }

    public void addInvocation(String microserviceName, Long instanceId, Invocation invocation) {
        List<MicroserviceHistory> histories = microservices.get(microserviceName);
        if (histories == null) {
            histories = new ArrayList<>();
            microservices.put(microserviceName, histories);
        }
        MicroserviceHistory history = getMicroservice(microserviceName, instanceId);
        if (history == null) {
            history = addMicroservice(microserviceName, instanceId);
        }
        List<Invocation> invocations = getInvocations(microserviceName, instanceId);
        if (invocations == null) {
            invocations = new ArrayList<>();
            history.setInvocations(invocations);
        }
        invocations.add(invocation);
    }

    public void addUpdate(String microservice, Update update) {
        throw new RuntimeException("Not implemented");
    }

    public ServiceSummary(String masterRequestId) {
        this.masterRequestId = masterRequestId;
    }

    public ServiceSummary(JSONObject json) {
        this.masterRequestId = json.getString("masterRequestId");
        if (json.has("microservices")) {
            JSONObject summaryJson = json.getJSONObject("microservices");
            String[] microserviceNames = JSONObject.getNames(summaryJson);
            if (microserviceNames != null) {
                for (String microserviceName : microserviceNames) {
                    List<MicroserviceHistory> histories = new ArrayList<>();
                    JSONArray microservicesArr = summaryJson.getJSONArray(microserviceName);
                    for (int i = 0; i < microservicesArr.length(); i++) {
                        histories.add(new MicroserviceHistory(microserviceName, microservicesArr.getJSONObject(i)));
                    }
                    microservices.put(microserviceName, histories);
                }
            }
        }
    }

    public JSONObject getJson() {
        JSONObject json = create();
        json.put("masterRequestId", masterRequestId);
        JSONObject summaryJson = create();
        json.put("microservices", summaryJson);
        for (String microserviceName : microservices.keySet()) {
            List<MicroserviceHistory> histories = microservices.get(microserviceName);
            JSONArray microservicesArr = new JSONArray();
            summaryJson.put(microserviceName, microservicesArr);
            for (MicroserviceHistory history : histories) {
                JSONObject microserviceJson = history.getJson();
                microserviceJson.remove("microservice");
                microservicesArr.put(microserviceJson);
            }
        }
        return json;
    }
}
