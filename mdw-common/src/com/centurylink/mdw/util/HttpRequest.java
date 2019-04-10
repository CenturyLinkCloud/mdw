package com.centurylink.mdw.util;

/**
 * Models an HttpRequest.
 */
public class HttpRequest {
    private HttpConnection connection;
    public HttpConnection getConnection() { return connection; }

    public HttpRequest(HttpConnection httpConnection) {
        this.connection = httpConnection;
    }
}
