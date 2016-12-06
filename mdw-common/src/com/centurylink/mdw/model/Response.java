/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model;

public class Response {

    private String content;
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    private Integer statusCode;
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer code) { this.statusCode = code; }

    private String statusMessage;
    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String message) { this.statusMessage = message; }

    public Response(String content) {
        this.content = content;
    }

    public boolean isEmpty() {
        return content != null && !content.isEmpty();
    }

}
