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

    private Long instanceId;  // ActivityInstanceId that created this service summary
    public Long getInstanceId() { return instanceId; }

    private JSONObject attributes;  // General purpose
    public JSONObject getAttributes() { return attributes; }
    public void setAttributes(JSONObject attr) {
        this.attributes = attr;
    }

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
        if (instances == null && childServiceSummaryList != null) {
            MicroserviceInstance instance = null;
            int i = 0;
            while (instance == null && i < childServiceSummaryList.size()) {
                instance = childServiceSummaryList.get(i).getMicroservice(name, instanceId);
                i++;
            }
            return instance;
        }
        return instances == null ? null : instances.getInstance(instanceId);
    }

    /**
     * Adds instance and/or empty invocation list for this instance if not found.
     */
    public List<Invocation> getInvocations(String microserviceName, Long instanceId) {
        MicroserviceInstance instance = getMicroservice(microserviceName, instanceId);
        if (instance == null) {
            instance = addMicroservice(microserviceName, instanceId);
        }
        List<Invocation> invocations = instance.getInvocations();
        if (invocations == null) {
            invocations = new ArrayList<>();
            instance.setInvocations(invocations);
        }

        return invocations;
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
        MicroserviceInstance instance = getMicroservice(microserviceName, instanceId);
        if (instance == null) {
            instance = addMicroservice(microserviceName, instanceId);
        }
        List<Invocation> invocations = instance.getInvocations();
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

    private List<ServiceSummary> childServiceSummaryList = null;
    public List<ServiceSummary> getChildServiceSummaryList() { return childServiceSummaryList; }
    public ServiceSummary addServiceSummary(String masterRequestId, Long instanceId) {
        if (childServiceSummaryList == null)
            childServiceSummaryList = new ArrayList<>();

        ServiceSummary childServiceSummary = new ServiceSummary(masterRequestId, instanceId);
        childServiceSummaryList.add(childServiceSummary);
        return childServiceSummary;
    }

    /**
     * Find the ServiceSummary that contains a microservice with ID matching procInstId
     */
    public ServiceSummary findParent(Long procInstId) {
        ServiceSummary parent = null;
        for (MicroserviceList list : getMicroservices().values()) {
            for (MicroserviceInstance instance : list.getInstances()) {
                if (procInstId.equals(instance.getId()))
                    return this;
            }
        }
        if (childServiceSummaryList != null) {
            for (ServiceSummary child : childServiceSummaryList) {
                parent = child.findParent(procInstId);
                if (parent != null)
                    return parent;
            }
        }
        return parent;
    }

    /**
     * Find the ServiceSummary that was created by activity with activityInstanceId matching instanceId
     */
    public ServiceSummary findCurrent(Long instanceId) {
        ServiceSummary current = null;

        if (instanceId.equals(this.instanceId))
            return this;

        if (childServiceSummaryList != null) {
            for (ServiceSummary child : childServiceSummaryList) {
                current = child.findCurrent(instanceId);
                if (current != null)
                    return current;
            }
        }
        return current;
    }

    public ServiceSummary(String masterRequestId) {
        this.masterRequestId = masterRequestId;
    }

    public ServiceSummary(String masterRequestId, Long instanceId) {
        this.masterRequestId = masterRequestId;
        this.instanceId = instanceId;
    }

    public ServiceSummary(JSONObject json) {
        this.masterRequestId = json.getString("masterRequestId");
        this.instanceId = json.optLong("instanceId");
        this.attributes = json.optJSONObject("attributes");
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
        if (json.has("subServiceSummaries")) {
            JSONArray serviceSummariesArr = json.getJSONArray("subServiceSummaries");
            childServiceSummaryList = new ArrayList<>();
            for (int i = 0; i < serviceSummariesArr.length(); i++)
                childServiceSummaryList.add(new ServiceSummary(serviceSummariesArr.getJSONObject(i)));
        }
    }

    public JSONObject getJson() {
        JSONObject json = create();
        json.put("masterRequestId", masterRequestId);
        if (instanceId != null && instanceId > 0L)
            json.put("instanceId", instanceId);
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
        if (attributes != null)
            json.put("attributes", attributes);
        if (childServiceSummaryList != null) {
            JSONArray arr = new JSONArray();
            for (ServiceSummary child : childServiceSummaryList) {
                arr.put(child.getJson());
            }
            json.put("subServiceSummaries", arr);
        }
        return json;
    }
}
