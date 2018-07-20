/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util;

public class HttpResponse {
    private int code;
    public int getCode() { return code; }
    void setCode(int code) { this.code = code; }

    private String message;
    public String getMessage() { return message; }
    void setMessage(String message) { this.message = message; }

    private byte[] content;
    public byte[] getContent() { return content; }

    HttpResponse(byte[] content) {
        this.content = content;
    }

}
