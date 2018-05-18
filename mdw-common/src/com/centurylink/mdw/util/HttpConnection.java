/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

/**
 * Wraps an HttpURLConnection to support non-standard methods like PATCH.
 */
public class HttpConnection {

    public static final String HTTP_BASIC_AUTH_HEADER = "Authorization";

    private URL url;
    protected URL getUrl() { return url; }

    private HttpURLConnection connection;
    HttpURLConnection getConnection() { return connection; }


    private boolean followRedirects = true;
    public boolean isFollowRedirects(){ return followRedirects; }
    public void setFollowRedirects(boolean follow) {
        this.followRedirects = follow;
    }

    private int connectTimeout = -1;
    public int getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(int timeout) {
        this.connectTimeout = timeout;
    }

    private int readTimeout = -1;
    public int getReadTimeout() { return readTimeout; }
    public void setReadTimeout(int timeout) {
        this.readTimeout = timeout;
    }

    Map<String,String> headers;
    public Map<String,String> getHeaders() { return headers; }
    public String getHeader(String name) {
        return headers == null ? null : headers.get(name);
    }
    public void setHeaders(Map<String,String> headers) { this.headers = headers; }
    public void setHeader(String name, String value) {
        if (headers == null)
            headers = new HashMap<>();
        headers.put(name, value);
    }

    private String proxyHost;
    public String getProxyHost() { return proxyHost; }
    public void setProxyHost(String host) { this.proxyHost = host; }

    private int proxyPort;
    public int getProxyPort() { return proxyPort; }
    public void setProxyPort(int port) { this.proxyPort = port; }

    private String proxyProtocol = "http";
    public String getProxyProtocol() { return proxyProtocol; }
    public void setProxyProtocol(String protocol) { this.proxyProtocol = protocol; }

    public void setProxy(String host, int port) {
        this.proxyHost = host;
        this.proxyPort = port;
    }

    private long maxBytes = -1;
    public long getMaxBytes() { return maxBytes; }
    public void setMaxBytes(long max) { this.maxBytes = max; }

    private String user;
    String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    private String password;
    String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public HttpConnection(URL url) {
        this.url = url;
        if (url.getUserInfo() != null) {
            int colon = url.getUserInfo().indexOf(':');
            if (colon > 0) {
                this.user = url.getUserInfo().substring(0, colon);
                this.password = url.getUserInfo().substring(colon + 1);
            }
        }
    }

    public HttpConnection(URL url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    protected HttpConnection(HttpURLConnection connection) {
        this.connection = connection;
    }

    protected HttpConnection(HttpURLConnection connection, String user, String password) {
        this.connection = connection;
        this.user = user;
        this.password = password;
    }

    public boolean isOpen() {
        return connection != null;
    }

    public void open() throws IOException {
        if (proxyHost != null) {
            int port = proxyPort == 0 ? url.getPort() : proxyPort;
            open(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, port)));
        }
        else {
            connection = (HttpURLConnection)url.openConnection();
        }
    }

    private void open(Proxy proxy) throws IOException {
        connection = (HttpURLConnection)url.openConnection(proxy);
    }

    public void prepare(String method) throws IOException {
        if (readTimeout >= 0)
            connection.setReadTimeout(readTimeout);
        if (connectTimeout >= 0)
            connection.setConnectTimeout(connectTimeout);

        if (headers != null) {
            for (String key : headers.keySet()) {
                connection.setRequestProperty(key, headers.get(key));
            }
        }
        if (user != null) {
            String value = user + ":" + password;
            connection.setRequestProperty(HTTP_BASIC_AUTH_HEADER, "Basic " + new String(Base64.encodeBase64(value.getBytes())));
        }

        if ("PATCH".equalsIgnoreCase(method)) {
            // use HttpAltConnection for true HTTP PATCH
            connection.setRequestProperty("X-HTTP-Method-Override", "PATCH");
            connection.setRequestMethod("PUT");
        }
        else {
            connection.setRequestMethod(method);
        }

        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
            connection.setDoOutput(true);
        }
        else if ("GET".equalsIgnoreCase(method)) {
            connection.setDoOutput(false);
            HttpURLConnection.setFollowRedirects(followRedirects);
        }
    }

    HttpResponse response;
    public HttpResponse getResponse() { return response; }
    public void setResponse(HttpResponse resp) { response = resp; }

    /**
     * Populates the response member.  Closes the connection.
     */
    protected HttpResponse readInput() throws IOException {
        InputStream is = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[2048];
            try {
                is = connection.getInputStream();
                while (maxBytes == -1 || baos.size() < maxBytes) {
                    int bytesRead = is.read(buffer);
                    if (bytesRead == -1)
                        break;
                    baos.write(buffer, 0, bytesRead);
                }
                response = new HttpResponse(baos.toByteArray());
                return response;
            }
            catch (IOException ex) {
                InputStream eis = null;
                try {
                    eis = connection.getErrorStream();
                    while (maxBytes == -1 || baos.size() < maxBytes) {
                        int bytesRead = eis.read(buffer);
                        if (bytesRead == -1)
                            break;
                        baos.write(buffer, 0, bytesRead);
                    }
                    response = new HttpResponse(baos.toByteArray());
                    return response;
                }
                catch (Exception ex2) {
                    // throw original exception
                }
                finally {
                    if (eis != null) {
                        eis.close();
                    }
                }
                throw ex;
            }
        }
        finally {
            if (is != null)
                is.close();
            connection.disconnect();
            if (response != null) {
              response.setCode(connection.getResponseCode());
              response.setMessage(connection.getResponseMessage());
            }
            headers = new HashMap<String,String>();
            for (String headerKey : connection.getHeaderFields().keySet()) {
                if (headerKey == null)
                    headers.put("HTTP", connection.getHeaderField(headerKey));
                else
                    headers.put(headerKey, connection.getHeaderField(headerKey));
            }
        }
    }

    public OutputStream getOutputStream() throws IOException {
        return connection.getOutputStream();
    }

    public static String getBasicAuthHeader(String user, String password) {
        String value = user + ":" + password;
        return "Basic " + new String(Base64.encodeBase64(value.getBytes()));
    }

    /**
     * In return array, zeroth element is user and first is password.
     */
    public static String[] extractBasicAuthCredentials(String authHeader) {
        return new String(Base64.decodeBase64(authHeader.substring("Basic ".length()).getBytes())).split(":");
    }

}
