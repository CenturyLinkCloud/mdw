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

    private Map<String,MicroserviceList> microservices = new LinkedHashMap<>();
    public Map<String,MicroserviceList> getMicroservices() { return microservices; }
    public void setMicroservices(Map<String,MicroserviceList> microservices) {
        this.microservices = microservices;
    }

    public MicroserviceList getMicroservices(String name) {
        return microservices.get(name);
    }

    public MicroserviceInstance getMicroservice(String name, Long instanceId) {
        MicroserviceList instances = getMicroservices(name);
        return instances == null ? null : instances.getInstance(instanceId);
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
        MicroserviceList instances = getMicroservices(name);
        if (instances == null) {
            instances = new MicroserviceList(name);
            microservices.put(name, instances);
        }
        instances.add(microservice);
        return microservice;
    }

    public void addInvocation(String microserviceName, Long instanceId, Invocation invocation) {
        MicroserviceList instances = microservices.get(microserviceName);
        if (instances == null) {
            instances = new MicroserviceList(microserviceName);
            microservices.put(microserviceName, instances);
        }
        MicroserviceInstance instance = instances.getInstance(instanceId);
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

    public void addUpdate(String microserviceName, Long instanceId, Update update) {
        MicroserviceList instances = microservices.get(microserviceName);
        if (instances == null) {
            instances = new MicroserviceList(microserviceName);
            microservices.put(microserviceName, instances);
        }
        MicroserviceInstance instance = instances.getInstance(instanceId);
        if (instance == null) {
            instance = addMicroservice(microserviceName, instanceId);
        }
        List<Update> updates = getUpdates(microserviceName, instanceId);
        if (updates == null) {
            updates = new ArrayList<>();
            instance.setUpdates(updates);
        }
        updates.add(update);
    }

    public ServiceSummary(String masterRequestId) {
        this.masterRequestId = masterRequestId;
    }

    public ServiceSummary(JSONObject json) {
        this.masterRequestId = json.getString("masterRequestId");
        if (json.has("microservices")) {
            JSONArray microservicesArr = json.getJSONArray("microservices");
            for (int i = 0; i < microservicesArr.length(); i++) {
                JSONObject listObj = microservicesArr.getJSONObject(i);
                String name = listObj.getString("name");
                if (listObj.has("instances")) {
                    MicroserviceList instances = new MicroserviceList(name);
                    microservices.put(name, instances);
                    JSONArray instancesArr = listObj.getJSONArray("instances");
                    for (int j = 0; j < instancesArr.length(); j++) {
                        MicroserviceInstance instance = new MicroserviceInstance(instancesArr.getJSONObject(j));
                        instance.setMicroservice(name);
                        instances.add(instance);
                    }
                }
            }
        }
    }

    public JSONObject getJson() {
        JSONObject json = create();
        json.put("masterRequestId", masterRequestId);
        JSONArray microservicesArr = new JSONArray();
        json.put("microservices", microservicesArr);
        for (String microserviceName : microservices.keySet()) {
            JSONObject listObj = new JSONObject();
            listObj.put("name", microserviceName);
            microservicesArr.put(listObj);
            MicroserviceList instances = getMicroservices(microserviceName);
            if (instances != null) {
                JSONArray instancesArr = new JSONArray();
                listObj.put("instances", instancesArr);
                for (MicroserviceInstance instance : instances.getInstances()) {
                    JSONObject instanceObj = instance.getJson();
                    instanceObj.remove("microservice");
                    instancesArr.put(instanceObj);
                }
            }
        }
        return json;
    }
}
