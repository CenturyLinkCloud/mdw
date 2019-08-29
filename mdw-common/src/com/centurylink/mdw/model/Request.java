package com.centurylink.mdw.model;

import org.json.JSONObject;

/**
 * Generic request for Endpoints API.
 */
public class Request implements Jsonable {

    public Request(JSONObject json) {
        bind(json);
    }

    private String protocol = "REST";
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    private String operation;
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    private String path;
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    private String content;
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

}
