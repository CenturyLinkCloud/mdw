package com.centurylink.mdw.microservice;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

/**
 * Instances for a specific microservice.
 */
public class MicroserviceList implements Jsonable {

    /**
     * Microservice logical name.
     */
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private List<MicroserviceInstance> instances;
    public List<MicroserviceInstance> getInstances() { return instances; }
    public void setInstances(List<MicroserviceInstance> instances) { this.instances = instances; }

    public MicroserviceList(String name) {
        this.name = name;
        this.instances = new ArrayList<>();
    }

    public MicroserviceList(JSONObject json) {
        bind(json);
    }

    public MicroserviceInstance getInstance(Long id) {
        for (MicroserviceInstance instance : instances) {
            if (id.equals(instance.getId()))
                return instance;
        }
        return null;
    }

    public MicroserviceInstance add(MicroserviceInstance instance) {
        instances.add(instance);
        return instance;
    }

}
