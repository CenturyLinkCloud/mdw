package com.centurylink.mdw.microservice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

/**
 * Summarizes microservice invocations and updates from runtime state.
 * This is designed to be used and updated throughout a master flow.
 */
public class ServiceSummary implements Jsonable {

    private String masterRequestId;
    public String getMasterRequestId() { return masterRequestId; }

    private Map<String,List<MicroserviceInstance>> microservices = new LinkedHashMap<>();
    public Map<String,List<MicroserviceInstance>> getMicroservices() { return microservices; }
    public void setMicroservices(Map<String,List<MicroserviceInstance>> microservices) {
        this.microservices = microservices;
    }

    public List<MicroserviceInstance> getMicroservices(String name) {
        List<MicroserviceInstance> instances = microservices.get(name);
        return instances == null ? new ArrayList<>() : instances;
    }

    public MicroserviceInstance getMicroservice(String name, Long instanceId) {
        for (MicroserviceInstance instance : getMicroservices(name)) {
            if (instanceId.equals(instance.getInstanceId()))
                return instance;
        }
        return null;
    }

    public List<Invocation> getInvocations(String microserviceName, Long instanceId) {
        MicroserviceInstance instance = getMicroservice(microserviceName, instanceId);
        return instance == null ? null : instance.getInvocations();
    }

    public List<Update> getUpdates(String microserviceName, Long instanceId) {
        MicroserviceInstance instance = getMicroservice(microserviceName, instanceId);
        return instance == null ? null : instance.getUpdates();
    }

    /**
     * Adds and returns a microservice.
     */
    public MicroserviceInstance addMicroservice(String name, Long instanceId) {
        MicroserviceInstance microservice = new MicroserviceInstance(name, instanceId);
        List<MicroserviceInstance> instances = microservices.get(name);
        if (instances == null) {
            instances = new ArrayList<>();
            microservices.put(name, instances);
        }
        instances.add(microservice);
        return microservice;
    }

    public void addInvocation(String microserviceName, Long instanceId, Invocation invocation) {
        List<MicroserviceInstance> instances = microservices.get(microserviceName);
        if (instances == null) {
            instances = new ArrayList<>();
            microservices.put(microserviceName, instances);
        }
        MicroserviceInstance instance = getMicroservice(microserviceName, instanceId);
        if (instance == null) {
            instance = addMicroservice(microserviceName, instanceId);
        }
        List<Invocation> invocations = getInvocations(microserviceName, instanceId);
        if (invocations == null) {
            invocations = new ArrayList<>();
            instance.setInvocations(invocations);
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
            JSONArray instancesArr = json.getJSONArray("microservices");
            for (int i = 0; i < instancesArr.length(); i++) {
                MicroserviceInstance instance = new MicroserviceInstance(instancesArr.getJSONObject(i));
                List<MicroserviceInstance> instances = microservices.get(instance.getMicroservice());
                if (instances == null) {
                    instances = new ArrayList<>();
                    microservices.put(instance.getMicroservice(), instances);
                }
                instances.add(instance);
            }
        }
    }

    public JSONObject getJson() {
        JSONObject json = create();
        json.put("masterRequestId", masterRequestId);
        JSONArray instancesArr = new JSONArray();
        json.put("microservices", instancesArr);
        for (String microserviceName : microservices.keySet()) {
            for (MicroserviceInstance instance : microservices.get(microserviceName)) {
                instancesArr.put(instance.getJson());
            }
        }
        return json;
    }
}
