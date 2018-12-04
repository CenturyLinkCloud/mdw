package com.centurylink.mdw.microservice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.Status;

/**
 * Represents a microservice's invocations and updates for one process instance.
 * Invocations will always be non-null (empty if none), whereas updates can be null.
 */
public class MicroserviceInstance implements Jsonable {

    private String microservice;
    public String getMicroservice() { return microservice; }
    public void setMicroservice(String microservice) { this.microservice = microservice; }

    /**
     * Process instance ID.
     */
    private Long id;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    /**
     * Process instance triggered.
     */
    private Instant triggered;
    public Instant getTriggered() { return triggered; }
    public void setTriggered(Instant triggered) { this.triggered = triggered; }

    /**
     * Process instance status.
     */
    private String status;
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    /**
     * Microservice correlationID (transactionID from StatusMgr)
     */
    private String transactionId;
    public String getTransactionId() { return this.transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    /**
     * Calls to the microservice.
     */
    private List<Invocation> invocations;
    public List<Invocation> getInvocations() { return invocations; }
    public void setInvocations(List<Invocation> invocations) { this.invocations = invocations; }

    /**
     * Async response callbacks from the microservice.
     */
    private List<Update> updates;
    public List<Update> getUpdates() { return updates; }
    public void setUpdates(List<Update> updates) { this.updates = updates; }

    public MicroserviceInstance(String microservice, Long instanceId) {
        this.microservice = microservice;
        this.id = instanceId;
        this.invocations = new ArrayList<Invocation>();
    }

    public MicroserviceInstance(JSONObject json) {
        bind(json);
    }

    public Status latestStatus() {
        Invocation latestInvoke = getInvocations().stream().max((invoke1, invoke2) -> {
            return invoke1.getSent().compareTo(invoke2.getSent());
        }).orElse(null);
        Update latestUpdate = null;
        if (getUpdates() != null) {
            latestUpdate = getUpdates().stream().max((update1, update2) -> {
                return update1.getReceived().compareTo(update2.getReceived());
            }).orElse(null);
        }
        if (latestInvoke != null) {
            if (latestUpdate == null
                    || latestUpdate.getReceived().compareTo(latestInvoke.getSent()) < 0) {
                return latestInvoke.getStatus();
            }
            else {
                return latestUpdate.getStatus();
            }
        }
        return null;
    }

    @Override
    public String getJsonName() {
        return microservice;
    }
}
