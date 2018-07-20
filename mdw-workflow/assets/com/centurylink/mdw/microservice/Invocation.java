package com.centurylink.mdw.microservice;

import java.time.Instant;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.Status;

/**
 * A single invocation of a microservice.
 */
public class Invocation implements Jsonable {

    private Long requestId;
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    private Long responseId;
    public Long getResponseId() { return responseId; }
    public void setResponseId(Long responseId) { this.responseId = responseId; }

    private Status status;
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    private Instant sent;
    public Instant getSent() { return sent; }
    public void setSent(Instant sent) { this.sent = sent; }

    public Invocation(Long requestId, Status status, Instant sent) {
        this.requestId = requestId;
        this.status = status;
        this.sent = sent;
    }
    public Invocation(Long requestId, Status status, Instant sent, Long responseId) {
        this.requestId = requestId;
        this.responseId = responseId;
        this.status = status;
        this.sent = sent;
    }

    public Invocation(JSONObject json) {
        bind(json);
    }
}
