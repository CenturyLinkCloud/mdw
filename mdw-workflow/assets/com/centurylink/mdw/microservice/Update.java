package com.centurylink.mdw.microservice;

import java.time.Instant;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.Status;

/**
 * An update from a microservice.
 */
public class Update implements Jsonable {

    private Long requestId;
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    private Status status;
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    private Instant received;
    public Instant getReceived() { return received; }
    public void setReceived(Instant received) { this.received = received; }

    public Update(Long requestId, Status status, Instant received) {
        this.requestId = requestId;
        this.status = status;
        this.received = received;
    }
    public Update(JSONObject json) {
        bind(json);
    }
}
