package com.centurylink.mdw.util;

/**
 * Models an HttpResponse.
 */
public class HttpRequest {
    private HttpConnection connection;
    public HttpConnection getConnection() { return connection; }

    public HttpRequest(HttpConnection httpConnection) {
        this.connection = httpConnection;
    }

    public void setHeader(String name, String value) {
        connection.setHeader(name, value);
    }
}
